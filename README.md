
# PlayerFX

## Overview

- [What is this?](#what-is-this)
- [Am I the target group?](#am-i-the-target-group)
- [Features](#features)
- [Screenshots](#screenshots)
- [Issues](#issues)
- [Download & Use](#download--use)
- [Development](#development)
- [Credits & Licence](#credits-%26-license)

## What is this?

  The <b>Player</b> or <b>PlayerFX</b> is a desktop audio player and audio management application, with a dynamic module system - a multipurpose extensible application capable of compiling and running custom java widgets.

![Playlist View](https://raw.githubusercontent.com/sghpjuikit/player/master/extra/screenshot_playlists.jpg)

## Am I the target group?

There are two reasons to be interested in this project:

- as an <b>audio management system</b>. You may ask, why another audio player? Because there is significant lack of such application for a power user. It's not just about hitting a play button. Ever wanted to use timed comment? Ever lost your entire library song ratings due to corrupted database file? Ever needed multiple playlists or display/manipulate your songs in a way that just wasnt possible for some reason? This audioplayer addresses a lot of such issues.

- as a <b>multiapplication</b> - collection of unrelated miniapplications. Image browser, movie explorer, file renamer and more. And if you know java, you can make your own app with simple text editor and literaly dozen lines of code - without java, without IDE and without hassles of creating and deploying your application - just write your code, hit save and watch as it autocompiles and runs as widget, which you can run as a standalone application! All that with included support for configurations, skins and everything else.

## Features

### Guiding Principles

- <b>Customizability</b> - User uses the application how he wants, not how it was designed to be used. Therefore emphasis on customization, skins, settings, etc.
- <b>Portability</b> - No installation (or need for java or other programs), run from anywhere, little/no trace, everything is packaged along (no hunting your library database file in hidden Windows directories... 
- <b>Modular functionality</b> - User can launch or use only selected components he is interested in and ignore everything else as if it was never there.
- <b>Modular user interface</b> - User has the ability to 'make his own gui'. Completely custom component layout. He can create all-in-one GUI or use separate windows or - anything really.
- <b>Fancy features</b> like: rating in tag, time comments, good image support, advanced & intuitive library management, etc.
- <b>Library independence</b> - Moving & renaming files will not result in loss of any information. Every single bit is in the tag. Always. If you move on to different application or lose your library - you never lose data. Ever again.
- <b>Usability</b> - Ease of use and efficient workflow due to minimalistic and unobtursive graphical user interface design. Think shortcuts, swiping, icons instead buttons, closing with right click instead of trying to hit small button somewhere in the corner of whatever you are doing..., etc.
- <b>Responsive</b> - fast and responsive. Minimal modal dialogs. No more stuck windows while your library is scanning that big fat audio collection of yours
- <b>Sexy</b> - Your way of sexy if you know tiny bit about css.

### Play audio 

- mp3, mp4, m4a, wav, ogg, flac, possibly more to come
- file, http (over internet, support is limited to what javaFX can currently do - no flac & ogg).

### Manage audio files

Song database:
- small footprint: in 10s of MBs for 10000s audio files
- big: 40000 files no problem
- fast: library is loaded into main memory (RAM).
- no dependency: song files always store all data in their tag, moving or renaming files poses no problem.
- no inconsistencies: displayed song metadata can only be out of sync with real data if tag is edited by external application (or as a result of a bug)
- no data loss guarantee: losing database has no effect at all, it can be completely rebuilt anytime. The library serves as a persistable cache, not as data storage.

Management system is only as good as its user interface. There are powerful tables that try to be as flexible as possible.
Tables:
- big: 30000 songs in playlist no problem (although good luck loading it at the app start...)
- smart columns: set visibility, width, sorting (multiple column), order of any column for any song attribute
- visual searching: by any (textual) attribute (artist, composer, title, etc) simply by writing. Scrolls 1st match (to center) and highlights matches - so they 'pop' visually - shich doesnt strain your eyes that much. Its fast (no CTRL+F, just type...) and convenient.
- powerful filtering - CTRL+F. Shows only matches. Filtering here is basically constructing logical predicates (e.g.: 'year' 'less than' '2004') and it is possible to use <b>any</b> combination of attributes (columns), 'comparators' and permissible values. Filters can be inverted (negation) or chained (conjunction).
- group by - e.g. table of groups of songs per attribute (e.g. year or artist) Searching, filtering and sorting fully supported of course.
- multiple column sorting by any attribute (artist, year, rating, bitrate, etc)
- cascading - link tables to other tables as filters and display only selected items (e.g. show songs of autor A's  albums X,D,E in year Y in three linked tables reacting on table selection). Basically library widgets allow linking selection of the table as an input, while simultaneously providing its selection as an output to other tables. Use however library widgets (each having 1 table) you wish and link them up in any way you want.

### Audio tag editing

PlayerFX supports reading & writing of song tags

- individually
- by group
  - using Tagger to write the same data to multiple tags (songs may share an artist)
  - using Converter to write multiple data to multiple tags (e.g. using titles from a tracklist)

Supported are:
- all file types (see at the top), including wma and mp4 (which normally can not have a tag)
- all fields (comprehensive list later), including rating, playcount, color, timed comments.

The aim is to be interoperable with other players, where possible. Noteworthy or nonstandard supported tags include:
- <b>Rating</b>
  - values are in percent values independent of implementation (mp3=0-255, flac/ogg=0-100)
  - floating values (0-1). Values like {1,2,3,4,5} are obsolete, illogical and nobody agrees on what they mean. Use full granularity (limited only by tag (1/255 for mp3, 1/100 for other formats)) and pick graphical representation (progress bar or any number of "stars" you want). Basically rate 1) how you want 2) all audio types the same 3) be happy the value is in the tag 4) visualie the rating value as you want - be it 3 stars or 10 or a progress bar.
  - interoperable with other players (POPM frame), but most of them will only recognize the value in their own way
- <b>Playcount</b>
  - number of times the song has been played (the exact definition is left upon the user, who can set up the playcount incrementation behavior arbitrarily, or edit the value manually (increment/decrement/set arbitrary number - its your collection, excert your power!).
  - the data are written in custom tag (in mp3, writen duplicitly in POPM frame counter)
- <b>Time comments/chapters</b>
  - comments associated with specific time/part of the song. They can be added during playback on the seeker and viewed in popup menus. The length of the comment should be a non-issue (the upper value is unknown, but should be enough).
  - The gui makes it really easy to add or edit these and takes no space, since it is using seeker bar and popup windows.
  - the data are written in custom tag
- <b>Color</b>
  - just in case you want to associate songs with a colors, you can.
  - using custom tag
- <b>Cover</b>
  - image in tag can be imported/exported (but I advise against placing images in audio tags, it eats space and is semantically incorrect (cover is album metadata, not song metadata)).
  - cover read from file location is supported too, looking for image files named:
    - song title.filetype
    - song album.filetype
    - cover.filetype or folder.filetype
  
### Configurability

  All settings and entire user interface layout serialize into a human readable and editable files. These can be edited, backed up or switched between applications.

### Modularity

  Most of the functionalitiess are implemented as widgets, that can be loaded, closed, moved and configured separately. Multiple instances of the same widget can run at once in windows, layouts or popups. Widgets' source files can be created and edited in runtime and any changes will be immediatelly reflected in the application. This means that if you are a developer you just edit code of the .java file, hit save and watch as the widgets are (recompiled and then) reloaded with previous state and configuration. 
  
Comprehensive widget list:
- <b>Playback</b>
  - controls for playback, like seeking. Supports chapters.
- <b>Playback Mini</b>
  - Minimalistic dock version of Playback widget.
- <b>Playlist</b>
  - Table or playing songs. Of course it is possible to use more of themat once. Very handy to have a side-playlist sometimes.
- <b>FileInfo</b>
  - Shows cover and song metadata. Supports cover download (url drag & drop) and rating.
- <b>Tagger</b>
  - Tag editor
- <b>Converter</b>
  - Object-object converting facility.
  - Displays objects as text while allowing user to apply function transformations. Handy file renamer and per-song group tagger. Supports object lists, text transformations, manual text editing, regex, writing to file etc.
  - Provides set of functions that transform java objects (such as String or File). User can then set some kind of input (formally List<INPUT>) and apply and chain these transformation functions (on every element of the list) to get some result (List<OUTPUT>. It may sound confusing at first, but its a intuitive and very powerful thing, particularly when combined with the ability to see the transformation output as a text at every step and the ability to manually edit this text and reuse it as an input for further transformation. The final output can be used as text or for an action, such as
  - file renaming
  - tagging
This makes it possible to import song titles from copypasted tracklist found on web by 'cleaning' it up with text line transformations (remove x characters from start, etc.) rather than manually. Changing extension or names of bunch of files is peace of cake.
- <b>Library</b> + <b>LibraryView</b> - song tables. User can link them up so they display increasingly filtered/sepecialized content. For example, 1st table can display all artists (all unique artists in entire song library), 2nd table linked to 1st would display all unique albums of songs by any artist/s selected in the 1st table. So on until Nth table displays the songs. Combinations are endless. In addition, because of the modular gui, you can set up table size and position as you wish and the layout is not restricted to area of the window (layout has own virtual space, like virtual desktops). Lastly, in is possible to set up widgets (in this case individual tables) to be 'passive' until user allows them to load properly - it is possible to create a multiple views with lots of tables with practically no performance impact at all (by using views only when needed and have them 'sleep', but be prepared and configured all the time).
- <b>Image</b>
  - Image viewr - displays an image.
- <b>ImageViewer</b>
  - Image browser displays animage and thumbnails for images in a directory. It can also be set up to display directory of the currently playing song (or any song for that matter). useful to show more than a song cover, if you have more alum images.
  - has slideshow
  - has 'theater' mode intended to use as fullscreen album cover slideshow with a 'now playing' info overlay
- <b>Settings</b>
  - configuring facility displaying settings for either:
    - application
    - widget
    - javaFX scenegraph
    - other java object implementing Configurable. There are methods to turnplain java objects into Configurable. For example using annotations on fields or using javaFX proeprties.
- <b>Explorer</b>
  - simple file system browser. Nothing big. Currently slow for big folders (1000+ of files).
- <b>Inspector</b>
  - displays hierarchies:
    - application modules
    - file system
    - scene graph (this is taking the idea from the ScenicView app and works basically the same). When used with the combination of Settings widget its a great way to debug the application scenegraph (yeah from within the application)
- <b>Logger</b>
  - displays System.out in a TextArea. For developers
- <b>IconBox</b>
  - configurable icon bar/dock. User can add/remove custom icons executing some predefined.
- <b>FunctionViewer</b>
  - plots custom mathematical function in a graph
- <b>Spectrum</b>
  - Displays the audio spectrum of the playing song (doesnt work for flac and ogg). Looks nothing like the low-fps sample from Oracle. Uses Canvas and smooth 60FPS animation. Looks very cool. Yet its only 58 lines!! Not really useful aside being a fancy visualisation though.
- <b>WebReader</b>
  - In-app web browser. Experimental &not very useful. Image drag & drop has terrible performance for some reason.
- <b>Layouts</b>
  - Layout manager. Saved layout browser. Currently outdated and argely useless. Planned for complete rewrite.
- <b>Comet</b>
  - Loose java port of the (popular?) game Comet from 90s. Something between Crimsonland and Geometry Wars. Basically you fly a rocket and shoot UFOs and asteroids to clear missions with increasing difficulty. For up to 8 players (no internet support, so prepare lots of keyboards and a living room). Pretty advanced effects though, including Black holes, force fields and wapring frid (inspired by Grid Wars/Geometry Wars). Anyone running this app better try the game out. Line count sits at about 5000). There was no particular reason to do this other than bring my childhood back and master javaFX Canvas.

### Portability

The application in its self-contained form:
- has executable .exe
- requires no installation
- requires no software (e.g. java) preinstalled
- runs from anywhere
- does not require internet access
- does not write to registry or create files outside its directory (except for some cache & temporary files)

### GUI

  UI is minimalistic but powerful and fully modular. Modules (widgets) are part of layout hierarchy, which can be manipulated, saved or even loaded as standalone application. 

- minimalistic - shows only whats important, no endless headers and borders taking up important space. With headerless and borderless window mode 100% of the space is given to the widgets.
- powerful - infinite virtual space, horizontally scrollable, zoomable
- layout mode - temporary 2nd ui layer allowing user to edit and configure the layout, widgets and more, alleviating normal user interface from all this
- completely skinnable (css)(skin discovery + change + refresh does not require application restart)

Widgets:
- can provide input and output (e.g. playlist table has selected song as output)
- inputs and outputs can be bound - when output value changes, it is passed into the listening input of other widget
- inputs can be set to custom values
- the whole system is displayed visually as editable graph
- can load lazily - be passive until user explicitly requests loading with mouse click.

Layouts:
- widget management: Gui gives the ability to divide layouts into containers for widgets. These allow resizing, positioning, swapping, adding, removing and many more widget operations.
- Individual containers form nested hierarchy that can be configured at any level easily through user interface, navigating with left (down) + right (up) mouse click in layout mode. 
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
- defaults: Every settings has a default value you can revert to easily.
- shortcuts: Quick & easy control over the application anytime.
- smart ui: Notifications that can be closed when they get in the way or keep being open when mouse hovers over. Throw notifications manually or put whole widgets in it. Tables or docked window that monitor user activity. Clickless ui reacting on mouse hover rather than click

### More

- Configurable playcount incrementing strategy: at specified minimal time or percent of song playback.
- Web search with a query, e.g., search album on Google (opens default browser).
- Cover downloading on drag&drop.
- Cool animations & effects.
- Smart drag&drop system (born from neessity). Drag&drop has never been more easy to use.
  - area that is would accept drag&drop (if mouse button were to be released) is visually highlighted anddisplays icon and description of the action that is would execute
  - individual components supporting drag&drop can cover each other
- crisp images in any size, be it 100x100px thumbnail or mammoth image displayed on your new big display. Images load quickly and dont cause any lag (not even the big ones), images around 5000px are handled just fine (just dont look at memory).

Platforms:
- Windows
- Linux (untested, no support for global shortcuts)
- Mac (untested)

## Screenshots

<details>
  <summary>Show</summary>
  
![ScreenShot](/extra/screenshot_playlists.jpg)
Depicts:
- multiple playlists
- drag&drop on 2nd playlist
- time comment displayed in the popup


![ScreenShot](/extra/screenshot_actions.jpg)

Depicts:
- application support action menu
- 'glass' effect


![ScreenShot](/extra/screenshot_layoutmode.jpg)

Depicts:
- layout mode
- widget input-output links


![ScreenShot](/extra/screenshot_widgets.jpg)

Depicts:
- function plotting, audio spectrum, logger widgets
- interactive guide
- custom skin


![ScreenShot](/extra/screenshot_comet1.jpg)

![ScreenShot](/extra/screenshot_comet2.jpg)

Depicts:
- Comet game widget


![ScreenShot](/extra/screenshot_old1.png)

Depicts an old build of the application with an old skin.


![ScreenShot](/extra/screenshot_old2.png)

Depicts an old build of the application with an old skin.

</details>

## Issues

- Some of the widgets or features are **experimental**, buggy or confusing (its being worked on, so stay tuned).
- Linux and Mac not tested for now.
- Memory consumption is worse than what native applications could do. Normally i have get about 250-450MB, but it depends on use case. Lots of widgets will eat more memory. Handling large pictures (4000^2 px) on large monitors can also rapidly increase memory consumption (but picture quality stays great). 32bit is more effective (64bit effectively doubles memory consumption), so ill only provide 32-bit version.
- No playlist  support (.m3u, etc) for now. Maybe later.
- Using shadows on text or icons (in custom skins) can severely impact performance, but i think that goes true for any app.
- no transparent bgr for now (due to java bug causing massive performance degradation)
- visually big tables with lots of text can impact performance (we are talking full-hd and beyond)

## Download & Use

Download link coming soon.

- download zip
- extract anywhere
- run the executable file

  Starting the application for the first time will run a guide. Before you close it, read at least first couple of tips (like where to find the guide if you need it again...).

Tips:
- use tooltips! Cant stress enough.
- If you get 'trapped' and 'locked in' with no idea what to do, press right ALT (layout edit mode) or click anywhere (mouse buttons often navigate) - once you get the hang of it, you will see how convenient it is.
- widgets, popups and containers have informative "i" buttons that provide valuable info on possible course of action

# Development

### Project setup

- Language
  - Java jdk-9.0.0 or higher
  - and Kotlin (latest version) are both required.
- IDE: due to use of Kotlin, Intellij Idea is strongly recommended, although not required
- dependencies
  - Java/Kotlin: all jars are included in /working dir/lib
  - VLC: installation must be placed in working dir/vlc directory. Obtain latest [here](https://www.videolan.org/vlc/). Without this step, audio playback will not be possible.
  - Kotlin compiler: kotlinc must be placed in working dir/kotlinc directory. Obtain latest [here](https://github.com/JetBrains/kotlin/releases). Without this step, compiling widgets written in Kotlin will not be possible.

#### Widgets

It is required to add widget source codes to the source code of the project. 

In Netbeans: project > properties > sources > add folder. Add /src widgets directory. You should see 2 source directories in you project: 'src' and 'src widgets'.

In Intellij Idea: create a separate module depending (type=PROVIDED) on the main module and all its dependensies plus all the jars (recursively) in the /widgets directory.
    
### Running

  - main class: main.App
  - annotation processing: must be enabled, set to obtain processor from classpath (classindex.jar)
  - javac arg:<br>
    -Xlint:unchecked<br/>
    --add-exports javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED<br/>
    --add-exports javafx.graphics/com.sun.javafx.scene.traversal=ALL-UNNAMED<br/>
    --add-exports javafx.web/com.sun.webkit=ALL-UNNAMED<br/>
    --add-exports javafx.graphics/com.sun.glass.ui=ALL-UNNAMED<br/>
  - jvm args:<br>
    -Xmx3g<br>
    --add-opens java.base/java.util=ALL-UNNAMED<br/>
    --add-opens java.base/java.lang.reflect=ALL-UNNAMED<br/>
    --add-opens java.base/java.text=ALL-UNNAMED<br/>
    --add-opens java.base/java.util.stream=ALL-UNNAMED<br/>
    --add-opens java.base/java.lang=ALL-UNNAMED<br/>
    --add-opens java.desktop/java.awt.font=ALL-UNNAMED<br/>
    --add-opens javafx.controls/javafx.scene.control=ALL-UNNAMED<br/>
    --add-opens javafx.controls/javafx.scene.control.skin=ALL-UNNAMED<br/>
    --add-opens javafx.graphics/com.sun.glass.ui=ALL-UNNAMED<br/>
    --add-opens javafx.graphics/com.sun.javafx.scene.traversal=ALL-UNNAMED<br/>
    --add-opens javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED<br/>
    --add-opens javafx.graphics/javafx.scene.image=ALL-UNNAMED<br/>
    --add-opens javafx.web/com.sun.webkit=ALL-UNNAMED<br/>
    
### Debugging

- use 'block current thread only' for breakpoints. Due to a mouse polling (using a native library), blocking all threads (like on a breakpoint) can cause major slow downs and unresponsive mouse (up to dozens of seconds) in the system.

### Coding style

- Overall
  - Kotlin: follow [official style giude](https://kotlinlang.org/docs/reference/coding-conventions.html)
  - Java: I encourage using Kotlin instead
      
- Logging

  - Mechanism
    - java: [sl4j](https://github.com/qos-ch/slf4j) + [logback](https://github.com/qos-ch/logback)
    - kotlin: [sl4j](https://github.com/qos-ch/slf4j) + [kotlin-logging](https://github.com/MicroUtils/kotlin-logging)

  - Obtain logger instance
    - java:<br>
      old school: `private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(This.class);`<br>
      convenience method: `util.dev.UtilKt.log(this)`, `util.dev.UtilKt.log(this.getClass())`, `util.dev.UtilKt.log(This.class)`
    - kotlin:<br>
      classes: companion object: `mu.internal.KLoggerFactory.KLogging()`<br>
      top level functions: `private val logger = mu.internal.KLoggerFactory.KotlinLogging.logger {}`
  
  - Configuration
      - log_configuration.xml in /working dir/log, where the log output is also located
      - the logger appends WARN, ERROR to file and all to console ( this can be changed in runtime by user in application settings)

- Imports
  - use static imports where possible (enum types, utility methods, etc.)
  - no empty lines, just alphabetical sort
  - separate imports and static imports
  - never use package imports

- Assertions
  - always try to avoid implicit conditions with proper design and typesafety
  - always check method parameters for all required conditions, always document these in @param tags
  - do not use java assertions
  - use runtime exceptions (e.g. AssertionError) or methods like Objects.requireNonNull(), util.dev.throwIf, kotlin.test.fail and never document them in @throw (to avoid anyone catching them)

- Comments
  - always write javadoc for public elements, be as concise as possible, but describe and define full contract
  - use simple comments (//) to provide code intention
  - avoid using comments by using proper names, proper code structure and avoiding arbitrary/exceptional/edge cases 

### Skinning

A skin is a single css file that works the same way as if you are skinning a html web site. See [javafx css reference guide](http://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html). Skins can import each other.
The application autodiscovers the skins when it starts and monitors them for changes. The skins are located in working dir/skins, in separate folders.

# Credits & Licence

You are free to use the application or make your own builds of the project for personal use.

The project is to adopt MIT licence in the future, but for now remains personal. I would appreciate to be
informed before taking any actions that could result in publicizing or sharing this project.
