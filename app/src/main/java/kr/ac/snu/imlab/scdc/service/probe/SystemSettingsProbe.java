package kr.ac.snu.imlab.scdc.service.probe;

import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Handler;
import android.provider.Settings;

import com.google.gson.JsonObject;

import java.math.BigDecimal;

import edu.mit.media.funf.Schedule;
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
    private JsonObject systemSettings;
    private static final int DEFAULT_VALUE = -1;
    private SettingsContentObserver settingsContentObserver;

    private double checkInterval = 2.5;
    private SettingChecker settingChecker = new SettingChecker();
    private long lastTimeMillis;
    private int lastScreenBrightness;
    private int lastAccelerometerRotation;
    private int lastVolumeAlarm;
    private int lastVolumeMusic;
    private int lastVolumeNotification;
    private int lastVolumeRing;
    private int lastVolumeSystem;
    private int lastVolumeVoice;
    private boolean replicateOn = false;

    private class SettingChecker implements Runnable {
        @Override
        public void run() {
            getHandler().postDelayed(this, TimeUtil.secondsToMillis(checkInterval));
            long currentTimeMillis = System.currentTimeMillis();
            if (replicateOn){
                if (currentTimeMillis > lastTimeMillis + TimeUtil.secondsToMillis(checkInterval)){
//					Log.d(SCDCKeys.LogKeys.DEB, "[Sensor] curr: " + currentTimeMillis + ", last: " + lastTimeMillis);
                    replicateData(lastScreenBrightness, lastAccelerometerRotation,
                            lastVolumeAlarm, lastVolumeMusic, lastVolumeNotification,
                            lastVolumeRing, lastVolumeSystem, lastVolumeVoice, currentTimeMillis);
                }
            }
        }

        public void endCurrentTask() {
//			Log.d(SCDCKeys.LogKeys.DEB, "[Sensor] End replicate task");
            reset();
        }

        public void reset() {
//			Log.d(SCDCKeys.LogKeys.DEB, "[Sensor] Reset replicate task");
            replicateOn = false;
        }
    }

    protected void replicateData(int sb, int ar, int va, int vm, int vn,
                                 int vr, int vs, int vv, long timeMillis) {
//		Log.d(SCDCKeys.LogKeys.DEB, "[Sensor] Replicate data!");
        JsonObject data = new JsonObject();
        data.addProperty(SCDCKeys.SystemSettingsKeys.SCREEN_BRIGHTNESS, sb);
        data.addProperty(SCDCKeys.SystemSettingsKeys.ACCELEROMETER_ROTATION, ar);
        data.addProperty(SCDCKeys.SystemSettingsKeys.VOLUME_ALARM, va);
        data.addProperty(SCDCKeys.SystemSettingsKeys.VOLUME_MUSIC, vm);
        data.addProperty(SCDCKeys.SystemSettingsKeys.VOLUME_NOTIFICATION, vn);
        data.addProperty(SCDCKeys.SystemSettingsKeys.VOLUME_RING, vr);
        data.addProperty(SCDCKeys.SystemSettingsKeys.VOLUME_SYSTEM, vs);
        data.addProperty(SCDCKeys.SystemSettingsKeys.VOLUME_VOICE, vv);
        data.addProperty(ProbeKeys.BaseProbeKeys.TIMESTAMP, edu.mit.media.funf.time.TimeUtil.getTimestamp());
        data.addProperty("rep", true);

        // check one more time
        if (timeMillis > lastTimeMillis + TimeUtil.secondsToMillis(checkInterval)){
            sendData(data);
        }
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        initializeAudioManager();
        initializeSettingsContentObserver();
    }

    @Override
    protected void onStart() {
        super.onStart();
        initializeSystemSettings();
        registerContentObserver();
        onContinue();
    }

    protected void onContinue() {
//		Log.d(SCDCKeys.LogKeys.DEB, "[Sensor] onContinue");
        getHandler().post(settingChecker);
    }

    @Override
    protected void onStop() {
//		Log.d(SCDCKeys.LogKeys.DEB, "[Sensor] onStop");
        unregisterContentObserver();
        onPause();
    }

    protected void onPause() {
//		Log.d(SCDCKeys.LogKeys.DEB, "[Sensor] onPause");
        getHandler().removeCallbacks(settingChecker);
        settingChecker.endCurrentTask();
    }

    @Override
    protected void onDisable() {
//		Log.d(SCDCKeys.LogKeys.DEB, "[Sensor] onDisable");
        settingChecker.reset();
    }

    private void initializeAudioManager() {
        audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
    }

    private void initializeSystemSettings() {
        systemSettings = getCurrentSystemSettings();
        sendData(systemSettings);
        replicateOn = true;
    }

    private JsonObject getCurrentSystemSettings() {
        JsonObject currentSystemSettings = new JsonObject();

        lastScreenBrightness = getCurrentValue(Settings.System.SCREEN_BRIGHTNESS);
        lastAccelerometerRotation = getCurrentValue(Settings.System.ACCELEROMETER_ROTATION);
        lastVolumeAlarm = getCurrentVolume(AudioManager.STREAM_ALARM);
        lastVolumeMusic = getCurrentVolume(AudioManager.STREAM_MUSIC);
        lastVolumeNotification = getCurrentVolume(AudioManager.STREAM_NOTIFICATION);
        lastVolumeRing = getCurrentVolume(AudioManager.STREAM_RING);
        lastVolumeSystem = getCurrentVolume(AudioManager.STREAM_SYSTEM);
        lastVolumeVoice = getCurrentVolume(AudioManager.STREAM_VOICE_CALL);
        lastTimeMillis = System.currentTimeMillis();

        currentSystemSettings.addProperty(SCDCKeys.SystemSettingsKeys.SCREEN_BRIGHTNESS, lastScreenBrightness);
        currentSystemSettings.addProperty(SCDCKeys.SystemSettingsKeys.ACCELEROMETER_ROTATION, lastAccelerometerRotation);
        currentSystemSettings.addProperty(SCDCKeys.SystemSettingsKeys.VOLUME_ALARM, lastVolumeAlarm);
        currentSystemSettings.addProperty(SCDCKeys.SystemSettingsKeys.VOLUME_MUSIC, lastVolumeMusic);
        currentSystemSettings.addProperty(SCDCKeys.SystemSettingsKeys.VOLUME_NOTIFICATION, lastVolumeNotification);
        currentSystemSettings.addProperty(SCDCKeys.SystemSettingsKeys.VOLUME_RING, lastVolumeRing);
        currentSystemSettings.addProperty(SCDCKeys.SystemSettingsKeys.VOLUME_SYSTEM, lastVolumeSystem);
        currentSystemSettings.addProperty(SCDCKeys.SystemSettingsKeys.VOLUME_VOICE, lastVolumeVoice);
        currentSystemSettings.addProperty(ProbeKeys.BaseProbeKeys.TIMESTAMP, DecimalTimeUnit.MILLISECONDS.toSeconds(lastTimeMillis));

        return currentSystemSettings;
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
            JsonObject currentSystemSettings = getCurrentSystemSettings();
            if (isSystemSettingsChanged(currentSystemSettings)) {
                systemSettings = currentSystemSettings;
                sendData(systemSettings);
                replicateOn = true;
            }
        }
    }

    private void registerContentObserver() {
        getContext().getContentResolver().registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, settingsContentObserver);
    }

    private boolean isSystemSettingsChanged(JsonObject currentSystemSettings) {
//        systemSettings.remove(ProbeKeys.BaseProbeKeys.TIMESTAMP);
//        return !(systemSettings.entrySet().equals(currentSystemSettings.entrySet()));

        BigDecimal currTimeStamp = currentSystemSettings.get(ProbeKeys.BaseProbeKeys.TIMESTAMP).getAsBigDecimal();
        currentSystemSettings.remove(ProbeKeys.BaseProbeKeys.TIMESTAMP);
        systemSettings.remove(ProbeKeys.BaseProbeKeys.TIMESTAMP);
        boolean result = !(systemSettings.entrySet().equals(currentSystemSettings.entrySet()));
        currentSystemSettings.addProperty(ProbeKeys.BaseProbeKeys.TIMESTAMP, currTimeStamp);
        return result;
    }

    private void unregisterContentObserver() {
        getContext().getContentResolver().unregisterContentObserver(settingsContentObserver);
    }
}
