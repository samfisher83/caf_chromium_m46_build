/*
 * Copyright (c) 2016, The Linux Foundation. All rights reserved.
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
package org.chromium.chrome.browser.ntp;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;

import org.chromium.chrome.R;
import org.chromium.chrome.browser.NativePage;
import org.chromium.chrome.browser.compositor.layouts.content.InvalidationAwareThumbnailProvider;
import org.chromium.chrome.browser.offline_pages.OfflinePageBridge;
import org.chromium.chrome.browser.profiles.Profile;
import org.chromium.chrome.browser.tab.Tab;
import org.chromium.chrome.browser.util.ViewUtils;

public class StoredOfflinePage
             implements NativePage, InvalidationAwareThumbnailProvider {
    public static final String OFFLINE_BOOKMARK_URL = "swe://offline";

    private final Profile mProfile;
    private OfflinePageBridge mOfflinePageBridge;

    private final StoredOfflineView mPageView;

    public StoredOfflinePage(Activity activity, Tab tab) {
        mProfile = tab.getProfile();
        LayoutInflater inflater = LayoutInflater.from(activity);
        mPageView = (StoredOfflineView)
                inflater.inflate(R.layout.stored_offline_page, null);

        mOfflinePageBridge = new OfflinePageBridge(mProfile);

        mPageView.initialize(tab, mOfflinePageBridge);
    }

    public boolean isEmpty() { //Assume not empty if the model is not loaded.
        return mOfflinePageBridge.isOfflinePageModelLoaded()
                && mOfflinePageBridge.getAllPages().isEmpty();
    }

    public void addObserver(OfflinePageBridge.OfflinePageModelObserver modelObserver) {
        mOfflinePageBridge.addObserver(modelObserver);
    }

    @Override
    public View getView() {
        return mPageView;
    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public String getUrl() {
        return null;
    }

    @Override
    public String getHost() {
        return null;
    }

    @Override
    public int getBackgroundColor() {
        return Color.WHITE;
    }

    @Override
    public void updateForUrl(String url) {
    }

    @Override
    public void destroy() {
        mOfflinePageBridge.destroy();
    }

    // InvalidationAwareThumbnailProvider

    @Override
    public boolean shouldCaptureThumbnail() {
        return mPageView.shouldCaptureThumbnail();
    }

    @Override
    public void captureThumbnail(Canvas canvas) {
        ViewUtils.captureBitmap(mPageView, canvas);
        mPageView.updateThumbnailState();
    }

    public void onExternalCapture() {
        mPageView.updateThumbnailState();
    }
}
