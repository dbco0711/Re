plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.practice_pleos"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.practice_pleos"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    implementation("ai.pleos.playground:FusedLocation:1.1.3") //FusedLocatino을 추가하기 위한
    implementation("ai.pleos.playground:SpeechToText:3.3.1") //stt 사용하기 위한 추가
    implementation("ai.pleos.playground:NaviHelper:3.0.5") // navi를 사용하기 위해 추가

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}