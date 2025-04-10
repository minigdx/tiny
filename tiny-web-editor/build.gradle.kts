@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.minigdx.mpp)
}

dependencies {
    commonTestImplementation(kotlin("test"))

    jsMainImplementation(libs.luak)
    jsMainImplementation(libs.kotlin.coroutines)
    jsMainImplementation("org.jetbrains.kotlin:kotlinx-atomicfu-runtime:2.1.20")
        ?.because("https://youtrack.jetbrains.com/issue/KT-57235")
    jsMainImplementation(project(":tiny-engine"))
}

// FIXME: depends on the _boot.lua, ... from :tiny-engine instead of having a copy here.

configurations.create("tinyWebEditorEngine") {
    isCanBeResolved = false
    isCanBeConsumed = true
}

val tinyWebEditor =
    tasks.register("tinyWebEditor", Zip::class) {
        group = "tiny"
        from(tasks.getByName("jsBrowserDistribution"))
        this.destinationDirectory.set(project.buildDir.resolve("tiny-dist"))
        this.archiveVersion.set("")
    }

artifacts {
    add("tinyWebEditorEngine", tinyWebEditor)
}
