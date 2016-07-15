/*
 * Copyright 2015 MaxMustermann2.0
 *
 * Licensed under the Apache License,Version2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,software
 * distributed under the License is distributed on an"AS IS"BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.mm20.otaupdater.fragments;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Date;

import de.mm20.otaupdater.R;
import de.mm20.otaupdater.util.UpdaterUtils;
import de.mm20.otaupdater.widget.UpdaterPreference;

public class UpdaterFragment extends PreferenceFragment
        implements Preference.OnPreferenceClickListener,
        SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceChangeListener {
    private static final String TAG = "OTAUpdater";
    PreferenceCategory mUpdatesCategory;
    private String[] mFileNames;
    private String[] mMD5Sums;
    private String[] mNames;
    private String[] mUris;
    private String[] mTypes;
    private String[] mFileSizes;
    private String mUpdatesJson;
    private Preference mCheckUpdates;
    private ListPreference mInterval;
    private ArrayList<UpdaterPreference> mUpdaterPreferences;

    private Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.updater);
        mUpdaterPreferences = new ArrayList<>();
        mUpdatesCategory = (PreferenceCategory) findPreference("updater_category");
        mInterval = (ListPreference) findPreference("interval");
        mInterval.setOnPreferenceChangeListener(this);
        mCheckUpdates = findPreference("search_updates");
        mCheckUpdates.setOnPreferenceClickListener(this);
        PreferenceManager.getDefaultSharedPreferences(getContext())
                .registerOnSharedPreferenceChangeListener(this);
        setupPreferenceViews();
    }

    private void setupPreferenceViews(){
        long lastCheckedTime = PreferenceManager.getDefaultSharedPreferences(getActivity())
                .getLong("updates_last_checked", -1);
        String lastChecked = getString(R.string.last_checked) + " ";
        if (lastCheckedTime == -1) {
            lastChecked += getString(R.string.never);
        } else {
            Date date = new Date(lastCheckedTime);
            lastChecked += DateFormat.getDateFormat(getActivity()).format(date) + ", " +
                    DateFormat.getTimeFormat(getActivity()).format(date);
        }
        mCheckUpdates.setSummary(lastChecked);
        String json = PreferenceManager.getDefaultSharedPreferences(getContext())
                .getString("updates_json", "[]");
        mUpdatesCategory.removeAll();
        try {
            JSONArray jsonArray = new JSONArray(json);
            mFileNames = new String[jsonArray.length()];
            mNames = new String[jsonArray.length()];
            mUris = new String[jsonArray.length()];
            mMD5Sums = new String[jsonArray.length()];
            for (int i = 0; i < jsonArray.length(); i++) {
                UpdaterPreference preference = new UpdaterPreference(getContext(),
                        jsonArray.optJSONObject(i));
                mUpdatesCategory.addPreference(preference);
                mUpdaterPreferences.add(preference);
            }
        } catch (JSONException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }
    


    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mCheckUpdates) {
            Intent checkUpdates = new Intent("de.mm20.otaupdater.CHECK_UPDATES");
            getContext().sendBroadcast(checkUpdates);
        }
        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (!isAdded()) return;
        if (s.equals("updates_last_checked")) {
            setupPreferenceViews();
        } else if (s.equals("dl_progress_current") || s.equals("currently_downloading")) {
            String dlFile = PreferenceManager.getDefaultSharedPreferences(getContext())
                    .getString("currently_downloading", "");
            int progress = PreferenceManager.getDefaultSharedPreferences(getContext())
                    .getInt("dl_progress_current", 0);
            for (int i = 0; i < mUpdaterPreferences.size(); i++) {
                if (mUpdaterPreferences.get(i).getFileName().equals(dlFile)) {
                    mUpdaterPreferences.get(i).setProgress(progress);
                } else {
                    mUpdaterPreferences.get(i).setProgress(-1);
                }
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        Intent i = new Intent("de.mm20.otaupdater.CHECK_UPDATES");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(), 0, i, 0);
        AlarmManager manager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        if (o.equals("-1") || o.equals("0")) {
            manager.cancel(pendingIntent);
            return true;
        }
        int interval = Integer.parseInt((String) o) * 60000;
        long now = System.currentTimeMillis();
        manager.setRepeating(AlarmManager.RTC_WAKEUP, now + interval, interval, pendingIntent);
        return true;
    }
}
