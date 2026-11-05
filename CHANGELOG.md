# Changelog

## [Unreleased]

- Supports IntelliJ Platform 2026.3. The tool window drop hint added in 2026.3
  uses the accent instead of JetBrains blue.

## [2.0.3]

- Unused, deprecated, disabled, and unresolved code has its own muted lavender
  color, so it no longer looks like a comment. Unused code's wavy underline uses
  the same color and is now visible.
- Comments are brighter in Emberkai Islands (3.5:1 instead of 2.9:1).
- The editor selection is darker and tinted, so comments and punctuation stay
  readable when selected.
- Grammar-Kit keywords, JFlex macro references, class references, inactive
  breadcrumbs, and `.ignore` comments were nearly invisible and now use palette
  colors.
- Muted text that was still 2.9:1 is brighter: code vision hints, doc comment
  markup, Markdown markers, inactive links, inline completion, and Doxygen
  commands. Contrast brightens these too.
- Line numbers, whitespace, soft wrap signs, indent guides, and tearlines are
  easier to see. Indent guides and tearlines no longer disappear on the caret row.
- Theme validation fails if editor text is below 3:1 against the background, or
  if unused or deprecated code uses the comment color.
- Ordinary buttons have a raised fill and a brighter border instead of a
  transparent outline. Contrast keeps its brighter border.
- Selected segmented buttons (scope and mode switches) and selected tool window
  header tabs use the accent's muted selection color. Header tabs were a leftover
  navy.
- The selected Search Everywhere tab label is readable; it was dark text on the
  dark selection.
- Disabled text is brighter (2.9:1 instead of 2:1).
- Message dialog text, settings group titles, selected menu shortcuts, combo box
  arrows, and the LightEdit status bar no longer use Darcula's grays and blues.
- Counter badges, such as the number of plugin updates, use the accent color.
- Popups, menus, and notification balloons have a visible border. Popup and
  menu borders are a subtle dark gray; balloons previously had a border the
  same color as their background.
- Checked checkboxes and radio buttons use the accent with a dark mark instead
  of JetBrains blue.
- Git log rows on the current branch use a neutral gray instead of a blue tint.
- Badge dots, such as the update dot on the settings gear and the "new" dot next
  to Settings pages, use the accent instead of JetBrains blue. Error, warning,
  and success dots use the palette's red, orange, and green.
- Checkbox colors use the current key names, so the IDE no longer logs
  deprecation warnings for them on startup.
- Legacy hyperlinks, tool window drop areas, "new lesson" badges, the inactive
  diff block border, and Mercurial log icons use palette colors.
- Theme validation fails if buttons are unfilled, the selected segment or Search
  Everywhere tab is unreadable, a UI color is one of Darcula's leftover grays, or
  badge dots don't follow the accent.
- The preselected item in the code completion popup is easier to spot. Its
  highlight was barely lighter than the popup.
- Git-ignored files and excluded folders, such as `target`, are readable in the
  Project view (3.5:1 against the panel instead of 2:1). Contrast brightens them
  further.
- Parameter name hints use a soft gray chip instead of bright white text on a
  near-black chip, so they no longer compete with the code around them.
- Theme validation checks the completion highlight, ignored file colors, and
  that parameter hints are readable but quieter than code.

## [2.0.2]

- New **Emberkai Islands Ember** accent: a light ember orange, kept distinct from
  the warning color so links and focus rings don't read as warnings.
- Editor colors that still came from Darcula now use the palette. These include
  inlay hints and inlay buttons, unmatched braces, grammar and style underlines,
  regular expressions, XML and HTML entities, Sass, Less, Velocity and XPath, inline
  completion and AI prompts, debugger smart step into and evaluated expressions,
  bookmark mnemonics, the terminal's block colors, and UML and Mermaid diagrams.
- Bundled plugin colors follow the palette too: SQL, Kotlin, Angular signals,
  Compose, Docker Compose and Kubernetes status, code review, Code With Me
  participants, and find usages.
- RustRover lifetimes, type parameters, macros, mutable bindings, and unsafe,
  disabled, and generated code use palette colors.
- Theme validation fails if a scheme inherits any color that Darcula or a bundled
  plugin defines for Darcula.

## [2.0.1]

- First Marketplace release of 2.0. The 2.0.0 release workflow failed before
  publishing, so 2.0.1 ships the changes from 2.0.0 unchanged.

## [2.0.0]

- Requires IntelliJ Platform 2026.1 or newer. Support for 2025.3 is dropped.
**Breaking:** Emberkai now has one palette, based on Monokai Pro Spectrum. The
Default, Machine, Octagon, and Ristretto palettes are removed. Themes are now
**Emberkai Islands**, **Contrast**, **Teal**, and **Rose**, with matching
**Emberkai** editor schemes. Spectrum users keep their theme selection; reselect
the editor scheme (Settings → Editor → Color Scheme) if it resets.

- Status colors come from the palette. Run and stop buttons, plugin install and
  restart buttons, toggles, banners, badges, and progress bars no longer use
  Islands' stock green and red. All eight Islands color ramps are rebuilt from
  the palette's hues.
- Editor schemes use palette colors throughout. Parameters, constants, rainbow
  brackets, TODOs, `this`/`self`, breakpoints, coverage, live templates, and
  matched braces previously used colors copied from other themes.
- Unused code uses the comment color instead of a bright gray.
- The window frame, menus, and popups (including completion and quick documentation)
  are darker, closer to stock Islands Dark.
- The Rose accent is a lighter rose, so links and focus rings no longer match errors.
- Scope file colors (tests, non-project files) are tinted from the palette.
- Project colors (toolbar tint and Recent Projects avatars) use nine palette blends.
- The Git log, file history, debugger variables, test progress, badges, drag-and-drop
  highlights, memory indicator, code review, sliders, and project tabs use palette
  colors instead of JetBrains blue and blue-grays. The legacy Dark color scales
  that plugins read (Gray1 to Gray14, Blue1 to Blue13, and others) are rebuilt
  from the palette too.
- Docker, webpack, Cypress, and Orval Symbols icons follow the palette.
- Removed unused Markdown Navigator and Material Theme scheme entries.

## [1.0.0]

Initial Emberkai release for IntelliJ Platform 2025.3 and newer.

- Five palettes: Default, Machine, Octagon, Ristretto, and Spectrum.
- Islands layout, Contrast variants, and Teal and Rose accents.
- Matching No Italics editor schemes.
- Coordinated diff and console colors, plus optional palette-matched semantic highlighting.
- Explicitly registered editor schemes to prevent missing-scheme errors after
  project or plugin reloads, including Spectrum Rose.
- Match common Symbols file and folder icon colors to the selected Emberkai
  palette, including selected rows in the Project panel.
