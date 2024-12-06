plugins {
    id("com.android.application") version "8.7.1" apply false
    id("com.android.library") version "8.7.1" apply false
    id("org.jetbrains.kotlin.android") version "2.0.10" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.10" apply false
    id("com.google.dagger.hilt.android") version "2.44" apply false
    id("com.mikepenz.aboutlibraries.plugin") version "11.3.0-rc02" apply false
    id("com.google.devtools.ksp") version "2.0.10-1.0.24" apply false
    id("org.jmailen.kotlinter") version "4.0.0" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}