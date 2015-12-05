package de.mm20.otaupdater.receiver;/*
 *Copyright 2015 MaxMustermann2.0
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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.preference.PreferenceManager;

import java.io.File;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        File file = new File(Environment.getExternalStorageDirectory() +
                "/cmupdater/cm-download.part");
        //Assuming there is no download currently running when this is called, delete download file,
        //if the previous download failed.
        if (file.exists()) file.delete();
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString("currently_downloading", "")
                .putBoolean("abort_download", false).apply();
        String updateInterval = PreferenceManager.getDefaultSharedPreferences(context)
                .getString("interval", "60");
        if (updateInterval.equals("-1")) return;
        Intent i = new Intent("de.mm20.otaupdater.CHECK_UPDATES");
        context.sendBroadcast(i);
        if (updateInterval.equals("0")) return;
            int interval = Integer.parseInt(updateInterval) * 60000;
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, i, 0);
        long now = System.currentTimeMillis();
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        manager.setRepeating(AlarmManager.RTC_WAKEUP, now + interval, interval, pendingIntent);
    }
}
