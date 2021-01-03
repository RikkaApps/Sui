-repackageclasses app.rikka.sui

-keep class app.rikka.sui.server.Starter {
    public static void main(java.lang.String[]);
}

-keep class app.rikka.sui.systemserver.SystemProcess {
    public static void main(java.lang.String[]);
    public static boolean execTransact(int, long, long, int);
}

-assumenosideeffects class android.util.Log {
    public static *** d(...);
}

-assumenosideeffects class app.rikka.sui.util.Logger {
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