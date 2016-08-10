package kr.ac.snu.imlab.scdc.service.probe;

import android.Manifest;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;

import com.google.gson.JsonObject;

import java.math.BigDecimal;

import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.builtin.ProbeKeys;
import edu.mit.media.funf.time.DecimalTimeUnit;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys;
import kr.ac.snu.imlab.scdc.util.TimeUtil;

@Probe.RequiredPermissions({Manifest.permission.ACCESS_NETWORK_STATE})
@Probe.DisplayName("Network Settings Log Probe")
@Probe.Description("Records Network status(mobile data on/off, wifi usage on/off, airplane mode on/off) for all time")
@Schedule.DefaultSchedule(interval = 0, duration = 0, opportunistic = true)
public class NetworkSettingsProbe extends Probe.Base implements Probe.ContinuousProbe {

    private static final String MOBILE_DATA_ON_NAME = "mobile_data";
    private JsonObject networkSettings;
    private static final int DEFAULT_VALUE = -1;
    private SettingsContentObserver settingsContentObserver;

    private double checkInterval = 2.5;
    private SettingChecker settingChecker = new SettingChecker();
    private long lastTimeMillis;
    private int lastAirplaneMode;
    private int lastMobileDataMode;
    private int lastWifiMode;
    private boolean replicateOn = false;

    private class SettingChecker implements Runnable {
        @Override
        public void run() {
            getHandler().postDelayed(this, edu.mit.media.funf.time.TimeUtil.secondsToMillis(checkInterval));
            long currentTimeMillis = System.currentTimeMillis();
            if (replicateOn){
                if (currentTimeMillis > lastTimeMillis + edu.mit.media.funf.time.TimeUtil.secondsToMillis(checkInterval)){
//					Log.d(SCDCKeys.LogKeys.DEB, "[Sensor] curr: " + currentTimeMillis + ", last: " + lastTimeMillis);
                    replicateData(lastAirplaneMode, lastMobileDataMode,
                            lastWifiMode, currentTimeMillis);
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

    protected void replicateData(int am, int mdm, int wm, long timeMillis) {
//		Log.d(SCDCKeys.LogKeys.DEB, "[Sensor] Replicate data!");
        JsonObject data = new JsonObject();
        data.addProperty(SCDCKeys.NetworkSettingsKeys.AIR_PLANE_MODE_ON, am);
        data.addProperty(SCDCKeys.NetworkSettingsKeys.MOBILE_DATA_ON, mdm);
        data.addProperty(SCDCKeys.NetworkSettingsKeys.WIFI_ON, wm);
        data.addProperty(ProbeKeys.BaseProbeKeys.TIMESTAMP, edu.mit.media.funf.time.TimeUtil.getTimestamp());
//        data.addProperty("rep", true);

        // check one more time
        if (timeMillis > lastTimeMillis + edu.mit.media.funf.time.TimeUtil.secondsToMillis(checkInterval)){
            sendData(data);
        }
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        initializeSettingsContentObserver();
    }

    @Override
    protected void onStart() {
        super.onStart();
        initializeNetworkSettings();
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

    private void initializeNetworkSettings() {
        networkSettings = getCurrentNetworkSettings();
        sendData(networkSettings);
        replicateOn = true;
    }

    private JsonObject getCurrentNetworkSettings() {
        JsonObject currentNetworkSettings = new JsonObject();

        lastAirplaneMode = getCurrentValue(Settings.Global.AIRPLANE_MODE_ON);
        lastMobileDataMode = getCurrentValue(MOBILE_DATA_ON_NAME);
        lastWifiMode = getCurrentValue(Settings.Global.WIFI_ON);
        lastTimeMillis = System.currentTimeMillis();

        currentNetworkSettings.addProperty(SCDCKeys.NetworkSettingsKeys.AIR_PLANE_MODE_ON, lastAirplaneMode);
        currentNetworkSettings.addProperty(SCDCKeys.NetworkSettingsKeys.MOBILE_DATA_ON, lastMobileDataMode);
        currentNetworkSettings.addProperty(SCDCKeys.NetworkSettingsKeys.WIFI_ON, lastWifiMode);
        currentNetworkSettings.addProperty(ProbeKeys.BaseProbeKeys.TIMESTAMP, DecimalTimeUnit.MILLISECONDS.toSeconds(lastTimeMillis));
        return currentNetworkSettings;
    }

    private int getCurrentValue(String name) {
        return android.provider.Settings.Global.getInt(getContext().getContentResolver(), name, DEFAULT_VALUE);
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
            JsonObject currentNetworkSettings = getCurrentNetworkSettings();
            if (isNetworkSettingsChanged(currentNetworkSettings)) {
                networkSettings = currentNetworkSettings;
                sendData(networkSettings);
                replicateOn = true;
            }
        }

    }

    private void registerContentObserver() {
        getContext().getContentResolver().registerContentObserver(android.provider.Settings.Global.CONTENT_URI, true, settingsContentObserver);
    }

    private boolean isNetworkSettingsChanged(JsonObject currentNetworkSettings) {
        BigDecimal currTimeStamp = currentNetworkSettings.get(ProbeKeys.BaseProbeKeys.TIMESTAMP).getAsBigDecimal();
        currentNetworkSettings.remove(ProbeKeys.BaseProbeKeys.TIMESTAMP);
        networkSettings.remove(ProbeKeys.BaseProbeKeys.TIMESTAMP);
        boolean result = !(networkSettings.entrySet().equals(currentNetworkSettings.entrySet()));
        currentNetworkSettings.addProperty(ProbeKeys.BaseProbeKeys.TIMESTAMP, currTimeStamp);
        return result;
    }

    private void unregisterContentObserver() {
        getContext().getContentResolver().unregisterContentObserver(settingsContentObserver);
    }

    @Override
    public void sendLastData() {

    }
}
