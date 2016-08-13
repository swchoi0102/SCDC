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
import android.os.Build;
import android.util.Log;

import com.google.gson.JsonObject;

import edu.mit.media.funf.probe.builtin.ProbeKeys.AndroidInfoKeys;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys;
import kr.ac.snu.imlab.scdc.util.SharedPrefsHandler;

public class AndroidInfoProbe extends ImpulseProbe implements AndroidInfoKeys {

	public AndroidInfoProbe(){
		lastCollectTimeKey = SCDCKeys.SharedPrefs.ANDROID_INFO_COLLECT_LAST_TIME;
		lastCollectTimeTempKey = SCDCKeys.SharedPrefs.ANDROID_INFO_COLLECT_TEMP_LAST_TIME;
	}

	@Override
	protected void onStart() {
		super.onStart();

		long currentTime = System.currentTimeMillis();
		if(itIsTimeToStart()){
			Log.d(SCDCKeys.LogKeys.DEB, "[" + probeName + "] It is time to start!!!");
			JsonObject data = new JsonObject();
			data.addProperty(FIRMWARE_VERSION, Build.VERSION.RELEASE);
			data.addProperty(BUILD_NUMBER,
					Build.PRODUCT + "-" + Build.TYPE
							+ " " + Build.VERSION.RELEASE
							+ " " + Build.ID
							+ " " + Build.VERSION.INCREMENTAL
							+ " " + Build.TAGS);
			data.addProperty(SDK, Build.VERSION.SDK_INT);
			sendData(data);
			setTempLastCollectTime(currentTime);
		} else {
			Log.d(SCDCKeys.LogKeys.DEB, "[" + probeName + "] may be next time..");
		}
		stop();
	}

	@Override
	protected boolean itIsTimeToStart() {
		// Is it first time?
		long lastSavedTime = getLastCollectTime();
		long tempLastSavedTime = getTempLastCollectTime();
		boolean firstTime = (lastSavedTime == 0L && tempLastSavedTime == 0L);
		Log.d(SCDCKeys.LogKeys.DEB, "[" + probeName + "] is it first time?: " + firstTime);

		// Is it 24 hours passed from the last collection?
		long currentTime = System.currentTimeMillis();
		boolean passed24Hours = currentTime > lastSavedTime + SCDCKeys.SharedPrefs.DEFAULT_IMPULSE_INTERVAL;

		// Is it sleeping context?
		//		FIXME: sleeping label ID is just assigned as integer value not as a variable
		long startLoggingTime = SharedPrefsHandler.getInstance(this.getContext(),
				SCDCKeys.Config.SCDC_PREFS, Context.MODE_PRIVATE).getStartLoggingTime(0);
		boolean sleepingContext = startLoggingTime != -1;

		// Is it 2 hours passed from the start logging time?
		boolean labeling2Hours = currentTime > startLoggingTime + 1800000L;

		return firstTime || passed24Hours && sleepingContext && labeling2Hours;
	}
}
