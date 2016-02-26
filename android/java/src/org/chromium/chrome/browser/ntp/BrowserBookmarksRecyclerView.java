/*
 * Copyright (c) 2015-2016, The Linux Foundation. All rights reserved.
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
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import org.chromium.base.VisibleForTesting;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.BookmarksBridge;
import org.chromium.chrome.browser.favicon.LargeIconBridge;
import org.chromium.chrome.browser.preferences.PrefServiceBridge;
import org.chromium.chrome.browser.profiles.Profile;
import org.chromium.chrome.browser.widget.RoundedIconGenerator;

public class BrowserBookmarksRecyclerView extends RecyclerView {
    private BookmarksInternalListView mBookmarksInternalListView;
    private LargeIconBridge mLargeIconBridge;
    private GridLayoutManager mLayoutManager;
    private BaseAdapter mAdapter;
    private BookmarksPageView.BookmarksPageManager mManager;
    private BookmarkItemView.DrawingData mDrawingData;
    private int mMaxWidth;
    private int mMinMargin;
    private boolean mCaptureNeeded;

    private static final int ICON_CORNER_RADIUS_DP = 4;
    private static final int ICON_TEXT_SIZE_DP = 20;
    private static final int ICON_MIN_SIZE_DP = 48;
    private static final int GRID_SPAN_COUNT = 4;
    private static final int GRID_ITEM_ANIMATION_MS = 250;
    private static final int MAX_BOOKMARKS_VIEW_WIDTH_DP = 550;
    private static final int MIN_MARGIN_DP = 6;

    public BrowserBookmarksRecyclerView(Context context, AttributeSet attributeSet) {
        super(context);
        this.setId(R.id.bookmarks_list_view);
        mBookmarksInternalListView = new BookmarksInternalListView(context, attributeSet);
        if (mLayoutManager == null) mLayoutManager =
                new GridLayoutManager(context, GRID_SPAN_COUNT);
        this.setLayoutManager(mLayoutManager);
        float density = getResources().getDisplayMetrics().density;
        mMaxWidth = Math.round(MAX_BOOKMARKS_VIEW_WIDTH_DP * density);
        mMinMargin = Math.round(MIN_MARGIN_DP * density);

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

        View folderView = getRootView().findViewById(R.id.folder_structure_scroll_view);

        setPadding(horizontalPadding, 0, horizontalPadding, 0);

        if (folderView != null) {
            folderView.setPadding(horizontalPadding, folderView.getPaddingTop(),
                    horizontalPadding, folderView.getPaddingBottom());
        }

        if (width != 0) {
            int spanCount = Math.round(width / itemWidth);
            if (spanCount > 0) {
                mLayoutManager.setSpanCount(spanCount);
            }
        }
    }

    public BookmarksInternalListView getList() {
        return mBookmarksInternalListView;
    }

    public void setManager(BookmarksPageView.BookmarksPageManager manager) {
        mManager = manager;
        if (mDrawingData == null) mDrawingData = new BookmarkItemView.DrawingData(getContext());
    }

    public void updateCapture() {
        mCaptureNeeded = false;
    }

    public boolean captureNeeded() {
        return mCaptureNeeded;
    }

    private class BookmarksInternalListView extends BookmarksListView {

        public BookmarksInternalListView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        public void setAdapter(ListAdapter adapter) {
            super.setAdapter(adapter);
            mAdapter = (BaseAdapter) adapter;
            BrowserBookmarksRecyclerView.this.setAdapter(new BrowserBookmarksViewAdapter());
            mAdapter.registerDataSetObserver(new DataSetObserver() {
                @Override
                public void onChanged() {
                    super.onChanged();
                    BrowserBookmarksRecyclerView.this.getAdapter().notifyDataSetChanged();
                }
            });
        }
    }

    private class BrowserBookmarksViewAdapter extends Adapter {

        private class BookmarkItemHolder extends RecyclerView.ViewHolder {
            private TextView mTitle;
            private ImageView mThumbnail;
            private BookmarksBridge.BookmarkItem mBookmarkItem;
            private BookmarkItemView mBookmarkItemView;

            public BookmarkItemHolder(View itemView) {
                super(itemView);
                mTitle = (TextView)itemView.findViewById(R.id.most_visited_title);
                mThumbnail = (ImageView) itemView.findViewById(R.id.most_visited_icon);
                itemView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mBookmarkItemView.onClick(v);
                    }
                });
                itemView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
                    @Override
                    public void onCreateContextMenu(ContextMenu menu, View v,
                                                    ContextMenu.ContextMenuInfo menuInfo) {
                        mBookmarkItemView.onCreateContextMenu(menu, v, menuInfo);
                    }
                });
            }

            public void setTitle(String title) {
                mTitle.setText(title);
            }

            public void setThumbnail(String url) {
                if (mLargeIconBridge == null) {
                    mLargeIconBridge = new LargeIconBridge(Profile.getLastUsedProfile());
                }
                BookmarkLargeIconCallback callback = new BookmarkLargeIconCallback(url, mThumbnail);
                mLargeIconBridge.getLargeIconForUrl(url, ICON_MIN_SIZE_DP, callback);
            }

            /*
            Setup/Update information for this ViewHolder.
             */
            public void refreshInformation(BookmarksBridge.BookmarkItem bookmarkItem) {
                mBookmarkItem = bookmarkItem;
                if (mManager != null) {
                    mBookmarkItemView = new BookmarkItemView(getContext(), mManager,
                            bookmarkItem.getId(), bookmarkItem.getTitle(), bookmarkItem.getUrl(),
                            bookmarkItem.isEditable(), bookmarkItem.isManaged(), mDrawingData);
                }
                setTitle(bookmarkItem.getTitle());
                if (!bookmarkItem.isFolder()) {
                    setThumbnail(bookmarkItem.getUrl());
                } else {
                    if (!bookmarkItem.isManaged()) {
                        mThumbnail.setImageDrawable(getResources()
                                .getDrawable(R.drawable.eb_folder));
                    } else {
                        mThumbnail.setImageDrawable(getResources()
                                .getDrawable(R.drawable.eb_managed));
                    }
                }
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.icon_most_visited_item, parent, false);
            return new BookmarkItemHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            BookmarksBridge.BookmarkItem bookmarkItem =
                    (BookmarksBridge.BookmarkItem) mAdapter.getItem(position);
            ((BookmarkItemHolder)holder).refreshInformation(bookmarkItem);
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
            return mAdapter.getCount();
        }
    }

    private class BookmarkLargeIconCallback implements LargeIconBridge.LargeIconCallback {
        String mUrl;
        ImageView mThumbnail;

        public BookmarkLargeIconCallback(String url, ImageView thumbnail) {
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

    @VisibleForTesting
    public int getCount() {
        return mAdapter.getCount();
    }

    @VisibleForTesting
    public Object getItemAtPosition(int position) {
        return mAdapter.getItem(position);
    }
}
