-repackageclasses rikka.sui

-keepclasseswithmembers class rikka.sui.SuiActivity {
     public <init>(...);
}

-keepclasseswithmembers class rikka.sui.SuiRequestPermissionDialog {
     public <init>(...);
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
    public static void check*(...);
    public static void throw*(...);
}

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-dontwarn android.**
-dontwarn com.android.**
