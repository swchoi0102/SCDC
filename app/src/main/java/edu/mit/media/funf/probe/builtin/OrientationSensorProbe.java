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
import edu.mit.media.funf.util.LogUtil;

@Description("Records a three dimensional vector of the magnetic field.")
@RequiredFeatures("android.hardware.sensor.gyroscope")
@Schedule.DefaultSchedule(interval = 180, duration = 15)
public class OrientationSensorProbe extends Probe.Base implements Probe.ContinuousProbe, ProbeKeys.SensorKeys, OrientationSensorKeys {

    public static final double DEFAULT_PERIOD = 61;
    public static final double DEFAULT_DURATION = 60;

    @Configurable
    private String sensorDelay = SENSOR_DELAY_GAME;
    private long lastTimeMillis;
    private final long MIN_INTERVAL_MILLIS = 2;

    public static final String
            SENSOR_DELAY_FASTEST = "FASTEST",
            SENSOR_DELAY_GAME = "GAME",
            SENSOR_DELAY_UI = "UI",
            SENSOR_DELAY_NORMAL = "NORMAL";

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private SensorEventListener sensorListener;

    @Override
    protected void onEnable() {
        super.onEnable();
        sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        final String[] valueNames = getValueNames();
        lastTimeMillis = 0;
        sensorListener = new SensorEventListener() {

            float[] mAccelerometer = new float[3];
            float[] mGeomagnetic = new float[3];
            boolean acceleInitialized = false;
            boolean magneticInitialized = false;

            @Override
            public void onSensorChanged(SensorEvent event) {
                long currentTimeMillis = System.currentTimeMillis();
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    System.arraycopy(event.values, 0, mAccelerometer, 0, mAccelerometer.length);
                    acceleInitialized = true;
                }
                if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                    System.arraycopy(event.values, 0, mGeomagnetic, 0, mGeomagnetic.length);
                    magneticInitialized = true;
                }
                if (acceleInitialized && magneticInitialized) {
                    float R[] = new float[9];
                    float I[] = new float[9];
                    boolean success = SensorManager.getRotationMatrix(R, I, mAccelerometer, mGeomagnetic);
                    if (success) {
                        JsonObject data = new JsonObject();
                        data.addProperty(TIMESTAMP, DecimalTimeUnit.MILLISECONDS.toSeconds(currentTimeMillis));
                        data.addProperty(ACCURACY, event.accuracy);

                        float orientation[] = new float[3];
                        SensorManager.getOrientation(R, orientation);

                        for (int i = 0; i < valueNames.length; i++) {
                            String valueName = valueNames[i];
                            float tempValue = (float) Math.toDegrees(orientation[i]);
                            data.addProperty(valueName, tempValue);
                        }
                        if (currentTimeMillis > lastTimeMillis + MIN_INTERVAL_MILLIS) {
                            lastTimeMillis = currentTimeMillis;
                            sendData(data);
                        }
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
        getSensorManager().registerListener(sensorListener, accelerometer, getSensorDelay(sensorDelay));
        getSensorManager().registerListener(sensorListener, magnetometer, getSensorDelay(sensorDelay));
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

