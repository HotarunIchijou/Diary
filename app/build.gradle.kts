plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
	alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "org.kaorun.diary"
	compileSdk = 35

	defaultConfig {
        applicationId = "org.kaorun.diary"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0-beta01"

		setProperty("archivesBaseName", "Diary-$versionName")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
			isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
		debug {
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
		}

		splits {
			abi {
				isEnable = true
				reset()
				include("armeabi-v7a", "arm64-v8a")
				isUniversalApk = true
			}
		}
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
	buildFeatures{
		viewBinding = true
	}
}

dependencies {
	implementation(libs.androidx.navigation.fragment)
	implementation(libs.androidx.navigation.ui)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
	implementation(libs.firebase.database)
	implementation(libs.firebase.auth)
	implementation(libs.androidx.annotation)
	implementation(libs.androidx.lifecycle.livedata.ktx)
	implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
	implementation(libs.android.rteditor)
}
