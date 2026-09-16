# Add project specific ProGuard rules here.
-keep class com.example.c002_dhruvil_assignment1.** { *; }
-keep public class * extends android.app.Activity { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
-keep class androidx.datastore.preferences.** { *; }

# Keep Ktor Client classes used by Google AI Client SDK
-keep class io.ktor.client.** { *; }
-dontwarn io.ktor.**
