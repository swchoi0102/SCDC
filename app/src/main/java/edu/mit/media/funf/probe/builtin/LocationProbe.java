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
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.Probe.ContinuousProbe;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.probe.Probe.PassiveProbe;
import edu.mit.media.funf.probe.Probe.RequiredFeatures;
import edu.mit.media.funf.probe.Probe.RequiredPermissions;
import edu.mit.media.funf.probe.builtin.ProbeKeys.LocationKeys;
import edu.mit.media.funf.time.DecimalTimeUnit;
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
public class LocationProbe extends Base implements ContinuousProbe, LocationKeys {

//	@Configurable
//	private boolean useGps = true;
//
//	@Configurable
//	private boolean useNetwork = false;
//
//	@Configurable
//	private boolean useCache = true;

	private Gson gson;
	private LocationManager mLocationManager;
	private LocationListener locationListener = new ProbeLocationListener();
//	private LocationListener passiveListener = new ProbeLocationListener();


	private long checkInterval = 4;
	private LocationChecker locationChecker = new LocationChecker();
	private long lastTimeMillis;
	private long lastGpsTimeMillis;
	private long lastNetTimeMillis;
	private JsonObject lastData;
	private boolean replicateOn = false;

	@Override
	public void sendLastData() {

	}

	private class LocationChecker implements Runnable {
		@Override
		public void run() {
			getHandler().postDelayed(this, edu.mit.media.funf.time.TimeUtil.secondsToMillis(checkInterval));
			long currentTimeMillis = System.currentTimeMillis();

			Log.d(SCDCKeys.LogKeys.DEB, "[LocationProbe] replicateOn: " + replicateOn + ", curr: " + currentTimeMillis + ", last: " + lastTimeMillis);
			if (currentTimeMillis > lastTimeMillis + edu.mit.media.funf.time.TimeUtil.secondsToMillis(checkInterval*2)){
				// even if gps and network are on, location is unknown.
				if (replicateOn) generateReplicateData(currentTimeMillis);
			}
		}

		public void endCurrentTask() {
			Log.d(SCDCKeys.LogKeys.DEB, "[LocationProbe] End task");
			reset();
		}

		public void reset() {
			Log.d(SCDCKeys.LogKeys.DEB, "[LocationProbe] Reset");
			replicateOn = false;
		}
	}

	protected void generateReplicateData(long timeMillis) {
		Log.d(SCDCKeys.LogKeys.DEB, "[LocationProbe] Generate unknown data!");

		JsonObject data = new JsonObject();
		data.addProperty("mAccuracy", -1);
		data.addProperty("mAltitude", lastData.get("mAltitude").getAsFloat());
		data.addProperty("mBearing", lastData.get("mBearing").getAsFloat());
		data.addProperty("mElapsedRealtimeNanos", lastData.get("mElapsedRealtimeNanos").getAsLong());
		data.addProperty("mExtras", "replicate");
		data.addProperty("mHasAccuracy", false);
		data.addProperty("mHasAltitude", lastData.get("mHasAltitude").getAsBoolean());
		data.addProperty("mHasBearing", lastData.get("mHasBearing").getAsBoolean());
		data.addProperty("mHasSpeed", lastData.get("mHasSpeed").getAsBoolean());
		data.addProperty("mIsFromMockProvider", lastData.get("mIsFromMockProvider").getAsBoolean());
		data.addProperty("mLatitude", lastData.get("mLatitude").getAsFloat());
		data.addProperty("mLongitude", lastData.get("mLongitude").getAsFloat());
		data.addProperty("mProvider", lastData.get("mProvider").getAsString());
		data.addProperty("mSpeed", lastData.get("mSpeed").getAsFloat());
		data.addProperty("mTime", lastData.get("mTime").getAsLong());
		data.addProperty(ProbeKeys.BaseProbeKeys.TIMESTAMP, edu.mit.media.funf.time.TimeUtil.getTimestamp());

		// check one more time
		if (timeMillis > lastTimeMillis + edu.mit.media.funf.time.TimeUtil.secondsToMillis(checkInterval*2)){
			sendData(data);
		}
	}


	@Override
	protected void onEnable() {
		Log.d(SCDCKeys.LogKeys.DEB, "[LocationProbe] onEnable()");
		super.onEnable();
		gson = getGsonBuilder().addSerializationExclusionStrategy(new LocationExclusionStrategy()).create();
		mLocationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
//		String passiveProvider = getPassiveProvider();
//		if (passiveProvider != null) {
//			mLocationManager.requestLocationUpdates(getPassiveProvider(), 0, 0, passiveListener);
//		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(SCDCKeys.LogKeys.DEB, "[LocationProbe] onStart()");
		lastTimeMillis = System.currentTimeMillis();
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, checkInterval, 0f, locationListener);
		mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, checkInterval, 0f, locationListener);
//		if (useGps) {
//			mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, checkInterval, 0.5f, locationListener);
//			lastGpsTimeMillis = lastTimeMillis;
//		}
//		if (useNetwork) {
//			mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, checkInterval, 0.5f, locationListener);
//			lastNetTimeMillis = lastTimeMillis;
//		}
//		if (useCache) {
//			locationListener.onLocationChanged(mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
//			locationListener.onLocationChanged(mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
//		}
//		if(!useGps && !useNetwork){
//			stop();
//		}
		getHandler().post(locationChecker);
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.d(SCDCKeys.LogKeys.DEB, "[LocationProbe] onStop()");
		mLocationManager.removeUpdates(locationListener);
		onPause();
	}

	protected void onPause() {
		Log.d(SCDCKeys.LogKeys.DEB, "[LocationProbe] onPause()");
		getHandler().removeCallbacks(locationChecker);
		locationChecker.endCurrentTask();
	}

	@Override
	protected void onDisable() {
		super.onDisable();
		Log.d(SCDCKeys.LogKeys.DEB, "[LocationProbe] onDisable()");
//		mLocationManager.removeUpdates(passiveListener);
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
				Log.d(SCDCKeys.LogKeys.DEB, "[LocationProbe] location given by provider: " + provider + ", at: " + lastTimeMillis);
				if (provider.equals(LocationManager.GPS_PROVIDER)){
					lastGpsTimeMillis = lastTimeMillis;
					Log.d(SCDCKeys.LogKeys.DEB, "[LocationProbe] lastGps updated: " + lastGpsTimeMillis);
				}
				if (provider.equals(LocationManager.NETWORK_PROVIDER) && (lastTimeMillis > lastGpsTimeMillis + edu.mit.media.funf.time.TimeUtil.secondsToMillis(checkInterval*2))){
					// write location given by network provider, when no location is given by gps provider for a long time.
					lastNetTimeMillis = lastTimeMillis;
					Log.d(SCDCKeys.LogKeys.DEB, "[LocationProbe] lastNet updated: " + lastNetTimeMillis);
				}
				data.addProperty(TIMESTAMP, DecimalTimeUnit.MILLISECONDS.toSeconds(lastTimeMillis));
				sendData(gson.toJsonTree(location).getAsJsonObject());
				lastData = data;
				replicateOn = true;
//				Log.d(SCDCKeys.LogKeys.DEB, "[LocationProbe] data sended: " + lastNetTimeMillis);
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
