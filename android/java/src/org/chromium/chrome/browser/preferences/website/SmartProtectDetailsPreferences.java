/*
 * Copyright (c) 2015-2016, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package org.chromium.chrome.browser.preferences.website;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Base64InputStream;
import android.util.Base64OutputStream;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import org.chromium.base.ApplicationStatus;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.document.BrandColorUtils;
import org.chromium.chrome.browser.preferences.BrowserPreferenceFragment;
import org.chromium.chrome.browser.preferences.PrefServiceBridge;
import org.chromium.chrome.browser.preferences.TextMessagePreference;
import org.chromium.content.browser.WebDefender;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

import java.util.Comparator;
import java.util.Formatter;
import java.util.HashMap;

/**
 * Fragment to show smart protect details.
 */
public class SmartProtectDetailsPreferences extends BrowserPreferenceFragment {
    public static final String EXTRA_SMARTPROTECT_PARCEL = "extra_smartprotect_parcel";
    public static final int GREEN = Color.parseColor("#008F02");
    public static final int YELLOW = Color.parseColor("#CBB325");
    public static final int RED = Color.parseColor("#AA232A");
    public static final int GRAY = Color.GRAY;
    private static final int BAR_GRAPH_HEIGHT = 100;
    private static final String DOMAIN_PREF = "tracker_domains";
    private boolean mIsIncognito;
    private int mMaxBarGraphWidth;
    private WebDefender.ProtectionStatus mStatus;
    private HashMap<String, Integer> mUpdatedTrackerDomains;
    private String mTitle;
    private int mSmartProtectColor;
    private int mWebRefinerBlockedCount;
    private boolean mWebDefenderEnabled;
    private boolean mWebRefinerEnabled;

    private WebDefenderVectorsRecyclerView mVectorsRecyclerView;

    private static int mPrivacyMeterBarWidth = 125;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        addPreferencesFromResource(R.xml.smartprotect_details_preferences);

        mSmartProtectColor = getResources().getColor(R.color.smart_protect);

        Bundle arguments = getArguments();
        if (arguments != null) {
            mIsIncognito = arguments.getBoolean(BrowserSingleWebsitePreferences.EXTRA_INCOGNITO);

            Object extraSite = arguments.getSerializable(SingleWebsitePreferences.EXTRA_SITE);
            Object extraOrigin = arguments.getSerializable(SingleWebsitePreferences.EXTRA_ORIGIN);
            mWebDefenderEnabled = arguments.getBoolean(
                    BrowserSingleWebsitePreferences.WEBDEFENDER_SETTING);
            mWebRefinerEnabled = arguments.getBoolean(
                    BrowserSingleWebsitePreferences.WEBREFINER_SETTING);

            if (extraSite != null && extraOrigin == null) {
                Website site = (Website) extraSite;
                mTitle = site.getAddress().getTitle();
            } else if (extraOrigin != null && extraSite == null) {
                WebsiteAddress siteAddress = WebsiteAddress.create((String) extraOrigin);
                mTitle = siteAddress.getTitle();
            }
            mWebRefinerBlockedCount =
            arguments.getInt(BrowserSingleWebsitePreferences.EXTRA_WEB_REFINER_ADS_INFO, 0)
            + arguments.getInt(BrowserSingleWebsitePreferences.EXTRA_WEB_REFINER_TRACKER_INFO, 0)
            + arguments.getInt(BrowserSingleWebsitePreferences.EXTRA_WEB_REFINER_MALWARE_INFO, 0);
            WebDefenderPreferenceHandler.StatusParcel parcel =
                    arguments.getParcelable(EXTRA_SMARTPROTECT_PARCEL);
            if (parcel != null)
                mStatus = parcel.getStatus();
        }

        String settingName = getSmartProtectString(getResources(), getActivity(),
                "swe_security_branding_label");
        if (TextUtils.isEmpty(settingName))
            settingName = getResources().getString(R.string.website_settings_smartprotect_title);

        getActivity().setTitle(settingName);

        setupOverviewPanel("web_refiner_info_title", "web_refiner_title", "web_refiner_docs_url",
                WebRefinerPreferenceHandler.isInitialized());

        if (WebRefinerPreferenceHandler.isInitialized()) {
            TextMessagePreference webrefiner_details =
                    (TextMessagePreference) findPreference("webrefiner_details");
            BrowserSingleWebsitePreferences.WebRefinerStatsInfo info =
                    BrowserSingleWebsitePreferences.getWebRefinerInformation(
                            getResources(), arguments);
            if (info != null && !TextUtils.isEmpty(info.mFormattedMsg)) {
                webrefiner_details.setTitle(Html.fromHtml(info.mFormattedMsg));
            } else {
                PreferenceCategory prefCat =
                        (PreferenceCategory) findPreference("web_refiner_info_title");
                prefCat.removePreference(webrefiner_details);
            }

            TextMessagePreference webrefiner_overview =
                    (TextMessagePreference) findPreference("webrefiner_overview");
            String overviewText = getSmartProtectString(getResources(), getActivity(),
                    "web_refiner_about_text");
            if (!TextUtils.isEmpty(overviewText)) {
                webrefiner_overview.setTitle(overviewText);
            }
        }

        setupOverviewPanel("web_defender_info_title", "web_defender_title", "web_defender_docs_url",
                WebDefenderPreferenceHandler.isInitialized());

        Preference meter = findPreference("webdefender_privacy_meter");
        TextMessagePreference webdefender_details =
                (TextMessagePreference) findPreference("webdefender_details");

        TextMessagePreference webdefender_overview =
                (TextMessagePreference) findPreference("webdefender_overview");

        if (mStatus == null || mStatus.mTrackerDomains == null
                || !WebDefenderPreferenceHandler.isInitialized()
                || mStatus.mTrackerDomains.length == 0) {
            PreferenceScreen vectorList = (PreferenceScreen) findPreference("vector_list");
            if (vectorList != null)
                getPreferenceScreen().removePreference(vectorList);
            PreferenceScreen vectorChart = (PreferenceScreen) findPreference("vector_chart");
            if (vectorChart != null)
                getPreferenceScreen().removePreference(vectorChart);
            if (webdefender_details != null)
                getPreferenceScreen().removePreference(webdefender_details);
            if (webdefender_overview != null)
                getPreferenceScreen().removePreference(webdefender_overview);
        } else {
            String overviewText = getSmartProtectString(getResources(), getActivity(),
                    "web_defender_about_text");
            if (!TextUtils.isEmpty(overviewText) && webdefender_overview != null) {
                webdefender_overview.setTitle(overviewText);
            }
        }

        if (mTitle != null) {
            TextMessagePreference siteTitle = (TextMessagePreference) findPreference("site_title");
            if (siteTitle != null) {
                siteTitle.setTitle(mTitle);
                byte[] data = arguments.getByteArray(BrowserSingleWebsitePreferences.EXTRA_FAVICON);
                if (data != null) {
                    Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length);
                    if (bm != null) {
                        Drawable drawable = new BitmapDrawable(getResources(), bm);
                        siteTitle.setIcon(drawable);
                    }
                }
            }
        }

        if (mStatus != null && mStatus.mTrackerDomains.length != 0) {
            webdefender_details.setTitle(mWebDefenderEnabled
                    ? getOverviewMessage(getResources(), mStatus)
                    : getDisabledMessage(getResources(), mStatus));
        }

        meter.setSummary(mTitle);
    }

    public static String getSmartProtectString(Resources res, Activity ctx, String id) {
        try {
            ApplicationInfo appInfo = ctx.getPackageManager().getApplicationInfo(
                    ctx.getPackageName(), PackageManager.GET_META_DATA);
            String packageName = res.getResourcePackageName(appInfo.icon);

            int resId = res.getIdentifier(id, "string", packageName);
            if (resId == 0) {
                return null;
            }

            return res.getString(resId);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private void setupOverviewPanel(String prefCatName, String titleStringResName,
                                    String supportURLResName, boolean available) {
        SmartProtectPreferenceCategory prefCat =
                (SmartProtectPreferenceCategory) findPreference(prefCatName);
        if (prefCat != null) {
            if (available) {
                String titleName = getSmartProtectString(getResources(), getActivity(),
                        titleStringResName);
                if (!TextUtils.isEmpty(titleName)) {
                    prefCat.setTitle(titleName);
                }

                prefCat.setTitleAttributes(
                        getResources().getColor(android.R.color.transparent), mSmartProtectColor);
                String url = getSmartProtectString(getResources(), getActivity(),supportURLResName);
                if (!TextUtils.isEmpty(url))
                    prefCat.setSupportURL(url);
            } else {
                PreferenceScreen screen = getPreferenceScreen();
                if (screen != null)
                    screen.removePreference(prefCat);
            }
        }
    }


    @Override
    @SuppressWarnings("unchecked")
    public void onStart() {
        super.onStart();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String domains = prefs.getString(DOMAIN_PREF, "");
        if (TextUtils.isEmpty(domains)) return;
        byte[] bytes = domains.getBytes();
        if (bytes.length == 0)
            return;
        ByteArrayInputStream byteArray = new ByteArrayInputStream(bytes);
        Base64InputStream base64InputStream = new Base64InputStream(byteArray, Base64.DEFAULT);
        ObjectInputStream in;
        try {
            in = new ObjectInputStream(base64InputStream);
            mUpdatedTrackerDomains = (HashMap<String, Integer>) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    public static CharSequence getOverviewMessage(Resources resources,
                                            WebDefender.ProtectionStatus status) {
        int count = 0;
        for (int i = 0; i < status.mTrackerDomains.length; i++) {
            if (status.mTrackerDomains[i].mProtectiveAction !=
                    WebDefender.TrackerDomain.PROTECTIVE_ACTION_UNBLOCK) {
                count++;
            }
        }

        return (count > 0)
                ? Html.fromHtml(new Formatter().format(
                    resources.getString(R.string.website_settings_webdefender_brief_message),
                    "<b>" + count + "</b>").toString())
                : resources.getString(R.string.website_settings_webdefender_enabled);
    }

    private void appendActionBarDisplayOptions(ActionBar bar, int extraOptions) {
        int options = bar.getDisplayOptions();
        options |= extraOptions;
        bar.setDisplayOptions(options);
    }

    private void setStatusBarColor(int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final Activity activity = ApplicationStatus.getLastTrackedFocusedActivity();
            if (!PrefServiceBridge.getInstance().getPowersaveModeEnabled()) {
                activity.getWindow().addFlags(
                        WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            }

            int statusBarColor = BrandColorUtils.computeStatusBarColor(color);
            activity.getWindow().setStatusBarColor(statusBarColor);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            ActionBar bar = activity.getSupportActionBar();

            Bitmap bitmap = BitmapFactory.decodeResource(getResources(),
                    R.drawable.img_deco_smartprotect_webdefender);

            appendActionBarDisplayOptions(bar,
                    ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
            bar.setHomeButtonEnabled(true);
            bar.setIcon(new BitmapDrawable(getResources(), bitmap));
            bar.setBackgroundDrawable(new ColorDrawable(
                    BrandColorUtils.computeActionBarColor(mSmartProtectColor)
            ));

            setStatusBarColor(mSmartProtectColor);
        }

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        mMaxBarGraphWidth = (int) (Math.min(size.x, size.y) * 0.40);
    }

    private static Drawable generateBarDrawable(int width, int height, int color) {
        PaintDrawable drawable = new PaintDrawable(color);
        drawable.setIntrinsicWidth(width);
        drawable.setIntrinsicHeight(height);
        drawable.setBounds(0, 0, width, height);

        Bitmap thumb = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(thumb);
        drawable.draw(c);

        return new BitmapDrawable(thumb);
    }

    private Drawable normalizedBarGraphDrawable(int value, int maxValue) {
        if (value == 0)
            return null;

        int normalizedWidth = mMaxBarGraphWidth * value / maxValue;

        return generateBarDrawable(normalizedWidth, BAR_GRAPH_HEIGHT, mSmartProtectColor);
    }

    private String getStringForCount(int count) {
        if (count == 0)
            return getResources().getString(
                    R.string.website_settings_webdefender_tracking_not_detected);

        return Integer.toString(count);
    }

    private static void createRatingStar(ImageView imageView, int height, int color) {
        Drawable drawable = generateBarDrawable(mPrivacyMeterBarWidth, height, color);
        imageView.setImageDrawable(drawable);
    }

    public static void setupPrivacyMeterDisplay(
            Resources res, View view, boolean webDefenderEnabled,
            boolean webRefinerEnabled, WebDefender.ProtectionStatus status,
            int webRefinerBlockedCount) {
        if (view == null) {
            return;
        }
        int blockedUrlCount = 0;
        int strippedCount = 0;
        int possibleConnections = 0;
        boolean addedProtection = webDefenderEnabled
                || (webRefinerEnabled && webRefinerBlockedCount > 0);
        if (WebDefenderPreferenceHandler.isInitialized() && status != null) {
            for (WebDefender.TrackerDomain trackerDomain : status.mTrackerDomains) {
                if (trackerDomain.mProtectiveAction ==
                        WebDefender.TrackerDomain.PROTECTIVE_ACTION_BLOCK_URL) {
                    blockedUrlCount++;
                } else if (trackerDomain.mProtectiveAction ==
                        WebDefender.TrackerDomain.PROTECTIVE_ACTION_BLOCK_COOKIES) {
                    strippedCount++;
                } else if (trackerDomain.mPotentialTracker) {
                    possibleConnections++;
                }
            }
        }

        int rawCount = ((webDefenderEnabled) ? blockedUrlCount + strippedCount : 0) +
                ((webRefinerEnabled) ? webRefinerBlockedCount : 0);
        double weightedWebDefenderCount = blockedUrlCount*1.37 + strippedCount;
        double weightedWebRefinerCount = webRefinerBlockedCount*1.37;
        double weightedCount = weightedWebDefenderCount + weightedWebRefinerCount;
        double effectiveWeightedCount =
                ((webDefenderEnabled) ? blockedUrlCount*1.37 + strippedCount : 0) +
                ((webRefinerEnabled) ? webRefinerBlockedCount*1.37 : 0);
        int maxProtectImpact = getPrivacyImpact(weightedCount);
        int actualImpact = getPrivacyImpact(effectiveWeightedCount);
        int possibleImpact = getPrivacyImpact(possibleConnections);
        int unprotectedRating = 5 - maxProtectImpact - possibleImpact;
        int starRating = (addedProtection)
                ? 5 - possibleImpact - (maxProtectImpact - actualImpact)
                : unprotectedRating;
        int passiveColor = (unprotectedRating <= 1) ? RED :
                (unprotectedRating <= 4) ? YELLOW : GREEN;

        if (res.getDisplayMetrics().density < 3.0f)
            mPrivacyMeterBarWidth = 65;

        if (addedProtection) {
            int starHeight = 50;
            int scaleIncrease = 100;
            int scaleCount = 1;
            ImageView imageView = (ImageView) view.findViewById(R.id.star1);
            if ((starRating >= 1)) {

                createRatingStar(imageView, (unprotectedRating >= 1)
                        ? starHeight : (starHeight + (scaleCount++ * scaleIncrease)),
                        (unprotectedRating >= 1) ? passiveColor : GREEN);
            } else {
                createRatingStar(imageView, starHeight, GRAY);
            }

            imageView = (ImageView) view.findViewById(R.id.star2);
            if ((starRating >= 2)) {
                createRatingStar(imageView, (unprotectedRating >= 2)
                                ? starHeight : (starHeight + (scaleCount++ * scaleIncrease)),
                        (unprotectedRating >= 2) ? passiveColor : GREEN);
            } else {
                createRatingStar(imageView, starHeight, GRAY);
            }

            imageView = (ImageView) view.findViewById(R.id.star3);
            if ((starRating >= 3)) {
                createRatingStar(imageView, (unprotectedRating >= 3)
                                ? starHeight : (starHeight + (scaleCount++ * scaleIncrease)),
                        (unprotectedRating >= 3) ? passiveColor : GREEN);
            } else {
                createRatingStar(imageView, starHeight, GRAY);
            }

            imageView = (ImageView) view.findViewById(R.id.star4);
            if ((starRating >= 4)) {
                createRatingStar(imageView, (unprotectedRating >= 4)
                                ? starHeight : (starHeight + (scaleCount++ * scaleIncrease)),
                        (unprotectedRating >= 4) ? passiveColor : GREEN);
            } else {
                createRatingStar(imageView, starHeight, GRAY);
            }

            imageView = (ImageView) view.findViewById(R.id.star5);
            if ((starRating >= 5)) {
                createRatingStar(imageView, (unprotectedRating >= 5)
                                ? starHeight : (starHeight + (scaleCount * scaleIncrease)),
                        (unprotectedRating >= 5) ? passiveColor : GREEN);
            } else {
                createRatingStar(imageView, starHeight, Color.GRAY);
            }

        } else {
            ImageView imageView = (ImageView) view.findViewById(R.id.star1);
            createRatingStar(imageView, 50, (starRating >= 1) ? passiveColor : GRAY);

            imageView = (ImageView) view.findViewById(R.id.star2);
            createRatingStar(imageView, 50, (starRating >= 2) ? passiveColor : GRAY);

            imageView = (ImageView) view.findViewById(R.id.star3);
            createRatingStar(imageView, 50, (starRating >= 3) ? passiveColor : GRAY);

            imageView = (ImageView) view.findViewById(R.id.star4);
            createRatingStar(imageView, 50, (starRating >= 4) ? passiveColor : GRAY);

            imageView = (ImageView) view.findViewById(R.id.star5);
            createRatingStar(imageView, 50, (starRating >= 5) ? passiveColor : GRAY);
        }

        TextView textView = (TextView) view.findViewById(R.id.count);
        if (addedProtection && rawCount > 0) {
            textView.setVisibility(View.VISIBLE);
            String improvement = "+<b>" + rawCount + "</b>!";
            textView.setText(Html.fromHtml(improvement));
        } else {
            textView.setVisibility(View.INVISIBLE);
        }
    }

    private static int getPrivacyImpact(double count) {
        return Math.max((int) Math.round(Math.log1p(count)) - 1, 0);
    }

    @Override
    public void onChildViewAddedToHierarchy(View parent, View child) {
        TextView title = (TextView) child.findViewById(android.R.id.title);
        if (title != null && title.getText().equals(
               getResources().getText(R.string.website_settings_webdefender_privacy_meter_title))) {
            View meter = child.findViewById(R.id.webdefender_privacy_meter);
            if (meter != null) {
                SmartProtectDetailsPreferences
                        .setupPrivacyMeterDisplay(getResources(), meter,
                                mWebDefenderEnabled, mWebRefinerEnabled,
                                mStatus, mWebRefinerBlockedCount);
            }
        }


        if (child.getId() == R.id.browser_pref_cat
                || child.getId() == R.id.browser_pref_cat_first
                || child.getId() == R.id.browser_pref_cat_switch) {

            if (title != null) {
                title.setTextColor(mSmartProtectColor);
            }
        }

        if (child.getId() == R.id.webdefender_vectorchart_layout) {
            int numCookieTrackers = 0;
            int numStorageTrackers = 0;
            int numFingerprintTrackers = 0;
            int numFontEnumTrackers = 0;

            if (mStatus != null) {
                for (int i = 0; i < mStatus.mTrackerDomains.length; i++) {
                    if ((mStatus.mTrackerDomains[i].mTrackingMethods &
                            WebDefender.TrackerDomain.TRACKING_METHOD_HTTP_COOKIES) != 0) {
                        numCookieTrackers++;
                    }
                    if ((mStatus.mTrackerDomains[i].mTrackingMethods &
                            WebDefender.TrackerDomain.TRACKING_METHOD_HTML5_LOCAL_STORAGE) != 0) {
                        numStorageTrackers++;
                    }
                    if ((mStatus.mTrackerDomains[i].mTrackingMethods &
                            WebDefender.TrackerDomain.TRACKING_METHOD_CANVAS_FINGERPRINT) != 0) {
                        numFingerprintTrackers++;
                    }
                }
            }

            int max = Math.max(Math.max(numCookieTrackers, numFingerprintTrackers),
                    Math.max(numStorageTrackers, numFontEnumTrackers));

            TextView view = (TextView) child.findViewById(R.id.cookie_storage);
            if (view != null) {
                view.setText(getStringForCount(numCookieTrackers));
                view.setCompoundDrawablesWithIntrinsicBounds(
                        normalizedBarGraphDrawable(numCookieTrackers, max), null, null, null);
            }
            view = (TextView) child.findViewById(R.id.fingerprinting);
            if (view != null) {
                view.setText(getStringForCount(numStorageTrackers));
                view.setCompoundDrawablesWithIntrinsicBounds(
                        normalizedBarGraphDrawable(numStorageTrackers, max), null, null, null);
            }
            view = (TextView) child.findViewById(R.id.html5_storage);
            if (view != null) {
                view.setText(getStringForCount(numFingerprintTrackers));
                view.setCompoundDrawablesWithIntrinsicBounds(
                        normalizedBarGraphDrawable(numFingerprintTrackers, max), null, null, null);
            }
            view = (TextView) child.findViewById(R.id.font_enumeration);
            if (view != null) {
                view.setText(getStringForCount(numFontEnumTrackers));
                view.setCompoundDrawablesWithIntrinsicBounds(
                        normalizedBarGraphDrawable(numFontEnumTrackers, max), null, null, null);
            }
        } else if (child.getId() == R.id.webdefender_vectorlist_layout) {
            WebDefenderVectorsRecyclerView view =
                    (WebDefenderVectorsRecyclerView) child.findViewById(R.id.webdefender_vectors);
            if (view != null && mStatus != null) {
                mVectorsRecyclerView = view;
                mVectorsRecyclerView.setUpdatedDomains(mUpdatedTrackerDomains);
                view.updateVectorArray(sortDomains(mStatus.mTrackerDomains));
            }
        }
    }

    private WebDefender.TrackerDomain[] sortDomains(WebDefender.TrackerDomain[] trackerDomains) {
        Comparator<WebDefender.TrackerDomain> trackerDomainComparator =
                new Comparator<WebDefender.TrackerDomain>() {
            @Override
            public int compare(WebDefender.TrackerDomain lhs, WebDefender.TrackerDomain rhs) {
                if (mUpdatedTrackerDomains != null
                        && (mUpdatedTrackerDomains.containsKey(lhs.mName)
                        || mUpdatedTrackerDomains.containsKey(rhs.mName))) {
                    if (mUpdatedTrackerDomains.containsKey(lhs.mName)
                            && !mUpdatedTrackerDomains.containsKey(rhs.mName)) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
                //list is reverse sorted to handle the way recyclerview adds elements.
                else if (lhs.mUsesUserDefinedProtectiveAction || rhs.mUsesUserDefinedProtectiveAction) {
                    if (lhs.mUsesUserDefinedProtectiveAction
                            && rhs.mUsesUserDefinedProtectiveAction) {
                        return 0;
                    } else {
                        return (lhs.mUsesUserDefinedProtectiveAction) ? -1 : 1;
                    }
                } else if (lhs.mProtectiveAction != rhs.mProtectiveAction) {
                    if (lhs.mProtectiveAction
                            == WebDefender.TrackerDomain.PROTECTIVE_ACTION_BLOCK_URL) {
                        return -1;
                    } else if (lhs.mProtectiveAction
                            == WebDefender.TrackerDomain.PROTECTIVE_ACTION_BLOCK_COOKIES
                            && rhs.mProtectiveAction
                            != WebDefender.TrackerDomain.PROTECTIVE_ACTION_BLOCK_URL) {
                        return -1;
                    } else {
                        return 1;
                    }
                } else if (lhs.mPotentialTracker || rhs.mPotentialTracker) {
                    if (lhs.mPotentialTracker
                            && rhs.mPotentialTracker) {
                        return 0;
                    } else {
                        return (lhs.mPotentialTracker) ? -1 : 1;
                    }
                } else {
                    return 0;
                }
            }
        };
        Arrays.sort(trackerDomains, trackerDomainComparator);
        return trackerDomains;
    }

    public static CharSequence getDisabledMessage(Resources resources,
                                            WebDefender.ProtectionStatus status) {
        if (status == null || status.mTrackerDomains == null) {
            return resources.getString(R.string.website_settings_webdefender_disabled);
        }
        int count = 0;
        for (int i = 0; i < status.mTrackerDomains.length; i++) {
            if (status.mTrackerDomains[i].mProtectiveAction !=
                    WebDefender.TrackerDomain.PROTECTIVE_ACTION_UNBLOCK) {
                count++;
            }
        }

        return count > 0
                ? Html.fromHtml(new Formatter().format(
                resources.getString(R.string.website_settings_webdefender_disabled_count),
                "<b>" + count + "</b>").toString())
                : resources.getString(R.string.website_settings_webdefender_disabled);
    }

    @Override
    public void onStop () {
        super.onStop();
        HashMap<String, Integer> updatedDomains;
        if (mVectorsRecyclerView != null) {
            updatedDomains = mVectorsRecyclerView.getUpdatedDomains();
            if (updatedDomains == null) {
                return;
            }
        } else {
            return;
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = prefs.edit();

        try {
            objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(updatedDomains);
            byte[] data = byteArrayOutputStream.toByteArray();
            objectOutputStream.close();
            byteArrayOutputStream.close();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Base64OutputStream base64OutputStream = new Base64OutputStream(out, Base64.DEFAULT);
            base64OutputStream.write(data);
            base64OutputStream.close();
            out.close();

            editor.putString(DOMAIN_PREF, new String(out.toByteArray()));
            editor.apply();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
