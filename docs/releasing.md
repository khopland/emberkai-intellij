# Releasing Emberkai

The current release is **2.0.0**, tagged **v2.0.0**. Releases run through GitHub
Actions. Keep `pluginVersion` in `gradle.properties`, the `## <version>` entry in
`CHANGELOG.md`, and the change notes in `src/main/resources/META-INF/plugin.xml`
in sync.

## Prepare

1. Bump `pluginVersion` in `gradle.properties`.
2. Add a `## <version>` section to `CHANGELOG.md` and update the plugin change
   notes.
3. Open a pull request. CI validates the themes, plugin structure, and Plugin
   Verifier against IntelliJ 2026.1.4 and 2026.2.3.
4. Merge the pull request to `main`.

## Build and check locally

Use JDK 25 (available through SDKMAN), then run:

```sh
./gradlew prepareRelease
```

This runs the theme checks, plugin structure validation, and JetBrains Plugin
Verifier against the configured IDE. Reports are in
`build/reports/pluginVerifier/`.

The installable ZIP and `SHA256SUMS` are staged in `build/release/`.
Install that ZIP through **Plugins → gear menu → Install Plugin from Disk…** in
a fresh IDE profile and follow the [visual checks](validation.md). Check theme
switching, No Italics, and semantic highlighting on and off.

## Draft release

When a push to `main` passes both validation jobs, CI creates or updates a
**draft GitHub release** for `pluginVersion` if `CHANGELOG.md` has a matching
`## <version>` entry. The draft contains the tested ZIP, `SHA256SUMS`, and the
release notes from the changelog. Later pushes to `main` refresh the draft.

Download the draft ZIP and smoke-test it. When it is ready, publish the draft.
Publishing creates the `v<version>` tag and starts the release workflow. Do not
push release tags by hand; the draft owns the tag.

## Release workflow

Publishing a GitHub release starts the release workflow:

1. Checks that the tag matches `pluginVersion`.
2. Runs the theme checks, structure validation, and Plugin Verifier, and builds
   the plugin ZIP from the tagged commit.
3. Uploads the ZIP and `SHA256SUMS` to the GitHub release.
4. Rebuilds the tagged plugin, signs it when the signing secrets are set, and
   publishes it to JetBrains Marketplace. Versions with a suffix, such as
   `2.1.0-beta.1`, go to the `beta` channel; plain versions go to `default`.

If the Marketplace step fails, re-run the failed **Release** workflow from the
Actions tab after fixing the cause.

## Secrets

Add these under **Settings → Secrets and variables → Actions**:

- `PUBLISH_TOKEN`: a JetBrains Marketplace token from
  [plugins.jetbrains.com/author/me/tokens](https://plugins.jetbrains.com/author/me/tokens).
- `CERTIFICATE_CHAIN` and `PRIVATE_KEY`: the PEM certificate chain and private
  key used to sign the ZIP.
- `PRIVATE_KEY_PASSWORD`: only if the private key is encrypted.

## Signing

To sign release builds, set `CERTIFICATE_CHAIN` and `PRIVATE_KEY` to the PEM
certificate chain and private key, and `PRIVATE_KEY_PASSWORD` if the key is
encrypted. Locally, use environment variables; for CI, use the repository
secrets with the same names. `prepareRelease` signs the ZIP and verifies its
signature when both the certificate and key are provided. With neither set, it
stages the unsigned ZIP. Providing only one fails the task.

Generate your signing certificate using the
[JetBrains signing instructions](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html).

## First Marketplace upload

The first version can be uploaded by the release workflow, or
[manually](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html)
using the tested ZIP. Use the same **khopland** vendor profile as Version Checker
and keep **Show email** off. Public support goes through
[GitHub Issues](https://github.com/khopland/emberkai-intellij/issues); no email
address is embedded in the plugin or documentation.

Use the plugin description and change notes from `plugin.xml`, and set:

- Source: `https://github.com/khopland/emberkai-intellij`
- Issue tracker: `https://github.com/khopland/emberkai-intellij/issues`
- Documentation: `https://github.com/khopland/emberkai-intellij#readme`
- License: Apache 2.0, with the [inherited MIT notices](../NOTICE).
- Screenshots: the [native IDE captures](screenshots/README.md), with Emberkai Islands first.

Review the [Marketplace requirements](https://plugins.jetbrains.com/docs/marketplace/jetbrains-marketplace-approval-guidelines.html)
when submitting. After approval, add the Marketplace installation link to the README.
