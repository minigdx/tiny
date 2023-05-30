@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.minigdx.mpp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.ksp)
}

dependencies {
    this.commonTestImplementation(kotlin("test"))

    // Multiplatform
    this.commonMainImplementation(libs.luak)
    this.commonMainImplementation(libs.kotlin.coroutines)
    this.commonMainImplementation(libs.kotlin.serialization.json)
    this.commonMainImplementation(libs.kgl.core)
    this.commonMainImplementation(project(":tiny-doc-annotations"))

    // JVM Specific
    this.jvmMainImplementation(libs.kgl.lwjgl)

    this.jvmMainImplementation(libs.lwjgl.core)
    this.jvmMainImplementation(variantOf(libs.lwjgl.core) { classifier("natives-windows") })
    this.jvmMainImplementation(variantOf(libs.lwjgl.core) { classifier("natives-linux") })
    this.jvmMainImplementation(variantOf(libs.lwjgl.core) { classifier("natives-macos") })

    this.jvmMainImplementation(libs.lwjgl.glfw)
    this.jvmMainImplementation(variantOf(libs.lwjgl.glfw) { classifier("natives-windows") })
    this.jvmMainImplementation(variantOf(libs.lwjgl.glfw) { classifier("natives-linux") })
    this.jvmMainImplementation(variantOf(libs.lwjgl.glfw) { classifier("natives-macos") })

    this.jvmMainImplementation(libs.lwjgl.opengl)
    this.jvmMainImplementation(variantOf(libs.lwjgl.opengl) { classifier("natives-windows") })
    this.jvmMainImplementation(variantOf(libs.lwjgl.opengl) { classifier("natives-linux") })
    this.jvmMainImplementation(variantOf(libs.lwjgl.opengl) { classifier("natives-macos") })

    this.jvmMainImplementation(libs.jvm.gifencoder)

    // See https://github.com/cagpie/PicoAudio.js
    jsMainImplementation(npm("picoaudio", "1.1.2"))?.because("get midi over web audio API.")

    add("kspJvm", project(":tiny-doc-generator")) {
        because("KSP will generate the asciidoctor documentation of all Lua libs from Tiny.")
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
