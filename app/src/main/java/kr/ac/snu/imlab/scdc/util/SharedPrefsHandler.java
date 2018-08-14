package kr.ac.snu.imlab.scdc.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.provider.Settings.Secure;
import android.util.Log;
import android.widget.EditText;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import edu.mit.media.funf.config.HttpConfigUpdater;
import kr.ac.snu.imlab.scdc.activity.LaunchActivity;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys.LabelKeys;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys.LogKeys;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys.Config;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys.SharedPrefs;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys.AlarmKeys;
import kr.ac.snu.imlab.scdc.R;

/**
 * Created by kilho on 15. 8. 5.
 */
public class SharedPrefsHandler {

  protected static final String TAG = "SharedPrefsHandler";

  private Context context;
  private SharedPreferences prefs;
  private static SharedPrefsHandler instance;
  private String deviceId;
  private String userinfoUrl;
  private static boolean firstrun = true;
  private long startLoggingTime;

  private SharedPrefsHandler() {
  }

  private SharedPrefsHandler(Context context, String name, int mode) {
    this.context = context;
    this.prefs = context.getSharedPreferences(name, mode);
    this.deviceId = Secure.getString(this.context.getContentResolver(),
            Secure.ANDROID_ID);
    this.userinfoUrl = Config.DEFAULT_USERINFO_URL;
//    Log.d(LogKeys.DEBUG,
//      "SharedPrefsHandler.SharedPrefsHandler(): deviceId=" + this.deviceId);
    firstrun = prefs.getBoolean(SharedPrefs.FIRST_RUN, true);
    if (firstrun && context instanceof LaunchActivity) {
      try {
        new GetPrefsFromServerTask().execute(userinfoUrl + deviceId + "/");
      } catch (Exception e) {
        Log.e(LogKeys.DEBUG, "SharedPrefsHandler.SharedPrefsHandler(): error=", e);
      }
    }
  }

  public static synchronized SharedPrefsHandler
    getInstance(Context context, String name, int mode) {
    instance = new SharedPrefsHandler(context, name, mode);
    return instance;
  }


  public boolean isTooMuchData(){
    return prefs.getBoolean(SharedPrefs.TOO_MUCH_DATA, false);
  }
  public boolean setTooMuchData(boolean isTooMuchData){
    prefs.edit().putBoolean(SharedPrefs.TOO_MUCH_DATA, isTooMuchData).apply();
    return true;
  }

  // Funf sensor by each label & alone/together
  public boolean isSensorOn() {
//    Log.d(LogKeys.DEBUG, "SharedPrefsHandler.isSensorOn(): called");
    return prefs.getBoolean(SharedPrefs.SENSOR_ON, Config.DEFAULT_SENSOR_ON);
  }
  public boolean setSensorOn(boolean isSensorOn) {
    prefs.edit().putBoolean(SharedPrefs.SENSOR_ON, isSensorOn).apply();
    return true;
  }
  public boolean isAloneOn() {
    return prefs.getBoolean(SharedPrefs.ALONE_ON, Config.DEFAULT_ALONE_ON);
  }
  public boolean setAloneOn(boolean isAloneOn) {
    prefs.edit().putBoolean(SharedPrefs.ALONE_ON, isAloneOn).apply();
    return true;
  }

  public boolean isTogetherOn() {
    return prefs.getBoolean(SharedPrefs.TOGETHER_ON, Config.DEFAULT_TOGETHER_ON);
  }
  public boolean setTogetherOn(boolean isTogetherOn) {
    prefs.edit().putBoolean(SharedPrefs.TOGETHER_ON, isTogetherOn).apply();
    return true;
  }


  // User info
  public String getUsername() {
    Log.d(LogKeys.DEBUG, "SharedPrefsHandler.getUsername(): firstrun=" + firstrun);
    return prefs.getString(SharedPrefs.USERNAME, Config.DEFAULT_USERNAME);
  }

  public void setUsername(String username) {
    prefs.edit().putString(SharedPrefs.USERNAME, username).apply();
  }

  public boolean getIsFemale() {
    Log.d(LogKeys.DEBUG, "SharedPrefsHandler.getIsFemale(): firstrun=" + firstrun);
    return prefs.getBoolean(SharedPrefs.IS_FEMALE, Config.DEFAULT_IS_FEMALE);
  }

  public void setIsFemale(boolean isFemale) {
    prefs.edit().putBoolean(SharedPrefs.IS_FEMALE, isFemale).apply();
  }

  // Id's related to data collection
  // Methods to track expId's for each data emission from Probe
  public int getExpId(String probeConfig) {
    return prefs.getInt(SharedPrefs.LABEL_EXP_ID_PREFIX + probeConfig, 0);
  }

  public void setExpId(String componentString, int expId) {
    prefs.edit().putInt(SharedPrefs.LABEL_EXP_ID_PREFIX +
            componentString, expId).apply();
  }

  // Methods to track sensorId's for each sensor switch off-->on
  public int getSensorId() {
    return prefs.getInt(SharedPrefs.KEY_SENSOR_ID, SharedPrefs.DEFAULT_SENSOR_ID);
  }

  public void setSensorId(int sensorId) {
    prefs.edit().putInt(SharedPrefs.KEY_SENSOR_ID, sensorId).apply();
  }

  // Methods to track sensorId's to remove
  public String getSensorIdsInData() {
    return prefs.getString(SharedPrefs.SENSOR_ID_IN_DATA, "");
  }

  public String getSensorIdsToRemove() {
    return prefs.getString(SharedPrefs.SENSOR_ID_TO_REMOVE, "");
  }

  public void insertSensorIdToRemove(int sensorId) {
    String tempIdsToRemove = getSensorIdsToRemove();
    if (tempIdsToRemove.equals("")) {
      prefs.edit().putString(SharedPrefs.SENSOR_ID_TO_REMOVE, "" + sensorId).apply();
    } else {
      prefs.edit().putString(SharedPrefs.SENSOR_ID_TO_REMOVE, tempIdsToRemove + "," + sensorId).apply();
    }
  }

  public void popSensorIdToRemove(int sensorId) {
    String sidToRemove = "" + sensorId;
    String tempIdsToRemove = getSensorIdsToRemove();
    String updateStr = "";
    if (tempIdsToRemove.contains(",")) {
      String[] tempIdsToRemoveArr = tempIdsToRemove.split(",");
      for (int i=0; i<tempIdsToRemoveArr.length; i++) {
        String sid = tempIdsToRemoveArr[i];
        if (sid.equals(sidToRemove)) continue;
        if (i > 0) updateStr += ",";
        updateStr += sid;
      }
    } else {
      if (tempIdsToRemove.equals(sidToRemove)) updateStr = "";
    }
    prefs.edit().putString(SharedPrefs.SENSOR_ID_TO_REMOVE, updateStr).apply();
  }

  public void initializeSensorIdToRemove() {
    prefs.edit().putString(SharedPrefs.SENSOR_ID_TO_REMOVE, "").apply();
  }

  public boolean getSensorIdsRemoveOrNot() {
    return prefs.getBoolean(SharedPrefs.SENSOR_ID_REMOVE_OR_NOT, false);
  }

  public void setSensorIdsRemoveOrNot(boolean removeOrNot) {
    prefs.edit().putBoolean(SharedPrefs.SENSOR_ID_REMOVE_OR_NOT, removeOrNot).apply();
  }

  public void initializeDataFixing() {
//    initializeSensorIdsInData();
    initializeSensorIdToRemove();
    setSensorIdsRemoveOrNot(false);
  }

  // Synchronize preferences with server
  // IMPORTANT: This method is only executed while uploading
  public boolean setPrefsToServer()
          throws ExecutionException, InterruptedException {
    return new SetPrefsToServerTask().execute(userinfoUrl + deviceId + "/").get();
  }

  // Labels
  public int getNumLabels() {
    return prefs.getInt(SharedPrefs.NUM_LABELS, 1);
  }

  public void setNumLabels(int size) {
    prefs.edit().putInt(SharedPrefs.NUM_LABELS, size).apply();
  }


  public String getLabelName(int labelId) {
    return prefs.getString(SharedPrefs.LABEL_NAME_PREFIX +
            String.valueOf(labelId), null);
  }

  public void setLabelName(int labelId, String labelName) {
    prefs.edit().putString(SharedPrefs.LABEL_NAME_PREFIX +
      String.valueOf(labelId), labelName).apply();
  }

  public long getStartLoggingTime(int labelId) {
    return prefs.getLong(SharedPrefs.LABEL_START_LOGGING_TIME_PREFIX +
            String.valueOf(labelId), -1);
  }

  public void setStartLoggingTime(int labelId, long startLoggingTime) {
    prefs.edit().putLong(SharedPrefs.LABEL_START_LOGGING_TIME_PREFIX +
            String.valueOf(labelId), startLoggingTime).apply();
  }



  public void setStartLoggingTime(long startLoggingTime) {
    prefs.edit().putLong(SharedPrefs.LABEL_START_LOGGING_TIME_PREFIX, startLoggingTime).apply();
  }

  public long getStartLoggingTime(){
    return prefs.getLong(SharedPrefs.LABEL_START_LOGGING_TIME_PREFIX ,-1);
  }



  public long getAccumulatedTime(int labelId){
    return prefs.getLong(SharedPrefs.LABEL_ACCUMULATED_TIME_PREFIX+String.valueOf(labelId), 0);
  }

  public void setAccumulatedTime(int labelId, long elapsedTime){
    long accum = getAccumulatedTime(labelId) + elapsedTime;
    Log.d(LogKeys.DEBB, TAG+": accumulated time is "+String.valueOf(accum));
    prefs.edit().putLong(SharedPrefs.LABEL_ACCUMULATED_TIME_PREFIX+String.valueOf(labelId), accum).apply();
  }

  public String getLabelSensingTimeInfo(){
    long sleepingTime = 0L;
    long eatingTime = 0L;
    long inClassTime = 0L;
    long studyingTime = 0L;
    long drinkingTime = 0L;
    long movingTime = 0L;
    long etcTime = 0L;

    String totalString = getTotalSensingTimeInfo();
    if (!totalString.equals("")) {
      String[] sensingTimeArr = totalString.split("/");
      for (String tempSensingInfo : sensingTimeArr) {
        String[] tempInfoArr = tempSensingInfo.split(",");
        if (tempInfoArr[2].equals("0")) sleepingTime += Long.parseLong(tempInfoArr[4]);
        if (tempInfoArr[2].equals("1")) eatingTime += Long.parseLong(tempInfoArr[4]);
        if (tempInfoArr[2].equals("2")) drinkingTime += Long.parseLong(tempInfoArr[4]);
        if (tempInfoArr[2].equals("3")) inClassTime += Long.parseLong(tempInfoArr[4]);
        if (tempInfoArr[2].equals("4")) studyingTime += Long.parseLong(tempInfoArr[4]);
        if (tempInfoArr[2].equals("5")) movingTime += Long.parseLong(tempInfoArr[4]);
        if (tempInfoArr[2].equals("6")) etcTime += Long.parseLong(tempInfoArr[4]);
      }
    }
    return "SL"+sleepingTime
            +"-EA"+ eatingTime
            +"-DR"+ drinkingTime
            +"-IC"+ inClassTime
            +"-ST"+ studyingTime
            +"-MV"+ movingTime
            +"-NO"+ etcTime;
  }

  public String getTotalSensingTimeInfo(){
    return prefs.getString(SharedPrefs.SENSOR_ID_LABELING_TIME, "");
  }

  public void insertSensingTimeInfo(int sensorId, boolean isTogether, int labelId, long startTime, long elapsedTime){
    String togetherStatus = "";
    if (isTogether) {
      togetherStatus += 1;
    } else {
      togetherStatus += 0;
    }
    String sensingTimeInfo = "" + sensorId + "," + togetherStatus + "," + labelId + "," + startTime + "," + elapsedTime;
    Log.d(LogKeys.DEBB, TAG+": insertSensingTimeInfo(): sensorId labeling time is "+ sensingTimeInfo);

    String totalSensingTimeInfo = getTotalSensingTimeInfo();
    if (totalSensingTimeInfo.equals("")) {
      prefs.edit().putString(SharedPrefs.SENSOR_ID_LABELING_TIME, sensingTimeInfo).apply();
    } else {
      prefs.edit().putString(SharedPrefs.SENSOR_ID_LABELING_TIME, totalSensingTimeInfo + "/" + sensingTimeInfo).apply();
    }
  }

  public void popSensingTimeInfo(int sensorId){
    String sid = "" + sensorId;
    String timeStr = getTotalSensingTimeInfo();
    if (!timeStr.equals("")) {
      String[] timeArr = timeStr.split("/");
      String returnString = "";
      for (String tempSensingTimeInfo : timeArr) {
        String[] tempInfo = tempSensingTimeInfo.split(",");
        if (sid.equals(tempInfo[0])) {
          Log.d(LogKeys.DEBB, TAG + ": popSensingTimeInfo(): sensorId labeling time info : " + tempSensingTimeInfo);
        } else {
          if (!returnString.equals("")) returnString += "/";
          returnString += tempSensingTimeInfo;
        }
      }
      prefs.edit().putString(SharedPrefs.SENSOR_ID_LABELING_TIME, returnString).apply();
    }
  }

  public void resetSensingTimeInfo(){
    prefs.edit().putString(SharedPrefs.SENSOR_ID_LABELING_TIME, "").apply();
  }

  // Alone/Together status (trinary value)
  public int getTogetherStatus_Tri() {
    if(isAloneOn()) return 0;
    else if(isTogetherOn()) return 1;
    else return -1;
  }

  // Alone/Together status (binary value)
  public Boolean getTogetherStatus_Bi() {
    if(isAloneOn()) return false;
    else if(isTogetherOn()) return true;
    else return null;
  }

  // AccompanyingStatusLabel
  public int getAccompanyingStatus(int labelId) {
    return prefs.getInt(SharedPrefs.LABEL_ACCOMPANYING_STATUS_PREFIX +
            String.valueOf(labelId), LabelKeys.ACCOMPANYING_STATUS_NONE);
  }

  // AccompanyingStatusLabel
  public void setAccompanyingStatus(int labelId, int accompanyingStatusId) {
    prefs.edit().putInt(SharedPrefs.LABEL_ACCOMPANYING_STATUS_PREFIX +
            String.valueOf(labelId), accompanyingStatusId).apply();
  }

  // ConversingStatusLabel
  public int getConversingStatus(int labelId) {
    return prefs.getInt(SharedPrefs.LABEL_CONVERSING_STATUS_PREFIX +
            String.valueOf(labelId), LabelKeys.CONVERSING_STATUS_NONE);
  }

  // ConversingStatusLabel
  public void setConversingStatus(int labelId, int conversingStatusId) {
    prefs.edit().putInt(SharedPrefs.LABEL_CONVERSING_STATUS_PREFIX +
            String.valueOf(labelId), conversingStatusId).apply();
  }

  public boolean getIsLogged(int labelId) {
    if (getStartLoggingTime(labelId) == -1) {
      return false;
    } else {
      return true;
    }
  }

  // return true if at least one of active labels is on
  public boolean isActiveLabelOn() {
    String[] normalLabelNames = LaunchActivity.normalLabelNames;
    String[] specialLabelNames = LaunchActivity.specialLabelNames;
    String[] activeLabelNames = LaunchActivity.activeLabelNames;

    for (int i = 0; i < normalLabelNames.length; i++) {
      // if current normal label is logged,
      // check if it is included in active labels
      if (getStartLoggingTime(i) != -1) {
        for (int j = 0; j < activeLabelNames.length; j++) {
          if (getLabelName(i).equals(activeLabelNames[j])) {
            return true;
          }
        }
      }
    }

    for (int i = 0; i < specialLabelNames.length; i++) {
      // if current special label is logged,
      // check if it is included in active labels
      if (getStartLoggingTime(1000+i) != -1) {
        for (int j = 0; j < activeLabelNames.length; j++) {
          if (getLabelName(1000+i).equals(activeLabelNames[j])) {
            return true;
          }
        }
      }
    }
    return false;
  }

  // Probe Config - active / idle
  public String getActiveConfig() {
    return prefs.getString(SharedPrefs.ACTIVE_CONFIG, null);
  }

  public void setActiveConfig(String config) {
    prefs.edit().putString(SharedPrefs.ACTIVE_CONFIG, config).apply();
  }

  public String getIdleConfig() {
    return prefs.getString(SharedPrefs.IDLE_CONFIG, null);
  }

  public void setIdleConfig(String config) {
    prefs.edit().putString(SharedPrefs.IDLE_CONFIG, config).apply();
  }

  public long getLastConfigUpdate() {
    return prefs.getLong(SharedPrefs.LAST_CONFIG_UPDATE, SharedPrefs.DEFAULT_LAST_CONFIG_UPDATE);
  }

  public void setLastConfigUpdate(long updateTime) {
    prefs.edit().putLong(SharedPrefs.LAST_CONFIG_UPDATE, updateTime).apply();
  }


  // Alarm - General
  public int getGeneralRepeatType() {
    return prefs.getInt(SharedPrefs.GENERAL_REPEAT_TYPE,
            Integer.parseInt(AlarmKeys.DEFAULT_REPEAT_TYPE));
  }

  public void setGeneralRepeatType(int repeatType) {
    if (repeatType >= 0 && repeatType <= 5) {
      prefs.edit().putInt(SharedPrefs.GENERAL_REPEAT_TYPE,
                          repeatType).apply();
    }
  }

  public int getGeneralRepeatInterval() {
    return prefs.getInt(SharedPrefs.GENERAL_REPEAT_INTERVAL,
            Integer.parseInt(AlarmKeys.DEFAULT_GENERAL_ALARM_REPEAT_INTERVAL));
  }

  public void setGeneralRepeatInterval(int repeatInterval) {
    prefs.edit().putInt(SharedPrefs.GENERAL_REPEAT_INTERVAL,
                        repeatInterval).apply();
  }


  // Alarm - Labels
  public boolean getIsCompleted(int labelId) {
    return prefs.getBoolean(SharedPrefs.LABEL_IS_COMPLETED_PREFIX +
            String.valueOf(labelId), true);
  }

  public void setIsCompleted(int labelId, boolean isCompleted) {
    prefs.edit().putBoolean(SharedPrefs.LABEL_IS_COMPLETED_PREFIX +
                            String.valueOf(labelId), isCompleted).apply();
  }

  public void toggleIsCompleted(int labelId) {
    boolean isCompleted = getIsCompleted(labelId) ? false : true;
    setIsCompleted(labelId, isCompleted);
  }

  public boolean getHasDateDue(int labelId) {
    return prefs.getBoolean(SharedPrefs.LABEL_HAS_DATE_DUE_PREFIX +
                            String.valueOf(labelId), false);
  }

  public void setHasDateDue(int labelId, boolean hasDateDue) {
    prefs.edit().putBoolean(SharedPrefs.LABEL_HAS_DATE_DUE_PREFIX +
                            String.valueOf(labelId), hasDateDue).apply();
  }

  public boolean getHasFinalDateDue(int labelId) {
    return prefs.getBoolean(SharedPrefs.LABEL_HAS_FINAL_DATE_DUE +
            String.valueOf(labelId), false);
  }

  public void setHasFinalDateDue(int labelId, boolean hasFinalDateDue) {
    prefs.edit().putBoolean(SharedPrefs.LABEL_HAS_FINAL_DATE_DUE +
            String.valueOf(labelId), hasFinalDateDue).apply();
  }

  // for repeating alarm
  public boolean getIsRepeating(int labelId) {
    return prefs.getBoolean(SharedPrefs.LABEL_IS_REPEATING +
                            String.valueOf(labelId), true);
  }

  public void setIsRepeating(int labelId, boolean isRepeating) {
    prefs.edit().putBoolean(SharedPrefs.LABEL_IS_REPEATING +
                            String.valueOf(labelId), isRepeating).apply();
  }

  public int getRepeatType(int labelId) {
    return prefs.getInt(SharedPrefs.LABEL_REPEAT_TYPE_PREFIX +
                    String.valueOf(labelId),
            Integer.parseInt(AlarmKeys.DEFAULT_REPEAT_TYPE));
  }

  public void setRepeatType(int labelId, int repeatType) {
    if (repeatType >= 0 && repeatType <= 5) {
      setIsRepeating(labelId, true);
      prefs.edit().putInt(SharedPrefs.LABEL_REPEAT_TYPE_PREFIX +
              String.valueOf(labelId), repeatType).apply();
    }
  }

  public int getRepeatInterval(int labelId) {
    return prefs.getInt(SharedPrefs.LABEL_REPEAT_INTERVAL_PREFIX +
                    String.valueOf(labelId),
            Integer.parseInt(AlarmKeys.DEFAULT_REPEAT_INTERVAL));
  }

  public void setRepeatInterval(int labelId, int repeatInterval) {
    prefs.edit().putInt(SharedPrefs.LABEL_REPEAT_INTERVAL_PREFIX +
            String.valueOf(labelId), repeatInterval).apply();
//    Log.d(SCDCKeys.LogKeys.DEBUG, "SharedPrefsHandler.setRepeatInterval(" +
//            labelId + ", " + repeatInterval + ")");
//    Log.d(SCDCKeys.LogKeys.DEBUG, "SharedPrefsHandler.getRepeatInterval(" +
//            labelId + ")=" + getRepeatInterval(labelId));
  }

  // FIXME:
  public long getDateDue(int labelId) {
    return prefs.getLong(SharedPrefs.LABEL_DATE_DUE_PREFIX +
                    String.valueOf(labelId),
            Long.parseLong(AlarmKeys.DEFAULT_DATE_DUE));
  }

  public void setDateDue(int labelId, long dateDue) {
    prefs.edit().putLong(SharedPrefs.LABEL_DATE_DUE_PREFIX +
                         String.valueOf(labelId), dateDue).apply();
  }

  public boolean getIsPastDue(int labelId) {
    if (!getHasDateDue(labelId) || getIsCompleted(labelId))
      return false;

    return getDateDue(labelId) - System.currentTimeMillis() < 0;
  }

  // for procrastinator alarm
  public String getAlarmTime() {
    return prefs.getString(SharedPrefs.ALARM_TIME,
                           AlarmKeys.DEFAULT_ALARM_TIME);
  }

  // for reminder
  public String getReminderTime() {
    return prefs.getString(SharedPrefs.REMINDER_TIME,
            AlarmKeys.DEFAULT_REMINDER_TIME);
  }

  // for notification
  public boolean getVibrateOnAlarm() {
    return prefs.getBoolean(SharedPrefs.VIBRATE_ON_ALARM, true);
  }

  // for time settings
  public String getDefaultHour() {
    return prefs.getString(SharedPrefs.DEFAULT_HOUR,
            AlarmKeys.DEFAULT_HOUR_VALUE);
  }

  // Save reminderToggleButton state
  public boolean isReminderRunning() {
    return prefs.getBoolean(SharedPrefs.IS_REMINDER_RUNNING, false);
  }

  public void setReminderRunning(boolean isRunning) {
    prefs.edit().putBoolean(SharedPrefs.IS_REMINDER_RUNNING,
                            isRunning).apply();
  }

  public void saveTempLastIndex(){
    long accountTT = prefs.getLong(SCDCKeys.SharedPrefs.ACCOUNTS_COLLECT_TEMP_LAST_TIME, 0L);
    long androidTT = prefs.getLong(SCDCKeys.SharedPrefs.ANDROID_INFO_COLLECT_TEMP_LAST_TIME, 0L);
    long appTT = prefs.getLong(SCDCKeys.SharedPrefs.APPLICATIONS_COLLECT_TEMP_LAST_TIME, 0L);
    long hardwareTT = prefs.getLong(SCDCKeys.SharedPrefs.HARDWARE_INFO_COLLECT_TEMP_LAST_TIME, 0L);
    long callTT = prefs.getLong(SCDCKeys.SharedPrefs.CALL_COLLECT_TEMP_LAST_TIME, 0L);
    long audioTT = prefs.getLong(SCDCKeys.SharedPrefs.AUDIO_COLLECT_TEMP_LAST_TIME, 0L);
    long bookmarkTT = prefs.getLong(SCDCKeys.SharedPrefs.BOOKMARK_COLLECT_TEMP_LAST_TIME, 0L);
    long searchTT = prefs.getLong(SCDCKeys.SharedPrefs.SEARCH_COLLECT_TEMP_LAST_TIME, 0L);
    long imageTT = prefs.getLong(SCDCKeys.SharedPrefs.IMAGE_COLLECT_TEMP_LAST_TIME, 0L);
    long smsTT = prefs.getLong(SCDCKeys.SharedPrefs.SMS_COLLECT_TEMP_LAST_TIME, 0L);
    long videoTT = prefs.getLong(SCDCKeys.SharedPrefs.VIDEO_COLLECT_TEMP_LAST_TIME, 0L);
    long contactTT = prefs.getLong(SCDCKeys.SharedPrefs.CONTACT_COLLECT_TEMP_LAST_TIME, 0L);

    long callTID = prefs.getLong(SCDCKeys.SharedPrefs.CALL_LOG_TEMP_LAST_INDEX, 0L);
    long audioTID = prefs.getLong(SCDCKeys.SharedPrefs.AUDIO_LOG_TEMP_LAST_INDEX, 0L);
    long bookmarkTID = prefs.getLong(SCDCKeys.SharedPrefs.BOOKMARK_LOG_TEMP_LAST_INDEX, 0L);
    long searchTID = prefs.getLong(SCDCKeys.SharedPrefs.SEARCH_LOG_TEMP_LAST_INDEX, 0L);
    long imageTID = prefs.getLong(SCDCKeys.SharedPrefs.IMAGE_LOG_TEMP_LAST_INDEX, 0L);
    long smsTID = prefs.getLong(SCDCKeys.SharedPrefs.SMS_LOG_TEMP_LAST_INDEX, 0L);
    long videoTID = prefs.getLong(SCDCKeys.SharedPrefs.VIDEO_LOG_TEMP_LAST_INDEX, 0L);
    long contactTID = prefs.getLong(SCDCKeys.SharedPrefs.CONTACT_LOG_TEMP_LAST_INDEX, 0L);

    if (accountTT > 0L) prefs.edit().putLong(SCDCKeys.SharedPrefs.ACCOUNTS_COLLECT_LAST_TIME, accountTT).apply();
    if (androidTT > 0L) prefs.edit().putLong(SCDCKeys.SharedPrefs.ANDROID_INFO_COLLECT_LAST_TIME, androidTT).apply();
    if (appTT > 0L) prefs.edit().putLong(SCDCKeys.SharedPrefs.APPLICATIONS_COLLECT_LAST_TIME, appTT).apply();
    if (hardwareTT > 0L) prefs.edit().putLong(SCDCKeys.SharedPrefs.HARDWARE_INFO_COLLECT_LAST_TIME, hardwareTT).apply();
    if (callTT > 0L) prefs.edit().putLong(SCDCKeys.SharedPrefs.CALL_COLLECT_LAST_TIME, callTT).apply();
    if (audioTT > 0L) prefs.edit().putLong(SCDCKeys.SharedPrefs.AUDIO_COLLECT_LAST_TIME, audioTT).apply();
    if (bookmarkTT > 0L) prefs.edit().putLong(SCDCKeys.SharedPrefs.BOOKMARK_COLLECT_LAST_TIME, bookmarkTT).apply();
    if (searchTT > 0L) prefs.edit().putLong(SCDCKeys.SharedPrefs.SEARCH_COLLECT_LAST_TIME, searchTT).apply();
    if (imageTT > 0L) prefs.edit().putLong(SCDCKeys.SharedPrefs.IMAGE_COLLECT_LAST_TIME, imageTT).apply();
    if (smsTT > 0L) prefs.edit().putLong(SCDCKeys.SharedPrefs.SMS_COLLECT_LAST_TIME, smsTT).apply();
    if (videoTT > 0L) prefs.edit().putLong(SCDCKeys.SharedPrefs.VIDEO_COLLECT_LAST_TIME, videoTT).apply();
    if (contactTT > 0L) prefs.edit().putLong(SCDCKeys.SharedPrefs.CONTACT_COLLECT_LAST_TIME, contactTT).apply();

    if (callTID > 0L) prefs.edit().putLong(SCDCKeys.SharedPrefs.CALL_LOG_LAST_INDEX, callTID).apply();
    if (audioTID > 0L) prefs.edit().putLong(SCDCKeys.SharedPrefs.AUDIO_LOG_LAST_INDEX, audioTID).apply();
    if (bookmarkTID > 0L) prefs.edit().putLong(SCDCKeys.SharedPrefs.BOOKMARK_LOG_LAST_INDEX, bookmarkTID).apply();
    if (searchTID > 0L) prefs.edit().putLong(SCDCKeys.SharedPrefs.SEARCH_LOG_LAST_INDEX, searchTID).apply();
    if (imageTID > 0L) prefs.edit().putLong(SCDCKeys.SharedPrefs.IMAGE_LOG_LAST_INDEX, imageTID).apply();
    if (smsTID > 0L) prefs.edit().putLong(SCDCKeys.SharedPrefs.SMS_LOG_LAST_INDEX, smsTID).apply();
    if (videoTID > 0L) prefs.edit().putLong(SCDCKeys.SharedPrefs.VIDEO_LOG_LAST_INDEX, videoTID).apply();
    if (contactTID > 0L) prefs.edit().putLong(SCDCKeys.SharedPrefs.CONTACT_LOG_LAST_INDEX, contactTID).apply();
  }

  public void rollbackTempLastIndex(){
    prefs.edit().remove(SCDCKeys.SharedPrefs.ACCOUNTS_COLLECT_TEMP_LAST_TIME).apply();
    prefs.edit().remove(SCDCKeys.SharedPrefs.ANDROID_INFO_COLLECT_TEMP_LAST_TIME).apply();
    prefs.edit().remove(SCDCKeys.SharedPrefs.APPLICATIONS_COLLECT_TEMP_LAST_TIME).apply();
    prefs.edit().remove(SCDCKeys.SharedPrefs.HARDWARE_INFO_COLLECT_TEMP_LAST_TIME).apply();
    prefs.edit().remove(SCDCKeys.SharedPrefs.CALL_COLLECT_TEMP_LAST_TIME).apply();
    prefs.edit().remove(SCDCKeys.SharedPrefs.AUDIO_COLLECT_TEMP_LAST_TIME).apply();
    prefs.edit().remove(SCDCKeys.SharedPrefs.BOOKMARK_COLLECT_TEMP_LAST_TIME).apply();
    prefs.edit().remove(SCDCKeys.SharedPrefs.SEARCH_COLLECT_TEMP_LAST_TIME).apply();
    prefs.edit().remove(SCDCKeys.SharedPrefs.IMAGE_COLLECT_TEMP_LAST_TIME).apply();
    prefs.edit().remove(SCDCKeys.SharedPrefs.SMS_COLLECT_TEMP_LAST_TIME).apply();
    prefs.edit().remove(SCDCKeys.SharedPrefs.VIDEO_COLLECT_TEMP_LAST_TIME).apply();
    prefs.edit().remove(SCDCKeys.SharedPrefs.CONTACT_COLLECT_TEMP_LAST_TIME).apply();

    prefs.edit().remove(SCDCKeys.SharedPrefs.CALL_LOG_TEMP_LAST_INDEX).apply();
    prefs.edit().remove(SCDCKeys.SharedPrefs.AUDIO_LOG_TEMP_LAST_INDEX).apply();
    prefs.edit().remove(SCDCKeys.SharedPrefs.BOOKMARK_LOG_TEMP_LAST_INDEX).apply();
    prefs.edit().remove(SCDCKeys.SharedPrefs.SEARCH_LOG_TEMP_LAST_INDEX).apply();
    prefs.edit().remove(SCDCKeys.SharedPrefs.IMAGE_LOG_TEMP_LAST_INDEX).apply();
    prefs.edit().remove(SCDCKeys.SharedPrefs.SMS_LOG_TEMP_LAST_INDEX).apply();
    prefs.edit().remove(SCDCKeys.SharedPrefs.VIDEO_LOG_TEMP_LAST_INDEX).apply();
    prefs.edit().remove(SCDCKeys.SharedPrefs.CONTACT_LOG_TEMP_LAST_INDEX).apply();
  }

  public void setLastSavedIndex(String key, long lastTime){
    prefs.edit().putLong(key, lastTime).apply();
  }

  public long getLastSavedIndex(String key){
    return prefs.getLong(key, 0L);
  }

  // Last saved id for contact probe
  public void setCPLastSavedId(String key, int id){
    prefs.edit().putInt(key, id).apply();
  }

  public int getCPLastSavedId(String key){
    return prefs.getInt(key, 0);
  }

  private class GetPrefsFromServerTask extends AsyncTask<String, Void, Boolean> {

    private ProgressDialog progressDialog;

    @Override
    protected void onPreExecute() {
//      progressDialog = new ProgressDialog(context);
//      progressDialog.setMessage(context.getString(R.string.get_prefs_from_server_message));
//      progressDialog.setCancelable(false);
//      progressDialog.show();
    }

    @Override
    protected Boolean doInBackground(String... urls) {
      try {
        String response = HttpUtil.sendGet(urls[0]);
        Log.d(LogKeys.DEBUG, "SharedPrefsHandler.GetPrefsFromServerTask" +
                              ".doInBackground(): response=" + response);
        JsonObject userInfo = new JsonParser().parse(response).getAsJsonObject();
        String newUsername = userInfo.get(SharedPrefs.USERNAME).getAsString();
        int newSensorId = userInfo.get(SharedPrefs.KEY_SENSOR_ID).getAsInt();

        String currUsername = prefs.getString(SharedPrefs.USERNAME,
                                              Config.DEFAULT_USERNAME);
        int currSensorId = prefs.getInt(SharedPrefs.KEY_SENSOR_ID, 0);

        if (!currUsername.equals(newUsername))
          prefs.edit().putString(SharedPrefs.USERNAME, newUsername).apply();
        if (currSensorId != newSensorId)
          prefs.edit().putInt(SharedPrefs.KEY_SENSOR_ID, newSensorId).apply();

//        // Get pipeline config from server for both active and idle state
//        HttpConfigUpdater hcu = new HttpConfigUpdater();
//        String updateActiveUrl, updateIdleUrl;
//        String newConfig;
//        if (LaunchActivity.DEBUGGING) {
//          updateActiveUrl = Config.DEFAULT_UPDATE_URL_DEBUG;
//          updateIdleUrl = Config.DEFAULT_UPDATE_URL_DEBUG;
//        } else {
//          updateActiveUrl = Config.DEFAULT_UPDATE_URL_ACTIVE;
//          updateIdleUrl = Config.DEFAULT_UPDATE_URL_IDLE;
//        }
//        if (getActiveConfig() == null) {
//          hcu.setUrl(updateActiveUrl);
//          Log.d(LogKeys.DEBUG,
//            TAG+".GetPrefsFromServerTask.doInBackground()/ updateActiveUrl=" + updateActiveUrl);
//          newConfig = hcu.getConfig().toString();
//          setActiveConfig(newConfig);
//          Log.d(LogKeys.DEBUG,
//                  TAG+".GetPrefsFromServerTask.doInBackground()/ newConfig=" + newConfig);
//        }
//        if (getIdleConfig() == null) {
//          hcu.setUrl(updateIdleUrl);
//          Log.d(LogKeys.DEBUG,
//                  TAG+".GetPrefsFromServerTask.doInBackground()/ updateIdleUrl=" + updateIdleUrl);
//          newConfig = hcu.getConfig().toString();
//          setIdleConfig(newConfig);
//          Log.d(LogKeys.DEBUG,
//                  TAG+".GetPrefsFromServerTask.doInBackground()/ newConfig=" + newConfig);
//        }

        return true;

      } catch (Exception e) {
        Log.e(LogKeys.DEBUG, "SharedPrefsHandler.GetPrefsFromServerTask" +
                              ".onPostExecute(): error=", e);
        return false;
      }
    }

    @Override
    protected void onPostExecute(Boolean result) {
      // updated = result;
      // Change UI of LaunchActivity
      prefs.edit().putBoolean(SharedPrefs.FIRST_RUN, false).apply();

      EditText username;
//      RadioButton isMaleRadioButton, isFemaleRadioButton;
      if (context instanceof LaunchActivity) {
        username = (EditText)((LaunchActivity)context)
                .findViewById(R.id.user_name);
        username.setText(getUsername());
      }

      progressDialog.dismiss();
      Log.d(LogKeys.DEBUG, "SharedPrefsHandler.GetPrefsFromServerTask" +
                            ".onPostExecute(): get prefs from server complete");
    }
  }

  private class SetPrefsToServerTask extends AsyncTask<String, Void, Boolean> {
    @Override
    protected Boolean doInBackground(String... urls) {
      try {
        String currUsername = prefs.getString(SharedPrefs.USERNAME,
                Config.DEFAULT_USERNAME);
        boolean currIsFemale =
                prefs.getBoolean(SharedPrefs.IS_FEMALE, Config.DEFAULT_IS_FEMALE);
        int currSensorId = prefs.getInt(SharedPrefs.KEY_SENSOR_ID, 0);

        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair(SharedPrefs.DEVICE_ID,
                                                  deviceId));
        nameValuePairs.add(new BasicNameValuePair(SharedPrefs.USERNAME,
                                                  currUsername));
        nameValuePairs.add(new BasicNameValuePair(SharedPrefs.IS_FEMALE,
                              String.valueOf((currIsFemale) ? 1 : 0)));
        nameValuePairs.add(new BasicNameValuePair(SharedPrefs.KEY_SENSOR_ID,
                                              String.valueOf(currSensorId)));
        String response = HttpUtil.sendPost(urls[0], nameValuePairs);
        Log.d(LogKeys.DEBUG, "SharedPrefsHandler.SetPrefsToServerTask" +
                              ".doInBackground(): response=" + response);
        return true;
      } catch (Exception e) {
        Log.e(LogKeys.DEBUG, "SharedPrefsHandler.SetPrefsToServerTask" +
                              ".onPostExecute(): error=", e);
        return false;
      }
    }

    @Override
    protected void onPostExecute(Boolean result) {
      Log.d(LogKeys.DEBUG, "SharedPrefsHandler.SetPrefsToServerTask" +
                            ".onPostExecute(): set prefs to server complete");
    }
  }

}
