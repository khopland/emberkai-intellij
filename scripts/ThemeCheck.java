import com.intellij.ide.ui.UITheme;
import com.intellij.ide.ui.UIThemeBean;
import com.intellij.ide.ui.UIThemeBeanKt;
import com.intellij.ide.ui.ColorMapKt;
import com.intellij.codeHighlighting.RainbowHighlighter;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.colors.TextAttributesScheme;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.options.SchemeMetaInfo;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.ExperimentalUI;
import com.intellij.ui.svg.SvgKt;
import com.intellij.util.SVGLoader;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import javax.swing.UIDefaults;

class ThemeCheck {
  static final ClassLoader LOADER = UITheme.class.getClassLoader();
  static final Map<UITheme, UIThemeBean> BEANS = new IdentityHashMap<>();
  static final Function2<String, Throwable, Unit> WARN = (message, error) -> { throw new AssertionError(message, error); };
  static UIThemeBean read(byte[] data) throws Exception {
    Object parser;
    try {
      Class<?> factoryType = Class.forName("tools.jackson.core.json.JsonFactory");
      Class<?> contextType = Class.forName("tools.jackson.core.ObjectReadContext");
      Object factory = factoryType.getConstructor().newInstance();
      Object context = contextType.getMethod("empty").invoke(null);
      parser = factoryType.getMethod("createParser", contextType, byte[].class).invoke(factory, context, data);
    } catch (ClassNotFoundException oldJackson) {
      Class<?> factoryType = Class.forName("com.fasterxml.jackson.core.JsonFactory");
      Object factory = factoryType.getConstructor().newInstance();
      parser = factoryType.getMethod("createParser", byte[].class).invoke(factory, (Object)data);
    }
    var readTheme = Arrays.stream(UIThemeBeanKt.class.getMethods()).filter(method -> method.getName().equals("readTheme")).findFirst().orElseThrow();
    UIThemeBean bean = (UIThemeBean)readTheme.invoke(null, parser, WARN);
    ((AutoCloseable)parser).close();
    return bean;
  }
  static UITheme load(UITheme parent, byte[] data, String id) throws Exception {
    UIThemeBean bean = read(data);
    if (parent != null) UIThemeBeanKt.importFromParentTheme(bean, BEANS.get(parent));
    ColorMapKt.initializeNamedColors(bean, WARN);
    UITheme theme = new UITheme(id, bean, null, null, null, LOADER);
    BEANS.put(theme, bean);
    return theme;
  }
  static UITheme builtin(UITheme parent, String resource, String id) throws Exception {
    try (InputStream stream = LOADER.getResourceAsStream(resource)) {
      if (stream == null) throw new AssertionError(resource);
      return load(parent, stream.readAllBytes(), id);
    }
  }
  static void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
  static double luminance(Color color) {
    double[] weights = {0.2126, 0.7152, 0.0722};
    int[] channels = {color.getRed(), color.getGreen(), color.getBlue()};
    double result = 0;
    for (int i = 0; i < 3; i++) {
      double value = channels[i] / 255.0;
      result += weights[i] * (value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4));
    }
    return result;
  }
  static double contrast(Color a, Color b) {
    double first = luminance(a), second = luminance(b);
    return (Math.max(first, second) + 0.05) / (Math.min(first, second) + 0.05);
  }
  static double distance(Color a, Color b) {
    return Math.hypot(Math.hypot(a.getRed() - b.getRed(), a.getGreen() - b.getGreen()), a.getBlue() - b.getBlue());
  }
  static Color color(UIDefaults defaults, String key) {
    Color result = defaults.getColor(key);
    check(result != null, "Missing color: " + key + " = " + defaults.get(key));
    return result;
  }
  static final Map<String,UITheme> THEMES = new HashMap<>();
  static final Map<String,String[]> PROVIDERS = new LinkedHashMap<>();
  static final Set<String> LOADING = new HashSet<>();
  static Path RESOURCES;
  static UITheme provider(String id) throws Exception {
    if (THEMES.containsKey(id)) return THEMES.get(id);
    check(LOADING.add(id), "Cyclic theme inheritance: " + id);
    String[] data = PROVIDERS.get(id);
    check(data != null, "Missing theme parent: " + id);
    String parent = data[2].isEmpty() ? (String)com.intellij.ide.ui.laf.UiThemeProviderListManager.class.getField("DEFAULT_DARK_PARENT_THEME").get(null) : data[2];
    UITheme result = load(provider(parent), Files.readAllBytes(RESOURCES.resolve(data[1])), id);
    LOADING.remove(id);
    THEMES.put(id,result);
    return result;
  }
  static org.jdom.Element xml(Path path) throws Exception {
    return com.intellij.openapi.util.JDOMUtil.load(Files.readAllBytes(path));
  }
  static Map<String,com.intellij.openapi.editor.markup.TextAttributes> attributes(org.jdom.Element root) {
    var result = new HashMap<String,com.intellij.openapi.editor.markup.TextAttributes>();
    for (org.jdom.Element option : root.getChild("attributes").getChildren("option")) {
      result.put(option.getAttributeValue("name"),new com.intellij.openapi.editor.colors.impl.TextAttributesReader().readAttributes(option.getChild("value"), null));
    }
    return result;
  }
  static Map<String,Color> colors(org.jdom.Element root) {
    var result = new HashMap<String,Color>();
    for (org.jdom.Element option : root.getChild("colors").getChildren("option")) {
      result.put(option.getAttributeValue("name"),new com.intellij.openapi.editor.colors.impl.TextAttributesReader().read(Color.class, option));
    }
    return result;
  }

  static final Set<String> JETBRAINS_COLORS = Set.of(
    "3574f0", "3573f0", "3871e1", "2f5eb9", "548af7", "3671e5", "366acf", "35538f", "375fad", "1d2336");
  // Licensing, profiler, and retired plugin UI that Emberkai leaves to the IDE.
  static final String VENDOR_KEYS =
    "(Profiler|LineProfiler|CompilationCharts|CodeWithMe|Recap|TrialWidget|PackageSearch|FlameGraph|"
    + "Managed|UnattendedHostStatus|LicenseDialog|NuGet)\\b.*";
  static final Set<String> FOREIGN_COLORS = Set.of(
    "f59762", "f78c6c", "f65f87", "87cefa", "ffd700", "da70d6", "e91e63", "c1c0c0", "939393",
    "b7b6b6", "848383", "59575a", "4b1515", "264b33", "12404b", "3b514d", "4d6168", "9373a5",
    "c7c8f5", "219598", "39adb5", "c4b3a3", "536c46", "fcfafa", "646877", "a2a2a2", "727272",
    "808080", "dbdbdb", "404040", "fffbf7", "19181a", "7867e7", "5789dd");

  static final java.util.regex.Pattern HEX_COLOR = java.util.regex.Pattern.compile("#?[0-9a-fA-F]{6}([0-9a-fA-F]{2})?");
  static final java.util.regex.Pattern DARCULA_ADDITION = java.util.regex.Pattern.compile("<additionalTextAttributes\\b[^>]*>");
  static final java.util.regex.Pattern ADDITION_FILE = java.util.regex.Pattern.compile("\\bfile=\"/?([^\"]+)\"");

  static void collectColored(org.jdom.Element root, String origin, Map<String,String> result) {
    var options = new ArrayList<org.jdom.Element>(root.getChildren("option"));
    for (var section : List.of("colors", "attributes"))
      if (root.getChild(section) != null) options.addAll(root.getChild(section).getChildren("option"));
    for (var option : options) {
      if (option.getAttributeValue("baseAttributes") != null) continue;
      var values = option.getChild("value") == null ? List.of(option) : option.getChild("value").getChildren("option");
      for (var value : values) {
        String field = value == option ? "value" : value.getAttributeValue("name");
        String color = value.getAttributeValue("value");
        if (color != null && HEX_COLOR.matcher(color).matches()
            && (value == option || field.endsWith("COLOR") || field.equals("FOREGROUND") || field.equals("BACKGROUND")))
          result.putIfAbsent(option.getAttributeValue("name"), origin);
      }
    }
  }

  // Keys the Darcula base scheme and bundled plugins' Darcula additions color explicitly. Emberkai
  // extends Darcula, so any of these it does not redefine shows Darcula's colors.
  static Map<String,String> darculaColoredKeys(Path ide) throws Exception {
    var result = new TreeMap<String,String>();
    try (InputStream stream = LOADER.getResourceAsStream("DefaultColorSchemesManager.xml")) {
      check(stream != null, "Missing DefaultColorSchemesManager.xml");
      for (var scheme : com.intellij.openapi.util.JDOMUtil.load(stream.readAllBytes()).getChildren("scheme"))
        if ("Darcula".equals(scheme.getAttributeValue("name"))) collectColored(scheme, "Darcula", result);
    }
    // Plugins may register an addition in one jar and ship the file in another jar of the same plugin.
    var groups = new TreeMap<Path,List<java.util.zip.ZipFile>>();
    try (var files = Files.walk(ide)) {
      for (Path jar : files.filter(p -> p.toString().endsWith(".jar")).sorted().toList()) {
        Path relative = ide.relativize(jar);
        if (!relative.startsWith("lib") && !relative.startsWith("plugins")) continue;
        Path group = relative.startsWith("plugins") ? relative.subpath(0, 2) : relative.subpath(0, 1);
        try { groups.computeIfAbsent(group, g -> new ArrayList<>()).add(new java.util.zip.ZipFile(jar.toFile())); }
        catch (java.io.IOException notAnArchive) { }
      }
    }
    try {
      for (var jars : groups.values()) {
        for (var jar : jars) {
          for (var entry : Collections.list(jar.entries())) {
            String name = entry.getName();
            if (!name.endsWith(".xml") || (name.contains("/") && !name.startsWith("META-INF/"))) continue;
            String descriptor = new String(jar.getInputStream(entry).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            if (!descriptor.contains("additionalTextAttributes")) continue;
            var tags = DARCULA_ADDITION.matcher(descriptor);
            while (tags.find()) {
              if (!tags.group().contains("scheme=\"Darcula\"")) continue;
              var fileMatch = ADDITION_FILE.matcher(tags.group());
              check(fileMatch.find(), "Darcula addition without file: " + tags.group());
              String file = fileMatch.group(1).endsWith(".xml") ? fileMatch.group(1) : fileMatch.group(1) + ".xml";
              // Prefer the registering jar, then its plugin; split frontend modules use platform files.
              var owner = java.util.stream.Stream.of(java.util.stream.Stream.of(jar), jars.stream(),
                  groups.values().stream().flatMap(List::stream)).flatMap(s -> s)
                .filter(candidate -> candidate.getEntry(file) != null).findFirst();
              check(owner.isPresent(), "Missing Darcula addition: " + jar.getName() + " / " + file);
              collectColored(com.intellij.openapi.util.JDOMUtil.load(owner.get().getInputStream(owner.get().getEntry(file)).readAllBytes()), file, result);
            }
          }
        }
      }
    } finally {
      for (var jars : groups.values()) for (var jar : jars) jar.close();
    }
    check(result.size() > 300, "Darcula scheme keys not found: " + result.size());
    return result;
  }

  static Map<String,Color> schemeColors(org.jdom.Element root) {
    var result = new HashMap<String,Color>();
    colors(root).forEach((key, value) -> { if (value != null) result.put(key, value); });
    attributes(root).forEach((key, value) -> {
      if (value == null) return;
      if (value.getForegroundColor() != null) result.put(key + ".foreground", value.getForegroundColor());
      if (value.getBackgroundColor() != null) result.put(key + ".background", value.getBackgroundColor());
      if (value.getEffectColor() != null) result.put(key + ".effect", value.getEffectColor());
      if (value.getErrorStripeColor() != null) result.put(key + ".stripe", value.getErrorStripeColor());
    });
    return result;
  }

  // Use the real semantic keys and gradient generator without starting an IDE.
  // A lightweight scheme avoids application services used to adjust colors
  // around language-specific diagnostics in a running editor.
  record SemanticScheme(Map<String,TextAttributes> attributes, Properties getMetaProperties)
      implements TextAttributesScheme, SchemeMetaInfo {
    public TextAttributes getAttributes(TextAttributesKey key) {
      return attributes.get(key.getExternalName());
    }
  }

  static void checkSemanticRange(Path path) throws Exception {
    var root = xml(path);
    var attrs = attributes(root);
    var properties = new Properties();
    if (root.getChild("metaInfo") != null) {
      for (var entry : root.getChild("metaInfo").getChildren("property"))
        properties.setProperty(entry.getAttributeValue("name"), entry.getText());
    }
    var scheme = new SemanticScheme(attrs, properties);
    check(!RainbowHighlighter.isRainbowEnabledWithInheritance(scheme, Language.ANY),
          "Semantic highlighting must remain opt-in: " + path);
    check(properties.stringPropertyNames().stream().noneMatch(RainbowHighlighter::isRainbowKey),
          "Do not override language-specific semantic preferences: " + path);
    check(attrs.keySet().stream().noneMatch(k -> k.startsWith("RAINBOW_TEMP_")),
          "Do not persist IDE-generated semantic colors: " + path);
    for (var key : RainbowHighlighter.RAINBOW_COLOR_KEYS) {
      var value = attrs.get(key.getExternalName());
      check(value != null && value.getForegroundColor() != null && value.getFontType() == 0,
            "Plain semantic color stop: " + path + " / " + key);
    }
    Color[] generated = RainbowHighlighter.testRainbowGenerateColors(scheme);
    check(generated.length == 21 && new HashSet<>(List.of(generated)).size() == 21,
          "Distinct interpolated semantic colors: " + path);
    Color background = attrs.get("TEXT").getBackgroundColor();
    Color caretRow = colors(root).get("CARET_ROW_COLOR");
    double threshold = path.getFileName().toString().contains("-contrast") ? 5.5 : 4.5;
    for (Color value : generated)
      check(contrast(value, background) >= threshold && contrast(value, caretRow) >= threshold,
            "Semantic range readability: " + path);
    // Check the native on/off toggle against a copy of this scheme's metadata.
    RainbowHighlighter.setRainbowEnabled(scheme, null, true);
    check(RainbowHighlighter.isRainbowEnabledWithInheritance(scheme, Language.ANY), "Enable semantics");
    RainbowHighlighter.setRainbowEnabled(scheme, null, false);
    check(!RainbowHighlighter.isRainbowEnabledWithInheritance(scheme, Language.ANY), "Disable semantics");
  }

  static void checkIconPalette(UITheme theme, Map<String,TextAttributes> attrs, String family) throws Exception {
    UIThemeBean bean = BEANS.get(theme);
    var colors = bean.colorMap.map;
    for (var entry : Map.of("icon-yellow", "DEFAULT_STRING", "icon-blue", "CONSOLE_BLUE_OUTPUT",
                            "icon-cyan", "DEFAULT_METADATA", "icon-purple", "DEFAULT_NUMBER").entrySet()) {
      check(attrs.get(entry.getValue()).getForegroundColor().equals(colors.get(entry.getKey())),
            "Icon color follows palette: " + theme.getName() + " / " + entry.getKey());
    }

    // Inheritance is already resolved above. Select the New UI scope without
    // starting application services to look up the parent theme again.
    UIThemeBean iconTheme = new UIThemeBean();
    iconTheme.name = "Dark";
    iconTheme.dark = bean.dark;
    iconTheme.colorMap = bean.colorMap;
    iconTheme.icons = bean.icons;
    iconTheme.iconColorOnSelectionMap = bean.iconColorOnSelectionMap;
    var factory = Arrays.stream(Class.forName("com.intellij.ide.ui.UIThemeKt").getDeclaredMethods())
      .filter(method -> method.getName().equals("createTheme")).findFirst().orElseThrow();
    factory.setAccessible(true);
    var nativeTheme = (UITheme)factory.invoke(null, iconTheme, null, LOADER, null, theme.getId(), WARN);
    var provider = nativeTheme.getColorPatcher();
    check(provider != null, "Missing icon color patcher: " + theme.getName());
    var selectedProvider = nativeTheme.selectionColorPatcher;
    check(selectedProvider != null, "Missing selected icon color patcher: " + theme.getName());
    checkIconColor(selectedProvider, "/symbols/icons/files/xml.svg", "#EA580C", colors.get("text-warning"));
    var mappings = (Map<?,?>)bean.icons.get("ColorPalette");
    for (var entry : mappings.entrySet()) {
      // Parent themes also contribute palette entries for built-in controls.
      if (!entry.getKey().toString().startsWith("#")) continue;
      Color expected = colors.get(entry.getValue());
      check(expected != null, "Unknown named icon color: " + entry);
      checkIconColor(provider, "/symbols/icons/files/java.svg", entry.getKey().toString(), expected);
      checkIconColor(provider, "/symbols/icons/folders/folder.svg", entry.getKey().toString(), expected);
    }
    var base = read(Files.readAllBytes(RESOURCES.resolve("islands/" + family + ".theme.json")));
    var symbolsMappings = (Map<?,?>)base.icons.get("ColorPalette");
    for (var entry : symbolsMappings.entrySet()) {
      Color expected = colors.get(entry.getValue());
      checkIconColor(selectedProvider, "/symbols/icons/files/java.svg", entry.getKey().toString(), expected);
      checkIconColor(selectedProvider, "/symbols/icons/folders/folder.svg", entry.getKey().toString(), expected);
    }
    // Built-in selected icons must keep their inherited selection colors too.
    for (var entry : bean.iconColorOnSelectionMap.map.entrySet())
      checkIconColor(selectedProvider, "/actions/test.svg", entry.getKey(), entry.getValue());
    checkIconColor(provider, "/symbols/icons/files/java.svg", "#F87171", attrs.get("DEFAULT_KEYWORD").getForegroundColor());
    checkIconColor(provider, "/symbols/icons/files/js.svg", "#F59E0B", attrs.get("DEFAULT_STRING").getForegroundColor());
    checkIconColor(provider, "/symbols/icons/files/code-blue.svg", "#2563EB", attrs.get("CONSOLE_BLUE_OUTPUT").getForegroundColor());
    checkIconColor(provider, "/symbols/icons/files/code-green.svg", "#16A34A", attrs.get("DEFAULT_FUNCTION_DECLARATION").getForegroundColor());
    checkIconColor(provider, "/symbols/icons/folders/folder.svg", "#64748B", colors.get("icon-default-stroke"));
    checkIconColor(provider, "/symbols/icons/files/claude.svg", "#D97656", Color.decode("#D97656"));
    checkIconColor(selectedProvider, "/symbols/icons/files/claude.svg", "#D97656", Color.decode("#D97656"));
    checkIconColor(provider, "/other-plugin/icon.svg", "#F87171", attrs.get("DEFAULT_KEYWORD").getForegroundColor());
    checkIconSelectionPainting(nativeTheme, colors.get("text-warning"));
  }

  static void checkIconSelectionPainting(UITheme theme, Color expected) throws Exception {
    Path fixture = Files.createTempFile("emberkai-selected-icon-", ".svg");
    var previousProvider = SVGLoader.getColorPatcherProvider();
    try {
      Files.writeString(fixture, """
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16">
          <rect x="2" y="2" width="12" height="12" fill="#EA580C"/>
        </svg>
        """);
      IconLoader.activate();
      SVGLoader.setColorPatcherProvider(theme.getColorPatcher());
      var icon = IconLoader.findIcon(fixture.toUri().toURL(), true);
      check(icon != null, "Load selection fixture");
      // The selected renderer calls colorPatchedIcon, replacing the normal
      // patcher. Paint the cached icon before, during, and after selection.
      for (var state : List.of(icon, SvgKt.colorPatchedIcon(icon, theme.selectionColorPatcher), icon)) {
        var image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        try {
          state.paintIcon(null, graphics, 0, 0);
        } finally {
          graphics.dispose();
        }
        check(image.getRGB(8, 8) == expected.getRGB(), "Preserve rendered icon color on selection: " + theme.getId());
      }
    } finally {
      SVGLoader.setColorPatcherProvider(previousProvider);
      Files.deleteIfExists(fixture);
    }
  }

  static void checkIconColor(SVGLoader.SvgElementColorPatcherProvider provider, String path, String source, Color expected) {
    var attributes = new HashMap<String,String>();
    attributes.put("fill", source);
    attributes.put("stroke", source);
    attributes.put("stroke-width", "2");
    var patcher = provider.attributeForPath(path);
    check(patcher != null, "Missing SVG patcher: " + path);
    patcher.patchColors(attributes);
    check(expected.equals(Color.decode(attributes.get("fill"))) && expected.equals(Color.decode(attributes.get("stroke"))),
          "SVG fill/stroke mapping: " + path + " / " + source + " -> " + attributes);
    check("2".equals(attributes.get("stroke-width")), "Preserve SVG geometry: " + path);
  }

  public static void main(String[] args) throws Exception {
    check(ExperimentalUI.isNewUI(), "Validate the same New UI defaults used by the IDE");
    RESOURCES = Path.of(args[0]);
    UITheme darcula = builtin(null,"themes/darcula.theme.json","Darcula");
    UITheme dark = builtin(darcula,"themes/expUI/expUI_dark.theme.json","ExperimentalDark");
    UITheme islands = builtin(dark,"themes/islands/ManyIslandsDark.theme.json","Islands Dark");
    THEMES.put("Darcula",darcula); THEMES.put("ExperimentalDark",dark); THEMES.put("Islands Dark",islands);
    var descriptor = xml(RESOURCES.resolve("META-INF/plugin.xml"));
    Set<String> paths = new HashSet<>(), names = new HashSet<>();
    for (var entry : descriptor.getChild("extensions").getChildren("themeProvider")) {
      String id = entry.getAttributeValue("id");
      String path = entry.getAttributeValue("path").substring(1);
      check(paths.add(path), "Duplicate theme path: " + path);
      UIThemeBean bean = read(Files.readAllBytes(RESOURCES.resolve(path)));
      check(names.add(bean.name), "Duplicate theme name: " + bean.name);
      check(bean.dark, "Expected dark theme: " + path);
      check(PROVIDERS.put(id, new String[]{id, path, bean.parentTheme == null ? "" : bean.parentTheme}) == null, "Duplicate provider id: " + id);
    }
    Set<String> expectedPaths = new HashSet<>();
    for (String variant : List.of("", "-contrast", "-teal", "-rose", "-ember"))
      expectedPaths.add("islands/emberkai" + variant + ".theme.json");
    check(paths.equals(expectedPaths), "Complete Islands preset matrix");
    try (var files = Files.walk(RESOURCES)) {
      Set<String> actual = new HashSet<>(files.filter(p -> p.toString().endsWith(".theme.json")).map(p -> RESOURCES.relativize(p).toString().replace('\\', '/')).toList());
      check(actual.equals(paths), "Unregistered or missing theme resources");
    }
    Map<String,String> bundledSchemes = new HashMap<>();
    Set<String> schemeNames = new HashSet<>();
    for (var entry : descriptor.getChild("extensions").getChildren("bundledColorScheme")) {
      String path = entry.getAttributeValue("path") + ".xml";
      String name = entry.getAttributeValue("id");
      var scheme = xml(RESOURCES.resolve(path));
      check(name != null && name.equals(scheme.getAttributeValue("name")), "Scheme registration name: " + path);
      check(bundledSchemes.put(path, name) == null, "Duplicate scheme path: " + path);
      check(schemeNames.add(name), "Duplicate scheme name: " + name);
    }
    int contrastThemes=0, accentThemes=0, islandThemes=0;
    String[] ansi={"BLACK","RED","GREEN","YELLOW","BLUE","MAGENTA","CYAN","GRAY","DARKGRAY","RED_BRIGHT","GREEN_BRIGHT","YELLOW_BRIGHT","BLUE_BRIGHT","MAGENTA_BRIGHT","CYAN_BRIGHT","WHITE"};
    for(String id:PROVIDERS.keySet()) {
      UITheme theme=provider(id);
      String file=Path.of(PROVIDERS.get(id)[1]).getFileName().toString();
      String family=file.replace(".theme.json","").split("-")[0];
      boolean isContrast=file.contains("-contrast");
      boolean isAccent=file.contains("-teal")||file.contains("-rose")||file.contains("-ember");
      UIDefaults defaults=new UIDefaults(); theme.applyTheme(defaults);
      String schemePath = theme.getEditorSchemeId().substring(1);
      var scheme=xml(RESOURCES.resolve(schemePath));
      // Project/plugin reloads must be able to load the scheme independently of
      // theme discovery, which replaces the theme's XML path with its scheme name.
      check(scheme.getAttributeValue("name").equals(bundledSchemes.get(schemePath)),
            "Theme scheme must be explicitly bundled for reloads: " + theme.getName() + " / " + schemePath);
      var attrs=attributes(scheme); var colors=colors(scheme);
      checkIconPalette(theme, attrs, family);
      var original=attributes(xml(RESOURCES.resolve("schemes/"+family+".xml")));
      Color background=attrs.get("TEXT").getBackgroundColor();
      for (String key : List.of("STATIC_METHOD_ATTRIBUTES", "STATIC_METHOD_IMPORTED_ATTRIBUTES"))
        check(attrs.get("DEFAULT_STATIC_METHOD").equals(attrs.get(key)), "Java static methods follow palette/font style: " + key);
      check(background.equals(original.get("TEXT").getBackgroundColor()),"Preserved background: "+theme.getName());
      for (String key : original.keySet()) {
        if (key.startsWith("DEFAULT_") && !key.contains("COMMENT"))
          check(original.get(key).equals(attrs.get(key)), "Preserved syntax: " + theme.getName() + " / " + key);
      }
      {
        for(String key:List.of("DIFF_INSERTED","DIFF_DELETED","DIFF_MODIFIED","DIFF_CONFLICT")) {
          var value=attrs.get(key);
          check(value.getBackgroundColor()!=null&&value.getForegroundColor()!=null&&value.getErrorStripeColor()!=null,"Diff fields: "+key);
          check(contrast(value.getBackgroundColor(),attrs.get("TEXT").getForegroundColor())>=4.5,"Diff readability");
        }
        Set<Integer> consoleColors=new HashSet<>();
        for(String color:ansi) consoleColors.add(attrs.get("CONSOLE_"+color+"_OUTPUT").getForegroundColor().getRGB());
        check(consoleColors.size()==16,"Distinct ANSI colors: "+theme.getName());
        check(!attrs.containsKey("CONSOLE_OUTPUT"), "Console must inherit dark background");
        Color blue = attrs.get("CONSOLE_BLUE_OUTPUT").getForegroundColor();
        check(blue.getBlue() > blue.getRed() && blue.getBlue() > blue.getGreen(), "ANSI blue hue");
        check(colors.get("MODIFIED_LINES_COLOR").equals(attrs.get("DIFF_MODIFIED").getErrorStripeColor()), "Matching diff/gutter color");
      }
      if(defaults.getInt("Islands")==1) {
        var palette=BEANS.get(theme).colorMap.map;
        check(color(defaults,"ToolWindow.Button.selectedBackground").equals(palette.get("accent")),"Selected tool window uses preset accent: "+theme.getName());
        check(color(defaults,"ToolWindow.Button.selectedForeground").equals(palette.get("surface")),"Selected tool window icon uses contrasting foreground: "+theme.getName());
        check(defaults.getInt("Island.arc")==20&&defaults.getInt("Island.arc.compact")==16,"Native geometry");
        check(color(defaults,"Island.borderColor").equals(color(defaults,"ToolWindow.background")),"Panel edge");
        // Stock Islands Dark separates islands from the frame at about 1.18:1.
        check(contrast(color(defaults,"ToolWindow.background"),color(defaults,"MainWindow.background"))>=1.15,"Panel separation");
        check(contrast(color(defaults,"EditorTabs.underlinedTabBackground"),color(defaults,"EditorTabs.underlinedTabForeground"))>=4.5,"Tabs");
        check(contrast(color(defaults,"Button.default.startBackground"),color(defaults,"Button.default.foreground"))>=4.5,"Buttons");
        check(contrast(color(defaults,"ToolWindow.Button.selectedBackground"),color(defaults,"ToolWindow.Button.selectedForeground"))>=4.5,"Selected tool windows: "+theme.getName());
        // Filled status controls (run/stop, plugin restart) use palette hues, not Islands green/red.
        check(color(defaults,"RunWidget.runningBackground").equals(palette.get("text-success"))
              && color(defaults,"RunWidget.stopBackground").equals(palette.get("text-error"))
              && color(defaults,"Plugins.Button.installFillBackground").equals(palette.get("text-success")),
              "Status controls follow palette: "+theme.getName());
        for (String[] pair : new String[][]{{"Plugins.Button.installFillBackground","Plugins.Button.installFillForeground"},
                                            {"RunWidget.runningBackground","RunWidget.runningIconColor"},
                                            {"RunWidget.stopBackground","RunWidget.runningIconColor"}})
          check(contrast(color(defaults,pair[0]),color(defaults,pair[1]))>=4.5,"Status control readability: "+theme.getName()+" / "+pair[0]);
        var islandsPalette = BEANS.get(THEMES.get("Islands Dark")).colorMap.map;
        // The near-white 130-160 steps carry almost no hue and may coincide.
        for (var entry : islandsPalette.entrySet())
          if (entry.getKey().matches("(gray|blue|green|red|yellow|orange|purple|teal)-([1-9]0|1[0-2]0)"))
            check(!entry.getValue().equals(palette.get(entry.getKey())),"Islands color ramp follows palette: "+theme.getName()+" / "+entry.getKey());
        check(contrast(palette.get("text-muted"),palette.get("surface"))>=4.5,"Muted text: "+theme.getName());
        // JetBrains' brand blue and its navy tints must not leak through the palette.
        var leaks = new TreeSet<String>();
        for (Object key : Collections.list(defaults.keys())) {
          if (!(key instanceof String name) || name.matches(VENDOR_KEYS)) continue;
          if (defaults.get(key) instanceof Color value && JETBRAINS_COLORS.contains(String.format("%06x", value.getRGB() & 0xffffff)))
            leaks.add(name + "=" + String.format("#%06x", value.getRGB() & 0xffffff));
        }
        check(leaks.isEmpty(), "JetBrains colors instead of palette: " + theme.getName() + " / " + leaks);
        // Links and focus rings use the accent; they must not read as errors or warnings.
        if (isAccent)
          for (String status : List.of("text-error", "text-warning"))
            check(distance(palette.get("accent"), palette.get(status)) >= 40, "Accent must differ from "+status+": "+theme.getName());
        islandThemes++;
      }
      if(isContrast) {
        double comments=contrast(attrs.get("DEFAULT_LINE_COMMENT").getForegroundColor(),background);
        double gutter=contrast(colors.get("LINE_NUMBERS_COLOR"),background);
        check(comments>=5.5&&gutter>=5.5,"Contrast comments/gutter: "+theme.getName());
        check(contrast(color(defaults,"Label.infoForeground"),color(defaults,"Panel.background"))>=4.5,"Secondary text");
        if(defaults.getInt("Islands")==1) {
          check(contrast(color(defaults,"StatusBar.Widget.foreground"),color(defaults,"StatusBar.background"))>=4.5,"Status labels");
          check(contrast(color(defaults,"ToolWindow.Button.foreground"),color(defaults,"MainWindow.background"))>=4.5,"Tool window labels");
        }
        for(String color:ansi) check(contrast(attrs.get("CONSOLE_"+color+"_OUTPUT").getForegroundColor(),background)>=4.5,"ANSI contrast: "+color);
        System.out.printf("%s: comments %.2f:1, gutter %.2f:1; ANSI/diff/UI readability OK%n",theme.getName(),comments,gutter);
        contrastThemes++;
      }
      if(isAccent) {
        check(!theme.getEditorSchemeId().contains("contrast"),"Accent keeps original syntax scheme");
        accentThemes++;
      }
    }
    int noItalics=0, semanticRanges=0;
    var darculaKeys = darculaColoredKeys(Path.of(args[1]));
    try(var schemePaths=Files.list(RESOURCES.resolve("schemes"))) {
      var files = schemePaths.filter(p->p.getFileName().toString().endsWith(".xml")).toList();
      Set<String> actualPaths = new HashSet<>(files.stream().map(p -> RESOURCES.relativize(p).toString().replace('\\', '/')).toList());
      check(actualPaths.equals(bundledSchemes.keySet()), "Unregistered or missing editor schemes");
      for(Path path:files) {
        checkSemanticRange(path);
        semanticRanges++;
        // Colors and keys inherited from the themes Emberkai was adapted from
        // (Darcula, Material Theme, Rainbow Brackets, Markdown Navigator).
        var root = xml(path);
        for (var entry : schemeColors(root).entrySet())
          check(!FOREIGN_COLORS.contains(String.format("%06x", entry.getValue().getRGB() & 0xffffff)),
                "Scheme color must come from the palette: " + path + " / " + entry.getKey());
        // Text drawn straight on the editor must stay readable; only placeholders that
        // IntelliJ shows on hover are meant to be invisible.
        var schemeAttrs = attributes(root);
        Color editor = schemeAttrs.get("TEXT").getBackgroundColor();
        for (var entry : schemeAttrs.entrySet()) {
          var value = entry.getValue();
          if (value.getForegroundColor() == null || value.getBackgroundColor() != null
              || entry.getKey().matches("INVISIBLE_INLAY_BUTTON|COMPOSE_INVISIBLE_PORT_BUTTON")) continue;
          check(contrast(value.getForegroundColor(), editor) >= 3, "Editor text contrast below 3:1: " + path + " / " + entry.getKey());
        }
        // Dimmed code must not read as a comment.
        Color comment = schemeAttrs.get("DEFAULT_LINE_COMMENT").getForegroundColor();
        for (String key : List.of("NOT_USED_ELEMENT_ATTRIBUTES", "DEPRECATED_ATTRIBUTES"))
          check(distance(schemeAttrs.get(key).getForegroundColor(), comment) >= 30, "Dimmed code must differ from comments: " + path + " / " + key);
        for (var section : List.of("colors", "attributes"))
          for (var option : root.getChild(section).getChildren("option"))
            check(!option.getAttributeValue("name").matches("MT_.*|MARKDOWN_NAVIGATOR\\..*"),
                  "Unused plugin key: " + path + " / " + option.getAttributeValue("name"));
        var defined = new HashSet<String>();
        for (var section : List.of("colors", "attributes"))
          for (var option : root.getChild(section).getChildren("option")) defined.add(option.getAttributeValue("name"));
        var inherited = new TreeMap<>(darculaKeys);
        inherited.keySet().removeAll(defined);
        inherited.remove("CONSOLE_OUTPUT"); // Deliberately inherited; see the console background check.
        check(inherited.isEmpty(), "Darcula colors inherited instead of the palette: " + path + " / " + inherited);
        if (!path.getFileName().toString().endsWith("-no-italics.xml")) continue;
        var regular = xml(path.resolveSibling(path.getFileName().toString().replace("-no-italics", "")));
        var plain = xml(path);
        check(colors(regular).equals(colors(plain)), "No Italics colors: " + path);
        var expected = attributes(regular);
        expected.values().forEach(a -> a.setFontType(a.getFontType() & ~2));
        var actual = attributes(plain);
        for (var entry : expected.entrySet()) {
          var value = actual.get(entry.getKey());
          check(entry.getValue().equals(value), "No Italics mismatch: " + path + " / " + entry.getKey() + " expected " + entry.getValue() + " got " + value);
        }
        check(expected.keySet().equals(actual.keySet()), "No Italics attribute keys: " + path);
        noItalics++;
      }
    }
    check(PROVIDERS.size()==5&&contrastThemes==1&&accentThemes==3&&islandThemes==5&&noItalics==2&&semanticRanges==4,"Complete preset matrix");
    System.out.printf("Validated %d inheritance chains, %d Contrast themes, %d accent themes, and %d No Italics schemes.%n",PROVIDERS.size(),contrastThemes,accentThemes,noItalics);
    System.out.printf("Validated %d opt-in semantic ranges, including all 21 interpolated colors per scheme.%n",semanticRanges);
    System.out.printf("Validated normal and selected Symbols SVG fill and stroke colors in all %d presets.%n",PROVIDERS.size());
    System.out.printf("Validated %d explicitly bundled editor schemes for theme reloads.%n",bundledSchemes.size());
    System.out.printf("Validated that no scheme inherits any of %d Darcula and bundled plugin colors.%n",darculaKeys.size());
  }
}
