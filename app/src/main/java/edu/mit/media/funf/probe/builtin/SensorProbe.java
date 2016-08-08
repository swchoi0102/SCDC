/**
 *
 * Funf: Open Sensing Framework
 * Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland.
 * Acknowledgments: Alan Gardner
 * Contact: nadav@media.mit.edu
 *
 * This file is part of Funf.
 *
 * Funf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * Funf is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Funf. If not, see <http://www.gnu.org/licenses/>.
 *
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
import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.Probe.ContinuousProbe;
import edu.mit.media.funf.probe.builtin.ProbeKeys.SensorKeys;
import edu.mit.media.funf.time.DecimalTimeUnit;
import edu.mit.media.funf.time.TimeUtil;
import edu.mit.media.funf.util.LogUtil;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys;

@Schedule.DefaultSchedule(interval=SensorProbe.DEFAULT_PERIOD, duration=SensorProbe.DEFAULT_DURATION)
public abstract class SensorProbe extends Base implements ContinuousProbe, SensorKeys {

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
	private Sensor sensor;
	private SensorEventListener sensorListener;

	private SensorChecker sensorChecker = new SensorChecker();
	private long lastTimeMillis;
	private float[] lastValues;
	private int lastAccuracy;
	private boolean replicateOn = false;

	private class SensorChecker implements Runnable {
		@Override
		public void run() {
			getHandler().postDelayed(this, TimeUtil.secondsToMillis(checkInterval));
			long currentTimeMillis = System.currentTimeMillis();
			if (lastValues != null && replicateOn){
				if (currentTimeMillis > lastTimeMillis + TimeUtil.secondsToMillis(checkInterval)){
//					Log.d(SCDCKeys.LogKeys.DEB, "[Sensor] curr: " + currentTimeMillis + ", last: " + lastTimeMillis);
					replicateData(lastValues, currentTimeMillis, lastAccuracy);
				}
			}
		}

		public void endCurrentTask() {
//			Log.d(SCDCKeys.LogKeys.DEB, "[Sensor] End replicate task");
			reset();
		}

		public void reset() {
//			Log.d(SCDCKeys.LogKeys.DEB, "[Sensor] Reset replicate task");
			lastValues = null;
			replicateOn = false;
		}
	}

	protected void replicateData(float[] vArr, long timeMillis, int acc) {
//		Log.d(SCDCKeys.LogKeys.DEB, "[Sensor] Replicate data!");
		JsonObject data = new JsonObject();
		data.addProperty(TIMESTAMP, DecimalTimeUnit.MILLISECONDS.toSeconds(timeMillis));
		data.addProperty(ACCURACY, acc);
		data.addProperty("rep", true);
		final String[] valueNames = getValueNames();

		for (int i = 0; i < vArr.length; i++) {
			String valueName = valueNames[i];
			data.addProperty(valueName, vArr[i]);
		}

		// check one more time
		if (timeMillis > lastTimeMillis + TimeUtil.secondsToMillis(checkInterval)){
			sendData(data);
		}
	}


	@Override
	protected void onEnable() {
		super.onEnable();
		sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
		sensor = sensorManager.getDefaultSensor(getSensorType());
		final String[] valueNames = getValueNames();
		lastValues = new float[valueNames.length];
		sensorListener = new SensorEventListener() {

			@Override
			public void onSensorChanged(SensorEvent event) {
				JsonObject data = new JsonObject();
        // FIXME: TIMESTAMP for all SensorProbe's
				// data.addProperty(TIMESTAMP, TimeUtil.uptimeNanosToTimestamp(event.timestamp));
				lastTimeMillis = System.currentTimeMillis();
        		data.addProperty(TIMESTAMP, DecimalTimeUnit.MILLISECONDS.toSeconds(lastTimeMillis));
				data.addProperty(ACCURACY, event.accuracy);
				int valuesLength = Math.min(event.values.length, valueNames.length);
				for (int i = 0; i < valuesLength; i++) {
					String valueName = valueNames[i];
					data.addProperty(valueName, event.values[i]);
					lastValues[i] = event.values[i];
					lastAccuracy = event.accuracy;
				}
				sendData(data);
				replicateOn = true;
			}

			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
			}
		};
	}

	@Override
	protected void onStart() {
		super.onStart();
		getSensorManager().registerListener(sensorListener,sensor, getSensorDelay(sensorDelay));
		onContinue();
	}

	protected void onContinue() {
//		Log.d(SCDCKeys.LogKeys.DEB, "[Sensor] onContinue");
		getHandler().post(sensorChecker);
	}

	@Override
	protected void onStop() {
//		Log.d(SCDCKeys.LogKeys.DEB, "[Sensor] onStop");
		getSensorManager().unregisterListener(sensorListener);
		onPause();
	}

	protected void onPause() {
//		Log.d(SCDCKeys.LogKeys.DEB, "[Sensor] onPause");
		getHandler().removeCallbacks(sensorChecker);
		sensorChecker.endCurrentTask();
	}

	@Override
	protected void onDisable() {
//		Log.d(SCDCKeys.LogKeys.DEB, "[Sensor] onDisable");
		sensorChecker.reset();
	}

	protected SensorManager getSensorManager() {
		if (sensorManager == null) {
			sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
		}
		return sensorManager;
	}

	protected int getSensorDelay(String specifiedSensorDelay) {
		int sensorDelay = -1;
		JsonElement el =  getGson().toJsonTree(specifiedSensorDelay);
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

	public abstract int getSensorType();
	public abstract String[] getValueNames();
}
