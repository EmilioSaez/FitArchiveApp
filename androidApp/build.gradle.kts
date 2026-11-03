import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

// Firma de release: nunca se guardan secretos en el repo. Se leen de variables de entorno
// (FITARCHIVE_KEYSTORE_FILE, FITARCHIVE_KEYSTORE_PASSWORD, FITARCHIVE_KEY_ALIAS, FITARCHIVE_KEY_PASSWORD)
// o de `keystore.properties` en la raíz (ignorado por git; ver keystore.properties.example).
// Sin ninguna de las dos, el release se genera sin firmar (válido para comprobar que compila).
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun signingValue(envName: String, propName: String): String? =
    providers.environmentVariable(envName).orNull?.takeIf { it.isNotBlank() }
        ?: keystoreProps.getProperty(propName)?.takeIf { it.isNotBlank() }

val releaseStoreFile = signingValue("FITARCHIVE_KEYSTORE_FILE", "storeFile")
val releaseStorePassword = signingValue("FITARCHIVE_KEYSTORE_PASSWORD", "storePassword")
val releaseKeyAlias = signingValue("FITARCHIVE_KEY_ALIAS", "keyAlias")
val releaseKeyPassword = signingValue("FITARCHIVE_KEY_PASSWORD", "keyPassword")
val hasReleaseSigning = listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword)
    .all { it != null }

android {
    namespace = "app.luxion.fitarchiveapp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "app.luxion.fitarchiveapp"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        // Play exige un versionCode mayor en cada subida. Se puede forzar con -PversionCode=N.
        versionCode = (findProperty("versionCode") as String?)?.toInt() ?: 1
        versionName = "1.0.0"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(projects.shared)
    implementation(libs.androidx.activity.compose)
}
