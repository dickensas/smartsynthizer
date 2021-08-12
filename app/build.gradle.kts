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

            val gtk4 by creating {
                when (platform) {
                    "mingwx64" -> 
                    includeDirs {
                        allHeaders(
                           "C:\\msys64\\mingw64\\lib\\glib-2.0\\include",
                           "C:\\msys64\\mingw64\\lib\\graphene-1.0\\include",
                           "C:\\msys64\\mingw64\\include\\atk-1.0",
                           "C:\\msys64\\mingw64\\include\\gdk-pixbuf-2.0",
                           "C:\\msys64\\mingw64\\include\\cairo",
                           "C:\\msys64\\mingw64\\include\\harfbuzz",
                           "C:\\msys64\\mingw64\\include\\pango-1.0",
                           "C:\\msys64\\mingw64\\include\\gtk-4.0",
                           "C:\\msys64\\mingw64\\include\\glib-2.0",
                           "C:\\msys64\\mingw64\\include\\graphene-1.0",
                           "C:\\msys64\\mingw64\\include\\librsvg-2.0",
                           "C:\\msys64\\mingw64\\include"
                        )
                    }
                    
                    "linuxx64" -> 
                    includeDirs {
                        allHeaders(
                           "/usr/lib/glib-2.0/include",
                           "/usr/lib/graphene-1.0/include",
                           "/usr/include/atk-1.0",
                           "/usr/include/gdk-pixbuf-2.0",
                           "/usr/include/cairo",
                           "/usr/include/harfbuzz",
                           "/usr/include/pango-1.0",
                           "/usr/include/gtk-4.0",
                           "/usr/include/glib-2.0",
                           "/usr/include/graphene-1.0",
                           "/usr/include/librsvg-2.0",
                           "/usr/include/x86_64-linux-gnu",
                           "/usr/include"
                        )
                    }
                }
            }

            val openal by creating {
                when (platform) {
                    "mingwx64" -> 
                       includeDirs {
                          allHeaders(
                             "C:\\msys64\\mingw64\\include"
                          )
                       }
                    "linuxx64" ->
                       includeDirs {
                          allHeaders(
                             "/usr/include/x86_64-linux-gnu",
                             "/usr/include"
                          )
                       }
                }
            }

            val mgl by creating {
                when (platform) {
                    "mingwx64" -> 
                       includeDirs {
                          allHeaders(
                             "${project.rootDir}/include",
                             "C:\\msys64\\mingw64\\include"
                          )
                       }
                    "linuxX64" ->
                       includeDirs {
                          allHeaders(
                             "${project.rootDir}/include",
                             "/usr/include/x86_64-linux-gnu",
                             "/usr/include"
                          )
                       }
                }
            }
            
            val rtmidi by creating {
                when (platform) {
                    "mingwx64" -> 
                       includeDirs {
                          allHeaders(
                             "C:\\msys64\\mingw64\\include"
                          )
                       }
                    "linuxx64" ->
                       includeDirs {
                          allHeaders(
                             "/usr/include/x86_64-linux-gnu",
                             "/usr/include"
                          )
                       }
                 }
            }
            
            val synth by creating {}
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