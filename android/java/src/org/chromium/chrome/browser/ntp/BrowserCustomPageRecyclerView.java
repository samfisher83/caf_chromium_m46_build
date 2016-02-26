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
import android.graphics.Bitmap;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import org.chromium.chrome.R;
import org.chromium.chrome.browser.favicon.FaviconHelper;
import org.chromium.chrome.browser.favicon.LargeIconBridge;
import org.chromium.chrome.browser.preferences.PrefServiceBridge;
import org.chromium.chrome.browser.profiles.Profile;
import org.chromium.chrome.browser.tab.Tab;
import org.chromium.chrome.browser.widget.RoundedIconGenerator;
import org.chromium.content_public.browser.LoadUrlParams;

import java.util.ArrayList;

public class BrowserCustomPageRecyclerView extends RecyclerView {
    private LargeIconBridge mLargeIconBridge;
    private GridLayoutManager mLayoutManager;
    private int mMaxWidth;
    private int mMinMarginHorizontal;
    private int mMinMarginVertical;
    private Tab mTab;
    private FaviconHelper mFaviconHelper;
    private boolean mCaptureNeeded;

    private static final int ICON_CORNER_RADIUS_DP = 4;
    private static final int ICON_TEXT_SIZE_DP = 20;
    private static final int ICON_MIN_SIZE_DP = 48;
    private static final int GRID_SPAN_COUNT = 4;
    private static final int GRID_ITEM_ANIMATION_MS = 250;
    private static final int MIN_MARGIN_HORIZONTAL_DP = 20;

    public BrowserCustomPageRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (mLayoutManager == null) mLayoutManager =
                new GridLayoutManager(context, GRID_SPAN_COUNT);
        this.setLayoutManager(mLayoutManager);
        float density = getResources().getDisplayMetrics().density;
        mMinMarginHorizontal = Math.round(MIN_MARGIN_HORIZONTAL_DP * density);
        mMaxWidth = Math.round(getResources()
                .getDimension(R.dimen.icon_most_visited_layout_max_width));
        mMinMarginVertical = Math.round(getResources()
                .getDimension(R.dimen.icon_most_visited_layout_no_logo_padding_top));

        this.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                mCaptureNeeded = true;
            }
        });
    }

    public void setup(ArrayList<PredefinedSite> predefinedSites, Tab tab) {
        mTab = tab;
        if (mFaviconHelper == null) mFaviconHelper = new FaviconHelper();
        this.setAdapter(new BrowserCustomPageViewAdapter(predefinedSites));
    }

    /*
        Determine how many items fit in a row dynamically.
     */
    @Override
    public void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        float itemWidth = getResources().getDimension(R.dimen.icon_most_visited_tile_width)
                + getResources().getDimension(R.dimen.icon_most_visited_max_horizontal_spacing);
        int width = MeasureSpec.getSize(widthSpec);
        int excessWidth = width - mMaxWidth;
        int horizontalPadding = mMinMarginHorizontal;

        if (excessWidth > 0) {
            horizontalPadding += excessWidth / 2;
            width = mMaxWidth;
        }

        setPadding(horizontalPadding, mMinMarginVertical, horizontalPadding, 0);

        if (width != 0) {
            int spanCount = Math.round(width / itemWidth);
            if (spanCount > 0) {
                mLayoutManager.setSpanCount(spanCount);
            }
        }
    }

    public boolean captureNeeded() {
        return mCaptureNeeded;
    }

    public void updateCapture() {
        mCaptureNeeded = false;
    }

    private class BrowserCustomPageViewAdapter extends Adapter {
        ArrayList<PredefinedSite> mDataSet;

        public BrowserCustomPageViewAdapter(ArrayList<PredefinedSite> dataSet) {
            mDataSet = dataSet;
        }

        private class CustomPageItemHolder extends RecyclerView.ViewHolder {
            private TextView mTitle;
            private ImageView mThumbnail;
            private PredefinedSite mSite;

            public CustomPageItemHolder(View itemView) {
                super(itemView);
                mTitle = (TextView)itemView.findViewById(R.id.most_visited_title);
                mThumbnail = (ImageView) itemView.findViewById(R.id.most_visited_icon);
                itemView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mTab.loadUrl(new LoadUrlParams(mSite.getUrl()));
                    }
                });
            }

            public void setTitle(String title) {
                mTitle.setText(title);
            }

            public void setThumbnail(String url, String iconUrl) {
                if (mLargeIconBridge == null) {
                    mLargeIconBridge = new LargeIconBridge(Profile.getLastUsedProfile());
                }
                String requestUrl =  TextUtils.isEmpty(iconUrl) ? url : iconUrl;
                CustomPageLargeIconCallback callback =
                        new CustomPageLargeIconCallback(requestUrl, url, mThumbnail);
                mLargeIconBridge.getLargeIconForUrl(requestUrl, ICON_MIN_SIZE_DP, callback);
            }

            /*
            Setup/Update information for this ViewHolder.
             */
            public void refreshInformation(final PredefinedSite site) {
                mSite = site;
                if (!TextUtils.isEmpty(site.getIconUrl())) {
                    mFaviconHelper.ensureFaviconIsAvailable(Profile.getLastUsedProfile(),
                            mTab.getWebContents(), site.getIconUrl(), site.getIconUrl(),
                            new FaviconHelper.FaviconAvailabilityCallback() {
                                @Override
                                public void onFaviconAvailabilityChecked(boolean newlyAvailable) {
                                    if (newlyAvailable) {
                                        setThumbnail(site.getUrl(), site.getIconUrl());
                                    }
                                }
                            });
                }
                setTitle(site.getTitle());
                setThumbnail(site.getUrl(), site.getIconUrl());
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.icon_most_visited_item, parent, false);
            return new CustomPageItemHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            PredefinedSite site = mDataSet.get(position);
            ((CustomPageItemHolder)holder).refreshInformation(site);
            if (!PrefServiceBridge.getInstance().getPowersaveModeEnabled()) {
                setAnimation(holder.itemView);
            }
        }

        /*
        Fade-in animation for items/
         */
        private void setAnimation(View itemView) {
            AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
            anim.setDuration(GRID_ITEM_ANIMATION_MS);
            itemView.startAnimation(anim);
        }

        @Override
        public int getItemCount() {
            return mDataSet.size();
        }
    }

    private class CustomPageLargeIconCallback implements LargeIconBridge.LargeIconCallback {
        String mRequestedUrl;
        String mPageUrl;
        ImageView mThumbnail;

        public CustomPageLargeIconCallback(String requestedUrl, String url, ImageView thumbnail) {
            mRequestedUrl = requestedUrl;
            mPageUrl = url;
            mThumbnail = thumbnail;
        }

        @Override
        public void onLargeIconAvailable(Bitmap icon, int fallbackColor) {
            mCaptureNeeded = true;
            if (icon == null) {
                RoundedIconGenerator roundedIconGenerator = new RoundedIconGenerator(
                        getContext(), ICON_MIN_SIZE_DP, ICON_MIN_SIZE_DP,
                        ICON_CORNER_RADIUS_DP, fallbackColor,
                        ICON_TEXT_SIZE_DP);
                icon = roundedIconGenerator.generateIconForUrl(mPageUrl);
                mThumbnail.setImageBitmap(icon);
            } else {
                RoundedBitmapDrawable roundedIcon = RoundedBitmapDrawableFactory.create(
                        getResources(), icon);
                int cornerRadius = Math.round(ICON_CORNER_RADIUS_DP * icon.getWidth()
                        / ICON_MIN_SIZE_DP);
                roundedIcon.setCornerRadius(cornerRadius);
                roundedIcon.setAntiAlias(true);
                roundedIcon.setFilterBitmap(true);
                mThumbnail.setImageDrawable(roundedIcon);
            }
        }
    }

}