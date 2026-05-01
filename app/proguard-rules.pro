# Planora ProGuard / R8 rules

# Keep all Room entities, DAOs, and database class
-keep class com.planora.app.core.data.database.** { *; }
-keep @androidx.room.Entity class * { *; }
-keepclassmembers @androidx.room.Entity class * { *; }
-keep class * extends androidx.room.RoomDatabase { *; }

# Keep workers and their constructors
-keep class com.planora.app.core.worker.** { *; }

# Keep Hilt-generated components
-keep class dagger.hilt.android.internal.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}

# noinspection ExpensiveKeepRuleInspection
-keep class net.zetetic.database.** { *; }
-dontwarn net.zetetic.**

# AndroidX Security
-keep class androidx.security.crypto.** { *; }

# Google Error Prone annotations (referenced by Tink, not needed at runtime)
-dontwarn com.google.errorprone.annotations.**

# noinspection ExpensiveKeepRuleInspection
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# Kotlin
-dontwarn kotlin.**
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# Keep enum names used by Room TypeConverters
-keepclassmembers enum com.planora.app.core.data.database.entities.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# Remove debug logging in release
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Google Drive API & Client Libraries
# Targeted keeps for reduced scope and better obfuscation
-keepclassmembers class * {
  @com.google.api.client.util.Key <fields>;
}
-keep class com.google.api.services.drive.Drive** { <fields>; <methods>; }
-keep class com.google.api.services.drive.model.** { <fields>; <methods>; }
-keep class com.google.api.client.googleapis.json.GoogleJsonResponseException { <fields>; <methods>; }
-keep class com.google.api.client.http.** { <fields>; <methods>; }
-keep class com.google.api.client.json.** { <fields>; <methods>; }
-keep class com.google.api.client.util.** { <fields>; <methods>; }

-keep class com.google.auth.oauth2.** { <fields>; <methods>; }
-keep class com.google.auth.http.** { <fields>; <methods>; }
-dontwarn com.google.auth.**


-dontwarn com.google.api.client.**
-dontwarn com.google.api.services.drive.**
-dontwarn sun.misc.Unsafe

# Gson
# Standard GSON rules to avoid broad blanket keep
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.TypeAdapter
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.Unsafe

# Planora Cloud Sync Data Models
# We MUST keep these names because Gson uses them for JSON serialization!
-keep class com.planora.app.core.data.backup.CloudSyncData { *; }
-keep class com.planora.app.core.data.backup.SyncPreferences { *; }
-keep class com.planora.app.core.data.database.entities.** { *; }

# Modern Identity & Credentials
# Only keep the specific classes we use via reflection / createFrom()
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keep class com.google.android.gms.auth.api.identity.** { *; }
-dontwarn androidx.credentials.internal.FrameworkImpl

# PDFBox optional dependencies
-dontwarn com.gemalto.jp2.**
-dontwarn org.bouncycastle.**
-dontwarn javax.xml.crypto.**
-dontwarn java.awt.**
