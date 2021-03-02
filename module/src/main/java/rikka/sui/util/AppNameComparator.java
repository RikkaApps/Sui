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
