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
- Implement `Screen Rotator` plugin dialog
- Implement `Start Screen` plugin
- Implement `Wallpaper` plugin File.setWallpaper action
- Implement `Wallpaper` plugin start/changeWallpaper() animation
- Implement `Wallpaper` plugin window hiding when no image
- Implement new popups
- Implement application settings import/export/defaults actions
- Implement select settings group when opening setting from application search
- Implement confirmation popup for remove songs from library
- Improve application settings order/hierarchy
- Improve SLAVE application startup (due to not starting some plugins)
- Improve initial window showing performance
- Improve video cover extraction performance (reuses previous output)
- Improve overlay animation performance and ux
- Improve image loading performance for common formats dramatically
- Improve image loading performance
- Improve `Tagger` widget UX
- Improve `Tagger` reading performance
- Display date & time in UI in human readable format
- Hide technical settings from UI & avoid persisting computed settings
- Icon focusable using TAB and invokable using ENTER

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
- GridView search style not discarded on cancel
- GridView scrolling when selecting last row
- GridView cover size change throws exceptions
- Overlay settings change not taking effect
- Overlay blur affecting content in some scenarios
- Icon with text has wrong layout
- Starting some processes blocked until application stopped
- Progress indicator not centered in certain sizes
- Drag & Drop support for COPY/MOVE/LINK
- Writing certain custom fields' values not working correctly
- `Tagger` widget displaying wrong cover in rare cases
- View filter not working in some cases
- View filter closing instead of focusing on Ctrl+F
- Application search cell graphics layout
- Application search empty tooltip visible
- Application skin setting not restored properly
- Application rating skin not allowing null value sometimes
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