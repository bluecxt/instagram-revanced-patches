dependencies {
    compileOnly(project(":extensions:shared:library"))
}

android {
    defaultConfig {
        minSdk = 26
    }
}

// Bundle-specific resource path. ReVanced Manager loads every patch bundle through one
// class loader, so the default "extensions/instagram.rve" would resolve to the official bundle's copy.
configure<app.revanced.patches.gradle.ExtensionExtension> {
    name = "extensions/instagram-revanced-patches/instagram.rve"
}
