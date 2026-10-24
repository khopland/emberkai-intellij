import org.jetbrains.intellij.platform.gradle.tasks.aware.IntelliJPlatformVersionAware

plugins {
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

abstract class ThemeCheckTask : JavaExec(), IntelliJPlatformVersionAware

val validateThemes = tasks.register<ThemeCheckTask>("validateThemes") {
    group = "verification"
    description = "Parses every theme with the target IDE and checks palette and scheme consistency."
    inputs.dir("src/main/resources")
    inputs.file("scripts/ThemeCheck.java")
    jvmArgs("-Djava.awt.headless=true")
    mainClass = layout.projectDirectory.file("scripts/ThemeCheck.java").asFile.absolutePath
    args(layout.projectDirectory.dir("src/main/resources").asFile.absolutePath)
    // Include module jars too: recent IDEs keep UITheme and the scheme reader there.
    classpath = files(provider {
        fileTree(platformPath.resolve("lib").toFile()) { include("*.jar", "modules/*.jar") }
    })
}

tasks.check {
    dependsOn(validateThemes)
}

tasks.processResources {
    from(listOf("LICENSE", "NOTICE")) {
        into("META-INF")
    }
    from("licenses") {
        into("META-INF/licenses")
    }
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
