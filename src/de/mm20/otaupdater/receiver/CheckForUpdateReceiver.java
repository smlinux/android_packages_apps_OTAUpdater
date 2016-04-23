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

import org.json.JSONArray;
import org.json.JSONException;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import de.mm20.otaupdater.R;
import de.mm20.otaupdater.util.UpdaterUtils;

public class CheckForUpdateReceiver extends BroadcastReceiver {

    private static final String TAG = "CheckForUpdateReceiver";
    private String mBuildsListUri;
    private String mDevice;
    private Context mContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        mBuildsListUri = mContext.getString(R.string.builds_list_uri);
        new FetchBuildsAsyncTask().execute("");
    }


    class FetchBuildsAsyncTask extends AsyncTask<String, Integer, Integer> {

        @Override
        protected Integer doInBackground(String... params) {
            try {
                Log.d(TAG, "Checking for updates...");
                URL url = new URL(mBuildsListUri);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.setDoOutput(true);
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(url.openStream()));
                String jsonString = "";
                String line = reader.readLine();
                while (line != null) {
                    jsonString += line;
                    line = reader.readLine();
                }
                Log.d(TAG, jsonString);
                reader.close();
                JSONArray jsonArray = new JSONArray(jsonString);
                JSONArray compatibleBuildsArray = new JSONArray();
                int numNewUpdates = 0;
                for (int i = 0; i < jsonArray.length(); i++) {
                    int buildDate = jsonArray.getJSONObject(i).getInt("date");
                    int patchLevel = jsonArray.getJSONObject(i).getInt("patchlevel");
                    String device = jsonArray.getJSONObject(i).getString("device");
                    String fileName = jsonArray.getJSONObject(i).getString("filename");
                    if (UpdaterUtils.isUpdateCompatible(buildDate, patchLevel, device)) {
                        compatibleBuildsArray.put(jsonArray.get(i));
                    } else {
                        File file = new File(Environment.getExternalStorageDirectory() +
                                "/cmupdater/" + fileName);
                        if (file.exists()) file.delete();
                    }
                    if (UpdaterUtils.isUpdateNew(buildDate, patchLevel, device)) numNewUpdates++;
                }
                PreferenceManager.getDefaultSharedPreferences(mContext).edit()
                        .putString("updates_json", compatibleBuildsArray.toString())
                        .putLong("updates_last_checked", System.currentTimeMillis())
                        .apply();
                return numNewUpdates;
            } catch (IOException | JSONException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
            return 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
            boolean notifyUpdate = PreferenceManager.getDefaultSharedPreferences(mContext)
                    .getBoolean("notify_update", true);
            if (notifyUpdate && result > 0) {
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
    }
}
