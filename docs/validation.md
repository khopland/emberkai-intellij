# Theme validation

Run `./gradlew check verifyPluginStructure verifyPlugin buildPlugin` before submitting changes.
Use `-PplatformVersion=2026.1.4` to check the oldest supported IDE, or
`-PplatformLocalPath=/path/to/IDE` to use a local installation.

`validateThemes` uses the target IDE's theme parser and editor scheme reader. It
loads the IDE's libraries before optional module jars, which can contain
conflicting library copies such as JSVG in the charts module. It
checks all theme registrations and inheritance chains, matching No Italics
schemes, preserved syntax colors, 16 distinct ANSI colors, diff readability,
Contrast text thresholds, and Islands geometry and selected controls.
Run/stop and plugin install buttons must use the palette's green and red with
4.5:1 foregrounds, every Islands color ramp must be replaced by the palette,
secondary text must reach 4.5:1, editor text without its own background must
reach 3:1, unused and deprecated code must differ from comments, and accents must differ visibly from the error
and warning colors. Editor schemes must not contain known colors or plugin keys inherited
from the themes Emberkai was adapted from. Emberkai's dark schemes extend Darcula
and its light schemes extend Default, so every key that the base scheme, or a
bundled plugin's addition to it, colors explicitly must be redefined from the
palette. The check reads these keys from the target IDE, so a new IDE version
that adds base scheme colors fails until they are mapped.
No resolved UI color may be JetBrains'
brand blue or its navy tints, or one of Darcula's leftover grays, tab tints, link
blue, and badge colors, apart from licensing, profiler, and retired plugin UI.
Emberkai Islands Light is checked against Islands Light instead: its color ramps
and IntelliJ Light's literal grays and blues must be replaced. Ordinary buttons must be
filled with a 2.5:1 border, the selected segmented button must stand out from the
panel, and the selected Search Everywhere tab label and counter badges must
reach 4.5:1. Popup, menu, and notification borders must reach 1.5:1 against
their backgrounds. The preselected completion item must reach 1.2:1 against the
popup, like stock Islands Dark. Ignored file colors must reach 3:1, and
parameter hints must reach 4.5:1 on their chip without using the code's text
color. The current parameter in parameter info, Presentation Assistant popups,
progress counters, and plugin tags must reach 4.5:1 on their own backgrounds.
Find results, highlighted usages, and breakpoints need a visible background and
a scrollbar mark, and write usages must differ from read usages. Indeterminate
progress needs two distinct colors, split editors need a divider, the default
yellow and green file colors must stay subtle and differ from the selection,
and editor scrollbars must change on hover. Info and "new" badge dots must use the preset accent, and checkbox
palette keys must use the unsuffixed names that New UI themes expect.
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
2026.1, 2026.2, and 2026.3. The newest target also uploads an installable ZIP.

These checks are headless. Use `./gradlew runIde` for a visual smoke test:

Open `docs/samples/pom.xml` as a Maven project to run `EmberkaiDemo.main()`
and inspect ANSI output in the Run console. The sample requires Java 21+.

- Switch between Emberkai Islands, Contrast, Teal, Rose, Ember, and Light.
- With Emberkai Islands Rose selected, open another project and install or enable a
  plugin without restarting. Check that editor and Project view colors remain
  correct and the IDE log has no "refers to unknown color scheme" errors.
- In Islands, check the base theme, Contrast, Teal, Rose, Ember, and Light with regular and compact
  spacing. Open another window to inspect the inactive frame.
- Focus the Project tool window in each preset and check that its sidebar
  button uses the preset accent, including after switching between themes.
- Install Symbols Icons and check Java, JavaScript, TypeScript, JSON, and folder
  icons while switching between Contrast, Teal, Rose, and Ember. Check that neutral
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
- In the sample Java file, check inlay hints (parameter names, chained-call
  types, code vision), an unmatched brace, a regular expression, and a grammar
  error in a comment.
- Open `emberkai.sql` with a PostgreSQL dialect and check keywords, aliases, and
  the outer-query column inside the correlated subquery.
- In RustRover, open `emberkai.rs` and check lifetimes, type parameters, macros,
  mutable bindings, the `unsafe` block, and the `cfg`-disabled function. Rust
  isn't bundled with IntelliJ IDEA, so the headless checks don't cover these keys.
- Open `SemanticDemo.java`, enable semantic highlighting in Language Defaults,
  and check that declarations and references share colors. Disable it again to
  check the original parameter/variable colors. Repeat with Contrast, Light, and No Italics.

## Visual check, 12 November 2026

Checked Emberkai Islands Light in the development IDE (IntelliJ IDEA 2026.3) on
macOS with the sample project:

- The Java fixtures with inlay hints, code vision, the caret row, identifiers
  under the caret, find results with their scrollbar marks, and code completion.
- Search Everywhere, Settings (tree, checkboxes, combo boxes, buttons), a
  message dialog, a context menu, and notification balloons.
- The Project tool window with focused and unfocused selection, the Git log,
  the diff viewer, and the Run console with all 16 ANSI colors.

The check replaced heavy outlines on identifiers under the caret and on find
results with soft fills. Not checked: the debugger, the terminal, semantic
highlighting, compact mode, and Windows or Linux.

## Visual check, 11 November 2026

Checked in the development IDE (IntelliJ IDEA 2026.3) on macOS with the sample project:

- Find results, with their scrollbar marks, in the base theme, Teal, and
  Contrast. Read and write usages of a loop variable in the base theme.
- The matched text highlight in Find Action in the base theme, Teal, Rose, and
  Ember.
- A split editor, Settings → Plugins, the parameter info popup, and the diff
  viewer in the base theme.
- Contrast with its No Italics scheme in compact mode, and the Run console
  with all 16 ANSI colors in Teal.

Not triggered: indeterminate progress, Presentation Assistant, and feature tips.

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

The 2026.1 compatibility checks use native readers and Plugin Verifier;
live UI checks were performed on 2026.2.3.

The original layouts and Classic palette were removed after this visual check.
