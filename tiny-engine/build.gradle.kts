plugins {
    // kotlin("jvm") version "1.8.0"
    id("com.github.minigdx.gradle.plugin.developer.mpp") version "DEV-SNAPSHOT"
    kotlin("plugin.serialization") version "1.8.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.danielgergely.com/releases/")
    }

}

dependencies {
    this.commonTestImplementation(kotlin("test"))
    // testImplementation(kotlin("test"))

    // Multiplatform
    this.commonMainImplementation("com.soywiz.korlibs.luak:luak:4.0.0-alpha-2")
    this.commonMainImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    this.commonMainImplementation("com.danielgergely.kgl:kgl:0.6.1")
    this.commonMainImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")


    // JVM Specific
    this.jvmMainImplementation("com.danielgergely.kgl:kgl-lwjgl:0.6.1")

    this.jvmMainImplementation("org.lwjgl:lwjgl-glfw:3.3.1")
    this.jvmMainImplementation("org.lwjgl:lwjgl-opengl:3.3.1")

    this.jvmMainImplementation("org.lwjgl:lwjgl:3.3.1:natives-windows")
    this.jvmMainImplementation("org.lwjgl:lwjgl:3.3.1:natives-linux")
    this.jvmMainImplementation("org.lwjgl:lwjgl:3.3.1:natives-macos")

    this.jvmMainImplementation("org.lwjgl:lwjgl-glfw:3.3.1")
    this.jvmMainImplementation("org.lwjgl:lwjgl-glfw:3.3.1:natives-windows")
    this.jvmMainImplementation("org.lwjgl:lwjgl-glfw:3.3.1:natives-linux")
    this.jvmMainImplementation("org.lwjgl:lwjgl-glfw:3.3.1:natives-macos")

    this.jvmMainImplementation("org.lwjgl:lwjgl-opengl:3.3.1")
    this.jvmMainImplementation("org.lwjgl:lwjgl-opengl:3.3.1:natives-windows")
    this.jvmMainImplementation("org.lwjgl:lwjgl-opengl:3.3.1:natives-linux")
    this.jvmMainImplementation("org.lwjgl:lwjgl-opengl:3.3.1:natives-macos")

    this.jvmMainImplementation("com.squareup:gifencoder:0.10.1")
}
