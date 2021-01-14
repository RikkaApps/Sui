package rikka.sui.util;

import android.os.Process;

import java.util.Comparator;

public class AppNameComparator<T> implements Comparator<T> {

    private final LabelComparator mLabelComparator;
    private final InfoProvider<T> mInfoProvider;

    public interface InfoProvider<T> {
        CharSequence getTitle(T item);
        String getPackageName(T item);
        int getUserId(T item);
    }

    public AppNameComparator(InfoProvider<T> infoProvider) {
        mInfoProvider = infoProvider;
        mLabelComparator = new LabelComparator();
    }

    @Override
    public int compare(T a, T b) {
        // Order by the title in the current locale
        int result = mLabelComparator.compare(
                mInfoProvider.getTitle(a).toString(),
                mInfoProvider.getTitle(b).toString());

        if (result != 0) {
            return result;
        }

        // If labels are same, compare component names
        result = mInfoProvider.getPackageName(a).compareTo(mInfoProvider.getPackageName(b));
        if (result != 0) {
            return result;
        }

        if (Process.myUserHandle().hashCode() == mInfoProvider.getUserId(a)) {
            return -1;
        } else {
            int aUserId = mInfoProvider.getUserId(a);
            int bUserId = mInfoProvider.getUserId(b);
            return Integer.compare(aUserId, bUserId);
        }
    }
}
