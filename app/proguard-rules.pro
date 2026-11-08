# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.euroleague.courtside.data.remote.**$$serializer { *; }
-keepclassmembers class com.euroleague.courtside.data.remote.** {
    *** Companion;
}
-keepclasseswithmembers class com.euroleague.courtside.data.remote.** {
    kotlinx.serialization.KSerializer serializer(...);
}
