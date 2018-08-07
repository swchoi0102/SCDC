package kr.ac.snu.imlab.scdc.service.core;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapterFactory;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.Schedule.DefaultSchedule;
import edu.mit.media.funf.Schedule.BasicSchedule;
import edu.mit.media.funf.config.ConfigUpdater;
import edu.mit.media.funf.config.ConfigurableTypeAdapterFactory;
import edu.mit.media.funf.config.ContextInjectorTypeAdapaterFactory;
import edu.mit.media.funf.config.DefaultRuntimeTypeAdapterFactory;
import edu.mit.media.funf.config.DefaultScheduleSerializer;
import edu.mit.media.funf.config.HttpConfigUpdater;
import edu.mit.media.funf.config.SingletonTypeAdapterFactory;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.json.JsonUtils;
import edu.mit.media.funf.pipeline.Pipeline;
import edu.mit.media.funf.pipeline.PipelineFactory;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.Probe.PassiveProbe;
import edu.mit.media.funf.probe.Probe.ContinuousProbe;
import edu.mit.media.funf.probe.Probe.ContinuableProbe;
import edu.mit.media.funf.probe.Probe.State;
import edu.mit.media.funf.probe.Probe.StateListener;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.storage.DefaultArchive;
import edu.mit.media.funf.storage.FileArchive;
import edu.mit.media.funf.storage.HttpArchive;
import edu.mit.media.funf.storage.RemoteFileArchive;
import edu.mit.media.funf.time.TimeUtil;
import edu.mit.media.funf.util.StringUtil;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys.Config;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys.LogKeys;
import kr.ac.snu.imlab.scdc.service.probe.InsensitiveProbe;
import kr.ac.snu.imlab.scdc.util.SharedPrefsHandler;

/**
 * Created by kilho on 15. 8. 3.
 */
public class SCDCManager extends FunfManager {

  protected static final String TAG = "SCDCManager";

  public static final String
          ACTION_KEEP_ALIVE = "funf.keepalive",
          ACTION_INTERNAL = "funf.internal";

  private static final String
          PROBE_TYPE = "funf/probe",
          PIPELINE_TYPE = "funf/pipeline";

  private static final String
          DISABLED_PIPELINE_LIST = "__DISABLED__";

  private static final String
          PROBE_ACTION_REGISTER = "register",
          PROBE_ACTION_UNREGISTER = "unregister",
          PROBE_ACTION_REGISTER_PASSIVE = "register-passive",
          PROBE_ACTION_UNREGISTER_PASSIVE = "unregister-passive";


  private Handler handler;
  private JsonParser parser;
  private SharedPreferences prefs;
  private Map<String,Pipeline> pipelines;
  private Map<String,Pipeline> disabledPipelines;
  private Set<String> disabledPipelineNames;
  private Map<IJsonObject,List<DataRequestInfo>> dataRequests;
  private class DataRequestInfo {
    private DataListener listener;
    private Schedule schedule;
    private BigDecimal lastSatisfied;
    private JsonElement checkpoint;
  }

  private SharedPrefsHandler spHandler;

  private StateListener probeStateListener = new StateListener() {
    @Override
    public void onStateChanged(Probe probe, State previousState) {
      if (probe instanceof ContinuableProbe && previousState == State.RUNNING) {
        JsonElement checkpoint = ((ContinuableProbe)probe).getCheckpoint();
        IJsonObject config = (IJsonObject)JsonUtils.immutable(gson.toJsonTree(probe));
        for (DataRequestInfo requestInfo : dataRequests.get(config)) {
          requestInfo.checkpoint = checkpoint;
        }
      }
    }
  };

  // TODO: triggers

  // Maybe instances of probes are different from other, and are created in manager

  private Scheduler scheduler;


  @Override
  public void onCreate() {
    // super.onCreate();
//    Log.d(LogKeys.DEBUG, "SCDCManager.onCreate(): entering onCreate()");
    Log.d(SCDCKeys.LogKeys.DEB, TAG+".onCreate()");
    this.parser = new JsonParser();
    this.scheduler = new Scheduler();
    this.handler = new Handler();
    getGson(); // Sets gson
    this.dataRequests = new HashMap<IJsonObject, List<DataRequestInfo>>();
    this.prefs = getSharedPreferences(getClass().getName(), MODE_PRIVATE);
    this.pipelines = new HashMap<String, Pipeline>();
    this.disabledPipelines = new HashMap<String, Pipeline>();
    this.disabledPipelineNames = new HashSet<String>(Arrays.asList(prefs.getString(DISABLED_PIPELINE_LIST, "").split(",")));
    this.disabledPipelineNames.remove(""); // Remove the empty name, if no disabled pipelines exist
    this.spHandler = SharedPrefsHandler.getInstance(this,
                      Config.SCDC_PREFS, Context.MODE_PRIVATE);
    reload();
  }

  public void reload() {
    if (Looper.myLooper() != Looper.getMainLooper()) {
      handler.post(new Runnable() {
        @Override
        public void run() {
          reload();
        }
      });
      return;
    }
    Set<String> pipelineNames = new HashSet<String>();
//    Log.d(LogKeys.DEBUG, "SCDCManager.reload(): prefs=" + prefs.getAll().toString());
    Log.d(SCDCKeys.LogKeys.DEB, TAG+".reload(): prefs=" + prefs.getAll().toString());
    pipelineNames.addAll(prefs.getAll().keySet());
    pipelineNames.remove(DISABLED_PIPELINE_LIST);
    Bundle metadata = getMetadata();
    pipelineNames.addAll(metadata.keySet());
    for (String pipelineName : pipelineNames) {
      reload(pipelineName);
    }
  }

  public void reload(final String name) {
    if (Looper.myLooper() != Looper.getMainLooper()) {
      handler.post(new Runnable() {
        @Override
        public void run() {
          reload(name);
        }
      });
      return;
    }
    String pipelineConfig = null;
    Bundle metadata = getMetadata();
    if (prefs.contains(name)) {
      pipelineConfig = prefs.getString(name, null);
    } else if (metadata.containsKey(name)) {
      pipelineConfig = metadata.getString(name);
    }
    Log.d(SCDCKeys.LogKeys.DEB, TAG+".reload(pipeline): pipelineConfig=" + pipelineConfig);
//    Log.d(LogKeys.DEBUG, "SCDCManager.reload(): pipelineConfig=" + pipelineConfig);
    if (disabledPipelineNames.contains(name)) {
      // Disabled, so don't load any config
      Pipeline disabledPipeline = gson.fromJson(pipelineConfig, Pipeline.class);
      disabledPipelines.put(name, disabledPipeline);
      pipelineConfig = null;
    }
    if (pipelineConfig == null) {
      unregisterPipeline(name);
    } else {
      Pipeline newPipeline = gson.fromJson(pipelineConfig, Pipeline.class);
      registerPipeline(name, newPipeline); // Will unregister previous before running
    }
  }

  public JsonObject getPipelineConfig(String name) {
    String configString = prefs.getString(name, null);
    Bundle metadata = getMetadata();
    if (configString == null && metadata.containsKey(name)) {
      configString = metadata.getString(name);
    }
    return configString == null ? null : new JsonParser().parse(configString).getAsJsonObject();
  }

  public boolean save(String name, JsonObject config) {
    try {
      // Check if this is a valid pipeline before saving
      Pipeline pipeline = getGson().fromJson(config, Pipeline.class);
      return prefs.edit().putString(name, config.toString()).commit();
    } catch (Exception e) {
      Log.e(LogKeys.DEBUG, "Unable to save config: " + config.toString());
      return false;
    }
  }

  public boolean saveAndReload(String name, JsonObject config) {
    boolean success = save(name, config);
    if (success) {
      reload(name);
    }
    return success;
  }

  @Override
  public void onDestroy() {
    Log.d(SCDCKeys.LogKeys.DEB, TAG+".onDestroy()");
    // super.onDestroy();

    // TODO: call onDestroy on all pipelines
    for (Pipeline pipeline : pipelines.values()) {
      pipeline.onDestroy();
    }

    // TODO: save outstanding requests
    // TODO: remove all remaining Alarms

    // TODO: make sure to destroy all probes
    for (Object probeObject : getProbeFactory().getCached()) {
      String componentString = JsonUtils.immutable(gson.toJsonTree(probeObject)).toString();
      cancelProbe(componentString);
      ((Probe)probeObject).destroy();
    }
    getProbeFactory().clearCache();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    String action = intent.getAction();
//    Log.d(LogKeys.DEBSW, action);
    if (action == null || ACTION_KEEP_ALIVE.equals(action)) {
      // Does nothing, but wakes up SCDCManager
//      Log.d(SCDCKeys.LogKeys.DEB, TAG+".onStartCommand() : Does nothing, but wakes up SCDCManager");
    } else if (!spHandler.isSensorOn()) {
      // IMPORTANT: Does nothing if sensor button is not on
//      Log.d(SCDCKeys.LogKeys.DEB, TAG+".onStartCommand() : IMPORTANT: Does nothing if sensor button is not on");
//      Log.d(LogKeys.DEBUG, TAG+".onStartCommand(): spHandler.isSensorOn()=" + spHandler.isSensorOn());
    } else if (ACTION_INTERNAL.equals(action)) {
//      Log.d(SCDCKeys.LogKeys.DEB, TAG+".onStartCommand() : sensor button is on : " + ACTION_INTERNAL);
      String type = intent.getType();
      Uri componentUri = intent.getData();
      if (PROBE_TYPE.equals(type)) {
        // Handle probe action
        IJsonObject probeConfig = (IJsonObject)JsonUtils.immutable(parser.parse(getComponentName(componentUri)));
        String probeAction = getAction(componentUri);

        BigDecimal now = TimeUtil.getTimestamp();
        final Probe probe = getGson().fromJson(probeConfig, Probe.class);
        List<DataRequestInfo> requests = dataRequests.get(probeConfig);

        // TODO: Need to allow for some listeners to be registered and unregistered on different schedules
        if (probe != null) {
          if (PROBE_ACTION_REGISTER.equals(probeAction)) {
            if (requests != null) {

              // expId counter:
              String currProbeConfig = probeConfig.toString();
              int currExpId = spHandler.getExpId(currProbeConfig);
              spHandler.setExpId(currProbeConfig, currExpId+1);
//              Log.d(LogKeys.DEBUG, "SCDCManager.onStartCommand(): probeConfig=" + probeConfig.toString() +
//                      ", probeAction=" + probeAction + ", currExpId=" + spHandler.getExpId(currProbeConfig));

//              // FIXME: Request GC once
//              Log.d(LogKeys.DEBUG, "SCDCManager.onStartCommand(): request GC");
//              System.gc();

              List<DataListener> listenersThatNeedData = new ArrayList<Probe.DataListener>();
              List<DataRequestInfo> infoForListenersThatNeedData = new ArrayList<SCDCManager.DataRequestInfo>();
              for (DataRequestInfo requestInfo : requests) {
                BigDecimal interval = requestInfo.schedule.getInterval();
                // Compare date last satisfied to schedule interval
                if (requestInfo.lastSatisfied == null || now.subtract(requestInfo.lastSatisfied).compareTo(interval) >= 0) {
                  listenersThatNeedData.add(requestInfo.listener);
                  infoForListenersThatNeedData.add(requestInfo);
                }
              }

              final DataListener[] listenerArray = new DataListener[listenersThatNeedData.size()];
              listenersThatNeedData.toArray(listenerArray);
              if (listenerArray.length > 0) {
//                if (probe instanceof ContinuableProbe) {
//                  // TODO: how do we take care of multiple registrants with different checkpoints
//                  ((ContinuableProbe)probe).setCheckpoint(requests.get(0).checkpoint);
//                }
                probe.registerListener(listenerArray);
              }

//              Log.d(LogKeys.DEBUG, "Request: " + probe.getClass().getName());

              // Schedule unregister if continuous
              // TODO: do different durations for each schedule
              if (probe instanceof ContinuousProbe) {
                Schedule mergedSchedule = getMergedSchedule(infoForListenersThatNeedData);
//                Log.d(LogKeys.DEBSW, String.valueOf(mergedSchedule.getDuration()));
                if (mergedSchedule != null) {
                  long duration = TimeUtil.secondsToMillis(mergedSchedule.getDuration());
                  if (duration > 0) {
                    if (probe instanceof InsensitiveProbe) {
                      handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                          synchronized (listenerArray) {
                            Log.d(SCDCKeys.LogKeys.DEB, TAG+", PROBE_ACTION_REGISTER (1-sec earlier): call sendFinalData()");
                            ((InsensitiveProbe) probe).sendFinalData();
                          }
                        }
                      }, TimeUtil.secondsToMillis(mergedSchedule.getDuration().subtract(new BigDecimal(1))));
                    }

                    handler.postDelayed(new Runnable() {
                      @Override
                      public void run() {
//                        if (probe instanceof InsensitiveProbe) {
//                          synchronized (listenerArray) {
//                            Log.d(SCDCKeys.LogKeys.DEB, TAG+", PROBE_ACTION_REGISTER: call sendFinalData()");
//                            ((InsensitiveProbe) probe).sendFinalData();
//                          }
//                        }
                        ((ContinuousProbe) probe).unregisterListener(listenerArray);
                      }
                    }, TimeUtil.secondsToMillis(mergedSchedule.getDuration()));
                  }
                }
              }
            }
          }
          else if (PROBE_ACTION_UNREGISTER.equals(probeAction) && probe instanceof ContinuousProbe) {
            for (DataRequestInfo requestInfo : requests) {
              if (probe instanceof InsensitiveProbe) {
                synchronized (requestInfo.listener) {
                  Log.d(SCDCKeys.LogKeys.DEB, TAG+", PROBE_ACTION_UNREGISTER: call sendFinalData()");
                  ((InsensitiveProbe) probe).sendFinalData();
                }
              }
              ((ContinuousProbe)probe).unregisterListener(requestInfo.listener);
            }
          }
          else if (PROBE_ACTION_REGISTER_PASSIVE.equals(probeAction) && probe instanceof PassiveProbe) {
            if (requests != null) {
              for (DataRequestInfo requestInfo : requests) {
                if (requestInfo.schedule.isOpportunistic()) {
                  ((PassiveProbe)probe).registerPassiveListener(requestInfo.listener);
                }
              }
            }
          } else if (PROBE_ACTION_UNREGISTER_PASSIVE.equals(probeAction) && probe instanceof PassiveProbe) {
            if (requests != null) {
              for (DataRequestInfo requestInfo : requests) {
                ((PassiveProbe)probe).unregisterPassiveListener(requestInfo.listener);
              }
            }
          }
        }

        // TODO: Calculate new schedule for probe
      } else if (PIPELINE_TYPE.equals(type)) {
        // Handle pipeline action
        String pipelineName = getComponentName(componentUri);
        String pipelineAction = getAction(componentUri);
        Pipeline pipeline = pipelines.get(pipelineName);
        if (pipeline != null) {
          pipeline.onRun(pipelineAction, null);  // BY KILHO KIM
        }
      }

    }
    return Service.START_FLAG_RETRY; // TODO: may want the last intent always redelivered to make sure system starts up
  }

  private Bundle getMetadata() {
    try {
      Bundle metadata = getPackageManager().getServiceInfo(new ComponentName(this, this.getClass()), PackageManager.GET_META_DATA).metaData;
      return metadata == null ? new Bundle() : metadata;
    } catch (NameNotFoundException e) {
      throw new RuntimeException("Unable to get metadata for the SCDCManager service.");
    }
  }

  /**
   * Get a gson builder with the probe factory built in
   * @return
   */
  public GsonBuilder getGsonBuilder() {
    return getGsonBuilder(this);
  }

  public static class ConfigurableRuntimeTypeAdapterFactory<E> extends DefaultRuntimeTypeAdapterFactory<E> {

    public ConfigurableRuntimeTypeAdapterFactory(Context context, Class<E> baseClass, Class<? extends E> defaultClass) {
      super(context,
              baseClass,
              defaultClass,
              new ContextInjectorTypeAdapaterFactory(context, new ConfigurableTypeAdapterFactory()));
    }

  }

  /**
   * Get a gson builder with the probe factory built in
   * @return
   */
  public static GsonBuilder getGsonBuilder(Context context) {
    return new GsonBuilder()
            .registerTypeAdapterFactory(getProbeFactory(context))
            .registerTypeAdapterFactory(getPipelineFactory(context))
            .registerTypeAdapterFactory(new ConfigurableRuntimeTypeAdapterFactory<Schedule>(context, Schedule.class, BasicSchedule.class))
            .registerTypeAdapterFactory(new ConfigurableRuntimeTypeAdapterFactory<ConfigUpdater>(context, ConfigUpdater.class, HttpConfigUpdater.class))
            .registerTypeAdapterFactory(new ConfigurableRuntimeTypeAdapterFactory<FileArchive>(context, FileArchive.class, DefaultArchive.class))
            .registerTypeAdapterFactory(new ConfigurableRuntimeTypeAdapterFactory<RemoteFileArchive>(context, RemoteFileArchive.class, HttpArchive.class))
            .registerTypeAdapter(DefaultSchedule.class, new DefaultScheduleSerializer())
            .registerTypeAdapter(Class.class, new JsonSerializer<Class<?>>() {

              @Override
              public JsonElement serialize(Class<?> src, Type typeOfSrc, JsonSerializationContext context) {
                return src == null ? JsonNull.INSTANCE : new JsonPrimitive(src.getName());
              }
            });
  }

  private Gson gson;
  /**
   * Get a Gson instance which includes the SingletonProbeFactory
   * @return
   */
  public Gson getGson() {
    if (gson == null) {
      gson = getGsonBuilder().create();
    }
    return gson;
  }

  public TypeAdapterFactory getPipelineFactory() {
    return getPipelineFactory(this);
  }

  private static PipelineFactory PIPELINE_FACTORY;
  public static PipelineFactory getPipelineFactory(Context context) {
    if (PIPELINE_FACTORY == null) {
      PIPELINE_FACTORY = new PipelineFactory(context);
    }
    return PIPELINE_FACTORY;
  }

  public SingletonTypeAdapterFactory getProbeFactory() {
    return getProbeFactory(this);
  }

  private static SingletonTypeAdapterFactory PROBE_FACTORY;
  public static SingletonTypeAdapterFactory getProbeFactory(Context context) {
    if (PROBE_FACTORY == null) {
      PROBE_FACTORY = new SingletonTypeAdapterFactory(
              new DefaultRuntimeTypeAdapterFactory<Probe>(
                      context,
                      Probe.class,
                      null,
                      new ContextInjectorTypeAdapaterFactory(context, new ConfigurableTypeAdapterFactory())));
    }
    return PROBE_FACTORY;
  }

  // triggers
  // SCDCManager should allow you to register for triggers
  // Scheduler will register for triggers

  public void registerPipeline(String name, Pipeline pipeline) {
    synchronized (pipelines) {
//      Log.d(LogKeys.DEBUG, "Registering pipeline: " + name);
      unregisterPipeline(name);
      pipelines.put(name, pipeline);
      pipeline.onCreate(this);
    }
  }

  public Pipeline getRegisteredPipeline(String name) {
    Pipeline p = pipelines.get(name);
    if (p == null) {
      p = disabledPipelines.get(name);
    }
    return p;
  }

  public void unregisterPipeline(String name) {
    synchronized (pipelines) {
      Pipeline existingPipeline = pipelines.remove(name);
      if (existingPipeline != null) {
        existingPipeline.onDestroy();
      }
    }
  }

  public void enablePipeline(String name) {
    Log.d(LogKeys.DEB, "SCDCManager: enablePipeline");
    boolean previouslyDisabled = disabledPipelineNames.remove(name);
    if (previouslyDisabled) {
      prefs.edit().putString(DISABLED_PIPELINE_LIST, StringUtil.join(disabledPipelineNames, ",")).commit();
      reload(name);
    }
  }

  public boolean isEnabled(String name) {
    return this.pipelines.containsKey(name) && !disabledPipelineNames.contains(name);
  }

  public void disablePipeline(String name) {
    Log.d(LogKeys.DEB, "SCDCManager: disablePipeline");
    boolean previouslyEnabled = disabledPipelineNames.add(name);
    if (previouslyEnabled) {
      prefs.edit().putString(DISABLED_PIPELINE_LIST, StringUtil.join(disabledPipelineNames, ",")).commit();
      reload(name);
    }
  }

  public void requestData(DataListener listener, JsonElement probeConfig) {
//    Log.d(SCDCKeys.LogKeys.DEB, "requestData");
    requestData(listener, (JsonElement)probeConfig, null);
  }

  public void requestData(DataListener listener, JsonElement probeConfig, Schedule schedule) {
    if (probeConfig == null) {
      throw new IllegalArgumentException("Probe config cannot be null");
    }
    // Use schedule in probeConfig @schedule annotation
    Probe probe = gson.fromJson(probeConfig, Probe.class);
    probe.addStateListener(probeStateListener);
    if (schedule == null) {
      DefaultSchedule defaultSchedule = probe.getClass().getAnnotation(DefaultSchedule.class);
      JsonObject scheduleObject = defaultSchedule == null ? new JsonObject() : gson.toJsonTree(defaultSchedule, DefaultSchedule.class).getAsJsonObject();
      if (probeConfig.isJsonObject() && probeConfig.getAsJsonObject().has(PipelineFactory.SCHEDULE)) {
        JsonUtils.deepCopyOnto(probeConfig.getAsJsonObject().get(PipelineFactory.SCHEDULE).getAsJsonObject(), scheduleObject, true);
      }
      schedule = gson.fromJson(scheduleObject, Schedule.class);
    }
    IJsonObject completeProbeConfig = (IJsonObject)JsonUtils.immutable(gson.toJsonTree(probe));  // Make sure probe config is complete and consistent
    requestData(listener, completeProbeConfig, schedule);
  }

  private void requestData(DataListener listener, IJsonObject completeProbeConfig, Schedule schedule) {
    if (listener == null) {
      throw new IllegalArgumentException("Listener cannot be null");
    }
    if (completeProbeConfig == null) {
      throw new IllegalArgumentException("Probe config cannot be null");
    }
    DataRequestInfo newDataRequest = new DataRequestInfo();
    newDataRequest.lastSatisfied = null;
    newDataRequest.listener = listener;
    newDataRequest.schedule = schedule;
    synchronized (dataRequests) {
      List<DataRequestInfo> requests = dataRequests.get(completeProbeConfig);
      if (requests == null) {
        requests = new ArrayList<SCDCManager.DataRequestInfo>();
        dataRequests.put(completeProbeConfig, requests);
      }
      unrequestData(listener, completeProbeConfig);
      requests.add(newDataRequest);
    }
    rescheduleProbe(completeProbeConfig);
  }

  public void unrequestAllData(DataListener listener) {
    unrequestData(listener, (IJsonObject)null);
  }

  public void unrequestData(DataListener listener, JsonElement probeConfig) {
//    Log.d(SCDCKeys.LogKeys.DEB, "unrequestData");
    Probe probe = gson.fromJson(probeConfig, Probe.class);
    IJsonObject completeProbeConfig = (IJsonObject)JsonUtils.immutable(gson.toJsonTree(probe));  // Make sure probe config is complete and consistent
    unrequestData(listener, completeProbeConfig);
    rescheduleProbe(completeProbeConfig);
  }

  private String getPipelineName(Pipeline pipeline) {
    for (Map.Entry<String, Pipeline> entry : pipelines.entrySet()) {
      if (entry.getValue() == pipeline) {
        return entry.getKey();
      }
    }
    return null;
  }

  public void registerPipelineAction(Pipeline pipeline, String action, Schedule schedule) {
    String name = getPipelineName(pipeline);
    if (name != null) {
      Log.d(LogKeys.DEB, TAG+".registerPipelineAction(): " + name);
      scheduler.set(PIPELINE_TYPE, getComponenentUri(name, action), schedule);
    }
  }

  public void unregisterPipelineAction(Pipeline pipeline, String action) {
    String name = getPipelineName(pipeline);
    if (name != null) {
      Log.d(LogKeys.DEB, TAG+".unregisterPipelineAction(): " + name);
      scheduler.cancel(PIPELINE_TYPE, getComponenentUri(name, action));
    }
  }

  /**
   * This version does not reschedule.
   * @param listener
   * @param completeProbeConfig
   */
  private void unrequestData(DataListener listener, IJsonObject completeProbeConfig) {
    synchronized (dataRequests) {
      List<DataRequestInfo> requests = null;
      if (completeProbeConfig == null) {
        requests = new ArrayList<SCDCManager.DataRequestInfo>();
        for (List<DataRequestInfo> requestInfos : dataRequests.values()) {
          requests.addAll(requestInfos);
        }
      } else {
        requests = dataRequests.get(completeProbeConfig);
      }
      Probe probe = gson.fromJson(completeProbeConfig, Probe.class);
      for (int i = 0; i < requests.size(); i++) {
        if (requests.get(i).listener == listener) {
          requests.remove(i);
          if (probe instanceof ContinuousProbe) {
            ((ContinuousProbe)probe).unregisterListener(listener);
          }
          if (probe instanceof PassiveProbe) {
            ((PassiveProbe)probe).unregisterPassiveListener(listener);
          }
          break; // Should only have one request for this listener and probe
        }
      }
    }
  }

  private Schedule getMergedSchedule(List<DataRequestInfo> requests) {
    BasicSchedule mergedSchedule = null;
    for (DataRequestInfo request: requests) {
      if (mergedSchedule == null) {
        mergedSchedule = new BasicSchedule(request.schedule);
      } else {
        // Min interval
        mergedSchedule.setInterval(mergedSchedule.getInterval().min(request.schedule.getInterval()));
        // Max duration
        mergedSchedule.setDuration(mergedSchedule.getDuration().max(request.schedule.getDuration()));
        // Strict if one is strict
        mergedSchedule.setStrict(mergedSchedule.isStrict() || request.schedule.isStrict());
      }
    }
    return mergedSchedule;
  }

  private void rescheduleProbe(IJsonObject completeProbeConfig) {
    synchronized (dataRequests) {
      // Simple schedule merge for now
      // TODO: make this more efficient
      String componentString = completeProbeConfig.toString();
      List<DataRequestInfo> requests = dataRequests.get(completeProbeConfig);
      if (requests.isEmpty()) {
        cancelProbe(componentString);
      } else {
        Schedule mergedSchedule = getMergedSchedule(requests);
        for (DataRequestInfo request: requests) {
          // Schedule passive listening if opportunistic
          if (request.schedule.isOpportunistic()) {
            Probe probe = gson.fromJson(completeProbeConfig, Probe.class);
            if (probe instanceof PassiveProbe) {
              ((PassiveProbe)probe).registerPassiveListener(request.listener);
            }
          }
        }
        Intent intent = getFunfIntent(SCDCManager.this, PROBE_TYPE, getComponenentUri(componentString, PROBE_ACTION_REGISTER));
        startService(intent);
//        bindService(intent, scdcServiceConn, BIND_AUTO_CREATE); // BIND_IMPORTANT?
//        unbindService(scdcManagerConn);
//        scheduler.set(PROBE_TYPE, getComponenentUri(componentString, PROBE_ACTION_REGISTER), mergedSchedule);
      }
    }
  }

  public class LocalBinder extends Binder {
    public SCDCManager getManager() {
      return SCDCManager.this;
    }
  }

  @Override
  public IBinder onBind(Intent intent) {
    Log.d(SCDCKeys.LogKeys.DEB, TAG+".onBind()");
    return new LocalBinder();
  }

  /////////////////////////////////////////////
  // Reserve action for later inter-funf communication
  // Use type to differentiate between probe/pipeline
  // funf:<componenent_name>#<action>

  private static final String
          FUNF_SCHEME = "funf";


  // TODO: should these public?  May be confusing for people just using the library
  private static Uri getComponenentUri(String component, String action) {
    return new Uri.Builder()
            .scheme(FUNF_SCHEME)
            .path(component) // Automatically prepends slash
            .fragment(action)
            .build();
  }

  private static String getComponentName(Uri componentUri) {
    return componentUri.getPath().substring(1); // Remove automatically prepended slash from beginning
  }

  private static String getAction(Uri componentUri) {
    return componentUri.getFragment();
  }

  private static Intent getFunfIntent(Context context, String type, String component, String action) {
    return getFunfIntent(context, type, getComponenentUri(component, action));
  }

  private static Intent getFunfIntent(Context context, String type, Uri componentUri) {
    Intent intent = new Intent();
    intent.setClass(context, SCDCManager.class);
    intent.setPackage(context.getPackageName());
    intent.setAction(ACTION_INTERNAL);
    intent.setDataAndType(componentUri, type);
    return intent;
  }


  private void cancelProbe(String probeConfig) {
    scheduler.cancel(PROBE_TYPE, getComponenentUri(probeConfig, PROBE_ACTION_REGISTER));
    scheduler.cancel(PROBE_TYPE, getComponenentUri(probeConfig, PROBE_ACTION_UNREGISTER));
    scheduler.cancel(PROBE_TYPE, getComponenentUri(probeConfig, PROBE_ACTION_REGISTER_PASSIVE));
    scheduler.cancel(PROBE_TYPE, getComponenentUri(probeConfig, PROBE_ACTION_UNREGISTER_PASSIVE));

    // Set expId back to 0
    spHandler.setExpId(probeConfig.toString(), 0);
  }

  ////////////////////////////////////////////////////


  private Scheduler getScheduler() {
    return scheduler;
  }

  private class Scheduler {

    private AlarmManager alarmManager;
    private Context context;

    // private Map<Pipeline,Config,Schedule>
    // Need to be able to merge schedules for common types quickly, across pipelines
    // private Map<Config,<Schedule,Pipeline>>

    // Use factory to build data listeners from gson?
    // Or just grab data listener from pipeline
    // What about running other operations?  Should they all just have a run/start and maybe stop?

    // IDEA:
    // Send config back to pipeline, let it decide how to handle it.


    public Scheduler() {
      this.alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
      this.context = SCDCManager.this;
    }


    public void cancel(String type, Uri componentAndAction) {
      Intent intent = getFunfIntent(context, type, componentAndAction);
      PendingIntent operation = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_NO_CREATE);
      if (operation != null) {
        // FIXME: Cancel existing alarms
        alarmManager.cancel(operation);
        operation.cancel();
//        Log.d(LogKeys.DEB, TAG+".scheduler.cancel()");
      }
    }

    public void cancel(String type, String component, String action) {
      cancel(type, getComponenentUri(component, action));
    }

    public void set(String type, String component, String action, Schedule schedule) {
      set(type, getComponenentUri(component, action), schedule);
    }

    public void set(String type, Uri componentAndAction, Schedule schedule) {

      // Creates pending intents that will call back into SCDCManager
      // Uses alarm manager to time them
      Intent intent = getFunfIntent(context, type, componentAndAction);
      PendingIntent operation = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

      // TODO: figure out how to do previous time for all components, including pipeline actions
      Number previousTime = null;

      // TODO: add random start for initial
      // startTimeMillis += random;


      BigDecimal startTime = schedule.getNextTime(previousTime);
      if (startTime != null) {
        long startTimeMillis = TimeUtil.secondsToMillis(startTime);
        if (schedule.getInterval() == null || schedule.getInterval().intValue() == 0) {
          alarmManager.set(AlarmManager.RTC_WAKEUP, startTimeMillis, operation);
        } else {
          long intervalMillis = TimeUtil.secondsToMillis(schedule.getInterval());
          if (schedule.isStrict()) {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, startTimeMillis, intervalMillis, operation);
          } else {
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, startTimeMillis, intervalMillis, operation);
          }
        }
      }

    }

    // TODO: Feature to wait a certain amount of seconds after boot to begin
    // TODO: Feature to prevent too many things from running at once, w/ random backoff times


    // All are in timestamp seconds
    // PARAMS
    // time (time to run, may not need this for now)
    // strict (do we need wakeup as a separate parameter, or are all wakeup?)
    // interval (period)
    // duration (start to stop time)
    // opportunistic (For probes, means use other probe being run as an excuse to run this one, not sure what it means for others, its possible this won't be part of scheduling)



    // Schedule runnables
    // Data request would be able to contain

    // Pipeline could provide data listener for probes
    // Pipeline could provide mechanism for creating objects to RUN!!!

    // Configuration, is loaded into pipeline which creates the object to run
    // This allows separate probes if required
    // Allows creation of database services to be determined by pipeline
  }

}
