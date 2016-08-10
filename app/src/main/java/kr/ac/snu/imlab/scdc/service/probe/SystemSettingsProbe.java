package kr.ac.snu.imlab.scdc.service.probe;

import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;

import com.google.gson.JsonObject;

import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.builtin.ProbeKeys;
import edu.mit.media.funf.time.TimeUtil;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys;

@Probe.DisplayName("System Settings Log Probe")
@Probe.Description("Records System content(screen brightness, accelerometer rotation, volume) for all time")
@Schedule.DefaultSchedule(interval = 0, duration = 0, opportunistic = true)
public class SystemSettingsProbe extends InsensitiveProbe implements Probe.ContinuousProbe {

    private AudioManager audioManager;
    private static final int DEFAULT_VALUE = -1;
    private SettingsContentObserver settingsContentObserver;

    @Override
    protected void onEnable() {
        super.onEnable();
//        Log.d(SCDCKeys.LogKeys.DEB, "[SysSet] onEnable");
        initializeAudioManager();
        initializeSettingsContentObserver();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(SCDCKeys.LogKeys.DEB, "[SystemSettingsProbe] onStart");
        initializeSystemSettings();
        registerContentObserver();
    }

    @Override
    protected void onStop() {
        Log.d(SCDCKeys.LogKeys.DEB, "[SystemSettingsProbe] onStop");
        super.onStop();
//        Log.d(SCDCKeys.LogKeys.DEB, "[SysSet] onStop");
        unregisterContentObserver();
    }

    private void initializeAudioManager() {
        audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
    }

    private void initializeSystemSettings() {
        lastData = getCurrData();
    }

    @Override
    protected JsonObject getCurrData() {
        Log.d(SCDCKeys.LogKeys.DEB, "[SystemSettingsProbe] getCurrData");
        JsonObject systemSettings = new JsonObject();

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
            if (lastData == null) {
                lastData = getCurrData();
            } else {
                currData = getCurrData();
                if (isDataChanged()) sendData();
            }
        }
    }

    private void registerContentObserver() {
        getContext().getContentResolver().registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, settingsContentObserver);
    }

    private void unregisterContentObserver() {
        getContext().getContentResolver().unregisterContentObserver(settingsContentObserver);
    }

}
