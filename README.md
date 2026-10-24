# Emberkai Theme

A dark theme collection for IntelliJ-based IDEs, with original and Islands styles.

Includes six palettes: Default, Classic, Machine, Octagon, Ristretto, and Spectrum,
plus optional **No Italics** editor schemes.

Every palette includes a **Contrast** option with brighter comments and secondary
text, plus **Teal** and **Rose** Islands accents. Accent presets keep the palette's
syntax colors. Spectrum also includes refined diff and console colors.

Requires **IntelliJ Platform 2025.3 or newer**.

## Build and install

```sh
./gradlew buildPlugin
```

1. Open **Settings → Plugins → gear menu → Install Plugin from Disk…** and
   select the ZIP from `build/distributions/`.
2. Choose an Emberkai theme under **Settings → Appearance & Behavior → Appearance**.
3. For no italics, choose the matching scheme under **Settings → Editor → Color Scheme**.

To preview in a development IDE, run `./gradlew runIde`.

## Credits

[Neokai by tautastic](https://github.com/tautastic/neokai) and Monokai Pro
Theme 1.10.
