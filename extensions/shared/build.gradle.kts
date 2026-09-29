dependencies {
    implementation(project(":extensions:shared:library"))
    compileOnly(libs.okhttp)
}

android {
    defaultConfig {
        minSdk = 23
    }
}

// Bundle-specific resource path. ReVanced Manager loads every patch bundle through one
// class loader, so the default "extensions/shared.rve" would resolve to the official bundle's copy.
configure<app.revanced.patches.gradle.ExtensionExtension> {
    name = "extensions/instagram-revanced-patches/shared.rve"
}
