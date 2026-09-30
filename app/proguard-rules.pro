# Room Database keep rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# kotlinx.serialization keep rules for Gemini SDK
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class **$serializer {
    *** INSTANCE;
}

# Keep DataStore Preferences
-keepclassmembers class * extends androidx.datastore.preferences.core.Preferences
