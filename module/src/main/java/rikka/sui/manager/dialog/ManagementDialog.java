package rikka.sui.manager.dialog;

import android.app.ActivityThread;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.os.Binder;
import android.os.IBinder;
import android.text.Html;
import android.util.LruCache;
import android.util.Pair;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import rikka.sui.BuildConfig;
import rikka.sui.databinding.ManagementAppItemBinding;
import rikka.sui.databinding.ManagementDialogBinding;
import rikka.sui.ktx.DrawableKt;
import rikka.sui.ktx.HandlerKt;
import rikka.sui.ktx.ResourcesKt;
import rikka.sui.ktx.WindowKt;
import rikka.sui.manager.BridgeServiceClient;
import rikka.sui.manager.WorkerHandler;
import rikka.sui.manager.res.Res;
import rikka.sui.manager.res.Strings;
import rikka.sui.manager.res.Xml;
import rikka.sui.model.AppInfo;
import rikka.sui.server.config.Config;
import rikka.sui.util.UserHandleCompat;

import static rikka.sui.manager.ManagerConstants.LOGGER;

public class ManagementDialog {

    private static final IBinder TOKEN = new Binder();

    private static final AppIconLruCache ICON_CACHE;

    static {
        long maxMemory = Runtime.getRuntime().maxMemory() / 1024;
        int availableCacheSize = (int) maxMemory / 4;
        ICON_CACHE = new AppIconLruCache(availableCacheSize);
    }

    private static class AppIconLruCache extends LruCache<Pair<String, Integer>, Bitmap> {

        /**
         * @param maxSize for caches that do not override {@link #sizeOf}, this is
         *                the maximum number of entries in the cache. For all other caches,
         *                this is the maximum sum of the sizes of the entries in this cache.
         */
        public AppIconLruCache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected int sizeOf(Pair<String, Integer> key, Bitmap value) {
            return value.getByteCount() / 1024;
        }

        public Bitmap getOrCreate(ApplicationInfo applicationInfo, PackageManager packageManager, int userId, int size) {
            Bitmap cached = get(new Pair<>(applicationInfo.packageName, userId));
            if (cached != null) {
                return cached;
            }

            Drawable drawable;
            try {
                drawable = applicationInfo.loadIcon(packageManager);
            } catch (Throwable e) {
                LOGGER.w(e, "loadIcon for %s %d", applicationInfo.packageName, userId);
                drawable = null;
            }

            if (drawable == null) {
                return null;
            }

            Bitmap bitmap = DrawableKt.toBitmap(drawable, size, size, null);
            put(new Pair<>(applicationInfo.packageName, userId), bitmap);
            return bitmap;
        }
    }

    public static void show() {
        HandlerKt.getMainHandler().post(ManagementDialog::showInternal);
    }

    private static void showInternal() {
        Context application = ActivityThread.currentActivityThread().getApplication();
        if (application == null) {
            return;
        }

        boolean isNight = (application.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_YES) != 0;
        Context context = new ContextThemeWrapper(application, isNight ? android.R.style.Theme_Material : android.R.style.Theme_Material_Light);
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        Resources.Theme theme = context.getTheme();

        float density = context.getResources().getDisplayMetrics().density;

        SystemDialogRootView root = new SystemDialogRootView(context);

        View view = layoutInflater.inflate(Xml.get(Res.layout.management_dialog), root, false);
        ManagementDialogBinding binding = ManagementDialogBinding.bind(view);
        root.addView(binding.getRoot());
        binding.title.setNavigationOnClickListener((v) -> root.dismiss());

        setupView(context, layoutInflater, binding);

        root.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                v.removeOnAttachStateChangeListener(this);
                v.requestApplyInsets();
            }

            @Override
            public void onViewDetachedFromWindow(View v) {

            }
        });

        root.setSystemUiVisibility(root.getSystemUiVisibility()
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int left = insets.getSystemWindowInsetLeft();
            int top = insets.getSystemWindowInsetTop();
            int right = insets.getSystemWindowInsetRight();
            int bottom = insets.getSystemWindowInsetBottom();

            /*v.setPadding(v.getPaddingLeft() + left,
                    v.getPaddingTop(),
                    v.getPaddingRight() + right,
                    v.getPaddingBottom());*/

            binding.title.setPadding(
                    binding.title.getPaddingLeft(),
                    /*binding.title.getPaddingTop() + */top,
                    binding.title.getPaddingRight(),
                    binding.title.getPaddingBottom()
            );

            binding.list.setPadding(
                    binding.list.getPaddingLeft(),
                    binding.list.getPaddingTop(),
                    binding.list.getPaddingRight(),
                    /*binding.list.getPaddingBottom() + */bottom
            );

            return insets;
        });

        WindowManager.LayoutParams attr = new WindowManager.LayoutParams();
        attr.width = ViewGroup.LayoutParams.MATCH_PARENT;
        attr.height = ViewGroup.LayoutParams.MATCH_PARENT;
        attr.dimAmount = 0;
        attr.flags = WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
                | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        attr.type = WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG;
        attr.token = TOKEN;
        attr.gravity = Gravity.CENTER;
        attr.windowAnimations = android.R.style.Animation_Dialog;
        attr.format = PixelFormat.TRANSLUCENT;
        WindowKt.setPrivateFlags(attr, WindowKt.getPrivateFlags(attr) | WindowKt.getSYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS());

        root.show(attr);
    }

    private static void setupView(Context context, LayoutInflater layoutInflater, ManagementDialogBinding binding) {
        binding.title.setTitle(Strings.get(Res.string.management_title));
        binding.title.setSubtitle("Sui v" + BuildConfig.VERSION_NAME);

        Drawable closeDrawable = null;
        try {
            closeDrawable = VectorDrawable.createFromXml(context.getResources(), Xml.get(Res.drawable.ic_close_24));
            closeDrawable.setTint(ResourcesKt.resolveColor(context.getTheme(), android.R.attr.textColorPrimary));
            closeDrawable.setTintMode(PorterDuff.Mode.SRC_IN);
        } catch (IOException | XmlPullParserException e) {
            LOGGER.e(e, "setImageDrawable");
        }
        binding.title.setNavigationIcon(closeDrawable);
        binding.title.setNavigationContentDescription(Strings.get(Res.string.close));
        binding.list.setFastScrollEnabled(true);

        Adapter adapter = new Adapter(layoutInflater);
        binding.list.setAdapter(adapter);

        WorkerHandler.get().post(() -> {
            List<AppInfo> data = new ArrayList<>();
            try {
                PackageManager pm = context.getPackageManager();
                data.addAll(BridgeServiceClient.getApplications(-1 /* ALL */));
                for (AppInfo item : data) {
                    try {
                        item.label = item.packageInfo.applicationInfo.loadLabel(pm);
                    } catch (Throwable e) {
                        LOGGER.w(e, "loadLabel");
                        item.label = item.packageInfo.packageName;
                    }
                }

                Collections.sort(data, new AppInfoComparator());
            } catch (Throwable e) {
                LOGGER.e(e, "getApplications");
            }

            binding.getRoot().post(() -> {
                if (!data.isEmpty()) {
                    adapter.updateData(data);
                    binding.progress.setVisibility(View.GONE);
                    binding.list.setVisibility(View.VISIBLE);
                } else {

                }
            });

        });
    }

    private static class Adapter extends BaseAdapter {

        private static final int VH_KEY = 1599296842;

        private final LayoutInflater layoutInflater;
        private final List<AppInfo> data;

        private Adapter(LayoutInflater layoutInflater) {
            this.layoutInflater = layoutInflater;
            this.data = new ArrayList<>();
        }

        public void updateData(List<AppInfo> newData) {
            data.clear();
            data.addAll(newData);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public AppInfo getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup container) {
            AppItemViewHolder viewHolder;

            if (convertView == null) {
                convertView = layoutInflater.inflate(Xml.get(Res.layout.management_app_item), container, false);
                viewHolder = null;
            } else {
                viewHolder = (AppItemViewHolder) convertView.getTag(VH_KEY);
            }

            if (viewHolder == null) {
                viewHolder = new AppItemViewHolder(ManagementAppItemBinding.bind(convertView));
                convertView.setTag(VH_KEY, viewHolder);
            }
            viewHolder.data = getItem(position);
            viewHolder.position = position;
            viewHolder.bind();
            return convertView;
        }
    }

    private static class AppItemViewHolder {

        private static final Typeface SANS_SERIF = Typeface.create("sans-serif", Typeface.NORMAL);
        private static final Typeface SANS_SERIF_MEDIUM = Typeface.create("sans-serif-medium", Typeface.NORMAL);

        public final ManagementAppItemBinding binding;
        public final ArrayAdapter<CharSequence> adapter;
        public final AdapterView.OnItemSelectedListener onItemSelectedListener = new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int newValue;
                switch (position) {
                    case 0:
                        newValue = Config.FLAG_ALLOWED;
                        break;
                    case 1:
                        newValue = Config.FLAG_DENIED;
                        break;
                    case 2:
                        newValue = Config.FLAG_HIDDEN;
                        break;
                    default:
                        newValue = 0;
                        break;
                }

                try {
                    BridgeServiceClient.getService().updateFlagsForUid(data.packageInfo.applicationInfo.uid, Config.MASK_PERMISSION, newValue);

                } catch (Throwable e) {
                    LOGGER.e("updateFlagsForUid");
                    return;
                }

                data.flags = (data.flags & ~Config.MASK_PERMISSION) | newValue;
                parent.setSelection(position);
                syncViewStateForFlags();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        };

        public AppInfo data;
        public int position;

        private final ColorStateList textColorSecondary;
        private final ColorStateList textColorPrimary;

        public AppItemViewHolder(ManagementAppItemBinding binding) {
            Context context = binding.getRoot().getContext();
            Resources.Theme theme = context.getTheme();
            boolean isNight = (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_YES) != 0;
            int colorAccent = isNight ? 0xffc8e6c9 : 0xff338158/*ResourcesKt.resolveColor(theme, android.R.attr.colorAccent)*/;
            int colorForeground = ResourcesKt.resolveColor(theme, android.R.attr.colorForeground);
            ColorStateList textColorTertiary = ResourcesKt.resolveColorStateList(theme, android.R.attr.textColorTertiary);
            int colorError = isNight ? 0xFF8A80 : 0xFF5252;

            textColorSecondary = ResourcesKt.resolveColorStateList(theme, android.R.attr.textColorSecondary);
            textColorPrimary = ResourcesKt.resolveColorStateList(theme, android.R.attr.textColorPrimary);

            this.binding = binding;
            this.binding.getRoot().setOnClickListener(v -> this.binding.button1.performClick());
            this.adapter = new ArrayAdapter<CharSequence>(binding.button1.getContext(),
                    android.R.layout.simple_spinner_item, new CharSequence[]{
                    Html.fromHtml(String.format("<font face=\"sans-serif-medium\" color=\"#%2$s\">%1$s</font>",
                            Strings.get(Res.string.permission_allowed),
                            String.format(Locale.ENGLISH, "%06x", colorAccent & 0xffffff)
                    )),
                    Html.fromHtml(String.format("<font face=\"sans-serif-medium\" color=\"#%2$s\">%1$s</font>",
                            Strings.get(Res.string.permission_denied),
                            String.format(Locale.ENGLISH, "%06x", colorError & 0xffffff)
                    )),
                    Html.fromHtml(String.format("<font face=\"sans-serif-medium\" color=\"#%2$s\">%1$s</font>",
                            Strings.get(Res.string.permission_hidden),
                            String.format(Locale.ENGLISH, "%06x", colorForeground & 0xffffff)
                    )),
                    Strings.get(Res.string.permission_ask),
            }) {
                @NonNull
                @Override
                public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    if (convertView == null) {
                        View view = super.getView(position, convertView, parent);
                        TextView textView = view.findViewById(android.R.id.text1);
                        textView.setPadding(0, textView.getPaddingTop(), 0, textView.getPaddingBottom());
                        textView.setTextColor(textColorTertiary);
                        textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
                        return view;
                    } else {
                        return super.getView(position, convertView, parent);
                    }

                }
            };
            this.adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }

        private void bind() {
            Context context = binding.getRoot().getContext();

            int userId = UserHandleCompat.getUserId(data.packageInfo.applicationInfo.uid);
            if (userId != 0) {
                binding.title.setText(String.format(Strings.get(Res.string.brackets_format), data.label, userId));
            } else {
                binding.title.setText(data.label);
            }
            binding.summary.setText(data.packageInfo.packageName);
            binding.button1.setAdapter(adapter);
            binding.button1.setOnItemSelectedListener(onItemSelectedListener);

            syncViewStateForFlags();

            Bitmap bitmap = ICON_CACHE.getOrCreate(data.packageInfo.applicationInfo, context.getPackageManager(), userId, Math.round(context.getResources().getDisplayMetrics().density * 32));
            binding.icon.setImageBitmap(bitmap);
        }

        private void syncViewStateForFlags() {
            boolean allowed = (data.flags & Config.FLAG_ALLOWED) != 0;
            boolean denied = (data.flags & Config.FLAG_DENIED) != 0;
            boolean hidden = (data.flags & Config.FLAG_HIDDEN) != 0;
            if (allowed) {
                binding.title.setTextColor(textColorPrimary);

                binding.title.setTypeface(SANS_SERIF_MEDIUM);

                binding.button1.setSelection(0);
            } else if (denied) {
                binding.title.setTextColor(textColorSecondary);

                binding.title.setTypeface(SANS_SERIF);

                binding.button1.setSelection(1);
            } else if (hidden) {
                binding.title.setTextColor(textColorSecondary);

                binding.title.setTypeface(SANS_SERIF);

                binding.button1.setSelection(2);
            } else {
                binding.title.setTextColor(textColorSecondary);

                binding.title.setTypeface(SANS_SERIF);

                binding.button1.setSelection(3);
            }
        }
    }

}
