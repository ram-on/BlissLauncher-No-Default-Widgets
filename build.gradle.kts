buildscript {
    repositories {
        google()
        jcenter()
        maven {
            url = uri("https://maven.fabric.io/public")
        }
    }
    dependencies {
        classpath(foundation.e.blisslauncher.buildsrc.Libs.androidGradlePlugin)
        classpath(foundation.e.blisslauncher.buildsrc.Libs.Kotlin.gradlePlugin)
        classpath(foundation.e.blisslauncher.buildsrc.Libs.Kotlin.extensions)
        classpath(foundation.e.blisslauncher.buildsrc.Libs.dexcountGradlePlugin)
    }
}

plugins {
    id("com.diffplug.gradle.spotless") version "3.14.0"
    id("com.github.ben-manes.versions") version "0.25.0"
}

allprojects {
    repositories {
        google()
        jcenter()
        maven { url = uri("https://jitpack.io") }
        mavenCentral()
    }
}

subprojects {
    apply(plugin = ("com.diffplug.gradle.spotless"))
    spotless {
        java {
            target("**/*.java")
            removeUnusedImports() // removes any unused imports
        }
        kotlin {
            target("**/*.kt")
            ktlint()
        }
        kotlinGradle {
            // same as kotlin, but for .gradle.kts files (defaults to '*.gradle.kts')
            target("*.gradle.kts", "additionalScripts/*.gradle.kts")
            ktlint()
        }
    }
}
