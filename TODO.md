## Currently considered
- action refactor
  - Next all `ActionPane` actions will be migrated to `AppActions.kt`, so they can be accessed type-safely.
  - Next configurable properties will be used to avoid reflection & manual registering per class holder (AppActions).
  - Next integrate with `Action`, define all configurable actions in the same manner, adding icon to Action & keys to ActionData 
- Implement better application running detection: app running without exe and app running from different directory
- Fix application not shutting sometimes down (non-daemon thread?)
- `EnumerableCE` does not support observable collections as inputs because Config.enumerate() removes observability
- Layouts loaded from file should have a Save action
- Implement `MdNode` monitor file & reload
- Implement `SliderCE` logarithmic scale
- Implement actions for widget management: delete widget, open with strategy
- Implement **Playback Knobs** widget skinnable size
- Implement **Playback Knobs** widget writable chapters
- song order in ui, editable
- Implement **Object Info** widget scale image use Object Info in form
- Improve Json serialization to support graph cycles
- `Placeholder` should distinguish file `MOVE` and file `COPY` in description?
- Improve **Tooltip** text layout
- Improve **kotlinc**/**vlc**/**mpeg** setup should not be `Future`, but restartable `Task`
- `ObsListConfig.setDefaultValue` not removing disabled items
- Fix manual widget loading not rebinding widget inputs properly
- Write application documentation
- Implement `app.stateful` on/off config
- Implement data info tooltip progress
- `Node.isAnyParentOf` not implemented optimally
- drag & drop playlist file support in audio
- `image.toFX(null)` creates null `Image.url`, which disables some image menu behavior
- `Configuration.propertiesRaw` are not synced with configs, should be?
- Improve **Tagger** widget
  - tagging non audio file with audio extension causes exception and corrupted application state
  - editable song check list not refreshing content properly
  - implement chapters tag
  - tag writing should fail when tag reading fails?
  - make writing to tag handle errors properly (& report read-only files)

## Potential future work
- Named widgets to avoid cloning
  Basically persisted settings of widgets, better Templates (persisted layouts)
- Show progress for file downloads progress, [Inspiration](https://betterprogramming.pub/show-download-progress-in-kotlin-style-64d157995e27)  
  Must work for downloading up `kotlinc` and `Vlc`
- Support table column hierarchy, i.e., nested columns, e.g., `Metadata.File.Size`  
  JavaFx supports this, however `ObjectField` and `FieldedTable` need some work
- Implement `Window` & `Component` `Shower` using smart window resize algorithm expanding window to available screen area  
  It is not clear where exactly this would make the most impact. Potentially, custom action to reorganize windows.
- Use standalone `README.md` for widgets  
  Will require lots of work. Also needs markdown support for displaying widget info in `Settings` and `WidgetPicker`.
- Implement `ComboBox` search box  
  `ComboBoxSkin` is most probably not sufficiently extendable for this. 
- Implement `OverlayPane` autohide/onTop icons  
  Dock windows already have this feature. Overlay already supports the shortcuts, so this is very low priority.
- Implement multi-screen support for `Dock`  
  It is not clear how it should behave - be duplicated or displayed on screen with mouse?
- Implement [virtual keyboard](https://github.com/comtel2000/fx-experience)  
  Useful to control pc without mouse. This would be an interesting addition. But needs lots of work
- Consider using [ScaleNode](https://github.com/miho/ScaledFX)  
  May be useful in some use cases
- Reimplement [Tray] using [FXTrayIcon](https://github.com/dustinkredmond/FXTrayIcon)  
  Would add high-dpi support and more
- Expose **fxwl** templates of **Custom** widgets for useful ui components  
  May require rethinking widget identity and UX
- Implement **IconViewer** widget as ordinary `Node` used in **CustomNode** widget  
  Would reduce widget count, however this is low priority & is blocked by the above issue
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
- Implement **Song album detail** widget  
  Needs well-formed album identity, but so far this application has no such concept
- Remove `Metadata` comment reading workaround in `Metadata.loadComment`  
  Currently, due to a bug in **Jaudiotagger**, `CUSTOM` field is also considered
- Improve **Converter** widget
  - support input generators
  - transformation chain projection slider: cut off transformation tail
  - column: line numbers
  - column: item numbers
  - column: item types
- Improve **Image** widget: add image controls (+,  -, ...)  
  Low priority and technically difficult
- Improve **search** by allowing user to switch sources  
  Challenging UX
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
- Implement cover/preview support for `pdf`  
  What library? Ideally integrated with `image-io`
- Improve table column resizing
  - https://github.com/edvin/tornadofx/wiki/TableView-SmartResize
  - https://dlemmermann.wordpress.com/2015/12/10/javafx-tip-22-autosize-tree-table-columns/
- improve process handling  
  Look into [NuProcess](https://github.com/brettwooldridge/NuProcess)

## Bugs

## External bugs
- [ ] [jfx-481](https://github.com/javafxports/openjdk-jfx/issues/481) JavaFx: table `CTRL+A` performance  
  open
- [ ] [jfx-409](https://github.com/javafxports/openjdk-jfx/issues/409) JavaFx: table columns performance  
  open
- [x] [jfx-364](https://github.com/javafxports/openjdk-jfx/issues/364) `javafx.scene.control.TextField` focus styling changes on window focus in/out  
  open, but no longer occurs
- [x] [JDK-8261077](https://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8261077) JavaFx: `javafx.scene.control.skin.TextAreaSkin` dispose() throws `UnsupportedOperationException`  
  reported, fixed
- [x] [JDK-8252446](https://github.com/openjdk/jfx/pull/295) `javafx.stage.Screen.getScreens()` is empty sometimes  
  fixed
- [x] [JDK-8195750](https://bugs.openjdk.java.net/browse/JDK-8195750) `sp.it.pl.ui.nodeinfo.TableInfo` change throws exception sometimes  
  open, but no longer occurs
- [ ] [KT-41373](https://youtrack.jetbrains.com/issue/KT-41373) Reflection: Anonymous class inspection fails  
  reported, open
- [x] [KT-41310](https://youtrack.jetbrains.com/issue/KT-41310) ReflectionL Creating `KType` from `Java`  
  reported, not an issue
- [x] [KT-41309](https://youtrack.jetbrains.com/issue/KT-41309) Kotlinc: `-cp`  requiring escape after Kotlin update
  reported, documented, worked around
- [ ] [KT-41300](https://youtrack.jetbrains.com/issue/KT-41300) Kotlinc: Incorrect warning on Windows "advanced option value is passed in an obsolete form" on unquoted argument with delimiter character  
  requires project fix // TODO
- [x] [KT-38817](https://youtrack.jetbrains.com/issue/KT-38817) Bad behavior:`String.capitalize()`  
  fixed. See `TextUtilTest`.
- [ ] [KT-22792](https://youtrack.jetbrains.com/issue/KT-22792) Reflection:`objectInstance` throws IllegalAccessException  
  reported, open
- [x] [KT-14119](https://youtrack.jetbrains.com/issue/KT-14119) Bad API: `String?.toBoolean()`  
  reported, implemented wrongly
