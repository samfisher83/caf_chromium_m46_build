// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.preferences;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.text.format.DateUtils;
import android.view.ContextThemeWrapper;

import org.chromium.base.ApplicationStatus;
import org.chromium.base.CommandLine;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.ChromeSwitches;
import org.chromium.chrome.browser.ChromeVersionInfo;
import org.chromium.chrome.browser.IntentHelper;
import org.chromium.chrome.browser.UpdateNotificationService;
import org.chromium.chrome.browser.document.ChromeLauncherActivity;
import org.chromium.chrome.browser.preferences.PrefServiceBridge.AboutVersionStrings;

/**
 * Settings fragment that displays information about Chrome.
 */
public class AboutChromePreferences extends BrowserPreferenceFragment {

    private static final String PREF_APPLICATION_VERSION = "application_version";
    private static final String PREF_OS_VERSION = "os_version";
    private static final String PREF_FEEDBACK = "feedback";
    public static final String TABURL = "tab_url";
    public static final String TABTITLE = "tab_title";
    public static final String TABBUNDLE = "tab_bundle";
    public static final String PREF_UPDATE_NOTIFICATION = "update_notification";
    private String mTabURL = "";
    private String mTabTitle = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle(R.string.prefs_about_chrome);
        addPreferencesFromResource(R.xml.about_chrome_preferences);

        final Bundle arguments = getActivity().getIntent().getBundleExtra(TABBUNDLE);
        if (arguments != null) {
            mTabTitle =  arguments.getString(TABTITLE);
            mTabURL = arguments.getString(TABURL);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            ChromeBasePreference deprecationWarning = new ChromeBasePreference(
                    new ContextThemeWrapper(getActivity(),
                            R.style.DeprecationWarningPreferenceTheme));
            deprecationWarning.setOrder(-1);
            deprecationWarning.setTitle(R.string.deprecation_warning);
            deprecationWarning.setIcon(R.drawable.exclamation_triangle);
            getPreferenceScreen().addPreference(deprecationWarning);
        }

        PrefServiceBridge prefServiceBridge = PrefServiceBridge.getInstance();
        AboutVersionStrings versionStrings = prefServiceBridge.getAboutVersionStrings();
        Preference p = findPreference(PREF_APPLICATION_VERSION);
        p.setSummary(getApplicationVersion(versionStrings.getApplicationVersion()));
        p = findPreference(PREF_OS_VERSION);
        p.setSummary(versionStrings.getOSVersion());
        ButtonPreference prefFeedback =
                (ButtonPreference) findPreference(PREF_FEEDBACK);

        if(CommandLine.getInstance().hasSwitch(ChromeSwitches.CMD_LINE_SWITCH_FEEDBACK)) {
            prefFeedback.setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    IntentHelper.sendEmail(getActivity(),
                            CommandLine.getInstance().getSwitchValue(
                                    ChromeSwitches.CMD_LINE_SWITCH_FEEDBACK),
                            getResources().getString(R.string.swe_feedback_subject),
                            getFeedbackMsg(),
                            null,
                            null);
                    return true;
                }
            });
        } else {
            getPreferenceScreen().removePreference(prefFeedback);
        }

        ButtonPreference updatePreference =
                (ButtonPreference) findPreference(PREF_UPDATE_NOTIFICATION);
        if (CommandLine.getInstance().hasSwitch(ChromeSwitches.AUTO_UPDATE_SERVER_CMD) &&
                UpdateNotificationService.getCurrentVersionCode(getActivity()) <
                        UpdateNotificationService.getLatestVersionCode(getActivity())) {
            updatePreference.setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            Context context = ApplicationStatus.getApplicationContext();
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setPackage(context.getPackageName());
                            intent.setClass(context, ChromeLauncherActivity.class);
                            intent.setData(Uri.parse(
                                    UpdateNotificationService.getLatestDownloadUrl(getActivity())));
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                            return true;
                        }
                    });
        } else {
            getPreferenceScreen().removePreference(updatePreference);
        }
    }

    private String getFeedbackMsg() {
        PrefServiceBridge prefServiceBridge = PrefServiceBridge.getInstance();
        return getResources().getString(R.string.swe_feedback_msg,
                prefServiceBridge.getAboutVersionStrings().getApplicationVersion(),
                ChromeVersionInfo.getProductHash(), mTabTitle, mTabURL);
    }

    private String getApplicationVersion(String version) {
        if (ChromeVersionInfo.isOfficialBuild()) {
            return version;
        }

        // For developer builds, show how recently the app was installed/updated.
        PackageInfo info;
        try {
            info = getActivity().getPackageManager().getPackageInfo(
                    getActivity().getPackageName(), 0);
        } catch (NameNotFoundException e) {
            return version;
        }
        CharSequence updateTimeString = DateUtils.getRelativeTimeSpanString(
                info.lastUpdateTime, System.currentTimeMillis(), 0);
        return getActivity().getString(R.string.version_with_update_time, version,
                updateTimeString);
    }
}
