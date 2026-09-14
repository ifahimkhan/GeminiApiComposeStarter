# Keep Rules for Generative AI SDK
-keep class com.google.ai.client.generativeai.** { *; }

# Keep Rules for Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep Room Entities and DAOs
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}

# Keep DataStore
-keep class androidx.datastore.** { *; }
