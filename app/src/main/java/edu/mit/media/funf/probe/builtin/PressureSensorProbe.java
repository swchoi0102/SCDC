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

import android.hardware.Sensor;
import android.util.Log;

import edu.mit.media.funf.probe.Probe.Description;
import edu.mit.media.funf.probe.builtin.ProbeKeys.PressureSensorKeys;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys;

@Description("Records a three dimensional vector of the magnetic field.")
//@RequiredFeatures("android.hardware.sensor.barometer") // varies
public class PressureSensorProbe extends SensorProbe implements PressureSensorKeys {

	public int getSensorType() {
		return Sensor.TYPE_PRESSURE;
	}
	
	public String[] getValueNames() {
		return new String[] {
			PRESSURE
		};
	}

	@Override
	public void onStart() {
		Log.d(SCDCKeys.LogKeys.DEB, "[PressureSensorProbe] onStart");
		super.onStart();
	}

	@Override
	public void onStop() {
		Log.d(SCDCKeys.LogKeys.DEB, "[PressureSensorProbe] onStop");
		super.onStop();
	}

}
