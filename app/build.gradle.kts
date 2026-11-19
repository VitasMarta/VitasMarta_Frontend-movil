import java.net.URI
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
}

val localConfiguration = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val gatewayUrl = providers.gradleProperty("api.gateway.url")
    .orElse(providers.environmentVariable("VITASMARTA_API_BASE_URL"))
    .orElse(localConfiguration.getProperty("api.gateway.url", ""))
    .get()
    .trim()
if (gatewayUrl.isNotEmpty()) {
    val uri = runCatching { URI(gatewayUrl) }.getOrNull()
    require(
        uri != null &&
            uri.scheme in setOf("http", "https") &&
            !uri.host.isNullOrBlank() &&
            uri.rawUserInfo == null &&
            uri.rawQuery == null &&
            uri.rawFragment == null &&
            (uri.rawPath.isNullOrEmpty() || uri.rawPath == "/")
    ) {
        "api.gateway.url debe ser el origen HTTP o HTTPS del Gateway, sin credenciales, prefijo, query ni fragmento"
    }
}

// The local origin drives both Retrofit and Android's host-specific cleartext policy.
val gatewayNetworkResources = layout.buildDirectory.dir("generated/gatewayNetwork/res")
val generateGatewayNetworkSecurity by tasks.registering {
    inputs.property("gatewayOrigin", gatewayUrl)
    outputs.dir(gatewayNetworkResources)
    doLast {
        val uri = gatewayUrl.takeIf { it.isNotEmpty() }?.let(::URI)
        val target = gatewayNetworkResources.get().file("xml/gateway_network_security.xml").asFile
        target.parentFile.mkdirs()
        target.writeText(
            buildString {
                appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
                appendLine("<network-security-config>")
                appendLine("    <base-config cleartextTrafficPermitted=\"false\" />")
                if (uri?.scheme == "http") {
                    val host = uri.host.removeSurrounding("[", "]").lowercase()
                    appendLine("    <domain-config cleartextTrafficPermitted=\"true\">")
                    appendLine("        <domain includeSubdomains=\"false\">$host</domain>")
                    appendLine("    </domain-config>")
                }
                appendLine("</network-security-config>")
            }
        )
    }
}

android {
    namespace = "co.vitasmarta.mobile"
    compileSdk = 36
    defaultConfig {
        applicationId = "co.vitasmarta.mobile"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_BASE_URL", "\"${gatewayUrl.trimEnd('/')}\"")
    }
    sourceSets.getByName("main").res.srcDir(gatewayNetworkResources)
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    bundle { language { enableSplit = false } }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    lint {
        abortOnError = true
        warningsAsErrors = true
        // Upgrade notices stay visible without breaking a reproducible, pinned build.
        informational +=
            setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
    }
}
tasks.named("preBuild").configure { dependsOn(generateGatewayNetworkSecurity) }
kapt { correctErrorTypes = true }
ktlint { version.set("1.5.0") }

val validateReleaseGateway by tasks.registering {
    doLast {
        require(gatewayUrl.isNotEmpty()) { "Release requiere api.gateway.url explícita" }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(validateReleaseGateway) }

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons)
    implementation(libs.compose.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.splashscreen)
    implementation(libs.lifecycle.compose)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation)
    kapt(libs.hilt.compiler)
    implementation(libs.coroutines.android)
    implementation(libs.serialization.json)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp.core)
    implementation(libs.datastore)
    implementation(libs.coil.compose)
    implementation(libs.coil.network)
    debugImplementation(libs.compose.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.okhttp.tls)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.espresso)
    debugImplementation(libs.compose.test.manifest)
}
