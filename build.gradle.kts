plugins {
    id("com.android.application")             version "9.1.0"         apply false
    id("com.android.library")                 version "9.1.0"         apply false
    id("com.google.devtools.ksp")             version "2.3.6" apply false
    id("com.google.dagger.hilt.android")      version "2.59.2"        apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20"        apply false
    id("com.google.gms.google-services")      version "4.4.4"         apply false
}

// Hook into the native 'clean' task triggered by Android Studio
tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
    
    // Custom paths to auto-clean
    delete("app/release")
}
