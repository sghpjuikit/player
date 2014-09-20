player
======

info:

JavaFX based audio player application.
Application's main focus is on modularity of the graphical user interface (GUI) and its customizability, aiming to provide put-anything-anywhere functionality. Indivindual functionalities are represented by widgets that can be used anywhere within the layout. Widgets are developed as plugable modules.

The application is portable.

Below are screenshots of how the application looks like with different skins:

![ScreenShot](/extra/screenshot1.png)
![ScreenShot](/extra/screenshot3.png)


use:

To run the application download the [zip with the executable](/extra/executable.zip), extract and run the Player.jar file. 
You will only be able to run the file with latest java installed on your system - Java Runtime Enviroment (JRE) 8u40 [a link](https://jdk8.java.net/download.html).

Starting the application for the first time will run an automatic guide, that will guide you through the basics of the application.
Tip: Most of the controls like buttons have informative tooltips explaining their functionality. Mouse over the controls to display tooltips.
Tip: Some widgets, popups or containers have informative buttons (marked "i") that can display available actions and further explain the functionalities of given module. 


contribution:
In case you are interested in the development or in contribution, send mail associated with this github account.
There are several areas that one can contribute to
- application core - involves java & javaFX code
- skins - requires very basic knowledge of css
- widgets - involves java & javaFX code
- testing - simply run the application from IDE and report bugs


developing:

The provided files are
- source files
- working directory containing application data.
- libraries

In order to successfully build and run the application the working directory should be set up in the project's settings in the IDE to: '/working dir'. The included libraries must be included and linked in the project.

In order to be able to develop and use widgets (even those included in this project already) '/src widgets' directory must be included as a source location for source files in project properties.

Proper manuals and HOWTOs will be provided later.


skinning:

A skin is a single css file that works the same way as if you are skinning a html web site. [a link](http://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html) is an official reference guide that contains a lot of useful information.
The application autodiscovers the skins when it starts. The skins are located in Skins directory, each in its own folder.


dependencies:
All required libraries to successfully build the project is indluded in /extra/lib
The list of dependencies such as libraries and other useful information in this regard will be provided later
(hopefully soon).

The project makes use of work of sevaral other individuals (with their permission), who will be properly credited later as well.


external code & licences:
You are free to use the application or make your own builds of the project.

The project is to adopt MIT licence in the future, but for now remains personal. I would appreciate to be
informed before taking any actions that could result in publicizing or sharing this project.

