/*
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
*/

package org.chromium.chrome.browser.ntp;

import android.content.Context;
import android.graphics.Canvas;
import android.view.LayoutInflater;
import android.view.View;

import org.chromium.chrome.R;
import org.chromium.chrome.browser.NativePage;
import org.chromium.chrome.browser.UrlConstants;
import org.chromium.chrome.browser.compositor.layouts.content.InvalidationAwareThumbnailProvider;
import org.chromium.chrome.browser.tab.Tab;

import java.util.ArrayList;

public class BrowserCustomPage implements NativePage, InvalidationAwareThumbnailProvider {
    private BrowserCustomPageView mPageView;
    private Context mContext;
    private int mCaptureWidth;
    private int mCaptureHeight;

    public BrowserCustomPage(Context context, Tab tab, ArrayList<PredefinedSite> predefinedSites) {
        LayoutInflater inflater = LayoutInflater.from(context);
        mContext = context;
        mPageView = (BrowserCustomPageView) inflater.inflate(R.layout.browser_custom_page, null);
        mPageView.setup(predefinedSites, tab);
    }

    @Override
    public boolean shouldCaptureThumbnail() {
        return mPageView.getHeight() != mCaptureHeight ||
                mPageView.getWidth() != mCaptureWidth ||
                mPageView.captureNeeded();

    }

    @Override
    public void captureThumbnail(Canvas canvas) {
        mCaptureHeight = mPageView.getHeight();
        mCaptureWidth = mPageView.getWidth();
        mPageView.updateCapture();
    }

    @Override
    public View getView() {
        return mPageView;
    }

    @Override
    public String getTitle() {
        return mContext.getResources().getString(R.string.button_new_tab); //Title shouldn't change.
    }

    @Override
    public String getUrl() {
        return UrlConstants.NTP_URL;
    } //pose as NTP

    @Override
    public String getHost() {
        return UrlConstants.NTP_HOST;
    } //pose as NTP

    @Override
    public int getBackgroundColor() {
        return mContext.getResources().getColor(R.color.ntp_bg);
    }

    @Override
    public void updateForUrl(String url) {

    }

    @Override
    public void destroy() {

    }
}