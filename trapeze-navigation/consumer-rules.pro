# Consumer ProGuard/R8 rules for TrapezeNavigation.
#
# Intentionally empty of keep rules. The one reflective path in this module is the backstack
# saver's `Bundle.getParcelable`, which needs `Parcelable.CREATOR` kept — that rule ships with
# `:trapeze`, and `trapeze-navigation` depends on it with `api(...)`, so every consumer of this
# module already receives it.
#
# Navigation results are addressed by string keys held in a `Bundle`, not by type lookup, so
# obfuscation does not affect them.
