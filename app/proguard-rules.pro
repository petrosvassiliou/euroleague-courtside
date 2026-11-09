# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisibleAnnotations, EnclosingMethod
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.euroleague.courtside.**$$serializer { *; }
-keepclassmembers class com.euroleague.courtside.** {
    *** Companion;
}
-keepclasseswithmembers class com.euroleague.courtside.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Preserve EuroLeague Courtside Data DTOs, Retrofit Interfaces, Room Entities & Domain Models
-keep interface com.euroleague.courtside.data.remote.** { *; }
-keep class com.euroleague.courtside.data.remote.** { *; }
-keep class com.euroleague.courtside.data.local.** { *; }
-keep class com.euroleague.courtside.domain.model.** { *; }
-keep class com.euroleague.courtside.data.repository.** { *; }
-keep class kotlin.coroutines.Continuation

# Retrofit 2 & OkHttp 3
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes MethodParameters, Exceptions
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

