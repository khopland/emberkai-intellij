# Emberkai Theme

<img src="src/main/resources/META-INF/pluginIcon.svg" alt="Emberkai ember logo" width="64" height="64">

A warm, high-color dark theme for IntelliJ-based IDEs, using the Islands layout.

Comes with a **Contrast** option with brighter comments and secondary text,
**Teal**, **Rose**, and **Ember** Islands accents, a **Light** theme, and optional
**No Italics** editor schemes.
Accent presets keep the same syntax colors. Buttons, run and stop controls,
badges, diff, console, and file icon colors all come from one palette.

**Emberkai Islands Light** uses warm paper backgrounds, a bright orange accent, and its
own **Emberkai Light** editor scheme.

Requires **IntelliJ Platform 2026.1 or newer**.

## Preview

Direct IntelliJ IDEA captures, with no mouse pointer. See the
[full screenshot gallery](docs/screenshots/README.md) for accents and features.

[![Emberkai Islands](docs/screenshots/emberkai-islands.png)](docs/screenshots/emberkai-islands.png)

### Features

Emberkai Islands Contrast, compact mode, with the matching No Italics scheme:

![Emberkai Islands Contrast editor preview](docs/screenshots/emberkai-contrast-compact.png)

Emberkai Islands Teal in the diff viewer:

![Emberkai Islands Teal diff preview](docs/screenshots/emberkai-teal-diff.png)

| ANSI console colors | Optional semantic highlighting |
| --- | --- |
| [![Emberkai Teal normal and bright ANSI console colors](docs/screenshots/emberkai-teal-console.png)](docs/screenshots/emberkai-teal-console.png) | [![Emberkai Contrast semantic highlighting for parameters and local variables](docs/screenshots/emberkai-semantic-highlighting.png)](docs/screenshots/emberkai-semantic-highlighting.png) |

The gallery also compares [Teal and Rose accents](docs/screenshots/README.md#teal-and-rose-accents).

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

## Optional file icons

Install [Symbols Icons](https://github.com/joansgarcia/jetbrains-symbols) separately
to use its file and folder icons with Emberkai. The common Symbols colors follow
the selected Emberkai palette automatically. Contrast variants brighten neutral
icons; Teal, Rose, and Ember keep their palette's icon colors. Icons keep their colors
when selected in the Project panel.

Emberkai uses JetBrains' global SVG color replacements. Other icons using the
same source colors can also change. The replacements are active whenever an
Emberkai theme is selected, even without Symbols installed. See
[icon color support](docs/icon-colors.md) for the mappings and limitations.

## Development

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
