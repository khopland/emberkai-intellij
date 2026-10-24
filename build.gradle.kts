plugins {
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "com.github.khopland.emberkai"
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        val localPath = providers.gradleProperty("platformLocalPath").orNull
        if (localPath != null) {
            local(localPath)
        } else {
            intellijIdea(providers.gradleProperty("platformVersion"))
        }
    }
}

intellijPlatform {
    // This plugin contains only theme resources, with no settings pages or code.
    buildSearchableOptions = false
    instrumentCode = false

    pluginConfiguration {
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = provider { null }
        }
    }
}
