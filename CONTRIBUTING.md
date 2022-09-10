# Contributing

## Preparations

1. Clone the repository
  - `git clone https://github.com/sghpjuikit/player.git`
  - `cd player`
1. Set up Java
  JDK is downloaded and set up automatically during gradle build through java [toolchain](https://docs.gradle.org/current/userguide/toolchains.html)  
  For manual JDK setup or when automatic setup fails for some reason:
    - Download & install/extract [64-bit OpenJDK15](https://github.com/ibmruntimes/semeru17-binaries/releases)  
      To avoid problems, it is recommended to use project-local JDK:
      - Copy JDK contents to `<project-dir>/app/java`
      - Create a `gradle.properties` file at project directory
      - Add property: `org.gradle.java.home=/path/to/jdk`.
2. Set up Vlc
  The program is downloaded and set up automatically during initial application start - this is currently not supported on Linux. (Linux only)  
  For manual Vlc setup:
    - 64-bit Vlc must be installed on your system or portable version placed in the `app/vlc` directory. Obtain latest [here](https://www.videolan.org/vlc/), in case of issues use version 3.

#### IDE: Intellij IDEA

1. Import Project -> Select project folder  
2. Import from external model -> Gradle 
3. Check "Use auto-import"  
   Disable "Create separate module per source set"
4. Set `Project > Open Module Settings > Project > Project SDK` to the same JDK as set in gradle properties
5. `File > Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JVM` set to `Use Project JVM`
6. Optionally run `git checkout .idea` in the Terminal to regain the codeStyles
7. Optionally enable external annotations and use those provided in [idea/annotations](.idea/annotations)   
   This will provide project specific deprecations and null type-safety for numerous JDK APIs   
   For more information about this see [official documentation](https://www.jetbrains.com/help/idea/external-annotations.html)

## Running project

- `./gradlew build` compiles the application
- `./gradlew run` compiles and runs the application
- `./gradlew clean` cleans temporary files

For more tasks:
`./gradlew tasks`

## Running application

#### Windows
- open `SpitPlayer.exe`

#### Windows console
- `./SpitPlayerc.exe` starts the application
- `./SpitPlayerc.exe --help` shows help

#### OSX & Linux
- `./SpitPlayerc.sh` starts the application
- `./SpitPlayerc.sh --help` shows help

#### Properties

- [gradle/wrapper/gradle-wrapper.properties](gradle/wrapper/gradle-wrapper.properties)  
  Project properties, specifies gradle version used
- [gradle.properties](gradle.properties)  
  Per-developer project properties, specifies optional language, build (gradle) and custom application run properties, which are:
    - player.memoryMin=100m  // defines -Xms of the JVM of the application
    - player.memoryMax=3g  // defines -Xmx of the JVM of the application
    - player.buildDir=Z:/build  // build output directory
    - player.jvmArgs= // custom JVM arguments
- [settings.gradle.kts](settings.gradle.kts)  
  Project properties, defines gradle build configuration, like build files, etc.
- [app/user/application.properties](app/user/application.properties)  
  User application properties, managed by application and editable through its ui
- [app/SpitPlayer.l4j.ini](app/SpitPlayer.l4j.ini)  
  User JVM arguments for [SpitPlayer.exe](app/SpitPlayer.exe)
- [app/SpitPlayerc.l4j.ini](app/SpitPlayerc.l4j.ini)  
  User JVM arguments for [SpitPlayerc.exe](app/SpitPlayerc.exe)

## Dependencies

- `./gradlew dependencies --configuration compileClasspath` shows dependency tree
- 
#### Debugging

Use 'block current thread only' for breakpoints. 
Due to mouse polling (using a native library), blocking all threads (like on a breakpoint) will cause mouse freeze.

## Widgets

Widgets are compiled and loaded by the application, their development is completely standalone and can be done while the application is running.
It also does not require an IDE or any setup whatsoever.

The widgets bundled with the application are no different, but for convenience, they are part of the project as separate modules.
This allows auto-completion, syntax highlighting as well as build failure when any widget fails to compile - so they seem as though they are ordinary project source files.

Create widget `MyWidget`:
  - create `app/widgets/myWidget` widget directory (use `camelCase` naming, no whitespace)
  - create `app/widgets/mywidget/src` directory for source code
  - create `app/widgets/mywidget/src/MyWidget.kt` main widget class (kotlin (`.kt`) or java (`.java`))
    - declare package `package myWidget` (the name of the widget directory)
    - declare a top level class `MyWidget` (the name of the widget directory, capitalized)
    - have the class extend `sp.it.pl.layout.controller.SimpleController` and implement abstract members
    - declare widget metadata:
      - Java: annotate your class with `sp.it.pl.layout.Widget.Info` and specify members
      - Kotlin: declare companion object `companion object: sp.it.pl.layout.WidgetCompanion {}` and implement abstract members
  - for resources create `app/widgets/mywidget/rsc` directory
  - for tests create `app/widgets/mywidget/tst` directory
  - for jar dependencies create `app/widgets/mywidget/lib` directory

Delete widget:
- delete the widget directory
  - if the application uses some resources, such as widget-only jar, the application must be closed first

Developing a widget carries several conveniences:
- Widget source files are monitored and open widget instances automatically reload when any source file is modified.  
  Simply hit `save` and watch the widget reload.
- Widget-only dependencies are declared by being put into the widget's directory.  
  For IDE to detect these and get auto-completion, simply `Refresh all Gradle projects`. This is done by customized build, which scans the widget directories for dependencies.

Restrictions:
- Widget directory name, widget package, main widget class must share the same name, which must be unique
- Widget can have multiple source files, but mixing Kotlin and Java for same widget is unsupported

## Code style

The project contains a shared code style in .idea/codeStyles for IDEA with definitions for auto-formatting
  - Kotlin: Follow [official style guide](https://kotlinlang.org/docs/reference/coding-conventions.html)
  - Java: It is encouraged to write any new code on Kotlin
      
### Logging

##### Mechanism
[slf4j](https://github.com/qos-ch/slf4j) + [kotlin-logging](https://github.com/MicroUtils/kotlin-logging)

##### Obtain a logger instance
 - classes: `companion object: KLogging()`<br>
 - top level functions: `private val logger = KotlinLogging.logger {}`

##### Configuration
 - configured in [app/resources/log_configuration.xml](app/resources/log_configuration.xml)
 - the logger appends WARN and ERROR to file and everything to console (this can be changed in the settings at runtime)

### Imports
 - use static imports where possible (enum types, utility methods, etc.)
 - no empty lines, just alphabetical sort 
 - no package (star) imports

### Assertions
 - always try to avoid implicit conditions with proper design and type-safety
 - always check method parameters for all required conditions, always document these in @param tags
 - do not use java assertions
 - use runtime exceptions (e.g. `java.lang.AssertionError`), Encouraged is the use of methods:
   - `sp.it.util.dev.fail`
   - `sp.it.util.dev.failIf`

### Comments
 - always write javadoc for public elements, be as concise as possible, but describe and define full contract
 - never use `/* */` comments
 - avoid using `//` comments by using proper names and code structure and avoiding arbitrary/exceptional/edge cases 

## Skins

A skin is a css file that styles the elements of the application, similar to styling a html website -
see [javafx css reference guide](http://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html). 
Skins can depend on each other.

The application automatically discovers the skins when it starts and monitors them for changes. 
The skins are located in separate folders in [app/skins](/app/skins), 
which also contains further instructions on how to create your own skin.

Customize the appearance of the application by creating a new skin depending on the default skin and modifying what you want. 
This way your changes won't be overridden by an update of the original skin.  
If you feel like you added substantial value, feel free to submit a pull request, so it can be incorporated into the application!
