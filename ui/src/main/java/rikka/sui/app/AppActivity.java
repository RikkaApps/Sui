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

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import rikka.core.res.ResourcesKt;
import rikka.material.app.DayNightDelegate;
import rikka.material.app.MaterialActivity;
import rikka.material.widget.AppBarLayout;
import rikka.sui.R;

public class AppActivity extends MaterialActivity {

    private final Application application;
    private final Resources resources;

    private ViewGroup rootView;
    private AppBarLayout toolbarContainer;

    public AppActivity(Application application, Resources resources) {
        this.application = application;
        this.resources = resources;
        DayNightDelegate.setApplicationContext(this);
    }

    @Override
    public Context getApplicationContext() {
        return application;
    }

    @Override
    public ClassLoader getClassLoader() {
        return AppActivity.class.getClassLoader();
    }

    @Override
    public Resources getResources() {
        return resources;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.appbar_fragment_activity);

        rootView = findViewById(R.id.root);
        toolbarContainer = findViewById(R.id.toolbar_container);
        Toolbar toolbar = findViewById(R.id.toolbar);

        setAppBar(toolbarContainer, toolbar);
    }

    @Override
    public void setContentView(int layoutResID) {
        getLayoutInflater().inflate(layoutResID, rootView, true);
        rootView.bringChildToFront(toolbarContainer);
    }

    public void setContentView(@Nullable View view) {
        setContentView(view, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    public void setContentView(@Nullable View view, @Nullable ViewGroup.LayoutParams params) {
        rootView.addView(view, 0, params);
    }

    public void onApplyTranslucentSystemBars() {
        super.onApplyTranslucentSystemBars();

        final Window window = getWindow();
        final Resources.Theme theme = getTheme();

        if (Build.VERSION.SDK_INT >= 26 && window != null) {
            View decorView = window.getDecorView();
            if (decorView == null) {
                return;
            }

            decorView.post(() -> {
                WindowInsets insets = decorView.getRootWindowInsets();
                float insetsBottom = (float) (insets != null ? insets.getSystemWindowInsetBottom() : 0);
                if (insetsBottom >= Resources.getSystem().getDisplayMetrics().density * (float) 40) {
                    window.setNavigationBarColor(ResourcesKt.resolveColor(theme, android.R.attr.navigationBarColor) & 0x00ffffff | 0xdf000000);
                    if (Build.VERSION.SDK_INT >= 29) {
                        window.setNavigationBarContrastEnforced(true);
                    }
                } else {
                    window.setNavigationBarColor(Color.TRANSPARENT);
                    if (Build.VERSION.SDK_INT >= 29) {
                        window.setNavigationBarContrastEnforced(false);
                    }
                }
            });
        }
    }
}
