plugins {
    kotlin("jvm") version "1.8.0"
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
    testImplementation(kotlin("test"))
    // Multiplatform
    implementation("com.soywiz.korlibs.luak:luak:4.0.0-alpha-2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("com.danielgergely.kgl:kgl:0.6.1")

    // JVM Specific
    implementation("com.danielgergely.kgl:kgl-lwjgl:0.6.1")

    implementation("org.lwjgl:lwjgl-glfw:3.3.1")
    implementation("org.lwjgl:lwjgl-opengl:3.3.1")

    this.implementation("org.lwjgl:lwjgl:3.3.1:natives-windows")
    this.implementation("org.lwjgl:lwjgl:3.3.1:natives-linux")
    this.implementation("org.lwjgl:lwjgl:3.3.1:natives-macos")

    this.implementation("org.lwjgl:lwjgl-glfw:3.3.1")
    this.implementation("org.lwjgl:lwjgl-glfw:3.3.1:natives-windows")
    this.implementation("org.lwjgl:lwjgl-glfw:3.3.1:natives-linux")
    this.implementation("org.lwjgl:lwjgl-glfw:3.3.1:natives-macos")

    this.implementation("org.lwjgl:lwjgl-opengl:3.3.1")
    this.implementation("org.lwjgl:lwjgl-opengl:3.3.1:natives-windows")
    this.implementation("org.lwjgl:lwjgl-opengl:3.3.1:natives-linux")
    this.implementation("org.lwjgl:lwjgl-opengl:3.3.1:natives-macos")

    implementation("org.l33tlabs.twl:pngdecoder:1.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}
