## Currently considered
- popup focus (Win 10)
- Cache DeserializationFactory ComponentDb to avoid IO if file was not modified in the meantime
- Document all used libraries, how they are used, why, where not to use them, etc.
- Action refactor
  - Next all `ActionPane` actions will be migrated to `AppActions.kt`, so they can be accessed type-safely.
  - Next configurable properties will be used to avoid reflection & manual registering per class holder (AppActions).
  - Next integrate with `Action`, define all configurable actions in the same manner, adding icon to Action & keys to ActionData 
- `EnumerableCE` does not support observable collections as inputs because Config.enumerate() removes observability
- Implement actions for widget management: delete widget, open with strategy
- Implement **Playback Knobs** widget skinnable size
- Implement **Playback Knobs** widget writable chapters
- song order in ui, editable
- `Placeholder` should distinguish file `MOVE` and file `COPY` in description?
- Improve **kotlinc**/**vlc**/**mpeg** setup should not be `Future`, but restartable `Task`
- `ObsListConfig.setDefaultValue` not removing disabled items
- Fix manual widget loading not rebinding widget inputs properly
- Implement `app.stateful` on/off config
- Implement data info tooltip progress
- Drag & drop playlist file support in audio
- `Configuration.propertiesRaw` are not synced with configs, should be?
- Improve **Tagger** widget
  - tagging non audio file with audio extension causes exception and corrupted application state
  - editable song check list not refreshing content properly
  - implement chapters tag
  - tag writing should fail when tag reading fails?
  - make writing to tag handle errors properly (& report read-only files)

## Potential future work
- Implement better application running detection: app running without exe and app running from different directory
- Implement converting Layout into ContainerSwitch and back
- Implement `MdNode` monitor file & reload
- Fix **Function Viewer** plotting steep functions clipped too soon (requires plotted point look-ahead)
- Implement **Function Viewer** derivation, see [kotlingrad](https://github.com/breandan/kotlingrad)
- Implement `SliderCE` logarithmic scale
  Should support arbitrary N(0-1) -> N(0-1) function and config dsl
- App onStart/End command user defined callbacks
  Requires CommandRunnable of sort
- MetadataGroup filter by Song predicate
  Unclear how the predicate should be generalized (group.anyMatch(predicate)? group.allMatch(predicate)?), ui needs work
- File rename event
  Similar to file delete event, then support file renaming in file tables/grids 
- Optimize markdown & support selection
  Will probably require [RichTextFX](https://github.com/FXMisc/RichTextFX)
- Implement multivalued **Song.Artist** support
  Should work like tags
- Show progress for file downloads progress, [Inspiration](https://betterprogramming.pub/show-download-progress-in-kotlin-style-64d157995e27)  
  Must work for downloading up `kotlinc` and `Vlc`
- Implement `Window` & `Component` `Shower` using smart window resize algorithm expanding window to available screen area  
  It is not clear where exactly this would make the most impact. Potentially, custom action to reorganize windows.
- Use standalone `README.md` for widgets  
  Will require lots of work. Also needs markdown support for displaying widget info in `Settings` and `WidgetPicker`.
- Implement `OverlayPane` autohide/onTop icons  
  Dock windows already have this feature. Overlay already supports the shortcuts, so this is very low priority.
- Implement multiple-screen support for `Dock`  
  It is not clear how it should behave - be duplicated or displayed on screen with mouse?
- Implement [virtual keyboard](https://github.com/comtel2000/fx-experience)  
  Useful to control pc without mouse. This would be an interesting addition. But needs lots of work
- Consider using [ScaleNode](https://github.com/miho/ScaledFX)  
  May be useful in some use cases
- Reimplement [Tray] using [FXTrayIcon](https://github.com/dustinkredmond/FXTrayIcon)  
  Would add high-dpi support and more
- Implement **FavLocations** widget selection persistence  
  Natural idea, low priority
- Implement **playlists** table  
  Requires deeper integration with **Update library** action, playlist discovery, etc.
- Implement widget factory ref example **Tester** widget:
  ```
  private val widgetFactory by cvn<FactoryRef<Any>>(null).def(name = "Component", info = "Component").but(ValueSealedSetIfNotIn(USE)).valuesIn {
     APP.widgetManager.factories.getFactoriesWith()
  }
  ```
- Adopt proper code style using [ktlint](https://ktlint.github.io/)  
  Or at least add .idea code style files to git
- Support task hierarchy, i.e., task tree  
  Challenging issue
- Improve `GridView` to retain position on column change  
- Remove `Metadata` comment reading workaround in `Metadata.loadComment`  
  Currently, due to a bug in **Jaudiotagger**, `CUSTOM` field is also considered
- Improve **Converter** widget
  - support input generators
  - transformation chain projection slider: cut off transformation tail
  - column: line numbers
  - column: item numbers
- Improve **Image** widget: add image controls (+,  -, ...)  
  Low priority and technically difficult
- Fix the bug where playlist song moving (CTRL+drag) changes order of unrelated songs when moving too fast  
  Low priority
- Implement **Settings** sort option DEFAULT/BY_NAME  
  `CheckIcon(sortAlphabetically).icons(IconFA.SORT_ALPHA_ASC, IconFA.SORT_AMOUNT_ASC)`
- Implement application **ui-less** mode properly (no window needs to be open)  
  Requires rethinking how application would be closed
- Improve image loading performance using [PixelBuffer](https://github.com/jgneff/pixel-buffer)  
  Can provide lots of speedup
- Implement **TabContainer** and **StackContainer**  
  The challenge is the layout mode UI
- Implement isolating song playback and song db to `MASTER`application  
  Right now multiple instances could get in the way of each other
- Implement song fulltext search  
  **Lucene**?
- improve process handling  
  Look into [NuProcess](https://github.com/brettwooldridge/NuProcess)
- Improve I/O to support cross-window  
  Challenging. See [issue](/TODO-ISSUES.md#io-ui)
- Write documentation
  See [issue](/TODO-ISSUES.md#documentation)
- ❌ Implement ComboBox search  
  Challenging. Use autocomplete. See [issue](/TODO-ISSUES.md#searchable-combobox)
- ❌ Use less/sass/kotlin dsl for css  
  Not useful enough. See [issue](/TODO-ISSUES.md#styling)

## Bugs

## External bugs
- [ ] [jaudiotagger-65](https://bitbucket.org/ijabz/jaudiotagger/issues/65/add-support-for-ogg-opus-format) opus ogg support
  open
- [x] [jfx-481](https://bugs.openjdk.java.net/browse/JDK-8197991) JavaFx: table `CTRL+A` performance  
  fixed
- [x] [jfx-364](https://github.com/javafxports/openjdk-jfx/issues/364) `javafx.scene.control.TextField` focus styling changes on window focus in/out  
  open, but no longer occurs
- [x] [JDK-8261077](https://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8261077) JavaFx: `javafx.scene.control.skin.TextAreaSkin` dispose() throws `UnsupportedOperationException`  
  reported, fixed
- [x] [JDK-8252446](https://github.com/openjdk/jfx/pull/295) `javafx.stage.Screen.getScreens()` is empty sometimes  
  fixed
- [x] [JDK-8195750](https://bugs.openjdk.java.net/browse/JDK-8195750) `sp.it.pl.ui.nodeinfo.TableInfo` change throws exception sometimes  
  open, but no longer occurs
- [ ] [KT-41373](https://youtrack.jetbrains.com/issue/KT-41373) Reflection: Anonymous class inspection fails  
  reported, open, worked around by using anonymous class's superclass for reflection
- [x] [KT-41310](https://youtrack.jetbrains.com/issue/KT-41310) ReflectionL Creating `KType` from `Java`  
  reported, not an issue
- [x] [KT-41309](https://youtrack.jetbrains.com/issue/KT-41309) Kotlinc: `-cp`  requiring escape after Kotlin update
  reported, documented, fixed
- [x] [KT-41300](https://youtrack.jetbrains.com/issue/KT-41300) Kotlinc: Incorrect warning on Windows "advanced option value is passed in an obsolete form" on unquoted argument with delimiter character  
  fixed (by quoting), documented at https://kotlinlang.org/docs/compatibility-guide-14.html#compiler-arguments-with-delimiter-characters-must-be-passed-in-double-quotes-on-windows
- [x] [KT-38817](https://youtrack.jetbrains.com/issue/KT-38817) Bad behavior:`String.capitalize()`  
  fixed. See `TextUtilTest`.
- [ ] [KT-22792](https://youtrack.jetbrains.com/issue/KT-22792) Reflection:`objectInstance` throws IllegalAccessException  
  reported, open
- [-] [KT-14119](https://youtrack.jetbrains.com/issue/KT-14119) Bad API: `String?.toBoolean()`  
  reported, implemented wrongly
- [ ] [OSHII-2016](https://github.com/oshi/oshi/pull/2016/files) Disabled performance counters check failing
  reported, worked around by enabling the counters on app startup using `OshiCore::class`
- [-] [KT-52460](https://youtrack.jetbrains.com/issue/KT-52460) Slow Kotlin compilation
  reported demand for fast native excelsior compiler that has been discontinued -> Excelsior native compiler can no longer be build (no more experimental kotlinc)
- [x] [694](https://github.com/haraldk/TwelveMonkeys/issues/694) Incorrect subSampling out put for bmp image  
  fixed
- [-] [695](https://github.com/haraldk/TwelveMonkeys/issues/695) No image reader for some webp images  
  reported, lossless webp not supported for now
- [x] [695](https://github.com/kwhat/jnativehook/issues/420) Consuming global hotkey modifier key release
  asked, figured out (not an issue, modifiers should not be consumed)