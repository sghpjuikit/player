# Player

JavaFX based audio player application.
Aims:
- customizability - allow user use the application how he wants, lots of settings, skins, etc.
- modular user interface - ability to 'make your own gui'
- support fancy features like: rating in tag, time comments, big images, advanced & intuitive library management
- no library dependence(moving & renaming files will not result in loss of any information)
- usability - efficient gui & workflow
- responsive - fast and responsive gui, no modal dialogs, etc.
- sexy

## Features

###Playback 

Filetypes:
- mp3
- mp4, m4a
- wav
- ogg, flac (not yet, but planned)

Protocols:
- file
- http

###Media library 
  
Song database:
- small footprint: roughly about 10MB for 20000 files
- big: 40000 no problem
- fast: library is loaded into main memory.
- no dependency: song files always store all data in their tag, moving or renaming files poses no problem
- no inconsistencies: displayed song metadata can only be out of sync with real data if tag is edited by external application
- no data loss guarantee: losing database has no effect at all, it can be completely rebuilt anytime. The library serves as a cache, rather than storage.

Song tables:
- big: 30000 songs in playlist no problem
- smart columns: set visibility, width, sorting, order of any column for any song attribute
- visual searching: by any (textual) attribute (artist, composer, title, etc). Scrolls 1st match (to center) and highlights matches - so they 'pop' visually - the result is very convenient search, particularly on presorted tables.
- powerful filtering - by any attribute, infinitely chainable, custom queries, negation, etc 
- group by - e.g. table of songs per year, artist, etc. Searching, filtering and sorting fully supported.
- multiple column sorting by any attribute (artist, year, rating, bitrate, etc)
- cascading - link tables to other tables as filters and display only selected items (e.g. show songs by authors selected in linked table)

###Tag editing

  Application supports reading and writing song tag information, individually or for number of songs at once. The supported are standard fields like artist or title, but possibly many more later. Interoperability with other players is intended. Some of the less supported by other players, but fully supported (read/write) tags are:

  **Rating** 
- values are in percent values
- granularity limited only by tag (1/255 for mp3, 1/100 for other formats)
- interoperable with other players, but most of them will only recognize the value as 3/5 or so
  
  **Playcount**
  Signifies number of times the song was played (the exact definition is left upon the user, who can set up the playcount incrementation behavior arbitrarily.
  
  **Chapters**
  Comments added at specific time of the song. They can be added during playback on the seeker and browsed as popup menus. The comments' length should be a non-issue (the upper value is unknown, but at least 500 characters (for all chapters together), very likely a lot more).
  
  **Cover**
- image in tag can be imported/exported
- cover read from file location is supported too, looking for image files named:
  - song title
  - song album
  - "cover" or "folder"
  
### Configurability

  All settings and entire user interface layout serialize into a human readable and editable files. These can be edited, backed up or switched between applications.

### Modularity

  Most of the functionalitiess are implemented as widgets, that can be loaded, closed, moved and configured separately. Multiple instances of the same widget can run at once in windows, layouts or popups. New widgets can be added as plugins.
  Some of the existing widgets are:
- Playback & Mini - controls for playback, like seeking. Supports chapters.
- FileInfo - shows cover and information about song (e.g. playing). Allows cover download on drag&drop.
- Tagger - for tagging
- Library & LibraryView - song tables
- ImageViewer - shows images associated with the songs, supports subfolders when discovering the images
- Settings - application settings, but it is also a generic configurator for any object exposing its Configurable API (like widgets or even GUI itself)
- Converter - object -> object converting. Displays objects as text while allowing user to apply functions transformations. Handy file renamer and per-song tagger. Supports object lists, text transformations, manual text editing, regex, writing to file etc.
- Explorer - simple file system browser. Nothing bigCurrently slow for big folders.
- Inspector - displays hierarchies, like file system or gui scene graph. 
- Icon - fully configurable icon bar. Icons can execute any (supported) application action.

### Portability

  The application in its self-contained form:
- has executable .exe
- requires no installation
- requires no software (e.g. java) preinstalled
- runs from anywhere
- does not require internet access
- does not write to registry or create any file outside its directory (as long as java itself does not require it, e.g., cached data by WebView (in-app web browser)

### GUI

- minimalistic - shows only whats important, no endless headers and borders taking up important space. With headerless and borderless window mode 100% of the space is given to the widgets.
- powerful - infinite space, horizontally scrollable, zoomable
- layout mode - powerful mode displaying 2nd ui layer allowing user to edit and configure the layout, widgets and more, alleviating normal user interface from all this
- skinnable completely by css
- skin discovery + change + refresh without requiring application restart
- custom skins supported

Widgets:
- can provide input and output (e.g. playlist table has selected song as output)
- inputs and outputs can be bound - when output value changes, it is passed into the listening input of other widget
- inputs can be set to custom values
- the whole system is displayed visually as editable graph

Layouts:
- widget management: Gui gives the ability to divide layouts into containers for widgets. These allow resizing, positioning, swapping, adding, removing and many more widget operations
- multiple layout support/virtual layout space. Switching layouts by dragging them horizontally (left,right) opens new space for more layouts. This provides virtually infinitely large and conveniently navigable working space.

Windows:
- snap to screen edges and other windows, screen-to-screen edges also supported.
- auto-resize when put into screen edges and corners (altogether 7 different modes - all, left/right half, right half, topleft/topright/bottomleft/bottomright quadrant)
- system tray, taskbar, fullscreen, always on top
- mini mode - a docked bar snapped to the top edge of the screen
- multiple screen support
- multiple windows
- configurable notification positions (corners, center) + window/screen oriented

### Hotkeys

- global hotkey supported - shortcuts dont need application focus if so desired
- media keys supported
- customizable (any combination of keys:  "F5", "CTRL+L", etc)
- large number of actions (playback control, layout, etc)

### Usability
- navigation: No more back and up buttons. Use left and right mouse butttons to quickly and seamlessly navigate within user interface.
- icons: No ugly buttons doing the unexpected. Icons are designed to visually aid user to understand the action. Decorated with tooltips. Some are also skinnable or change to visualize application state.
- tooltips: Everywhere. And big too. Explain all kinds of functionalities, so read to your heart's content. There are also info buttons opening information popups.
- units: No more '5000 of what questions', everything that needs a unit has a unit, e.g. filesize (kB,MB,...), time duration, bitrate, etc. Multiple units are supported when possible, e.g., using 2000ms or 2s has the same effect. This is all supported in application settings or in table filter queries.  
- validation: Designed to eliminate input errors by preventing user to input incorrect data. Warning icons signal incorrect input. Really helps with writing regular exressions.
- defaults: every settings has a default value you can revert to easily

### More

- configurable playcount incrementing strategy: at specified minimal time or percent of song playback
- cover downloading on drag&drop
- animations & effects

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
