package rikka.sui.model;

import android.content.pm.PackageInfo;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class AppInfo implements Parcelable {

    public PackageInfo packageInfo;
    public int flags;
    public CharSequence label = null;

    public AppInfo() {
    }

    protected AppInfo(Parcel in) {
        packageInfo = in.readParcelable(PackageInfo.class.getClassLoader());
        flags = in.readInt();
    }

    public static final Creator<AppInfo> CREATOR = new Creator<AppInfo>() {
        @Override
        public AppInfo createFromParcel(Parcel in) {
            return new AppInfo(in);
        }

        @Override
        public AppInfo[] newArray(int size) {
            return new AppInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(packageInfo, flags);
        dest.writeInt(this.flags);
    }
}
