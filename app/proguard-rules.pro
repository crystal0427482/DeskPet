# DeskPet ProGuard Rules

# Keep data classes used with Gson
-keep class com.deskpet.app.data.** { *; }
-keep class com.deskpet.app.PetState { *; }

# - Room not used in this project
