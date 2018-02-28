# Contributing

- Language
  - Java: jdk-9
  - Kotlin: latest version
- IDE: due to use of Kotlin, Intellij IDEA is strongly recommended, although not required

## Preparations

- Clone the repository
- To enable audio playback, VLC must be installed on your system or or in the working dir/vlc directory. Obtain latest [here](https://www.videolan.org/vlc/)
- To use a jdk other than your default system one, create a `gradle.properties` file at project root with the following content: `org.gradle.java.home=/path/to/jdk`

#### Intellij IDEA

Import Project -> Select PlayerFX directory<br>
Import from external model - Gradle 
- "Use auto-import"
- Disable "Create separate module per source set"
- Don't worry if Gradle buildscripts (*.kts) are not recognised, that is a known issue and does not affect the build process

## Running

- `./gradlew run` compiles and runs the application
- `./gradlew build` only compiles
- `./gradlew clean` cleans temporary files

For more tasks: `./gradlew tasks`

#### Debugging

Use 'block current thread only' for breakpoints. 
Due to mouse polling (using a native library), blocking all threads (like on a breakpoint) can cause freezes.

## Widgets

Widgets don't need to be compiled by the IDE, the application will compile them itself. 
But for syntax highlighting and error reporting it is recommended to create a separate module for them, depending on PlayerFX and all jars in the widgets directory. 
This should automatically be setup by Gradle and imported into your IDE.

## Code style

The project contains a shared code style [code-style.xml](code-style.xml) for IDEA with definitions that allows beneficial use of auto formatting
  - Kotlin: Follow [official style giude](https://kotlinlang.org/docs/reference/coding-conventions.html)
  - Java: It is encouraged to write any new code on Kotlinin
      
#### Logging
 - Mechanism
   - java: [slf4j](https://github.com/qos-ch/slf4j) + [logback](https://github.com/qos-ch/logback)
   - kotlin: [slf4j](https://github.com/qos-ch/slf4j) + [kotlin-logging](https://github.com/MicroUtils/kotlin-logging)

 - Obtain logger instance
   - java:<br>
      old school: `private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(This.class);`<br>
      convenience method: `util.dev.UtilKt.log(this)`, `util.dev.UtilKt.log(this.getClass())`, `util.dev.UtilKt.log(This.class)`
   - kotlin:<br>
      classes: `companion object: mu.internal.KLoggerFactory.KLogging()`
      
      top level functions: `private val logger = mu.internal.KLoggerFactory.KotlinLogging.logger {}`
  
 - Configuration
    - log_configuration.xml in /working dir/log, where the log output is also located
    - the logger appends WARN, ERROR to file and everything to console (this can be changed at runtime in the settings)

#### Imports
 - use static imports where possible (enum types, utility methods, etc.)
 - no empty lines, just alphabetical sort
 - separate imports and static imports
 - never use package (star) imports

#### Assertions
 - always try to avoid implicit conditions with proper design and typesafety
 - always check method parameters for all required conditions, always document these in @param tags
 - do not use java assertions
 - use runtime exceptions (e.g. AssertionError) or methods like Objects.requireNonNull(), util.dev.throwIf, util.dev.fail and never document them in @throw (to avoid anyone catching them)

#### Comments
 - always write javadoc for public elements, be as concise as possible, but describe and define full contract
 - use simple comments (//) to provide code intention
 - avoid using comments by using proper names and code structure and avoiding arbitrary/exceptional/edge cases 

## Skins

A skin is a single css file that works similar to styling a html web site - see [javafx css reference guide](http://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html). 
Skins can depend on each other.

The application autodiscovers the skins when it starts and monitors them for changes. 
The skins are located in separate folders in [working dir/skins](/working%20dir/skins), where you will also find further instructions on how to create your own skin.

Customize the appearance of the application by creating a new skin depending on the default skin and modifying what you want. 
This way your changes won't be overridden by an update of the original skin. 
If you feel like you added substantial value, feel free to submit a pull request so it can be incorporated into the application!
