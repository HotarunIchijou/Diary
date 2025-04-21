# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keepattributes Signature
-keepclassmembers class * extends com.onegravity.rteditor.spans.RTSpan {
    public <init>(int);
}
# Keep RTEditor classes and their members
-keep class com.onegravity.rteditor.** { *; }
-keep class com.onegravity.rteditor.api.** { *; }
-keep class com.onegravity.rteditor.spans.** { *; }
-keep class com.onegravity.rteditor.utils.** { *; }

# Keep all classes that might be involved in reflection
-keep class * implements com.onegravity.rteditor.api.RTMediaFactory { *; }
-keep class * implements com.onegravity.rteditor.api.RTProxy { *; }

# Keep signatures for generic types
-keepattributes Signature

# Keep annotations
-keepattributes *Annotation*

# Keep the RTManager and its components
-keep class com.onegravity.rteditor.RTManager { *; }
-keep class com.onegravity.rteditor.RTEditText { *; }

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-keep class com.google.firebase.database.** { *; }
