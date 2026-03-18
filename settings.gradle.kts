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

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }

    versionCatalogs {
        create("kotlinWrappers") {
            val wrappersVersion = "2025.10.4"
            from("org.jetbrains.kotlin-wrappers:kotlin-wrappers-catalog:$wrappersVersion")
        }
    }
}

plugins {
    id("com.gradle.develocity") version ("4.1")
}

include("tiny-cli")
include("tiny-doc")
include("tiny-doc-annotations")
include("tiny-annotation-processors:tiny-asciidoctor-dsl")
include("tiny-annotation-processors:tiny-lua-dsl")
include("tiny-annotation-processors:tiny-api-to-json-generator")
include("tiny-annotation-processors:tiny-lua-stub-generator")
include("tiny-engine")
include("tiny-samples:breakout")
include("tiny-samples:home")
include("tiny-web-editor")
include("tiny-debugger")

develocity {
    buildScan {
        termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
        termsOfUseAgree.set("yes")
        // Publish only if build from Github Action.
        publishing.onlyIf { System.getenv("CI") == "true" }
    }
}
