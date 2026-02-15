@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.minigdx.mpp)
    alias(libs.plugins.kotlin.serialization)

    id("io.github.turansky.seskar") version "4.27.0"
    id("org.jetbrains.kotlin.plugin.js-plain-objects") version "2.2.20"
    id("io.github.turansky.kfc.application") version "14.12.0"
}

configurations.create("tinyDebugger") {
    isCanBeResolved = false
    isCanBeConsumed = true
}

dependencies {
    commonTestImplementation(kotlin("test"))

    commonMainImplementation(libs.kotlin.serialization.json)

    jsMainImplementation(libs.kotlin.coroutines)
    jsMainImplementation("org.jetbrains.kotlin:kotlinx-atomicfu-runtime:2.1.20")
        ?.because("https://youtrack.jetbrains.com/issue/KT-57235")
}

val tinyDebugger = tasks.register("tinyDebugger", Zip::class) {
    val jsBundleArchive = tasks.named<Jar>("jsBundleProduction").flatMap { it.archiveFile }

    group = "tiny"
    from(jsBundleArchive.map { zipTree(it) })
    exclude("index.html")
    this.destinationDirectory.set(project.layout.buildDirectory.dir("tiny-dist"))
    this.archiveVersion.set("")
}

artifacts {
    add("tinyDebugger", tinyDebugger)
}
