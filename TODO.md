## Potential future work


- Implement about page
- Implement Thumbnail video support
- Implement AI emotion state
- impl MdNode edit
- JDK-8320359 ImageView: add styleable fitWidth, fitHeight, preserveRatio, smooth properties
- show emote should be in kotlin too
- error label warn icon
- timeline css color
- COnverter function picker add combobox for text/each/value
- Load widget factories in bgr
- Comet lock FPS to 60 or support 120
- implement plus icon for custom widget custom inputs in layout mode
- implement custom outputs for custom widgets
- Improve **Tagger** widget
  - implement table view and use for error handling
  - tagging non audio file with audio extension causes exception and corrupted application state
  - editable song check list not refreshing content properly
  - implement chapters tag
  - tag writing should fail when tag reading fails?
  - make writing to tag handle errors properly (& report read-only files)
- Fix `ObsListConfig.setDefaultValue` not removing disabled items
- Fix `EnumerableCE` does not support observable collections as inputs because Config.enumerate() removes observability
- Fix`Configuration.propertiesRaw` are not synced with configs, should be?
- Fix Thumbnail image scaling for directories
- Fix LibraryView not refreshing on song library add sometimes
- Implement **Playback Knobs** widget skinnable size
- Implement `Placeholder` to distinguish file drag `MOVE` and file `COPY` in description
- Implement icons and widget metadata for all Node widgets
- Implement more convenient plugin on/off toggle in Plugin Management
- Document all used libraries, how they are used, why, where not to use them, etc.
- Experiment with StartScreen that keeps content loaded between hide/show
- Implement **vlc**/**mpeg** setup to be restartable
- Implement `app.stateful` on/off config
- Drag & drop playlist file support in audio
- Migrate `ActionPane`  actions to `AppActions.kt`& integrate with `Action`, adding icon to Action & keys to ActionData 
- Implement better application running detection: app running without exe and app running from different directory
- Implement `SliderCE` logarithmic scale
  Should support arbitrary N(0-1) -> N(0-1) function and config dsl
- App onStart/End command user defined callbacks
  Requires CommandRunnable of sort
- File rename event
  Similar to file delete event, then support file renaming in file tables/grids 
- Implement multivalued **Song.Artist** support
  Should work like tags
- Implement `Window` & `Component` `Shower` using smart window resize algorithm expanding window to available screen area  
  It is not clear where exactly this would make the most impact. Potentially, custom action to reorganize windows.
- Implement and update widget descriptions as markdown.
- Implement **playlists** table  
  Requires deeper integration with **Update library** action, playlist discovery, etc.
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
- Implement **Settings** sort option DEFAULT/BY_NAME  
  `CheckIcon(sortAlphabetically).icons(IconFA.SORT_ALPHA_ASC, IconFA.SORT_AMOUNT_ASC)`
- Implement isolating song playback and song db to `MASTER`application  
  Right now multiple instances could get in the way of each other
- Improve I/O to support cross-window  
  Challenging. See [issue](/TODO-ISSUES.md#io-ui)

## Known issues
- **Function Viewer** plotting steep functions clipped too soon   
  Solution requires 100% a way to find whether function is not continuous in specified range.
  The current algorithm is not perfect, but mostly does the job
- Window blur effect !support rounded corners other than Windows hardcoded size   
  This is **Windows** OS limitation, `SetWindowCompositionAttribute` does not respect radius and `SetWindowRgn` causes aliasing.
  We have to use `DwmSetWindowAttribute` with `DWMWA_WINDOW_CORNER_PREFERENCE`, but it doesn't support arbitrary radius.
  So when usung **Windows** blur effect the window radius must be 0.5em.
- Recompiling or manual widget loading does not rebind widget inputs properly  
  This needs more investigation

## External bugs
- [-] [jaudiotagger-65](https://bitbucket.org/ijabz/jaudiotagger/issues/65/add-support-for-ogg-opus-format) opus ogg support   
  open, but fixed in maintained fork
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
- [ ] [JDK-8334854](https://bugs.openjdk.org/browse/JDK-8334854)
  reported, open
- [x] [JDK-8334853](https://bugs.openjdk.org/browse/JDK-8334853) ScrollPaneSkin null during sizeToScene
  not a bug, caused by incorrect scale animation setting scaleX/Y to 0.0
- [x] [KT-41373](https://youtrack.jetbrains.com/issue/KT-41373) Reflection: Anonymous class inspection fails  
  fixed
- [x] [KT-41310](https://youtrack.jetbrains.com/issue/KT-41310) ReflectionL Creating `KType` from `Java`  
  reported, not an issue
- [x] [KT-41309](https://youtrack.jetbrains.com/issue/KT-41309) Kotlinc: `-cp`  requiring escape after Kotlin update
  reported, documented, fixed
- [x] [KT-41300](https://youtrack.jetbrains.com/issue/KT-41300) Kotlinc: Incorrect warning on Windows "advanced option value is passed in an obsolete form" on unquoted argument with delimiter character  
  fixed (by quoting), documented at https://kotlinlang.org/docs/compatibility-guide-14.html#compiler-arguments-with-delimiter-characters-must-be-passed-in-double-quotes-on-windows
- [x] [KT-38817](https://youtrack.jetbrains.com/issue/KT-38817) Bad behavior:`String.capitalize()`  
  fixed. See `TextUtilTest`.
- [ ] [KT-22792](https://youtrack.jetbrains.com/issue/KT-22792) Reflection:`objectInstance` throws IllegalAccessException  
  reported, worked around using objectInstanceSafe method
- [-] [KT-14119](https://youtrack.jetbrains.com/issue/KT-14119) Bad API: `String?.toBoolean()`  
  reported, implemented wrongly
- [-] [KT-52460](https://youtrack.jetbrains.com/issue/KT-52460) Slow Kotlin compilation
  reported, native excelsior compiler has been discontinued; using kotlin-compiler-embeddable jar improves speed dramatically due to daemon; K2 also improves speed a lot 
- [ ] [KT-54348](https://youtrack.jetbrains.com/issue/KT-54348) False positive: "Inappropriate blocking method call" with coroutines and Dispatchers.IO.invoke
  reported, open
- [ ] [KT-54348](https://youtrack.jetbrains.com/issue/KT-69539) KClass::createType fails for inner nested java class
  reported, open
- [-] [OSHII-2016](https://github.com/oshi/oshi/pull/2016/files) Disabled performance counters check failing
  reported, worked around by enabling the counters on app startup using `CoreOshi::class`
- [x] [694](https://github.com/haraldk/TwelveMonkeys/issues/694) Incorrect subSampling out put for bmp image  
  fixed
- [x] [695](https://github.com/haraldk/TwelveMonkeys/issues/695) No image reader for some webp images  
  reported, implemented
- [x] [704](https://github.com/haraldk/TwelveMonkeys/issues/704) Performance of WebP Reader  
  reported, fixed
- [x] [706](https://github.com/haraldk/TwelveMonkeys/issues/706) JDK19 & Loom compatibility  
  reported, fixed
- [ ] [711](https://github.com/haraldk/TwelveMonkeys/issues/711) Expose WEbP image animation data  
  reported, open
- [x] [695](https://github.com/kwhat/jnativehook/issues/420) Consuming global hotkey modifier key release
  asked, figured out (not an issue, modifiers should not be consumed)




