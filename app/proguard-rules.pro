# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/user/Library/Android/sdk/tools/proguard/proguard-android.txt

-keepattributes *Annotation*

# Keep PDFBox classes
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**

# Keep Verum Omnis engine classes
-keep class com.verumomnis.forensic.** { *; }
