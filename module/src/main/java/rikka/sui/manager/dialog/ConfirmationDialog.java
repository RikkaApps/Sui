package rikka.sui.manager.dialog;

import android.app.ActivityThread;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.VectorDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Html;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import hidden.HiddenApiBridge;
import rikka.sui.databinding.ConfirmationDialogBinding;
import rikka.sui.ktx.HandlerKt;
import rikka.sui.ktx.ResourcesKt;
import rikka.sui.ktx.TextViewKt;
import rikka.sui.ktx.WindowKt;
import rikka.sui.util.BridgeServiceClient;
import rikka.sui.resource.Res;
import rikka.sui.resource.Strings;
import rikka.sui.resource.Utils;
import rikka.sui.resource.Xml;
import rikka.sui.util.UserHandleCompat;

import static rikka.shizuku.ShizukuApiConstants.REQUEST_PERMISSION_REPLY_ALLOWED;
import static rikka.shizuku.ShizukuApiConstants.REQUEST_PERMISSION_REPLY_IS_ONETIME;
import static rikka.sui.manager.ManagerConstants.LOGGER;

public class ConfirmationDialog {

    private static final IBinder TOKEN = new Binder();

    public static void show(int requestUid, int requestPid, String requestPackageName, int requestCode) {
        HandlerKt.getMainHandler().post(() -> showInternal(requestUid, requestPid, requestPackageName, requestCode));
    }

    private static void setResult(int requestUid, int requestPid, int requestCode, boolean allowed, boolean onetime) {
        Bundle data = new Bundle();
        data.putBoolean(REQUEST_PERMISSION_REPLY_ALLOWED, allowed);
        data.putBoolean(REQUEST_PERMISSION_REPLY_IS_ONETIME, onetime);

        try {
            BridgeServiceClient.getService().dispatchPermissionConfirmationResult(requestUid, requestPid, requestCode, data);
        } catch (Throwable e) {
            LOGGER.e("dispatchPermissionConfirmationResult");
        }
    }

    private static void showInternal(int requestUid, int requestPid, String requestPackageName, int requestCode) {
        Context application = ActivityThread.currentActivityThread().getApplication();
        if (application == null) {
            return;
        }
        boolean isNight = (application.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_YES) != 0;

        Context context = new ContextThemeWrapper(application, isNight ? android.R.style.Theme_Material_Dialog_Alert : android.R.style.Theme_Material_Light_Dialog_Alert);
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        Resources.Theme theme = context.getTheme();
        float density = context.getResources().getDisplayMetrics().density;
        float l1 = density * 8;

        int colorForeground = ResourcesKt.resolveColor(theme, android.R.attr.colorForeground);
        int colorAccent = isNight ? 0xffc8e6c9 : 0xff338158/*ResourcesKt.resolveColor(theme, android.R.attr.colorAccent)*/;

        ColorStateList buttonTextColor = new ColorStateList(
                new int[][]{
                        new int[]{-android.R.attr.state_enabled},
                        new int[]{}
                }, new int[]{colorForeground & 0xffffff | 0x61000000, colorAccent});

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

        View view = layoutInflater.inflate(Xml.get(Res.layout.confirmation_dialog), root, false);
        ConfirmationDialogBinding binding = ConfirmationDialogBinding.bind(view);
        root.addView(binding.getRoot());

        String label = requestPackageName;
        int userId = UserHandleCompat.getUserId(requestUid);
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo ai = HiddenApiBridge.PackageManager_getApplicationInfoAsUser(pm, requestPackageName, 0x00002000 /*MATCH_UNINSTALLED_PACKAGES*/, userId);
            label = Utils.getAppLabel(ai, context);
        } catch (Throwable e) {
            LOGGER.e("getApplicationInfoAsUser");
        }

        try {
            binding.icon.setImageDrawable(VectorDrawable.createFromXml(context.getResources(), Xml.get(Res.drawable.ic_su_24)));
        } catch (IOException | XmlPullParserException e) {
            LOGGER.e(e, "setImageDrawable");
        }
        binding.icon.setImageTintList(ColorStateList.valueOf(colorAccent));
        binding.title.setText(Html.fromHtml(
                String.format(Strings.get(Res.string.permission_warning_template), label, Strings.get(Res.string.permission_description))));
        binding.button1.setText(Strings.get(Res.string.grant_dialog_button_allow_always));
        binding.button2.setText(Strings.get(Res.string.grant_dialog_button_allow_one_time));
        binding.button3.setText(Strings.get(Res.string.grant_dialog_button_deny_and_dont_ask_again));

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

        ShapeDrawable shapeDrawable = new ShapeDrawable();
        shapeDrawable.setShape(new RoundRectShape(new float[]{l1, l1, l1, l1, l1, l1, l1, l1}, null, null));
        shapeDrawable.getPaint().setColor(ResourcesKt.resolveColor(theme, android.R.attr.colorBackground));
        binding.getRoot().setBackground(shapeDrawable);

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
