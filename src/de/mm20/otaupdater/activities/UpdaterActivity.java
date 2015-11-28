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
import android.os.Bundle;

import de.mm20.otaupdater.fragments.UpdaterFragment;

public class UpdaterActivity extends Activity {
    private static final String TAG = "OTAUpdater";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new UpdaterFragment())
                .commit();
    }
}
