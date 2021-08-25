/*
 * This file is part of Sui.
 *
 * Sui is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Sui is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Sui.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2021 Sui Contributors
 */

package rikka.sui;

import android.app.ActivityManager;
import android.app.Application;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.annotation.Nullable;

import java.util.Objects;

import rikka.sui.app.AppActivity;
import rikka.sui.management.ManagementFragment;

public class SuiActivity extends AppActivity {

    public SuiActivity(Application application, Resources resources) {
        super(application, resources);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setTitle("Sui");
        Objects.requireNonNull(getSupportActionBar()).setSubtitle(BuildConfig.VERSION_NAME);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new ManagementFragment())
                .commit();

        setTaskDescription(new ActivityManager.TaskDescription("Sui"));
    }
}
