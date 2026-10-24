# Theme validation

Run `./gradlew check verifyPluginStructure buildPlugin` before submitting changes.
Use `-PplatformVersion=2025.3.6.1` to check the oldest supported IDE, or
`-PplatformLocalPath=/path/to/IDE` to use a local installation.

`validateThemes` uses the target IDE's theme parser and editor scheme reader. It
checks all theme registrations and inheritance chains, matching No Italics
schemes, preserved syntax colors, 16 distinct ANSI colors, diff readability,
Contrast text thresholds, and Islands geometry and selected controls.
GitHub Actions runs this check and the plugin packaging checks on IntelliJ
2025.3, 2026.1, and 2026.2. The newest target also uploads an installable ZIP.

These checks are headless. Use `./gradlew runIde` for a visual smoke test:

- Switch through the six original palettes and their Contrast variants.
- In Islands, check Spectrum, Contrast, Teal, and Rose with regular and compact
  spacing. Open another window to inspect the inactive frame.
- Open a diff with additions, deletions, and modifications; inspect conflict
  colors in Editor → Color Scheme → Diff & Merge.
- Run `bash docs/samples/ansi-colors.sh` in the IDE terminal. Inspect normal and
  bright colors in Editor → Color Scheme → Console Colors too: terminal palettes
  can be customized separately from the console scheme.
- Select a matching No Italics scheme under Editor → Color Scheme, then check
  comments, strings, keywords, selection, and the gutter in the sample Java file.
