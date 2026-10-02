# Freemarker

Package: `cz.bliksoft.javautils.freemarker`

A wrapper around Freemarker 2.3.x that adds a library of built-in template extensions, a pluggable object wrapper, and a shared template loader facility.

## `FreemarkerGenerator`

Main entry point. Create one instance per template root; reuse it for multiple renders.

### Constructors

| Constructor | Template source |
|---|---|
| `FreemarkerGenerator()` | Uses the globally configured `defaultTemplateLoader` |
| `FreemarkerGenerator(Configuration config)` | Pre-built Freemarker `Configuration` |
| `FreemarkerGenerator(TemplateLoader loader)` | Single loader |
| `FreemarkerGenerator(TemplateLoader... loaders)` | Multiple loaders wrapped in a `MultiTemplateLoader` |
| `FreemarkerGenerator(File basePath)` | Filesystem directory |
| `FreemarkerGenerator(Class<?> templateLoaderClass)` | Classpath relative to the given class |
| `FreemarkerGenerator(File templatesBasePath, Class<?> templateLoaderClass)` | Filesystem + classpath fallback |

### Configuring the global default loader

```java
FreemarkerGenerator.setDefaultTemplateLoader(new File("templates/"));
FreemarkerGenerator.setDefaultTemplateLoader(MyApp.class);
FreemarkerGenerator.setDefaultTemplateLoader(myLoader);
```

Must be set before the first no-arg constructor call.

### Generating output

```java
String html  = gen.generate("report.ftl", dataObject);
gen.generateToFile("report.ftl", dataObject, new File("out/report.html"));
gen.generateToStream(template, dataObject, outputStream);
```

The `data` argument is available in the template as the `data` variable.

### Additional root variables

```java
gen.setVariable("title", "My Report");
gen.setVariables(propertiesOrMap);
```

These are available in every render alongside `data`.

### Other options

| Method | Description |
|---|---|
| `setLocalizedTemplateLookup(boolean)` | Enable/disable `.locale`-variant template resolution (disabled by default) |
| `storeEnvironment()` / `getLastEnvironment()` | Retain the Freemarker `Environment` after the last render for post-processing |
| `getConfiguration()` | Access the underlying `Configuration` |
| `setNumberFormat(NumberFormats)` | Override the default number format |
| `useJaxenXPathSupport()` | Enable Jaxen-based XPath in XML templates |
| `setResolveTemplateDefaults(boolean)` | Before rendering, fill in `{var|...}`-declared defaults for any variable not already set (see [Template-declared parameter defaults](#template-declared-parameter-defaults)) |
| `readTemplateSource(String templateName)` | Read a template's raw source via the configured `TemplateLoader` (file, classpath, or multi-loader) |

---

## Global extensions

Registered automatically in every `FreemarkerGenerator` instance. Available in all templates.

| Template variable | Class | Description |
|---|---|---|
| `formatAsHTML` | `HtmlPreformat` | Escapes HTML entities in plain text |
| `identifyObjectType` | `IdentifyObjectType` | Returns the class name of a template variable |
| `imgRes` | `ImageResource` | Loads an image from classpath resources |
| `code128` | `Code128Encode` | Encodes a string as a Code128 barcode SVG/data |
| `code128width` | `Code128Width` | Calculates Code128 barcode width |
| `regroup` | `Regroup` | Groups `List<Map>` by key columns → nested `Map`; last level is a `List` |
| `reindex` | `Reindex` | Like `regroup` but last level is the row `Map` directly (assumes unique key) |
| `prettyXML` | `PrettyPrintXml` | Pretty-prints an XML string |
| `parseXML` | `ParseXml` | Parses an XML string into a Freemarker DOM node |
| `GUIPrompt` | `GUIPrompt` | Shows a Swing dialog to prompt the user for input |
| `CMDPrompt` | `CMDPrompt` | Reads input from stdin; supports option lists |
| `CMDWrite` | `CMDWrite` | Writes text to stdout |
| `Base64File` | `Base64File` | Encodes a file's bytes as a Base64 string |
| `Base64QR` | `Base64QR` | Generates a QR code image and returns it as Base64 (requires ZXing) |
| `Base64IconSpec` | `Base64IconSpec` | Resolves an icon-spec string (see [`IconSpecEngine`](image-utils.md#iconspecengine)) to a PNG and returns it as Base64 — no JavaFX required |
| `LogVariable` | `LogVariable` | Logs a variable's value |
| `LogMessage` | `LogMessage` | Logs a literal message |
| `SystemMessage` | `SystemMessage` | Shows a system message (log or dialog) |
| `DescribeVariable` | `DescribeVariable` | Dumps a full description of a variable |
| `StringBuilder` | `StringBuilderDirective` | Captures a template block into a `StringWriter`; also a method returning a new `StringWriter` |
| `ltrim` / `rtrim` | `Trim` | Left / right trims whitespace from a string |
| `TXTTOHTML` | `TextReplacer` | Escapes `& < > " '` and converts `\n` → `<br>` |
| `TXTTOHTML_WHITESPACE` | `TextReplacer` | Same plus space → `&nbsp;` and tab → `&nbsp;&nbsp;&nbsp;` |

Modify the global set:

```java
FreemarkerGenerator.addGlobalExtension("myFunc", new MyTemplateMethod());
FreemarkerGenerator.removeGlobalExtension("myFunc");
```

These changes affect all generator instances, including those shared with Spring Boot or other frameworks.

---

## Local extensions

Registered per-instance. Available in every render of that instance.

| Template variable | Class | Description |
|---|---|---|
| `registerVariable` | `VariableRegistrator` | Registers a variable on the generator for *subsequent* renders (not the current one); useful in SQL query templates |
| `anchorNumberer` | `AnchorNumberer` | Auto-incrementing counter for anchors or headings |
| `variableCache` | `VariableCache` | Dynamic in-template storage: `set`, `get`, `add` (list append), `put` (map put), `remove`, `clear` |
| `applyTemplateDefaults` | `ApplyTemplateDefaults` | `${applyTemplateDefaults()}` — fill in `{var|...}`-declared defaults for the current template (see below) |

Add per-instance extensions:

```java
gen.addExtension("myExt", new MyExtension());
gen.addExtensions(myExtensionMap);
```

---

## Template-declared parameter defaults

Templates may declare their parameters in a leading comment block, one declaration per line:

```
{var|type|name|title[|default[|parameters]]}
```

e.g.

```ftl
<#--
{var|int|labelCount|Number of labels|1|1:100}
{var|string|txt|Text||40}
{var|boolean|debug|Debug|false}
-->
```

`cz.bliksoft.javautils.freemarker.utils.TemplateParameterUtils` parses these declarations
(`parseParameters`, or `parseFormParameters` for building a form - see
[Hints and translated texts](#hints-and-translated-texts)) and derives a `Map<String, Object>` of default values per declared
variable (`extractDefaultVariables`), typed by their declared `type` (`INT` → `Integer`,
`DECIMAL` → `Double`, `BOOLEAN` → `Boolean`, `DATE` → `LocalDate`, `DATETIME` → `LocalDateTime`
- a [date expression](#date-expressions) evaluated now, `null` when empty -, everything else →
`String`). `INFO`, `COMMENT`, `HINT` and `CSVFILE` declarations are skipped — they have no usable
scalar default. A template should print an optional date as `${due!}` / test `due??`.

`FreemarkerGenerator` can apply these defaults automatically, filling in **only** variables
that have not already been set — explicitly supplied values always win:

- **Opt in from Java**: `gen.setResolveTemplateDefaults(true)`. Before the next
  `generate(templateName, data)` call, the generator reads the target template's source
  (via its configured `TemplateLoader`, so this works for file, classpath, or
  multi-loaders) and fills in any missing variables with their declared defaults.
- **Opt in from a template**: call `${applyTemplateDefaults()}` (e.g. from a shared
  include such as `common.ftl`). It reads the *main* template's source and fills in
  any variables not yet defined in the current environment.

Both mechanisms share a single "applied" flag on the generator instance, so calling
`${applyTemplateDefaults()}` is a no-op if `setResolveTemplateDefaults(true)` already
applied the defaults for this render (and vice versa) — it is safe to use both at once.

### Conventional parameter types

`TemplateParameterUtils` itself treats `type` as an opaque string (compared ignoring case,
`TYPE_*` constants) — its regex doesn't special-case any particular value. The type vocabulary
below is followed by each UI that turns a parsed parameter list into an actual form:
StorageManagerServer's web print UI (`ui.ftlh`'s `parameterinputs` macro, consumed by
`PrintController`) and BSToolbox-jfx `ParametricFormPane` (StorageManagerDesktopClient2's
`PrintLabelDialog`, BSLabelDesigner). Their type-independent logic - options, initial values,
typing of entered text - is shared in `TemplateFormSupport` (see
[Prefill model and options](#prefill-model-and-name_options)); the controls themselves are
per UI, keep them in sync when adding/changing a type.

| Type | `default` meaning | `parameters` meaning | Rendered as |
|---|---|---|---|
| `int` | numeric default | `min:max:step` | Number spinner |
| `decimal` | number, dot or comma | — | Text input for a decimal number; value `Double` |
| `string` | text default | `maxlength:size` | Single-line text input |
| `multiline` | text default | `rows:cols` | Textarea |
| `boolean` | `true`/`false` | — | Checkbox |
| `combo` | selected option | `;`-separated option list (`,` when there is no `;`) | Dropdown; options replaceable by the model's `<name>_options` |
| `font` | selected font | option list used when the printer's fonts are unknown | Dropdown of the printer's fonts |
| `date` | ISO date or [date expression](#date-expressions) (`today+7`) | `min..max` range (date expressions, either side optional) | Date picker; value `LocalDate` |
| `datetime` | ISO date-time (`2026-10-02T14:30`, `2026-10-02 14:30`) or date expression (`now-1h`) | `min..max` range | Date + time (`HH:mm`) input; value `LocalDateTime` |
| `hidden` | the value | — | No row; the value (or the model's) is submitted |
| `radio` | — | — | Not implemented in either consumer yet |
| `csvfile` | — (no default) | `size:accept` (e.g. `20:.csv,.txt`) | File picker; submitted value becomes a parsed `List<Map<String,String>>`, not a scalar |
| `info` | **the actual displayed value** | — | Read-only row: `title` as the left-hand label, `default` as the right-hand value — a label:value pair, not an input |
| `comment` | fallback text of a translated comment (see below) | — | Free text taken from `title`, rendered spanning the **whole row** (full width, wrapped/pre-wrapped) instead of split into a label/value pair |
| `hint` | fallback text (see below) | — | No row: help text of the **preceding** parameter, shown as a tooltip of its label |

`info` and `comment` are both purely presentational — neither is bound to a real
variable — but they place their text in different fields and render at different widths:
use `info` for a short label:value fact (e.g. the template's own display name —
conventionally one `{var|info|-|Šablona|<name>}` line per template, name field `-` since it
isn't a real variable), and `comment` for a longer explanatory note that needs the full row
rather than being squeezed into the narrow value column next to a label. `comment`'s `title`
group isn't restricted to a single line — since `TemplateParameterUtils`'s regex only
excludes literal `|` and `}` characters (not newlines), a `comment` declaration can span
several source lines and they're preserved (`ui.ftlh` renders it with `pre-wrap`;
`PrintLabelDialog` uses a wrapping `Label`):

```ftl
<#--
{var|info|-|Šablona|My Label}
{var|comment|comment|Explains something the user should know before filling out the form,
possibly across more than one line.}
{var|multiline|zpl|ZPL content||10:200}
-->
```

### Date expressions

A `date`/`datetime` default (and either side of its `min..max` range) is an ISO value or an
expression evaluated when the form is shown / the defaults are applied (`TemplateDateValues`):

```
expr   := base offset*
base   := today [('T'|' ') time] | now | yyyy-MM-dd [('T'|' ') time]     time := H[H]:mm[:ss]
offset := ('+'|'-') digits [d|w|M|y|h|m|s]                              (no unit = d)
```

Keywords ignore case, units do not (`M` month, `m` minute). `today` is midnight, `now` the
current time; offsets apply left to right: `today+7`, `today-1M`, `today+1w`, `now-1h`,
`now+1d-2h`, `today 08:00+1d`. A `date` takes the date part. The JVM's time zone is used.
`TemplateValueCoercion.coerce(raw, LocalDate.class)` accepts the same, so a printer
configuration variable may be `today` too.

```ftl
<#--
{var|date|due|Due date|today+14|today..today+90}
{var|datetime|packed|Packed|now}
-->
Due: ${(due.format('d.M.yyyy'))!}
```

`.format(pattern)` needs the `freemarker-java8` object wrapper on the classpath (registered by
`ObjectWrapperRegister` when present); without it `${due}` prints the ISO value.

### Prefill model and `<name>_options`

A form generator may get a data model from the application (`Map<String, ?>`, optional). A
field's initial value is the first usable of:

1. the value the user entered (kept when the form is rebuilt),
2. the model's value of the same name (`null` = empty; typed values are converted -
   `BigDecimal`, `LocalDate`, `java.util.Date`, enums, ...),
3. the template default.

A value that does not fit (not a number, not one of a combo's options, ...) is skipped. A
`combo`'s options are replaced by the model's `<name>_options`:

- a `List` (or an array) - the items are the values and the labels,
- a `Map` - keys are the values, map values the displayed labels, in the map's order,
- text - `S;M;L`, or `r=Red;b=Blue` (value=label) when every item has a `=`.

The selected value is `String.valueOf` of the key - a template gets text, like from other
fields (`setVariableCoerced` retypes it to match a lower layer). A `font`'s options are the
printer's fonts; `<name>_options` is used only when those are unknown.

`TemplateFormSupport`: `resolveOptions(type, name, parameters, resolverOptions, model)`,
`initialValue(type, name, default, model, entered, optionValues)`, `formValue(type, value)`
(value → form text), `typedValue(type, text)` (form text → `Integer`/`Double`/`Boolean`/
`LocalDate`/`LocalDateTime`/`String`) and `coerceToDeclaredTypes(parameters, variables)` for
values that did not come from a form (AI tools, configuration).

### Hints and translated texts

`comment` and `hint` texts can come from the XML-filesystem translations
(`FileSystem.getTranslation`, the `translations/` tree of the module XMLs). When the
declaration has a `default` part, `title` is a translation **key** and `default` the fallback
text used when the key has no translation (key `-` = no key, the fallback is used as is). Without
a `default` part, `title` is the text itself (the original form):

```ftl
<#--
{var|comment|-|A literal comment.}
{var|comment|-|my/app/note|Fallback note text}
{var|csvfile|list|List||:.csv}
{var|hint|-|labels/csv/count.copies|count - number of copies}
{var|hint|-|-|title - a literal hint line}
-->
```

The name field of `comment`/`hint`/`info` is not a variable; use `-`.

Any parameter's **title** can be translated as well: `:key[:fallback]` - the translation of `key`,
else `fallback` (the key itself when there is none); a title without the leading `:` is shown as it
is. E.g. `{var|int|printQuantity|:labels/param/printQuantity:počet výtisků|1|1:100}`
(`resolveTitle(title, locale)`; `parseFormParameters` applies it to every title, `parseParameters`
keeps the raw declaration).
`TemplateParameterUtils.parseFormParameters` returns the declarations ready for a form: `comment`
titles resolved to their text, `hint` lines removed and their texts attached (joined by newlines)
to the preceding parameter's `getHint()`. A hint with no preceding parameter is dropped.
`resolveText(key, fallback)` is the key-or-fallback lookup. Both take an optional `Locale`
(`parseFormParameters(source, locale)`, `resolveText(key, fallback, locale)`) for the texts in a
given language (`FileSystem.getTranslation(id, locale)`) - StorageManagerServer's web form passes
the browser's; without it the language `FileSystem.loadTranslations()` loaded is used. `extractDefaultVariables` skips
`hint` as well. Consumers: BSToolbox-jfx `FormField.fromTemplate`/`ParametricFormPane` (and
`FormField.withHint` for rows built in code), StorageManagerServer's `PrintController`/`ui.ftlh`.
The label templates' CSV help texts are in BSToolbox-print's `PrintModule.xml`
(`labels/csv/*`).

---

## Extensions added manually

These are not registered by default; add them when needed.

### `Query`

Executes SQL queries from templates. Requires `IDBConnectionProvider` and `IQueryProvider`.

```java
Query query = new Query(connectionProvider, queryProvider);
gen.addExtension("query", query);
```

Template usage:
```ftl
<#assign rows = query("myQueryId", param1, param2)>
<#list rows as row>
  ${row.columnName}
</#list>
```

After each call the template variable `lastQuery` is set:

| Key | Value |
|---|---|
| `columns` | `List<String>` of column names |
| `columnTypes` | `List<String>` of SQL type names |
| `SQL` | The executed SQL string |
| `parameters` | `List` of parameter values |
| `resultCount` | Row count (non-iterable mode only) |

For large result sets construct the `Query` with `iterable = true`; the result implements `Iterator<Map<String,Object>>` and `Closeable`.

**`IQueryProvider`** supplies the SQL for each query ID:

| Method | Description |
|---|---|
| `createQuery(String queryID)` | Prepare the query |
| `String getSql(String queryID)` | Return the SQL string |
| `List<Integer> getArgumentTypes(String queryID)` | Return `java.sql.Types` constants for each parameter |

Concrete implementations: `FileQueryProvider` (loads from `FileObject`), `TemplatedQueryProvider` (queries are themselves Freemarker templates).

Use `QueryListArgs` to format SQL `IN`-clause parameter lists.

### `RegexMatcher`

```java
RegexMatcher matcher = new RegexMatcher("(\\d+)-(\\w+)");
matcher.addGroup("number", 1);
matcher.addGroup("word", 2);
gen.addExtension("matchItem", matcher);
```

Returns `List<Map>` per match with keys `match`, `groups` (list), and `namedGroups` (map, when groups are defined via `addGroup`).

### Collectors (read back from Java after render)

| Class | Method to read result | Description |
|---|---|---|
| `ListCollector` | `getValues()` → `List<Object>` | Accumulates values during template execution |
| `MapCollector` | `getValues()` → `Map<String,String>` | Accumulates key-value pairs |
| `StringCollector` | — | Accumulates string fragments |

```java
ListCollector collector = new ListCollector();
gen.addExtension("collect", collector);
gen.generate("template.ftl", data);
List<Object> results = collector.getValues();
```

### `LocalDateTimeFormatter`

Formats `LocalDateTime` values with a `DateTimeFormatter` pattern. Requires `freemarker-java8` on the classpath.

---

## Object wrapper

`ObjectWrapperRegister` holds a singleton `DefaultObjectWrapper` shared by all generator instances. Built-in conversions:

| Java type | Freemarker representation |
|---|---|
| `Optional<T>` | Unwrapped value or `null` |
| `File` | Path string |
| `TimestampedObject` | Map with keys `millis`, `timestamp` (LocalDateTime), `value` |

Register custom converters before the singleton is first created:

```java
ObjectWrapperRegister.addConverter(MyType.class, obj -> obj.toString());
```

Call `ObjectWrapperRegister.useToString()` to enable a last-resort `toString()` fallback for types without a registered converter.

If `no.api.freemarker.java8.Java8ObjectWrapper` is on the classpath it is loaded reflectively to handle `java.time` types.

---

## Built-in template loader

`BuiltinTemplateLoader` exposes a library of helper templates bundled in the JAR.

```java
// use built-in templates only
TemplateLoader builtins = BuiltinTemplateLoader.getBuiltinTemplateLoader();

// let application templates take precedence; fall back to built-ins
TemplateLoader combined = BuiltinTemplateLoader.getTemplateLoader(myLoader);
```
