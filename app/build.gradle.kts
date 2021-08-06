import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.multiplatform") version "1.5.20"
}

group = "in.co.dickens.music"
version = "1.0.0.0"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

kotlin {
    mingwX64("libgnuplot") {
        binaries {
            executable {
                entryPoint = "plot.main"
            }
        }
        compilations["main"].cinterops {

            val gtk4 by creating {
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
            }

            val openal by creating {
               includeDirs {
                  allHeaders(
                     "C:\\msys64\\mingw64\\include"
                  )
               }
            }

            val mgl by creating {
               includeDirs {
                  allHeaders(
                     "${project.rootDir}/include",
                     "C:\\msys64\\mingw64\\include"
                  )
               }
            }
            
            val synth by creating {}
        }
    }
    
    sourceSets {
        val libgnuplotMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1-native-mt")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-mingwx64:1.5.1-native-mt")
            }
        }
    }
}