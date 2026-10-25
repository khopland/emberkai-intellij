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
import com.intellij.ui.ExperimentalUI;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;
import java.awt.Color;
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
    for (String family : List.of("default", "machine", "octagon", "ristretto", "spectrum"))
      for (String variant : List.of("", "-contrast", "-teal", "-rose"))
        expectedPaths.add("islands/" + family + variant + ".theme.json");
    check(paths.equals(expectedPaths), "Complete Islands preset matrix");
    try (var files = Files.walk(RESOURCES)) {
      Set<String> actual = new HashSet<>(files.filter(p -> p.toString().endsWith(".theme.json")).map(p -> RESOURCES.relativize(p).toString().replace('\\', '/')).toList());
      check(actual.equals(paths), "Unregistered or missing theme resources");
    }
    for (var entry : descriptor.getChild("extensions").getChildren("bundledColorScheme")) {
      var scheme = xml(RESOURCES.resolve(entry.getAttributeValue("path") + ".xml"));
      check(entry.getAttributeValue("id").equals(scheme.getAttributeValue("name")), "Scheme registration name");
    }
    int contrastThemes=0, accentThemes=0, islandThemes=0;
    String[] ansi={"BLACK","RED","GREEN","YELLOW","BLUE","MAGENTA","CYAN","GRAY","DARKGRAY","RED_BRIGHT","GREEN_BRIGHT","YELLOW_BRIGHT","BLUE_BRIGHT","MAGENTA_BRIGHT","CYAN_BRIGHT","WHITE"};
    for(String id:PROVIDERS.keySet()) {
      UITheme theme=provider(id);
      String file=Path.of(PROVIDERS.get(id)[1]).getFileName().toString();
      String family=file.replace(".theme.json","").split("-")[0];
      boolean isContrast=file.contains("-contrast");
      boolean isAccent=file.contains("-teal")||file.contains("-rose");
      UIDefaults defaults=new UIDefaults(); theme.applyTheme(defaults);
      var scheme=xml(RESOURCES.resolve(theme.getEditorSchemeId().substring(1)));
      var attrs=attributes(scheme); var colors=colors(scheme);
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
        check(contrast(color(defaults,"ToolWindow.background"),color(defaults,"MainWindow.background"))>=1.2,"Panel separation");
        check(contrast(color(defaults,"EditorTabs.underlinedTabBackground"),color(defaults,"EditorTabs.underlinedTabForeground"))>=4.5,"Tabs");
        check(contrast(color(defaults,"Button.default.startBackground"),color(defaults,"Button.default.foreground"))>=4.5,"Buttons");
        check(contrast(color(defaults,"ToolWindow.Button.selectedBackground"),color(defaults,"ToolWindow.Button.selectedForeground"))>=4.5,"Selected tool windows: "+theme.getName());
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
    try(var schemePaths=Files.list(RESOURCES.resolve("schemes"))) {
      for(Path path:schemePaths.filter(p->p.getFileName().toString().endsWith(".xml")).toList()) {
        checkSemanticRange(path);
        semanticRanges++;
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
    check(PROVIDERS.size()==20&&contrastThemes==5&&accentThemes==10&&islandThemes==20&&noItalics==10&&semanticRanges==20,"Complete preset matrix");
    System.out.printf("Validated %d inheritance chains, %d Contrast themes, %d accent themes, and %d No Italics schemes.%n",PROVIDERS.size(),contrastThemes,accentThemes,noItalics);
    System.out.printf("Validated %d opt-in semantic ranges, including all 21 interpolated colors per scheme.%n",semanticRanges);
  }
}
