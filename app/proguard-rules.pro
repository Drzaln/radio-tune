# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisible*Annotations, AnnotationDefault
-keep,includedescriptorclasses class com.rizal.radiotune.**$$serializer { *; }
-keepclassmembers class com.rizal.radiotune.** {
    *** Companion;
}
-keepclasseswithmembers class com.rizal.radiotune.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit / OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Media3
-dontwarn androidx.media3.**

# Coil 3 network fetcher is registered through ServiceLoader; keep the target so
# it is not stripped and images keep loading in release builds.
-keep class * implements coil3.util.FetcherServiceLoaderTarget { *; }
-keep class * implements coil3.util.DecoderServiceLoaderTarget { *; }
