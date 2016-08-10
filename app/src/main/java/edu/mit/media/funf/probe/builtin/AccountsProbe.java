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

import java.lang.reflect.Type;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import edu.mit.media.funf.probe.Probe.RequiredPermissions;
import edu.mit.media.funf.probe.builtin.ProbeKeys.AccountsKeys;
import kr.ac.snu.imlab.scdc.service.core.SCDCKeys;
import kr.ac.snu.imlab.scdc.util.SharedPrefsHandler;

@RequiredPermissions(android.Manifest.permission.GET_ACCOUNTS)
public class AccountsProbe extends ImpulseProbe implements AccountsKeys {

	@Override
	protected GsonBuilder getGsonBuilder() {
		GsonBuilder builder = super.getGsonBuilder();
		builder.registerTypeAdapter(Account.class, new JsonSerializer<Account>() {
			@Override
			public JsonElement serialize(Account src, Type typeOfSrc, JsonSerializationContext context) {
				JsonObject account = new JsonObject();
				account.addProperty(NAME, sensitiveData(src.name));
				account.addProperty(TYPE, src.type);
				return account;
			}
		});
		return builder;
	}

	@Override
	public void onStart() {
		Log.d(SCDCKeys.LogKeys.DEB, "[AccountsProbe] onStart");
		super.onStart();

		long currentTime = System.currentTimeMillis();
		long lastSavedTime = getLastSavedTime();

		if(currentTime > lastSavedTime + SCDCKeys.SharedPrefs.DEFAULT_IMPULSE_INTERVAL){
			AccountManager am = (AccountManager)getContext().getSystemService(Context.ACCOUNT_SERVICE);
			Gson gson = getGson();
			for (Account account : am.getAccounts()) {
				sendData(gson.toJsonTree(account).getAsJsonObject());
			}
			setLastSavedTime(currentTime);
		}
		disable();
	}

	@Override
	public void onStop() {
		Log.d(SCDCKeys.LogKeys.DEB, "[AccountsProbe] onStop");
		super.onStop();
	}

	protected void setLastSavedTime(long lastSavedTime) {
		SharedPrefsHandler.getInstance(this.getContext(),
				SCDCKeys.Config.SCDC_PREFS, Context.MODE_PRIVATE).setCPLastSavedTime(SCDCKeys.SharedPrefs.ACCOUNTS_LOG_LAST_TIME, lastSavedTime);
	}

	protected long getLastSavedTime() {
		return SharedPrefsHandler.getInstance(this.getContext(),
				SCDCKeys.Config.SCDC_PREFS, Context.MODE_PRIVATE).getCPLastSavedTime(SCDCKeys.SharedPrefs.ACCOUNTS_LOG_LAST_TIME);
	}
}
