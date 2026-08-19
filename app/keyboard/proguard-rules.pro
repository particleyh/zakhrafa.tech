-dontwarn com.google.android.gms.ads.**
-keep class com.google.android.gms.ads.** { *; }
-keep class com.zakhrafa.keyboard.** { *; }
-keep class com.zakhrafa.engine.** { *; }

# AdMob callbacks
-keep class com.google.android.gms.ads.FullScreenContentCallback { *; }
-keep class com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback { *; }
-keep class com.google.android.gms.ads.rewarded.RewardedAdLoadCallback { *; }
-keep class com.google.android.gms.ads.rewarded.RewardItem { *; }
-keep class com.google.android.gms.ads.OnUserEarnedRewardListener { *; }

# Firebase
-keep class com.google.firebase.analytics.** { *; }
-keep class com.google.firebase.installations.** { *; }

# Firebase Crashlytics
-keep class com.google.firebase.crashlytics.** { *; }
-dontwarn com.google.firebase.crashlytics.**

# InputMethodService
-keep class * extends android.inputmethodservice.InputMethodService { *; }

# Avoid stripping enums
-keepclassmembers enum * { **[] $VALUES; public *; }
