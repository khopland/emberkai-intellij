# Theme validation

Run `./gradlew check verifyPluginStructure verifyPlugin buildPlugin` before submitting changes.
Use `-PplatformVersion=2025.3.6.1` to check the oldest supported IDE, or
`-PplatformLocalPath=/path/to/IDE` to use a local installation.

`validateThemes` uses the target IDE's theme parser and editor scheme reader. It
loads the IDE's libraries before optional module jars, which can contain
conflicting library copies such as JSVG in IntelliJ 2025.3's charts module. It
checks all theme registrations and inheritance chains, matching No Italics
schemes, preserved syntax colors, 16 distinct ANSI colors, diff readability,
Contrast text thresholds, and Islands geometry and selected controls.
Every editor scheme must also have an explicit `bundledColorScheme` registration
whose ID matches the XML scheme name. This keeps schemes available independently
of theme discovery when projects or plugins reload.
The parser runs with New UI enabled so IntelliJ's per-theme selection defaults
cannot hide inheritance issues. Selected tool window buttons must use the
preset's accent background and contrasting icon foreground.
Icon checks run the target IDE's SVG color patcher against every preset's
inherited Symbols mappings, including fills, strokes, neutral Contrast colors,
and colors outside the map. Both normal and selected icon color patchers are
created by the IDE's native theme factory and checked against the mappings.
An orange SVG fixture is also painted normally, with the selected icon
renderer, and normally again; its color must remain unchanged.
It also checks the five native semantic color stops in every scheme, all 21
interpolated shades against the editor and caret-row backgrounds (4.5:1, or
5.5:1 for Contrast), and the native opt-in toggle. A running IDE may further
adjust semantic shades around its language-specific diagnostic colors.
`verifyPlugin` also runs JetBrains Plugin Verifier against the selected IDE,
with reports under `build/reports/pluginVerifier/`.
GitHub Actions runs these checks on IntelliJ
2025.3, 2026.1, and 2026.2. The newest target also uploads an installable ZIP.

These checks are headless. Use `./gradlew runIde` for a visual smoke test:

Open `docs/samples/pom.xml` as a Maven project to run `EmberkaiDemo.main()`
and inspect ANSI output in the Run console. The sample requires Java 21+.

- Switch through the five Islands palettes and their Contrast variants.
- With Spectrum Rose selected, open another project and install or enable a
  plugin without restarting. Check that editor and Project view colors remain
  correct and the IDE log has no "refers to unknown color scheme" errors.
- In Islands, check Spectrum, Contrast, Teal, and Rose with regular and compact
  spacing. Open another window to inspect the inactive frame.
- Focus the Project tool window in each preset and check that its sidebar
  button uses the preset accent, including after switching between themes.
- Install Symbols Icons and check Java, JavaScript, TypeScript, JSON, and folder
  icons while switching palettes, Contrast, Teal, and Rose. Check that neutral
  icons brighten with Contrast. Select and deselect files and folders; their
  icon colors must stay the same, including orange XML icons. Disable Symbols
  and confirm the normal IDE icons return; SVGs sharing mapped source colors
  can still be recolored.
- Open a diff with additions, deletions, and modifications; inspect conflict
  colors in Editor → Color Scheme → Diff & Merge.
- Run `bash docs/samples/ansi-colors.sh` in the IDE terminal. Inspect normal and
  bright colors in Editor → Color Scheme → Console Colors too: terminal palettes
  can be customized separately from the console scheme.
- Select a matching No Italics scheme under Editor → Color Scheme, then check
  comments, strings, keywords, selection, and the gutter in the sample Java file.
- Open `SemanticDemo.java`, enable semantic highlighting in Language Defaults,
  and check that declarations and references share colors. Disable it again to
  check the original parameter/variable colors. Repeat with Contrast and No Italics.

## Visual check, 24 October 2026

Checked in an isolated IntelliJ IDEA 2026.2.3 instance on macOS:

- All six palettes available at the time in the native diff viewer, with added,
  deleted, and modified lines. Conflict colors were inspected in the IDE's
  Diff & Merge preview.
- Islands theme switching, with Spectrum editor syntax, selected tabs and tool
  windows, regular and compact spacing, and active/inactive window colors.
- Spectrum ANSI output in the Run console: 16 colors and successful execution.
- Spectrum Contrast with its No Italics scheme, including comments, class names,
  and Java static method calls. The check exposed inherited Java method styles;
  explicit Java mappings now follow each palette's generic static-method style.
- Spectrum Teal in the native diff viewer. [Screenshots](screenshots/README.md)
  are direct IDE captures.
- Installed the prepared 1.0.0 ZIP through Install Plugin from Disk in a fresh
  IDE profile, restarted, and confirmed Emberkai Theme 1.0.0 was enabled.

The 2025.3 and 2026.1 compatibility checks use native readers and Plugin Verifier;
live UI checks were performed on 2026.2.3.

The original layouts and Classic palette were removed after this visual check.
