# Keep Xposed/LSPosed module entry declared in assets/xposed_init.
-keep class com.astor.oplusconfighook.HookEntry { *; }

# Keep exact hook classes invoked directly from entry path.
-keep class com.astor.oplusconfighook.PolicyStore { *; }

# Keep Xposed API signatures used by compileOnly integration and silence warnings.
-keep class de.robv.android.xposed.** { *; }
-dontwarn de.robv.android.xposed.**
