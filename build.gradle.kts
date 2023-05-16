plugins {
    id("com.github.minigdx.gradle.plugin.developer.mpp") version "1.3.1" apply false
    id("com.github.minigdx.gradle.plugin.developer") version "1.3.1"
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
