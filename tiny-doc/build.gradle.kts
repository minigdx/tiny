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
        asciidoctorResources.name,
        project(
            mapOf(
                "path" to ":tiny-web-editor",
                "configuration" to "tinyWebEditorEngine",
            ),
        ),
    )

    add(
        asciidoctorDependencies.name,
        project(
            mapOf(
                "path" to ":tiny-engine",
                "configuration" to "tinyApiAsciidoctor",
            ),
        ),
    )
}

val unzipAsciidoctorResources =
    tasks.maybeCreate("unzip-asciidoctorResources", Copy::class).also { cp ->
        asciidoctorResources.resolvedConfiguration.resolvedArtifacts.forEach {
            cp.from(zipTree(asciidoctorResources.incoming.artifacts.artifactFiles.files.first()))
        }
        cp.into(project.layout.buildDirectory.get().asFile.resolve("docs/asciidoc"))
    }

val copyAsciidoctorDependencies =
    tasks.maybeCreate("copy-asciidoctorDependencies", Copy::class).also { cp ->
        asciidoctorDependencies.resolvedConfiguration.resolvedArtifacts.forEach {
            cp.from(it.file)
        }
        // I'm bit lazy, I copy the result stray in the source directory :grimace:
        cp.into(project.projectDir.resolve("src/docs/asciidoc/dependencies"))
    }

val copySample =
    tasks.register("copy-sample", Copy::class) {
        from(project.projectDir.resolve("src/docs/asciidoc/sample"))
        into(project.layout.buildDirectory.get().asFile.resolve("docs/asciidoc/sample"))
    }

val copyResources =
    tasks.register("copy-resources", Copy::class) {
        from(project.projectDir.resolve("src/docs/asciidoc/resources"))
        into(project.layout.buildDirectory.get().asFile.resolve("docs/asciidoc/resources"))
    }

tasks.withType(AsciidoctorTask::class.java).configureEach {
    this.baseDirFollowsSourceDir()

    this.dependsOn(
        unzipAsciidoctorResources.dependsOn(":tiny-web-editor:tinyWebEditor"),
        copyAsciidoctorDependencies,
        copySample,
        copyResources,
    )
}
