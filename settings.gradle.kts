
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

include("tiny-cli")
include("tiny-doc")
include("tiny-doc-annotations")
include("tiny-doc-generator")
include("tiny-engine")
include("tiny-sample")
