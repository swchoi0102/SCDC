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



import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import com.google.gson.JsonObject;

import edu.mit.media.funf.probe.Probe.PassiveProbe;
import edu.mit.media.funf.probe.builtin.ProbeKeys.ApplicationsKeys;
import edu.mit.media.funf.time.TimeUtil;
import edu.mit.media.funf.util.LogUtil;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys;
import kr.ac.snu.imlab.scdc.util.SharedPrefsHandler;

//public class ApplicationsProbe extends ImpulseProbe implements PassiveProbe, ApplicationsKeys{
public class ApplicationsProbe extends ImpulseProbe implements ApplicationsKeys{
	
	private PackageManager pm;
	private long currentTime;

	@Override
	protected void onStart() {
		Log.d(SCDCKeys.LogKeys.DEB, "[ApplicationsProbe] onStart");
		super.onStart();

		currentTime = System.currentTimeMillis();
		long lastSavedTime = getLastSavedTime();

		if(currentTime > lastSavedTime + SCDCKeys.SharedPrefs.DEFAULT_IMPULSE_INTERVAL){
			pm = getContext().getPackageManager();
			List<ApplicationInfo> allApplications = pm.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
			List<ApplicationInfo> installedApplications = new ArrayList<ApplicationInfo>(pm.getInstalledApplications(0));
			List<ApplicationInfo> uninstalledApplications = getUninstalledApps(allApplications, installedApplications);
			for (ApplicationInfo info : installedApplications) {
				sendData(info, true, null);
			}
			for (ApplicationInfo info : uninstalledApplications) {
				sendData(info, false, null);
			}
			setLastSavedTime(currentTime);
		}

		disable();
	}

	@Override
	protected void onStop() {
		Log.d(SCDCKeys.LogKeys.DEB, "[ApplicationsProbe] onStop");
		super.onStop();
	}


	private void sendData(ApplicationInfo info, boolean installed, BigDecimal installedTimestamp) {
		JsonObject data = getGson().toJsonTree(info).getAsJsonObject();
		data.addProperty(TIMESTAMP, currentTime);
		data.addProperty(INSTALLED, installed);
		data.add(INSTALLED_TIMESTAMP, getGson().toJsonTree(installedTimestamp));
		sendData(data);
	}
	
	private static Set<String> getInstalledAppPackageNames(List<ApplicationInfo> installedApps) {
		HashSet<String> installedAppPackageNames = new HashSet<String>();
		for (ApplicationInfo info : installedApps) {
			installedAppPackageNames.add(info.packageName);
		}
		return installedAppPackageNames;
	}
	
	private static ArrayList<ApplicationInfo> getUninstalledApps(List<ApplicationInfo> allApplications, List<ApplicationInfo> installedApps) {
		Set<String> installedAppPackageNames = getInstalledAppPackageNames(installedApps);
		ArrayList<ApplicationInfo> uninstalledApps = new ArrayList<ApplicationInfo>();
		for (ApplicationInfo info : allApplications) {
			if (!installedAppPackageNames.contains(info.packageName)) {
				uninstalledApps.add(info);
			}
		}
		return uninstalledApps;
	}


	protected void setLastSavedTime(long lastSavedTime) {
		SharedPrefsHandler.getInstance(this.getContext(),
				SCDCKeys.Config.SCDC_PREFS, Context.MODE_PRIVATE).setCPLastSavedTime(SCDCKeys.SharedPrefs.APPLICATIONS_LOG_LAST_TIME, lastSavedTime);
	}

	protected long getLastSavedTime() {
		return SharedPrefsHandler.getInstance(this.getContext(),
				SCDCKeys.Config.SCDC_PREFS, Context.MODE_PRIVATE).getCPLastSavedTime(SCDCKeys.SharedPrefs.APPLICATIONS_LOG_LAST_TIME);
	}
}
