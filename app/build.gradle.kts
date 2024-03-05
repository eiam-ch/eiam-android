Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
	id("com.android.application")
	id("kotlin-android")
}

android {
	namespace = "ch.ubique.eiam"
	compileSdk = 34

	defaultConfig {
		minSdk = 26
		targetSdk = 34

		applicationId = "ch.ubique.eiam"
		versionName = "1.0.0"
		versionCode = 1_00_00_00

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}
	flavorDimensions += listOf("eiam")
// compile with Java 17 compatibility
	android.compileOptions {
		compileOptions.sourceCompatibility = JavaVersion.VERSION_17
		compileOptions.targetCompatibility = JavaVersion.VERSION_17
	}


	productFlavors {
		defaultConfig {
			buildConfigField("String", "IDP_REF_DISCOVERY_URL", "\"https://identity-eiam-r.eiam.admin.ch/realms/bund_bk-picardapp/\"")
			buildConfigField("String", "IDP_REF_CLIENT_ID", "\"BK-picardapp\"")
			buildConfigField("String", "IDP_REF_REDIRECT_URL", "\"eiam://redirect/\"")
			buildConfigField("String", "DEMO_REF_BACKEND", "\"https://eiam-demo-dev.ubique.ch/demo/\"")

			buildConfigField("String", "IDP_ABN_DISCOVERY_URL", "\"https://identity-eiam-a.eiam.admin.ch/realms/bund_bk-picardapp/\"")
			buildConfigField("String", "IDP_ABN_CLIENT_ID", "\"BK-picardapp\"")
			buildConfigField("String", "IDP_ABN_REDIRECT_URL", "\"eiam://redirect/\"")
			buildConfigField("String", "DEMO_ABN_BACKEND", "\"https://eiam-demo-abn.ubique.ch/demo/\"")

			buildConfigField("String", "IDP_PROD_DISCOVERY_URL", "\"https://identity-eiam.eiam.admin.ch/realms/bund_bk-picardapp/\"")
			buildConfigField("String", "IDP_PROD_CLIENT_ID", "\"BK-picardapp\"")
			buildConfigField("String", "IDP_PROD_REDIRECT_URL", "\"eiam://redirect/\"")
			buildConfigField("String", "DEMO_PROD_BACKEND", "\"https://eiam-demo-prod.ubique.ch/demo/\"")
			manifestPlaceholders.put("appAuthRedirectScheme", "eiam")

		}
		create("dev") {
			dimension = "eiam"
			applicationIdSuffix =".dev"
		}
		create("prod") {
			dimension = "eiam"
		}
	}

	buildTypes {
		release {
			isMinifyEnabled = true
			proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
			signingConfig = signingConfigs.getByName("debug")
		}
	}

	buildFeatures {
		viewBinding = true
		compose = true
		buildConfig = true
	}

	composeOptions {
		kotlinCompilerExtensionVersion = "1.5.4"
	}
}

dependencies {
	// AndroidX
	implementation("androidx.appcompat:appcompat:1.6.1")
	implementation("androidx.fragment:fragment-ktx:1.6.2")
	implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
	implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

	// KotlinX
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

	// Compose
	implementation(platform("androidx.compose:compose-bom:2023.10.01")) // https://developer.android.com/jetpack/compose/setup#using-the-bom
	implementation("androidx.compose.ui:ui")
	implementation("androidx.compose.ui:ui-tooling")
	implementation("androidx.compose.ui:ui-tooling-preview")
	implementation("androidx.compose.foundation:foundation")
	implementation("androidx.compose.material3:material3:1.2.0")
	implementation("androidx.activity:activity-compose:1.8.2")

	// Networking
	implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
	implementation("com.squareup.retrofit2:retrofit:2.9.0")
	implementation("com.squareup.retrofit2:converter-scalars:2.9.0")

	// Authentication
	implementation("com.auth0.android:jwtdecode:2.0.2")
	implementation("net.openid:appauth:0.11.1")


	// DataStore
	implementation("androidx.datastore:datastore:1.0.0")
	implementation("androidx.datastore:datastore-preferences:1.0.0")
	implementation("com.google.protobuf:protobuf-javalite:3.19.4")
	implementation("androidx.security:security-crypto-ktx:1.1.0-alpha06")

	// Test
	testImplementation("androidx.test.ext:junit-ktx:1.1.5")
	androidTestImplementation("androidx.test.ext:junit-ktx:1.1.5")

	//Youtube
	implementation("com.pierfrancescosoffritti.androidyoutubeplayer:core:11.1.0")
}
