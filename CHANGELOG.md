# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Avoid starting some plugins for SLAVE application instances
- Improve DirView cover loading performance
- Improve video cover extraction performance (reuses output)
- Improve overlay animation performance and ux
- Implement `--dev` developer mode argument option
- Implement `--uiless` argument option
- Implement ui less application mode
- Implement widget recompilation when application jars are more recent
- Implement automatic setup for Vlc player
- Implement automatic setup for Kotlin compiler
- Implement automatic setup for ffmpeg compiler
- Implement `Start Screen` plugin
- Implement `Wallpaper` plugin File.setWallpaper action
- Implement `Wallpaper` plugin start/changeWallpaper() animation
- Implement `Wallpaper` plugin window hiding when no image
### Fix
- Main window icon showing in ui-less application mode
- Widgets launched as new process not closing application 
- Widgets not recompiling when application jars have newer version
- Image loading blocks ui sometimes
- Audio Library import ui visibility and layout
- Shortcuts settings not being applied after restart
- GridView search style not discarded on cancel
- Overlay settings change not taking effect
- Overlay blur affecting content in some scenarios
- Icon text layout
### Removed
- experimental Spectrum widget
- Terminal widget for its dependencies and maintenance overhead  
  Standalone terminal applications can do a better job anyway
- experimental WebReader widget

## [v0.7.0]
### Added
- Initial release features