# Consumer ProGuard/R8 rules for Trapeze.
#
# These are applied to every application that depends on this library. Keep them minimal and
# justified — a keep rule is a permanent instruction to leave code unshrunk in someone else's
# app, so each one below names the reflective access it exists for.

# `TrapezeScreen` and `TrapezeNavigationResult` extend `Parcelable` on Android, and
# `TrapezeBackStack`'s saver restores them through `Bundle.getParcelable`. That resolves the
# `CREATOR` field by name at runtime, so R8 sees no reference to it and is free to remove it.
# Without this rule, every screen on a restored backstack fails to unparcel after process death
# — and because the saver drops entries it cannot read, the symptom is a silently truncated
# history rather than a crash.
#
# AGP's own `proguard-android-optimize.txt` carries an equivalent rule, but a library cannot
# assume its consumer opted into that file.
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
