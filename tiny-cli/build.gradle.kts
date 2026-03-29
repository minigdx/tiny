
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.minigdx.jvm)
    alias(libs.plugins.kotlin.serialization)
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

    implementation(project(":tiny-doc-annotations"))
    implementation(project(":tiny-engine", "jvmRuntimeElements"))!!
        .because("Depends on the JVM Jar containing commons resources in the JAR.")
    implementation(project(":tiny-debugger", "jvmRuntimeElements"))!!
        .because("Depends on the debugger protocol classes and web UI.")

    implementation(libs.kgl.lwjgl)

    implementation(libs.bundles.jvm.ktor.server)

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

    add(
        externalDependencies.name,
        project(
            mapOf(
                "path" to ":tiny-debugger",
                "configuration" to "tinyDebugger",
            ),
        ),
    )?.because(
        "Embed the web debugger in the CLI " +
            "so it can be served by the run command debug server.",
    )
}

application {
    mainClass.set("com.github.minigdx.tiny.cli.MainKt")

    // Add the external dependencies (Tiny Web engine, ...) in the client.
    this.applicationDistribution.from(externalDependencies).into("lib")
}

// Update the start script to include jar from the Kotlin MPP dependencies
project.tasks.withType(CreateStartScripts::class.java).configureEach {
    setClasspath(
        tasks.named<Jar>("jar").map { it.outputs.files }.get()
            .plus(configurations.named("runtimeClasspath").get())
            .plus(externalDependencies),
    )

    (this.unixStartScriptGenerator as TemplateBasedScriptGenerator).template =
        resources.text.fromFile(project.layout.projectDirectory.file("unixCustomStartScript.txt"))
}

// Make the application plugin start with the right classpath
// See https://youtrack.jetbrains.com/issue/KT-50227/MPP-JVM-target-executable-application
project.tasks.withType(JavaExec::class.java).configureEach {
    val jar by tasks.existing
    val runtimeClasspath by configurations.existing

    classpath(jar, runtimeClasspath, externalDependencies)

    if (project.hasProperty("tiny.workDir")) {
        workingDir = rootProject.projectDir.resolve(project.property("tiny.workDir") as String)
    }
    if (System.getProperty("os.name").contains("Mac", ignoreCase = true)) {
        jvmArgs("-XstartOnFirstThread")
    }
}
