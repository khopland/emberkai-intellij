# Third-party plugins

Emberkai colors every key that IntelliJ's bundled plugins add to Darcula and
Default, and `validateThemes` fails when a new IDE version adds one. Plugins
from the Marketplace are not bundled, so they are listed here.

## Themed

- **[Rainbow Brackets](https://plugins.jetbrains.com/plugin/10080-rainbow-brackets)**:
  all ten levels of round, square, curly, and angle brackets, and the rainbow
  indent guides, cycle through the palette: yellow, violet, cyan, green, orange,
  rose, then yellow again. Light uses its darker versions of the same hues,
  and Light Contrast darkens them further to match its syntax colors.
  Rainbow variables use the IDE's
  [semantic highlighting](semantic-highlighting.md) colors, which Emberkai sets.
- **[Symbols Icons](https://github.com/joansgarcia/jetbrains-symbols)**: see
  [icon color support](icon-colors.md).

## Configured in the plugin

These plugins store their colors in their own settings, so a theme cannot
change them. Use the palette values below to match Emberkai.

| Role | Emberkai (dark) | Emberkai Light |
| --- | --- | --- |
| Background | `#222222` | `#faf4f2` |
| Text | `#f7f1ff` | `#29242a` |
| Muted | `#8b888f` | `#706b6e` |
| Yellow | `#fce566` | `#c27800` |
| Orange | `#fd9353` | `#e74e10` |
| Rose | `#fc618d` | `#e1346e` |
| Green | `#7bd88f` | `#00935e` |
| Cyan | `#5ad4e6` | `#0086a3` |
| Violet | `#948ae3` | `#7459c8` |

- **IdeaVim**: the mode widget in the status bar has custom colors per mode.
  For example, Normal yellow, Insert green, Visual violet, Replace rose, and
  Command cyan, each with the background color as text. The highlighted yank
  extension takes a color in `.ideavimrc`:
  `let g:highlightedyank_highlight_color = "rgba(252, 229, 102, 60)"`.
- **AceJump**: under **Settings → Tools → AceJump**, use yellow for jump tags,
  with the background color for their text.
- **Indent Rainbow**: choose the custom palette and enter the hues above. The
  plugin blends them with the editor background itself.
