import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.io.FileOutputStream

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

    defaultConfig {
        applicationId = "com.Michael.Oikonomia"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

googleServices {
  missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN
}


// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  implementation(libs.firebase.appcheck.recaptcha)
  
  // Firebase client dependencies managed by BoM
  implementation("com.google.firebase:firebase-auth")
  implementation("com.google.firebase:firebase-firestore")
  implementation("com.google.firebase:firebase-config")
  implementation("com.google.firebase:firebase-analytics")
  implementation("com.google.firebase:firebase-crashlytics")
  
  // Android Credential Manager and Google ID
  implementation("androidx.credentials:credentials:1.2.2")
  implementation("androidx.credentials:credentials-play-services-auth:1.2.2")
  implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
  
  // WorkManager for background tasks and notifications
  implementation("androidx.work:work-runtime-ktx:2.9.0")
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

abstract class DownloadCairoFontsTask : DefaultTask() {
    @get:OutputDirectory
    abstract val fontDirectory: DirectoryProperty

    @TaskAction
    fun download() {
        val fontDir = fontDirectory.get().asFile
        if (!fontDir.exists()) {
            fontDir.mkdirs()
        }
        val fonts = mapOf(
            "cairo_font.ttf" to "https://raw.githubusercontent.com/google/fonts/main/ofl/cairo/Cairo%5Bslnt%2Cwght%5D.ttf"
        )
        fonts.forEach { (fileName, url) ->
            val targetFile = File(fontDir, fileName)
            if (!targetFile.exists()) {
                println("Downloading $fileName...")
                val process = Runtime.getRuntime().exec(arrayOf("curl", "-g", "-L", "-o", targetFile.absolutePath, url))
                process.waitFor()
            }
        }
    }
}

abstract class DownloadNotificationSoundTask : DefaultTask() {
    @get:OutputDirectory
    abstract val rawDirectory: DirectoryProperty

    @TaskAction
    fun download() {
        val rawDir = rawDirectory.get().asFile
        if (!rawDir.exists()) {
            rawDir.mkdirs()
        }
        val targetFile = File(rawDir, "custom_notification.wav")
        if (!targetFile.exists()) {
            println("Generating professional custom_notification.wav...")
            val sampleRate = 44100
            val durationSeconds = 0.65
            val numSamples = (sampleRate * durationSeconds).toInt()
            val samples = ShortArray(numSamples)
            
            // Note 1: A5 (880 Hz), starts at t = 0.0s
            val freq1_1 = 880.0
            val freq1_2 = freq1_1 * 2.0
            val freq1_3 = freq1_1 * 3.0
            
            // Note 2: E6 (1318.51 Hz), starts at t = 0.12s
            val note2Start = 0.12
            val freq2_1 = 1318.51
            val freq2_2 = freq2_1 * 2.0
            val freq2_3 = freq2_1 * 3.0
            
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                var signal = 0.0
                
                // First note
                if (t >= 0.0) {
                    val u = t - 0.0
                    val envelope = if (u < 0.015) {
                        u / 0.015 // smooth attack
                    } else {
                        Math.exp(-10.0 * (u - 0.015)) // exponential decay
                    }
                    val tone = Math.sin(2.0 * Math.PI * freq1_1 * u) + 
                               0.25 * Math.sin(2.0 * Math.PI * freq1_2 * u) + 
                               0.1 * Math.sin(2.0 * Math.PI * freq1_3 * u)
                    signal += 0.4 * tone * envelope
                }
                
                // Second note
                if (t >= note2Start) {
                    val u = t - note2Start
                    val envelope = if (u < 0.015) {
                        u / 0.015 // smooth attack
                    } else {
                        Math.exp(-7.0 * (u - 0.015)) // exponential decay (longer sustain)
                    }
                    val tone = Math.sin(2.0 * Math.PI * freq2_1 * u) + 
                               0.25 * Math.sin(2.0 * Math.PI * freq2_2 * u) + 
                               0.1 * Math.sin(2.0 * Math.PI * freq2_3 * u)
                    signal += 0.45 * tone * envelope
                }
                
                // Hard limiting to avoid clipping
                if (signal > 1.0) signal = 1.0
                if (signal < -1.0) signal = -1.0
                
                samples[i] = (signal * 32767.0).toInt().toShort()
            }
            
            val totalAudioLen = samples.size * 2
            val totalDataLen = totalAudioLen + 36
            val longSampleRate = sampleRate.toLong()
            val channels = 1
            val byteRate = (sampleRate * channels * 2).toLong()
            
            val header = ByteArray(44)
            header[0] = 'R'.code.toByte()
            header[1] = 'I'.code.toByte()
            header[2] = 'F'.code.toByte()
            header[3] = 'F'.code.toByte()
            header[4] = (totalDataLen and 0xff).toByte()
            header[5] = ((totalDataLen shr 8) and 0xff).toByte()
            header[6] = ((totalDataLen shr 16) and 0xff).toByte()
            header[7] = ((totalDataLen shr 24) and 0xff).toByte()
            header[8] = 'W'.code.toByte()
            header[9] = 'A'.code.toByte()
            header[10] = 'V'.code.toByte()
            header[11] = 'E'.code.toByte()
            header[12] = 'f'.code.toByte()
            header[13] = 'm'.code.toByte()
            header[14] = 't'.code.toByte()
            header[15] = ' '.code.toByte()
            header[16] = 16
            header[17] = 0
            header[18] = 0
            header[19] = 0
            header[20] = 1
            header[21] = 0
            header[22] = channels.toByte()
            header[23] = 0
            header[24] = (longSampleRate and 0xff).toByte()
            header[25] = ((longSampleRate shr 8) and 0xff).toByte()
            header[26] = ((longSampleRate shr 16) and 0xff).toByte()
            header[27] = ((longSampleRate shr 24) and 0xff).toByte()
            header[28] = (byteRate and 0xff).toByte()
            header[29] = ((byteRate shr 8) and 0xff).toByte()
            header[30] = ((byteRate shr 16) and 0xff).toByte()
            header[31] = ((byteRate shr 24) and 0xff).toByte()
            header[32] = 2
            header[33] = 0
            header[34] = 16
            header[35] = 0
            header[36] = 'd'.code.toByte()
            header[37] = 'a'.code.toByte()
            header[38] = 't'.code.toByte()
            header[39] = 'a'.code.toByte()
            header[40] = (totalAudioLen and 0xff).toByte()
            header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
            header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
            header[43] = ((totalAudioLen shr 24) and 0xff).toByte()
            
            FileOutputStream(targetFile).use { out ->
                out.write(header)
                val buffer = ByteArray(samples.size * 2)
                for (j in samples.indices) {
                    val s = samples[j].toInt()
                    buffer[j * 2] = (s and 0xff).toByte()
                    buffer[j * 2 + 1] = ((s shr 8) and 0xff).toByte()
                }
                out.write(buffer)
            }
        }
    }
}

val downloadCairoFonts = tasks.register<DownloadCairoFontsTask>("downloadCairoFonts") {
    fontDirectory.set(layout.projectDirectory.dir("src/main/res/font"))
}

val downloadNotificationSound = tasks.register<DownloadNotificationSoundTask>("downloadNotificationSound") {
    rawDirectory.set(layout.projectDirectory.dir("src/main/res/raw"))
}

tasks.named("preBuild") {
    dependsOn(downloadCairoFonts, downloadNotificationSound)
}
