plugins {
    id("com.github.minigdx.gradle.plugin.developer.jvm") version "DEV-SNAPSHOT"
    kotlin("kapt") version("1.8.0")
    kotlin("plugin.serialization") version "1.8.0"
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.danielgergely.com/releases/")
    }

}

dependencies {
    implementation("info.picocli:picocli:4.7.1")
    implementation(project(":tiny-engine"))
    implementation("com.danielgergely.kgl:kgl-lwjgl:0.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")

    kapt("info.picocli:picocli-codegen:4.7.1")

}

kapt {
    arguments {
        arg("project", "${project.group}/${project.name}")
    }
}
