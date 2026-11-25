"""Derive the Emberkai Light Contrast schemes from Emberkai Light.

Every text color below its target is darkened along its own hue (HLS lightness only) until it
reaches the target against both the editor and caret-row backgrounds: 4.6:1 for code, 5.4:1 for
bright ANSI colors, and 5.6:1 for comments, line numbers, and semantic highlighting stops.
Backgrounds, effects drawn in other colors, and invisible placeholders keep Light's colors.

Run after changing emberkai-light.xml, then copy any new icon colors into
emberkai-light-contrast.theme.json:

    python3 scripts/light_contrast.py src/main/resources/schemes/emberkai-light.xml
"""
import colorsys, re, sys, pathlib, json

def lum(h):
    h = h.lstrip('#')[:6]; c = [int(h[i:i+2], 16) / 255 for i in (0, 2, 4)]
    c = [x / 12.92 if x <= 0.04045 else ((x + 0.055) / 1.055) ** 2.4 for x in c]
    return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2]
def cr(a, b):
    la, lb = lum(a), lum(b); return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)

BG, CARET = '#faf4f2', '#f4eeeb'
CODE, STRONG = 4.6, 5.6
cache = {}
def darken(hexcolor, target):
    """Lower HLS lightness, keeping hue and saturation, until both backgrounds reach the target."""
    raw = hexcolor.lstrip('#'); alpha = raw[6:]; base = raw[:6].lower()
    key = (base, target)
    if key not in cache:
        r, g, b = (int(base[i:i+2], 16) / 255 for i in (0, 2, 4))
        h, l, s = colorsys.rgb_to_hls(r, g, b)
        out = base
        while min(cr(out, BG), cr(out, CARET)) < target:
            l -= 0.004
            rr, gg, bb = colorsys.hls_to_rgb(h, max(l, 0), s)
            out = '%02x%02x%02x' % tuple(round(x * 255) for x in (rr, gg, bb))
        cache[key] = out
    return '#' + cache[key] + alpha

src = pathlib.Path(sys.argv[1]); out_dir = src.parent
text = src.read_text()
text = text.replace('<scheme name="Emberkai Light"', '<scheme name="Emberkai Light Contrast"', 1)
colors_start, colors_end = text.index('<colors>'), text.index('</colors>')
attrs_start = text.index('<attributes>')

# Text colors in the colors section.
STRONG_COLORS = {'LINE_NUMBERS_COLOR'}
TEXT_COLORS = re.compile(r'FILESTATUS_.*|VCS_ANNOTATIONS_COLOR_\d|DOC_COMMENT_LINK|JUPYTER\.GUTTER_.*_EXECUTION_COUNT|ANNOTATIONS_COLOR|LINE_NUMBER_ON_CARET_ROW_COLOR')
def fix_color(m):
    name, value = m.group(1), m.group(2)
    if name in STRONG_COLORS: return m.group(0).replace(value, darken(value, STRONG))
    if TEXT_COLORS.fullmatch(name): return m.group(0).replace(value, darken(value, CODE))
    return m.group(0)
colors = re.sub(r'<option name="([^"]+)" value="(#[0-9a-fA-F]{6,8})" />', fix_color, text[colors_start:colors_end])

# Foregrounds drawn on the editor background. Comments and semantic stops get the stronger target.
STRONG_ATTRS = re.compile(r'DEFAULT_(LINE|BLOCK|DOC)_COMMENT|RAINBOW_COLOR\d')
attrs = text[attrs_start:]
# ANSI bright colors and gray keep their order against the normal ones once darkened.
TARGETS = {'CONSOLE_GRAY_OUTPUT': 5.2}
INVISIBLE = {'INVISIBLE_INLAY_BUTTON', 'COMPOSE_INVISIBLE_PORT_BUTTON'}
def fix_attr(m):
    name, body = m.group(1), m.group(2)
    if 'BACKGROUND' in body or 'baseAttributes' in m.group(0) or name in INVISIBLE: return m.group(0)
    target = TARGETS.get(name, STRONG if STRONG_ATTRS.fullmatch(name) else 5.4 if re.fullmatch(r'CONSOLE_\w+_BRIGHT_OUTPUT', name) else CODE)
    fg = re.search(r'name="FOREGROUND" value="(#?[0-9a-fA-F]{6,8})"', body)
    if not fg: return m.group(0)
    old, new = fg.group(1), darken('#' + fg.group(1).lstrip('#'), target)
    body = body.replace('name="FOREGROUND" value="%s"' % old, 'name="FOREGROUND" value="%s"' % new)
    # Underlines drawn in the text color follow it; other effects keep their color.
    body = body.replace('name="EFFECT_COLOR" value="%s"' % old, 'name="EFFECT_COLOR" value="%s"' % new)
    return '<option name="%s">%s</option>' % (name, body)
attrs = re.sub(r'<option name="([^"]+)">(.*?)</option>', fix_attr, attrs, flags=re.S)
result = text[:colors_start] + colors + text[colors_end:attrs_start] + attrs
(out_dir / 'emberkai-light-contrast.xml').write_text(result)

plain = result.replace('<scheme name="Emberkai Light Contrast"', '<scheme name="Emberkai Light Contrast - No Italics"', 1)
head, tail = plain[:plain.index('<attributes>')], plain[plain.index('<attributes>'):]
tail = re.sub(r'(<option name="FONT_TYPE" value=")([23])("\s*/>)', lambda m: m.group(1) + str(int(m.group(2)) - 2) + m.group(3), tail)
(out_dir / 'emberkai-light-contrast-no-italics.xml').write_text(head + tail)

mapping = {f'#{k[0]}@{k[1]}': '#' + v for k, v in sorted(cache.items()) if k[0] != v}
print(json.dumps(mapping, indent=1))
