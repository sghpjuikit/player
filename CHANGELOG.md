# Changelog
All notable changes to this project will be documented in this file. Format based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [2.1.0] 2021 09 2

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