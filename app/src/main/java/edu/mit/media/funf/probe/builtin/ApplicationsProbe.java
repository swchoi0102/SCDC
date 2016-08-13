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

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.google.gson.JsonObject;

import edu.mit.media.funf.probe.builtin.ProbeKeys.ApplicationsKeys;
import edu.mit.media.funf.time.DecimalTimeUnit;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys;

//public class ApplicationsProbe extends ImpulseProbe implements PassiveProbe, ApplicationsKeys{
public class ApplicationsProbe extends ImpulseProbe implements ApplicationsKeys{
	
	private PackageManager pm;
	private long currentTime;

	public ApplicationsProbe(){
		lastCollectTimeKey = SCDCKeys.SharedPrefs.APPLICATIONS_COLLECT_LAST_TIME;
		lastCollectTimeTempKey = SCDCKeys.SharedPrefs.APPLICATIONS_COLLECT_TEMP_LAST_TIME;
	}

	@Override
	protected void onStart() {
		super.onStart();

		currentTime = System.currentTimeMillis();
		if(itIsTimeToStart()){
			Log.d(SCDCKeys.LogKeys.DEB, "[" + probeName + "] It is time to start!!!");
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
			setTempLastCollectTime(currentTime);
		} else {
			Log.d(SCDCKeys.LogKeys.DEB, "[" + probeName + "] may be next time..");
		}

		disable();
	}

	private void sendData(ApplicationInfo info, boolean installed, BigDecimal installedTimestamp) {
		JsonObject data = getGson().toJsonTree(info).getAsJsonObject();
		data.addProperty(TIMESTAMP, DecimalTimeUnit.MILLISECONDS.toSeconds(currentTime));
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
}
