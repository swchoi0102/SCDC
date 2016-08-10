package kr.ac.snu.imlab.scdc.service.probe;

import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.math.BigDecimal;

import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.builtin.ProbeKeys;
import edu.mit.media.funf.time.DecimalTimeUnit;
import edu.mit.media.funf.time.TimeUtil;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys;

@Probe.DisplayName("System Settings Log Probe")
@Probe.Description("Records System content(screen brightness, accelerometer rotation, volume) for all time")
@Schedule.DefaultSchedule(interval = 0, duration = 0, opportunistic = true)
public class SystemSettingsProbe extends Probe.Base implements Probe.ContinuousProbe {

    private AudioManager audioManager;
    private JsonObject lastSystemSettings = null;
    private JsonObject currSystemSettings = null;
    private static final int DEFAULT_VALUE = -1;
    private SettingsContentObserver settingsContentObserver;

//    private double checkInterval = 2.5;
//    private SettingChecker settingChecker = new SettingChecker();
//    private long lastTimeMillis;
//    private int lastScreenBrightness;
//    private int lastAccelerometerRotation;
//    private int lastVolumeAlarm;
//    private int lastVolumeMusic;
//    private int lastVolumeNotification;
//    private int lastVolumeRing;
//    private int lastVolumeSystem;
//    private int lastVolumeVoice;
//    private boolean replicateOn = false;

//    private class SettingChecker implements Runnable {
//        @Override
//        public void run() {
//            getHandler().postDelayed(this, TimeUtil.secondsToMillis(checkInterval));
//            long currentTimeMillis = System.currentTimeMillis();
//            if (replicateOn){
//                if (currentTimeMillis > lastTimeMillis + TimeUtil.secondsToMillis(checkInterval)){
////					Log.d(SCDCKeys.LogKeys.DEB, "[Sensor] curr: " + currentTimeMillis + ", last: " + lastTimeMillis);
//                    replicateData(lastScreenBrightness, lastAccelerometerRotation,
//                            lastVolumeAlarm, lastVolumeMusic, lastVolumeNotification,
//                            lastVolumeRing, lastVolumeSystem, lastVolumeVoice, currentTimeMillis);
//                }
//            }
//        }
//
//        public void endCurrentTask() {
////			Log.d(SCDCKeys.LogKeys.DEB, "[Sensor] End replicate task");
//            reset();
//        }
//
//        public void reset() {
////			Log.d(SCDCKeys.LogKeys.DEB, "[Sensor] Reset replicate task");
//            replicateOn = false;
//        }
//    }
//
//    protected void replicateData(int sb, int ar, int va, int vm, int vn,
//                                 int vr, int vs, int vv, long timeMillis) {
////		Log.d(SCDCKeys.LogKeys.DEB, "[Sensor] Replicate data!");
//        JsonObject data = new JsonObject();
//        data.addProperty(SCDCKeys.SystemSettingsKeys.SCREEN_BRIGHTNESS, sb);
//        data.addProperty(SCDCKeys.SystemSettingsKeys.ACCELEROMETER_ROTATION, ar);
//        data.addProperty(SCDCKeys.SystemSettingsKeys.VOLUME_ALARM, va);
//        data.addProperty(SCDCKeys.SystemSettingsKeys.VOLUME_MUSIC, vm);
//        data.addProperty(SCDCKeys.SystemSettingsKeys.VOLUME_NOTIFICATION, vn);
//        data.addProperty(SCDCKeys.SystemSettingsKeys.VOLUME_RING, vr);
//        data.addProperty(SCDCKeys.SystemSettingsKeys.VOLUME_SYSTEM, vs);
//        data.addProperty(SCDCKeys.SystemSettingsKeys.VOLUME_VOICE, vv);
//        data.addProperty(ProbeKeys.BaseProbeKeys.TIMESTAMP, edu.mit.media.funf.time.TimeUtil.getTimestamp());
////        data.addProperty("rep", true);
//
//        // check one more time
//        if (timeMillis > lastTimeMillis + TimeUtil.secondsToMillis(checkInterval)){
//            sendData(data);
//        }
//    }

    @Override
    protected void onEnable() {
        super.onEnable();
        Log.d(SCDCKeys.LogKeys.DEB, "[SysSet] onEnable");
        initializeAudioManager();
        initializeSettingsContentObserver();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(SCDCKeys.LogKeys.DEB, "[SysSet] onStart");
        initializeSystemSettings();
        registerContentObserver();
//        onContinue();
    }

//    protected void onContinue() {
////		Log.d(SCDCKeys.LogKeys.DEB, "[Sensor] onContinue");
//        getHandler().post(settingChecker);
//    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(SCDCKeys.LogKeys.DEB, "[SysSet] onStop");
        unregisterContentObserver();
//        onPause();
    }

//    protected void onPause() {
////		Log.d(SCDCKeys.LogKeys.DEB, "[Sensor] onPause");
//        getHandler().removeCallbacks(settingChecker);
//        settingChecker.endCurrentTask();
//    }

    @Override
    protected void onDisable() {
//		Log.d(SCDCKeys.LogKeys.DEB, "[Sensor] onDisable");
        super.onDisable();
        lastSystemSettings = null;
        currSystemSettings = null;
//        settingChecker.reset();
    }

    private void initializeAudioManager() {
        audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
    }

    private void initializeSystemSettings() {
        lastSystemSettings = getCurrentSystemSettings();
//        sendData(lastSystemSettings);
//        replicateOn = true;
    }

    private JsonObject getCurrentSystemSettings() {
        Log.d(SCDCKeys.LogKeys.DEB, "[SysSet] getCurrentSystemSettings");
        JsonObject systemSettings = new JsonObject();

//        lastScreenBrightness = getCurrentValue(Settings.System.SCREEN_BRIGHTNESS);
//        lastAccelerometerRotation = getCurrentValue(Settings.System.ACCELEROMETER_ROTATION);
//        lastVolumeAlarm = getCurrentVolume(AudioManager.STREAM_ALARM);
//        lastVolumeMusic = getCurrentVolume(AudioManager.STREAM_MUSIC);
//        lastVolumeNotification = getCurrentVolume(AudioManager.STREAM_NOTIFICATION);
//        lastVolumeRing = getCurrentVolume(AudioManager.STREAM_RING);
//        lastVolumeSystem = getCurrentVolume(AudioManager.STREAM_SYSTEM);
//        lastVolumeVoice = getCurrentVolume(AudioManager.STREAM_VOICE_CALL);
//        lastTimeMillis = System.currentTimeMillis();

        systemSettings.addProperty(SCDCKeys.SystemSettingsKeys.SCREEN_BRIGHTNESS, getCurrentValue(Settings.System.SCREEN_BRIGHTNESS));
        systemSettings.addProperty(SCDCKeys.SystemSettingsKeys.ACCELEROMETER_ROTATION, getCurrentValue(Settings.System.ACCELEROMETER_ROTATION));
        systemSettings.addProperty(SCDCKeys.SystemSettingsKeys.VOLUME_ALARM, getCurrentVolume(AudioManager.STREAM_ALARM));
        systemSettings.addProperty(SCDCKeys.SystemSettingsKeys.VOLUME_MUSIC, getCurrentVolume(AudioManager.STREAM_MUSIC));
        systemSettings.addProperty(SCDCKeys.SystemSettingsKeys.VOLUME_NOTIFICATION, getCurrentVolume(AudioManager.STREAM_NOTIFICATION));
        systemSettings.addProperty(SCDCKeys.SystemSettingsKeys.VOLUME_RING, getCurrentVolume(AudioManager.STREAM_RING));
        systemSettings.addProperty(SCDCKeys.SystemSettingsKeys.VOLUME_SYSTEM, getCurrentVolume(AudioManager.STREAM_SYSTEM));
        systemSettings.addProperty(SCDCKeys.SystemSettingsKeys.VOLUME_VOICE, getCurrentVolume(AudioManager.STREAM_VOICE_CALL));
        systemSettings.addProperty(ProbeKeys.BaseProbeKeys.TIMESTAMP, TimeUtil.getTimestamp());

        return systemSettings;
    }

    private int getCurrentValue(String name) {
        return android.provider.Settings.System.getInt(getContext().getContentResolver(), name, DEFAULT_VALUE);
    }

    private int getCurrentVolume(int streamType) {
        return audioManager.getStreamVolume(streamType);
    }


    private void initializeSettingsContentObserver() {
        settingsContentObserver = new SettingsContentObserver(new Handler());
    }

    private class SettingsContentObserver extends ContentObserver {

        public SettingsContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return super.deliverSelfNotifications();
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            if (lastSystemSettings == null) {
                lastSystemSettings = getCurrentSystemSettings();
            } else{
                currSystemSettings = getCurrentSystemSettings();
                if (isSystemSettingsChanged()) {
                    BigDecimal prevTimestamp = lastSystemSettings.get(ProbeKeys.BaseProbeKeys.TIMESTAMP).getAsBigDecimal();
                    BigDecimal currTimestamp = currSystemSettings.get(ProbeKeys.BaseProbeKeys.TIMESTAMP).getAsBigDecimal();
                    lastSystemSettings.addProperty(SCDCKeys.SystemSettingsKeys.DURATION, currTimestamp.subtract(prevTimestamp).floatValue());
                    sendData(lastSystemSettings);
                    lastSystemSettings = currSystemSettings;
//                replicateOn = true;
                }
            }
        }
    }

    private void registerContentObserver() {
        getContext().getContentResolver().registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, settingsContentObserver);
    }

    private boolean isSystemSettingsChanged() {
//        lastSystemSettings.remove(ProbeKeys.BaseProbeKeys.TIMESTAMP);
//        return !(lastSystemSettings.entrySet().equals(currentSystemSettings.entrySet()));

        BigDecimal currTimeStamp = currSystemSettings.remove(ProbeKeys.BaseProbeKeys.TIMESTAMP).getAsBigDecimal();
        BigDecimal lastTimeStamp = lastSystemSettings.remove(ProbeKeys.BaseProbeKeys.TIMESTAMP).getAsBigDecimal();
//        float lastDuration = -1;
//        if (lastSystemSettings.has(SCDCKeys.SystemSettingsKeys.DURATION)) lastDuration = lastSystemSettings.remove(SCDCKeys.SystemSettingsKeys.DURATION).getAsFloat();

        boolean result = !(lastSystemSettings.entrySet().equals(currSystemSettings.entrySet()));
        currSystemSettings.addProperty(ProbeKeys.BaseProbeKeys.TIMESTAMP, currTimeStamp);
        lastSystemSettings.addProperty(ProbeKeys.BaseProbeKeys.TIMESTAMP, lastTimeStamp);
//        if (lastSystemSettings.has(SCDCKeys.SystemSettingsKeys.DURATION)) lastSystemSettings.addProperty(SCDCKeys.SystemSettingsKeys.DURATION, lastDuration);
        return result;
    }

    private void unregisterContentObserver() {
        getContext().getContentResolver().unregisterContentObserver(settingsContentObserver);
    }

//    @Override
//    public void unregisterListener(DataListener... listeners) {
//        if (listeners != null) {
//            if (lastSystemSettings != null){
//                long duration = System.currentTimeMillis() - lastSystemSettings.get(ProbeKeys.BaseProbeKeys.TIMESTAMP).getAsLong();
//                lastSystemSettings.addProperty(SCDCKeys.SystemSettingsKeys.DURATION, DecimalTimeUnit.MILLISECONDS.toSeconds(duration).floatValue());
//                sendData(lastSystemSettings);
//                Log.d(SCDCKeys.LogKeys.DEB, "[SysSet] send last data before unregisterListener!");
//            }
//
//            synchronized (dataListeners) {
//                for (DataListener listener : listeners) {
//                    dataListeners.remove(listener);
//                    listener.onDataCompleted(getConfig(), null);
//                }
//            }
//            // If no one is listening, stop using device resources
//            if (dataListeners.isEmpty()) {
//                stop();
//            }
//            if (passiveDataListeners.isEmpty()) {
//                disable();
//            }
//        }
//    }

    @Override
    public void sendLastData() {
        if (lastSystemSettings != null){
            BigDecimal duration = DecimalTimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()).subtract(lastSystemSettings.get(ProbeKeys.BaseProbeKeys.TIMESTAMP).getAsBigDecimal());
            Log.d(SCDCKeys.LogKeys.DEB, "[SysSet] sendLastData!, " + duration);
            lastSystemSettings.addProperty(SCDCKeys.SystemSettingsKeys.DURATION, duration.floatValue());
            sendData(lastSystemSettings);
        }
    }
}
