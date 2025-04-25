@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.minigdx.mpp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.mokkery)
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
    this.jvmMainImplementation(variantOf(libs.lwjgl.core) { classifier("natives-macos-arm64") })

    this.jvmMainImplementation(libs.lwjgl.glfw)
    this.jvmMainImplementation(variantOf(libs.lwjgl.glfw) { classifier("natives-windows") })
    this.jvmMainImplementation(variantOf(libs.lwjgl.glfw) { classifier("natives-linux") })
    this.jvmMainImplementation(variantOf(libs.lwjgl.glfw) { classifier("natives-macos") })
    this.jvmMainImplementation(variantOf(libs.lwjgl.glfw) { classifier("natives-macos-arm64") })

    this.jvmMainImplementation(libs.lwjgl.opengl)
    this.jvmMainImplementation(variantOf(libs.lwjgl.opengl) { classifier("natives-windows") })
    this.jvmMainImplementation(variantOf(libs.lwjgl.opengl) { classifier("natives-linux") })
    this.jvmMainImplementation(variantOf(libs.lwjgl.opengl) { classifier("natives-macos") })
    this.jvmMainImplementation(variantOf(libs.lwjgl.opengl) { classifier("natives-macos-arm64") })

    this.jvmMainImplementation(libs.jvm.gifencoder)

    jsMainImplementation("org.jetbrains.kotlin:kotlinx-atomicfu-runtime:2.1.20")
        ?.because("https://youtrack.jetbrains.com/issue/KT-57235")

    add("kspJvm", project(":tiny-doc-generator")) {
        because("KSP will generate the asciidoctor documentation of all Lua libs from Tiny.")
    }
}

// build.gradle.kts

project.tasks.register("tinyEngineJsZip", Zip::class.java) {
    from(tasks.getByName("jsBrowserDistribution"))
    this.destinationDirectory.set(project.layout.buildDirectory.dir("tiny-distributions"))
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

// -- Asciidoctor artifact configuration
configurations.create("tinyApiAsciidoctor") {
    isCanBeResolved = false
    isCanBeConsumed = true
}

artifacts {
    add("tinyApiAsciidoctor", project.layout.buildDirectory.dir("generated/ksp/jvm/jvmMain/resources/tiny-api.adoc")) {
        builtBy("kspKotlinJvm")
    }
}

// -- LUA Stub artifact configuration
configurations.create("tinyApiLuaStub") {
    isCanBeResolved = false
    isCanBeConsumed = true

    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class, "tiny-api-lua-stub"))
    }
}

artifacts {
    add("tinyApiLuaStub", project.layout.buildDirectory.dir("generated/ksp/jvm/jvmMain/resources/_tiny.stub.lua")) {
        builtBy("kspKotlinJvm")
    }
}
