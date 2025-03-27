
import java.io.Reader

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.minigdx.mpp)
    alias(libs.plugins.kotlin.serialization)
    application
}

val tinyEngineJsZip by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class, "tiny-engine-js-browser-distribution"))
    }
}

val tinyApiluaStub by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    commonMainImplementation(libs.kotlin.serialization.json)
    commonMainImplementation(libs.clikt)

    // Exception in thread "main" java.lang.NoClassDefFoundError: com/sun/jna/Platform
    // https://mvnrepository.com/artifact/net.java.dev.jna/jna-platform
    jvmMainImplementation("net.java.dev.jna:jna-platform:5.14.0")
    jvmMainImplementation("com.fifesoft:rsyntaxtextarea:3.6.0")

    jvmMainImplementation(project(":tiny-engine", "jvmRuntimeElements"))!!
        .because("Depends on the JVM Jar containing commons resources in the JAR.")

    jvmMainImplementation(libs.kgl.lwjgl)

    jvmMainImplementation(libs.bundles.jvm.ktor.server)
    jvmMainImplementation(libs.bundles.jvm.ktor.client)

    add(tinyEngineJsZip.name, project(":tiny-engine"))?.because(
        "Embed the JS engine in the CLI " +
            "so it can be included when the game is exported.",
    )

    add(
        tinyApiluaStub.name,
        project(
            mapOf(
                "path" to ":tiny-engine",
                "configuration" to "tinyApiLuaStub",
            ),
        ),
    )?.because(
        "Embed the Lua API stub so it can be included when a new game is created.",
    )
}

application {
    mainClass.set("com.github.minigdx.tiny.cli.MainKt")

    // Copy the JARs from the Kotlin MPP dependencies.
    this.applicationDistribution.from(
        project.configurations.getByName("jvmRuntimeClasspath"),
    ) {
        val jvmJar by tasks.existing
        this.from(jvmJar)
        this.from(tinyEngineJsZip)
        this.from(tinyApiluaStub)
        this.into("lib")
    }
}

// Update the start script to include jar from the Kotlin MPP dependencies
project.tasks.withType(CreateStartScripts::class.java).configureEach {
    this.classpath = project.tasks.getByName("jvmJar").outputs.files
        .plus(project.configurations.getByName("jvmRuntimeClasspath"))
        .plus(tinyEngineJsZip)
        .plus(tinyApiluaStub)

    (this.unixStartScriptGenerator as TemplateBasedScriptGenerator).template = object : TextResource {
        override fun getBuildDependencies(): TaskDependency = TODO("Not yet implemented")

        override fun asString(): String = TODO("Not yet implemented")

        override fun asReader(): Reader {
            return project.file("unixCustomStartScript.txt").reader()
        }

        override fun asFile(charset: String): File = TODO("Not yet implemented")

        override fun asFile(): File = TODO("Not yet implemented")

        override fun getInputProperties(): Any? = TODO("Not yet implemented")

        override fun getInputFiles(): FileCollection? = TODO("Not yet implemented")
    }
}

// Make the application plugin start with the right classpath
// See https://youtrack.jetbrains.com/issue/KT-50227/MPP-JVM-target-executable-application
project.tasks.withType(JavaExec::class.java).configureEach {
    val jvmJar by tasks.existing
    val jvmRuntimeClasspath by configurations.existing
    val tinyEngineJsZip by configurations.existing
    val tinyApiLuaStub by configurations.existing

    classpath(jvmJar, jvmRuntimeClasspath, tinyEngineJsZip, tinyApiLuaStub)
}
