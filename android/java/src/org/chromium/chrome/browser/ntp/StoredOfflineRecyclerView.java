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

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import org.chromium.chrome.R;
import org.chromium.chrome.browser.BookmarksBridge;
import org.chromium.chrome.browser.favicon.LargeIconBridge;
import org.chromium.chrome.browser.offline_pages.OfflinePageBridge;
import org.chromium.chrome.browser.offline_pages.OfflinePageBridge.OfflinePageModelObserver;
import org.chromium.chrome.browser.offline_pages.OfflinePageItem;
import org.chromium.chrome.browser.preferences.PrefServiceBridge;
import org.chromium.chrome.browser.tab.Tab;
import org.chromium.chrome.browser.widget.RoundedIconGenerator;
import org.chromium.components.bookmarks.BookmarkId;
import org.chromium.components.offline_pages.DeletePageResult;
import org.chromium.content_public.browser.LoadUrlParams;

import java.util.Collections;
import java.util.List;

public class StoredOfflineRecyclerView extends RecyclerView {
    // Context menu item IDs.
    static final int ID_REMOVE = 0;

    private LargeIconBridge mLargeIconBridge;
    private StoredOfflineViewAdapter mAdapter;
    private GridLayoutManager mLayoutManager;
    private Tab mTab;
    private OfflinePageBridge mOfflinePageBridge;
    private TextView mEmptyView;
    private int mMaxWidth;
    private int mMinMargin;
    private int mMinMarginVertical;
    private boolean mCaptureNeeded;

    private static final int ICON_CORNER_RADIUS_DP = 4;
    private static final int ICON_TEXT_SIZE_DP = 20;
    private static final int ICON_MIN_SIZE_DP = 48;
    private static final int GRID_SPAN_COUNT = 4;
    private static final int GRID_ITEM_ANIMATION_MS = 250;
    private static final int MAX_VIEW_WIDTH_DP = 550;
    private static final int MIN_MARGIN_DP = 6;

    public StoredOfflineRecyclerView(Context context, AttributeSet attributeSet) {
        super(context);
        this.setId(R.id.stored_offline_recycler_view);
        mAdapter = new StoredOfflineViewAdapter();
        this.setAdapter(mAdapter);

        if (mLayoutManager == null) mLayoutManager =
                new GridLayoutManager(context, GRID_SPAN_COUNT);
        this.setLayoutManager(mLayoutManager);
        float density = getResources().getDisplayMetrics().density;
        mMaxWidth = Math.round(MAX_VIEW_WIDTH_DP * density);
        mMinMargin = Math.round(MIN_MARGIN_DP * density);
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
        int horizontalPadding = mMinMargin;

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

    public void initialize(Tab tab,
            OfflinePageBridge offlinePageBridge, TextView emptyView) {
        mTab = tab;
        mOfflinePageBridge = offlinePageBridge;
        mEmptyView = emptyView;

        if (mOfflinePageBridge.isOfflinePageModelLoaded()) {
            mAdapter.setOfflinePagesList(mOfflinePageBridge.getAllPages());
        } else {
            mOfflinePageBridge.addObserver(new OfflinePageModelObserver() {
                @Override
                public void offlinePageModelLoaded() {
                    mAdapter.setOfflinePagesList(mOfflinePageBridge.getAllPages());
                }
            });
        }
    }

    private class StoredOfflineViewAdapter extends Adapter {
        private class OfflinePageItemHolder extends RecyclerView.ViewHolder
                implements MenuItem.OnMenuItemClickListener {
            private TextView mTitle;
            private ImageView mThumbnail;
            private OfflinePageItem mOfflinePageItem;

            public OfflinePageItemHolder(View itemView) {
                super(itemView);
                mTitle = (TextView)itemView.findViewById(R.id.most_visited_title);
                mThumbnail = (ImageView) itemView.findViewById(R.id.most_visited_icon);
                itemView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mTab.loadUrl(new LoadUrlParams(mOfflinePageItem.getOfflineUrl()));
                    }
                });
                itemView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
                    @Override
                    public void onCreateContextMenu(ContextMenu menu, View v,
                                                    ContextMenu.ContextMenuInfo menuInfo) {
                        menu.add(Menu.NONE, ID_REMOVE, Menu.NONE, R.string.remove)
                                 .setOnMenuItemClickListener(OfflinePageItemHolder.this);
                    }
                });
            }

            public void setTitle(String title) {
                mTitle.setText(title);
            }

            public void setThumbnail(String url) {
                if (mLargeIconBridge == null) {
                    mLargeIconBridge = new LargeIconBridge(mTab.getProfile());
                }
                OfflinePageLargeIconCallback callback = new OfflinePageLargeIconCallback(url, mThumbnail);
                mLargeIconBridge.getLargeIconForUrl(url, ICON_MIN_SIZE_DP, callback);
            }

            /*
            Setup/Update information for this ViewHolder.
             */
            public void refreshInformation(OfflinePageItem offlinePageItem) {
                BookmarksBridge bookmarksBridge = new BookmarksBridge(mTab.getProfile());
                BookmarkId id = offlinePageItem.getBookmarkId();
                String title = bookmarksBridge.getBookmarkById(id).getTitle();

                mOfflinePageItem = offlinePageItem;

                setTitle(title);
                setThumbnail(offlinePageItem.getUrl());
            }

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case ID_REMOVE:
                        mOfflinePageBridge.deletePage(mOfflinePageItem.getBookmarkId(),
                                new OfflinePageBridge.DeletePageCallback() {
                                    @Override
                                    public void onDeletePageDone(int deletePageResult) {
                                        if (deletePageResult == DeletePageResult.SUCCESS) {
                                            mAdapter.setOfflinePagesList(mOfflinePageBridge.getAllPages());
                                        }
                                    }
                                });
                        return true;
                    default:
                        return false;
                }
            }
        }

        public List<OfflinePageItem> mOfflinePages = Collections.emptyList();

        /**
         * Sets the offline pages list for adapter.
         * @param offlinePages OfflinePageItem list.
         */
        public void setOfflinePagesList(List<OfflinePageItem> offlinePages) {
            if (mEmptyView.length() == 0) {
                mEmptyView.setText(R.string.offline_readings_empty);
            }

            mEmptyView.setVisibility(offlinePages.isEmpty() ? View.VISIBLE
                                                            : View.GONE);

            mOfflinePages = offlinePages;
            this.notifyDataSetChanged();
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            OfflinePageItem offlinePageItem = mOfflinePages.get(position);
            ((OfflinePageItemHolder)holder).refreshInformation(offlinePageItem);
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
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.icon_most_visited_item, parent, false);
            return new OfflinePageItemHolder(view);
        }

        @Override
        public int getItemCount() {
            return mOfflinePages.size();
        }
    }

    private class OfflinePageLargeIconCallback implements LargeIconBridge.LargeIconCallback {
        String mUrl;
        ImageView mThumbnail;

        public OfflinePageLargeIconCallback(String url, ImageView thumbnail) {
            mUrl = url;
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
                icon = roundedIconGenerator.generateIconForUrl(mUrl);
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
