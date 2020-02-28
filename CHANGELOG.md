# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Rename application to `Spit Player`
- Reduced memory consumption (up to 30%) due to adopting AdoptOpenJDK with OpenJ9
- Reduce application size (largely due to removal of javafx.web)
- Implement `--dev` developer mode argument option
- Implement `--uiless` argument option
- Implement ui less application mode
- Implement widget recompilation when application jars are more recent
- Implement widget reload button for widgets that do not load properly
- Implement widget focusing when widget opens to be used (e.g. from context menu)
- Implement automatic setup for Vlc player
- Implement automatic setup for Kotlin compiler
- Implement automatic setup for ffmpeg compiler
- Implement application header progress indicator title showing running task count
- Implement `Object Info` widget
- Implement `Favourite Locations` widget
- Implement `Screen Rotator` plugin dialog
- Implement `Start Screen` plugin
- Implement `Wallpaper` plugin File.setWallpaper action
- Implement `Wallpaper` plugin to retain wallpaper when application closes
- Implement new popups
- Implement application settings import/export/defaults actions
- Implement application settings filter
- Implement select settings group when opening setting from application search
- Implement confirmation popup for remove songs from library
- Implement padding settings editor
- Implement checklist settings editor
- Implement 0 length support for chain settings editor
- Improve application settings order/hierarchy
- Improve SLAVE application startup (due to not starting some plugins)
- Improve initial window showing performance
- Improve video cover extraction performance (reuses previous output)
- Improve overlay animation performance and ux
- Improve image loading performance for common formats dramatically
- Improve image loading performance
- Improve `Tagger` widget UX
- Improve `Tagger` reading performance
- Improve `FileInfo`
    - use checklist to select which fields to display
    - improves UX by adjusting layout automatically depending on visible fields (unifies cover & rating with other fields)
- Display date & time in UI in human readable format
- Hide technical settings from UI & avoid persisting computed settings
- Icons are focusable using TAB and invokable using ENTER
- Popup close button has been removed and pin button UX improved

### Fix
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
- Image size reading sometimes fail
- Image loading blocks ui sometimes
- Audio Library import ui visibility and layout
- Shortcuts settings not being applied after restart
- Settings editors for complex values not understanding exact type (JVM erasure)
- Settings editors general null support
- Settings editors not applying value sometimes
- Settings editors not showing correct value for some observables
- Settings editors not showing correct warning tooltip text sometimes
- Settings editors UX
- GridView focus not applied properly sometimes
- GridView search style not discarded on cancel
- GridView search sometimes not selecting proper item
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
- `Tagger` widget displaying wrong cover in rare cases
- `Tagger` widget displaying wrong cover size cases
- Song `COMMENT` field never reading as `<none>`
- View filter not working in some cases
- View filter closing instead of focusing on Ctrl+F
- Search text field clear button showing incorrectly sometimes
- Application search cell graphics layout
- Application search empty tooltip visible
- Application skin setting not restored properly
- Application rating skin not allowing null value sometimes
- Plugin fixes
  - Non running plugins are instantiated
  - `Waiffu2k` plugin waiffu2k binary config is not persisted
  - `Waiffu2k` plugin scaling coefficient not configurable
  - `Guide` is now a plugin and can be disabled completely
- JavaFX Screen.getScreens fires invalid events with no screens
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
- Dependencies: javafx.web, javafx.fxml
- Annotation support for Config constraints, use delegated configurable properties and ConfigDefinition instead

## [v0.7.0]
### Added
- Initial release features