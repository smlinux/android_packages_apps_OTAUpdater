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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;

import de.mm20.otaupdater.R;
import de.mm20.otaupdater.widget.UpdaterPreference;

public class UpdaterFragment extends PreferenceFragment
        implements Preference.OnPreferenceClickListener,
        SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "OTAUpdater";
    PreferenceCategory mUpdatesCategory;
    private String[] mFileNames;
    private String[] mMD5Sums;
    private String[] mNames;
    private String[] mUris;
    private String[] mTypes;
    private Preference mCheckUpdates;
    private ArrayList<UpdaterPreference> mUpdaterPreferences;

    private Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.updater);
        mUpdaterPreferences = new ArrayList<>();
        mUpdatesCategory = (PreferenceCategory) findPreference("updater_category");
        mCheckUpdates = findPreference("search_updates");
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
        mCheckUpdates.setOnPreferenceClickListener(this);
        if(mFileNames.length < 1) return;
        for (int i = 0; i < mFileNames.length; i++) {
            UpdaterPreference pref = new UpdaterPreference(mContext, mNames[i], mFileNames[i],
                    mUris[i], mMD5Sums[i], mTypes[i], compareBuildDates(mFileNames[i]));
            mUpdatesCategory.addPreference(pref);
            mUpdaterPreferences.add(pref);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
        PreferenceManager.getDefaultSharedPreferences(activity)
                .registerOnSharedPreferenceChangeListener(this);
        mFileNames = PreferenceManager.getDefaultSharedPreferences(activity)
                .getString("file_name_list", "").split(",");
        mNames = PreferenceManager.getDefaultSharedPreferences(activity)
                .getString("build_name_list", "").split(",");
        mUris = PreferenceManager.getDefaultSharedPreferences(activity)
                .getString("build_uri_list", "").split(",");
        mMD5Sums = PreferenceManager.getDefaultSharedPreferences(activity)
                .getString("md5_sum_list", "").split(",");
        mTypes = PreferenceManager.getDefaultSharedPreferences(activity)
                .getString("type_list", "").split(",");

    }

    /**
     * Compares the build date of a new build with the build date of the installed one.
     *
     * @param fileName the file name of the new build
     * @return 1 if the new build is newer than the installed one,
     * 0 if the new build is already installed,
     * -1 if the new build is older than the installed one
     */
    private int compareBuildDates(String fileName) {
        String currentVersion = SystemProperties.get("ro.cm.version");
        int newBuild = Integer.parseInt(fileName.substring(8, 16));
        int currentBuild = Integer.parseInt(currentVersion.substring(5, 13));
        Log.d(TAG, "New build: " + newBuild + "; Installed build: " + currentBuild);
        if (newBuild > currentBuild) return 1;
        else if (newBuild == currentBuild) return 0;
        return -1;
    }


    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mCheckUpdates) {
            Intent checkUpdates = new Intent("de.mm20.otaupdater.CHECK_UPDATES");
            getActivity().sendBroadcast(checkUpdates);
        }
        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (!isAdded()) return;
        if (s.equals("updates_last_checked")) {
            long lastCheckedTime = PreferenceManager.getDefaultSharedPreferences(mContext)
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
            mCheckUpdates.setOnPreferenceClickListener(this);
            mUpdaterPreferences.clear();
            mUpdatesCategory.removeAll();
            mFileNames = PreferenceManager.getDefaultSharedPreferences(mContext)
                    .getString("file_name_list", "").split(",");
            mNames = PreferenceManager.getDefaultSharedPreferences(mContext)
                    .getString("build_name_list", "").split(",");
            mUris = PreferenceManager.getDefaultSharedPreferences(mContext)
                    .getString("build_uri_list", "").split(",");
            mMD5Sums = PreferenceManager.getDefaultSharedPreferences(mContext)
                    .getString("md5_sum_list", "").split(",");
            mTypes = PreferenceManager.getDefaultSharedPreferences(mContext)
                    .getString("type_list", "").split(",");
            for (int i = 0; i < mFileNames.length; i++) {
                UpdaterPreference pref = new UpdaterPreference(mContext, mNames[i], mFileNames[i],
                        mUris[i], mMD5Sums[i], mTypes[i], compareBuildDates(mFileNames[i]));
                mUpdatesCategory.addPreference(pref);
                mUpdaterPreferences.add(pref);
            }
        } else if (s.equals("dl_progress_current") || s.equals("currently_downloading")) {
            String dlFile = PreferenceManager.getDefaultSharedPreferences(mContext)
                    .getString("currently_downloading", "");
            int progress = PreferenceManager.getDefaultSharedPreferences(mContext)
                    .getInt("dl_progress_current", 0);
            for (int i = 0; i < mFileNames.length; i++) {
                if (mFileNames[i].equals(dlFile)) {
                    mUpdaterPreferences.get(i).setProgress(progress);
                } else {
                    mUpdaterPreferences.get(i).setProgress(-1);
                }
            }
        }
    }
}
