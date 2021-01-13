package rikka.sui.manager;

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
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import hidden.HiddenApiBridge;
import rikka.sui.databinding.ConfirmationDialogBinding;
import rikka.sui.ktx.HandlerKt;
import rikka.sui.ktx.ResourcesKt;
import rikka.sui.ktx.TextViewKt;
import rikka.sui.ktx.WindowKt;
import rikka.sui.manager.res.Drawables;
import rikka.sui.manager.res.Layouts;
import rikka.sui.manager.res.Strings;
import rikka.sui.manager.res.Utils;
import rikka.sui.manager.res.Xml;
import rikka.sui.util.UserHandleCompat;

import static rikka.shizuku.ShizukuApiConstants.REQUEST_PERMISSION_REPLY_ALLOWED;
import static rikka.shizuku.ShizukuApiConstants.REQUEST_PERMISSION_REPLY_IS_ONETIME;
import static rikka.sui.manager.ManagerConstants.LOGGER;

public class PermissionConfirmation {

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
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        Resources.Theme theme = context.getTheme();
        float density = context.getResources().getDisplayMetrics().density;
        float l1 = density * 8;
        float l2 = density * 16;
        int l2i = Math.round(l2);

        FrameLayout windowRoot = new FrameLayout(context);
        ConfirmationDialogBinding binding = ConfirmationDialogBinding.bind(layoutInflater.inflate(Xml.get(Layouts.confirmation_dialog), windowRoot, false));

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) binding.getRoot().getLayoutParams();
        lp.leftMargin = l2i;
        lp.rightMargin = l2i;
        lp.topMargin = l2i;
        lp.bottomMargin = l2i;
        binding.getRoot().setLayoutParams(lp);
        windowRoot.addView(binding.getRoot());

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
            binding.icon.setImageDrawable(VectorDrawable.createFromXml(context.getResources(), Xml.get(Drawables.ic_su_24)));
        } catch (IOException | XmlPullParserException e) {
            LOGGER.e(e, "setImageDrawable");
        }
        binding.title.setText(Html.fromHtml(
                String.format(Strings.get(Strings.permission_warning_template), label, Strings.get(Strings.permission_description))));
        binding.button1.setText(Strings.get(Strings.grant_dialog_button_allow_always));
        binding.button2.setText(Strings.get(Strings.grant_dialog_button_allow_one_time));
        binding.button3.setText(Strings.get(Strings.grant_dialog_button_deny_and_dont_ask_again));

        int colorForeground = ResourcesKt.resolveColor(theme, android.R.attr.colorForeground);
        int colorAccent = ResourcesKt.resolveColor(theme, android.R.attr.colorAccent);

        ColorStateList buttonTextColor = new ColorStateList(
                new int[][]{
                        new int[]{-android.R.attr.state_enabled},
                        new int[]{}
                }, new int[]{colorForeground & 0xffffff | 0x61000000, colorAccent});

        binding.button1.setTextColor(buttonTextColor);
        binding.button2.setTextColor(buttonTextColor);
        binding.button3.setTextColor(buttonTextColor);

        binding.button1.setOnClickListener(v -> {
            setResult(requestUid, requestPid, requestCode, true, false);
            wm.removeView(windowRoot);
        });
        binding.button2.setOnClickListener(v -> {
            setResult(requestUid, requestPid, requestCode, true, true);
            wm.removeView(windowRoot);
        });
        binding.button3.setOnClickListener(v -> {
            setResult(requestUid, requestPid, requestCode, false, false);
            wm.removeView(windowRoot);
        });

        TextViewKt.applyCountdown(binding.button1, 1, null, 0);
        TextViewKt.applyCountdown(binding.button2, 1, null, 0);
        TextViewKt.applyCountdown(binding.button3, 1, null, 0);

        ShapeDrawable shapeDrawable = new ShapeDrawable();
        shapeDrawable.setShape(new RoundRectShape(new float[]{l1, l1, l1, l1, l1, l1, l1, l1}, null, null));
        shapeDrawable.getPaint().setColor(ResourcesKt.resolveColor(theme, android.R.attr.colorBackground));
        //InsetDrawable insetDrawable = new InsetDrawable(shapeDrawable, l2i, l2i, l2i, l2i);
        binding.getRoot().setBackground(shapeDrawable);
        binding.getRoot().setElevation(l1);

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

        wm.addView(windowRoot, attr);
    }
}
