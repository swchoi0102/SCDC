/**
 * Funf: Open Sensing Framework
 * Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland.
 * Acknowledgments: Alan Gardner
 * Contact: nadav@media.mit.edu
 * <p/>
 * This file is part of Funf.
 * <p/>
 * Funf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * <p/>
 * Funf is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public
 * License along with Funf. If not, see <http://www.gnu.org/licenses/>.
 */
package edu.mit.media.funf.probe.builtin;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe.ContinuousProbe;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.probe.Probe.RequiredFeatures;
import edu.mit.media.funf.probe.Probe.RequiredPermissions;
import edu.mit.media.funf.probe.builtin.ProbeKeys.LocationKeys;
import edu.mit.media.funf.time.DecimalTimeUnit;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys;
import kr.ac.snu.imlab.scdc.service.probe.InsensitiveProbe;

/**
 * Sends all location points gathered by system.
 *
 * @author alangardner
 */
@DisplayName("Continuous Location Probe")
@RequiredPermissions({android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION})
@RequiredFeatures("android.hardware.location")
@Schedule.DefaultSchedule(interval = 1800)
public class LocationProbe extends InsensitiveProbe implements ContinuousProbe, LocationKeys {

    private Gson gson;
    private LocationManager mLocationManager;
    private LocationListener locationListener;
    private final long checkInterval = 3;
    private BigDecimal lastGpsTimestamp;


    @Override
    protected void onEnable() {
//		Log.d(SCDCKeys.LogKeys.DEB, "[LocationProbe] onEnable()");
        super.onEnable();
        gson = getGsonBuilder().addSerializationExclusionStrategy(new LocationExclusionStrategy()).create();
        mLocationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        locationListener = new ProbeLocationListener();
    }

    @Override
    protected void onStart() {
        Log.d(SCDCKeys.LogKeys.DEB, "[LocationProbe] onStart");
        super.onStart();
        initializeLocation();
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, checkInterval, 0f, locationListener);
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, checkInterval, 0f, locationListener);
    }

    @Override
    protected void onStop() {
        Log.d(SCDCKeys.LogKeys.DEB, "[LocationProbe] onStop()");
        super.onStop();
        mLocationManager.removeUpdates(locationListener);
    }

    protected void initializeLocation() {
        Log.d(SCDCKeys.LogKeys.DEB, "[LocationProbe] initializeLocation");
        Long currTime = System.currentTimeMillis();
        JsonObject data = new JsonObject();
        lastGpsTimestamp = DecimalTimeUnit.MILLISECONDS.toSeconds(currTime);
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
        data.addProperty("mTime", currTime);
        data.addProperty(TIMESTAMP, DecimalTimeUnit.MILLISECONDS.toSeconds(currTime));
        lastData = data;
    }

    private class ProbeLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            if (location != null) {
                String provider = location.getProvider();

                JsonObject data = gson.toJsonTree(location).getAsJsonObject();
                data.addProperty(TIMESTAMP, DecimalTimeUnit.MILLISECONDS.toSeconds(data.get("mTime").getAsLong()));
                Log.d(SCDCKeys.LogKeys.DEB, "[LocationProbe] location given by provider: " + provider + ", at: " + data.get("mTime").getAsBigDecimal());

                if (lastData == null) {
                    lastData = data;
                } else {
                    if (provider.equals(LocationManager.GPS_PROVIDER)) {
                        currData = data;
                        lastGpsTimestamp = data.get(TIMESTAMP).getAsBigDecimal();
                        sendData();
                    } else {
                        if (data.get(TIMESTAMP).getAsBigDecimal().subtract(lastGpsTimestamp)
                                .compareTo(new BigDecimal(checkInterval + 1)) > 0) {
                            currData = data;
                            sendData();
                        }
                    }
                }
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
     *
     * @return
     */
    private String getPassiveProvider() {
        try {
            Field passiveProviderField = LocationManager.class.getDeclaredField("PASSIVE_PROVIDER");
            return (String) passiveProviderField.get(null);
        } catch (SecurityException e) {
        } catch (NoSuchFieldException e) {
        } catch (IllegalArgumentException e) {
        } catch (IllegalAccessException e) {
        }
        return null;
    }

}
