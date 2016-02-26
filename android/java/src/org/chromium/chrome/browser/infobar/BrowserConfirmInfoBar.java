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

package org.chromium.chrome.browser.infobar;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.v7.widget.AppCompatCheckBox;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;

import org.chromium.chrome.R;
import org.chromium.chrome.browser.ContentSetting;
import org.chromium.ui.base.WindowAndroid;
import org.chromium.ui.widget.ButtonCompat;


public class BrowserConfirmInfoBar extends ConfirmInfoBar implements View.OnClickListener,
        CompoundButton.OnCheckedChangeListener {

    private Button mAllowFor24hourButton;

    private final InfoBarListeners.Confirm mConfirmListener;

    private WindowAndroid mWindowAndroid;

    boolean mRememberPreference = false;

    boolean mAllow24HourButtonClicked = false;

    public BrowserConfirmInfoBar(InfoBarListeners.Confirm confirmListener, int iconDrawableId,
                          Bitmap iconBitmap, String message, String linkText, String primaryButtonText,
                          String secondaryButtonText) {
        super(confirmListener, iconDrawableId, iconBitmap, message,
                linkText, primaryButtonText, secondaryButtonText);
        mConfirmListener = confirmListener;
    }

    private void createCustomButtonsView(InfoBarLayout layout) {

        Resources res = getContext().getResources();
        mAllowFor24hourButton = ButtonCompat.createBorderlessButton(getContext());;
        mAllowFor24hourButton.setId(R.id.button_24hour);
        mAllowFor24hourButton.setText(R.string.infobar_permission_allow_24hrs);
        mAllowFor24hourButton.setOnClickListener(this);
        mAllowFor24hourButton.setTextColor(res.getColor(R.color.infobar_accent_blue));
        layout.setCustomViewInButtonRow(mAllowFor24hourButton);

        AppCompatCheckBox rememberCheckBox = new AppCompatCheckBox(getContext());
        rememberCheckBox.setId(R.id.infobar_extra_check);
        rememberCheckBox.setText(R.string.infobar_permission_remember);
        rememberCheckBox.setTextColor(res.getColor(R.color.default_text_color));
        rememberCheckBox.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getContext().getResources().getDimension(R.dimen.infobar_text_size));
        rememberCheckBox.setChecked(true);
        mRememberPreference = true;
        rememberCheckBox.setOnCheckedChangeListener(this);
        layout.setCustomContent(rememberCheckBox);

    }

    @Override
    public void createContent(InfoBarLayout layout) {
        super.createContent(layout);
        createCustomButtonsView(layout);
    }

    @Override
    protected void setContentSettings(
            WindowAndroid windowAndroid, int[] contentSettings) {
        super.setContentSettings(windowAndroid, contentSettings);
        mWindowAndroid = windowAndroid;
    }

    @Override
    public void onClick(View view) {
        mAllow24HourButtonClicked = true;
        if (mWindowAndroid == null || getContext() == null
                || mContentSettingsToPermissionsMap == null
                || mContentSettingsToPermissionsMap.size() == 0) {
            onButtonClickedInternal(true);
            return;
        }
       requestAndroidPermissions();
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        mRememberPreference = isChecked;
        mAllowFor24hourButton.setClickable(isChecked);
        if(!isChecked) {
            mAllowFor24hourButton.setTextColor(getContext().getResources()
                    .getColor(R.color.light_normal_color));
        } else {
            mAllowFor24hourButton.setTextColor(getContext().getResources()
                    .getColor(R.color.infobar_accent_blue));
        }
    }

    @Override
    protected void onButtonClickedInternal(boolean isPrimaryButton) {
        if (mConfirmListener != null) {
            mConfirmListener.onConfirmInfoBarButtonClicked(this, isPrimaryButton);
        }
        if(isPrimaryButton) {
            if(mRememberPreference) {
                if(mAllow24HourButtonClicked) {
                    onButtonClicked(ContentSetting.ALLOW_24H, "");
                    mAllow24HourButtonClicked = false;
                } else {
                    onButtonClicked(ContentSetting.ALLOW, "");
                }
            } else {
                onButtonClicked(ContentSetting.SESSION_ONLY, "");
            }
        } else {
            if(mRememberPreference) {
                onButtonClicked(ContentSetting.BLOCK, "");
            } else {
                onCloseButtonClicked();
            }
        }
    }
}