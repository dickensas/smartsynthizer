import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.multiplatform") version "1.5.20"
}

group = "in.co.dickens.music"
version = "1.3.0.0"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

var currentOS = org.gradle.internal.os.OperatingSystem.current()
var platform = "mingwx64"
if (currentOS.isWindows()) {
    platform = "mingwx64"
} else if (currentOS.isLinux()) {
    platform = "linuxx64"
} else if (currentOS.isMacOsX()) {
    platform = "macosx64"
}

kotlin {
    // Determine host preset.
    val hostOs = System.getProperty("os.name")

    // Create target for the host platform.
    val hostTarget = when {
        hostOs == "Mac OS X" -> macosX64("libgnuplot")
        hostOs == "Linux" -> linuxX64("libgnuplot")
        hostOs.startsWith("Windows") -> mingwX64("libgnuplot")
        else -> throw GradleException("Host OS '$hostOs' is not supported in Kotlin/Native $project.")
    }
    
    hostTarget.apply {
        binaries {
            executable {
                entryPoint = "plot.main"
            }
        }
        compilations["main"].cinterops {
            val mgl by creating {
                when (platform) {
                    "mingwx64" -> 
                       includeDirs {
                          allHeaders(
                             "${project.rootDir}/app",
                             "C:\\msys64\\mingw64\\include"
                          )
                       }
                    "linuxx64" ->
                       includeDirs {
                          allHeaders(
                             "${project.rootDir}/app",
                             "/usr/include",
                             "/usr/include/x86_64-linux-gnu"
                          )
                       }
                }
            }
        }
    }
    
    sourceSets {
        val libgnuplotMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1-native-mt")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-${platform}:1.5.1-native-mt")
            }
        }
    }
}