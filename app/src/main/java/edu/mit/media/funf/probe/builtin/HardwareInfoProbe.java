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

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.Log;

import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe.RequiredPermissions;
import edu.mit.media.funf.probe.builtin.ProbeKeys.HardwareInfoKeys;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys;
import kr.ac.snu.imlab.scdc.util.SharedPrefsHandler;

@Schedule.DefaultSchedule(interval=604800)
@RequiredPermissions({android.Manifest.permission.ACCESS_WIFI_STATE, android.Manifest.permission.BLUETOOTH, android.Manifest.permission.READ_PHONE_STATE})
public class HardwareInfoProbe extends ImpulseProbe implements HardwareInfoKeys {

	public HardwareInfoProbe(){
		lastCollectTimeKey = SCDCKeys.SharedPrefs.HARDWARE_INFO_COLLECT_LAST_TIME;
		lastCollectTimeTempKey = SCDCKeys.SharedPrefs.HARDWARE_INFO_COLLECT_TEMP_LAST_TIME;
	}

	@Override
	protected void onStart() {
		super.onStart();

		long currentTime = System.currentTimeMillis();
		if(itIsTimeToStart()){
			Log.d(SCDCKeys.LogKeys.DEB, "[" + probeName + "] It is time to start!!!");
			sendData(getGson().toJsonTree(getData()).getAsJsonObject());
			setTempLastCollectTime(currentTime);
		} else {
			Log.d(SCDCKeys.LogKeys.DEB, "[" + probeName + "] may be next time..");
		}
		stop();
	}

	@Override
	protected boolean itIsTimeToStart() {
		// Is it first time?
//		long lastSavedTime = getLastCollectTime();
		long tempLastSavedTime = getTempLastCollectTime();
//		boolean firstTime = (lastSavedTime == 0L && tempLastSavedTime == 0L);
		boolean firstTime = tempLastSavedTime == 0L;
		Log.d(SCDCKeys.LogKeys.DEB, "[" + probeName + "] is it first time?: " + firstTime);
		return firstTime;

//		// Is it 24 hours passed from the last collection?
//		long currentTime = System.currentTimeMillis();
//		boolean passed24Hours = currentTime > lastSavedTime + SCDCKeys.SharedPrefs.DEFAULT_IMPULSE_INTERVAL;
//
//		// Is it sleeping context?
//		//		FIXME: sleeping label ID is just assigned as integer value not as a variable
//		long startLoggingTime = SharedPrefsHandler.getInstance(this.getContext(),
//				SCDCKeys.Config.SCDC_PREFS, Context.MODE_PRIVATE).getStartLoggingTime(0);
//		boolean sleepingContext = startLoggingTime != -1;
//
//		// Is it 2 hours passed from the start logging time?
//		boolean labeling2Hours = currentTime > startLoggingTime + 7200000L;
//
//		return firstTime || passed24Hours && sleepingContext && labeling2Hours;
	}

	private Bundle getData() {
		Context context = getContext();
		Bundle data = new Bundle();
		String bluetoothMac = getBluetoothMac();
		if (bluetoothMac != null) {
			data.putString(BLUETOOTH_MAC, bluetoothMac);
		}
		data.putString(ANDROID_ID, Secure.getString(context.getContentResolver(), Secure.ANDROID_ID));
		data.putString(BRAND, Build.BRAND);
		data.putString(MODEL, Build.MODEL);
		data.putString(DEVICE_ID, ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId());
		return data;
	}

	private String getBluetoothMac() {
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		return (adapter != null) ? adapter.getAddress() : null;
	}
}
