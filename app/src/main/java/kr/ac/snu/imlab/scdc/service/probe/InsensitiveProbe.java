package kr.ac.snu.imlab.scdc.service.probe;

import android.os.Message;
import android.util.Log;

import com.google.gson.JsonObject;

import java.math.BigDecimal;

import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.builtin.ProbeKeys;
import edu.mit.media.funf.time.TimeUtil;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys;

/**
 * Created by ethan on 16. 8. 10.
 */
public abstract class InsensitiveProbe extends Probe.Base {

    public JsonObject currData = null;
    public JsonObject lastData = null;

    @Override
    protected void onDisable() {
        super.onDisable();
        lastData = null;
        currData = null;
    }

    protected void sendData() {
        BigDecimal prevTimestamp = lastData.get(ProbeKeys.BaseProbeKeys.TIMESTAMP).getAsBigDecimal();
        BigDecimal currTimestamp = currData.get(ProbeKeys.BaseProbeKeys.TIMESTAMP).getAsBigDecimal();
        float duration = currTimestamp.subtract(prevTimestamp).floatValue();
        if (duration > 0.005) {
            lastData.addProperty(SCDCKeys.InsensitiveKeys.DURATION, duration);
            if (duration > 5) lastData.addProperty(SCDCKeys.InsensitiveKeys.IS_URGENT, true);
            sendData(lastData);
            lastData = currData;
        }
    }

    public void sendFinalData() {
        if (lastData != null) {
            float duration = TimeUtil.getTimestamp()
                    .subtract(lastData.get(ProbeKeys.BaseProbeKeys.TIMESTAMP).getAsBigDecimal())
                    .floatValue();
            lastData.addProperty(SCDCKeys.InsensitiveKeys.DURATION, duration);
            if (duration > 5) {
                lastData.addProperty(SCDCKeys.InsensitiveKeys.IS_URGENT, true);
            }
            sendData(lastData);
            String[] nameSplit = this.getClass().getName().split("\\.");
            Log.d(SCDCKeys.LogKeys.DEB, "[" + nameSplit[nameSplit.length - 1] + "] sendFinalData!, " + duration);
            lastData = null;
        }
    }

    protected JsonObject getCurrData() {
        return null;
    }

    protected boolean isDataChanged() {
        BigDecimal currTimeStamp = currData.remove(ProbeKeys.BaseProbeKeys.TIMESTAMP).getAsBigDecimal();
        BigDecimal lastTimeStamp = lastData.remove(ProbeKeys.BaseProbeKeys.TIMESTAMP).getAsBigDecimal();

        boolean result = !(lastData.entrySet().equals(currData.entrySet()));
        currData.addProperty(ProbeKeys.BaseProbeKeys.TIMESTAMP, currTimeStamp);
        lastData.addProperty(ProbeKeys.BaseProbeKeys.TIMESTAMP, lastTimeStamp);

        return result;
    }
}
