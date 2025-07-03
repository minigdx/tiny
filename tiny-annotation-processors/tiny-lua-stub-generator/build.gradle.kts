@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.minigdx.mpp)
}

dependencies {
    jvmMainImplementation(libs.ksp.symbol.processing.api)
    jvmMainImplementation(project(":tiny-doc-annotations"))
    jvmMainImplementation(project(":tiny-annotation-processors:tiny-asciidoctor-dsl"))
    jvmMainImplementation(project(":tiny-annotation-processors:tiny-lua-dsl"))
}
