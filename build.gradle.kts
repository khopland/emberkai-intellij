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

// Screenshots. Each capture starts the development IDE with a fresh config on a copy of
// docs/samples outside Git, so no branch or author hints show. A Groovy startup script selects
// the preset, and a Performance Testing script paints the IDE frame into a PNG, so captures
// contain no mouse pointer, popups, or other windows. Run `./gradlew screenshots`, or one
// capture such as `./gradlew screenshotEmberkaiLight`. On a display without 2x scaling, such as
// Xvfb in CI, add -PscreenshotScale=2 to keep the 2800 x 2000 size.
// `./gradlew marketplaceScreenshots` captures the 16:10 Marketplace media in upload order.
data class Capture(
    val name: String, val theme: String, val scheme: String, val compact: Boolean = false,
    val marketplace: Boolean = false,
) {
    val taskName = (if (marketplace) "marketplace-" else "screenshot-") + name
    // Window size in points. Marketplace media is 16:10; the gallery keeps its 7:5 images.
    val width = if (marketplace) 1440 else 1400
    val height = if (marketplace) 900 else 1000
    // An empty line: the last one scrolls the shorter Marketplace window past the first line.
    val caretLine = if (marketplace) 3 else 32
}

val galleryCaptures = listOf(
    Capture("emberkai-islands", "emberkai", "Emberkai"),
    Capture("emberkai-teal", "emberkai-teal", "Emberkai"),
    Capture("emberkai-rose", "emberkai-rose", "Emberkai"),
    Capture("emberkai-ember", "emberkai-ember", "Emberkai"),
    Capture("emberkai-contrast-compact", "emberkai-contrast", "Emberkai Contrast - No Italics", compact = true),
    Capture("emberkai-light", "emberkai-light", "Emberkai Light"),
)
val marketplaceCaptures = listOf(
    Capture("1-emberkai-islands", "emberkai", "Emberkai", marketplace = true),
    Capture("2-emberkai-light", "emberkai-light", "Emberkai Light", marketplace = true),
    Capture("3-emberkai-contrast", "emberkai-contrast", "Emberkai Contrast - No Italics", compact = true, marketplace = true),
    Capture("4-emberkai-teal", "emberkai-teal", "Emberkai", marketplace = true),
    Capture("5-emberkai-rose", "emberkai-rose", "Emberkai", marketplace = true),
    Capture("6-emberkai-ember", "emberkai-ember", "Emberkai", marketplace = true),
)
val themeIds = Regex("""<themeProvider id="([^"]+)" path="/islands/([^"]+)\.theme\.json"/>""")
    .findAll(file("src/main/resources/META-INF/plugin.xml").readText())
    .associate { it.groupValues[2] to it.groupValues[1] }
val screenshotSamples = layout.projectDirectory.dir("docs/samples").asFile
val screenshotOutput = layout.projectDirectory.dir("docs/screenshots").asFile
val marketplaceOutput = layout.projectDirectory.dir("docs/marketplace").asFile
val screenshotProject = layout.buildDirectory.dir("screenshot-project/samples")
val screenshotJdk = providers.systemProperty("java.home")
val screenshotScale = providers.gradleProperty("screenshotScale")

fun registerCapture(capture: Capture): String {
    val taskName = capture.taskName.split('-').joinToString("") { it.replaceFirstChar(Char::uppercase) }.replaceFirstChar(Char::lowercase)
    val output = if (capture.marketplace) marketplaceOutput else screenshotOutput
    val themeId = requireNotNull(themeIds[capture.theme]) { "No theme registered for ${capture.theme}" }
    intellijPlatformTesting.runIde.register(taskName) {
        task {
            group = "documentation"
            description = "Captures docs/${output.name}/${capture.name}.png in the development IDE."
            val script = temporaryDir.resolve("capture.txt")
            val config = sandboxConfigDirectory
            val system = sandboxSystemDirectory
            val logs = sandboxLogDirectory
            jvmArgumentProviders += CommandLineArgumentProvider {
                listOf(
                    "-Dtestscript.filename=$script",
                    "-Didea.trust.all.projects=true",
                    "-Djb.consents.confirmation.enabled=false",
                    "-Djb.privacy.policy.text=<!--999.999-->",
                    "-Dide.show.tips.on.startup.default.value=false",
                    "-Dide.newUsersOnboarding=false",
                    "-Didea.initially.ask.config=never",
                ) + screenshotScale.map { listOf("-Dsun.java2d.uiScale=$it") }.getOrElse(emptyList())
            }
            val projectDir = screenshotProject.get().asFile
            args(projectDir.absolutePath)
            doFirst {
                projectDir.deleteRecursively()
                screenshotSamples.listFiles { file -> file.name !in setOf(".idea", "target") }!!
                    .forEach { it.copyRecursively(projectDir.resolve(it.name)) }
                // Start from a fresh profile: cached project state would refer to an SDK that no
                // longer exists. Recreate the directories, because the IDE falls back to other
                // locations, such as the user's own caches, when a sandbox directory is missing.
                val configDir = config.get().asFile
                for (dir in listOf(system.get().asFile, configDir, logs.get().asFile)) {
                    dir.deleteRecursively()
                    dir.mkdirs()
                }
                // The IDE keeps the theme in its internal state database, so select it from a
                // startup script instead of a settings file.
                val startup = configDir.resolve("extensions/com.intellij/startup").apply { mkdirs() }
                startup.resolve("emberkai-screenshot.groovy").writeText(
                    """
                    import com.intellij.ide.ui.LafManager
                    import com.intellij.ide.ui.UIDensity
                    import com.intellij.ide.ui.UISettings
                    import com.intellij.notification.impl.NotificationsConfigurationImpl
                    import com.intellij.openapi.application.ApplicationManager
                    import com.intellij.openapi.editor.colors.EditorColorsManager
                    import com.intellij.openapi.wm.ToolWindowManager
                    import com.intellij.openapi.wm.WindowManager

                    // Notifications such as plugin update checks go to the tool window, not a balloon.
                    NotificationsConfigurationImpl.instanceImpl.SHOW_BALLOONS = false
                    ApplicationManager.application.invokeAndWait {
                        def laf = LafManager.instance
                        laf.setCurrentLookAndFeel(laf.findLaf("$themeId"), false)
                        UISettings.instance.uiDensity = UIDensity.${if (capture.compact) "COMPACT" else "DEFAULT"}
                        laf.applyDensity()
                        laf.updateUI()
                        def colors = EditorColorsManager.instance
                        colors.setGlobalScheme(colors.getScheme("${capture.scheme}"))
                        // Points, painted at the display's scale: 2800 x 2000 for the gallery on a 2x display.
                        WindowManager.instance.getFrame(IDE.project).setBounds(40, 40, ${capture.width}, ${capture.height})
                    }
                    // Keep the Project view narrow enough for the longest sample line.
                    ApplicationManager.application.invokeLater {
                        def project = ToolWindowManager.getInstance(IDE.project).getToolWindow("Project")
                        project.stretchWidth(340 - project.component.width)
                    }
                    """.trimIndent() + "\n"
                )
                script.writeText(
                    """
                    %setupSDK "emberkai-jdk" "JavaSDK" "${screenshotJdk.get()}"
                    %waitForSmart
                    %openFile -file EmberkaiDemo.java
                    %goto ${capture.caretLine} 1
                    %sleep 3000
                    %takeScreenshot ${capture.name}
                    %exitApp true
                    """.trimIndent() + "\n"
                )
            }
            doLast {
                // Fail if the IDE still left the sandbox for its settings, caches, or logs.
                val sandbox = config.get().asFile.parentFile
                val log = logs.get().asFile.resolve("idea.log")
                if (!log.exists()) throw GradleException("The IDE did not log to $log")
                val paths = Regex("""-Didea\.(?:config|system|log)\.path=([^,\]]+)""")
                    .findAll(log.readText()).map { it.groupValues[1].trim() }.toList()
                if (paths.isEmpty() || paths.any { !File(it).startsWith(sandbox) })
                    throw GradleException("The IDE used settings or caches outside the sandbox: $paths")
                // One PNG per window; the project frame is the largest. Which frame number it gets
                // depends on the platform (macOS frame0, Linux frame1).
                val shot = logs.get().asFile.resolve("screenshots").walkTopDown().filter { it.extension == "png" }
                    .maxByOrNull { javax.imageio.ImageIO.read(it).let { image -> image.width.toLong() * image.height } }
                    ?: throw GradleException("No screenshot for ${capture.name}; see ${logs.get().asFile}/idea.log")
                val target = output.apply { mkdirs() }.resolve("${capture.name}.png")
                shot.copyTo(target, overwrite = true)
                logger.lifecycle("Saved $target")
            }
        }
    }
    return taskName
}

val galleryTasks = galleryCaptures.map(::registerCapture)
val marketplaceTasks = marketplaceCaptures.map(::registerCapture)

tasks.register("screenshots") {
    group = "documentation"
    description = "Captures every preset in docs/screenshots."
    dependsOn(galleryTasks)
}

tasks.register("marketplaceScreenshots") {
    group = "documentation"
    description = "Captures the 16:10 Marketplace media in docs/marketplace."
    dependsOn(marketplaceTasks)
}
