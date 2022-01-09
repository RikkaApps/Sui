-repackageclasses rikka.sui

-keep class rikka.sui.server.Starter {
    public static void main(java.lang.String[]);
}

-keep class rikka.sui.server.userservice.Starter {
    public static void main(java.lang.String[]);
}

-keep class rikka.sui.systemserver.SystemProcess {
    public static void main(java.lang.String[]);
    public static boolean execTransact(android.os.Binder, int, long, long, int);
}

-keep class rikka.sui.manager.ManagerProcess {
    public static void main(java.lang.String[]);
}

-keep class rikka.sui.settings.SettingsProcess {
    public static void main(java.lang.String[]);
    public static boolean execTransact(android.os.Binder, int, long, long, int);
}

-keep class rikka.sui.installer.Installer {
    public static void main(java.lang.String[]);
}

-keep class rikka.sui.installer.Uninstaller {
    public static void main(java.lang.String[]);
}

-keep class rikka.sui.shell.Shell {
    public static void main(java.lang.String[]);
}

-keepnames class * implements android.os.Parcelable

-keepclassmembers class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator CREATOR;
}

-assumenosideeffects class android.util.Log {
    public static *** d(...);
}

-assumenosideeffects class rikka.sui.util.Logger {
    public *** d(...);
}

-assumenosideeffects class rikka.shizuku.server.util.Logger {
    public *** d(...);
}

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-dontwarn android.**
-dontwarn com.android.**
