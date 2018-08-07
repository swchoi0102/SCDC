package kr.ac.snu.imlab.scdc.service.core;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.config.ConfigUpdater;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.config.RuntimeTypeAdapterFactory;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.pipeline.Pipeline;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.probe.builtin.ProbeKeys;
import edu.mit.media.funf.storage.DefaultArchive;
import edu.mit.media.funf.storage.FileArchive;
import edu.mit.media.funf.storage.RemoteFileArchive;
import edu.mit.media.funf.storage.UploadService;
import edu.mit.media.funf.util.StringUtil;
import kr.ac.snu.imlab.scdc.activity.LaunchActivity;
import kr.ac.snu.imlab.scdc.activity.OnDataReceivedListener;
import kr.ac.snu.imlab.scdc.service.storage.SCDCDatabaseHelper;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys.LogKeys;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys.LabelKeys;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys.Config;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys.SharedPrefs;
import kr.ac.snu.imlab.scdc.util.SharedPrefsHandler;

/**
 * Created by kilho on 15. 7. 28.
 */
public class SCDCPipeline implements Pipeline, DataListener {

  protected static final String TAG = "SCDCPipeline";

  public static final String
    ACTION_ARCHIVE = "archive",
    ACTION_UPLOAD = "upload",
    ACTION_UPDATE = "update";

  protected final int ARCHIVE = 0, UPLOAD = 1, UPDATE = 2, DATA = 3;

  @Configurable
  protected String name = Config.PIPELINE_NAME;

  @Configurable
  protected int version = Config.PIPELINE_VERSION;

  @Configurable
  protected FileArchive archive = null;

  @Configurable
  protected RemoteFileArchive upload = null;

  @Configurable
  protected List<JsonElement> data = new ArrayList<JsonElement>();

  @Configurable
  protected Map<String, Schedule> schedules = new HashMap<String, Schedule>();

  private UploadService uploader;
  // private Activity activity;
  private OnDataReceivedListener odrl;
  private SharedPrefsHandler spHandler;

  private boolean enabled;
  private SCDCManager manager;
  private SQLiteOpenHelper databaseHelper = null;
  private Looper looper;
  private Handler handler;
  private Handler.Callback callback = new Handler.Callback() {

    @Override
    public boolean handleMessage(Message msg) {
      onBeforeRun(msg.what, (JsonObject)msg.obj);
      switch (msg.what) {
        case ARCHIVE:
          if (archive != null) {
            Log.w(LogKeys.DEBUG,
                    "SCDCPipeline.Handler.Callback().handleMessage(): " +
                    "running runArchive()");
            runArchive();
          }
          break;
        case UPLOAD:
//          if (archive != null && upload != null && uploader != null) {
//            Log.w(LogKeys.DEBUG,
//                    "SCDCPipeline.Handler.Callback().handleMessage(): " +
//                    "running uploader.run(archive, upload)");
//            // uploader.start();
//            uploader.run(archive, upload);
//          }
          Log.w(LogKeys.DEBUG,
                  "Pipeline upload is disabled!");
          break;
        case DATA:
          String name = ((JsonObject)msg.obj).get("name").getAsString();
          int sensorId = ((JsonObject)msg.obj).get(SharedPrefs.KEY_SENSOR_ID).getAsInt();
          int expId = ((JsonObject)msg.obj).get(SharedPrefs.KEY_EXP_ID).getAsInt();
          IJsonObject data = (IJsonObject)((JsonObject)msg.obj).get("value");
          writeData(name, sensorId, expId, data);
          break;
        default:
          break;
      }
      onAfterRun(msg.what, (JsonObject)msg.obj);
      return false;
    }
  };

  public void reloadDbHelper(Context ctx) {
    this.databaseHelper = new SCDCDatabaseHelper(ctx, StringUtil.simpleFilesafe(name), version);
  }

  // Edited by Kilho Kim:
  protected void runArchive() {
    // new BackgroundArchiver().execute();
    SQLiteDatabase db = databaseHelper.getWritableDatabase();
    // TODO: add check to make sure this is not empty
    File dbFile = new File(db.getPath());
    db.close();
    archive.add(dbFile);
    reloadDbHelper(manager);
    databaseHelper.getWritableDatabase(); // Build new database
  }

  protected void writeData(String name, int sensorId, int expId, IJsonObject data) {
    // In case: 1) When the data table is suddenly truncated
    //          2) When it tries to re-open an already-closed SQLiteDatabase
    try {
      SQLiteDatabase db = databaseHelper.getWritableDatabase();
      final double timestamp = data.get(ProbeKeys.BaseProbeKeys.TIMESTAMP).getAsDouble();
//      final int expId = data.get(SharedPrefs.KEY_EXP_ID).getAsInt();
//      final int sensorId = data.get(SharedPrefs.KEY_SENSOR_ID).getAsInt();

      final String value = data.toString();
      // if (name == null || value == null) {
      /*
      if (timestamp == 0L || name == null || value == null) {
          Log.e(LogUtil.TAG, "Unable to save data.  Not all required values specified. " + timestamp + " " + name + " - " + value);
          throw new SQLException("Not all required fields specified.");
      }
      */
      ContentValues cv = new ContentValues();
      cv.put(SCDCDatabaseHelper.COLUMN_NAME, name);
      cv.put(SCDCDatabaseHelper.COLUMN_SENSOR_ID, sensorId);
      cv.put(SCDCDatabaseHelper.COLUMN_EXP_ID, expId);
      cv.put(SCDCDatabaseHelper.COLUMN_VALUE, value);
      cv.put(SCDCDatabaseHelper.COLUMN_TIMESTAMP, timestamp);
//      cv.put(SharedPrefs.KEY_SENSOR_ID, sensorId);
      // Added by Kilho Kim: When the data table is suddenly truncated:
      db.insertOrThrow(SCDCDatabaseHelper.DATA_TABLE.name, "", cv);
    } catch (Exception e) {
      // Do nothing
      Log.e(LogKeys.DEBUG, TAG+".writeData(): " + e.toString());
    }
  }

  @Override
  public void onCreate(FunfManager manager) {
    Log.d(SCDCKeys.LogKeys.DEB, TAG+".onCreate()");
    if (archive == null) {
      archive = new DefaultArchive(manager, name);
    }
    if (uploader == null) {
      uploader = new UploadService(manager);
      uploader.start();
    }
    this.manager = (SCDCManager)manager;
    reloadDbHelper(manager);
    HandlerThread thread = new HandlerThread(getClass().getName());
//    Log.w("DEBUG", "new thread=" + thread.getName());
    thread.setPriority(Process.THREAD_PRIORITY_BACKGROUND); // FIXME: test if it works
    thread.start();
    this.looper = thread.getLooper();
    this.handler = new Handler(looper, callback);
    enabled = true;
    this.spHandler = SharedPrefsHandler.getInstance(this.manager,
                     Config.SCDC_PREFS, Context.MODE_PRIVATE);

    for (JsonElement dataRequest : data) {
      manager.requestData(this, dataRequest);
    }
    for (Map.Entry<String, Schedule> schedule : schedules.entrySet()) {
      manager.registerPipelineAction(this, schedule.getKey(), schedule.getValue());
    }
  }

  /**
   * @author Kilho Kim
   * @description Set dataReceivedListener for this pipeline
   */
  public void setDataReceivedListener(OnDataReceivedListener listener) {
    odrl = listener;
//    Log.d(LogKeys.DEBUG, "SCDCPipeline.setDataReceivedListener(): odrl=" + odrl);
  }

  @Override
  public void onDestroy() {
    Log.d(LogKeys.DEB, TAG+".onDestroy()");
    for (JsonElement dataRequest : data) {
      manager.unrequestData(this, dataRequest);
    }
    for (Map.Entry<String, Schedule> schedule : schedules.entrySet()) {
      manager.unregisterPipelineAction(this, schedule.getKey());
    }
    if (uploader != null) {
      uploader.stop();
    }

//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
//      looper.quitSafely();
//      Log.d(SCDCKeys.LogKeys.DEB, TAG+".onDestroy(): safely quit! (build version >= API-17)");
//    } else {
//      looper.quit();
//      Log.d(SCDCKeys.LogKeys.DEB, TAG+".onDestroy(): just quit... (build version < API-17)");
//    }
    looper.quit();
    Log.d(SCDCKeys.LogKeys.DEB, TAG+".onDestroy(): just quit...");

    enabled = false;
    odrl = null;
    databaseHelper.close();
  }

  @Override
  public void onRun(String action, JsonElement config) {
    Message message;
    // Run on handler thread
    if (ACTION_ARCHIVE.equals(action)) {
      message = Message.obtain(handler, ARCHIVE, config);
      handler.sendMessage(message);
    } else if (ACTION_UPLOAD.equals(action)) {
      message = Message.obtain(handler, UPLOAD, config);
      handler.sendMessage(message);
    } else if (ACTION_UPDATE.equals(action)) {
      message = Message.obtain(handler, UPDATE, config);
      handler.sendMessage(message);
    }
  }

  /**
   * Used as a hook to customize behavior before an action takes place.
   * @param action the type of action taking place
   * @param config the configuration for the action
   */
  protected void onBeforeRun(int action, JsonElement config) {

  }

  /**
   * Used as a hook to customize behavior after an action takes place.
   * @param action the type of action taking place
   * @param config the configuration for the action
   */
  protected void onAfterRun(int action, JsonElement config) {

  }

  public Handler getHandler() {
    return handler;
  }

  protected FunfManager getFunfManager() {
    return manager;
  }


  public SQLiteDatabase getDb() {
    return databaseHelper.getReadableDatabase();
  }

  public SQLiteDatabase getWritableDb() {
    return databaseHelper.getWritableDatabase();
  }

  public List<JsonElement> getDataRequests() {
    return data == null ? null : Collections.unmodifiableList(data);
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  public String getName() {
    return name;
  }


  public void setName(String name) {
    this.name = name;
  }


  public int getVersion() {
    return version;
  }


  public void setVersion(int version) {
    this.version = version;
  }


  public FileArchive getArchive() {
    return archive;
  }


  public void setArchive(FileArchive archive) {
    this.archive = archive;
  }


  public RemoteFileArchive getUpload() {
    return upload;
  }


  public void setUpload(RemoteFileArchive upload) {
    this.upload = upload;
  }


  public void setDataRequests(List<JsonElement> data) {
    this.data = new ArrayList<JsonElement>(data); // Defensive copy
  }


  public Map<String, Schedule> getSchedules() {
    return schedules;
  }


  public void setSchedules(Map<String, Schedule> schedules) {
    this.schedules = schedules;
  }


  public UploadService getUploader() {
    return uploader;
  }


  public void setUploader(UploadService uploader) {
    this.uploader = uploader;
  }


  public SQLiteOpenHelper getDatabaseHelper() {
    return databaseHelper;
  }


  public void setDatabaseHelper(SQLiteOpenHelper databaseHelper) {
    this.databaseHelper = databaseHelper;
  }


  @Override
  public void onDataReceived(IJsonObject probeConfig, IJsonObject data) {
    // Add expId and sensorId as new key-values to the original data
    JsonObject dataClone = data.getAsJsonObject();
//    dataClone.addProperty(SharedPrefs.KEY_EXP_ID,
//                          spHandler.getExpId(probeConfig.toString()));
//    dataClone.addProperty(SharedPrefs.KEY_SENSOR_ID,
//                          spHandler.getSensorId());


    // Add labeling info to data
    // 1) for normal labels
    String[] normalLabelNames = LaunchActivity.normalLabelNames;
    for (int i = 0; i < normalLabelNames.length; i++) {
      dataClone.addProperty(normalLabelNames[i],
              !(spHandler.getStartLoggingTime(i) == -1));
//      Log.d(LogKeys.DEBUG, String.valueOf(i)+normalLabelNames[i]+String.valueOf(!(spHandler.getStartLoggingTime(i) == -1)));
    }
    // 2) for special labels
    String[] specialLabelNames = LaunchActivity.specialLabelNames;
    for (int i = 0; i < specialLabelNames.length; i++) {
      dataClone.addProperty(specialLabelNames[i],
              !(spHandler.getStartLoggingTime(normalLabelNames.length+i) == -1));
//      Log.d(LogKeys.DEBUG, specialLabelNames[i]+String.valueOf(!(spHandler.getStartLoggingTime(normalLabelNames.length+i) == -1)));
    }

    // Add alone/together info to data
    dataClone.addProperty(LabelKeys.TOGETHER_STATUS, spHandler.getTogetherStatus_Bi());

//      // Add AccompanyingStatusLabelEntry info to data
//    dataClone.addProperty(LabelKeys.ACCOMPANYING_LABEL,
//                          spHandler.getAccompanyingStatus(
//                            LabelKeys.ACCOMPANYING_STATUS_LABEL_ID));
//    // Add ConversingStatusLabelEntry info to data
//    dataClone.addProperty(LabelKeys.CONVERSING_LABEL,
//                          spHandler.getConversingStatus(
//                            LabelKeys.CONVERSING_STATUS_LABEL_ID));

    IJsonObject dataWithLables = new IJsonObject(dataClone);
    // FIXME: Uncomment below to enhance CPU performance
//    Log.d(LogKeys.DEBUG, "SCDCPipeline.onDataReceived(): probeConfig=" + probeConfig.toString() +
//            ", data=" + dataWithExpId.toString());// + ", schedule=" + manager.getPipelineConfig(name));

    JsonObject sensorExpObj = new JsonObject();
    sensorExpObj.addProperty(SharedPrefs.KEY_SENSOR_ID, spHandler.getSensorId());
    sensorExpObj.addProperty(SharedPrefs.KEY_EXP_ID, spHandler.getExpId(probeConfig.toString()));

    JsonObject record = new JsonObject();
    record.add("name", probeConfig.get(RuntimeTypeAdapterFactory.TYPE));
//    record.add(SharedPrefs.KEY_SENSOR_ID, sensorIdObj);
//    record.add(SharedPrefs.KEY_EXP_ID, sensorIdObj);

    record.add(SharedPrefs.KEY_SENSOR_ID, sensorExpObj.get(SharedPrefs.KEY_SENSOR_ID));
    record.add(SharedPrefs.KEY_EXP_ID, sensorExpObj.get(SharedPrefs.KEY_EXP_ID));

    // add dataWithExpId instead of the original data
    record.add("value", dataWithLables);
    Message message = Message.obtain(handler, DATA, record);

//    double allocated = Debug.getNativeHeapAllocatedSize() / 1048576.0;
//    double available = Debug.getNativeHeapSize() / 1048576.0;
//    double free = Debug.getNativeHeapFreeSize() / 1048576.0;
//    DecimalFormat df = new DecimalFormat();
//    df.setMaximumFractionDigits(2);
//    df.setMinimumFractionDigits(2);
//
//    Log.d(LogKeys.DEBUG, "SCDCPipeline: heap native: allocated " + df.format(allocated) +
//                         "MB of " + df.format(available) + "MB (" + df.format(free) +
//                         "MB free)");
//    Log.d(LogKeys.DEBUG, "SCDCPipeline: memory: allocated " +
//                         df.format(Runtime.getRuntime().totalMemory() / 1048576.0) +
//                         "MB of " + df.format(Runtime.getRuntime().maxMemory() / 1048576.0) +
//                         "MB (" + df.format(Runtime.getRuntime().freeMemory() / 1048576.0) +
//                         "MB free)");

//    ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
//    ActivityManager activityManager =
//      (ActivityManager) manager.getSystemService(Context.ACTIVITY_SERVICE);
//    activityManager.getMemoryInfo(mi);
//    long availableMegs = mi.availMem / 1048576L;
//    long percentAvail = mi.availMem / mi.totalMem;
//    Log.d(LogKeys.DEBUG, "SCDCPipeline: maxMemory=" + maxMemory);
//    Log.d(LogKeys.DEBUG, "SCDCPipeline: availableMegs=" + availableMegs +
//                         " (percentAvail: " + percentAvail + ")");
    if (handler != null) {
//      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
//        handler.sendMessage(message);
//      } else {
//        if (dataClone.has("isUrgent")) {
//          handler.sendMessageAtFrontOfQueue(message);
//          Log.d(SCDCKeys.LogKeys.DEB, TAG+".onDataReceived: send urgent message at front of queue");
//        }
//        else handler.sendMessage(message);
//      }

      if (dataClone.has("isUrgent")) {
        handler.sendMessageAtFrontOfQueue(message);
        Log.d(SCDCKeys.LogKeys.DEB, TAG+".onDataReceived: send urgent message at front of queue");
      }
      else handler.sendMessage(message);
    }

    // FIXME:
//    if (odrl != null) {
//      odrl.updateLaunchActivityUi();
//    }
  }

  @Override
  public void onDataCompleted(IJsonObject probeConfig, JsonElement checkpoint) {
    // TODO Figure out what to do with continuations of probes, if anything

  }
}
