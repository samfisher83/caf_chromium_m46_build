/*
 * Copyright (c) 2015, The Linux Foundation. All rights reserved.
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
package org.chromium.chrome.browser;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.TextUtils;
import android.view.WindowManager;

import org.chromium.base.CommandLine;
import org.chromium.base.metrics.RecordUserAction;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.init.AsyncInitializationActivity;
import org.chromium.chrome.browser.ntp.StoredOfflinePage;
import org.chromium.chrome.browser.offline_pages.OfflinePageBridge;
import org.chromium.chrome.browser.preferences.AboutChromePreferences;
import org.chromium.chrome.browser.preferences.BrowserHomepagePreferences;
import org.chromium.chrome.browser.preferences.PrefServiceBridge;
import org.chromium.chrome.browser.preferences.Preferences;
import org.chromium.chrome.browser.preferences.PreferencesLauncher;
import org.chromium.chrome.browser.preferences.privacy.PrivacyPreferencesManager;
import org.chromium.chrome.browser.preferences.website.WebDefenderPreferenceHandler;
import org.chromium.chrome.browser.preferences.website.WebRefinerPreferenceHandler;
import org.chromium.chrome.browser.preferences.website.WebsiteAddress;
import org.chromium.chrome.browser.profiles.Profile;
import org.chromium.chrome.browser.snackbar.Snackbar;
import org.chromium.chrome.browser.snackbar.SnackbarManager;
import org.chromium.chrome.browser.snackbar.SnackbarManager.SnackbarController;
import org.chromium.chrome.browser.tab.ChromeTab;
import org.chromium.chrome.browser.tab.Tab;
import org.chromium.chrome.browser.tabmodel.EmptyTabModel;
import org.chromium.chrome.browser.tabmodel.EmptyTabModelObserver;
import org.chromium.chrome.browser.tabmodel.EmptyTabModelSelectorObserver;
import org.chromium.chrome.browser.tabmodel.TabModel;
import org.chromium.chrome.browser.tabmodel.TabModelSelector;
import org.chromium.chrome.browser.tabmodel.TabModelUtils;
import org.chromium.chrome.browser.toolbar.ToolbarFavicon;
import org.chromium.chrome.browser.util.FeatureUtilities;
import org.chromium.components.bookmarks.BookmarkId;
import org.chromium.components.offline_pages.SavePageResult;
import org.codeaurora.swe.SWECommandLine;

import java.util.List;
import java.util.Set;

public abstract class BrowserChromeActivity extends AsyncInitializationActivity {
    protected PowerConnectionReceiver mPowerChangeReceiver;
    protected PowerConnectionReceiver mLowPowerReceiver;
    private TabModelSelector mTabModelSelector;
    private EmptyTabModelObserver mBrowserTabModelObserver;
    private static final int PREFERENCE_REQUEST = 1;
    private OfflinePageBridge mOfflinePageBridge;
    private SnackbarManager mSnackbarManager;
    public static final int DOWNLOADPATH_SELECTION = 0;

    @SuppressLint("NewApi")
    @Override
    public void postInflationStartup() {
        super.postInflationStartup();

        mPowerChangeReceiver = new PowerConnectionReceiver();
        mLowPowerReceiver = new PowerConnectionReceiver();

        IntentFilter filter = new IntentFilter();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Power save mode only exists in Lollipop and above
            filter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
        }
        filter.addAction(Intent.ACTION_BATTERY_OKAY);
        this.registerReceiver(mPowerChangeReceiver, filter);

        if (PrivacyPreferencesManager.getInstance(this).isBlockScreenObserversEnabled()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                                 WindowManager.LayoutParams.FLAG_SECURE);
        }

        mSnackbarManager = new SnackbarManager(getWindow());
    }

    @Override
    public void onResumeWithNative() {
        super.onResumeWithNative();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (PrefServiceBridge.getInstance().getPowersaveModeEnabled()) {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            } else {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            }
        }

        this.registerReceiver(mLowPowerReceiver, new IntentFilter(Intent.ACTION_BATTERY_LOW));

        reloadTabsIfNecessary();
    }

    @Override
    public void onPauseWithNative() {
        this.unregisterReceiver(mLowPowerReceiver);
        super.onPauseWithNative();
    }

    /**
     * This cannot be overridden in order to preserve destruction order.  Override
     * {@link #onDestroyInternal()} instead to perform clean up tasks.
     */
    @SuppressLint("NewApi")
    @Override
    protected void onDestroy() {
        this.unregisterReceiver(mPowerChangeReceiver);
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PREFERENCE_REQUEST && resultCode == RESULT_OK) {
            if (data.getExtras().containsKey("Secure")){
                if (data.getBooleanExtra("Secure", false)){
                    getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                                         WindowManager.LayoutParams.FLAG_SECURE);
                }
                else {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
                }
            }
        } else if (requestCode == DOWNLOADPATH_SELECTION
                   && resultCode == RESULT_OK
                   && data != null) {
            ChromeTab currentTab = ChromeTab.fromTab(getActivityTab());
            String downloadPath = data.getStringExtra(
                    SWECommandLine.getInstance(this).getDownloadFileMgrResult());
            // Fallback logic to stock browser
            if (TextUtils.isEmpty(downloadPath)) {
                Uri uri = data.getData();
                if (uri != null) downloadPath = uri.getPath();
            }

            currentTab.updateDownloadPath(downloadPath);
        }
    }

    /**
     * Sets the {@link TabModelSelector} owned by this {@link ChromeActivity}.
     * @param tabModelSelector A {@link TabModelSelector} instance.
     */
    protected void setTabModelSelector(TabModelSelector tabModelSelector) {
        mTabModelSelector = tabModelSelector;
        if (!FeatureUtilities.isDocumentMode(this)) {
            mBrowserTabModelObserver = new EmptyTabModelObserver() {
                @Override
                public void didCloseTab(Tab tab) {
                    if (!tab.isIncognito()) return;
                    boolean incognitoSessionEnded = true;
                    for (TabModel tabModel : mTabModelSelector.getModels()) {

                        if (tabModel.isIncognito() && tabModel.getCount() != 0) {
                            incognitoSessionEnded = false;
                        }
                    }
                    if (incognitoSessionEnded) {
                        WebRefinerPreferenceHandler.onIncognitoSessionFinish();
                        WebDefenderPreferenceHandler.onIncognitoSessionFinish();
                    }
                }
            };

            for (TabModel tabModel : mTabModelSelector.getModels()) {
                if (tabModel.isIncognito()) {
                    tabModel.removeObserver(mBrowserTabModelObserver);
                    tabModel.addObserver(mBrowserTabModelObserver);
                }
            }
            tabModelSelector.addObserver(new EmptyTabModelSelectorObserver() {
                @Override
                public void onTabModelSelected(TabModel newModel, TabModel oldModel) {
                    if (newModel.isIncognito()) {
                        newModel.removeObserver(mBrowserTabModelObserver);
                        newModel.addObserver(mBrowserTabModelObserver);
                    }
                }
            });
        }
    }

    /**
     * Handles menu item selection and keyboard shortcuts.
     *
     * @param id The ID of the selected menu item (defined in main_menu.xml) or
     *           keyboard shortcut (defined in values.xml).
     * @param fromMenu Whether this was triggered from the menu.
     * @return Whether the action was handled.
     */
    public boolean onMenuOrKeyboardAction(int id, boolean fromMenu) {

        if (id == R.id.about_id) {
            Intent preferencesIntent = PreferencesLauncher.createIntentForSettingsPage(
                    this, AboutChromePreferences.class.getName());
            Bundle bundle = new Bundle();
            bundle.putCharSequence(AboutChromePreferences.TABTITLE, getActivityTab().getTitle());
            bundle.putCharSequence(AboutChromePreferences.TABURL, getActivityTab().getUrl());
            preferencesIntent.putExtra(AboutChromePreferences.TABBUNDLE, bundle);
            this.startActivity(preferencesIntent, bundle);
            RecordUserAction.record("MobileMenuAbout");
            return true;
        } else if (id == R.id.offline_page_id) {
            Profile profile = Profile.getLastUsedProfile().getOriginalProfile();
            if (profile == null)
                return false;

            if (mOfflinePageBridge == null) {
                mOfflinePageBridge = new OfflinePageBridge(profile);
            }

            if (mOfflinePageBridge == null)
                return false;

            Tab currentTab = getActivityTab();

            if (currentTab != null && currentTab.isOfflinePage()) {
                currentTab.goLive();
                return true;
            }

            if (currentTab != null && !currentTab.isNativePage()
                    && !currentTab.isShowingInterstitialPage() && !currentTab.isShowingSadTab()
                    && !UrlUtilities.isInternalScheme(Uri.parse(currentTab.getUrl()))) {
                BookmarksBridge bookmarksBridge = new BookmarksBridge(profile);
                final BookmarkId bookmarkId = bookmarksBridge.addBookmark(
                        bookmarksBridge.getOtherFolderId(),
                        0, currentTab.getTitle(), StoredOfflinePage.OFFLINE_BOOKMARK_URL);

                mOfflinePageBridge.savePage(currentTab.getWebContents(),
                        bookmarkId,
                        new OfflinePageBridge.SavePageCallback() {
                            @Override
                            public void onSavePageDone(int savePageResult, String url) {
                                if (savePageResult == SavePageResult.SUCCESS) {
                                    mSnackbarManager.showSnackbar(Snackbar.make(
                                            getString(R.string.offline_readings_snackbar_saved),
                                            new SnackbarController() {
                                                @Override
                                                public void onAction(Object actionData) {}

                                                @Override
                                                public void onDismissNoAction(Object actionData) {}

                                                @Override
                                                public void onDismissForEachType(boolean isTimeout) {}
                                            }
                                    ));
                                }
                            }
                        });
            }
            return true;
        } else if (id == R.id.info_menu_id) {
            final Tab currentTab = getActivityTab();

            if (currentTab != null && currentTab.isOfflinePage()) {
                ToolbarFavicon.showOfflinePageDialog(this, currentTab);
                return true;
            }
        } else if (id == R.id.preferences_id) {
            Intent intent = PreferencesLauncher.createIntentForSettingsPage(this, null);
            Tab currentTab = getActivityTab();
            if (currentTab != null && !currentTab.isIncognito() && !currentTab.isNativePage()
                    && !currentTab.isShowingInterstitialPage() && !currentTab.isShowingSadTab()) {
                Bundle args = new Bundle();
                args.putString(BrowserHomepagePreferences.CURRENT_URL, getActivityTab().getUrl());
                intent.putExtra(Preferences.EXTRA_SHOW_FRAGMENT_ARGUMENTS, args);
            }
            this.startActivityForResult(intent, PREFERENCE_REQUEST);
            RecordUserAction.record("MobileMenuSettings");
            return true;
        }
        return false;
    }

    /**
     * Gets the current (inner) TabModel.  This is a convenience function for
     * getModelSelector().getCurrentModel().  It is *not* equivalent to the former getModel()
     * @return Never null, if modelSelector or its field is uninstantiated returns a
     *         {@link EmptyTabModel} singleton
     */
    private TabModel getCurrentTabModel() {
        if (mTabModelSelector == null) return EmptyTabModel.getInstance();
        return mTabModelSelector.getCurrentModel();
    }

    /**
     * Returns the tab being displayed by this ChromeActivity instance. This allows differentiation
     * between ChromeActivity subclasses that swap between multiple tabs (e.g. ChromeTabbedActivity)
     * and subclasses that only display one Tab (e.g. FullScreenActivity and DocumentActivity).
     *
     * The default implementation grabs the tab currently selected by the TabModel, which may be
     * null if the Tab does not exist or the system is not initialized.
     */
    private Tab getActivityTab() {
        return TabModelUtils.getCurrentTab(getCurrentTabModel());
    }


    private void reloadTabsIfNecessary() {
        Set<String> origins = PrefServiceBridge.getInstance().getOriginsPendingReload();
        boolean reload = PrefServiceBridge.getInstance().getPendingReload();
        List<TabModel> tabModels;
        if (!reload && origins.isEmpty()) {
            return;
        }
        if (FeatureUtilities.isDocumentMode(this)) {
            tabModels = ChromeApplication.getDocumentTabModelSelector().getModels();
        } else {
            tabModels = mTabModelSelector.getModels();
        }

        for (TabModel model : tabModels) {
            if (model == null) continue;
            int tabCount = model.getCount();
            for (int tabCounter = 0; tabCounter < tabCount; tabCounter++) {
                Tab tab = model.getTabAt(tabCounter);
                if (tab == null || TextUtils.isEmpty(tab.getUrl())) continue;
                if (reload) {
                    if (tab == getActivityTab()) {
                        tab.reload();
                    } else {
                        tab.setNeedsReload(true);
                    }
                } else {
                    for (String url : origins) {
                        if (TextUtils.equals(url,
                                WebsiteAddress.create(tab.getUrl()).getOrigin())
                            || TextUtils.equals(url,
                                UrlUtilities.getOriginForDisplay(Uri.parse(tab.getUrl()), true))) {
                            if (tab == mTabModelSelector.getCurrentTab()) {
                                tab.reload();
                            } else {
                                tab.setNeedsReload(true);
                            }
                        }
                    }
                }
            }
        }
        PrefServiceBridge.getInstance().reloadComplete();
    }
}
