/**
 * Funf: Open Sensing Framework
 * Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland.
 * Acknowledgments: Alan Gardner
 * Contact: nadav@media.mit.edu
 * <p>
 * This file is part of Funf.
 * <p>
 * Funf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * <p>
 * Funf is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public
 * License along with Funf. If not, see <http://www.gnu.org/licenses/>.
 */
package kr.ac.snu.imlab.scdc.service.probe;

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
import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.Probe.ContinuousProbe;
import edu.mit.media.funf.probe.builtin.ProbeKeys.SensorKeys;
import edu.mit.media.funf.time.DecimalTimeUnit;
import edu.mit.media.funf.util.LogUtil;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys;

@Schedule.DefaultSchedule(interval = CorrelatedSensitiveSensorsProbe.DEFAULT_PERIOD, duration = CorrelatedSensitiveSensorsProbe.DEFAULT_DURATION)
public class CorrelatedSensitiveSensorsProbe extends Base implements ContinuousProbe, SensorKeys {

    public static final double DEFAULT_PERIOD = 61;
    public static final double DEFAULT_DURATION = 60;

    @Configurable
    private String sensorDelay = SENSOR_DELAY_GAME;
    private final long MIN_INTERVAL_MILLIS = 7;

    public static final String
            SENSOR_DELAY_FASTEST = "FASTEST",
            SENSOR_DELAY_GAME = "GAME",
            SENSOR_DELAY_UI = "UI",
            SENSOR_DELAY_NORMAL = "NORMAL";

    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private Sensor gravitySensor;
    private Sensor magneticSensor;

    private SensorEventListener sensorListener;

    private final String[] ACC_VALUE_NAMES = new String[]{SCDCKeys.SensitiveSensorsKeys.ACCEL_X, SCDCKeys.SensitiveSensorsKeys.ACCEL_Y, SCDCKeys.SensitiveSensorsKeys.ACCEL_Z};
    private final String[] GRVT_VALUE_NAMES = new String[]{SCDCKeys.SensitiveSensorsKeys.GRAVITY_X, SCDCKeys.SensitiveSensorsKeys.GRAVITY_Y, SCDCKeys.SensitiveSensorsKeys.GRAVITY_Z};
    private final String[] ORT_VALUE_NAMES = new String[]{SCDCKeys.SensitiveSensorsKeys.ORIENT_AZIMUTH, SCDCKeys.SensitiveSensorsKeys.ORIENT_PITCH, SCDCKeys.SensitiveSensorsKeys.ORIENT_ROLL};
    private final String[] MAG_VALUE_NAMES = new String[]{SCDCKeys.SensitiveSensorsKeys.MAGNET_X, SCDCKeys.SensitiveSensorsKeys.MAGNET_Y, SCDCKeys.SensitiveSensorsKeys.MAGNET_Z};


    @Override
    protected void onEnable() {
        super.onEnable();
        sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        sensorListener = new SensorEventListener() {
            float[] accelerometerValues = new float[ACC_VALUE_NAMES.length];
            float[] gravityValues = new float[GRVT_VALUE_NAMES.length];
            float[] magneticValues = new float[MAG_VALUE_NAMES.length];
            float[] orientationValues = new float[ORT_VALUE_NAMES.length];
            long lastTimeMillis = 0;
            boolean accelerometerInitialized = false;
            boolean gravityInitialized = false;
            boolean magneticInitialized = false;
            boolean orientationInitialized = false;

            @Override
            public void onSensorChanged(SensorEvent event) {
                long currentTimeMillis = System.currentTimeMillis();
                int sensorType = event.sensor.getType();
                if (sensorType == Sensor.TYPE_ACCELEROMETER){
                    System.arraycopy(event.values, 0, accelerometerValues, 0, accelerometerValues.length);
                    accelerometerInitialized = true;
                }
                if (sensorType == Sensor.TYPE_GRAVITY) {
                    System.arraycopy(event.values, 0, gravityValues, 0, gravityValues.length);
                    gravityInitialized = true;
                }
                if (sensorType == Sensor.TYPE_MAGNETIC_FIELD) {
                    System.arraycopy(event.values, 0, magneticValues, 0, magneticValues.length);
                    magneticInitialized = true;
                }
                if ((sensorType == Sensor.TYPE_ACCELEROMETER || sensorType == Sensor.TYPE_MAGNETIC_FIELD)
                        && (accelerometerInitialized && magneticInitialized)) {
                    float R[] = new float[9];
                    float I[] = new float[9];
                    boolean success = SensorManager.getRotationMatrix(R, I, accelerometerValues, magneticValues);
                    if (success) {
                        orientationValues = new float[3];
                        SensorManager.getOrientation(R, orientationValues);
                        orientationInitialized = true;
                    }
                }

                if (accelerometerInitialized && gravityInitialized && magneticInitialized && orientationInitialized) {
                    JsonObject data = new JsonObject();
                    data.addProperty(TIMESTAMP, DecimalTimeUnit.MILLISECONDS.toSeconds(currentTimeMillis));
                    for (int i = 0; i < ACC_VALUE_NAMES.length; i++)
                        data.addProperty(ACC_VALUE_NAMES[i], accelerometerValues[i]);
                    for (int i = 0; i < GRVT_VALUE_NAMES.length; i++)
                        data.addProperty(GRVT_VALUE_NAMES[i], gravityValues[i]);
                    for (int i = 0; i < ORT_VALUE_NAMES.length; i++)
                        data.addProperty(ORT_VALUE_NAMES[i], orientationValues[i]);
                    for (int i = 0; i < MAG_VALUE_NAMES.length; i++)
                        data.addProperty(MAG_VALUE_NAMES[i], magneticValues[i]);

                    if (currentTimeMillis > lastTimeMillis + MIN_INTERVAL_MILLIS) {
                        lastTimeMillis = currentTimeMillis;
                        sendData(data);
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
        super.onStart();
        getSensorManager().registerListener(sensorListener, accelerometerSensor, getSensorDelay(sensorDelay));
        getSensorManager().registerListener(sensorListener, gravitySensor, getSensorDelay(sensorDelay));
        getSensorManager().registerListener(sensorListener, magneticSensor, getSensorDelay(sensorDelay));
    }

    @Override
    protected void onStop() {
        super.onStop();
        getSensorManager().unregisterListener(sensorListener);
    }

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
                Log.w(LogUtil.TAG, "Unknown accelerometerSensor delay value: " + specifiedSensorDelay);
            }
        }

        if (sensorDelay < 0) {
            sensorDelay = SensorManager.SENSOR_DELAY_FASTEST;
        }

        return sensorDelay;
    }
}
