# Release hardening (APP-SEC-1, NFR-6.2): remove verbose, debug and info logging.
# Warnings and errors stay; they contain only ids, counts and exception class names.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
