# Consumer ProGuard/R8 rules for TrapezeTest.
#
# Intentionally empty. This artifact is a test dependency — it is consumed by `testImplementation`
# and `androidTestImplementation`, never packaged into a release build, so no keep rule it
# declared would ever be applied by R8.
