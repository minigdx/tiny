import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.minigdx.mpp)
    alias(libs.plugins.kotlin.serialization)
    application
}

configurations.create("tinyEngineJsZip") {
    isCanBeResolved = true
    isCanBeConsumed = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class, "tiny-engine-js-browser-distribution"))
    }
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

    add("tinyEngineJsZip", project(":tiny-engine"))?.because(
        "Embed the JS engine in the CLI " +
            "so it can be included when the game is exported.",
    )
}

application {
    mainClass.set("com.github.minigdx.tiny.cli.MainKt")
    applicationDefaultJvmArgs = if (System.getProperty("os.name").toLowerCaseAsciiOnly().contains("mac")) {
        listOf("-XstartOnFirstThread")
    } else {
        emptyList()
    }

    // Copy the JARs from the Kotlin MPP dependencies.
    this.applicationDistribution.from(
        project.configurations.getByName("jvmRuntimeClasspath"),
    ) {
        val jvmJar by tasks.existing
        this.from(jvmJar)
        this.from(project.configurations.getByName("tinyEngineJsZip"))
        this.into("lib")
    }
}

// Update the start script to include jar from the Kotlin MPP dependencies
project.tasks.withType(CreateStartScripts::class.java).configureEach {
    this.classpath = project.tasks.getByName("jvmJar").outputs.files
        .plus(project.configurations.getByName("jvmRuntimeClasspath"))
        .plus(project.configurations.getByName("tinyEngineJsZip"))
}

// Make the application plugin start with the right classpath
// See https://youtrack.jetbrains.com/issue/KT-50227/MPP-JVM-target-executable-application
project.tasks.withType(JavaExec::class.java).configureEach {
    val jvmJar by tasks.existing
    val jvmRuntimeClasspath by configurations.existing
    val tinyEngineJsZip by configurations.existing

    classpath(jvmJar, jvmRuntimeClasspath, tinyEngineJsZip)
}

// Create custom script for Mac + LWJGL with "-XstartOnFirstThread" JVM option.
val macStartScripts = project.tasks.register("startScriptsForMac", CreateStartScripts::class.java) {
    val startScripts = project.tasks.getByName("startScripts", CreateStartScripts::class)

    description = "Create Mac OS custom start script"
    classpath = startScripts.classpath

    mainModule.set(startScripts.mainModule)
    mainClass.set(startScripts.mainClass)

    conventionMapping.map("applicationName") { startScripts.conventionMapping.getConventionValue(null as String?, "applicationName", false) + "-mac" }
    conventionMapping.map("outputDir") { startScripts.conventionMapping.getConventionValue(null as File?, "outputDir", false) }
    conventionMapping.map("executableDir") { startScripts.conventionMapping.getConventionValue(null as String?, "executableDir", false) }
    conventionMapping.map("defaultJvmOpts") { listOf("-XstartOnFirstThread") }

    modularity.inferModulePath.convention(startScripts.modularity.inferModulePath)
}

project.afterEvaluate {
    project.tasks["distTar"].dependsOn(macStartScripts)
    project.tasks["distZip"].dependsOn(macStartScripts)
}
