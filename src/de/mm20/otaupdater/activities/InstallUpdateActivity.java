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
package de.mm20.otaupdater.activities;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

import de.mm20.otaupdater.R;

public class InstallUpdateActivity extends Activity {
    private static final String TAG = "InstallUpdateActivity";
    Runnable failedToast = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(InstallUpdateActivity.this, R.string.install_failed, Toast.LENGTH_LONG)
                    .show();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        int installedDeprecated = getIntent().getIntExtra("installed_deprecated", 1);
        int msgId = R.string.confirm_install;
        //if (!isSeLinuxEnforcing() || setSeLinuxPermissive()) {
        if (installedDeprecated == 0) msgId = R.string.confirm_install_installed;
        else if (installedDeprecated == -1) msgId = R.string.confirm_install_deprecated;
        builder.setMessage(msgId)
                .setTitle(R.string.system_update)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String fileName = getIntent().getStringExtra("file_name");
                        installUpdate(fileName);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                }).create().show();

    }

    private void installUpdate(final String fileName) {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage(getString(R.string.preparing_update));
        dialog.show();
        dialog.setCancelable(false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                String updateFile = Environment.getExternalStorageDirectory() + "/cmupdater/" +
                        fileName;
                try {
                    int attempts = 0;
                    File file = new File("/cache/recovery/update.zip");
                    File recoveryScript = new File("/cache/recovery/openrecoveryscript");
                    do {
                        attempts++;
                        Process process = Runtime.getRuntime().exec("sh");
                        DataOutputStream outputStream = new DataOutputStream(process.getOutputStream());
                        outputStream.writeBytes("cp -f " + updateFile +
                                " /cache/recovery/update.zip\n");
                        outputStream.writeBytes("cp -f " + updateFile +
                                ".md5sum /cache/recovery/update.zip.md5sum\n");
                        outputStream.writeBytes("printf \"install /cache/recovery/update.zip\nwipe " +
                                "cache\nreboot\" >/cache/recovery/openrecoveryscript");
                        outputStream.flush();
                        outputStream.close();
                        process.waitFor();
                    } while (!file.exists() && attempts <= 5);
                    if (attempts > 5) {
                        runOnUiThread(failedToast);
                        finish();
                        return;
                    }
                    PowerManager powerManager = (PowerManager)
                            getSystemService(Context.POWER_SERVICE);
                    powerManager.reboot("recovery");
                } catch (IOException | InterruptedException e) {
                    Log.e(TAG, e.getClass().getName() + " " + e.getMessage());
                    finish();
                }
            }
        }).start();
    }
}
