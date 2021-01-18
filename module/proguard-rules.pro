-repackageclasses rikka.sui

-keep class rikka.sui.server.Starter {
    public static void main(java.lang.String[]);
}

-keep class rikka.sui.server.userservice.Starter {
    public static void main(java.lang.String[]);
}

-keep class rikka.sui.systemserver.SystemProcess {
    public static void main(java.lang.String[]);
    public static boolean execTransact(int, long, long, int);
}

-keep class rikka.sui.manager.ManagerProcess {
    public static void main(java.lang.String[], java.nio.ByteBuffer[]);
}

-keep class rikka.sui.settings.SettingsProcess {
    public static void main(java.lang.String[], java.nio.ByteBuffer[]);
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

-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
	public static void checkExpressionValueIsNotNull(...);
	public static void checkNotNullExpressionValue(...);
	public static void checkReturnedValueIsNotNull(...);
	public static void checkFieldIsNotNull(...);
	public static void checkParameterIsNotNull(...);
}

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-dontwarn android.**
-dontwarn com.android.**