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
import edu.mit.media.funf.time.TimeUtil;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys;

@Probe.RequiredPermissions({Manifest.permission.ACCESS_NETWORK_STATE})
@Probe.DisplayName("Network Settings Log Probe")
@Probe.Description("Records Network status(mobile data on/off, wifi usage on/off, airplane mode on/off) for all time")
@Schedule.DefaultSchedule(interval = 0, duration = 0, opportunistic = true)
public class NetworkSettingsProbe extends InsensitiveProbe implements Probe.ContinuousProbe {

    private static final String MOBILE_DATA_ON_NAME = "mobile_data";
    private static final int DEFAULT_VALUE = -1;
    private SettingsContentObserver settingsContentObserver;

    @Override
    protected void onEnable() {
        super.onEnable();
        initializeSettingsContentObserver();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(SCDCKeys.LogKeys.DEB, "[NetworkSettingsProbe] onStart");
        initializeNetworkSettings();
        registerContentObserver();
    }

    @Override
    protected void onStop() {
        Log.d(SCDCKeys.LogKeys.DEB, "[NetworkSettingsProbe] onStop");
        super.onStop();
        unregisterContentObserver();
    }

    private void initializeNetworkSettings() {
        lastData = getCurrData();
    }

    @Override
    protected JsonObject getCurrData() {
        Log.d(SCDCKeys.LogKeys.DEB, "[NetworkSettingsProbe] getCurrData");
        JsonObject currentNetworkSettings = new JsonObject();

        currentNetworkSettings.addProperty(SCDCKeys.NetworkSettingsKeys.AIR_PLANE_MODE_ON, getCurrentValue(Settings.Global.AIRPLANE_MODE_ON));
        currentNetworkSettings.addProperty(SCDCKeys.NetworkSettingsKeys.MOBILE_DATA_ON, getCurrentValue(MOBILE_DATA_ON_NAME));
        currentNetworkSettings.addProperty(SCDCKeys.NetworkSettingsKeys.WIFI_ON, getCurrentValue(Settings.Global.WIFI_ON));
        currentNetworkSettings.addProperty(ProbeKeys.BaseProbeKeys.TIMESTAMP, TimeUtil.getTimestamp());
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
            if (lastData == null) {
                lastData = getCurrData();
            } else {
                currData = getCurrData();
                if (isDataChanged()) sendData();
            }
        }

    }

    private void registerContentObserver() {
        getContext().getContentResolver().registerContentObserver(android.provider.Settings.Global.CONTENT_URI, true, settingsContentObserver);
    }

    private void unregisterContentObserver() {
        getContext().getContentResolver().unregisterContentObserver(settingsContentObserver);
    }

}
