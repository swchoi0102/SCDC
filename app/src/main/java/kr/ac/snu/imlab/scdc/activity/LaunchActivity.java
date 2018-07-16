package kr.ac.snu.imlab.scdc.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.util.ArrayList;

import edu.mit.media.funf.config.ConfigUpdater.ConfigUpdateException;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.config.HttpConfigUpdater;
import edu.mit.media.funf.storage.FileArchive;
import edu.mit.media.funf.util.EqualsUtil;
import kr.ac.snu.imlab.scdc.R;
import kr.ac.snu.imlab.scdc.adapter.BaseAdapterExLabel2;
import kr.ac.snu.imlab.scdc.entry.AccompanyingStatusLabelEntry;
import kr.ac.snu.imlab.scdc.entry.ConversingStatusLabelEntry;
import kr.ac.snu.imlab.scdc.entry.LabelEntry;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys.AlertKeys;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys.Config;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys.LabelKeys;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys.LogKeys;
import kr.ac.snu.imlab.scdc.service.core.SCDCManager;
import kr.ac.snu.imlab.scdc.service.core.SCDCPipeline;
import kr.ac.snu.imlab.scdc.service.core.SCDCService;
import kr.ac.snu.imlab.scdc.service.storage.MultipartEntityArchive;
import kr.ac.snu.imlab.scdc.service.storage.SCDCDatabaseHelper;
import kr.ac.snu.imlab.scdc.service.storage.SCDCUploadService;
import kr.ac.snu.imlab.scdc.service.storage.ZipArchive;
import kr.ac.snu.imlab.scdc.util.SharedPrefsHandler;
import kr.ac.snu.imlab.scdc.util.TimeUtil;


public class LaunchActivity extends ActionBarActivity
                            implements OnDataReceivedListener {

  protected static final String TAG = "LaunchActivity";
  protected static final String TAGG = "editButton";

  @Configurable
  // FIXME: Change below to false when publishing
  public static boolean DEBUGGING = false;

  @Configurable
  protected int version = 5;

  // FIXME: The list of normal labels
  @Configurable
  public static final String[] normalLabelNames = {
          LabelKeys.MOVING_HAND,
          LabelKeys.STOP_HAND,
          LabelKeys.MOVING_POCKET,
          LabelKeys.STOP_POCKET,
          LabelKeys.MOVING_TABLE,
          LabelKeys.STOP_TABLE,
          LabelKeys.MOVING_BAG,
          LabelKeys.STOP_BAG,
  };

  // FIXME: The list of special labels
  @Configurable
  public static final String[] specialLabelNames = {
          LabelKeys.NONE_OF_ABOVE_LABEL
  };

  // FIXME: The list of 'active' labels
  @Configurable
  public static final String[] activeLabelNames = {
          LabelKeys.MOVING_HAND,
          LabelKeys.STOP_HAND,
          LabelKeys.MOVING_POCKET,
          LabelKeys.STOP_POCKET,
          LabelKeys.MOVING_TABLE,
          LabelKeys.STOP_TABLE,
          LabelKeys.MOVING_BAG,
          LabelKeys.STOP_BAG,
          LabelKeys.NONE_OF_ABOVE_LABEL
  };

  private Handler handler;
  private SharedPrefsHandler spHandler;

  // Username EditText and Button
  private EditText userName;
  private Button userNameButton;
  private RadioButton isMaleRadioButton;
  private RadioButton isFemaleRadioButton;
  boolean isEdited = false;

  // Probe list View
  private ViewGroup mAsLabelView;
  private ViewGroup mCsLabelView;
  private ListView mListView;

  private GridView mGridView;
  private GridView mGridViewNone;
  public BaseAdapterExLabel2 mAdapter;
  public BaseAdapterExLabel2 mAdapterNone;

  // Labels list
  private ArrayList<LabelEntry> normalLabelEntries;
  private ArrayList<LabelEntry> specialLabelEntries;

  // Run Data Collection button
//  private ToggleButton enabledToggleButton;
  private ToggleButton aloneToggleButton, togetherToggleButton;

  // Run Push notification button
//  private ToggleButton reminderToggleButton;

  private Button archiveButton, truncateDataButton, editDataButton;
  private TextView dataCountView;
  private ImageView receivingDataImageView;
  private TextView timeCountView;

  class AccompanyingStatusViewHolder {
    TextView asLogLabelTv;
    TextView asScheduleTv;
    Button endLogBt;
    ArrayList<Button> startLogBts;
  }
  private AccompanyingStatusViewHolder asViewHolder;
  private AccompanyingStatusLabelEntry asLabelEntry;

  class ConversingStatusViewHolder {
    TextView csLogLabelTv;
    TextView csScheduleTv;
    Button endLogBt;
    ArrayList<Button> startLogBts;
  }
  private ConversingStatusViewHolder csViewHolder;
  private ConversingStatusLabelEntry csLabelEntry;

  private BroadcastReceiver alertReceiver;

  private SCDCManager scdcManager;
  private SCDCPipeline pipeline;

  private SCDCService scdcService;

  /**
   * Alertdialog which shows up when there is a problem with connection
   * to Google API.
   */
  private AlertDialog mAlertDialog;

  public ServiceConnection scdcServiceConn = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {


      scdcService = ((SCDCService.LocalBinder) service).getService();
      Log.d(LogKeys.DEBB, TAG+".scdcServiceConn.onServiceConnected()");
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
      scdcService = null;
      Log.d(LogKeys.DEBB, TAG+".scdcServiceConn.onServiceDisconnected()");
    }
  };

  private ServiceConnection scdcManagerConn = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      // IMPORTANT: Should disable pipeline here,
      //            as unbindService in SCDCService isn't called
      //            if LaunchActivity binds to SCDCManager right after it
      scdcManager = ((SCDCManager.LocalBinder) service).getManager();
      pipeline = (SCDCPipeline) scdcManager
              .getRegisteredPipeline(Config.PIPELINE_NAME);
      scdcManager.disablePipeline(Config.PIPELINE_NAME);
      pipeline.reloadDbHelper(scdcManager);

      Log.d(LogKeys.DEBUG, TAG+".scdcManagerConn.onServiceConnected(): " +
                           "pipeline.getName()=" + pipeline.getName() +
                           ", pipeline.getDatabaseHelper()=" +
                           pipeline.getDatabaseHelper());
      Log.d(LogKeys.DEBB, TAG+".scdcManagerConn.onServiceConnected()");

      pipeline.setDataReceivedListener(LaunchActivity.this);

      // Update probe schedules of pipeline
      Log.d(LogKeys.DEBUG, TAG+".scdcManagerConn.onServiceConnected(): "
                            + "spHandler.isActiveLabelOn()=" +
                            spHandler.isActiveLabelOn());
//      changeConfig(spHandler.isActiveLabelOn());

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
      scdcManager = null;
      pipeline = null;
      Log.d(LogKeys.DEBB, TAG+".scdcManagerConn.onServiceDisconnected()");
    }
  };


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Log.d(LogKeys.DEBB, TAG+".onCreate()");
    spHandler = SharedPrefsHandler.getInstance(this,
            Config.SCDC_PREFS, Context.MODE_PRIVATE);

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_launch);

    // Make sure the keyboard only pops up
    // when a user clicks into an EditText
    this.getWindow().setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    setUserInfo();


    // Add a single AccompanyingStatusLabelEntry
    asLabelEntry =
      new AccompanyingStatusLabelEntry(LabelKeys.ACCOMPANYING_STATUS_LABEL_ID,
              LabelKeys.ACCOMPANYING_LABEL, LaunchActivity.this, Config.SCDC_PREFS);

    // Add a single ConversingStatusLabelEntry
    csLabelEntry =
      new ConversingStatusLabelEntry(LabelKeys.CONVERSING_STATUS_LABEL_ID,
              LabelKeys.CONVERSING_LABEL, LaunchActivity.this, Config.SCDC_PREFS);

    // The list of labels available
    normalLabelEntries = new ArrayList<LabelEntry>(normalLabelNames.length);
    for (int i = 0; i < normalLabelNames.length; i++) {
      normalLabelEntries.add(new LabelEntry(i, normalLabelNames[i],
                          LaunchActivity.this, Config.SCDC_PREFS));
//      Log.d(LogKeys.DEBUG, String.valueOf(i)+normalLabelNames[i]);
    }

    specialLabelEntries = new ArrayList<LabelEntry>(specialLabelNames.length);
    for (int i=0; i < specialLabelNames.length; i++) {
      specialLabelEntries.add(new LabelEntry(normalLabelNames.length+i, specialLabelNames[i],
              LaunchActivity.this, Config.SCDC_PREFS));
//      Log.d(LogKeys.DEBUG, String.valueOf(normalLabelNames.length+i)+specialLabelNames[i]);
    }

    // Put the total number of labels into SharedPreferences
    spHandler.setNumLabels(normalLabelEntries.size()+specialLabelEntries.size());

    receivingDataImageView = (ImageView)findViewById(R.id.receiving_data_iv);
    archiveButton = (Button) findViewById(R.id.archiveButton);
    truncateDataButton = (Button) findViewById(R.id.truncateDataButton);
    editDataButton = (Button) findViewById(R.id.editDataButton);

    aloneToggleButton = (ToggleButton)findViewById(R.id.aloneToggleButton);
    togetherToggleButton = (ToggleButton)findViewById(R.id.togetherToggleButton);

    mAdapter = new BaseAdapterExLabel2(this, normalLabelEntries);
    mGridView = (GridView)findViewById(R.id.label_grid_view);
    mGridView.setAdapter(mAdapter);
    setGridViewHeightBasedOnChildren(mGridView, 2);

    mAdapterNone = new BaseAdapterExLabel2(this, specialLabelEntries);
    mGridViewNone = (GridView)findViewById(R.id.label_grid_view_none);
    mGridViewNone.setAdapter(mAdapterNone);
    setGridViewHeightBasedOnChildren(mGridViewNone, 1);


    // ********* Need to get rid of these Views :(
    mAsLabelView = (ViewGroup) getLayoutInflater().inflate(R.layout.accompanying_status_label_view_item_layout, null, false);
    mCsLabelView = (ViewGroup) getLayoutInflater().inflate(R.layout.conversing_status_label_view_item_layout, null, false);
    // ********* Need to get rid of these Listeners :(
    setAccompanyingStatusListener();
    setConversingStatusListener();


    // Used to make interface changes on main thread
    handler = new Handler();

    // Displays the count of rows in the data
    dataCountView = (TextView) findViewById(R.id.dataCountText);

    // Displays the count of time
    timeCountView = (TextView)findViewById(R.id.timeCountTextView);

    // time count and alone/together button status
    if (spHandler.isSensorOn()){
      aloneToggleButton.setEnabled(false);
      togetherToggleButton.setEnabled(false);
    }
    else{
      if (spHandler.isAloneOn() || spHandler.isTogetherOn()) {
        timeCountView.setText(getString(R.string.select));
        timeCountView.setTextColor(getResources().getColor(R.color.select));
        aloneToggleButton.setEnabled(!spHandler.isTogetherOn());
        togetherToggleButton.setEnabled(!spHandler.isAloneOn());
      }
      // when too much data
      else if (spHandler.isTooMuchData()) {
        timeCountView.setText(getString(R.string.too_much_data));
        timeCountView.setTextColor(getResources().getColor(R.color.too_much_data));
        aloneToggleButton.setEnabled(false);
        togetherToggleButton.setEnabled(false);
      } else {
        timeCountView.setText(getString(R.string.disabled));
        timeCountView.setTextColor(getResources().getColor(R.color.disabled));
        aloneToggleButton.setEnabled(true);
        togetherToggleButton.setEnabled(true);
      }
    }

    aloneToggleButton.setChecked(spHandler.isAloneOn());
    togetherToggleButton.setChecked(spHandler.isTogetherOn());

    archiveButton.setEnabled(!spHandler.isAloneOn() && !spHandler.isTogetherOn());
    truncateDataButton.setEnabled(!spHandler.isAloneOn() && !spHandler.isTogetherOn());
    editDataButton.setEnabled(!spHandler.isAloneOn() && !spHandler.isTogetherOn());
    userNameButton.setEnabled(!spHandler.isAloneOn() && !spHandler.isTogetherOn());

    // FIXME
//    if (!spHandler.isAloneOn() && !spHandler.isTogetherOn()){
//      spHandler.setSensorOn(false);
//    }

//    // Bind SCDCManager service if sensor is off
//    if (!spHandler.isSensorOn()) {
//      bindService(new Intent(LaunchActivity.this, SCDCManager.class),
//              scdcManagerConn, BIND_AUTO_CREATE);
//      Log.d(LogKeys.DEBB, TAG+".bindService() : scdcManager");
//    } else { // Bind SCDCService service if sensor is on
//      bindService(new Intent(LaunchActivity.this, SCDCService.class),
//              scdcServiceConn, BIND_AUTO_CREATE);  // BIND_IMPORTANT?
//      Log.d(LogKeys.DEBB, TAG+".bindService() : scdcService");
//    }

    // Bind SCDCManager service if alone and together OFF
    if (!spHandler.isAloneOn() && !spHandler.isTogetherOn()) {
      bindService(new Intent(LaunchActivity.this, SCDCManager.class),
              scdcManagerConn, BIND_AUTO_CREATE);
      Log.d(LogKeys.DEBB, TAG+".bindService() : scdcManager");

//      if(spHandler.isTooMuchData()) Toast.makeText(LaunchActivity.this, getString(R.string.too_much_data), Toast.LENGTH_LONG).show();

    } else { // Bind SCDCService service if alone or together ON
      bindService(new Intent(LaunchActivity.this, SCDCService.class),
              scdcServiceConn, BIND_AUTO_CREATE);  // BIND_IMPORTANT?
      Log.d(LogKeys.DEBB, TAG+".bindService() : scdcService");
    }
    Log.d(LogKeys.DEB, "alone :\t" +String.valueOf(spHandler.isAloneOn()) + "\t"
            + "togeth :\t" +String.valueOf(spHandler.isTogetherOn()) + "\t"
            + "sensor :\t" +String.valueOf(spHandler.isSensorOn()));


    aloneToggleButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        if (isChecked) {
//          Log.d(LogKeys.DEB, TAG+".AloneButton checked!");

          Intent intent = new Intent(LaunchActivity.this, SCDCService.class);

          // Increment sensorId by 1
          spHandler.setSensorId(spHandler.getSensorId() + 1);
          Toast.makeText(LaunchActivity.this,
                  SCDCKeys.SharedPrefs.KEY_SENSOR_ID + ": " + spHandler.getSensorId(),
                  Toast.LENGTH_SHORT).show();

          // Start/Bind SCDCService and unbind SCDCManager instead
          startService(intent);
          bindService(intent, scdcServiceConn, BIND_AUTO_CREATE); // BIND_IMPORTANT?
          unbindService(scdcManagerConn);

          timeCountView.setText(getResources().getString(R.string.select));
          timeCountView.setTextColor(getResources().getColor(R.color.select));
          togetherToggleButton.setEnabled(false);

        } else {
          Log.d(LogKeys.DEB, TAG+".AloneButton unchecked!");
          mAdapter.notifyDataSetChanged();
          mAdapterNone.notifyDataSetChanged();
//          spHandler.setReminderRunning(isChecked);

          // when too much data
          if (spHandler.isTooMuchData()) {
            timeCountView.setText(getString(R.string.too_much_data));
            timeCountView.setTextColor(getResources().getColor(R.color.too_much_data));
            aloneToggleButton.setEnabled(false);
            togetherToggleButton.setEnabled(false);
          } else {
            timeCountView.setText(getString(R.string.disabled));
            timeCountView.setTextColor(getResources().getColor(R.color.disabled));
            aloneToggleButton.setEnabled(true);
            togetherToggleButton.setEnabled(true);
          }

          // Unbind/Stop SCDCService and bind SCDCManager instead
          unbindService(scdcServiceConn);
          stopService(new Intent(LaunchActivity.this, SCDCService.class));
          bindService(new Intent(LaunchActivity.this, SCDCManager.class),
                  scdcManagerConn, BIND_AUTO_CREATE);

//          spHandler.setSensorOn(false);
        }
        spHandler.setAloneOn(isChecked);

        archiveButton.setEnabled(!isChecked);
        truncateDataButton.setEnabled(!isChecked);
        editDataButton.setEnabled(!isChecked);
        userNameButton.setEnabled(!isChecked);
//        togetherToggleButton.setEnabled(!isChecked);
//        Log.d(LogKeys.DEBB, "alone :\t" +String.valueOf(spHandler.isAloneOn()) + "\t"
//                + "sensor :\t" +String.valueOf(spHandler.isSensorOn()));
        Log.d(LogKeys.DEB, "alone :\t" +String.valueOf(spHandler.isAloneOn()) + "\t"
                + "togeth :\t" +String.valueOf(spHandler.isTogetherOn()) + "\t"
                + "sensor :\t" +String.valueOf(spHandler.isSensorOn()));
      }
    });

    togetherToggleButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        if (isChecked) {
          Log.d(LogKeys.DEB, TAG+".TogetherButton checked!");

//          mAdapter.notifyDataSetChanged();
//          mAdapterNone.notifyDataSetChanged();

//            try {
//                Thread.sleep(3000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }

            Intent intent = new Intent(LaunchActivity.this, SCDCService.class);

          // Increment sensorId by 1
          spHandler.setSensorId(spHandler.getSensorId() + 1);
          Toast.makeText(LaunchActivity.this,
                  SCDCKeys.SharedPrefs.KEY_SENSOR_ID + ": " + spHandler.getSensorId(),
                  Toast.LENGTH_SHORT).show();

          // Start/Bind SCDCService and unbind SCDCManager instead
          startService(intent);
          bindService(intent, scdcServiceConn, BIND_AUTO_CREATE); // BIND_IMPORTANT?
          unbindService(scdcManagerConn);

          timeCountView.setText(getResources().getString(R.string.select));
          timeCountView.setTextColor(getResources().getColor(R.color.select));
          aloneToggleButton.setEnabled(false);

        } else {
          Log.d(LogKeys.DEB, TAG+".TogetherButton unchecked!");
          mAdapter.notifyDataSetChanged();
          mAdapterNone.notifyDataSetChanged();
//          spHandler.setReminderRunning(isChecked);

          // when too much data
          if (spHandler.isTooMuchData()) {
            timeCountView.setText(getString(R.string.too_much_data));
            timeCountView.setTextColor(getResources().getColor(R.color.too_much_data));
            aloneToggleButton.setEnabled(false);
            togetherToggleButton.setEnabled(false);
          } else {
            timeCountView.setText(getString(R.string.disabled));
            timeCountView.setTextColor(getResources().getColor(R.color.disabled));
            aloneToggleButton.setEnabled(true);
            togetherToggleButton.setEnabled(true);
          }

          // Unbind/Stop SCDCService and bind SCDCManager instead
          unbindService(scdcServiceConn);
          stopService(new Intent(LaunchActivity.this, SCDCService.class));
          bindService(new Intent(LaunchActivity.this, SCDCManager.class),
                  scdcManagerConn, BIND_AUTO_CREATE);
//          spHandler.setSensorOn(false);
        }
        spHandler.setTogetherOn(isChecked);

        archiveButton.setEnabled(!isChecked);
        truncateDataButton.setEnabled(!isChecked);
        editDataButton.setEnabled(!isChecked);
        userNameButton.setEnabled(!isChecked);
//        aloneToggleButton.setEnabled(!isChecked);

//        Log.d(LogKeys.DEBB, "togeth :\t" +String.valueOf(spHandler.isTogetherOn()) + "\t"
//                + "sensor :\t" +String.valueOf(spHandler.isSensorOn()));
        Log.d(LogKeys.DEB, "alone :\t" +String.valueOf(spHandler.isAloneOn()) + "\t"
                + "togeth :\t" +String.valueOf(spHandler.isTogetherOn()) + "\t"
                + "sensor :\t" +String.valueOf(spHandler.isSensorOn()));
      }
    });



    // Runs an archive if pipeline is enabled
    archiveButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(final View v) {
        if (isNetworkConnected()) {
          if (pipeline.getDatabaseHelper() != null) {
            v.setEnabled(false);

            try {
              // Asynchronously synchronize preferences with server
              if (spHandler.setPrefsToServer()) {
                SQLiteDatabase db = pipeline.getWritableDb();
                Log.d(LogKeys.DEBUG, "LaunchActivity/ db.getPath()=" + db.getPath());
                File dbFile = new File(db.getPath());

                // Asynchronously archive and upload dbFile
                spHandler.saveTempLastIndex();
                archiveAndUploadDatabase(dbFile);
                dropAndCreateTable(db, true);
                spHandler.rollbackTempLastIndex();

                // Wait 5 seconds for archive to finish, then refresh the UI
                // (Note: this is kind of a hack since archiving is seamless
                //         and there are no messages when it occurs)
                handler.postDelayed(new Runnable() {
                                  @Override
                                  public void run() {
                    // pipeline.onRun(BasicPipeline.ACTION_ARCHIVE, null);
                    // pipeline.onRun(BasicPipeline.ACTION_UPLOAD, null);
                    updateLaunchActivityUi();
                    if (!aloneToggleButton.isChecked() && !togetherToggleButton.isChecked()) {
                      v.setEnabled(true);
                    }
            }
          }, 5000L);
              }
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        } else {
          Toast.makeText(getBaseContext(),
                getString(R.string.check_internet_connection_message),
                Toast.LENGTH_LONG).show();
        }
      }
    });

    // Truncate the data
    truncateDataButton.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            if (pipeline.getDatabaseHelper() != null) {
              SQLiteDatabase db = pipeline.getWritableDb();
              dropAndCreateTable(db, true);
              spHandler.rollbackTempLastIndex();
            }
      }
    });

    // go to data activity
    editDataButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Log.d(LogKeys.DEBB, TAGG+"edit button clicked");
        Intent intent = new Intent(LaunchActivity.this, DataActivity.class);

        String totalSensingInfo = spHandler.getTotalSensingTimeInfo();
        if (totalSensingInfo.equals("")) {
          Toast.makeText(getBaseContext(), getString(R.string.no_data_message),
                  Toast.LENGTH_LONG).show();
        } else {
          intent.putExtra("data", totalSensingInfo);
          startActivity(intent);
        }
      }
    });


    IntentFilter filter = new IntentFilter();
    filter.addAction(AlertKeys.ACTION_ALERT);
    alertReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        Log.d(LogKeys.DEBUG, TAG+".onCreate.alertReceiver/ Received broadcast");

        AlertDialog.Builder alert = new AlertDialog.Builder(LaunchActivity.this);
        int code = intent.getIntExtra(AlertKeys.EXTRA_ALERT_ERROR_CODE, -1);
        String message = intent.getStringExtra(AlertKeys.EXTRA_ALERT_ERROR_MESSAGE);
        mAlertDialog = alert.setTitle("Error")
                .setMessage(message + " (alert code:" + code + ")")
                .setPositiveButton("OK", null)
                .show();
      }
    };
    this.registerReceiver(alertReceiver, filter);
  }

  @Override
  public void onResume() {
    Log.d(LogKeys.DEBB, TAG+".onResume()");
    super.onResume();

    if (spHandler != null) {
      if (spHandler.getSensorIdsRemoveOrNot()) {
        String idsToRemove = spHandler.getSensorIdsToRemove();
        if (idsToRemove.length() > 0) {
          boolean updateSuccess = false;
          String[] idsToRemoveArr = idsToRemove.split(",");

          if (pipeline != null) {
            SCDCDatabaseHelper databaseHelper = (SCDCDatabaseHelper) pipeline.getDatabaseHelper();

            if (databaseHelper == null) pipeline.reloadDbHelper(scdcManager);
            if (databaseHelper != null) {
              SQLiteDatabase db = pipeline.getWritableDb();
              try {
                for (String sidStr : idsToRemoveArr) {
                  databaseHelper.updateTable(db, Integer.parseInt(sidStr));
                  spHandler.popSensingTimeInfo(Integer.parseInt(sidStr));
                }
                updateSuccess = true;
              } catch(Exception e) {
                updateSuccess = false;
              }
            }
          }

          else if (scdcService != null) {
            updateSuccess = scdcService.updateDB(idsToRemoveArr);
          }

          if (updateSuccess) {
            Toast.makeText(getBaseContext(), getString(R.string.db_table_update_success),
                    Toast.LENGTH_LONG).show();
          } else {
            Toast.makeText(getBaseContext(), getString(R.string.db_table_update_fail),
                    Toast.LENGTH_LONG).show();
          }
        }
        spHandler.initializeDataFixing();
      }
    }

    if (pipeline != null) {
      updateLaunchActivityUi();

      // When the last update time from server passed more than 5 minutes, update config.
      if (!spHandler.isSensorOn()){
        long lastTime = spHandler.getLastConfigUpdate();
        if ((lastTime == SCDCKeys.SharedPrefs.DEFAULT_LAST_CONFIG_UPDATE)
                || (System.currentTimeMillis() - lastTime > 650000)){
          updateConfig();
//          changeConfig(spHandler.isActiveLabelOn());
        }
      }
    }

    // Dynamically refresh the ListView items
    handler.postDelayed(new Runnable() {
      @Override
      public void run() {

        mAdapter.notifyDataSetChanged();
        mAdapterNone.notifyDataSetChanged();

        updateLaunchActivityUi();   // FIXME

        if(spHandler.isSensorOn()){

            String elapsedTime = TimeUtil.getElapsedTimeUntilNow(mAdapter.getLoggedItem().getStartLoggingTime());

          if(mAdapter.getLoggedItem()!=null){

//              Log.d("alarm", elapsedTime);
            if((Integer.parseInt(elapsedTime.substring(0, elapsedTime.length()-1)) <= 5)
                    && (Integer.parseInt(elapsedTime.substring(0, elapsedTime.length()-1)) > 0)
                    && (elapsedTime.substring(elapsedTime.length()-1).equals("초"))){

                ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100000);
                toneGen.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 200);
            }

            timeCountView.setText(elapsedTime+getString(R.string.time_count));
            timeCountView.setTextColor(getResources().getColor(R.color.logging));
          }

          else if(mAdapterNone.getLoggedItem()!=null){
            timeCountView.setText(elapsedTime+getString(R.string.time_count));
            timeCountView.setTextColor(getResources().getColor(R.color.logging));
          }
        }
        else{
          if(spHandler.isAloneOn() || spHandler.isTogetherOn()){
            timeCountView.setText(getResources().getString(R.string.select));
            timeCountView.setTextColor(getResources().getColor(R.color.select));
            aloneToggleButton.setEnabled(!spHandler.isTogetherOn());
            togetherToggleButton.setEnabled(!spHandler.isAloneOn());
          }
          // when too much data
          else if (spHandler.isTooMuchData()) {
            timeCountView.setText(getString(R.string.too_much_data));
            timeCountView.setTextColor(getResources().getColor(R.color.too_much_data));
            aloneToggleButton.setEnabled(false);
            togetherToggleButton.setEnabled(false);
          } else {
            timeCountView.setText(getString(R.string.disabled));
            timeCountView.setTextColor(getResources().getColor(R.color.disabled));
            aloneToggleButton.setEnabled(true);
            togetherToggleButton.setEnabled(true);
          }
        }
        handler.postDelayed(this, 1000L);
      }
    }, 1000L);


    // stopService(new Intent(this, AlarmButlerService.class));
  }

  @Override
  public void onPause() {
    Log.d(LogKeys.DEBB, TAG+".onPause()");
    super.onPause();

    // Save running status of reminder
//    spHandler.setReminderRunning(reminderToggleButton.isChecked());
  }


  @Override
  protected void onDestroy() {
    Log.d(LogKeys.DEBB, TAG+".onDestroy()");
    super.onDestroy();
//    // Unbind SCDCManager service if sensor is off
//    if (!spHandler.isSensorOn()) {
//      unbindService(scdcManagerConn);
//    } else { // Unbind SCDCService service if sensor is on
//      unbindService(scdcServiceConn);
//    }
    // Unbind SCDCManager service if alone and together OFF
    if (!spHandler.isAloneOn() && !spHandler.isTogetherOn()) {
      Log.d(LogKeys.DEBB, TAG+".unbindService() : scdcManager");
      unbindService(scdcManagerConn);
    } else { // Unbind SCDCService service if alone or together ON
      Log.d(LogKeys.DEBB, TAG+".unbindService() : scdcService");
      unbindService(scdcServiceConn);
    }
    this.unregisterReceiver(alertReceiver);
  }

  /**
   * @author Kilho Kim
   * Set user info UI.
   */
  private void setUserInfo() {
    // Set current username
    userName = (EditText) findViewById(R.id.user_name);
    userName.setText(spHandler.getUsername());
//    isMaleRadioButton = (RadioButton) findViewById(R.id.radio_male);
//    isFemaleRadioButton = (RadioButton) findViewById(R.id.radio_female);
//    isMaleRadioButton.setChecked(!spHandler.getIsFemale());
//    isFemaleRadioButton.setChecked(spHandler.getIsFemale());
    userName.setEnabled(false);
//    isMaleRadioButton.setEnabled(false);
//    isFemaleRadioButton.setEnabled(false);
    isEdited = false;

    userNameButton = (Button) findViewById(R.id.user_name_btn);
    userNameButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        // If it's currently not being edited now:
        if (!isEdited) {
          userName.setEnabled(true);
//          isMaleRadioButton.setEnabled(true);
//          isFemaleRadioButton.setEnabled(true);
          isEdited = true;
          userNameButton.setText(getString(R.string.save));
          // If it has just finished being edited:
        } else {
          spHandler.setUsername(userName.getText().toString());
//          spHandler.setIsFemale(isFemaleRadioButton.isChecked());
          userName.setEnabled(false);
//          isMaleRadioButton.setEnabled(false);
//          isFemaleRadioButton.setEnabled(false);
          isEdited = false;
          userNameButton.setText(getString(R.string.edit));
        }
      }
    });
  }



  /**
   * @author Kilho Kim
   * Set click listeners for ConversingStatusView buttons.
   */
  private void setConversingStatusListener() {
    csViewHolder = new ConversingStatusViewHolder();
    csViewHolder.csLogLabelTv =
            (TextView) mCsLabelView.findViewById(R.id.cs_log_label_tv);
    csViewHolder.csScheduleTv =
            (TextView) mCsLabelView.findViewById(R.id.cs_schedule_tv);
    csViewHolder.endLogBt =
            (Button) mCsLabelView.findViewById(R.id.end_cs_label_log_bt);
    csViewHolder.startLogBts = new ArrayList<Button>();
    csViewHolder.startLogBts.add(
            (Button) mCsLabelView.findViewById(R.id.quiet));
    csViewHolder.startLogBts.add(
            (Button) mCsLabelView.findViewById(R.id.talking));

    csViewHolder.csLogLabelTv.setText(LabelKeys.CONVERSING_LABEL);
    csViewHolder.endLogBt.setEnabled(csLabelEntry.isLogged());
    for (int i = 0; i < csViewHolder.startLogBts.size(); i++) {
      int conversingStatusId = i + 1;
      Button currBt = csViewHolder.startLogBts.get(i);
//      if (enabledToggleButton.isChecked()) {
//        if (csLabelEntry.isLogged())
//          currBt.setEnabled(csLabelEntry.getLoggedStatus() != conversingStatusId);
//        else currBt.setEnabled(true);
//      } else {
//        currBt.setEnabled(false);
//      }
    }


    if (csLabelEntry.isLogged()) {
      String elapsedTime =
              TimeUtil.getElapsedTimeUntilNow(csLabelEntry.getStartLoggingTime());
      csViewHolder.csScheduleTv.setText(" for " + elapsedTime);
    } else {
      csViewHolder.csScheduleTv.setText(R.string.probe_disabled);
    }

    // OnClickListener for end log button
    csViewHolder.endLogBt.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        boolean pastIsActiveLabelOn = spHandler.isActiveLabelOn();
        csLabelEntry.endLog();  // end label logging

        v.setEnabled(false);
        for (int i = 0; i < csViewHolder.startLogBts.size(); i++) {
          csViewHolder.startLogBts.get(i).setEnabled(true);
        }

        boolean currIsActiveLabelOn = spHandler.isActiveLabelOn();
        // Update config again only when isActiveLabelOn status gets changed
        if (pastIsActiveLabelOn != currIsActiveLabelOn)
          changeConfig(currIsActiveLabelOn);
      }
    });

    // OnClickListener for start log buttons
    for (int i = 0; i < csViewHolder.startLogBts.size(); i++) {
      final int currIdx = i;
      csViewHolder.startLogBts.get(i).setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          final int conversingStatusId = currIdx+1;  // 1, 2, 3, 4
          boolean pastIsActiveLabelOn = spHandler.isActiveLabelOn();
          Log.d(LogKeys.DEBUG, TAG+"setConversingStatusListener(): " +
                  "csViewHolder.startLogBts.get(" + currIdx + ")" +
                  ".setOnClickListener(): " + conversingStatusId);
          csLabelEntry.startLog(conversingStatusId);  // start label logging

          v.setEnabled(false);
          csViewHolder.endLogBt.setEnabled(true);
          for (int i = 0; i < csViewHolder.startLogBts.size(); i++) {
            int otherConversingStatusId = i+1;
            if (otherConversingStatusId != conversingStatusId)
              csViewHolder.startLogBts.get(i).setEnabled(true);
          }

          boolean currIsActiveLabelOn = spHandler.isActiveLabelOn();
          // Update config again only when isActiveLabelOn status gets changed
          if (pastIsActiveLabelOn != currIsActiveLabelOn)
            changeConfig(currIsActiveLabelOn);
        }
      });
    }
  }

  /**
   * @author Kilho Kim
   * Set click listeners for AccompanyingStatusView buttons.
   */
  private void setAccompanyingStatusListener() {
    asViewHolder = new AccompanyingStatusViewHolder();
    asViewHolder.asLogLabelTv =
      (TextView) mAsLabelView.findViewById(R.id.as_log_label_tv);
    asViewHolder.asScheduleTv =
      (TextView) mAsLabelView.findViewById(R.id.as_schedule_tv);
    asViewHolder.endLogBt =
      (Button) mAsLabelView.findViewById(R.id.end_as_label_log_bt);
    asViewHolder.startLogBts = new ArrayList<Button>();
    asViewHolder.startLogBts.add(
      (Button) mAsLabelView.findViewById(R.id.alone_bt));
    asViewHolder.startLogBts.add(
      (Button) mAsLabelView.findViewById(R.id.with_2_to_3_bt));
    asViewHolder.startLogBts.add(
      (Button) mAsLabelView.findViewById(R.id.with_4_to_6_bt));
    asViewHolder.startLogBts.add(
      (Button) mAsLabelView.findViewById(R.id.with_over_7_bt));

    asViewHolder.asLogLabelTv.setText("Company?");
    asViewHolder.endLogBt.setEnabled(asLabelEntry.isLogged());
    for (int i = 0; i < asViewHolder.startLogBts.size(); i++) {
      int accompanyingStatusId = i + 1;
      Button currBt = asViewHolder.startLogBts.get(i);
//      if (enabledToggleButton.isChecked()) {
//        if (asLabelEntry.isLogged())
//          currBt.setEnabled(asLabelEntry.getLoggedStatus() != accompanyingStatusId);
//        else currBt.setEnabled(true);
//      } else {
//        currBt.setEnabled(false);
//      }
    }

    // Refresh the elapsed time if the label is logged
    if (asLabelEntry.isLogged()) {
      String elapsedTime =
        TimeUtil.getElapsedTimeUntilNow(asLabelEntry.getStartLoggingTime());
      asViewHolder.asScheduleTv.setText(" for " + elapsedTime);
    } else {
      asViewHolder.asScheduleTv.setText(R.string.probe_disabled);
    }

    // OnClickListener for end log button
    asViewHolder.endLogBt.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        boolean pastIsActiveLabelOn = spHandler.isActiveLabelOn();
        asLabelEntry.endLog();  // end label logging

        v.setEnabled(false);
        for (int i = 0; i < asViewHolder.startLogBts.size(); i++) {
          asViewHolder.startLogBts.get(i).setEnabled(true);
        }

        boolean currIsActiveLabelOn = spHandler.isActiveLabelOn();
        // Update config again only when isActiveLabelOn status gets changed
        if (pastIsActiveLabelOn != currIsActiveLabelOn)
          changeConfig(currIsActiveLabelOn);
      }
    });

    // OnClickListener for start log buttons
    for (int i = 0; i < asViewHolder.startLogBts.size(); i++) {
      final int currIdx = i;
      asViewHolder.startLogBts.get(i).setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          final int accompanyingStatusId = currIdx+1;  // 1, 2, 3, 4
          boolean pastIsActiveLabelOn = spHandler.isActiveLabelOn();
          Log.d(LogKeys.DEBUG, TAG+"setAccompanyingStatusListener(): " +
                  "anViewHolder.startLogBts.get(" + currIdx + ")" +
                  ".setOnClickListener(): " + accompanyingStatusId);
          asLabelEntry.startLog(accompanyingStatusId);  // start label logging

          v.setEnabled(false);
          asViewHolder.endLogBt.setEnabled(true);
          for (int i = 0; i < asViewHolder.startLogBts.size(); i++) {
            int otherAccompanyingStatusId = i+1;
            if (otherAccompanyingStatusId != accompanyingStatusId)
              asViewHolder.startLogBts.get(i).setEnabled(true);
          }

          boolean currIsActiveLabelOn = spHandler.isActiveLabelOn();
          // Update config again only when isActiveLabelOn status gets changed
          if (pastIsActiveLabelOn != currIsActiveLabelOn)
            changeConfig(currIsActiveLabelOn);
        }
      });
    }
  }

  /**
   * @author Kilho Kim
   * Truncate table of the database of the pipeline.
   */
  private void dropAndCreateTable(final SQLiteDatabase db,
                                  final boolean showProgress) {
    new AsyncTask<SQLiteDatabase, Void, Boolean>() {

      private ProgressDialog progressDialog;
      private SCDCDatabaseHelper databaseHelper;

      @Override
      protected void onPreExecute() {
        if (showProgress) {
          progressDialog = new ProgressDialog(LaunchActivity.this);
          progressDialog.setMessage(getString(R.string.truncate_message));
          progressDialog.setCancelable(false);
          progressDialog.show();
        }

        databaseHelper = (SCDCDatabaseHelper) pipeline.getDatabaseHelper();
      }

      @Override
      protected Boolean doInBackground(SQLiteDatabase... dbs) {
        return databaseHelper.dropAndCreateDataTable(dbs[0]);
      }

      @Override
      protected void onPostExecute(Boolean isSuccess) {
        if (showProgress) {
          progressDialog.dismiss();
        }

        // reset all accumulated times !
        spHandler.resetSensingTimeInfo();
//        for (LabelEntry entry : normalLabelEntries){
//          spHandler.resetAccumulatedTime(entry.getId());
//        }
//        for (LabelEntry entry : specialLabelEntries){
//          spHandler.resetAccumulatedTime(entry.getId());
//        }

        dataCountView.setText("Data size: 0.0 MB");
        spHandler.setTooMuchData(false);
        updateLaunchActivityUi();
        Toast.makeText(getBaseContext(), getString(R.string.truncate_complete_message),
                Toast.LENGTH_LONG).show();
      }
    }.execute(db);
  }

  public SCDCManager getActivitySCDCManager() {
    return scdcManager;
  }

  private void archiveAndUploadDatabase(final File dbFile) {
    new AsyncTask<File, Void, Boolean>() {

      private ProgressDialog progressDialog;
      private FileArchive archive;
      private MultipartEntityArchive upload;
      private SCDCUploadService uploader;

      @Override
      protected void onPreExecute() {
        progressDialog = new ProgressDialog(LaunchActivity.this);
        progressDialog.setMessage(getString(R.string.archive_message));
        progressDialog.setCancelable(false);
        progressDialog.show();

        archive = new ZipArchive(scdcManager, Config.PIPELINE_NAME);
        if (DEBUGGING) {
          upload = new MultipartEntityArchive(scdcManager,
                  Config.DEFAULT_UPLOAD_URL_DEBUG, LaunchActivity.this);
        } else {
          upload = new MultipartEntityArchive(scdcManager,
                  Config.DEFAULT_UPLOAD_URL, LaunchActivity.this);
        }
        uploader = new SCDCUploadService(scdcManager);
        uploader.setContext(LaunchActivity.this);
        uploader.start();
      }

      @Override
      protected Boolean doInBackground(File... files) {
        return archive.add(files[0]);
      }

      @Override
      protected void onPostExecute(Boolean isSuccess) {
        progressDialog.dismiss();
        uploader.run(archive, upload);

        // IMPORTANT: Update config at this time once more
        updateConfig();
        // uploader.stop();
      }
    }.execute(dbFile);
  }


  // to reset shared pref values when app freezes
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.

//    Toast.makeText(getBaseContext(), "option menu!", Toast.LENGTH_LONG).show();


    Log.d(LogKeys.DEBB, "1. end all labels");
    for (LabelEntry label : normalLabelEntries) {
      if(label.isLogged()) {
        long startTime = label.getStartLoggingTime();
        long elapsedTime = TimeUtil.getElapsedTimeUntilNow(startTime, "second");
//        label.endLog(elapsedTime);
        label.endLog(spHandler.getSensorId(), spHandler.getTogetherStatus_Bi(), startTime, elapsedTime);
      }
    }
    for (LabelEntry label : specialLabelEntries) {
      if(label.isLogged()) {
        long startTime = label.getStartLoggingTime();
        long elapsedTime = TimeUtil.getElapsedTimeUntilNow(startTime, "second");
//        label.endLog(elapsedTime);
        label.endLog(spHandler.getSensorId(), spHandler.getTogetherStatus_Bi(), startTime, elapsedTime);
      }
    }

    Log.d(LogKeys.DEBB, "2. set sp false");
    spHandler.setSensorOn(false);
    spHandler.setAloneOn(false);
    spHandler.setTogetherOn(false);

    Log.d(LogKeys.DEBB, "3. uncheck buttons");
    aloneToggleButton.setChecked(false);
    togetherToggleButton.setChecked(false);

//    Log.d(LogKeys.DEBB, "4. unbind scdcService and bind scdcManager");
//    unbindService(scdcServiceConn);
//    stopService(new Intent(LaunchActivity.this, SCDCService.class));
//    bindService(new Intent(LaunchActivity.this, SCDCManager.class),
//            scdcManagerConn, BIND_AUTO_CREATE);


    return super.onOptionsItemSelected(item);
  }



  // methods for Setting Activity
//  @Override
//  public boolean onCreateOptionsMenu(Menu menu) {
//    // Inflate the menu; this adds items to the action bar if it is present.
//    getMenuInflater().inflate(R.menu.menu_main, menu);
//    return true;
//  }
//
//  @Override
//  public boolean onOptionsItemSelected(MenuItem item) {
//    // Handle action bar item clicks here. The action bar will
//    // automatically handle clicks on the Home/Up button, so long
//    // as you specify a parent activity in AndroidManifest.xml.
//    int id = item.getItemId();
//
//    //noinspection SimplifiableIfStatement
//    if (id == R.id.action_settings) {
//      startActivity(new Intent(this, SettingsActivity.class));
//    }
//
//    return super.onOptionsItemSelected(item);
//  }

  private boolean isNetworkConnected() {
    ConnectivityManager cm =
      (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
    return cm.getActiveNetworkInfo() != null;
  }

  public boolean changeConfig(boolean isActiveLabelOn) {
      JsonObject oldConfig;
      if (spHandler.isSensorOn()) {  // when sensor is on
        oldConfig = scdcService.getPipelineConfig(Config.PIPELINE_NAME);
      } else {  // when sensor is off
        try {
          oldConfig = scdcManager.getPipelineConfig(Config.PIPELINE_NAME);
        } catch (Exception e) {
          oldConfig = scdcService.getPipelineConfig(Config.PIPELINE_NAME);
        }
      }

      String newConfigString;

      if (isActiveLabelOn) newConfigString = spHandler.getActiveConfig();
      else newConfigString = spHandler.getIdleConfig();

//        if (newConfigString == null) newConfigString = oldConfig.toString();

      JsonObject newConfig = new JsonParser().parse(newConfigString).getAsJsonObject();
      boolean result = false;
      if (!EqualsUtil.areEqual(oldConfig, newConfig)) {
        Log.d(LogKeys.DEB, TAG + ".changeConfig/ configuration should be changed.");
        if (spHandler.isSensorOn()) {
          result = scdcService.saveAndReload(Config.PIPELINE_NAME, newConfig);
        } else {
          try {
            result = scdcManager.saveAndReload(Config.PIPELINE_NAME, newConfig);
          } catch (Exception e) {
            result = scdcService.saveAndReload(Config.PIPELINE_NAME, newConfig);
          }
        }
        if (result) {
          if (DEBUGGING){
            Toast.makeText(getBaseContext(),
                    getString(R.string.change_config_complete_message),
                    Toast.LENGTH_SHORT).show();
          }
        }
      }
      return result;
//      Log.d(LogKeys.DEBUG, TAG + ".changeConfig/ failed to change config");
//      Toast.makeText(getBaseContext(),
//              getString(R.string.change_config_failed_message),
//              Toast.LENGTH_SHORT).show();
//      return false;
  }

  // Update config for both active and idle state
  private void updateConfig() {
    new AsyncTask<Void, Void, Boolean>() {
      private HttpConfigUpdater hcu;
      private String updateActiveUrl;
      private String updateIdleUrl;
      private JsonObject oldConfig;

      @Override
      protected void onPreExecute() {
        hcu = new HttpConfigUpdater();
        if (DEBUGGING) {
          updateActiveUrl = Config.DEFAULT_UPDATE_URL_DEBUG;
          updateIdleUrl = Config.DEFAULT_UPDATE_URL_DEBUG;
        } else {
          updateActiveUrl = Config.DEFAULT_UPDATE_URL_ACTIVE;
          updateIdleUrl = Config.DEFAULT_UPDATE_URL_IDLE;
        }
        oldConfig = scdcManager.getPipelineConfig(pipeline.getName());
      }

      @Override
      protected Boolean doInBackground(Void... voids) {
        String newConfig;
        if (pipeline != null) {
          try {
            hcu.setUrl(updateActiveUrl);
            Log.d(LogKeys.DEBUG, TAG + ".updateConfig()/ url=" + updateActiveUrl);
            newConfig = hcu.getConfig().toString();
            spHandler.setActiveConfig(newConfig);
            hcu.setUrl(updateIdleUrl);
            Log.d(LogKeys.DEBUG, TAG + ".updateConfig()/ url=" + updateIdleUrl);
            newConfig = hcu.getConfig().toString();
            spHandler.setIdleConfig(newConfig);

            return true;
          } catch (ConfigUpdateException e) {
            Log.w(LogKeys.DEBUG, TAG+".updateConfig()/ Unable to get config", e);
            return false;
          }
        } else {
          Log.d(LogKeys.DEBUG, TAG+".updateConfig/ failed to update config");
          return false;
        }
      }

      @Override
      protected void onPostExecute(Boolean isSuccess) {
        if (isSuccess) {
          if (DEBUGGING){
            Toast.makeText(getBaseContext(),
                    getString(R.string.update_config_complete_message),
                    Toast.LENGTH_LONG).show();
            spHandler.setLastConfigUpdate(System.currentTimeMillis());
          }
        } else {
          Toast.makeText(getBaseContext(),
                  getString(R.string.update_config_failed_message),
                  Toast.LENGTH_LONG).show();
        }
      }
    }.execute();
  }

  public void updateLaunchActivityUi() {
    new AsyncTask<Void, Void, Boolean>() {

      @Override
      protected Boolean doInBackground(Void... voids) {
        publishProgress(voids);

        return true;
      }

      @Override
      protected void onProgressUpdate(Void... voids) {
        /**
         * Queries the database of the pipeline to determine
         * how many rows of data we have recorded so far.
         */
        if (pipeline != null) {
          if (pipeline.getDatabaseHelper() == null) {
            pipeline.reloadDbHelper(scdcManager);
          }

          if (pipeline.getDatabaseHelper() != null) {
            // Query the pipeline db for the count of rows in the data table
            SQLiteDatabase db = pipeline.getDb();
            final long dbSize = new File(db.getPath()).length();  // in bytes
            double dbSizeDouble = Math.round((dbSize / (1048576.0)) * 10.0) / 10.0;
            dataCountView.setText("Data size: " +
                    dbSizeDouble + " MB");
            spHandler.setTooMuchData(dbSizeDouble>SCDCKeys.Data.MAX_DATA);

//            dataCountView.setText("Data size: " +
//                    Math.round((dbSize / (1048576.0)) * 10.0) / 10.0 + " MB");
          }
        } else if (scdcService != null) {
          long dbSize = scdcService.getDBSize();
          double dbSizeDouble = Math.round((dbSize / (1048576.0)) * 10.0) / 10.0;
          dataCountView.setText("Data size: " +
                  dbSizeDouble + " MB");
          spHandler.setTooMuchData(dbSizeDouble>SCDCKeys.Data.MAX_DATA);

//          dataCountView.setText("Data size: " +
//                  Math.round((dbSize / (1048576.0)) * 10.0) / 10.0 + " MB");
        }

        /**
         * Temporarily turns on the receiving_data_iv for 3 seconds.
         */
        receivingDataImageView.setVisibility(View.VISIBLE);

        // Turn off iv after 3 seconds
        handler.postDelayed(new Runnable() {
          @Override
          public void run() {
            receivingDataImageView.setVisibility(View.INVISIBLE);
          }
        }, 3000);
      }
    }.execute();
  }

  // fixed height for GridViews
  public void setGridViewHeightBasedOnChildren(GridView gridView, int columns) {
//    Log.d(LogKeys.DEB, TAG+".setGridViewHeightBasedOnChildren()");
    ListAdapter listAdapter = gridView.getAdapter();
    if (listAdapter == null) {
      // pre-condition
      return;
    }

    int totalHeight = 272;
    int items = listAdapter.getCount();
    int rows = 0;

    for (int i=0; i<items; i++){
      View listItem = listAdapter.getView(i, null, gridView);
      listItem.measure(0, 0);
      totalHeight = listItem.getMeasuredHeight();
//      Log.d(LogKeys.DEB, TAG+".totalHeight: " +totalHeight);

      float x = 1;
      if( items > columns ){
        if(items%columns != 0){
          x = items/columns + 1;
        }
        else{
          x = items/columns;
        }

        rows = (int) (x);
        totalHeight *= rows;
      }

      ViewGroup.LayoutParams params = gridView.getLayoutParams();
      params.height = totalHeight;
      gridView.setLayoutParams(params);
    }
  }
}
