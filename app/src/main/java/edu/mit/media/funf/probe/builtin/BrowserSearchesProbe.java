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

import android.content.Context;
import android.net.Uri;
import android.provider.Browser;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.probe.Probe.RequiredPermissions;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys;
import kr.ac.snu.imlab.scdc.util.SharedPrefsHandler;

@DisplayName("Browser Searches")
@Schedule.DefaultSchedule(interval=604800)
@RequiredPermissions(android.Manifest.permission.READ_HISTORY_BOOKMARKS)
public class BrowserSearchesProbe extends DatedContentProviderProbe {

	@Override
	protected Uri getContentProviderUri() {
		return Browser.SEARCHES_URI;
	}

	@Override
	protected String getDateColumnName() {
		return Browser.SearchColumns.DATE;
	}

	@Override
	protected void setLastSavedTime() {
		SharedPrefsHandler.getInstance(this.getContext(),
				SCDCKeys.Config.SCDC_PREFS, Context.MODE_PRIVATE).setCPLastSavedTime(SCDCKeys.SharedPrefs.SEARCH_LOG_LAST_TIME);
	}

	@Override
	protected long getLastSavedTime() {
		return SharedPrefsHandler.getInstance(this.getContext(),
				SCDCKeys.Config.SCDC_PREFS, Context.MODE_PRIVATE).getCPLastSavedTime(SCDCKeys.SharedPrefs.SEARCH_LOG_LAST_TIME);
	}

	@Override
	protected Map<String, CursorCell<?>> getProjectionMap() {
		Map<String,CursorCell<?>> projectionKeyToType = new HashMap<String, CursorCell<?>>();
		projectionKeyToType.put(Browser.SearchColumns._ID, intCell());
//		projectionKeyToType.put(Browser.SearchColumns.SEARCH, sensitiveStringCell());
		projectionKeyToType.put(Browser.SearchColumns.SEARCH, stringCell());
		projectionKeyToType.put(Browser.SearchColumns.DATE, longCell());
		return projectionKeyToType;
	}

}
