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

import com.google.gson.JsonObject;

import edu.mit.media.funf.probe.builtin.ProbeKeys.AndroidInfoKeys;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys;
import kr.ac.snu.imlab.scdc.util.SharedPrefsHandler;

public class AndroidInfoProbe extends ImpulseProbe implements AndroidInfoKeys {

	@Override
	protected void onStart() {
		super.onStart();

		long currentTime = System.currentTimeMillis();
		long lastSavedTime = getLastSavedTime();

		if(currentTime > lastSavedTime + SCDCKeys.SharedPrefs.DEFAULT_IMPULSE_INTERVAL){
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
			setLastSavedTime(currentTime);
		}
		stop();
	}

	protected void setLastSavedTime(long lastSavedTime) {
		SharedPrefsHandler.getInstance(this.getContext(),
				SCDCKeys.Config.SCDC_PREFS, Context.MODE_PRIVATE).setCPLastSavedTime(SCDCKeys.SharedPrefs.ANDROID_INFO_LOG_LAST_TIME, lastSavedTime);
	}

	protected long getLastSavedTime() {
		return SharedPrefsHandler.getInstance(this.getContext(),
				SCDCKeys.Config.SCDC_PREFS, Context.MODE_PRIVATE).getCPLastSavedTime(SCDCKeys.SharedPrefs.ANDROID_INFO_LOG_LAST_TIME);
	}
}
