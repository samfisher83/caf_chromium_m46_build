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

package org.chromium.chrome.browser.download;

import org.chromium.content.browser.DownloadInfo;
import org.chromium.chrome.R;

import android.os.Build;
import android.os.StatFs;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import android.os.Environment;

public class DownloadManagerUtils {
   // Reserved disk space to avoid filling disk. (Same as android DownloadManager)
   private static final long RESERVED_BYTES = 32 * 1024 * 1024;


    public static boolean isExternalMemoryEnough(DownloadInfo downloadInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            HttpURLConnection conn = null;
            long contentLength;
            try {
                URL url = new URL(downloadInfo.getUrl());
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("HEAD");
                conn.getInputStream();
                contentLength = conn.getContentLength();
            } catch (IOException e) {
                contentLength = -1;
            } finally {
                if (conn != null) conn.disconnect();
            }

            StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
            long blockSize = statFs.getBlockSizeLong();
            long availableSize = (statFs.getAvailableBlocksLong() * blockSize) - RESERVED_BYTES;

            if (contentLength > 0 && contentLength > availableSize) {
                return false;
            }
        }
        return true;
    }
}
