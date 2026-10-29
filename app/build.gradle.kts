plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
    id("androidx.room")
}

android {
    namespace = "ch.joshuah.bibleverseapp"
    compileSdk = 37

    defaultConfig {
        applicationId = "ch.joshuah.bibleverseapp"
        minSdk = 26
        targetSdk = 37
        versionCode = 14
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val apiKey = System.getenv("YOUVERSION_API_KEY")
            ?: (project.findProperty("YOUVERSION_API_KEY") as String?)
            ?: "6BGY5riHwZAR5PppAsrshxOyx3xjtwBRyzJNB7nj98MeuFQ6"
        buildConfigField("String", "YOUVERSION_API_KEY", "\"$apiKey\"")
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.all { test ->
            (test as org.gradle.api.tasks.testing.Test).jvmArgs("-Dnet.bytebuddy.experimental=true")
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_FILE") ?: project.findProperty("KEYSTORE_FILE") ?: "release.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: project.findProperty("KEYSTORE_PASSWORD") as String?
            keyAlias = System.getenv("KEY_ALIAS") ?: project.findProperty("KEY_ALIAS") as String?
            keyPassword = System.getenv("KEY_PASSWORD") ?: project.findProperty("KEY_PASSWORD") as String?
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    room {
        schemaDirectory("$projectDir/schemas")
    }
}

dependencies {
    val room_version = "2.8.4"

    implementation(platform("com.youversion.platform:platform-bom:2.7.0"))
    implementation("com.youversion.platform:platform-core")

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.4.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.4.0")
    implementation("androidx.fragment:fragment-ktx:1.6.1")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("androidx.room:room-ktx:$room_version")
    implementation("com.jaredrummler:colorpicker:1.1.0")
    implementation("io.coil-kt:coil:2.7.0")

    ksp("androidx.room:room-compiler:$room_version")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

tasks.withType<Test>().configureEach {
    jvmArgs("-Dnet.bytebuddy.experimental=true")
}
