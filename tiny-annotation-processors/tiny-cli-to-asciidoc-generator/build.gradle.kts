plugins {
    alias(libs.plugins.minigdx.mpp)
}

dependencies {
    jvmMainImplementation(libs.ksp.symbol.processing.api)
    jvmMainImplementation(project(":tiny-doc-annotations"))
    jvmMainImplementation(project(":tiny-annotation-processors:tiny-asciidoctor-dsl"))
}
