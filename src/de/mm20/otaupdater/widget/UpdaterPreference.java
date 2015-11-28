/*
 * Copyright 2015 MaxMustermann2.0
 *
 * Licensed under the Apache License,Version 2.0 (the "License");
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
package de.mm20.otaupdater.widget;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;

import de.mm20.otaupdater.R;
import de.mm20.otaupdater.activities.InstallUpdateActivity;

public class UpdaterPreference extends Preference implements View.OnClickListener {

    private static final String TAG = "OTAUpdater";
    private static final int STATE_DOWNLOAD = 0;
    private static final int STATE_ABORT_DOWNLOAD = 1;
    private static final int STATE_INSTALL = 2;

    private String mBuildName;
    private String mFileName;
    private String mFileUri;
    private String mMd5;
    private Context mContext;

    private TextView mTitle;
    private TextView mSummary;
    private ProgressBar mProgress;
    private ImageButton mIcon;

    private Notification.Builder mNotificationBuilder;
    private NotificationManager mNotificationManager;

    private int mState;
    private boolean mAbortDownload;
    private int mInstalledDeprecated;

    public UpdaterPreference(Context context, String buildName, String fileName, String fileUri,
                             String md5, int installedDeprecated) {
        super(context);
        mContext = context;
        mFileUri = fileUri;
        mMd5 = md5;
        mBuildName = buildName;
        mFileName = fileName;
        mAbortDownload = false;
        mInstalledDeprecated = installedDeprecated;
        setLayoutResource(R.layout.updater_preference);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mTitle = (TextView) view.findViewById(R.id.title);
        mSummary = (TextView) view.findViewById(R.id.summary);
        mProgress = (ProgressBar) view.findViewById(R.id.progress);
        mIcon = (ImageButton) view.findViewById(R.id.icon);
        if (mInstalledDeprecated == 0)
            mTitle.setText(mBuildName + " " + mContext.getString(R.string.installed));
        else if (mInstalledDeprecated == -1)
            mTitle.setText(mBuildName + " " + mContext.getString(R.string.deprecated));
        else mTitle.setText(mBuildName);
        mSummary.setText(mFileName);
        updateIconAndState();
        mIcon.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (mState) {
            case STATE_DOWNLOAD:
                mState = STATE_ABORT_DOWNLOAD;
                mProgress.setVisibility(View.VISIBLE);
                mProgress.setIndeterminate(true);
                mIcon.setImageDrawable(mContext.getDrawable(R.drawable.ic_abort));
                Intent download = new Intent("de.mm20.otaupdater.START_DOWNLOAD");
                download.putExtra("file_name", mFileName);
                download.putExtra("md5", mMd5);
                download.putExtra("uri", mFileUri);
                mContext.sendBroadcast(download);
                break;
            case STATE_ABORT_DOWNLOAD:
                mState = STATE_DOWNLOAD;
                mAbortDownload = true;
                mIcon.setImageDrawable(mContext.getDrawable(R.drawable.ic_download));
                PreferenceManager.getDefaultSharedPreferences(mContext).edit()
                        .putBoolean("abort_download", true).apply();
                break;
            case STATE_INSTALL:
                Intent installIntent = new Intent(mContext, InstallUpdateActivity.class);
                installIntent.putExtra("file_name", mFileName);
                installIntent.putExtra("installed_deprecated", mInstalledDeprecated);
                mContext.startActivity(installIntent);
                break;
        }
    }


    public void setProgress(int progress) {
        if (progress == -1) {
            mProgress.setVisibility(View.INVISIBLE);
            mProgress.setProgress(0);
            updateIconAndState();
            return;
        }
        mProgress.setIndeterminate(false);
        mProgress.setProgress(progress);
        mProgress.setVisibility(View.VISIBLE);
        mState = STATE_ABORT_DOWNLOAD;
        mIcon.setImageDrawable(mContext.getDrawable(R.drawable.ic_abort));
    }

    private void updateIconAndState() {
        File file = new File(Environment.getExternalStorageDirectory() + "/cmupdater/" + mFileName);
        if (file.exists()) {
            mIcon.setImageDrawable(mContext.getDrawable(R.drawable.ic_system_update));
            mState = STATE_INSTALL;
        } else {
            mIcon.setImageDrawable(mContext.getDrawable(R.drawable.ic_download));
            mState = STATE_DOWNLOAD;
        }
    }
}
