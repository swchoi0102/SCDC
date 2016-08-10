package kr.ac.snu.imlab.scdc.service.probe;

import android.util.Log;

import com.google.gson.JsonObject;

import java.math.BigDecimal;

import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.builtin.ProbeKeys;
import edu.mit.media.funf.time.DecimalTimeUnit;
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
        lastData.addProperty(SCDCKeys.InsensitiveKeys.DURATION, currTimestamp.subtract(prevTimestamp).floatValue());
        sendData(lastData);
        lastData = currData;
    }

    public void sendLastData() {
        if (lastData != null) {
            BigDecimal duration =
                    DecimalTimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()).subtract(
                            lastData.get(ProbeKeys.BaseProbeKeys.TIMESTAMP).getAsBigDecimal());
            lastData.addProperty(SCDCKeys.InsensitiveKeys.DURATION, duration.floatValue());
            sendData(lastData);
            Log.d(SCDCKeys.LogKeys.DEB, "[InsensitiveProbe] sendLastData!, " + duration);
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
