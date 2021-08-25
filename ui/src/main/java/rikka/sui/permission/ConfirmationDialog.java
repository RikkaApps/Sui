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

package rikka.sui.permission;

import static rikka.shizuku.ShizukuApiConstants.REQUEST_PERMISSION_REPLY_ALLOWED;
import static rikka.shizuku.ShizukuApiConstants.REQUEST_PERMISSION_REPLY_IS_ONETIME;

import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

import rikka.html.text.HtmlCompat;
import rikka.sui.R;
import rikka.sui.databinding.ConfirmationDialogBinding;
import rikka.sui.ktx.HandlerKt;
import rikka.sui.ktx.LayoutInflaterKt;
import rikka.sui.ktx.TextViewKt;
import rikka.sui.ktx.WindowKt;
import rikka.sui.util.AppLabel;
import rikka.sui.util.BridgeServiceClient;
import rikka.sui.util.Logger;
import rikka.sui.util.Unsafe;
import rikka.sui.util.UserHandleCompat;

public class ConfirmationDialog {

    private static final IBinder TOKEN = new Binder();
    private static final Logger LOGGER = new Logger("ConfirmationDialog");

    private final Context context;
    private final Resources resources;
    private final LayoutInflater layoutInflater;

    public ConfirmationDialog(Application application, Resources resources) {
        this.context = application;
        this.resources = resources;
        this.layoutInflater = LayoutInflater.from(application);
    }

    public void show(int requestUid, int requestPid, String requestPackageName, int requestCode) {
        HandlerKt.getMainHandler().post(() -> showInternal(requestUid, requestPid, requestPackageName, requestCode));
    }

    private void setResult(int requestUid, int requestPid, int requestCode, boolean allowed, boolean onetime) {
        Bundle data = new Bundle();
        data.putBoolean(REQUEST_PERMISSION_REPLY_ALLOWED, allowed);
        data.putBoolean(REQUEST_PERMISSION_REPLY_IS_ONETIME, onetime);

        try {
            BridgeServiceClient.getService().dispatchPermissionConfirmationResult(requestUid, requestPid, requestCode, data);
        } catch (Throwable e) {
            LOGGER.e("dispatchPermissionConfirmationResult");
        }
    }

    private void showInternal(int requestUid, int requestPid, String requestPackageName, int requestCode) {
        Resources.Theme theme = context.getTheme();
        boolean isNight = (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_YES) != 0;
        if (isNight) {
            theme.applyStyle(android.R.style.Theme_DeviceDefault_Dialog, true);
        } else {
            theme.applyStyle(android.R.style.Theme_DeviceDefault_Light_Dialog, true);
        }

        SystemDialogRootView root = new SystemDialogRootView(context) {

            @Override
            public boolean onBackPressed() {
                return false;
            }

            @Override
            public void onClose() {
                setResult(requestUid, requestPid, requestCode, false, true);
            }
        };

        View view = layoutInflater.inflate(resources.getLayout(R.layout.confirmation_dialog), root, false);
        ConfirmationDialogBinding binding = ConfirmationDialogBinding.bind(view);
        root.addView(binding.getRoot());

        String label = requestPackageName;
        int userId = UserHandleCompat.getUserId(requestUid);
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo ai = Objects.requireNonNull(Unsafe.<$android.content.pm.PackageManager>unsafeCast(pm)
                    .getApplicationInfoAsUser(requestPackageName, $android.content.pm.PackageManager.MATCH_UNINSTALLED_PACKAGES, userId));
            label = AppLabel.getAppLabel(ai, context);
        } catch (Throwable e) {
            LOGGER.e("getApplicationInfoAsUser");
        }

        binding.icon.setImageDrawable(resources.getDrawable(R.drawable.ic_su_24, theme));
        binding.title.setText(HtmlCompat.fromHtml(
                String.format(resources.getString(R.string.permission_warning_template), label, resources.getString(R.string.permission_description))));
        binding.button1.setText(resources.getString(R.string.grant_dialog_button_allow_always));
        binding.button2.setText(resources.getString(R.string.grant_dialog_button_allow_one_time));
        binding.button3.setText(resources.getString(R.string.grant_dialog_button_deny_and_dont_ask_again));

        ColorStateList buttonTextColor = resources.getColorStateList(R.color.confirmation_dialog_button_text, theme);
        binding.button1.setTextColor(buttonTextColor);
        binding.button2.setTextColor(buttonTextColor);
        binding.button3.setTextColor(buttonTextColor);

        binding.button1.setOnClickListener(v -> {
            setResult(requestUid, requestPid, requestCode, true, false);
            root.dismiss();
        });
        binding.button2.setOnClickListener(v -> {
            setResult(requestUid, requestPid, requestCode, true, true);
            root.dismiss();
        });
        binding.button3.setOnClickListener(v -> {
            setResult(requestUid, requestPid, requestCode, false, false);
            root.dismiss();
        });

        TextViewKt.applyCountdown(binding.button1, 1, null, 0);
        TextViewKt.applyCountdown(binding.button2, 1, null, 0);
        TextViewKt.applyCountdown(binding.button3, 1, null, 0);

        binding.getRoot().setBackground(resources.getDrawable(R.drawable.confirmation_dialog_background, theme));
        binding.getRoot().setClipToOutline(true);

        WindowManager.LayoutParams attr = new WindowManager.LayoutParams();
        attr.width = ViewGroup.LayoutParams.MATCH_PARENT;
        attr.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        attr.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        attr.type = WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG;
        attr.token = TOKEN;
        attr.gravity = Gravity.CENTER;
        attr.windowAnimations = android.R.style.Animation_Dialog;
        attr.dimAmount = 0.32f;
        attr.format = PixelFormat.TRANSLUCENT;
        WindowKt.setPrivateFlags(attr, WindowKt.getPrivateFlags(attr) | WindowKt.getSYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS());

        root.show(attr);
    }
}
