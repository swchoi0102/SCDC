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

import java.lang.reflect.Field;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.renderscript.Script;
import android.util.Log;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.Probe.ContinuousProbe;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.probe.Probe.PassiveProbe;
import edu.mit.media.funf.probe.Probe.RequiredFeatures;
import edu.mit.media.funf.probe.Probe.RequiredPermissions;
import edu.mit.media.funf.probe.builtin.ProbeKeys.LocationKeys;
import edu.mit.media.funf.time.DecimalTimeUnit;
import edu.mit.media.funf.util.NameGenerator;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys;

/**
 * Sends all location points gathered by system.
 * @author alangardner
 *
 */
@DisplayName("Continuous Location Probe")
@RequiredPermissions({android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION})
@RequiredFeatures("android.hardware.location")
@Schedule.DefaultSchedule(interval=1800)
public class LocationProbe extends Base implements ContinuousProbe, PassiveProbe, LocationKeys {

	@Configurable
	private boolean useGps = true;
	
	@Configurable
	private boolean useNetwork = true;
	
//	@Configurable
//	private boolean useCache = true;
	
	private Gson gson;
	private LocationManager mLocationManager;
	private LocationListener gpsListener = new ProbeLocationListener();
	private LocationListener networkListener = new ProbeLocationListener();
	private LocationListener passiveListener = new ProbeLocationListener();


	private long checkInterval = 3;
	private ProviderChecker providerChecker = new ProviderChecker();
	private long lastTimeMillis;
	private long lastGpsTimeMillis;
	private long lastNetTimeMillis;
	private boolean networkOn = false;

	private class ProviderChecker implements Runnable {
		@Override
		public void run() {
			getHandler().postDelayed(this, edu.mit.media.funf.time.TimeUtil.secondsToMillis(checkInterval));
			long currentTimeMillis = System.currentTimeMillis();

			Log.d(SCDCKeys.LogKeys.DEB, "[LocationProbe] networkOn: " + networkOn + ", curr: " + currentTimeMillis + ", last: " + lastTimeMillis + ", lastGps: " + lastGpsTimeMillis + ", lastNet: " + lastGpsTimeMillis);
			if (useGps && useNetwork){
				if (networkOn){
					if (currentTimeMillis <= lastGpsTimeMillis + edu.mit.media.funf.time.TimeUtil.secondsToMillis(checkInterval*2)){
						// turn off network when it's sufficient with gps only.
						mLocationManager.removeUpdates(networkListener);
						networkOn = false;
					} else if(currentTimeMillis > Math.max(lastTimeMillis, lastNetTimeMillis) + edu.mit.media.funf.time.TimeUtil.secondsToMillis(checkInterval*2)){
						// even if gps and network are on, location is unknown.
						generateUnknownData(currentTimeMillis, Math.max(lastTimeMillis, lastNetTimeMillis));
					}
				} else{
					if (currentTimeMillis > lastTimeMillis + edu.mit.media.funf.time.TimeUtil.secondsToMillis(checkInterval*2)){
						// turn on network when it's not sufficient with gps only.
						mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, (long) checkInterval, 0, networkListener);
						networkOn = true;
						lastNetTimeMillis = System.currentTimeMillis();
					}
				}
			} else if (useNetwork){
				if (currentTimeMillis > Math.max(lastTimeMillis, lastNetTimeMillis) + edu.mit.media.funf.time.TimeUtil.secondsToMillis(checkInterval*2)){
					// location is unknown.
					generateUnknownData(currentTimeMillis, Math.max(lastTimeMillis, lastNetTimeMillis));
				}
			} else if (useGps){
				if (currentTimeMillis > Math.max(lastTimeMillis, lastGpsTimeMillis) + edu.mit.media.funf.time.TimeUtil.secondsToMillis(checkInterval*2)){
					// location is unknown.
					generateUnknownData(currentTimeMillis, Math.max(lastTimeMillis, lastGpsTimeMillis));
				}
			}
		}

		public void endCurrentTask() {
			Log.d(SCDCKeys.LogKeys.DEB, "[LocationProbe] End task");
			reset();
		}

		public void reset() {
			Log.d(SCDCKeys.LogKeys.DEB, "[LocationProbe] Reset");
			networkOn = false;
		}
	}

	protected void generateUnknownData(long timeMillis, long comparedTimeMillis) {
		Log.d(SCDCKeys.LogKeys.DEB, "[LocationProbe] Generate unknown data!");
		JsonObject data = new JsonObject();
		data.addProperty("mAccuracy", -1);
		data.addProperty("mAltitude", -1);
		data.addProperty("mBearing", -1);
		data.addProperty("mElapsedRealtimeNanos", -1);
		data.addProperty("mExtras", "unknown");
		data.addProperty("mHasAccuracy", false);
		data.addProperty("mHasAltitude", false);
		data.addProperty("mHasBearing", false);
		data.addProperty("mHasSpeed", false);
		data.addProperty("mIsFromMockProvider", false);
		data.addProperty("mLatitude", -1);
		data.addProperty("mLongitude", -1);
		data.addProperty("mProvider", -1);
		data.addProperty("mSpeed", -1);
		data.addProperty("mTime", -1);
		data.addProperty(ProbeKeys.BaseProbeKeys.TIMESTAMP, edu.mit.media.funf.time.TimeUtil.getTimestamp());
		data.addProperty("rep", true);

		// check one more time
		if (timeMillis > comparedTimeMillis + edu.mit.media.funf.time.TimeUtil.secondsToMillis(checkInterval)){
			sendData(data);
		}
	}

	
	@Override
	protected void onEnable() {
		Log.d(SCDCKeys.LogKeys.DEB, "[LocationProbe] onEnable()");
		super.onEnable();
		gson = getGsonBuilder().addSerializationExclusionStrategy(new LocationExclusionStrategy()).create();
		mLocationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
		String passiveProvider = getPassiveProvider();
		if (passiveProvider != null) {
			mLocationManager.requestLocationUpdates(getPassiveProvider(), 0, 0, passiveListener);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(SCDCKeys.LogKeys.DEB, "[LocationProbe] onStart()");
		lastTimeMillis = System.currentTimeMillis();
		if (useGps) {
			mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, checkInterval, 0, gpsListener);
			lastGpsTimeMillis = lastTimeMillis;
		}
		else if (useNetwork) {
			mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, checkInterval, 0, gpsListener);
			lastNetTimeMillis = lastTimeMillis;
		}
//		if (useCache) {
//			gpsListener.onLocationChanged(mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
//			gpsListener.onLocationChanged(mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
//		}
		else{
			stop();
		}
		getHandler().post(providerChecker);
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.d(SCDCKeys.LogKeys.DEB, "[LocationProbe] onStop()");
		if (useGps){
			mLocationManager.removeUpdates(gpsListener);
			if (useNetwork && networkOn) mLocationManager.removeUpdates(networkListener);
		} else{
			if (useNetwork) mLocationManager.removeUpdates(networkListener);
		}
		onPause();
	}

	protected void onPause() {
		Log.d(SCDCKeys.LogKeys.DEB, "[LocationProbe] onPause()");
		getHandler().removeCallbacks(providerChecker);
		providerChecker.endCurrentTask();
	}

	@Override
	protected void onDisable() {
		super.onDisable();
		Log.d(SCDCKeys.LogKeys.DEB, "[LocationProbe] onDisable()");
		mLocationManager.removeUpdates(passiveListener);
	}

	private class ProbeLocationListener implements LocationListener{

		@Override
		public void onLocationChanged(Location location) {
			if (location != null) {
				String provider = location.getProvider();
//				if (provider == null
//						|| (useGps && LocationManager.GPS_PROVIDER.equals(provider))
//						|| (useNetwork && LocationManager.NETWORK_PROVIDER.equals(provider))) {
//					JsonObject data = gson.toJsonTree(location).getAsJsonObject();
//					data.addProperty(TIMESTAMP, DecimalTimeUnit.MILLISECONDS.toSeconds(data.get("mTime").getAsBigDecimal()));
//					sendData(gson.toJsonTree(location).getAsJsonObject());
//				}
				JsonObject data = gson.toJsonTree(location).getAsJsonObject();
				lastTimeMillis = data.get("mTime").getAsLong();
				if (provider.equals(LocationManager.GPS_PROVIDER)) lastGpsTimeMillis = lastTimeMillis;
				if (provider.equals(LocationManager.NETWORK_PROVIDER)) lastNetTimeMillis = lastTimeMillis;
				data.addProperty(TIMESTAMP, DecimalTimeUnit.MILLISECONDS.toSeconds(lastTimeMillis));
				sendData(gson.toJsonTree(location).getAsJsonObject());
				Log.d(SCDCKeys.LogKeys.DEB, "[LocationProbe] location given by provider: " + provider + ", at: " + lastTimeMillis);
			}
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onProviderDisabled(String provider) {
		}
		
	}
	
    public class LocationExclusionStrategy implements ExclusionStrategy {

        public boolean shouldSkipClass(Class<?> cls) {
            return false;
        }

        public boolean shouldSkipField(FieldAttributes f) {
        	String name = f.getName();
            return (f.getDeclaringClass() == Location.class && 
            			(name.equals("mResults") 
            				|| name.equals("mDistance") 
            				|| name.equals("mInitialBearing") 
            				|| name.equals("mLat1") 
            				|| name.equals("mLat2") 
            				|| name.equals("mLon1") 
            				|| name.equals("mLon2") 
            				|| name.equals("mLon2") 
            				)
            		);
        }
    }
	
	/**
	 * Supporting API level 7 which does not have PASSIVE provider
	 * @return
	 */
	private String getPassiveProvider() {
		try {
			Field passiveProviderField = LocationManager.class.getDeclaredField("PASSIVE_PROVIDER");
			return (String)passiveProviderField.get(null);
		} catch (SecurityException e) {
		} catch (NoSuchFieldException e) {
		} catch (IllegalArgumentException e) {
		} catch (IllegalAccessException e) {
		}
		return null;
	}
	
}
