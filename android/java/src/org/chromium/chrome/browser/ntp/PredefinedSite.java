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

import android.text.TextUtils;

import org.chromium.chrome.browser.UrlUtilities;

/**
 * Object to hold predefined website information
 */
public class PredefinedSite {
    private String mTitle;
    private String mUrl;
    private String mIconUrl;


    public PredefinedSite(String title, String url, String iconUrl) throws IllegalArgumentException {
        mTitle = title;
        mUrl = UrlUtilities.fixupUrl(url);
        mIconUrl = UrlUtilities.fixupUrl(iconUrl);

        if (TextUtils.isEmpty(mUrl)) {
            throw new IllegalArgumentException("Invalid URL");
        }
    }

    public String getTitle() {
        return mTitle;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getIconUrl() {
        return mIconUrl;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) return true;
        if (!(object instanceof PredefinedSite)) return false;
        PredefinedSite input = (PredefinedSite) object;
        return TextUtils.equals(input.getUrl(), getUrl());
    }

    @Override
    public int hashCode() { return 31 * mTitle.hashCode(); }
}
