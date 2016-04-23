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

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

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

import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

import de.mm20.otaupdater.R;

public class CheckForUpdateReceiver extends BroadcastReceiver {

    private static final String TAG = "CheckForUpdateReceiver";
    private ArrayList<String> mNames;
    private ArrayList<String> mFileNames;
    private ArrayList<String> mUris;
    private ArrayList<String> mMD5Sums;
    private ArrayList<String> mTypes;
    private ArrayList<Integer> mPatchLevel;
    private ArrayList<String> mFileSizes;
    private String mBuildsListUri;
    private String mDevice;
    private Context mContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        mBuildsListUri = mContext.getString(R.string.builds_list_uri);
        mFileNames = new ArrayList<>();
        mMD5Sums = new ArrayList<>();
        mNames = new ArrayList<>();
        mUris = new ArrayList<>();
        mFileSizes = new ArrayList<>();
        mTypes = new ArrayList<>();
        mPatchLevel = new ArrayList<>();
        mDevice = SystemProperties.get("ro.cm.device");
        new FetchBuildsAsyncTask().execute("");
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

    private int getSystemPatchLevel() {
        String patchLevel = SystemProperties.get("ro.cm.patchlevel");
        if (patchLevel.isEmpty()) return 0;
        return Integer.parseInt(patchLevel);
    }

    class FetchBuildsAsyncTask extends AsyncTask<String, Integer, Integer> {

        @Override
        protected Integer doInBackground(String... params) {
            try {
                Log.d(TAG, "Checking for updates...");
                URL url = new URL(mBuildsListUri);
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                XmlPullParser parser = factory.newPullParser();
                parser.setInput(new InputStreamReader(url.openStream()));
                while (parser.nextToken() != XmlPullParser.END_DOCUMENT) {
                    if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName()
                            .equals("build") && parser.getAttributeValue(null, "device")
                            .equals(mDevice)) {
                        mNames.add(parser.getAttributeValue(null, "name"));
                        mFileNames.add(parser.getAttributeValue(null, "filename"));
                        mUris.add(parser.getAttributeValue(null, "uri"));
                        mMD5Sums.add(parser.getAttributeValue(null, "md5"));
                        mTypes.add(parser.getAttributeValue(null, "type"));
                        mFileSizes.add(parser.getAttributeValue(null, "size"));
                        mPatchLevel.add(Integer.parseInt(parser
                                .getAttributeValue(null, "patchlevel")));
                    }
                }

                for (int i = mFileNames.size() - 1; i >= 0; i--) {
                    if (compareBuildDates(mFileNames.get(i)) == -1 || (mTypes.get(i).equals("patch")
                            && (mPatchLevel.get(i) <= getSystemPatchLevel() ||
                            compareBuildDates(mFileNames.get(i)) != 0))) {
                        //Delete builds which are older than the installed one
                        File file = new File(Environment.getExternalStorageDirectory() +
                                "/cmupdater/" + mFileNames.get(i));
                        if (file.exists()) file.delete();
                        file = new File(Environment.getExternalStorageDirectory() +
                                "/cmupdater/" + mFileNames.get(i) + ".md5sum");
                        if (file.exists()) file.delete();
                        mFileNames.remove(i);
                        mMD5Sums.remove(i);
                        mUris.remove(i);
                        mNames.remove(i);
                        mFileSizes.remove(i);
                        mTypes.remove(i);
                        mPatchLevel.remove(i);
                    }
                }
                PreferenceManager.getDefaultSharedPreferences(mContext)
                        .edit()
                        .putString("build_name_list", asString(mNames))
                        .putString("file_name_list", asString(mFileNames))
                        .putString("build_uri_list", asString(mUris))
                        .putString("md5_sum_list", asString(mMD5Sums))
                        .putString("file_size_list", asString(mFileSizes))
                        .putString("type_list", asString(mTypes))
                        .putLong("updates_last_checked", System.currentTimeMillis())
                        .apply();
            } catch (java.io.IOException | XmlPullParserException e) {
                Log.e(TAG, "Failed to fetch builds: " + e.getClass().getName());
            }
            return 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
            boolean notifyUpdate = PreferenceManager.getDefaultSharedPreferences(mContext)
                    .getBoolean("notify_update", true);
            if (notifyUpdate && mFileNames.size() > 1) {
                Notification.Builder builder = new Notification.Builder(mContext);
                PendingIntent intent = PendingIntent
                        .getActivity(mContext, 0,
                                new Intent("android.settings.SYSTEM_UPDATE_SETTINGS"), 0);
                builder.setSmallIcon(R.drawable.ic_system_update)
                        .setContentTitle(mContext.getString(R.string.new_updates))
                        .setContentText(mContext.getString(R.string.new_update_info))
                        .setOngoing(false)
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setContentIntent(intent);
                NotificationManager manager = (NotificationManager)
                        mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                manager.notify(0, builder.build());
            }
        }

        private String asString(ArrayList<String> list) {
            String returnVal = "";
            for (String s : list) {
                if (returnVal.length() > 0) returnVal += ",";
                returnVal = returnVal + s;
            }
            return returnVal;
        }
    }
}
