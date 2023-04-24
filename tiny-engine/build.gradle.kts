plugins {
    id("com.github.minigdx.gradle.plugin.developer.mpp")
    kotlin("plugin.serialization") version "1.8.0"

    id("com.google.devtools.ksp") version "1.8.20-1.0.10"
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.danielgergely.com/releases/")
    }
}

dependencies {
    this.commonTestImplementation(kotlin("test"))

    // Multiplatform
    this.commonMainImplementation("com.soywiz.korlibs.luak:luak:4.0.0-alpha-2")
    this.commonMainImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    this.commonMainImplementation("com.danielgergely.kgl:kgl:0.6.1")
    this.commonMainImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    this.commonMainImplementation(project(":tiny-doc-annotations"))

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

    jsMainImplementation(npm("prismjs", "1.23.0"))?.because(
        "tiny-web-editor require it. " +
            "KotlinJS doesn't support well two KotlinJS application with different dependencies."
    )

    // See https://github.com/cagpie/PicoAudio.js
    jsMainImplementation(npm("picoaudio", "1.1.2"))?.because("get midi over web audio API.")

    add("kspJvm", project(":tiny-doc-generator")) {
        because("KSP will generate the asciidoctor documentation of all LUA libs from Tiny.")
    }
}

project.tasks.register("tinyEngineJsZip", Zip::class.java) {
    from(tasks.getByName("jsBrowserDistribution"))
    this.destinationDirectory.set(project.buildDir.resolve("tiny-distributions"))
    this.into("tiny-engine-js")
    group = "tiny"
    description = "Build a zip containing all resources to run the Tiny engine in a web application."
}

configurations.create("tinyWebEngine") {
    isCanBeResolved = false
    isCanBeConsumed = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class, "tiny-engine-js-browser-distribution"))
    }
    outgoing.artifact(tasks.getByName("tinyEngineJsZip"))
}

configurations.create("tinyApiAsciidoctor") {
    isCanBeResolved = false
    isCanBeConsumed = true
}

artifacts {
    add("tinyApiAsciidoctor", project.buildDir.resolve("generated/ksp/jvm/jvmMain/resources/tiny-api.adoc")) {
        builtBy("kspKotlinJvm")
    }
}
