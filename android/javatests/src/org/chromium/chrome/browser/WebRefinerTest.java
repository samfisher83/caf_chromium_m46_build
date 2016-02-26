/*
 *  Copyright (c) 2015-2016, The Linux Foundation. All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 *      * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 *  ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 *  BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 *  BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 *  WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 *  OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 *  IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Portions of this file derived from Chromium code, which is BSD licensed, copyright The Chromium Authors.
 */

package org.codeaurora.swe.test;

import android.app.Activity;
import android.content.Context;
import android.test.suitebuilder.annotation.MediumTest;
import android.util.Log;
import android.util.Pair;

import org.chromium.base.test.util.Feature;
import org.chromium.chrome.browser.ChromeActivity;
import org.chromium.chrome.browser.tab.EmptyTabObserver;
import org.chromium.chrome.browser.tab.Tab;
import org.chromium.chrome.browser.tab.TabObserver;
import org.chromium.chrome.browser.tabmodel.TabModel;
import org.chromium.chrome.browser.tabmodel.TabModelUtils;
import org.chromium.chrome.test.ChromeTabbedActivityTestBase;
import org.chromium.chrome.test.util.ChromeTabUtils;
import org.chromium.chrome.test.util.browser.TabLoadObserver;
import org.chromium.content.browser.test.util.CallbackHelper;
import org.chromium.content.browser.ContentViewCore;
import org.chromium.content.browser.test.util.DOMUtils;
import org.chromium.content.browser.WebRefiner;
import org.chromium.content.browser.WebRefiner.RuleSet;
import org.chromium.content.browser.WebDefender;
import org.chromium.net.test.util.TestWebServer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;


public class WebRefinerTest extends ChromeTabbedActivityTestBase {
    private static final String LOGTAG = "WebRefinerTest";

    private ChromeActivity mActivity;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
    }

    @Override
    public void startMainActivity() throws InterruptedException {
        startMainActivityFromLauncher();
    }

    @MediumTest
    @Feature({"WebRefiner"})
    public void testInitialization() throws Exception {
        WebRefiner wbr = WebRefiner.getInstance();
        assertNotNull(wbr);
        assertTrue(WebRefiner.isInitialized());

        WebDefender wbdfndr = WebDefender.getInstance();
        assertNotNull(wbdfndr);
        assertTrue(WebDefender.isInitialized());
    }

    private boolean writeToFile(String data, String fileName) {

        boolean result = true;
        try {
            File file = new File(mActivity.getApplicationInfo().dataDir, fileName);
            OutputStream os = null;
            try {
                os = new FileOutputStream(file);
                os.write(data.getBytes());
                os.flush();
            } finally {
                if (os != null) {
                    os.close();
                }
            }
        } catch (Exception e) {
            Log.e(LOGTAG, e.getMessage());
            result = false;
        }

        return result;
    }

    public void loadUrlAndWaitForPageLoadCompletion(String url)  throws InterruptedException {
        Tab currentTab = getActivity().getActivityTab();
        final CallbackHelper loadedCallback = new CallbackHelper();
        final AtomicBoolean tabCrashReceived = new AtomicBoolean();
        currentTab.addObserver(new EmptyTabObserver() {
            @Override
            public void onPageLoadFinished(Tab tab) {
                loadedCallback.notifyCalled();
                tab.removeObserver(this);
            }

            @Override
            public void onCrash(Tab tab, boolean sadTabShown) {
                tabCrashReceived.set(true);
                tab.removeObserver(this);
            }
        });

        loadUrl(url);
        assertEquals(url, getActivity().getActivityTab().getUrl());

        boolean pageLoadReceived = true;
        try {
            loadedCallback.waitForCallback(0);
        } catch (TimeoutException ex) {
            pageLoadReceived = false;
        }

        assertTrue("Neither PAGE_LOAD_FINISHED nor a TAB_CRASHED event was received",
                pageLoadReceived || tabCrashReceived.get());
    }

    boolean waitForRuleSetToApply(WebRefiner wbr) {
        boolean result = true;
        try {
            Class noparams[] = {};
            Method method = wbr.getClass().getDeclaredMethod("ensurePendingRuleSetsApplied", noparams);
            method.invoke(wbr,  (Object[]) null);
        } catch(Exception e){
            e.printStackTrace();
            result = false;
        }
        return result;
    }

    private static final String TEST_PAGE_CONTENT = "<html>\n" +
            "<head>\n" +
            "    <script src=\"http://localhost/ad_script01.js\" type=\"text/javascript\"></script>\n" +
            "    <link rel=\"stylesheet\" type=\"text/css\" href=\"http://localhost/ad_style01.css\">\n" +
            "</head>\n" +
            "<body>\n" +
            "    <p>WebRefiner test page.</p>\n" +
            "    <iframe src=\"http://localhost/ad_frame01.html\"></iframe>\n" +
            "    <iframe src=\"http://localhost/ad_frame02.html\"></iframe>\n" +
            "    <script src=\"http://localhost/ad_script02.js\" type=\"text/javascript\"></script>\n" +
            "    <link rel=\"stylesheet\" type=\"text/css\" href=\"http://localhost/ad_style02.css\">\n" +
            "    <img src=\"http://localhost/ad_img01.jpg\">\n" +
            "    <img src=\"http://localhost/ad_img02.png\">\n" +
            "</body>";

    private static final String RULE_SET_DATA = "ad_frame\n" +
                                                "ad_img\n" +
                                                "ad_style\n" +
                                                "ad_script\n";

    @MediumTest
    @Feature({"WebRefiner"})
    public void testRuleSet() throws Exception {

        TestWebServer webServer = TestWebServer.start();
        try {

            WebRefiner wbr = WebRefiner.getInstance();
            assertNotNull(wbr);

            String ruleSetFileName = "rule_set_01.rules";
            assertTrue(writeToFile(RULE_SET_DATA, ruleSetFileName));

            RuleSet rs = new RuleSet("TestFilters", new File(mActivity.getApplicationInfo().dataDir, ruleSetFileName).getAbsolutePath(), WebRefiner.RuleSet.CATEGORY_ADS, 1);
            wbr.addRuleSet(rs);

            assertTrue(waitForRuleSetToApply(wbr));

            final String urlString = webServer.setResponse("/webrefiner_test.html", TEST_PAGE_CONTENT , null);
            final URL url = new URL(urlString);
            final int expectedTotalURLs = 8/*subrequests*/ + 1/*main request*/;

            // Subtest 1
            {
            loadUrlAndWaitForPageLoadCompletion(urlString);

            final ContentViewCore cvc = getActivity().getActivityTab().getContentViewCore();
            int actualTotalURLs = wbr.getTotalURLCount(cvc);
            // Sometimes favicon urls get mixed up with the page session, here we account for the
            // favicon url to be counted.
            assertTrue((actualTotalURLs >= expectedTotalURLs && actualTotalURLs <= (expectedTotalURLs + 1)));

            assertEquals(8, wbr.getBlockedURLCount(cvc));

            WebRefiner.PageInfo pageInfo = wbr.getPageInfo(cvc);
            assertNotNull(pageInfo);
            assertEquals(actualTotalURLs, pageInfo.mTotalUrls);
            assertEquals(8, pageInfo.mBlockedUrls);
            assertEquals(0, pageInfo.mWhiteListedUrls);
            assertEquals(pageInfo.mBlockedUrls + pageInfo.mWhiteListedUrls, pageInfo.mMatchedURLInfoList.length);

            int ads = 0;
            int trackers = 0;
            int malwares = 0;
            int images = 0;
            int scripts = 0;
            int stylesheets = 0;
            int subframes = 0;
            int whitelisted = 0;
            int blocked = 0;

            for (WebRefiner.MatchedURLInfo urlInfo : pageInfo.mMatchedURLInfoList) {
                if (urlInfo.mActionTaken == WebRefiner.MatchedURLInfo.ACTION_BLOCKED) {
                    blocked++;
                    switch (urlInfo.mMatchedFilterCategory) {
                        case WebRefiner.RuleSet.CATEGORY_ADS:
                            ads++;
                            break;
                        case WebRefiner.RuleSet.CATEGORY_TRACKERS:
                            trackers++;
                            break;
                        case WebRefiner.RuleSet.CATEGORY_MALWARE_DOMAINS:
                            malwares++;
                            break;
                    }
                    if (0 == urlInfo.mType.compareTo("Image")) {
                        images++;
                    } else if (0 == urlInfo.mType.compareTo("Script")) {
                        scripts++;
                    } else if (0 == urlInfo.mType.compareTo("Stylesheet")) {
                        stylesheets++;
                    } else if (0 == urlInfo.mType.compareTo("SubFrame")) {
                        subframes++;
                    }
                } else if (urlInfo.mActionTaken == WebRefiner.MatchedURLInfo.ACTION_WHITELISTED) {
                    whitelisted++;
                }
            }

            assertEquals(8, ads);
            assertEquals(0, trackers);
            assertEquals(0, malwares);
            assertEquals(2, images);
            assertEquals(2, scripts);
            assertEquals(2, stylesheets);
            assertEquals(2, subframes);
            assertEquals(0, whitelisted);
            assertEquals(8, blocked);

            String origin = url.getProtocol() + "://" + url.getHost();
            String[] origins = new String[1];
            origins[0] = origin;
            }

            // Subtest 2 - disable default
            {
            wbr.setDefaultPermission(false);

            loadUrlAndWaitForPageLoadCompletion(urlString);

            final ContentViewCore cvc = getActivity().getActivityTab().getContentViewCore();
            int actualTotalURLs = wbr.getTotalURLCount(cvc);
            assertTrue((actualTotalURLs >= expectedTotalURLs && actualTotalURLs <= (expectedTotalURLs + 1)));

            assertEquals(0, wbr.getBlockedURLCount(cvc));
            WebRefiner.PageInfo pageInfo = wbr.getPageInfo(cvc);
            assertNotNull(pageInfo);
            assertEquals(actualTotalURLs, pageInfo.mTotalUrls);
            assertEquals(0, pageInfo.mBlockedUrls);
            assertEquals(0, pageInfo.mWhiteListedUrls);
            assertNotNull(pageInfo.mMatchedURLInfoList);
            assertEquals(pageInfo.mBlockedUrls + pageInfo.mWhiteListedUrls, pageInfo.mMatchedURLInfoList.length);
            }

            // Subtest 3 - Enable default
            {
            wbr.setDefaultPermission(true);

            loadUrlAndWaitForPageLoadCompletion(urlString);

            final ContentViewCore cvc = getActivity().getActivityTab().getContentViewCore();
            int actualTotalURLs = wbr.getTotalURLCount(cvc);
            assertTrue((actualTotalURLs >= expectedTotalURLs && actualTotalURLs <= (expectedTotalURLs + 1)));

            assertEquals(8, wbr.getBlockedURLCount(cvc));
            WebRefiner.PageInfo pageInfo = wbr.getPageInfo(cvc);
            assertNotNull(pageInfo);
            assertEquals(actualTotalURLs, pageInfo.mTotalUrls);
            assertEquals(8, pageInfo.mBlockedUrls);
            assertEquals(0, pageInfo.mWhiteListedUrls);
            assertNotNull(pageInfo.mMatchedURLInfoList);
            assertEquals(pageInfo.mBlockedUrls + pageInfo.mWhiteListedUrls, pageInfo.mMatchedURLInfoList.length);
            }

            // Subtest 4 - enable default, but disable for origin
            {
            String origin = url.getProtocol() + "://" + url.getHost() + ":" + url.getPort() + "/";
            String[] origins = new String[1];
            origins[0] = origin;
            wbr.setPermissionForOrigins(origins, WebRefiner.PERMISSION_DISABLE, false);

            loadUrlAndWaitForPageLoadCompletion(urlString);

            final ContentViewCore cvc = getActivity().getActivityTab().getContentViewCore();
            int actualTotalURLs = wbr.getTotalURLCount(cvc);
            assertTrue((actualTotalURLs >= expectedTotalURLs && actualTotalURLs <= (expectedTotalURLs + 1)));

            assertEquals(0, wbr.getBlockedURLCount(cvc));
            WebRefiner.PageInfo pageInfo = wbr.getPageInfo(cvc);
            assertNotNull(pageInfo);
            assertEquals(actualTotalURLs, pageInfo.mTotalUrls);
            assertEquals(0, pageInfo.mBlockedUrls);
            assertEquals(0, pageInfo.mWhiteListedUrls);
            assertNotNull(pageInfo.mMatchedURLInfoList);
            assertEquals(pageInfo.mBlockedUrls + pageInfo.mWhiteListedUrls, pageInfo.mMatchedURLInfoList.length);
            }

        } finally {
            webServer.shutdown();
        }
    }
}
