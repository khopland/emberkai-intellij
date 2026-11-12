import java.security.MessageDigest
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask
import org.jetbrains.intellij.platform.gradle.tasks.aware.IntelliJPlatformVersionAware

plugins {
    id("org.jetbrains.intellij.platform") version "2.20.1"
}

abstract class ThemeCheckTask : JavaExec(), IntelliJPlatformVersionAware

val validateThemes = tasks.register<ThemeCheckTask>("validateThemes") {
    group = "verification"
    description = "Parses every theme with the target IDE and checks palette and scheme consistency."
    inputs.dir("src/main/resources")
    inputs.file("scripts/ThemeCheck.java")
    jvmArgs("-Djava.awt.headless=true", "-Dide.experimental.ui=true")
    mainClass = layout.projectDirectory.file("scripts/ThemeCheck.java").asFile.absolutePath
    args(layout.projectDirectory.dir("src/main/resources").asFile.absolutePath)
    // The IDE root, to find the colors bundled plugins add to Darcula.
    argumentProviders.add(CommandLineArgumentProvider { listOf(platformPath.toString()) })
    // Keep IDE libraries ahead of optional modules, which may bundle conflicting
    // copies of libraries (e.g. JSVG inside intellij.charts).
    // Recent IDEs also keep UITheme and the scheme reader in module jars.
    classpath = files(provider {
        val lib = platformPath.resolve("lib").toFile()
        fileTree(lib) { include("*.jar") }.files.sortedBy { it.name } +
            fileTree(lib.resolve("modules")) { include("*.jar") }.files.sortedBy { it.name }
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

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        // 2.1.0-beta.1 publishes to the "beta" channel; plain versions go to "default".
        channels = providers.gradleProperty("pluginVersion").map { pluginVersion ->
            listOf(pluginVersion.substringAfter('-', "").substringBefore('.').ifEmpty { "default" })
        }
    }
}

val hasCertificate = providers.environmentVariable("CERTIFICATE_CHAIN").orNull?.isNotBlank() == true
val hasPrivateKey = providers.environmentVariable("PRIVATE_KEY").orNull?.isNotBlank() == true
val releaseDir = layout.buildDirectory.dir("release")

// The release workflow tests the plugin in one job, then signs and publishes that same ZIP in a job with the
// secrets. There, -PtestedArchive=<zip> takes the place of buildPlugin's output, so nothing is rebuilt.
val testedArchive = providers.gradleProperty("testedArchive").orNull?.let { layout.projectDirectory.file(it) }
if (testedArchive != null) {
    tasks.signPlugin {
        archiveFile = testedArchive
    }
    // Publishes the ZIP that prepareRelease staged, without building or signing again.
    tasks.publishPlugin {
        setDependsOn(emptyList<Any>())
        archiveFiles.setFrom(fileTree(releaseDir) { include("*.zip") })
    }
}
val unsignedArchive = testedArchive?.let { provider { it } } ?: tasks.buildPlugin.flatMap { it.archiveFile }
val releaseArchive = if (hasCertificate && hasPrivateKey) {
    tasks.signPlugin.flatMap { it.signedArchiveFile }
} else {
    unsignedArchive
}

tasks.verifyPluginSignature {
    inputArchiveFile = tasks.signPlugin.flatMap { it.signedArchiveFile }
}

tasks.register<Sync>("prepareRelease") {
    group = "distribution"
    description = "Signs the plugin when configured and stages the release ZIP with a SHA-256 checksum."
    inputs.property("signingConfigurationComplete", hasCertificate == hasPrivateKey)
    if (hasCertificate && hasPrivateKey) dependsOn(tasks.verifyPluginSignature)
    from(releaseArchive)
    into(releaseDir)
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
