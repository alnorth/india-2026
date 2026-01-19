# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /usr/local/android-sdk/tools/proguard/proguard-android.txt

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Gson internal classes - critical for generic type handling
-keep class com.google.gson.internal.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers class com.google.gson.reflect.TypeToken { *; }

# Keep generic signature for classes used with Gson (prevents ClassCastException)
-keepattributes Signature
-keep class com.google.gson.** { *; }

# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Keep generic type information for collections
-keep class java.util.** { *; }
-keep interface java.util.** { *; }
-keepclassmembers class * {
    ** get*();
    void set*(***);
}

# Keep our API models with all fields and signatures
-keep class com.alnorth.india2026.api.** { *; }
-keep class com.alnorth.india2026.model.** { *; }

# Preserve field names and generic signatures in data classes
-keepclassmembers class com.alnorth.india2026.api.** {
    <fields>;
}
-keepclassmembers class com.alnorth.india2026.model.** {
    <fields>;
}
