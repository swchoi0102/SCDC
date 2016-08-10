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

import java.util.HashMap;
import java.util.Map;

import android.Manifest;
import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore.Audio;
import android.util.Log;

import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.time.DecimalTimeUnit;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys;
import kr.ac.snu.imlab.scdc.util.SharedPrefsHandler;

@DisplayName("Audio Media File Stats Probe")
@Schedule.DefaultSchedule(interval=36000)
@Probe.RequiredPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)
public class AudioMediaProbe extends DatedContentProviderProbe {

	@Override
	public void onStart() {
		Log.d(SCDCKeys.LogKeys.DEB, "[AudioMediaProbe] onStart");
		super.onStart();
	}

	@Override
	public void onStop() {
		Log.d(SCDCKeys.LogKeys.DEB, "[AudioMediaProbe] onStop");
		super.onStop();
	}

	@Override
	protected Uri getContentProviderUri() {
		return Audio.Media.EXTERNAL_CONTENT_URI;
	}

	@Override
	protected String getDateColumnName() {
		return Audio.Media.DATE_MODIFIED;
	}

	@Override
	protected Map<String, CursorCell<?>> getProjectionMap() {
		Map<String, CursorCell<?>> projectionMap = new HashMap<String, CursorCell<?>>();
		projectionMap.put(Audio.Media._ID, stringCell());
		// Ignoring DATA, too large and not relevant
		projectionMap.put(Audio.Media.DATE_ADDED, longCell());
		projectionMap.put(Audio.Media.DATE_MODIFIED, longCell());
		projectionMap.put(Audio.Media.DISPLAY_NAME, stringCell());
		projectionMap.put(Audio.Media.MIME_TYPE, stringCell());
		projectionMap.put(Audio.Media.SIZE, longCell());
		projectionMap.put(Audio.Media.TITLE, stringCell());
		projectionMap.put(Audio.Media.ALBUM, stringCell());
		//projectionMap.put(Audio.Media.ALBUM_ART, stringCell());  Not there for some reason
		projectionMap.put(Audio.Media.ALBUM_ID, longCell());
		//projectionMap.put(Audio.Media.ALBUM_KEY, stringCell()); Not relevant, Internal android use
		projectionMap.put(Audio.Media.ARTIST, stringCell());
		projectionMap.put(Audio.Media.ARTIST_ID, longCell());
		// projectionMap.put(Audio.Media.ARTIST_KEY, stringCell()); Not relevant, Internal android use
		projectionMap.put(Audio.Media.COMPOSER, stringCell());
		projectionMap.put(Audio.Media.DURATION, longCell());
		projectionMap.put(Audio.Media.IS_ALARM, booleanCell());
		projectionMap.put(Audio.Media.IS_MUSIC, booleanCell());
		projectionMap.put(Audio.Media.IS_NOTIFICATION, booleanCell());
		projectionMap.put(Audio.Media.IS_RINGTONE, booleanCell());
		//projectionMap.put(Audio.Media.TITLE_KEY, stringCell());  Not relevant, Internal android use
		projectionMap.put(Audio.Media.TRACK, intCell());
		projectionMap.put(Audio.Media.YEAR, intCell());
		
		return projectionMap;
	}
	
	@Override
	protected DecimalTimeUnit getDateColumnTimeUnit() {
		return DecimalTimeUnit.SECONDS;
	}

	@Override
	protected void setLastSavedTime(long lastSavedTime) {
		SharedPrefsHandler.getInstance(this.getContext(),
				SCDCKeys.Config.SCDC_PREFS, Context.MODE_PRIVATE).setCPLastSavedTime(SCDCKeys.SharedPrefs.AUDIO_LOG_LAST_TIME, lastSavedTime);
	}

	@Override
	protected long getLastSavedTime() {
		return SharedPrefsHandler.getInstance(this.getContext(),
				SCDCKeys.Config.SCDC_PREFS, Context.MODE_PRIVATE).getCPLastSavedTime(SCDCKeys.SharedPrefs.AUDIO_LOG_LAST_TIME);
	}

}
