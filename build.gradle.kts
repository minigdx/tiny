@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.minigdx.mpp).apply(false)
    alias(libs.plugins.minigdx.developer)
}

minigdxDeveloper {
    this.name.set("\uD83E\uDDF8 Tiny")
    this.description.set("Multiplatform 2D Game Engine with fast loop development using Kotlin Multiplatform and Lua")
    this.projectUrl.set("https://github.com/minigdx/tiny")
    this.licence {
        name.set("MIT Licence")
        url.set("https://github.com/minigdx/tiny/blob/master/LICENSE")
    }
    developer {
        name.set("David Wursteisen")
        email.set("david.wursteisen+minigdx@gmail.com")
        url.set("https://github.com/dwursteisen")
    }
}
