# Contributing

- Language
  - Kotlin: Latest version
  - Java: JDK 9 or 10
- IDE: due to use of Kotlin, Intellij IDEA is strongly recommended, although not required

## Preparations

- Clone the repository

Optional, but recommended:
- Set up Vlc (required for playback)  
  64-bit VLC must be installed on your system or in the `app/vlc` directory (portable version). Obtain latest [here](https://www.videolan.org/vlc/).
- Set up JDK  
  To avoid version mismatch, it is recommended to not use system default and use [OpenJDK11](https://jdk.java.net/11/). Extract to [app/java](app/java) or use arbitrary location: create a `gradle.properties` file at project root with property: `org.gradle.java.home=/path/to/jdk`. [app/java](app/java) link will be created pointing to the directory.

#### Intellij IDEA

1) Import Project -> Select project folder  
2) Import from external model -> Gradle 
3) Check "Use auto-import"  
   Disable "Create separate module per source set"
4) Run `git checkout .idea` in the Terminal to regain the codeStyles
5) If you ever want to compile the project with IDEA itself (usually not needed), 
   you need to add the command line parameters specified in [gradle/project.gradle.kts](gradle/project.gradle.kts)
   in the IDEA settings for the Kotlin Compiler, Java Compiler & JVM
6) Optionally enable external annotations and use those provided in [idea/annotations](idea/annotations)   
   This will provide project specific deprecations and null type-safety for numerous JDK APIs   
   For more information about this see [official documentation](https://www.jetbrains.com/help/idea/external-annotations.html)
   
#### Properties

- [gradle/wrapper/gradle-wrapper.properties](gradle/wrapper/gradle-wrapper.properties) shared project properties, specifies gradle version used
- [gradle.properties](gradle.properties) local project properties, specifies optional language, build (gradle) and custom application run properties, which are:
    - player.memoryMin=100m  // defines -Xms of the JVM of the application
    - player.memoryMax=3g  // defines -Xmx of the JVM of the application
    - player.buildDir=Z:/build  // build output directory
    - player.kotlinc.experimental=true  // whether fast experimental (native) or standard kotlinc is used to compile widgets, default true
    - player.jvmArgs= // custom JVM arguments
- [settings.gradle.kts](settings.gradle.kts)    // shared project properties, defines gradle build configuration, like build files, etc.
- [app/user/application.properties](app/user/application.properties) // local application properties, managed by application and editable through ui

## Running

- `./gradlew run` compiles and runs the application
- `./gradlew build` compiles the application and widgets
- `./gradlew clean` cleans temporary files

For more tasks: `./gradlew tasks`

#### Debugging

Use 'block current thread only' for breakpoints. 
Due to mouse polling (using a native library), blocking all threads (like on a breakpoint) can cause freezes.

## Widgets

Widgets are compiled and loaded by the application, their development is completely standalone and can be done while the application is running.
It also does not require an IDE or any setup whatsoever.

The widgets bundled with the application are no different, but for convenience, they are part of the project as separate modules.
This allows auto-completion, syntax highlighting as well as build failure when any widget fails to compile.
Simply put, they seem as though they are ordinary project source files.

- Creating a widget is done by creating a widget directory   
  To create widget `MyWidget`:
   - Create `app/widgets/myWidget` widget directory (should be [java package name](https://docs.oracle.com/javase/tutorial/java/package/namingpkgs.html)
   - `app/widgets/mywidget/MyWidget.kt` kotlin (.kt) or java (.java) source file
   - declare package `package myWidget` which is the name of the directory
   - there, declare a top level class `MyWidget` (must be the same as the name of the directory, capitalized [camel case](https://en.wikipedia.org/wiki/Camel_case))
   - and extend `sp.it.pl.layout.widget.controller.SimpleController`
- To delete widget delete the widget directory

Developing a widget carries several conveniences:
- Widget source files are monitored and open widget instances automatically reload when any source file is modified.  
  Simply hit `save` and watch the widget reload.
- Widget dependencies are declared by being put into the widget's directory.  
  For IDE to detect these and get auto-completion, simply `Refresh all Gradle projects`. This is done by customized build, which scans the widget directories for dependencies.

Restrictions:
- Widget directory name, widget package, main widget class must share the same name, which must be unique
- Widget can have multiple source files, but mixing Kotlin and Java for same widget is not supported

## Code style

The project contains a shared code style in .idea/codeStyles for IDEA with definitions for auto formatting
  - Kotlin: Follow [official style guide](https://kotlinlang.org/docs/reference/coding-conventions.html)
  - Java: It is encouraged to write any new code on Kotlin
      
### Logging

##### Mechanism
 - java: [slf4j](https://github.com/qos-ch/slf4j) + [logback](https://github.com/qos-ch/logback)
 - kotlin: [slf4j](https://github.com/qos-ch/slf4j) + [kotlin-logging](https://github.com/MicroUtils/kotlin-logging)

##### Obtain a logger instance
 - java:<br>
   - old school: `private static final Logger LOGGER = LoggerFactory.getLogger(This.class);`<br>
   - convenience method: `UtilKt.log(this)`, `UtilKt.log(this.getClass())`, `UtilKt.log(This.class)`
 - kotlin:<br>
   - classes: `companion object: KLogging()`<br>
   - top level functions: `private val logger = KotlinLogging.logger {}`

##### Configuration
 - configured in [app/resources/log_configuration.xml](app/resources/log_configuration.xml)
 - the logger appends WARN and ERROR to file and everything to console (this can be changed in the settings at runtime)

### Imports
 - use static imports where possible (enum types, utility methods, etc.)
 - no empty lines, just alphabetical sort
 - separate imports and static imports
 - no package (star) imports

### Assertions
 - always try to avoid implicit conditions with proper design and type-safety
 - always check method parameters for all required conditions, always document these in @param tags
 - do not use java assertions
 - use runtime exceptions (e.g. `java.lang.AssertionError`), Encouraged is the use of methods:
   - `Objects.requireNonNull()`
   - `sp.it.util.dev.fail`
   - `sp.it.util.dev.failIf`

### Comments
 - always write javadoc for public elements, be as concise as possible, but describe and define full contract
 - no `/* */` comments
 - avoid using `//` comments by using proper names and code structure and avoiding arbitrary/exceptional/edge cases 

## Skins

A skin is a css file that styles the elements of the application, similar to styling a html web site -
see [javafx css reference guide](http://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html). 
Skins can depend on each other.

The application automatically discovers the skins when it starts and monitors them for changes. 
The skins are located in separate folders in [app/skins](/working%20dir/skins), 
which also contains further instructions on how to create your own skin.

Customize the appearance of the application by creating a new skin depending on the default skin and modifying what you want. 
This way your changes won't be overridden by an update of the original skin.  
If you feel like you added substantial value, feel free to submit a pull request so it can be incorporated into the application!
