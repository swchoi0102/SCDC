package kr.ac.snu.imlab.scdc.service.probe;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.TrafficStats;
import android.util.Log;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys;

@Probe.DisplayName("Network Traffic Probe")
@Probe.Description("records network traffic(mobile data network, all network) generated for interval in bytes. also records network traffic with applications.")
@Schedule.DefaultSchedule(interval = 60)
public class NetworkTrafficProbe extends Probe.Base implements Probe.ContinuousProbe {

    private static final String TAG = "debug1013";
    private TrafficStatsDummy trafficStatsCurrent;
    public static TrafficStatsDummy trafficStatsPast;
    private int expId;

    @Override
    protected void onEnable() {
        Log.i(TAG, "NetworkTrafficProbe onEnable()");
        super.onEnable();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "NetworkTrafficProbe onStart");
        if (trafficStatsPast == null) {
            trafficStatsCurrent = snapTrafficStatsCurrent();
            setTrafficStatsPast();
        }
    }

    private TrafficStatsDummy snapTrafficStatsCurrent() {
        TrafficStatsDummy currentTrafficStats = new TrafficStatsDummy();
        currentTrafficStats.totalRxBytes = TrafficStats.getTotalRxBytes();
        currentTrafficStats.totalTxBytes = TrafficStats.getTotalTxBytes();
        currentTrafficStats.mobileRxBytes = TrafficStats.getMobileRxBytes();
        currentTrafficStats.mobileTxBytes = TrafficStats.getMobileTxBytes();

        for (ApplicationInfo applicationInfo : ApplicationInfoInstalled()) {
            currentTrafficStats.applicationUIdAndPackageName.put(applicationInfo.uid, applicationInfo.packageName);
            currentTrafficStats.applicationRxBytes.put(applicationInfo.uid, TrafficStats.getUidRxBytes(applicationInfo.uid));
            currentTrafficStats.applicationTxBytes.put(applicationInfo.uid, TrafficStats.getUidTxBytes(applicationInfo.uid));
        }

        return currentTrafficStats;
    }

    private List<ApplicationInfo> ApplicationInfoInstalled() {
        PackageManager pm = getContext().getPackageManager();
        return pm.getInstalledApplications(PackageManager.GET_META_DATA);
    }

    private ArrayList<JsonObject> trafficDataList() {
        ArrayList<JsonObject> trafficDataList = new ArrayList<>();

        trafficDataList.add(totalTraffic());

        trafficDataList.add(mobileTraffic());

        for (int uid : trafficStatsCurrent.applicationUIdAndPackageName.keySet()) {
            JsonObject applicationTraffic = getApplicationTrafficWithUId(uid);
            if (applicationTraffic != null) {
                trafficDataList.add(applicationTraffic);
            }
        }

        return trafficDataList;
    }

    private JsonObject totalTraffic() {
        JsonObject traffic = new JsonObject();
        traffic.addProperty(SCDCKeys.NetworkTrafficKeys.WHERE, SCDCKeys.NetworkTrafficKeys.TOTAL_WHERE_VALUE);
        traffic.addProperty(SCDCKeys.NetworkTrafficKeys.RECEIVED, trafficStatsCurrent.totalRxBytes - trafficStatsPast.totalRxBytes);
        traffic.addProperty(SCDCKeys.NetworkTrafficKeys.TRANSMITTED, trafficStatsCurrent.totalTxBytes - trafficStatsPast.totalTxBytes);
        return traffic;
    }

    private JsonObject mobileTraffic() {
        JsonObject traffic = new JsonObject();
        traffic.addProperty(SCDCKeys.NetworkTrafficKeys.WHERE, SCDCKeys.NetworkTrafficKeys.MOBILE_WHERE_VALUE);
        traffic.addProperty(SCDCKeys.NetworkTrafficKeys.RECEIVED, trafficStatsCurrent.mobileRxBytes - trafficStatsPast.mobileRxBytes);
        traffic.addProperty(SCDCKeys.NetworkTrafficKeys.TRANSMITTED, trafficStatsCurrent.mobileTxBytes - trafficStatsPast.mobileTxBytes);
        return traffic;
    }

    private JsonObject getApplicationTrafficWithUId(int uid) {
        if (!trafficStatsPast.applicationUIdAndPackageName.keySet().contains(uid)) {
            return null;
        } else {
            long trafficRx = trafficStatsCurrent.applicationRxBytes.get(uid) - trafficStatsPast.applicationRxBytes.get(uid);
            long trafficTx = trafficStatsCurrent.applicationTxBytes.get(uid) - trafficStatsPast.applicationTxBytes.get(uid);
            if (trafficRx == 0 && trafficTx == 0) {
                return null;
            } else {
                JsonObject applicationTraffic = new JsonObject();
                applicationTraffic.addProperty(SCDCKeys.NetworkTrafficKeys.WHERE, trafficStatsCurrent.applicationUIdAndPackageName.get(uid));
                applicationTraffic.addProperty(SCDCKeys.NetworkTrafficKeys.RECEIVED, trafficRx);
                applicationTraffic.addProperty(SCDCKeys.NetworkTrafficKeys.TRANSMITTED, trafficTx);
                return applicationTraffic;
            }
        }
    }

    private void sendTraffic(ArrayList<JsonObject> trafficDataList) {
        if (isValidTrafficDataList(trafficDataList)) {
            for (JsonObject trafficData : trafficDataList) {
                Log.i(TAG, "send Traffic: " + trafficData);
                sendData(trafficData);
            }
        }
    }

    private boolean isValidTrafficDataList(ArrayList<JsonObject> trafficDataList) {
        if (trafficDataList == null) {
            return false;
        } else {
            for (JsonObject trafficData : trafficDataList) {
                if (trafficData.get(SCDCKeys.NetworkTrafficKeys.RECEIVED).getAsLong() < 0
                        || trafficData.get(SCDCKeys.NetworkTrafficKeys.TRANSMITTED).getAsLong() < 0) {
                    return false;
                }
            }
            return true;
        }
    }

    private void setTrafficStatsPast() {
        trafficStatsPast = trafficStatsCurrent;
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "NetworkTrafficProbe onStop()");
        trafficStatsCurrent = snapTrafficStatsCurrent();
        sendTraffic(trafficDataList());
    }

    @Override
    protected void onDisable() {
        Log.i(TAG, "NetworkTrafficProbe onDisable()");
    }

    private class TrafficStatsDummy {

        public long mobileRxBytes;
        public long mobileTxBytes;
        public long totalRxBytes;
        public long totalTxBytes;
        public HashMap<Integer, String> applicationUIdAndPackageName;
        public HashMap<Integer, Long> applicationRxBytes;
        public HashMap<Integer, Long> applicationTxBytes;

        public TrafficStatsDummy() {
            applicationUIdAndPackageName = new HashMap<>();
            applicationRxBytes = new HashMap<>();
            applicationTxBytes = new HashMap<>();
        }
    }

}