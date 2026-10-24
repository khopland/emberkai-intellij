import java.security.MessageDigest
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask
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
        pluginVerifier("1.411")
        zipSigner("0.1.43")
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

    pluginVerification {
        // Reuse the target IDE, including each version selected by the CI matrix.
        ides { current() }
        failureLevel.set(listOf(
            VerifyPluginTask.FailureLevel.COMPATIBILITY_PROBLEMS,
            VerifyPluginTask.FailureLevel.INTERNAL_API_USAGES,
            VerifyPluginTask.FailureLevel.OVERRIDE_ONLY_API_USAGES,
            VerifyPluginTask.FailureLevel.INVALID_PLUGIN,
            VerifyPluginTask.FailureLevel.MISSING_DEPENDENCIES,
            VerifyPluginTask.FailureLevel.PLUGIN_STRUCTURE_WARNINGS,
        ))
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }
}

val hasCertificate = providers.environmentVariable("CERTIFICATE_CHAIN").orNull?.isNotBlank() == true
val hasPrivateKey = providers.environmentVariable("PRIVATE_KEY").orNull?.isNotBlank() == true
val releaseArchive = if (hasCertificate && hasPrivateKey) {
    tasks.signPlugin.flatMap { it.signedArchiveFile }
} else {
    tasks.buildPlugin.flatMap { it.archiveFile }
}

tasks.verifyPluginSignature {
    inputArchiveFile = tasks.signPlugin.flatMap { it.signedArchiveFile }
}

tasks.register<Sync>("prepareRelease") {
    group = "distribution"
    description = "Verifies the plugin and stages the release ZIP with a SHA-256 checksum."
    dependsOn(tasks.check, tasks.verifyPluginStructure, tasks.verifyPlugin)
    inputs.property("signingConfigurationComplete", hasCertificate == hasPrivateKey)
    if (hasCertificate && hasPrivateKey) dependsOn(tasks.verifyPluginSignature)
    from(releaseArchive)
    into(layout.buildDirectory.dir("release"))
    doFirst {
        require(hasCertificate == hasPrivateKey) {
            "Provide both CERTIFICATE_CHAIN and PRIVATE_KEY, or leave both unset."
        }
    }
    doLast {
        val archive = destinationDir.resolve(releaseArchive.get().asFile.name)
        val digest = MessageDigest.getInstance("SHA-256").digest(archive.readBytes())
        val checksum = digest.joinToString("") { "%02x".format(it.toInt() and 0xff) }
        destinationDir.resolve("SHA256SUMS").writeText("$checksum  ${archive.name}\n")
    }
}
