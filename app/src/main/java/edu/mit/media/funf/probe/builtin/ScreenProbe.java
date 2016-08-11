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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import com.google.gson.JsonObject;

import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe.ContinuousProbe;
import edu.mit.media.funf.probe.Probe.Description;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.probe.builtin.ProbeKeys.ScreenKeys;
import edu.mit.media.funf.time.TimeUtil;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys;
import kr.ac.snu.imlab.scdc.service.probe.InsensitiveProbe;

@DisplayName("Screen On/Off")
@Description("Records when the screen turns off and on.")
@Schedule.DefaultSchedule(interval = 0, duration = 0, opportunistic = true)
public class ScreenProbe extends InsensitiveProbe implements ContinuousProbe, ScreenKeys {

    private BroadcastReceiver screenReceiver;
    private PowerManager pm;

    @Override
    protected void onEnable() {
        super.onEnable();
        pm = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (Intent.ACTION_SCREEN_OFF.equals(action)
                        || Intent.ACTION_SCREEN_ON.equals(action)) {
                    JsonObject data = new JsonObject();
                    data.addProperty(ProbeKeys.BaseProbeKeys.TIMESTAMP, TimeUtil.getTimestamp());
                    data.addProperty(SCREEN_ON, Intent.ACTION_SCREEN_ON.equals(action));
                    if (lastData == null) {
                        Log.d(SCDCKeys.LogKeys.DEB, "[ScreenProbe] getCurrData");
                        lastData = data;
                    } else {
                        currData = data;
                        if (isDataChanged()) sendData();
                    }
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
    }

    @Override
    protected void onStop() {
        Log.d(SCDCKeys.LogKeys.DEB, "[ScreenProbe] onStop");
        super.onStop();
        getContext().unregisterReceiver(screenReceiver);
    }

    private void initializeScreenStatus() {
        lastData = new JsonObject();

        boolean screeOn;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) screeOn = pm.isInteractive();
        else screeOn = pm.isScreenOn();

        lastData.addProperty(TIMESTAMP, TimeUtil.getTimestamp());
        lastData.addProperty(SCREEN_ON, screeOn);
    }
}
