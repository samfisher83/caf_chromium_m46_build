/**
 Copyright (c) 2016, The Linux Foundation. All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are
 met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above
 copyright notice, this list of conditions and the following
 disclaimer in the documentation and/or other materials provided
 with the distribution.
 * Neither the name of The Linux Foundation nor the names of its
 contributors may be used to endorse or promote products derived
 from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 **/

package org.chromium.chrome.browser.ntp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.chromium.base.ApplicationStatus;
import org.chromium.base.CommandLine;
import org.chromium.chrome.browser.ChromeSwitches;
import org.chromium.chrome.browser.UpdateNotificationService;
import org.chromium.chrome.browser.UrlUtilities;
import org.chromium.chrome.browser.util.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

/**
 * Fetcher to retrieve all the preloaded websites the browser should show.
 */
public class PredefinedSitesFetcher {
    private static final String LOGTAG = "PredefinedSitesFetcher";
    private static final String UPDATE_TIMESTAMP = "custom_homepage_update_timestamp";
    private static final String REMOTE_WEBSITES_PREFERENCE = "remote_predefined_websites";
    private static final String RAW_FILE_NAME = "predefined_websites";
    private static final String ARRAY_NAME = "websites";
    private static final String ARRAY_ITEM_TITLE = "title";
    private static final String ARRARY_ITEM_URL = "url";
    private static final String ARRAY_ITEM_ICON_URL = "icon";

    private static PredefinedSitesFetcher sIsInitialized;

    private ArrayList<PredefinedSite> mSites;
    private Context mContext;
    private boolean mEnabled;

    public static PredefinedSitesFetcher getInstance() {
        if (sIsInitialized == null) {
            sIsInitialized = new PredefinedSitesFetcher();
        }
        return sIsInitialized;
    }

    private PredefinedSitesFetcher() {
        mContext = ApplicationStatus.getApplicationContext();
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    /*
    Force an update of all sites
     */
    public ArrayList<PredefinedSite> fetch() {
        if (mSites == null) {
            mSites = new ArrayList<>();
            SharedPreferences preferences = mContext
                    .getSharedPreferences(REMOTE_WEBSITES_PREFERENCE, Context.MODE_PRIVATE);
            int fileId = mContext.getResources().getIdentifier(RAW_FILE_NAME, "raw",
                    mContext.getPackageName());
            if (!TextUtils.isEmpty(preferences.getString(REMOTE_WEBSITES_PREFERENCE, ""))) {
                updateFromPrefs();
            } else if (fileId != 0) {
                updateLocal(fileId);
            }
            updateRemote(); //always check with the server the first time.
        }
        if (!mSites.isEmpty()) mEnabled = true;
        return mSites;
    }

    private synchronized void populateSites(JSONObject jsonObject) {
        try {
            JSONArray jsonArray = jsonObject.getJSONArray(ARRAY_NAME);
            // Clear the current set of sites to prepare for the new set
            if (jsonArray.length() > 0) mSites.clear();
            else return;
            for (int i = 0; i < jsonArray.length(); i++) {
                if (jsonArray.isNull(i) || !jsonArray.getJSONObject(i).has(ARRARY_ITEM_URL))
                    continue;

                Logger.v(LOGTAG, "retrieved the following preloaded website - "
                        + jsonArray.getJSONObject(i).toString());
                JSONObject entry = jsonArray.getJSONObject(i);
                String title = entry.has(ARRAY_ITEM_TITLE) ? entry.getString(ARRAY_ITEM_TITLE) : "";
                String url = UrlUtilities.fixupUrl(entry.has(ARRARY_ITEM_URL) ?
                        entry.getString(ARRARY_ITEM_URL) : "");
                String icon = UrlUtilities.fixupUrl(entry.has(ARRAY_ITEM_ICON_URL)
                        ? entry.getString(ARRAY_ITEM_ICON_URL) : "");
                PredefinedSite site = new PredefinedSite(title, url, icon);
                if (!mSites.contains(site)) mSites.add(site);
            }
        } catch (Exception e) {
            Logger.e(LOGTAG, "Unable to read json : " + e.toString());
        }
    }

    private void storeRemoteSites(JSONObject jsonObject) {
        String jsonString = jsonObject.toString();
        SharedPreferences sharedPref = mContext.getSharedPreferences(REMOTE_WEBSITES_PREFERENCE,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = sharedPref.edit();
        prefEditor.putString(REMOTE_WEBSITES_PREFERENCE, jsonString);
        prefEditor.apply();
    }

    private void updateFromPrefs() {
        SharedPreferences preferences = mContext
                .getSharedPreferences(REMOTE_WEBSITES_PREFERENCE, Context.MODE_PRIVATE);
        String jsonString = preferences.getString(REMOTE_WEBSITES_PREFERENCE, "");
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            populateSites(jsonObject);
        } catch (Exception e) {
            Logger.e(LOGTAG, "invalid string in SharedPref : " + e.toString());
        }
    }

    /**
     * Read the local RAW file if it's present
     */
    private void updateLocal(int fileId) {
        InputStream inputStream = mContext.getResources().openRawResource(fileId);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        int counter;
        try {
            counter = inputStream.read();
            while (counter != -1) {
                byteArrayOutputStream.write(counter);
                counter = inputStream.read();
            }
            inputStream.close();
        } catch (IOException e) {
            Logger.e(LOGTAG, "raw file read failed - " + e.toString());
        }
        if (TextUtils.isEmpty(byteArrayOutputStream.toString())) {
            Logger.v(LOGTAG, "No predefined websites read" + byteArrayOutputStream.toString());
            return;
        }

        try {
            JSONObject jsonObject = new JSONObject(byteArrayOutputStream.toString());
            populateSites(jsonObject);

        } catch (Exception e) {
            Logger.e(LOGTAG, "invalid json format : " + e.toString());
        }
    }

    private void updateRemote() {
        Intent intent = new Intent(mContext, UpdateNotificationService.class);
        intent.setAction(UpdateNotificationService.ACTION_CUSTOM_HOME_UPDATE);
        mContext.startService(intent);
    }

    public void handleUpdateRemote() {
        String server_url = CommandLine.getInstance()
                .getSwitchValue(ChromeSwitches.CUSTOM_HOMEPAGE_SERVER_CMD);
        InputStream stream;
        if (!TextUtils.isEmpty(server_url)) {
            try {
                URLConnection connection = new URL(server_url).openConnection();
                stream = connection.getInputStream();
                String result = readContent(stream);
                Logger.v(LOGTAG, "remoteUpdate result : " + result);
                JSONObject jsonResult = new JSONObject(result);
                populateSites(jsonResult);
                storeRemoteSites(jsonResult);
                stream.close();
            } catch (Exception e) {
                Logger.e(LOGTAG, "remoteUpdate Exception : " + e.toString());
            } finally {
                // always update the timestamp
                updateTimeStamp();
            }
        }
    }

    private void updateTimeStamp() {
        SharedPreferences sharedPref = mContext.getSharedPreferences(
                REMOTE_WEBSITES_PREFERENCE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putLong(UPDATE_TIMESTAMP, System.currentTimeMillis());
        editor.apply();
    }

    public String readContent(InputStream is) {
        String line;
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        try {
            line = reader.readLine();
            while (line != null) {
                sb.append(line).append("\n");
                line = reader.readLine();
            }
        } catch (Exception e) {
            Logger.e(LOGTAG, "convertStreamToString Exception : " + e.toString());
        } finally {
            try {
                is.close();
            } catch (Exception e) {
                Logger.e(LOGTAG, "convertStreamToString Exception : " + e.toString());
            }
        }
        return sb.toString();
    }

}
