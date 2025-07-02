import java.io.Reader


@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.minigdx.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.ksp)
    application
}

val externalDependencies by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false

    attributes {
        attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "jar")
    }
}

dependencies {
    implementation(libs.kotlin.serialization.json)
    implementation(libs.clikt)

    // Exception in thread "main" java.lang.NoClassDefFoundError: com/sun/jna/Platform
    // https://mvnrepository.com/artifact/net.java.dev.jna/jna-platform
    implementation(libs.jna)
    implementation(libs.rsyntax)

    implementation(project(":tiny-engine", "jvmRuntimeElements"))!!
        .because("Depends on the JVM Jar containing commons resources in the JAR.")

    implementation(libs.kgl.lwjgl)

    implementation(libs.bundles.jvm.ktor.server)
    implementation(libs.bundles.jvm.ktor.client)

    add(
        externalDependencies.name,
        project(
            mapOf(
                "path" to ":tiny-engine",
                "configuration" to "tinyWebEngine",
            ),
        ),
    )?.because(
        "Embed the JS engine in the CLI " +
            "so it can be included when the game is exported.",
    )

    add("ksp", project(":tiny-annotation-processors:tiny-cli-to-asciidoc-generator")) {
        because("KSP will generate the asciidoctor documentation of all Lua libs from Tiny.")
    }
}

application {
    mainClass.set("com.github.minigdx.tiny.cli.MainKt")

    // Add the external dependencies (Tiny Web engine, ...) in the client.
    this.applicationDistribution.from(externalDependencies).into("lib")
}

// Update the start script to include jar from the Kotlin MPP dependencies
project.tasks.withType(CreateStartScripts::class.java).configureEach {
    this.classpath =
        project.tasks.getByName("jar").outputs.files
            .plus(project.configurations.getByName("runtimeClasspath"))
            .plus(externalDependencies)

    (this.unixStartScriptGenerator as TemplateBasedScriptGenerator).template =
        object : TextResource {
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
    val jar by tasks.existing
    val runtimeClasspath by configurations.existing

    classpath(jar, runtimeClasspath, externalDependencies)
}
