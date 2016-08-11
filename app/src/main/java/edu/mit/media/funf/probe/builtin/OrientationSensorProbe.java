/**
 * Funf: Open Sensing Framework
 * Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland.
 * Acknowledgments: Alan Gardner
 * Contact: nadav@media.mit.edu
 * <p/>
 * This file is part of Funf.
 * <p/>
 * Funf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * <p/>
 * Funf is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public
 * License along with Funf. If not, see <http://www.gnu.org/licenses/>.
 */
package edu.mit.media.funf.probe.builtin;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.Probe.Description;
import edu.mit.media.funf.probe.Probe.RequiredFeatures;
import edu.mit.media.funf.probe.builtin.ProbeKeys.OrientationSensorKeys;
import edu.mit.media.funf.time.DecimalTimeUnit;
import edu.mit.media.funf.time.TimeUtil;
import edu.mit.media.funf.util.LogUtil;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys;

@Description("Records a three dimensional vector of the magnetic field.")
@RequiredFeatures("android.hardware.sensor.gyroscope")
@Schedule.DefaultSchedule(interval = 180, duration = 15)
public class OrientationSensorProbe extends Probe.Base implements Probe.ContinuousProbe, ProbeKeys.SensorKeys, OrientationSensorKeys {

    public static final double DEFAULT_PERIOD = 3600;
    public static final double DEFAULT_DURATION = 60;

    @Configurable
    private String sensorDelay = SENSOR_DELAY_GAME;
    private double checkInterval = 1.0;

    public static final String
            SENSOR_DELAY_FASTEST = "FASTEST",
            SENSOR_DELAY_GAME = "GAME",
            SENSOR_DELAY_UI = "UI",
            SENSOR_DELAY_NORMAL = "NORMAL";

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private SensorEventListener sensorListener;

//    private SensorChecker sensorChecker = new SensorChecker();
//    private long lastTimeMillis;
//    private float[] lastValues;
//    private int lastAccuracy;
//    private boolean replicateOn = false;
//
////	@Override
////	public void sendFinalData() {
////
////	}
//
//    private class SensorChecker implements Runnable {
//        @Override
//        public void run() {
//            getHandler().postDelayed(this, TimeUtil.secondsToMillis(checkInterval));
//            long currentTimeMillis = System.currentTimeMillis();
//            if (lastValues != null && replicateOn) {
//                if (currentTimeMillis > lastTimeMillis + TimeUtil.secondsToMillis(checkInterval)) {
////					Log.d(SCDCKeys.LogKeys.DEB, "[Sensor] curr: " + currentTimeMillis + ", last: " + lastTimeMillis);
//                    replicateData(lastValues, currentTimeMillis, lastAccuracy);
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
//            lastValues = null;
//            replicateOn = false;
//        }
//    }
//
//    protected void replicateData(float[] vArr, long timeMillis, int acc) {
////		Log.d(SCDCKeys.LogKeys.DEB, "[Sensor] Replicate data!");
//        JsonObject data = new JsonObject();
//        data.addProperty(TIMESTAMP, DecimalTimeUnit.MILLISECONDS.toSeconds(timeMillis));
//        data.addProperty(ACCURACY, acc);
//        data.addProperty("rep", true);
//        final String[] valueNames = getValueNames();
//
//        for (int i = 0; i < vArr.length; i++) {
//            String valueName = valueNames[i];
//            data.addProperty(valueName, vArr[i]);
//        }
//
//        // check one more time
//        if (timeMillis > lastTimeMillis + TimeUtil.secondsToMillis(checkInterval)) {
//            sendData(data);
//        }
//    }


    @Override
    protected void onEnable() {
        super.onEnable();
        sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        final String[] valueNames = getValueNames();
//        lastValues = new float[valueNames.length];
        sensorListener = new SensorEventListener() {

            float[] mGravity;
            float[] mGeomagnetic;

            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                    mGravity = event.values;
                if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                    mGeomagnetic = event.values;
                if (mGravity != null && mGeomagnetic != null) {
                    float R[] = new float[9];
                    float I[] = new float[9];
                    boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
                    if (success) {
//                        lastTimeMillis = System.currentTimeMillis();
                        JsonObject data = new JsonObject();
//                        data.addProperty(TIMESTAMP, DecimalTimeUnit.MILLISECONDS.toSeconds(lastTimeMillis));
                        data.addProperty(TIMESTAMP, TimeUtil.getTimestamp());
                        data.addProperty(ACCURACY, event.accuracy);

                        float orientation[] = new float[3];
                        SensorManager.getOrientation(R, orientation);

                        for (int i = 0; i < valueNames.length; i++) {
                            String valueName = valueNames[i];
                            float tempValue = (float) Math.toDegrees(orientation[i]);
//							int tempValue = (int) ( Math.toDegrees( orientation[i] ) + 360 ) % 360;
                            data.addProperty(valueName, tempValue);
//                            lastValues[i] = tempValue;
//                            lastAccuracy = event.accuracy;
                        }
                        sendData(data);
//                        replicateOn = true;
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
    }

    @Override
    protected void onStart() {
        Log.d(SCDCKeys.LogKeys.DEB, "[OrientationSensorProbe] onStart");
        super.onStart();
        getSensorManager().registerListener(sensorListener, accelerometer, getSensorDelay(sensorDelay));
        getSensorManager().registerListener(sensorListener, magnetometer, getSensorDelay(sensorDelay));
//        onContinue();
    }

//    protected void onContinue() {
////		Log.d(SCDCKeys.LogKeys.DEB, "[Sensor] onContinue");
//        getHandler().post(sensorChecker);
//    }

    @Override
    protected void onStop() {
		Log.d(SCDCKeys.LogKeys.DEB, "[OrientationSensorProbe] onStop");
        super.onStop();
        getSensorManager().unregisterListener(sensorListener);
//        onPause();
    }

//    protected void onPause() {
////		Log.d(SCDCKeys.LogKeys.DEB, "[Sensor] onPause");
//        getHandler().removeCallbacks(sensorChecker);
//        sensorChecker.endCurrentTask();
//    }

//    @Override
//    protected void onDisable() {
////		Log.d(SCDCKeys.LogKeys.DEB, "[Sensor] onDisable");
//        sensorChecker.reset();
//    }

    protected SensorManager getSensorManager() {
        if (sensorManager == null) {
            sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        }
        return sensorManager;
    }

    protected int getSensorDelay(String specifiedSensorDelay) {
        int sensorDelay = -1;
        JsonElement el = getGson().toJsonTree(specifiedSensorDelay);
        if (!el.isJsonNull()) {
            try {
                int sensorDelayInt = el.getAsInt();
                if (sensorDelayInt == SensorManager.SENSOR_DELAY_FASTEST
                        || sensorDelayInt == SensorManager.SENSOR_DELAY_GAME
                        || sensorDelayInt == SensorManager.SENSOR_DELAY_UI
                        || sensorDelayInt == SensorManager.SENSOR_DELAY_NORMAL) {
                    sensorDelay = sensorDelayInt;
                }
            } catch (NumberFormatException e) {
            } catch (ClassCastException e) {
            } catch (IllegalStateException e) {
            }
        }

        if (sensorDelay < 0) {
            try {
                String sensorDelayString = el.getAsString().toUpperCase().replace("SENSOR_DELAY_", "");
                if (SENSOR_DELAY_FASTEST.equals(sensorDelayString)) {
                    sensorDelay = SensorManager.SENSOR_DELAY_FASTEST;
                } else if (SENSOR_DELAY_GAME.equals(sensorDelayString)) {
                    sensorDelay = SensorManager.SENSOR_DELAY_GAME;
                } else if (SENSOR_DELAY_UI.equals(sensorDelayString)) {
                    sensorDelay = SensorManager.SENSOR_DELAY_UI;
                } else if (SENSOR_DELAY_NORMAL.equals(sensorDelayString)) {
                    sensorDelay = SensorManager.SENSOR_DELAY_NORMAL;
                }
            } catch (ClassCastException cce) {
                Log.w(LogUtil.TAG, "Unknown sensor delay value: " + specifiedSensorDelay);
            }
        }

        if (sensorDelay < 0) {
            sensorDelay = SensorManager.SENSOR_DELAY_FASTEST;
        }

        return sensorDelay;
    }

    public String[] getValueNames() {
        return new String[]{
                AZIMUTH, PITCH, ROLL
        };
    }
}

