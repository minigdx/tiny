plugins {
    id("com.github.minigdx.gradle.plugin.developer.mpp")
}
repositories {
    mavenCentral()
}

dependencies {
    this.commonTestImplementation(kotlin("test"))

    jsMainImplementation(project(":tiny-engine"))
}

// FIXME: depends on the _boot.lua, ... from :tiny-engine instead of having a copy here.

configurations.create("tinyWebEditorEngine") {
    isCanBeResolved = false
    isCanBeConsumed = true
}

val tinyWebEditor = tasks.register("tinyWebEditor", Zip::class) {
    group = "tiny"
    from(tasks.getByName("jsBrowserDistribution"))
    this.destinationDirectory.set(project.buildDir.resolve("tiny-dist"))
}

artifacts {
    add("tinyWebEditorEngine", tinyWebEditor)
}
