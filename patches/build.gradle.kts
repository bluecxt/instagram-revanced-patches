group = "app.revanced"

patches {
    about {
        name = "Instagram ReVanced Patches - bluecxt"
        description = "Dedicated ReVanced patches for Instagram"
        source = "https://github.com/bluecxt/instagram-revanced-patches"
        author = "bluecxt"
        contact = "https://github.com/bluecxt/instagram-revanced-patches"
        website = "https://github.com/bluecxt/instagram-revanced-patches"
        license = "GNU General Public License v3.0"
    }
}

dependencies {
    // Required due to smali, or build fails. Can be removed once smali is bumped.
    implementation(libs.guava)

    implementation(libs.apksig)

    // Android API stubs defined here.
    compileOnly(project(":patches:stub"))
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xexplicit-backing-fields",
            "-Xcontext-parameters"
        )
    }
}

publishing {
    repositories {
        maven {
            name = "githubPackages"
            url = uri("https://maven.pkg.github.com/bluecxt/instagram-revanced-patches")
            credentials {
                username = providers.gradleProperty("githubPackagesUsername").orNull ?: System.getenv("GITHUB_ACTOR") ?: "bluecxt"
                password = providers.gradleProperty("githubPackagesPassword").orNull ?: System.getenv("GITHUB_TOKEN") ?: ""
            }
        }
    }
}

apply(from = "strings-processing.gradle.kts")
