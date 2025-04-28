import org.gradle.api.JavaVersion.VERSION_17

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
	alias(libs.plugins.google.gms.google.services)
	alias(libs.plugins.kotlin.ksp)
}

android {
    namespace = "org.kaorun.diary"
	compileSdk = 36

	defaultConfig {
        applicationId = "org.kaorun.diary"
        minSdk = 31
        targetSdk = 36
        versionCode = 5
        versionName = "1.1.0"

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
    }

	android.sourceSets {
		getByName("main") {
			java.srcDirs("src/main/kotlin")
		}
	}

    compileOptions {
        sourceCompatibility = VERSION_17
        targetCompatibility = VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
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
	implementation(libs.androidx.transition.ktx)
	implementation(libs.androidx.fragment.ktx)
	implementation(libs.play.services.auth)
	implementation(libs.googleid)
	implementation(libs.androidx.credentials.play.services.auth)
	implementation(platform(libs.firebase.bom))
	implementation(libs.androidx.credentials)
	implementation(libs.androidx.preference.ktx)
	implementation(libs.androidx.room.runtime)
	ksp(libs.androidx.room.compiler)
}
