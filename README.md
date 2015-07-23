# Player

JavaFX based audio player application.
Aims:
- customizability - allow user use the application how he wants, lots of settngs, skins, etc
- modular user interface - ability to 'make your own gui'
- support fancy features like: rating in tag, time comments, big covers, advanced & intuitive library management
- song independence from library (you can freely move files, rename them and never lose any metadata information)
- usability - efficient workflow and no repetitive tasks
- responsive - no waiting for tasks, no modal dialogs

## Features

###playback 

Filetypes:
- mp3
- mp4, m4a
- wav
- ogg, flac (not yet, but planned)

Protocols:
- file
- http

In addition:
- rate of play changable
- left-right stereo volume

###media library 
  
Song library database
- small footprint: roughly about 10MB for 20000 files
- can handle big collections: 40000 no problem. The only drawback can be real-time library refresh speed.
- no dependency: songs always store all data in tag, moving or renaming files poses no problem
- no inconsistencies: any song data changes are always reconfirmed with the files 
- no data loss guarantee: losing database has no effect at all, it can be completely rebuilt anytime. The library serves as a cache, rather than storage.

Song tables:
- big - 30000 songs in playlist = 0 problem
- column customization - set visibility, width, sorting, order of any column for any song attribute
- real-time searching (on type) by any (textual) attribute (artist, composer, title, etc). Scrolls to center and highlights rows - the matches 'pop' visually - the result is very convenient search, particularly on sorted tables.
- real-time chainable filtering by any attribute. Stack filters, individually turn them on/off, negate, filter 
- group by - e.g. table of songs per year, artist, etc. Searching, filtering and sorting fully supported.
- multiple column sorting by any attribute (artist, year, rating, bitrate, etc)
- cascading - link tables to other tables as filters and display only selected items (e.g. show songs by authors selected in linked table)

###tag editing

  Application supports reading and writing song tag information, individually or for number of songs at once. The supported are standard fields like artist or title, but possibly many more later. Interoperability with other players is intended. Some of the less supported by other players, but fully supported (read/write) tags are:

  **Rating** 
- values are in percent values
- granularity limited only by tag (1/255 for mp3, 1/100 for other formats)
- interoperable with other players, but most of them will only recognize the value as 3/5 or so
  
  **Playcount**
Signifies number of times the song was played (the exact definition is left upon the user, who can set up the playcount incrementation behavior arbitrarily.
  
  **Chapters**
Comments added at specific time of the song. They can be added during playback on the seeker and browsed as popup menus. The comments' length should be a non-issue (the upper value remains a mystery, but surpasses 500 characters (definitely for all chapters together) - probably by a large margin).
  
  **Cover**
- image in tag can be imported/exported
- cover read from file location is supported too, looking for image files named:
  - song title
  - song album
  - "cover" or "folder"

### portability 

  The application in its self-contained form:
- has executable .exe
- requires no installation
- requires no software (e.g. java) preinstalled
- runs from anywhere
- does not require internet access
- does not create any file outside its directory
- writes to registry only as long as java itself requires it, an example is cached data by WebView (in-app web browser)
  
extensibility & modularity

  Almost all functionalitiess are implemented as widgets, that can be loaded, closed, moved and configured separately. Multiple instances of the same widget can run at once in windows, layouts or popups. New widgets can be added for created as plugins.
  Some of the existing widgets are:
- Playback & Mini - controls for playback, like seeking. Supports chapters.
- FileInfo - shows cover and information about song (e.g. playing). Allows cover download on drag&drop.
- Tagger - for tagging
- Library & LibraryView - song tables
- ImageViewer - shows images associated with the songs, supports subfolders when discovering the images
- Settings - application settings, but it is also a generic configurator for any object exposing its Configurable API (like widgets or even GUI itself)
- Converter - text -> text converting. Also very handy file renamer. Supports text transformation chaining similar to table filters. Also manual editing using text area, regex, etc.
- Explorer - simple file system browser. Currently slow for big folders.
- Gui Inspector - displays gui objects hierarchically and uses Settings for editing their properties
- Image - displays static image
- Action - contains icon that can execute any application action (supported by shortcut)
  
### gui

  The gui allows custom layouts by providing the ability to divide layouts into containers for widgets. Window can contain multiple layouts similar to virtual desktops. The layouts are easily accessible by dragging the gui horizontally. This provides virtually infinitely large and convenient to navigate working space. The application supports multiple windows like this, which themselves form a higher level layout.
  
  The windows have docking (to other windows or screen edges) feature, and also auto-resize when put into cscreen edges and corners (altogether 7 different modes). There is (so far imperfect) support for system tray, taskbar, fullscreen mode and mini mode as a docked bar snapped to the edge of the screen.
  
### global & media hotkeys

- global hotkey supported - shortcuts dont need application focus if so desired
- media keys supported
- customizable (any combination of keys:  "F5", "CTRL+L", etc)
- number of actions (playback control, layout, etc)

### skin support

- skinnable completely by css
- skin discovery + change + relfresh without requiring application restart
- custom skins

## Screenshots

![ScreenShot](/extra/screenshot1.png)

![ScreenShot](/extra/screenshot3.png)

## The Catch XXII

- Memory consumption varies, but is higher than native apps. Normally i have get about 250-400MB, but it depends on use case. Lots of widgets will eat more MB. 32bit is more effective (64bit effectively doubles memory - so ill only provide 32-bit version). Handling large pictures (4000^2 px) fullscreen on large monitors can also rapidly increase memory consumption (but picture quality stays great).
- Some of the widgets or features are **experimental** or confusing
- No flac and ogg playback. Library supports them.
- No playlist file support (.m3u, etc)

## Download & Use

Download link coming soon.

Starting the application for the first time will run an automatic guide, that will guide you through the basics of the application. Nothing invasive, it can be closed forever very easily, but it does provide some context to the features of the application, so i **recomment the guide**.

Platforms:
- Windows
- Linux (not fully tested, no support for global shortcuts)
- Mac (untested).

Tips:
- use tooltips! Cant stress enough.
- widgets, popups and containers have informative "i" buttons that provide valuable info on possible course of action

## Contribution

In case you are interested in the development or in contribution, send mail to the address associated with this github account.

There are several areas that one can contribute to:
- application core - involves java & javaFX code, OOP + Functional + Reactive styles
- skins - requires very basic knowledge of css and a lots of patience
- widgets - involves java & javaFX code
- testing & bug reporting
- design - logos, overal app motives and spreading the word (no, not yet...)

### Development

The provided files are
- source files
- working directory containing application data.
- libraries

In order to successfully build and run the application the working directory should be set up in the project's settings in the IDE to: '/working dir'. All libraries in the 'extra/lib' must be imported in the project.

In order to be able to develop and use widgets (even those included in this project already) '/src widgets' directory must be included as a source location for source files in project properties.

Proper manuals and HOWTOs will be provided later.

### Skinning

A skin is a single css file that works the same way as if you are skinning a html web site. [a link](http://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html) is an official reference guide that contains a lot of useful information.
The application autodiscovers the skins when it starts. The skins are located in Skins directory, each in its own folder. Press F5 (by default) to refresh skin changes.

## Credits & Licence

You are free to use the application or make your own builds of the project for personal use.

The project is to adopt MIT licence in the future, but for now remains personal. I would appreciate to be
informed before taking any actions that could result in publicizing or sharing this project.

The project makes use of work of sevaral other individuals (with their permission), who will be properly credited later as well.

El Psy Congroo
