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

package org.codeaurora.swe.partnerbrowsercustomizations;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import org.chromium.chrome.R;


public class BrowserCustomizationsProvider extends ContentProvider {

    private static final String LOGTAG = "BrowserCustomizationsProvider";
    private static final int URI_MATCH_HOMEPAGE = 0;
    private static final int URI_MATCH_DISABLE_INCOGNITO_MODE = 1;
    private static final int URI_MATCH_DISABLE_BOOKMARKS_EDITING = 2;
    private static final int URI_MATCH_BOOKMARKS = 3;
    private static final int URI_MATCH_LAST_TAB_EXIT = 4;

    private static final String BOOKMARKS_COLUMN_ID = "_id";
    private static final String BOOKMARKS_COLUMN_URL = "url";
    private static final String BOOKMARKS_COLUMN_TITLE = "title";
    private static final String BOOKMARKS_COLUMN_TYPE = "type";
    private static final String BOOKMARKS_COLUMN_PARENT = "parent";
    private static final String BOOKMARKS_COLUMN_FAVICON = "favicon";
    private static final String BOOKMARKS_COLUMN_TOUCHICON = "touchicon";

    private UriMatcher URI_MATCHER;
    /*
            {
                "version": "1.0",
                "title" : "Carrier-Bookmarks", // toplevel folder
                "bookmarks": [{ // toplevel bookmark
                    "title": "bookmark-top",
                    "url": "www.foo.com",
                    "favicon": "data:image\/png;base64,.....",
                    "touchicon": "data:image\/png;base64,....."
                 }, {
                    "title": "nested-folder", // top level folder
                    "bookmarks": [{ // bookmark in folder
                        "title": "bar",
                        "url": "www.bar.com",
                        "favicon": "data:image\/png;base64,.....",
                        "touchicon": "data:image\/png;base64,....."
                        }]
                }]
            }
    */

    private static String API_AUTHORITY_SUFFIX = ".partnerbrowsercustomizations";
    private final Object mInitializeUriMatcherLock = new Object();

    private void ensureUriMatcherInitialized() {
        synchronized (mInitializeUriMatcherLock) {
            if (URI_MATCHER != null) return;

            URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

            String apiAuthority = getContext().getPackageName() + API_AUTHORITY_SUFFIX;
            URI_MATCHER.addURI(apiAuthority, "homepage",
                               URI_MATCH_HOMEPAGE);
            URI_MATCHER.addURI(apiAuthority, "enableexitonlasttab",
                               URI_MATCH_LAST_TAB_EXIT);
            URI_MATCHER.addURI(apiAuthority, "disableincognitomode",
                               URI_MATCH_DISABLE_INCOGNITO_MODE);
            URI_MATCHER.addURI(apiAuthority, "disablebookmarksediting",
                               URI_MATCH_DISABLE_BOOKMARKS_EDITING);
            URI_MATCHER.addURI(apiAuthority, "bookmarks",
                               URI_MATCH_BOOKMARKS);
        }
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public String getType(Uri uri) {
        switch (URI_MATCHER.match(uri)) {
            case URI_MATCH_HOMEPAGE:
                return "vnd.android.cursor.item/partnerhomepage";
            case URI_MATCH_DISABLE_INCOGNITO_MODE:
                return "vnd.android.cursor.item/partnerdisableincognitomode";
            case URI_MATCH_DISABLE_BOOKMARKS_EDITING:
                return "vnd.android.cursor.item/partnerdisablebookmarksediting";
            case URI_MATCH_LAST_TAB_EXIT:
                return "vnd.android.cursor.item/partnerenableexitonlasttab";
            case URI_MATCH_BOOKMARKS:
                return "vnd.android.cursor.item/bookmarks";
            default:
                return null;
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        ensureUriMatcherInitialized();
        switch (URI_MATCHER.match(uri)) {
            case URI_MATCH_HOMEPAGE:
            {
                MatrixCursor cursor = new MatrixCursor(new String[]{"homepage"}, 1);
                cursor.addRow(new Object[]{
                        getContext().getResources().getString(R.string.default_homepage_url)});
                return cursor;
            }
            case URI_MATCH_DISABLE_INCOGNITO_MODE:
            {
                MatrixCursor cursor = new MatrixCursor(new String[] { "disableincognitomode" }, 1);
                cursor.addRow(new Object[] {
                    getContext().getResources().getInteger(R.integer.disableincognitomode) });
                return cursor;
            }
            case URI_MATCH_LAST_TAB_EXIT:
            {
                MatrixCursor cursor = new MatrixCursor(new String[] { "enableexitonlasttab" },
                        1);
                cursor.addRow(new Object[] {
                        getContext().getResources().getInteger(R.integer.enableexitonlasttab) });
                return cursor;
            }
            case URI_MATCH_DISABLE_BOOKMARKS_EDITING:
            {
                MatrixCursor cursor = new MatrixCursor(
                        new String[] { "disablebookmarksediting" }, 1);
                cursor.addRow(new Object[] {
                    getContext().getResources().getInteger(R.integer.disablebookmarksediting) });
                return cursor;
            }
            case URI_MATCH_BOOKMARKS:
            {
                return getBookmarks(new DataUtil().getBookmarksJsonFromRaw(getContext(),
                        (int)R.raw.bookmarks_preload), projection);
            }
            default:
                return null;
        }
    }

    private MatrixCursor getBookmarks(String json, String[] projection) {
        return (new BookmarksParser(getContext(), json, projection)).parse();
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

}
