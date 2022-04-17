# Changelog
All notable changes to this project will be documented in this file. Format based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Latest]

- Update Kotlin to 0.6.20
- Implement **Mouse Info** widget
- Implement reopen widget settings after recompiling/reloading widget
- Improve widget focus traversal to work when only one widget is open
- Improve SwitchContainer tab aligning UX [smarter Alt+Up shortcut]
- Improve **Spektrum** widget
  - Implement effects (mirroring, pulsing, shifting)space smoothing, bar styles
  - Implement spatial smoothing
  - Implement multiple bar styles
  - Implement multiple bar input data choices 
- Improve transparent content styling and UX
- Improve window radius styling and UX
- Improve window & Component context menus
- Improve **Settings** widget layout in some situations
- Improve **Converter** widget:
  - Improve `CTRL + V` handling
  - Simplify initial UX state
  - Improve functor picker to use text field with autocompletion search instead of combobox
- Improve **Widget management** UI [add more information in some situations]
- Fix widget compilation never finishing and not reporting errors sometimes 
- Fix **SongTable**, **SongGroupTable**, **Playlist** widgets not focusing properly
- Fix form layout type settings not changed though form icon
- Fix window menu fullscreen item not working
- Fix launching widgets in new process failing due to jvm args considered app args

This update continues improving UX by fixing issues and preventing user to get into unintuitive UI situations.

Windows UX have been improved. Popups respect onTop and focus of their parents and gain/lose top z-order properly.
Transparent windows now support click-through behavior and remain interactive. For better UX, click-through is off when window is focused.
This allows using non-interactive always-on-top widgets for HUDs and overlays.
There is still work to be done regarding cumbersome control of transparent windows with no content, window resizing and so on.

The **Spektrum** widget has been massively improved.
It now supports large number of separate settings that can be freely combined into unique effects.
For instance, the widget allows specifying various data as input (FFT, volume, etc), various data transformations, drawing styles and
multiple effects, such as pulsing, mirroring, shifting and more. These are all applied separately and more choices could easily be implemented.
The widget could still do with some drawing optimizations and also suffers absolute vs relative coordinate/sizing issue that I am not sure how to tackle at the moment.

The **Comet** widget has been brought up to date.
Several issues have been fixed, mainly small game graphics, which is scaled up 2x.
Performance has been improved to cause no issues even in 4k, by rendering the Canvas at smaller size. This does introduce slight blurring for Canvas elements.
There are still number of issue, such as experimental features and performance issues regarding number of objects potentially crashing the game.
The widget may become bundled with the application in the future.

The **Converter** widget have long suffered from two issues.
First, initial state was set to Unit with transformation chain of containing `As Is` function.
The initial chain is now empty and pasting any content changes the input, rather than appends it to transformation chain.
Second, the functor picker, using ComboBox, did not make it easy to find sought after functor.
The selector is now a TextField with an autocomplete, which allows smartly to select the functor based on name and type.

For this both the chain and autocompletion components needed some improvements.
Particularly the autocomplete. It now handles events properly so to allow controlling the suggestions list and text field at once.
This solution is a satisfactory replacement for the non-existent ComboBox with search, which proved too problematic to realize. 

## [4.0.0] 2022 03 03

- Update **JDK** to 17 [improves performance & security]
- Implement recommended **Custom** widget classes as widgets
- Implement pause/resume icon for playlist table rows
- Improve icon hover/focus/select interaction (fixes subtle UX issues in some controls)
- Improve names/icons for some widgets
- Improve check menu check icons [use more compact icons]
- Implement application process name argument [makes it easier to identify Java process from process command]
- Implement **Song Info (Small)** widget [uses `SongInfo`]
- Implement **Album Info (Small)** widget [uses `SongAlbumInfo`]
- Implement **White Leaf** skin
- Implement **Dark Leaf** skin
- Improve styling
  - Table/List/Grid/Tree/TreeTable view headers are bigger, bold, align based on content, have proper padding, show borders on hover
  - Table/List/Grid/Tree/TreeTable view footers are bigger, better vertically aligned, align based on content, have proper padding, shw borders on hover
  - Separator is more sleek and less distracting
  - **ContainerSwitch** tab gap respects tab index [the center tab always covers entire window]
  - Check menu check icons use more compact icons
  - Check menu check icons use hover effect on item hover
  - Use bold font for some selections
  - Window content clipping respects radius
- Improve popup content complexity [use Layout instead of ContainerSwitch]
- Improve **Icon browser** widget
  - Improve layout & UX
- Improve **Git projects** widget
  - Improve layout & UX
- Improve **Tester** widget
  - Implement remembering selected content
  - Implement **Mouse events** testing suite
  - Implement **CssBorders** testing suite
  - Improve layout & UX
- Improve widget **Spektrum**
- Improve widget **Voronoi**
  - Update `JTS` dependency
  - Fix moiré effect by improving cell position precision
  - Implement better smoothing
  - Implement use skin colors 
- Improve widget **Function Viewer**
  - Handle very large numbers correctly [Use `BigDecimal` everywhere]
  - Implement function area highlighting
  - Implement use skin colors 
- Improve `GridView` context menus
- Fix `ProgressBar` not hiding after showing 0/0 progress
- Fix importing songs to library not working properly
- Fix `ActionPane` not returning data correctly sometimes
- Fix `ConfigPane` not respecting margins
- Fix `Configurator` widget content scroll pane not respecting height
- Fix windows opened through actions in **Search close** immediately close after showing
- Fix showing object settings showing application settings temporarily and with bad UI performance

This update brings lots of pleasant improvements. Particularly styling.

First, finally, **JDK** has been updated. It took 3 years to get over **JDK12**. Time flies.
The `linkJDK` Gradle task has been removed from build dependencies and now requires manual run. It is more of a convenience anyway.
Now, **JDK** updates are very comfortable. By the way, the project still uses **OpenJ9**, now called Semeru.

The application process command has received a no-op argument to help to distinguish the process from other Java processes.
This would not be such an issue if the **Launch4j** was set up to wrap the .jar into .exe, but it currently does not work well for 64-bit version.
This requires more research and testing to implement right, so for now, at least the command was enhanced.

The widgets **Icon browser**, **Git projects** and **Tester** now use much improved and unified UI for selecting content.
Focusing, hovering, selecting from the list of content choices using both mouse and keys is supported and works very nicely.

There are two new skins: **White Leaf** and **Dark Leaf**.
These are light and dark green themes easy on the eyes and with good contrast.

There are two new widgets: **Song Info (small)** and **Album Info (small)**.
These display basic song and song album information. The former is an improved version of the default **Now Playing** notification content.
This change is following the trend of many small widgets instead of few complicated ones. This gives more power to the user.

New names and icons for some widgets make them easier to find and use.
Even better, all the registered `Node` classes, viable to be used as widgets are now available as separate widgets.
This was surprisingly trivial to implement, and it truly improves the UX.
In the future, there may be widget metadata support for these widgets, such as icons or descriptions. There is potential for a lots of improvements here.

Another new feature is a graphical playlist table column for a pause/resume icon.
The icon allows controlling the playback from the **Playlist** widget - definitely useful.
In the future, there will probably be more such columns and with automatic width resizing and styling support.

Application's tables and other table-like components have also become more usable and elegant.
The ui elements relying on `Canvas` now support skin colors.

The **Function Viewer** widget has full `BigDecimal` support now.
This was not easy, different library for expression evaluation was necessary and using `BigDecimal`s properly is difficult.
There were lots of arithmetic errors related to precision, resulting in incorrect values, bad performance, exceptions and even infinite loops.
These are all solved and the plotter not supports unlimited zooming and precision.
The graph also highlights area under the function, which is elegant and quite helpful.

The **Spektrum** widget has also become prettier, as it uses more sophisticated and configurable smoothing.
These changes have been applied from the original project the widget is based on.

## [3.1.0] 2022 02 07

- Implement **widget I/O** for any type of window (docks, overlays)
- Implement **Save component** action to save component loaded from a previously exported file
- Implement **Convert Image** action for images. Supports batch conversion and is very fast on SSD due to io parallelism
- Implement **GitProjects** widget selection & context menu
- Implement **Inspect data** action content detection (user can get use raw data using **CTRL** on **drag**, **SHIFT** on **CTRL+V**)
- Implement **Inspect data** action content preview for `String`
- Implement `TextArea.wrapText` context menu item
- Implement **Change font size** on **CTRL+SCROLL**
- Implement **Show Lyrics** action (this is candidate for new widget)
- Implement **Synchronize file times** action for restoring file creation/modification time after copy
- Implement better tracking of background activities and their error results
- Improve entering/exiting layout mode performance
- Improve logging output [reduce logging for some libraries]
- Improve context menus, context menu item icon tooltip, tooltip layout
- Improve `DatePicker` & `DatePickerContent`
  - Implement selection
  - Implement locale-specific day of the week order
  - Improve styling
  - Fix incorrect calendar day numbers sometimes
  - Fix incorrect calendar layout sometimes
  - Fix unable to instantiate inside **Custom** widget
- Fix content detection often detecting text as a relative file or uri
- Fix image loading some images at full resolution (improves performance)
- Fix application not shutting down gracefully sometimes
- Fix `Collection.size` & `Map.size` functor output type
- Fix some configs and editors not using `UnsealedEnumerator` constraint in some cases resulting in no autocompletion
- Fix `GridView`s `GridInfo` not taking active filter into account, resulting in incorrect selection statistics
- Fix some actions/menu items are not available due to not registering 
- Fix drag & drop when external application sets `FILES` to empty list
- Fix **Logger** widget `area.wrapText` observability and default value not applied correctly
- Fix **cmd** file lookup not finding certain files sometimes

This update brings lots of usability improvements.

The main improvement is layout I/O UI.
The I/O ui was originally tied to **Switch** container, which provides the ui move/zoom capability, which affects the **IO**.
Because of this, layouts, which do not use **Switch** container (docks and overlays) did not support input/output editing.
The I/O layer has been made to support both **Layout** and **Switch** (which effectively acts as top level).
So there is no need to figure out which of the two needs to have it and no extra containers just for I/O.
When **Layout** contains **Switch**, it lets **Switch** take care of I/O. Now every content supports I/O, including docs and overlays.

Another improvement is content inspection/processing using **ActionPane**.
First, there is automatic content detection after **CTRL+V** or **drag & drop**.
Second, just like `Collection`s are displayed in table, `String`s are now displayed in `TextArea`, which can now change its wrapText settings through context menu.
This will be further improved to support images, or displaying text representation of objects that have no natural representation.
Ideally, this functionality would be absorbed into **Object Info** widget.

Another improvement is bgr task management.
First, task invoked through forms/popups now also display in task list. This makes closing the window not a destructive operation (as progress could no longer be tracked).
Second, if the task ends with error, this error can be displayed by clicking on the task result warn icon in the task list, which opens **ActionPane**. This makes it possible to track task errors.
Third, tasks that do parallel processing or produce multiple outputs (potentially with errors) also return future, can be tracked and have their errors displayed.
Actually acting on the errors is still not very easy.
The table should get `Try.Ok/Error` filtering predicates, nested columns (for `Ok` and `Success` branch) and transformation capabilities (like **Converter** widget).
These may come in future updates.

This update refactors the code to further unify actions for `ActionPane` and actions for `ContextMenu`.
Introduced is `ActContext`- an invocation context carrying auxiliary data, such as source of the event.
This enables using actions outside `ActionPane`, even if they use it. Now any action can be used in context menu.

Regarding the application shutdown fix - the old solution was brought back to forcefully shut down JVM even if non-daemon threads are running.
This is because proper cleanup is never guaranteed even if `App` is already stopped.

## [3.0.0] 2022 01 24

- Update **Kotlin** to `1.6.10`
- Implement application locale settings
- Implement locale-specific formatting for data in various parts of the application
- Implement **Node** widget object instantiation error notification
- Implement window move hint cursor - show **MOVE** cursor when **ALT** is pressed
- Implement **FreeForm** container window move hint cursor - show **MOVE** cursor in layout mode
- Implement **FreeForm** container window styling (show gap between windows)
- Implement more functors (word/sentence splitting, list operators)
- Improve tables
  - Sorting performance (up to 50 times faster)
  - Widget/ui loading performance (sorting does not block ui now)
- Improve component controls
  - Remove lock icon from component control header icons
  - Expose **Switch** container ui controls via ui, like every other container
  - More Component context menu entries
  - unify **Custom** widget menu with any widget controls header icons menu
- Improve context menus
  - Add more items for component
  - Unify Custom widget menu with widget controls header icons menu
- Improve song drag & drop UX - do not show placeholder for song tables
- Improve component drag & drop UX
  - Fix **FreeForm** container window closing sometimes after swapping content
  - Fix drag & drop placeholder sometimes at wrong position
  - Fix **FreeForm** container window move preventing component drag
  - Fix incorrect placeholder size sometimes
  - Avoid fading out content when placeholder does not cover entire area
- Improve editor constraint messages with values (uses human-readable converter)
- Improve widget **Command Grid**
  - Add max cell columns settings
- Improve widget **Image**
  - Make focusable
- Improve & refactor code, update dependencies
- Improve widget **Function Viewer**
  - Handle very large numbers correctly
  - Add validations
  - Use `BigDecimal` for coordinates
  - Use human-readable text for numbers where possible (avoids scientific notation)
  - Hide coordinates label when mouse is not hovering
- Fix `DateClockDigitalIos` crashing for some dates
- Fix slow selecting of lots of items in table, such as when **CTRL + A** is used (original JavaFX bug)
- Fix song group table showing incorrect selection statistics sometimes

I took a little break from coding, but I am back. This version brings major improvements, so I am bumping up to **3.0.0**.

First, the project is now finally using **Kotlin**/**Java** toolchain, which automates **JDK** setup into a build.
Yes, it means the build does not require any JDK installed!
So developers can now simply clone the repository and build/run through **Gradle** with no extra steps, without leaving **CLI**.
The build is much simplified, as most of the Java checking/setup has been removed.
Previously, migrating **Java** was a headache, but not anymore. I plan to use **Java 17**, a much-needed upgrade from **12**.

As for the application, several areas have received major upgrade.

Tables are the heart of the library as well as pride of this player.
This version finally fixes slow selecting, which could in large tables take up to several minutes!

Table sorting has been identified to cause problems, such as taking up to several seconds in some scenarios!
After some refactor, the speedup has turned out to be huge, for **Song Table (Big)**, which is the view-everything table, the reduction for 50k songs was 2.5s -> 50ms.
50ms still blocks ui for 3 frames (on 60Hz display), enough to cause ui loading problems, particularly during animations.
UI loading in general is problematic, because the single-threaded nature of UI frameworks will not allow for optimization.
But, I managed to make sorting for all tables asynchronous. This was tricky because in some scenarios the application must not expect the items in correct order.
But now table performance does not degrade with item count. UI responsiveness when table is loading or sorting is now 60hz smooth!

I also simplified widget/container controls. There is fewer icons and much better menu.
The changes provided a noticeable performance gain while moving/zooming the UI, which is now 60Hz smooth as well.
In edit mode, the layout can be traversed by **RMB**/**LMB**, and now it includes **Switch** container, which, as top container, was not previously accessible.

**FreeForm** container, which provides windows-in-window, can be used in screen overlays or bottom-most screen overlays.
Therefore, proper functioning is absolutely integral. After more use/testing several problems have been identified.
With this update, it has become much easier to use thx to improved styling and drag&drop, which it relies on in lots of situations.
This component is complicated, and I will continue to improve it.

Besides these changes, many smaller improvements and fixes have been made.
Both the project and the application have never been in better state.

For the future, I have no exact plans yet. I will continue to go through the backlog - it contains lots of non-trivial ideas.
However, the priority will also be to keep improving UX.

## [2.1.0] 2021 09 26

- Improve widget i/o system
  - Avoid creating ui elements for i/o in other windows
    - Improves performance
    - Removes visual artefacts
  - Implement context menus for linking/unlinking widget i/o
  - Implement support for application-wide `Input` and `Output`
  - Implement **Song Library** and **Playing song** as `Output` instead of `InOutput`
  - Remove ui support for application-wide i/o
- Improve markdown styling
- Improve Json serialization for arrays, unsigned numbers and nullable types
- Improve String char16, char32, grapheme facilities
- Improve widget & plugin settings UX
  - Add context menu support for ComponentFactory & PluginBox
  - Simplify & improve layout
  - Improve interaction (remove obscure behavior, add hover effects)
- Improve component ui controls header icons  
  **Settings** and **Actions** icons were merged into **Menu icon**, which is more flexible as well as consistent with window headers.
  In the future, the menu may be customizable through `CoreMenus`.
- Improve code
- Fix several widget serialization/deserialization issues

## [2.0.0] 2021 09 19

- Implement smart window resize (`window drag + SHIFT + LMB`) - resize window to available screen area  
  This behavior may be tweaked in the future, and leveraged more extensively.
- Implement running programs through .lnk files
- Implement `Class` & `KClass` editor autocomplete  
  It is difficult to obtain loaded class list, so instead hardcoded list is provided by developer in form of a resource file.
  More entries may be added to the list in the future.
- Improve markdown styling and UX
- Improve code
- Fix wrong table group column name in some table column menus

## [2.0.0] 2021 09 10

- Much improved handling of song's `TAGS: Set<String>` tag. The field:
  - has better editor
  - is displayed and can be quick-edited through **FileInfo** widget
  - supports autocompletion
  - supports song table grouping
- improved song chapters support in **PlayerControlsCircle** widget's circular seeker
  - layout and sizing issues
  - chapter hover effect
  - chapter click shows chapter popup & edit
- more converting functions
- fixes related to styling `MenuBar`, song tagging (`wav`), threading issues, reflection, and more

The tags grouping was tricky, because it is a multivalued tag.
Now that multivalued tags are supported, artist tag could also become `List<String>`, which would fix long-standing issue of grouping by artist.
Adding song chapters through circular seeker is not yet done, because it is not clear what the UX should be like.

## [2.0.0] 2021 09 03

- improved **Node** widget with better context menu & class suggestions. Also renamed to **Custom**.
- Improved ComboBox styling & autocompletion UX
- Improved table sorting through context menu & add playlist table sorting by db columns. It is finally possible to sort playlist by rating
- Improve **Converter** widget layout and Action settings
- Improved TextArea context menu
- Improve markdown node (drag&drop, anchors, animations)
- Improve ContainerFreeForm (resizing, absolute sizes, position aligning)
- Simplify window header & introduce window context menus
- Unify App actions, App context menu, Tray context menu, Window context menu
- More minor tweaks

The new context menus are an important addition and solve the issue of feature discoverability. I wanted to avoid this for years, but there simply is no better (compact) way. Anyway, now I can add Help/Documentation/About pages

The markdown still needs more work with link resolving as well as file monitoring and automatic reload. [RichTextFX](https://github.com/FXMisc/RichTextFX) may be used to produce more optimized and powerful UI.

## [2.0.0] 2021 08 27

- Support for Markdown text was implemented.
  - **GameView** widget is now using this for game info
  - new **GitProjects** widget has been implemented to browse git projects. It simply lists git projects in a directory and shows the `README.md` files. More features may come later
- **Kotlin** `1.5.30`. In the future I'll use gradle jvm toolchain so JVM sets up automatically. This will alleviate the remaining pains when setting up the project.
- Added cool stuff to **Voronoi** visualisation widget

Finally, an app user documentation can be written, as markdown of course. This will take a while and probably result in improving UX all around.

What's next? Well, as is, the application is very polished and has a lot of powerful features. I'd consider this state more or less ready for making available to anyone out there.

1. Right now, I'd like to keep improving the window system to make it easier to set up desktop-level widgets.
   Improving the Node widget will be necessary as well as discovery of recommended ui components for Node widget.
   This also involves more FreeFormContainer features, such as absolute resizing, snapping and styling.
1. I also want to integrate context menus and object actions (icons in `ActionPane` overlay) more closely, so that these are merely a different UX for the same thing.
   This requires implicit action parameter handling in menu (probably by displaying a Form for each parameter) and more convenient navigation in `ActionPane` (back & forth, custom ui, etc.).
   This also several other technicalities, so it will be challenging to come up with pleasant solution.
   The actions will probably end up defined as custom objects (and registered in global pool) rather than ordinary functions, which is unfortunate, but maybe special delegated properties could help.
1. Better initial experience & application documentation would also be nice, for example there is still no **About page**.
   Window headers are also too cluttered, maybe ordinary menu bar would be a better idea. Window headers could be their own components as well.

## [2021 08 21]

- improved how windows & window headers auto-hide and improved styling. The reworked functionalities make the application much more pleasant to use.
- improved `GpuNvidiaInfo` UI, although it requires more work.

## [2021 08 19]

- improved documentation, provided commands to build executable application from source  
  I'm thinking this may actually be better than doing releases, will see.
- improved `Container`s. They now have settings and support padding. Free-form has several issues fixed, further improving its use in various use-cases.

## Older

### Added
- Rename application to `Spit Player`
- Reduced memory consumption (up to 30%) due to adopting AdoptOpenJDK with OpenJ9
- Reduce application size (largely due to removal of javafx.web)
- Implement `--dev` developer mode argument option
- Implement `--uiless` argument option
- Implement ui less application mode
- Implement widget recompilation prioritizes open widgets
- Implement widget recompilation when application jars are more recent
- Implement widget reload button for widgets that do not load properly
- Implement widget focusing when widget opens to be used (e.g. from context menu)
- Implement auto-setup for Vlc player (Windows-only)
- Implement auto-setup for Kotlin compiler
- Implement auto-setup for ffmpeg compiler
- Implement application header progress indicator title showing running task count
- Implement new popups (fixes various issues related to popups)
- Implement application settings import/export/defaults actions
- Implement application settings filter
- Implement select settings group when opening setting from application search
- Implement confirmation popup for remove songs from library
- Implement padding settings editor
- Implement checklist settings editor
- Implement 0 length support for list editor
- Implement slider fill
- Implement Unicode character icons
- Implement more skins
- Implement application help page & shortcut
- Implement component help page & shortcut
- Implement window ALT+drag resizing+maximizing
- Implement window header animation
- Implement Webp image read support
- Implement Form support for asynchronous actions
- Implement webp image support
- Improve application settings order/hierarchy
- Improve SLAVE application startup (due to not starting some plugins)
- Improve initial window showing performance
- Improve video cover extraction performance (reuses previous output)
- Improve grid cell hover/focus effect
- Improve overlay animation performance and ux
- Improve overlay icon layout
- Improve drag&drop placeholder ux
- Improve shortcuts ui display (use unicode glyphs for keys)
- Improve SwitchContainer UX:
    - Focusing now aligns tabs so the focused content is visible when switching focus
- Improve BiContainer UX:
    - snapping to relevant positions (edges, 1:1 ratio, centre)
    - collapsing only taking visual effect on mouse release
    - resizing glitches
    - absolute position not restoring properly sometimes
    - absolute position being distorted on certain resize operations
- Improve object inspection type inference
    - supports generics, displays `List<SomeType>`
    - displays nullability, displays `List<SomeType?>?`
    - displays variance, displays `List<in/out SomeType>`
    - displays best-effort list type by inspecting elements
    - displays unknown types as STAR projections `*`, displays `List<*>`
- Improve image loading performance for common formats dramatically
- Improve image loading performance
- Plugins
    - Implement plugin metadata (simple)
    - Implement plugin management settings
    - Implement `Screen Rotator` plugin dialog
    - Implement `Start Screen` plugin
    - Implement `Wallpaper` plugin File.setWallpaper action
    - Implement `Wallpaper` plugin to retain wallpaper when application closes
    - Implement `Notifier` plugin setting that shows enabled notification types
- Widgets
    - Implement multiple ways to open widgets (configurable by user), i.e., window, popup, overlay, etc
    - Implement widget metadata (simple)
    - Implement widget management settings
    - Implement widget global state
    - Implement `Tester` widget for developers, showcases property sheets and animation interpolators
    - Implement `Object Info` widget
    - Implement `Favourite Locations` widget
    - Implement `File Explorer` composed widget (exported layout)
    - Implement `Hue` widget to control Phillips Hue light system
    - Implement `Dir Viewer` icon cell & option to toggle thumbnails/icons
    - Improve `Dir Viewer` cell size change performance
    - Improve `Tagger` widget UX
    - Improve `Converter` widget UX
    - Improve `Tagger` widget UX
    - Improve `Tagger` reading performance
    - Improve `FileInfo`
        - use checklist to select which fields to display
        - improves UX by adjusting layout automatically depending on visible fields (unifies cover & rating with other fields)
    - Improve `ImageViewer` widget UX
        - remove ability to display thumbnails (use grid widget for this)     
- Display date & time in UI in human-readable format
- Hide technical settings from UI
- Avoid persisting computed and other unwanted settings
- Handle icon focus traversal (TAB) and action invoking (ENTER) better
- Popup close button has been removed and pin button UX improved
- Info buttons in various parts of UI have been removed, `,` and `.` shortcuts now intended for same use case

### Fix
- Playback seek volume fade
- Playback loop song stopping playback
- Main window icon showing in ui-less application mode
- Widgets launched as new process not closing application 
- Widgets not recompiling when application jars have newer version
- Widget infinite recompilation when compiling invalid class
- Widget type id & widget name used interchangeably
- Widget compilation not showed in ui sometimes
- Widget factory removed when file with the same name as widget directory is removed from widgets directory
- Widget not reloading after factory becomes available sometimes
- Widget's technical name displayed instead of user defined name in ui sometimes
- Widget inputs not rebound correctly on recompilation
- Widget inputs not rebound correctly on fxwl loading due to duplicated id 
- Widget inputs applied too extensively during widget loading
- Image size reading sometimes fail
- Image loading blocks ui sometimes
- Audio Library loading fails if class version changed (now automatically reloads)
- Audio Library import ui visibility and layout
- Audio Library logging errors when no saved library exists yet
- Shortcuts settings not being applied after restart
- Settings editors for complex values not understanding exact type (JVM erasure)
- Settings editors general null support
- Settings editors not applying value sometimes
- Settings editors not showing correct value for some observables
- Settings editors not showing correct warning tooltip text sometimes
- Settings editors UX
- GridView focus not applied properly sometimes
- GridView search style not discarded on cancel
- GridView search not selecting proper item sometimes
- GridView scrolling when selecting last row
- GridView cover size change throws exceptions
- Overlay settings change not taking effect
- Overlay blur affecting content in some scenarios
- Icon with text has wrong layout
- Check icon with one icon not distinguishing on/off state
- Starting some processes blocked until application stopped
- Progress indicator not centered in certain sizes
- Drag & Drop support for COPY/MOVE/LINK
- Writing certain custom fields' values not working correctly
- Widgets
    - `Tagger` widget displaying wrong cover in rare cases
    - `Tagger` widget displaying wrong cover size cases
- Song `COMMENT` field never reading as `<none>`
- View filter not working in some cases
- View filter closing instead of focusing on Ctrl+F
- Search text field clear button showing incorrectly sometimes
- Application search results are hard to disambiguate (show icons)
- Application search slower for lots of items
- Application search cell graphics layout
- Application search empty tooltip visible
- Application skin setting not restored properly
- Application rating skin not allowing null value sometimes
- Plugins
  - Non-running plugins are instantiated
  - `Waiffu2k` plugin waiffu2k binary config is not persisted
  - `Waiffu2k` plugin scaling coefficient not configurable
  - `Guide` is now a plugin and can be disabled completely
  - `Directory Search` does not find any results
  - `Directory Search` & `AppSearch` logging errors for inaccessible files
- JavaFX Screen.getScreens crashing in rare situations
- Popup window fixes
  - popup does not receive focus on show not even when clicked
  - popup not returning focus to previously focused OS window
  - popup not resizable by OS
  - popup initial size and position not correct sometimes
  - hiding popups with ESCAPE closes only focused popup
  - popup not handling key events sometimes (instead passing them to owner)
  - popup focus style not having effect
  - popup close throwing 'window already closed' exception
  - certain popup properties not styleable
  - help popups closing too eagerly
  
### Removed
- Runtime JVM options editing. Was experimental and proved unnecessary/complicated.
- `Spectrum` widget. Was experimental and incomplete
- `WebReader` widget. Was experimental and proved incomplete
- `Terminal` widget for its dependencies and maintenance overhead. Standalone terminal applications can do a better job.
- Application (?) icons with UX tooltips (use help/component help)
- Dependencies: javafx.web, javafx.fxml
- Annotation support for Config constraints, use delegated configurable properties and ConfigDefinition instead

## [v0.7.0]
### Added
- Initial release features