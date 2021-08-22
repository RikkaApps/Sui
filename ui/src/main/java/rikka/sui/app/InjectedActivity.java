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

package rikka.sui.app;

import android.content.ComponentName;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.annotation.Nullable;

import rikka.material.app.DayNightDelegate;
import rikka.material.app.MaterialActivity;
import rikka.sui.R;

public class InjectedActivity extends MaterialActivity {

    private final Resources resources;

    public InjectedActivity(Resources resources) {
        this.resources = resources;
        DayNightDelegate.setApplicationContext(this);
    }

    @Override
    public ClassLoader getClassLoader() {
        return InjectedActivity.class.getClassLoader();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
    }

    @Override
    public Resources getResources() {
        return resources;
    }

    @Override
    public ComponentName getComponentName() {
        if (getIntent() != null && getIntent().hasExtra("apk")) {
            return ComponentName.unflattenFromString("null/null");
        }
        return super.getComponentName();
    }

}
