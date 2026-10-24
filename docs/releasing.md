# Releasing Emberkai

The first release is **1.0.0**, tagged **v1.0.0**. Keep `pluginVersion` in
`gradle.properties`, `CHANGELOG.md`, and the plugin's change notes in sync.

## Build and check

Use JDK 25 (available through SDKMAN), then run:

```sh
./gradlew prepareRelease
```

This runs the theme checks, plugin structure validation, and JetBrains Plugin
Verifier against the configured IDE. CI repeats verification on IntelliJ
2025.3.6.1, 2026.1.4, and 2026.2.3. Reports are in
`build/reports/pluginVerifier/`.

The installable ZIP and `SHA256SUMS` are staged in `build/release/`.
Install that ZIP through **Plugins → gear menu → Install Plugin from Disk…** in
a fresh IDE profile and follow the [visual checks](validation.md). Check theme
switching, No Italics, and semantic highlighting on and off.

## Signing

To sign release builds, set `CERTIFICATE_CHAIN` and `PRIVATE_KEY` to the PEM
certificate chain and private key, and `PRIVATE_KEY_PASSWORD` if the key is
encrypted. Locally, use environment variables; for tagged builds, use GitHub
Actions repository secrets with the same names. `prepareRelease` signs the ZIP
and verifies its signature when both the certificate and key are provided.
With neither set, it stages the unsigned ZIP. Providing only one fails the task.

Generate your signing certificate using the
[JetBrains signing instructions](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html).

## Create the release

After the release PR is merged and all three CI checks pass:

```sh
git switch main
git pull --ff-only
git tag v1.0.0
git push origin v1.0.0
```

The tag must match `pluginVersion`. The release workflow runs all three CI targets,
verifies the release package, and creates a **draft GitHub release** with the ZIP,
checksum, and release notes.
Download and smoke-test that exact ZIP, then publish the draft when ready.
The workflow does not upload to JetBrains Marketplace.

## First Marketplace upload

[Upload the first version manually](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html)
using the tested ZIP. Use the same **khopland** vendor profile as Version Checker
and keep **Show email** off. Public support goes through
[GitHub Issues](https://github.com/khopland/emberkai-intellij/issues); no email
address is embedded in the plugin or documentation.

Use the plugin description and change notes from `plugin.xml`, and set:

- Source: `https://github.com/khopland/emberkai-intellij`
- Issue tracker: `https://github.com/khopland/emberkai-intellij/issues`
- Documentation: `https://github.com/khopland/emberkai-intellij#readme`
- License: Apache 2.0, with the [inherited MIT notices](../NOTICE).
- Screenshots: the [native IDE captures](screenshots/README.md), with Spectrum first.

Review the [Marketplace requirements](https://plugins.jetbrains.com/docs/marketplace/jetbrains-marketplace-approval-guidelines.html)
when submitting. After approval, add the Marketplace installation link to the README.
