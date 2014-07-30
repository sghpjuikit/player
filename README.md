player
======

info:

JavaFX based audio player application.
Application's main focus is on modularity of the graphical user interface (GUI) and its customizability, aiming to provide put-anything-anywhere functionality. Indivindual functionalities are represented by widgets that can be used anywhere within the layout. Widgets can be developed as plugable modules.

The application is portable.

Below are screenshots of current looks with different (experimental) skins:

![ScreenShot](/extra/screenshot1.png)
![ScreenShot](/extra/screenshot3.png)


use:

To run the application download the [zip with the executable](/extra/executable.zip), extract to arbitrary location and run the Player.jar file. You will only be able to run the file with latest java installed on your system - Java Runtime Enviroment (JRE) 8u20 [a link](https://jdk8.java.net/download.html).

To customize layout, click on any empty area and chose the container. Placing mouse in the top right corner of the container will display container options. Holding ALT key will display options for all containers and show additional controls for layouting.
Tip: Most of the controls like buttons have informative tooltips explaining their functionality. Mouse over the controls to display tooltips.
Tip: Some widgets, popups or containers have informative buttons (marked "i") that can display available actions and further explain the functionalities of given module. 

Proper manuals and HOWTOs will be provided later.



repository: 

At the moment, this repository is for tight circle of people interested in the project's development. It is not
for sharing so far incomplete code of the application. Please bear this in mind and dont openly distirbute, provide or publicize this project without prior agreement until the project reaches viable state.



project:

Project is currently under development and being readied for use and collaborative development.
Know that:
- its still in early development stages
- some functionalities are incomplete, bugged, dysfunctional, undocumented or simply not intuitive.
- some functionalities are experimental or for educational or testing purposes


contribution:
In case you are interested in the development or in contribution, send mail associated with this github account.
There are several areas that can one can contribute to
- application core - involves java & javaFX code
- skins - requires knowledge of css
- widgets - involves java & javaFX code
- testing - simply run he application from IDE and report bugs


developing:

The provided files are
- source files
- working directory containing application data.
- libraries

In order to successfully build and run the application the working directory should be set up in the project's settings in the IDE to: '/working dir'. The included libraries must be included and linked in the project.

In order to be able to develop and use widgets (even thouse included in this project already) '/src widgets' directory must be included as a source location for source files in project properties.

Proper manuals and HOWTOs will be provided later.


skinning:

A skin is a single css file that works the same way as if you are skinning a html web site. [a link](http://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html) is an official reference guide that contains a lot of useful information.
The application autodiscovers the skins when it starts. The skins are located in Skins directory, each in its own folder. For now there are two complete skins Default1 and Default3.


dependencies:
All required libraries to successfully build the project is indluded in /extra/lib
The list of dependencies such as libraries and other useful information in this regard will be provided later
(hopefully soon).

The project makes use of work of sevaral other individuals (with their permission), who will be properly credited later as well.


external code & licences:
You are free to use the application or make your own builds of the project.

The project is to adopt MIT licence in the future, but for now remains personal. I would appreciate to be
informed before taking any actions that could result in publicizing or sharing this project.

