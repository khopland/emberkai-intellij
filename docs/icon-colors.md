# Icon colors

Emberkai recolors the common SVG colors used by
[Symbols Icons](https://github.com/joansgarcia/jetbrains-symbols).
Install Symbols Icons from **Settings → Plugins → Marketplace**, then choose an
Emberkai theme under **Appearance & Behavior → Appearance**. Emberkai supplies
the colors; Symbols supplies the icon shapes and file/folder associations.
Symbols is optional and is not bundled with Emberkai.

Each base theme defines an `icons.ColorPalette` map. The map replaces Symbols'
red and pink with the palette's rose, orange with orange, yellow with yellow,
green with green, blue with the console blue, sky and teal with cyan, and purple
with violet. Neutral file and folder icons use `icon-default-stroke`.
The matching `iconColorsOnSelection` map keeps those colors when a file or
folder is selected. JetBrains renders selected icons with a separate patcher,
so both maps are needed. Selection map keys use lowercase SVG color values.
The source colors come from the Symbols
[upstream icon set at its pinned revision](https://github.com/miguelsolorio/vscode-symbols/tree/296ef1b62287fb2315cb5651e552e09e8c8e1de8/src/icons).

Contrast variants inherit the same hue mappings and use their brighter neutral
icon color. Teal and Rose variants inherit the base palette's icon colors, so
changing the UI accent does not change the meaning of file icon colors.
Selecting and deselecting a row keeps its Emberkai icon color.

JetBrains applies these replacements to SVG fills and strokes throughout the
IDE, including other plugins whose icons use the same source colors. They are
active whenever Emberkai is selected, whether Symbols is installed or not.
Colors outside the map keep their existing appearance. This covers common
Symbols colors, rather than every color in every logo, and future Symbols
versions may introduce additional colors.

The theme does not replace icon shapes or add file associations. Restricting
the color replacements to Symbols would require a separate implementation;
the theme JSON mappings are global. See
[JetBrains' icon customization documentation](https://plugins.jetbrains.com/docs/intellij/themes-customize.html#overriding-the-global-color-of-icons).
