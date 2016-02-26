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

package org.chromium.chrome.browser.widget;

import android.content.Context;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.View;

import org.chromium.chrome.R;
import org.chromium.ui.base.LocalizationUtils;

public class BrowserTooltip {
    private CountDownTimer mTimer;
    private static final int MAX_NOTIFICATION_DIMENSION_DP = 600;
    private final int mTooltipMaxDimensions;
    private final int mTooltipPointerOffset;

    public interface TooltipTimeout {
        void onTimeout();
    }

    private class TooltipTextBubble extends TextBubble {
        public TooltipTextBubble(Context context, Bundle bundle) {
            super(context, bundle);
            setBackgroundDrawable(new TooltipBackground(context, bundle));
        }

        class TooltipBackground extends TextBubble.BubbleBackgroundDrawable {

            private int mBoundsWidth = 0;
            private int mBoundsHeight = 0;

            TooltipBackground(Context context, Bundle res) {
                super(context, res);
            }

            @Override
            public void setColorFilter(ColorFilter cf) {
                super.setColorFilter(cf);
                mBubbleArrowDrawable.setColorFilter(cf);
                mBubbleContentsDrawable.setColorFilter(cf);
            }

            @Override
            protected void onBoundsChange(Rect bounds) {
                if (bounds == null) return;

                super.onBoundsChange(bounds);
                if (mBoundsWidth != bounds.width() || mBoundsHeight != bounds.height()) {
                    mBoundsWidth = bounds.width();
                    mBoundsHeight = bounds.height();

                    // This is done just to trigger the recalculation of pointer position
                    TooltipTextBubble.this.onLayoutChange(null, 0, 0, 0, 0, 0, 0, 0, 0);
                }
            }

            @Override
            public void setBubbleArrowXOffset(int xOffset) {
                super.setBubbleArrowXOffset(xOffset + mTooltipPointerOffset);
            }
        }
    }

    private TooltipTextBubble mTooltipBubble;
    private int mCookie;

    public BrowserTooltip(Context context, String text, View anchor,
                          int bgColorResId, int txtColorResId, int cookie) {
        float density = context.getResources().getDisplayMetrics().density;
        mTooltipMaxDimensions = (int) (density * MAX_NOTIFICATION_DIMENSION_DP);
        mCookie = cookie;

        if (anchor != null) {
            mTooltipPointerOffset =
                    (LocalizationUtils.isLayoutRtl()) ?
                            anchor.getMeasuredWidth() / 4 :
                            - (anchor.getMeasuredWidth() / 4);
        } else {
            mTooltipPointerOffset = 0;
        }

        Bundle b = new Bundle();
        b.putBoolean(TextBubble.BACKGROUND_INTRINSIC_PADDING, true);
        b.putBoolean(TextBubble.UP_DOWN, true);
        b.putInt(TextBubble.TEXT_STYLE_ID, android.R.style.TextAppearance_DeviceDefault_Medium);
        b.putInt(TextBubble.ANIM_STYLE_ID, R.style.FullscreenNotificationBubble);
        mTooltipBubble = new TooltipTextBubble(context, b);

        mTooltipBubble.getBubbleTextView().setGravity(Gravity.CENTER_HORIZONTAL);
        mTooltipBubble.getBubbleTextView().setTextColor(context.getResources()
                .getColor(txtColorResId));
        mTooltipBubble.getBackground().setAlpha(225);

        Drawable drawable = mTooltipBubble.getBackground();
        drawable.setColorFilter(context.getResources()
                .getColor(bgColorResId), PorterDuff.Mode.MULTIPLY);

        mTooltipBubble.showTextBubble(text, anchor, mTooltipMaxDimensions, mTooltipMaxDimensions);
    }

    public void setTouchListener(View.OnTouchListener l) {
        mTooltipBubble.setTouchable(l != null);
        mTooltipBubble.setTouchInterceptor(l);
    }

    public int getCookie() {
        return mCookie;
    }

    public void show(int timeout, final TooltipTimeout timeoutCallback) {
        if (mTimer != null)
            mTimer.cancel();

        mTimer = new CountDownTimer(timeout, 1000) {
            public void onTick(long millisUntilFinished) {
            }
            public void onFinish() {
                mTooltipBubble.dismiss();
                timeoutCallback.onTimeout();
            }
        };

        mTimer.start();
    }

    public void dismiss() {
        if (mTimer != null)
            mTimer.cancel();

        mTooltipBubble.dismiss();
    }
}
