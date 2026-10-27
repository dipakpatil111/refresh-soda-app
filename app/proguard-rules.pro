# Proguard rules for Refresh Soda App
# Room
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}

