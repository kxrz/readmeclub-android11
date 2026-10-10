import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "club.readme.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "club.readme.android"
        minSdk = 30
        targetSdk = 30 // S4 runs Android 11; sideloaded, so no Play Store targetSdk floor
        versionCode = 16
        versionName = "2.0.0"
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
        }
        // Release key lives outside the repo, injected by CI from secrets (see .github/workflows/android.yml).
        System.getenv("RELEASE_KEYSTORE")?.let { path ->
            create("release") {
                storeFile = file(path)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // Separate app on the device, so dev builds and the distributed release coexist.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    // Kotlin metadata only serves reflection, which the app never uses: kept out of the APK budget.
    packaging {
        resources {
            excludes += listOf("kotlin/**", "META-INF/*.kotlin_module", "META-INF/*.version", "DebugProbesKt.bin")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        // targetSdk 30 is deliberate (see above).
        disable += "ExpiredTargetSdkVersion"
    }
}

// CHANGELOG.md ships in the APK, so the installed version's release notes read offline.
abstract class ChangelogAsset : DefaultTask() {
    @get:InputFile abstract val changelog: RegularFileProperty
    @get:OutputDirectory abstract val outputDir: DirectoryProperty

    @TaskAction
    fun copy() {
        changelog.get().asFile.copyTo(outputDir.get().file("CHANGELOG.md").asFile, overwrite = true)
    }
}

val changelogAsset = tasks.register<ChangelogAsset>("changelogAsset") {
    changelog.set(rootProject.layout.projectDirectory.file("CHANGELOG.md"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(changelogAsset, ChangelogAsset::outputDir)
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    // Unit tests run on the JVM, where Android's org.json is only a stub (test classpath only, not in the APK).
    testImplementation("org.json:json:20240303")
}
