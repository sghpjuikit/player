# Player

JavaFX based audio player application.
The project focuses on modular graphical user interface (GUI) and customizability, aiming to provide put-anything-anywhere functionality. Indivindual functionalities are represented by pluggable modules called widgets, that can be used anywhere within the layout.

## Features

- ###playback

  For now only mp3 and wav files are supported. Hopefully, flac and ogg will come soon.
  
- ###media library 
  
  Application creates a library for song metadata. The database has small footprint (roughly about 10MB for 20000 files). The library can handly lots of songs and provides powerfull tables to display them. Songs can be multi-level ordered by any attribute. They can be filtered out by multiple attributes at once at each table. The tables can be linked to each other arbitrarily. Besides song table, there is also table that displays song groups, again per any song attribute (authors, genres, years, comments, rating, anything...). Custom defined values for GENRE, with auto-complete functionality when typing

- ###tag editing

  Application supports reading and writing song tag information, individually or for number of songs at once. The support goes far beyond basic fields (like AUTHOR or TITLE or COVER) and includes fields like COLOR, PLAYCOUNT, RATING. All supported tags can be stored in the database and in the tag of the song and you will never lose them again, ever. There is planned support for multiple artists.
  
  **Playcount** signifies number of times the song was played (the exact definition is left upon the user, who can set up the playcount incrementation accordingly. Playcount can also be edited manually.
  
  **Rating** values are in percent values, specifically real number from <0;1>. Standard POPM id3tag is used and therefore, the values are interoperable with other players. Most of them however do not support partial values and will only recognize the value as 3/5 or 55/100 depending on their implementation.
  
  **Chapters** are comments added at specific time of the song. They can be added during playback on the seeker and browsed as popup menus. The comments can be decently long (the upper value remains a mystery, but surpasses 500 characters (for all chapters together) and probably by a large margin - possibly up to 16MB).
  
  **Cover** is fully upported. The image in tag can be easily extracted by drag and drop as standalone picture (the other way works too).

- ### portability 

  The application does not require installation and runs from anywhere. It is self-contained, which means it does not require java to be installed on the system at all. It is executable as exe and can be used like any other application. So far only Windows is supported, but it should work under Linux and Mac shold as well (untested).
  
- ### extensibility & modularity

  Almost all functionalitiess are implemented as widgets, that can be loaded, closed, moved and configured separately. Multiple instances of the same widget can run at once in windows, layouts or even popup windows. New widgets can be added for anything. Some of the existing widgets are:
  FileInfo - shows cover and tag information about the song
  Tagger - for tagging
  Library - actually broken down to 2 widgets to further separate the functionality
  ImageViewer - for images associated with the songs, supports subfolders when discovering the images
  Explorer - simple file system browser. Currently slow for big folders.
  Settings - rich configurations, including custom shortcuts
  
  There is plan for visualisations, cover downloading and some more cools ideas like song graphs.
  
- ### gui

  The gui allows custom layouts by providing the ability to divide layouts into containers for widgets. Window can contain multiple layouts similar to virtual desktops. The layouts are easily accessible by dragging the gui horizontally. This provides virtually infinitely large and fast to navigate working space. The application supports multiple windows like this, which themselves and as higher level layout.
  
  The windows have docking (to other windows or screen edges) feature, and also auto-resize when put into cscreen edges and corners (altogether 7 different modes). There is (so far imperfect) support for system tray, taskbar, fullscreen mode and mini mode as a docked bar snapped to the edge of the screen.
  
- ### global & media hotkeys
- ### skin support

  All it takes is a single css file to completely change everything. Well... Thats the goal anyway. 

## Screenshots

![ScreenShot](/extra/screenshot1.png)

![ScreenShot](/extra/screenshot3.png)

# The Catch XXII

- *Memory consumption* throughout my average use is about 500MB, but it depends. It can go from 200MB up to 1GB (rare cases when working with lots of high-resolution images). The effort to crack down on memory consumption is there, but its not there yet...
- Some of the widgets or features are too **experimental**, confusing or outright do not work. It is a **work in progress**, so consider this early alpha version.
- nothing im aware of

# Download & Use

Download link coming soon.

Starting the application for the first time will run an automatic guide, that will guide you through the basics of the application. Nothing invasive, it can be closed forever very easily, but it does provide some context to the features of the application, so i **recomment the guide**.

Tips:
- Some widgets, popups or containers have informative buttons (marked "i") that can display available actions and further explain the functionalities of given module. 
- Most of the controls like buttons have informative tooltips explaining their functionality. Mouse over the controls to display tooltips if you would like to know what each button does.

# Contribution

In case you are interested in the development or in contribution, send mail to the address associated with this github account. I welcome you.

There are several areas that one can contribute to:
- application core - involves java & javaFX code, OOP + Functional + Reactive styles
- skins - requires very basic knowledge of css, but lots of patience
- widgets - involves java & javaFX code
- testing - simply run the application from IDE and report bugs ?
- design - logos, overal app motives and spreading the word (no, not yet...)

### Developing

The provided files are
- source files
- working directory containing application data.
- libraries

In order to successfully build and run the application the working directory should be set up in the project's settings in the IDE to: '/working dir'. All libraries in the 'extra/lib' must be imported in the project.

In order to be able to develope and use widgets (even those included in this project already) '/src widgets' directory must be included as a source location for source files in project properties.

Proper manuals and HOWTOs will be provided later.

### Skinning

A skin is a single css file that works the same way as if you are skinning a html web site. [a link](http://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html) is an official reference guide that contains a lot of useful information.
The application autodiscovers the skins when it starts. The skins are located in Skins directory, each in its own folder.

### Credits & Licence

You are free to use the application or make your own builds of the project for personal use.

The project is to adopt MIT licence in the future, but for now remains personal. I would appreciate to be
informed before taking any actions that could result in publicizing or sharing this project.

The project makes use of work of sevaral other individuals (with their permission), who will be properly credited later as well.

Al Psy Congroo
