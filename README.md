# Emberkai Theme

<img src="src/main/resources/META-INF/pluginIcon.svg" alt="Emberkai ember logo" width="64" height="64">

A dark theme collection for IntelliJ-based IDEs, with original and Islands styles.

Includes six palettes: Default, Classic, Machine, Octagon, Ristretto, and Spectrum,
plus optional **No Italics** editor schemes.

Every palette includes a **Contrast** option with brighter comments and secondary
text, plus **Teal** and **Rose** Islands accents. Accent presets keep the palette's
syntax colors. All palettes include coordinated diff and console colors.

Requires **IntelliJ Platform 2025.3 or newer**.
See [IDE screenshots](docs/screenshots/README.md).

## Build and install

```sh
./gradlew buildPlugin
```

1. Open **Settings → Plugins → gear menu → Install Plugin from Disk…** and
   select the ZIP from `build/distributions/`.
2. Choose an Emberkai theme under **Settings → Appearance & Behavior → Appearance**.
3. For no italics, choose the matching scheme under **Settings → Editor → Color Scheme**.

Optional [semantic highlighting](docs/semantic-highlighting.md) gives individual
parameters and local variables colors from a range matched to your palette.

To preview in a development IDE, run `./gradlew runIde`.
Checks: `./gradlew check verifyPluginStructure verifyPlugin`. See [validation](docs/validation.md)
for the visual smoke test.

[Release instructions](docs/releasing.md) · [Changelog](CHANGELOG.md) ·
[Report an issue](https://github.com/khopland/emberkai-intellij/issues)

## Credits

[Neokai by tautastic](https://github.com/tautastic/neokai),
[Monokai Pro Theme by Subtheme](https://github.com/espositocode/monokai-pro),
and the original [Monokai color design](https://monokai.pro).

Licensed under [Apache 2.0](LICENSE), with inherited [MIT notices](NOTICE).
