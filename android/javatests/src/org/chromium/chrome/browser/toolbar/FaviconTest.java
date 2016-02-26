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

package org.chromium.chrome.browser.toolbar;

import static org.chromium.base.test.util.Restriction.RESTRICTION_TYPE_TABLET;

import android.test.suitebuilder.annotation.MediumTest;
import android.view.View;

import org.chromium.base.test.util.Feature;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.ChromeActivity;
import org.chromium.chrome.browser.toolbar.ToolbarFavicon;
import org.chromium.chrome.browser.toolbar.ToolbarLayout;
import org.chromium.chrome.test.ChromeActivityTestCaseBase;

import org.chromium.content.browser.test.util.TestTouchUtils;

/**
 * Tests for toolbar manager behavior.
 */
public class FaviconTest extends ChromeActivityTestCaseBase<ChromeActivity> {

    public FaviconTest() {
        super(ChromeActivity.class);
    }

    @Override
    public void startMainActivity() throws InterruptedException {
        startMainActivityWithURL("https://www.google.com");
    }

    @MediumTest
    @Feature({"Omnibox, SWE"})
    public void testClickFavicon() throws InterruptedException {
        ToolbarLayout toolbar = (ToolbarLayout) getActivity().findViewById(R.id.toolbar);
        assertTrue(toolbar != null);

        ToolbarFavicon favicon = toolbar.getFaviconView();
        assertTrue(favicon != null);
        assertFalse(favicon.isShowingSiteSettings());

        View clickable = getActivity().findViewById(R.id.swe_favicon_badge);

        TestTouchUtils.performClickOnMainSync(getInstrumentation(), clickable);
        Thread.sleep(1000);
        assertTrue(favicon.isShowingSiteSettings());
    }

}
