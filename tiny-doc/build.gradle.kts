import org.asciidoctor.gradle.jvm.AsciidoctorTask

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.asciidoctorj)
    alias(libs.plugins.minigdx.developer)
}

val asciidoctorResources by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val asciidoctorDependencies by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    add(
        "asciidoctorResources",
        project(
            mapOf(
                "path" to ":tiny-web-editor",
                "configuration" to "tinyWebEditorEngine"
            )
        )
    )

    add(
        "asciidoctorDependencies",
        project(
            mapOf(
                "path" to ":tiny-engine",
                "configuration" to "tinyApiAsciidoctor"
            )
        )
    )
}

val unzipAsciidoctorResources = tasks.create("unzip-asciidoctorResources", Copy::class) {
    asciidoctorResources.resolvedConfiguration.resolvedArtifacts.forEach {
        from(zipTree(it.file))
    }
    into(project.buildDir.resolve("docs/asciidoc"))
}

val copyAsciidoctorDependencies = tasks.create("copy-asciidoctorDependencies", Copy::class) {
    asciidoctorDependencies.resolvedConfiguration.resolvedArtifacts.forEach {
        from(it.file)
    }
    // I'm bit lazy, I copy the result stray in the source directory :grimace:
    into(project.projectDir.resolve("src/docs/asciidoc/dependencies"))
}

val copySample = tasks.create("copy-sample", Copy::class) {
    from(project.projectDir.resolve("src/docs/asciidoc/sample"))
    into(project.buildDir.resolve("docs/asciidoc/sample"))
}

tasks.withType(AsciidoctorTask::class.java).configureEach {
    this.baseDirFollowsSourceDir()

    this.dependsOn(unzipAsciidoctorResources, copyAsciidoctorDependencies, copySample)
}
