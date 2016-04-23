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

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.BulletSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import de.mm20.otaupdater.R;
import de.mm20.otaupdater.activities.InstallUpdateActivity;
import de.mm20.otaupdater.util.UpdaterUtils;

public class UpdaterPreference extends Preference implements View.OnClickListener {

    private static final String TAG = "OTAUpdater";
    private static final int STATE_DOWNLOAD = 0;
    private static final int STATE_ABORT_DOWNLOAD = 1;
    private static final int STATE_INSTALL = 2;

    private JSONObject mJsonObject;

    private Context mContext;

    private TextView mTitle;
    private TextView mSummary;
    private ProgressBar mProgress;
    private ImageButton mIcon;

    private Notification.Builder mNotificationBuilder;
    private NotificationManager mNotificationManager;

    private int mState;

    public UpdaterPreference(Context context, JSONObject jsonObject) {
        super(context);
        mContext = context;
        mJsonObject = jsonObject;
        setLayoutResource(R.layout.updater_preference);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mTitle = (TextView) view.findViewById(R.id.title);
        mSummary = (TextView) view.findViewById(R.id.summary);
        mProgress = (ProgressBar) view.findViewById(R.id.progress);
        mIcon = (ImageButton) view.findViewById(R.id.icon);
        int date = mJsonObject.optInt("date", 0);
        int patchLevel = mJsonObject.optInt("patchlevel", 0);
        String buildName = mJsonObject.optString("name", "Build");
        String device = mJsonObject.optString("device", "generic");
        if (UpdaterUtils.isBuildInstalled(date, patchLevel, device))
            mTitle.setText(buildName + " " + mContext.getString(R.string.installed));
        else mTitle.setText(buildName);
        if (isUpdateDownloaded()) {
            String summary = UpdaterUtils.fileSizeAsString(mJsonObject.optLong("size")) + " - " +
                    mContext.getString(R.string.downloaded);
            mSummary.setText(summary);
        } else {
            String summary = UpdaterUtils.fileSizeAsString(mJsonObject.optLong("size"));
            mSummary.setText(summary);
        }
        updateIconAndState();
        view.setOnClickListener(this);
        mIcon.setOnClickListener(this);
    }

    private void showUpdateInfo() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.update_detail_dialog,
                null);
        long releaseDate = mJsonObject.optLong("releasedate", 0);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(releaseDate);
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
        TextView releaseDateTv = (TextView) dialogView
                .findViewById(R.id.update_details_release_date);
        String releaseDateString = "<b>" + getContext().getString(R.string.released) + ":</b> "
                + df.format(calendar.getTime());
        releaseDateTv.setText(Html.fromHtml(releaseDateString));
        JSONArray changelog = mJsonObject.optJSONArray("changelog");
        String changelogString = "<b>" + getContext().getString(R.string.changelog) +
                ":</b><br>";
        for (int i = 0; i < changelog.length(); i++) {
            String entry = changelog.optString(i);
            changelogString += "&bull; " + entry;
            if (i < changelog.length() - 1) changelogString += "<br>";
        }
        TextView changelogTv = (TextView) dialogView
                .findViewById(R.id.update_details_changelog);
        changelogTv.setText(Html.fromHtml(changelogString));
        TextView typeTv = (TextView) dialogView
                .findViewById(R.id.update_details_type);
        String type = "<b>" + getContext().getString(R.string.type) + ":</b> ";
        type += getContext().getString(mJsonObject.optInt("patchlevel", 0) == 0 ?
                R.string.full_update : R.string.incremental_update);
        typeTv.setText(Html.fromHtml(type));
        TextView sizeTv = (TextView) dialogView
                .findViewById(R.id.update_details_filesize);
        String size = "<b>" + getContext().getString(R.string.size) + ":</b> ";
        size += UpdaterUtils.fileSizeAsString(mJsonObject.optLong("size", 0));
        sizeTv.setText(Html.fromHtml(size));
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(mJsonObject.optString("name", "Build"));
        builder.setView(dialogView);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.show();
    }

    @Override
    public void onClick(View v) {
        if (v == mIcon) {
            int date = mJsonObject.optInt("date", 0);
            int patchLevel = mJsonObject.optInt("patchlevel", 0);
            String device = mJsonObject.optString("device", "generic");
            String md5 = mJsonObject.optString("md5", "00000000000000000000000000000000");
            String uri = mJsonObject.optString("url", "");
            String fileName = mJsonObject.optString("filename", "");
            switch (mState) {
                case STATE_DOWNLOAD:
                    mState = STATE_ABORT_DOWNLOAD;
                    mProgress.setVisibility(View.VISIBLE);
                    mProgress.setIndeterminate(true);
                    mIcon.setImageDrawable(mContext.getDrawable(R.drawable.ic_abort));
                    Intent download = new Intent("de.mm20.otaupdater.START_DOWNLOAD");
                    download.putExtra("file_name", fileName);
                    download.putExtra("md5", md5);
                    download.putExtra("uri", uri);
                    download.putExtra("install_deprecated",
                            UpdaterUtils.isBuildInstalled(date, patchLevel, device) ? 0 : 1);
                    mContext.sendBroadcast(download);
                    break;
                case STATE_ABORT_DOWNLOAD:
                    mState = STATE_DOWNLOAD;
                    mIcon.setImageDrawable(mContext.getDrawable(R.drawable.ic_download));
                    PreferenceManager.getDefaultSharedPreferences(mContext).edit()
                            .putBoolean("abort_download", true).apply();
                    break;
                case STATE_INSTALL:
                    Intent installIntent = new Intent(mContext, InstallUpdateActivity.class);
                    installIntent.putExtra("file_name", fileName);
                    installIntent.putExtra("installed_deprecated",
                            UpdaterUtils.isBuildInstalled(date, patchLevel, device) ? 0 : 1);
                    mContext.startActivity(installIntent);
                    break;
            }
        } else {
            showUpdateInfo();
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
        if (isUpdateDownloaded()) {
            mIcon.setImageDrawable(mContext.getDrawable(R.drawable.ic_system_update));
            mState = STATE_INSTALL;
        } else {
            mIcon.setImageDrawable(mContext.getDrawable(R.drawable.ic_download));
            mState = STATE_DOWNLOAD;
        }
    }

    private boolean isUpdateDownloaded() {
        String fileName = mJsonObject.optString("filename");
        File file = new File(Environment.getExternalStorageDirectory() + "/cmupdater/" + fileName);
        return file.exists();
    }

    public String getFileName() {
        return mJsonObject.optString("filename");
    }
}
