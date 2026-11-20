# Keep JNI native bridge & methods called from libth_kb_core.so
-keep class com.thamnis.thkb.engine.NativeSuggestionBridge { *; }
-keepclasseswithmembers class com.thamnis.thkb.** {
    native <methods>;
}

# Keep UniFFI / JNA native symbols
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }
-dontwarn java.awt.**
