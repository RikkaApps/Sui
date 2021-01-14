package rikka.sui.manager.dialog;

import java.util.Comparator;

import rikka.sui.model.AppInfo;
import rikka.sui.util.AppNameComparator;
import rikka.sui.util.UserHandleCompat;

public class AppInfoComparator implements Comparator<AppInfo> {

    private final AppNameComparator<AppInfo> appNameComparator = new AppNameComparator<>(new AppInfoProvider());

    @Override
    public int compare(AppInfo o1, AppInfo o2) {
        int o1f = o1.flags, o2f = o2.flags;
        if (o1.flags == 0) o1f = Integer.MAX_VALUE;
        if (o2.flags == 0) o2f = Integer.MAX_VALUE;
        int c = Integer.compare(o1f, o2f);
        if (c == 0) return appNameComparator.compare(o1, o2);
        return c;
    }

    private static class AppInfoProvider implements AppNameComparator.InfoProvider<AppInfo> {

        @Override
        public CharSequence getTitle(AppInfo item) {
            if (item.label != null) return item.label;
            return item.packageInfo.packageName;
        }

        @Override
        public String getPackageName(AppInfo item) {
            return item.packageInfo.packageName;
        }

        @Override
        public int getUserId(AppInfo item) {
            return UserHandleCompat.getUserId(item.packageInfo.applicationInfo.uid);
        }
    }
}
