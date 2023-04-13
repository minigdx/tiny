plugins {
    id("org.asciidoctor.jvm.convert") version "3.3.2"
    id("com.github.minigdx.gradle.plugin.developer")
}

project.tasks.getByName("asciidoctor", org.asciidoctor.gradle.jvm.AsciidoctorTask::class) {
    this.baseDirFollowsSourceDir()
}
