# Contributing

- Language
  - Java: JDK 9 or 10
  - Kotlin: Latest version
- IDE: due to use of Kotlin, Intellij IDEA is strongly recommended, although not required

## Preparations

- Clone the repository
- To enable audio playback, VLC must be installed on your system or in the working dir/vlc directory. Obtain latest [here](https://www.videolan.org/vlc/)
- To use a jdk other than your system default, create a `gradle.properties` file at project root with the following content: `org.gradle.java.home=/path/to/jdk`

#### Intellij IDEA

1) Import Project -> Select project folder  
2) Import from external model -> Gradle 
3) Check "Use auto-import"  
   Disable "Create separate module per source set"
4) Run `git checkout .idea` in the Terminal to regain the codeStyles
5) If you ever want to compile the project with IDEA itself, you have to open
  `File > Settings > Java Compiler > Additional command line parameters` and insert  
  `-Xlint:unchecked --add-exports javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED --add-exports javafx.controls/com.sun.javafx.scene.control.skin=ALL-UNNAMED --add-exports javafx.web/com.sun.webkit=ALL-UNNAMED`

## Running

- `./gradlew run` compiles and runs the application
- `./gradlew build` compiles the application and widgets
- `./gradlew clean` cleans temporary files

For more tasks: `./gradlew tasks`

#### Debugging

Use 'block current thread only' for breakpoints. 
Due to mouse polling (using a native library), blocking all threads (like on a breakpoint) can cause freezes.

## Widgets

Widgets don't need to be compiled by the IDE, the application will compile them itself. 
But for syntax highlighting and error reporting it is recommended to create a separate module for them, 
depending on PlayerFX and all jars in the widgets directory. 

This should be set up automatically by Gradle and imported into your IDE.

## Code style

The project contains a shared code style in .idea/codeStyles for IDEA with definitions for auto formatting
  - Kotlin: Follow [official style giude](https://kotlinlang.org/docs/reference/coding-conventions.html)
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
 - working dir/resources/log_configuration.xml
 - the logger appends WARN and ERROR to file and everything to console (this can be changed at runtime in the settings)

### Imports
 - use static imports where possible (enum types, utility methods, etc.)
 - no empty lines, just alphabetical sort
 - separate imports and static imports
 - never use package (star) imports

### Assertions
 - always try to avoid implicit conditions with proper design and typesafety
 - always check method parameters for all required conditions, always document these in @param tags
 - do not use java assertions
 - use runtime exceptions (e.g. AssertionError) or methods like Objects.requireNonNull(), 
   util.dev.throwIf, util.dev.fail and never document them in @throw (to avoid anyone catching them)

### Comments
 - always write javadoc for public elements, be as concise as possible, but describe and define full contract
 - use simple comments `//` to provide code intention
 - avoid using comments by using proper names and code structure and avoiding arbitrary/exceptional/edge cases 

## Skins

A skin is a single css file that works similar to styling a html web site - 
see [javafx css reference guide](http://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html). 
Skins can depend on each other.

The application automatically discovers the skins when it starts and monitors them for changes. 
The skins are located in separate folders in [working dir/skins](/working%20dir/skins), 
which also contains further instructions on how to create your own skin.

Customize the appearance of the application by creating a new skin depending on the default skin and modifying what you want. 
This way your changes won't be overridden by an update of the original skin. <br>
If you feel like you added substantial value, feel free to submit a pull request so it can be incorporated into the application!
