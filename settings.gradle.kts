rootProject.name = "tiny"

pluginManagement {
    repositories {
        maven {
            url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        }.mavenContent {
            includeVersionByRegex("com.github.minigdx", "(.*)", "LATEST-SNAPSHOT")
        }
        gradlePluginPortal()
        google()
        mavenCentral()
        mavenLocal()
    }
}

plugins {
    id("com.gradle.develocity") version ("4.0")
}

include("tiny-cli")
include("tiny-doc")
include("tiny-doc-annotations")
include("tiny-doc-generator")
include("tiny-engine")
include("tiny-sample")
include("tiny-web-editor")

develocity {
    buildScan {
        termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
        termsOfUseAgree.set("yes")
        // Publish only if build from Github Action.
        publishing.onlyIf { System.getenv("CI") == "true" }
    }
}
