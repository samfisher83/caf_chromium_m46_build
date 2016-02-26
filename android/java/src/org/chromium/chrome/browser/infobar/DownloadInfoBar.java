// Copyright (c) 2016, The Linux Foundation. All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//     * Redistributions of source code must retain the above copyright
//       notice, this list of conditions and the following disclaimer.
//     * Redistributions in binary form must reproduce the above
//       copyright notice, this list of conditions and the following
//       disclaimer in the documentation and/or other materials provided
//       with the distribution.
//     * Neither the name of The Linux Foundation nor the names of its
//       contributors may be used to endorse or promote products derived
//       from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
// WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
// ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
// BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
// BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
// WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
// OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
// IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package org.chromium.chrome.browser.infobar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.view.View;

import org.chromium.base.annotations.CalledByNative;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.BrowserChromeActivity;
import org.chromium.chrome.browser.ChromeActivity;
import org.chromium.content_public.browser.WebContents;
import org.codeaurora.swe.SWECommandLine;

import java.util.List;
import android.util.Log;


/**
 * An infobar to ask download path
 */
public class DownloadInfoBar extends InfoBar {
    private static final String TAG = "DownloadInfoBar";

    private static String mFileName;
    private static String mDirName;
    private static String mDirFullPath;
    private static InfoBarLayout mInfoBarLayout;

    @CalledByNative
    private static InfoBar createInfoBar(String fileName, String dirName, String dirFullPath) {
        return new DownloadInfoBar(fileName, dirName, dirFullPath);
    }

    public static String getFilePath() {
        return mDirFullPath;
    }

    public static String getAllInfo() {
        return "" +  mFileName + " " + mDirFullPath + " " + mDirFullPath;
    }
    /**
     * Constructs DownloadInfoBar.
     * @param fileName The file name. ex) example.jpg
     * @param dirName The dir name. ex) Downloads
     * @param dirFullPath The full dir path. ex) sdcards/Downloads
     */
    protected DownloadInfoBar(String fileName, String dirName, String dirFullPath) {
        super(null, R.drawable.infobar_downloading, null, null);
        mFileName = fileName;
        mDirName = dirName;
        mDirFullPath = dirFullPath;
    }

    @Override
    public void onButtonClicked(boolean isPrimaryButton) {
        int action = isPrimaryButton ? InfoBar.ACTION_TYPE_CREATE_NEW_FILE
                                     : InfoBar.ACTION_TYPE_CANCEL;

        // Close the current download tab (empty) when CANCEL is pressed
        if (action == InfoBar.ACTION_TYPE_CANCEL) {
            WebContents contents = ((ChromeActivity) (mInfoBarLayout.getContext()))
                    .getActivityTab().getWebContents();
            boolean isInitialNavigation = contents == null
                    || contents.getNavigationController().isInitialNavigation();
            if (isInitialNavigation) {
                // Tab is created just for download, close it.
                ((ChromeActivity) (mInfoBarLayout.getContext()))
                        .getTabModelSelector()
                        .closeTab(((ChromeActivity) (mInfoBarLayout.getContext())).getActivityTab());
            }
        }

        onButtonClicked(action, mDirFullPath);
    }

    public void updateDownloadPath(String dirFullPath) {
        mDirFullPath = dirFullPath;
        mDirName = dirFullPath;
        Context context = mInfoBarLayout.getContext();
        mInfoBarLayout.setMessage(getMessageText(context));
    }

    private static CharSequence getMessageText(Context context) {
        String template = context.getString(R.string.swe_download_infobar_text);
        Intent intent = getIntentForDirectoryLaunch(mDirFullPath, context);
        return formatInfoBarMessage(context, template, mFileName, mDirName, intent);
    }

    /**
     * @param dirFullPath The full path of the directory to be launched.
     * @return An Android intent that can launch the directory.
     */
    private static Intent getIntentForDirectoryLaunch(String dirFullPath, Context context) {
        Intent intent = new Intent(SWECommandLine.getInstance(context).getDownloadFileMgrIntent());
        Uri uri = Uri.parse(dirFullPath);
        if (uri == null) {
            return null;
        }
        return intent;
    }

    @Override
    public void createContent(InfoBarLayout layout) {
        mInfoBarLayout = layout;
        Context context = layout.getContext();
        layout.setMessage(getMessageText(context));
        layout.setButtons("OK", "Cancel");
    }

    /**
     * Create infobar message in the form of CharSequence.
     *
     * @param context The context.
     * @param template The template CharSequence.
     * @param fileName The file name.
     * @param dirName The directory name.
     * @param dirNameIntent The intent to be launched when user touches the directory name link.
     * @return CharSequence formatted message for InfoBar.
     */
    private static CharSequence formatInfoBarMessage(final Context context, String template,
            String fileName, String dirName, final Intent dirNameIntent) {
        SpannableString formattedFileName = new SpannableString(fileName);
        formattedFileName.setSpan(new StyleSpan(Typeface.BOLD), 0, fileName.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        SpannableString formattedDirName = new SpannableString(dirName);
        if (canResolveIntent(context, dirNameIntent)) {
            formattedDirName.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View view) {
                    if (context instanceof Activity) {
                        ((Activity) context).startActivityForResult(
                                                  dirNameIntent,
                                                  BrowserChromeActivity.DOWNLOADPATH_SELECTION);
                    }
                }
            }, 0, dirName.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return TextUtils.expandTemplate(template, formattedFileName, formattedDirName);
    }

    private static boolean canResolveIntent(Context context, Intent intent) {
        if (context == null || intent == null) {
            return false;
        }
        final PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> resolveInfoList =
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfoList.size() > 0;
    }
}
