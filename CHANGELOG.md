# Changelog

## [Unreleased]

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
