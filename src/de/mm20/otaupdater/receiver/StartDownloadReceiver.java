/*
 * Copyright 2015 MaxMustermann2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.mm20.otaupdater.receiver;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import de.mm20.otaupdater.R;
import de.mm20.otaupdater.activities.InstallUpdateActivity;
import de.mm20.otaupdater.util.MD5;

public class StartDownloadReceiver extends BroadcastReceiver {
    private static final String TAG = "StartDownloadReceiver";
    NotificationManager mManager;
    private String mFileName;
    private String mUri;
    private String mMd5;
    private String mInstallDeprecated;
    private Context mContext;
    private Notification.Builder mBuilder;

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        mFileName = intent.getStringExtra("file_name");
        mUri = intent.getStringExtra("uri");
        mMd5 = intent.getStringExtra("md5");
        mInstallDeprecated = intent.getStringExtra("install_deprecated");
        new DownloadFileAsyncTask().execute(mUri);
    }

    private int compareBuildDates(String fileName) {
        String currentVersion = SystemProperties.get("ro.cm.version");
        int newBuild = Integer.parseInt(fileName.substring(8, 16));
        int currentBuild = Integer.parseInt(currentVersion.substring(5, 13));
        Log.d(TAG, "New build: " + newBuild + "; Installed build: " + currentBuild);
        if (newBuild > currentBuild) return 1;
        else if (newBuild == currentBuild) return 0;
        return -1;
    }

    class DownloadFileAsyncTask extends AsyncTask<String, Integer, Boolean> {

        @Override
        protected void onPreExecute() {
            PendingIntent intent = PendingIntent
                    .getActivity(mContext, 0,
                            new Intent("android.settings.SYSTEM_UPDATE_SETTINGS"), 0);
            mBuilder = new Notification.Builder(mContext);
            mBuilder.setSmallIcon(android.R.drawable.stat_sys_download)
                    .setContentTitle(mContext.getString(R.string.downloading_update))
                    .setOngoing(true)
                    .setContentIntent(intent)
                    .setProgress(0, 0, true);
            mManager = (NotificationManager)
                    mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            mManager.notify(1, mBuilder.build());
            PreferenceManager.getDefaultSharedPreferences(mContext).edit()
                    .putString("currently_downloading", mFileName).apply();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            int count;
            try {
                new File(Environment
                        .getExternalStorageDirectory() + "/cmupdater").mkdirs();
                URL url = new URL(params[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                while (connection.getResponseCode() == 301) {
                    url = new URL(connection.getHeaderField("Location"));
                    connection = (HttpURLConnection) url.openConnection();
                }
                connection.connect();
                int fileLenght = connection.getContentLength();
                InputStream input = new BufferedInputStream(url.openStream(), 8192);
                OutputStream output = new FileOutputStream(Environment
                        .getExternalStorageDirectory() + "/cmupdater/cm-download.part");
                byte data[] = new byte[1 << 20];
                long total = 0;
                int percentProgress = 0;
                int shownPercentProgress = 0;
                boolean abortDownload = false;
                while ((count = input.read(data)) != -1 && !abortDownload) {
                    total += count;
                    percentProgress = (int) ((total * 100) / fileLenght);
                    if (percentProgress > shownPercentProgress) {
                        shownPercentProgress = percentProgress;
                        publishProgress(percentProgress);
                    }

                    output.write(data, 0, count);
                    abortDownload = PreferenceManager.getDefaultSharedPreferences(mContext)
                            .getBoolean("abort_download", false);
                }
                output.flush();
                output.close();
                input.close();
                publishProgress(100);

                if (!mMd5.equals(MD5.getMd5(Environment.getExternalStorageDirectory() +
                        "/cmupdater/cm-download.part"))) {
                    File file = new File(Environment.getExternalStorageDirectory() + "/cmupdater/" +
                            "cm-download.part");
                    file.delete();
                    return false;
                } else {
                    File file = new File(Environment.getExternalStorageDirectory() + "/cmupdater/" +
                            "cm-download.part");
                    file.renameTo(new File(Environment.getExternalStorageDirectory() + "/cmupdater/"
                            + mFileName));
                    File md5File = new File(Environment.getExternalStorageDirectory() +
                            "/cmupdater/" + mFileName + ".md5sum");
                    try {
                        md5File.createNewFile();
                        FileWriter writer = new FileWriter(md5File);
                        writer.write(mMd5 + " " + mFileName);
                        writer.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Error writing md5 file");
                    }
                    return true;
                }
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
                return false;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            mBuilder.setProgress(100, values[0], false);
            mManager.notify(1, mBuilder.build());
            PreferenceManager.getDefaultSharedPreferences(mContext).edit()
                    .putInt("dl_progress_current", values[0]).apply();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            mManager.cancel(1);
            if (result) {
                Toast.makeText(mContext, R.string.build_ready, Toast.LENGTH_LONG).show();
                Intent i = new Intent(mContext, InstallUpdateActivity.class);
                i.putExtra("installed_deprecated", mInstallDeprecated);
                i.putExtra("file_name", mFileName);
                mBuilder.setOngoing(false)
                        .setUsesChronometer(true)
                        .setContentTitle(mContext.getString(R.string.build_ready))
                        .setContentText(mContext.getString(R.string.build_ready_description))
                        .setSmallIcon(R.drawable.ic_system_update)
                        .setContentIntent(PendingIntent.getActivity(mContext, 0, i, 0))
                        .setProgress(0, 0, false);
                mManager.notify(2, mBuilder.build());
            } else {
                Toast.makeText(mContext, R.string.download_failed, Toast.LENGTH_LONG).show();
            }
            PreferenceManager.getDefaultSharedPreferences(mContext).edit()
                    .putString("currently_downloading", "")
                    .putBoolean("abort_download", false)
                    .putInt("dl_progress_current", -1).apply();
        }

    }

}
