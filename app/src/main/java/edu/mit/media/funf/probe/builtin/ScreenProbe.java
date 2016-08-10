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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import com.google.gson.JsonObject;

import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.Probe.ContinuousProbe;
import edu.mit.media.funf.probe.Probe.Description;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.probe.builtin.ProbeKeys.ScreenKeys;
import edu.mit.media.funf.time.DecimalTimeUnit;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys;

@DisplayName("Screen On/Off")
@Description("Records when the screen turns off and on.")
@Schedule.DefaultSchedule(interval=0, duration=0, opportunistic=true)
public class ScreenProbe extends Base implements ContinuousProbe, ScreenKeys  {

	private BroadcastReceiver screenReceiver;
	private PowerManager pm;

	private double checkInterval = 1.0;
	private ScreenChecker screenChecker = new ScreenChecker();
	private long lastTimeMillis;
	private boolean lastScreenOn;
	private boolean replicateOn = false;

//	@Override
//	public void sendLastData() {
//
//	}

	private class ScreenChecker implements Runnable {
		@Override
		public void run() {
			getHandler().postDelayed(this, edu.mit.media.funf.time.TimeUtil.secondsToMillis(checkInterval));
			long currentTimeMillis = System.currentTimeMillis();
			if (replicateOn){
				if (currentTimeMillis > lastTimeMillis + edu.mit.media.funf.time.TimeUtil.secondsToMillis(checkInterval)){
//					Log.d(SCDCKeys.LogKeys.DEB, "[Sensor] curr: " + currentTimeMillis + ", last: " + lastTimeMillis);
					replicateData(lastScreenOn, currentTimeMillis);
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

	protected void replicateData(boolean so, long timeMillis) {
//		Log.d(SCDCKeys.LogKeys.DEB, "[Sensor] Replicate data!");
		JsonObject data = new JsonObject();
		data.addProperty(SCREEN_ON, so);
		data.addProperty(ProbeKeys.BaseProbeKeys.TIMESTAMP, edu.mit.media.funf.time.TimeUtil.getTimestamp());
//		data.addProperty("rep", true);

		// check one more time
		if (timeMillis > lastTimeMillis + edu.mit.media.funf.time.TimeUtil.secondsToMillis(checkInterval)){
			sendData(data);
		}
	}
	
	@Override
	protected void onEnable() {
		pm = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
		screenReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				JsonObject data = new JsonObject();

				final String action = intent.getAction();
				if (Intent.ACTION_SCREEN_OFF.equals(action)
						|| Intent.ACTION_SCREEN_ON.equals(action)) {
					lastTimeMillis = System.currentTimeMillis();
					lastScreenOn = Intent.ACTION_SCREEN_ON.equals(action);
					data.addProperty(TIMESTAMP, DecimalTimeUnit.MILLISECONDS.toSeconds(lastTimeMillis));
					data.addProperty(SCREEN_ON, lastScreenOn);
					sendData(data);
					replicateOn = true;
				}
			}
		};
	}

	@Override
	protected void onStart() {
		Log.d(SCDCKeys.LogKeys.DEB, "[ScreenProbe] onStart");
		super.onStart();
		initializeScreenStatus();

		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		getContext().registerReceiver(screenReceiver, filter);
		onContinue();
	}

	protected void onContinue() {
//		Log.d(SCDCKeys.LogKeys.DEB, "[Sensor] onContinue");
		getHandler().post(screenChecker);
	}

	@Override
	protected void onStop() {
		Log.d(SCDCKeys.LogKeys.DEB, "[ScreenProbe] onStop");
		super.onStop();
		getContext().unregisterReceiver(screenReceiver);
		onPause();
	}

	protected void onPause() {
//		Log.d(SCDCKeys.LogKeys.DEB, "[Sensor] onPause");
		getHandler().removeCallbacks(screenChecker);
		screenChecker.endCurrentTask();
	}

	@Override
	protected void onDisable() {
		screenChecker.reset();
	}

	private void initializeScreenStatus() {
		JsonObject currentScreenStatus = new JsonObject();

		lastTimeMillis = System.currentTimeMillis();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
			lastScreenOn = pm.isInteractive();
		}
		else{
			lastScreenOn = pm.isScreenOn();
		}
		currentScreenStatus.addProperty(TIMESTAMP, DecimalTimeUnit.MILLISECONDS.toSeconds(lastTimeMillis));
		currentScreenStatus.addProperty(SCREEN_ON, lastScreenOn);

		sendData(currentScreenStatus);
		replicateOn = true;
	}

//	@Override
//	protected boolean isWakeLockedWhileRunning() {
//		return false;
//	}
}
