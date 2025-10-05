@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.minigdx.mpp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.mokkery)
}

dependencies {
    this.commonTestImplementation(kotlin("test"))
    this.commonTestImplementation(libs.kotlin.coroutines.test)

    // Multiplatform
    this.commonMainApi(libs.kgl.core)
    this.commonMainImplementation(libs.luak)
    this.commonMainImplementation(libs.kotlin.coroutines)
    this.commonMainImplementation(libs.kotlin.serialization.json)
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

    jsMainImplementation("org.jetbrains.kotlin:kotlinx-atomicfu-runtime:2.1.20")?.because("https://youtrack.jetbrains.com/issue/KT-57235")
    jsMainImplementation("org.jetbrains.kotlin-wrappers:kotlin-browser:2025.6.9")
        ?.because("required to access AudioWorkletNode")

    add("kspJvm", project(":tiny-annotation-processors:tiny-lua-stub-generator")) {
        because("KSP will generate all Lua stub methods from all Lua libs from Tiny.")
    }

    add("kspJvm", project(":tiny-annotation-processors:tiny-api-to-asciidoc-generator")) {
        because("KSP will generate the asciidoctor documentation of all Lua libs from Tiny.")
    }
}

// Create the tiny engine javascript version as a Zip file
val tinyEngineJsJar =
    project.tasks.register(
        "tinyEngineJsJar",
        Jar::class.java,
    ) {
        from(tasks.getByName("jsBrowserDistribution"))
        this.into("tiny-engine-js")
        this.destinationDirectory.set(project.layout.buildDirectory.dir("tiny-distributions"))

        group = "tiny"
        description = "Build a jar containing all resources to run the Tiny engine in a web application."
    }

val tinyResourcesZip =
    project.tasks.register(
        "tinyResourcesZip",
        Zip::class.java,
    ) {
        from(tasks.getByName("jvmProcessResources"))
        this.into("")
        this.destinationDirectory.set(project.layout.buildDirectory.dir("tiny-distributions"))
        this.archiveBaseName.set("tiny-resources")

        group = "tiny"
        description = "Build a zip containing resources from the Tiny Engine (_boot.lua, ...)."
    }

// Create the configuration that will contain the tiny engine javascript as artifact
configurations.create("tinyWebEngine") {
    isCanBeResolved = false
    isCanBeConsumed = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class, "tiny-engine-js-browser-distribution"))
        attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "jar")
    }
    outgoing.artifact(tinyEngineJsJar)
}

// -- Asciidoctor artifact configuration
configurations.create("tinyApiAsciidoctor") {
    isCanBeResolved = false
    isCanBeConsumed = true
}

// -- LUA Stub artifact configuration
configurations.create("tinyApiLuaStub") {
    isCanBeResolved = false
    isCanBeConsumed = true

    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class, "tiny-api-lua-stub"))
    }
}

// -- resources file (_boot.lua, ...)
configurations.create("tinyResources") {
    isCanBeResolved = false
    isCanBeConsumed = true

    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class, "tiny-resources"))
        attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "zip")
    }
}

artifacts {
    // Tiny Engine Javascript version.
    add("tinyWebEngine", tinyEngineJsJar) {
        builtBy(tinyEngineJsJar)
    }
    // API as Asciidoctor.
    add("tinyApiAsciidoctor", project.layout.buildDirectory.dir("generated/ksp/jvm/jvmMain/resources/tiny-api.adoc")) {
        builtBy("kspKotlinJvm")
    }
    // API as Lua stub.
    add("tinyApiLuaStub", project.layout.buildDirectory.dir("generated/ksp/jvm/jvmMain/resources/_tiny.stub.lua")) {
        builtBy("kspKotlinJvm")
    }
    // Tiny Resources
    add("tinyResources", tinyResourcesZip) {
        builtBy(tinyResourcesZip)
    }
}
