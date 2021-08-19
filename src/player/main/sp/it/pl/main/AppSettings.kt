@file:Suppress("RemoveRedundantBackticks", "unused", "ClassName", "ObjectPropertyName", "SpellCheckingInspection")

package sp.it.pl.main

import sp.it.util.conf.ConfigDefinition
import sp.it.util.conf.EditMode

/** Application settings hierarchy. */
object AppSettings {

   object `app` {
      /** Name of the group. */
      const val name = "App"

      object `logging` {
         /** Name of the group. */
         const val name = "Logging"

         /** Logging level for logging to standard output. */
         object `level(stdout)`: ConfigDefinition {
            /** Compile-time constant equivalent to [name]. */
            const val cname: String = """Level (stdout)"""
            /** Compile-time constant equivalent to [info]. */
            const val cinfo: String = """Logging level for logging to standard output"""
            /** Compile-time constant equivalent to [group]. */
            const val cgroup: String = """App.Logging"""
            /** Name of the config. */
            override val name = cname
            /** Group of the config. */
            override val group = cgroup
            /** Description of the config. */
            override val info = cinfo
            /** Editability of the config. */
            override val editable = EditMode.USER
         }
         /** Logging level for logging to file. */
         object `level(file)`: ConfigDefinition {
            /** Compile-time constant equivalent to [name]. */
            const val cname: String = """Level (file)"""
            /** Compile-time constant equivalent to [info]. */
            const val cinfo: String = """Logging level for logging to file"""
            /** Compile-time constant equivalent to [group]. */
            const val cgroup: String = """App.Logging"""
            /** Name of the config. */
            override val name = cname
            /** Group of the config. */
            override val group = cgroup
            /** Description of the config. */
            override val info = cinfo
            /** Editability of the config. */
            override val editable = EditMode.USER
         }
      }
      object `settings` {
         /** Name of the group. */
         const val name = "Settings"

         /** Saves settings to the default application properties file. */
         object `saveSettings`: ConfigDefinition {
            /** Compile-time constant equivalent to [name]. */
            const val cname: String = """Save settings"""
            /** Compile-time constant equivalent to [info]. */
            const val cinfo: String = """Saves settings to the default application properties file"""
            /** Compile-time constant equivalent to [group]. */
            const val cgroup: String = """App.Settings"""
            /** Name of the config. */
            override val name = cname
            /** Group of the config. */
            override val group = cgroup
            /** Description of the config. */
            override val info = cinfo
            /** Editability of the config. */
            override val editable = EditMode.USER
         }
         /** Saves/exports settings to a file. */
         object `saveSettingsToFile`: ConfigDefinition {
            /** Compile-time constant equivalent to [name]. */
            const val cname: String = """Save settings to file..."""
            /** Compile-time constant equivalent to [info]. */
            const val cinfo: String = """Saves/exports settings to a file"""
            /** Compile-time constant equivalent to [group]. */
            const val cgroup: String = """App.Settings"""
            /** Name of the config. */
            override val name = cname
            /** Group of the config. */
            override val group = cgroup
            /** Description of the config. */
            override val info = cinfo
            /** Editability of the config. */
            override val editable = EditMode.USER
         }
         /** Loads settings to default values. Discards all non-default settings.. */
         object `loadDefaultSettings`: ConfigDefinition {
            /** Compile-time constant equivalent to [name]. */
            const val cname: String = """Load default settings"""
            /** Compile-time constant equivalent to [info]. */
            const val cinfo: String = """Loads settings to default values. Discards all non-default settings."""
            /** Compile-time constant equivalent to [group]. */
            const val cgroup: String = """App.Settings"""
            /** Name of the config. */
            override val name = cname
            /** Group of the config. */
            override val group = cgroup
            /** Description of the config. */
            override val info = cinfo
            /** Editability of the config. */
            override val editable = EditMode.USER
         }
         /** Loads settings from default application properties file. Discards any unsaved settings.. */
         object `loadSettings`: ConfigDefinition {
            /** Compile-time constant equivalent to [name]. */
            const val cname: String = """Load settings"""
            /** Compile-time constant equivalent to [info]. */
            const val cinfo: String = """Loads settings from default application properties file. Discards any unsaved settings."""
            /** Compile-time constant equivalent to [group]. */
            const val cgroup: String = """App.Settings"""
            /** Name of the config. */
            override val name = cname
            /** Group of the config. */
            override val group = cgroup
            /** Description of the config. */
            override val info = cinfo
            /** Editability of the config. */
            override val editable = EditMode.USER
         }
         /** Loads/imports settings from default application properties file. */
         object `loadSettingsFromFile`: ConfigDefinition {
            /** Compile-time constant equivalent to [name]. */
            const val cname: String = """Load settings from file..."""
            /** Compile-time constant equivalent to [info]. */
            const val cinfo: String = """Loads/imports settings from default application properties file"""
            /** Compile-time constant equivalent to [group]. */
            const val cgroup: String = """App.Settings"""
            /** Name of the config. */
            override val name = cname
            /** Group of the config. */
            override val group = cgroup
            /** Description of the config. */
            override val info = cinfo
            /** Editability of the config. */
            override val editable = EditMode.USER
         }
      }
      /** Rank of this application instance.
User may wish to run certain components as separate processes. In order to make these lightweight and safe, certain features might be disabled or delegated to the primary application instance called `MASTER`.
Other instances are called `SLAVE` instances. They can be thought of as single-purpose short-lived one-off programs.
The rank is determined at instance start up. If no other instances (of any rank) are running, the instance becomes `MASTER`, otherwise it becomes `SLAVE`. The rank can not be specified at startup or changed later.
Closing `MASTER` instance will not close `SLAVE` instances nor turn them into `MASTER` instance.. */
      object `rank`: ConfigDefinition {
         /** Compile-time constant equivalent to [name]. */
         const val cname: String = """Rank"""
         /** Compile-time constant equivalent to [info]. */
         const val cinfo: String = """Rank of this application instance.
User may wish to run certain components as separate processes. In order to make these lightweight and safe, certain features might be disabled or delegated to the primary application instance called `MASTER`.
Other instances are called `SLAVE` instances. They can be thought of as single-purpose short-lived one-off programs.
The rank is determined at instance start up. If no other instances (of any rank) are running, the instance becomes `MASTER`, otherwise it becomes `SLAVE`. The rank can not be specified at startup or changed later.
Closing `MASTER` instance will not close `SLAVE` instances nor turn them into `MASTER` instance."""
         /** Compile-time constant equivalent to [group]. */
         const val cgroup: String = """App"""
         /** Name of the config. */
         override val name = cname
         /** Group of the config. */
         override val group = cgroup
         /** Description of the config. */
         override val info = cinfo
         /** Editability of the config. */
         override val editable = EditMode.NONE
      }
      /** Enables certain features. Can be forced to `true` by starting the application with a `--dev` flag.
Features:
  * Widgets will not recompile when application jars are modified (Prevents recompilation on every application build)
  * Enables menu items that call object's methods using reflection
  * Shows experimental widgets
  * Shows class information about objects in object details. */
      object `developerMode`: ConfigDefinition {
         /** Compile-time constant equivalent to [name]. */
         const val cname: String = """Developer mode"""
         /** Compile-time constant equivalent to [info]. */
         const val cinfo: String = """Enables certain features. Can be forced to `true` by starting the application with a `--dev` flag.
Features:
  * Widgets will not recompile when application jars are modified (Prevents recompilation on every application build)
  * Enables menu items that call object's methods using reflection
  * Shows experimental widgets
  * Shows class information about objects in object details"""
         /** Compile-time constant equivalent to [group]. */
         const val cgroup: String = """App"""
         /** Name of the config. */
         override val name = cname
         /** Group of the config. */
         override val group = cgroup
         /** Description of the config. */
         override val info = cinfo
         /** Editability of the config. */
         override val editable = EditMode.USER
      }
      /** Closes this application. */
      object `close`: ConfigDefinition {
         /** Compile-time constant equivalent to [name]. */
         const val cname: String = """Close"""
         /** Compile-time constant equivalent to [info]. */
         const val cinfo: String = """Closes this application"""
         /** Compile-time constant equivalent to [group]. */
         const val cgroup: String = """App"""
         /** Name of the config. */
         override val name = cname
         /** Group of the config. */
         override val group = cgroup
         /** Description of the config. */
         override val info = cinfo
         /** Editability of the config. */
         override val editable = EditMode.USER
      }
      /** Restarts this application. */
      object `restart`: ConfigDefinition {
         /** Compile-time constant equivalent to [name]. */
         const val cname: String = """Restart"""
         /** Compile-time constant equivalent to [info]. */
         const val cinfo: String = """Restarts this application"""
         /** Compile-time constant equivalent to [group]. */
         const val cgroup: String = """App"""
         /** Name of the config. */
         override val name = cname
         /** Group of the config. */
         override val group = cgroup
         /** Description of the config. */
         override val info = cinfo
         /** Editability of the config. */
         override val editable = EditMode.USER
      }
      /** Loads last application state if not yet loaded. */
      object `startNormally`: ConfigDefinition {
         /** Compile-time constant equivalent to [name]. */
         const val cname: String = """Start normally"""
         /** Compile-time constant equivalent to [info]. */
         const val cinfo: String = """Loads last application state if not yet loaded"""
         /** Compile-time constant equivalent to [group]. */
         const val cgroup: String = """App"""
         /** Name of the config. */
         override val name = cname
         /** Group of the config. */
         override val group = cgroup
         /** Description of the config. */
         override val info = cinfo
         /** Editability of the config. */
         override val editable = EditMode.USER
      }
      /** Run JVM garbage collector using 'System.gc()'. Requires developer mode enabled.. */
      object `runGarbageCollector`: ConfigDefinition {
         /** Compile-time constant equivalent to [name]. */
         const val cname: String = """Run garbage collector"""
         /** Compile-time constant equivalent to [info]. */
         const val cinfo: String = """Run JVM garbage collector using 'System.gc()'. Requires developer mode enabled."""
         /** Compile-time constant equivalent to [group]. */
         const val cgroup: String = """App"""
         /** Name of the config. */
         override val name = cname
         /** Group of the config. */
         override val group = cgroup
         /** Description of the config. */
         override val info = cinfo
         /** Editability of the config. */
         override val editable = EditMode.USER
      }
   }
   object `plugins` {
      /** Name of the group. */
      const val name = "Plugins"

      object `screenDock` {
         /** Name of the group. */
         const val name = "Screen Dock"

         /** Enable/disable this plugin. Whether application has docked window in the top of the screen. */
         object `enable`: ConfigDefinition {
            /** Compile-time constant equivalent to [name]. */
            const val cname: String = """Enable"""
            /** Compile-time constant equivalent to [info]. */
            const val cinfo: String = """Enable/disable this plugin. Whether application has docked window in the top of the screen"""
            /** Compile-time constant equivalent to [group]. */
            const val cgroup: String = """Plugins.Screen Dock"""
            /** Name of the config. */
            override val name = cname
            /** Group of the config. */
            override val group = cgroup
            /** Description of the config. */
            override val info = cinfo
            /** Editability of the config. */
            override val editable = EditMode.USER
         }
         /** Component displayed as content in the dock. */
         object `content`: ConfigDefinition {
            /** Compile-time constant equivalent to [name]. */
            const val cname: String = """Content"""
            /** Compile-time constant equivalent to [info]. */
            const val cinfo: String = """Component displayed as content in the dock"""
            /** Compile-time constant equivalent to [group]. */
            const val cgroup: String = """Plugins.Screen Dock"""
            /** Name of the config. */
            override val name = cname
            /** Group of the config. */
            override val group = cgroup
            /** Description of the config. */
            override val info = cinfo
            /** Editability of the config. */
            override val editable = EditMode.USER
         }
         /** Mouse hover time it takes for the dock to show. */
         object `showDelay`: ConfigDefinition {
            /** Compile-time constant equivalent to [name]. */
            const val cname: String = """Show delay"""
            /** Compile-time constant equivalent to [info]. */
            const val cinfo: String = """Mouse hover time it takes for the dock to show"""
            /** Compile-time constant equivalent to [group]. */
            const val cgroup: String = """Plugins.Screen Dock"""
            /** Name of the config. */
            override val name = cname
            /** Group of the config. */
            override val group = cgroup
            /** Description of the config. */
            override val info = cinfo
            /** Editability of the config. */
            override val editable = EditMode.USER
         }
         /** Hide dock when no mouse activity is detected. */
         object `hideOnIdle`: ConfigDefinition {
            /** Compile-time constant equivalent to [name]. */
            const val cname: String = """Hide on idle"""
            /** Compile-time constant equivalent to [info]. */
            const val cinfo: String = """Hide dock when no mouse activity is detected"""
            /** Compile-time constant equivalent to [group]. */
            const val cgroup: String = """Plugins.Screen Dock"""
            /** Name of the config. */
            override val name = cname
            /** Group of the config. */
            override val group = cgroup
            /** Description of the config. */
            override val info = cinfo
            /** Editability of the config. */
            override val editable = EditMode.USER
         }
         /** Mouse away time it takes for the dock to hide. */
         object `hideOnIdleDelay`: ConfigDefinition {
            /** Compile-time constant equivalent to [name]. */
            const val cname: String = """Hide on idle delay"""
            /** Compile-time constant equivalent to [info]. */
            const val cinfo: String = """Mouse away time it takes for the dock to hide"""
            /** Compile-time constant equivalent to [group]. */
            const val cgroup: String = """Plugins.Screen Dock"""
            /** Name of the config. */
            override val name = cname
            /** Group of the config. */
            override val group = cgroup
            /** Description of the config. */
            override val info = cinfo
            /** Editability of the config. */
            override val editable = EditMode.USER
         }
      }
   }
   object `search` {
      /** Name of the group. */
      const val name = "Search"

      /** Sources providing potential search results. */
      object `sources`: ConfigDefinition {
         /** Compile-time constant equivalent to [name]. */
         const val cname: String = """Sources"""
         /** Compile-time constant equivalent to [info]. */
         const val cinfo: String = """Sources providing potential search results"""
         /** Compile-time constant equivalent to [group]. */
         const val cgroup: String = """Search"""
         /** Name of the config. */
         override val name = cname
         /** Group of the config. */
         override val group = cgroup
         /** Description of the config. */
         override val info = cinfo
         /** Editability of the config. */
         override val editable = EditMode.APP
      }
      /** Algorithm for text matching.. */
      object `searchAlgorithm`: ConfigDefinition {
         /** Compile-time constant equivalent to [name]. */
         const val cname: String = """Search algorithm"""
         /** Compile-time constant equivalent to [info]. */
         const val cinfo: String = """Algorithm for text matching."""
         /** Compile-time constant equivalent to [group]. */
         const val cgroup: String = """Search"""
         /** Name of the config. */
         override val name = cname
         /** Group of the config. */
         override val group = cgroup
         /** Description of the config. */
         override val info = cinfo
         /** Editability of the config. */
         override val editable = EditMode.USER
      }
      /** Algorithm for text matching will ignore case.. */
      object `searchIgnoreCase`: ConfigDefinition {
         /** Compile-time constant equivalent to [name]. */
         const val cname: String = """Search ignore case"""
         /** Compile-time constant equivalent to [info]. */
         const val cinfo: String = """Algorithm for text matching will ignore case."""
         /** Compile-time constant equivalent to [group]. */
         const val cgroup: String = """Search"""
         /** Name of the config. */
         override val name = cname
         /** Group of the config. */
         override val group = cgroup
         /** Description of the config. */
         override val info = cinfo
         /** Editability of the config. */
         override val editable = EditMode.USER
      }
      /** Maximal time delay between key strokes. Search text is reset after the delay runs out.. */
      object `searchDelay`: ConfigDefinition {
         /** Compile-time constant equivalent to [name]. */
         const val cname: String = """Search delay"""
         /** Compile-time constant equivalent to [info]. */
         const val cinfo: String = """Maximal time delay between key strokes. Search text is reset after the delay runs out."""
         /** Compile-time constant equivalent to [group]. */
         const val cgroup: String = """Search"""
         /** Name of the config. */
         override val name = cname
         /** Group of the config. */
         override val group = cgroup
         /** Description of the config. */
         override val info = cinfo
         /** Editability of the config. */
         override val editable = EditMode.USER
      }
      /** Deactivates search after period of inactivity.. */
      object `searchAutoCancel`: ConfigDefinition {
         /** Compile-time constant equivalent to [name]. */
         const val cname: String = """Search auto-cancel"""
         /** Compile-time constant equivalent to [info]. */
         const val cinfo: String = """Deactivates search after period of inactivity."""
         /** Compile-time constant equivalent to [group]. */
         const val cgroup: String = """Search"""
         /** Name of the config. */
         override val name = cname
         /** Group of the config. */
         override val group = cgroup
         /** Description of the config. */
         override val info = cinfo
         /** Editability of the config. */
         override val editable = EditMode.USER
      }
      /** Period of inactivity after which search is automatically deactivated.. */
      object `searchAutoCancelDelay`: ConfigDefinition {
         /** Compile-time constant equivalent to [name]. */
         const val cname: String = """Search auto-cancel delay"""
         /** Compile-time constant equivalent to [info]. */
         const val cinfo: String = """Period of inactivity after which search is automatically deactivated."""
         /** Compile-time constant equivalent to [group]. */
         const val cgroup: String = """Search"""
         /** Name of the config. */
         override val name = cname
         /** Group of the config. */
         override val group = cgroup
         /** Description of the config. */
         override val info = cinfo
         /** Editability of the config. */
         override val editable = EditMode.USER
      }
   }
   object `ui` {
      /** Name of the group. */
      const val name = "Ui"

      /** Skin of the application. Determines single stylesheet file applied on `.root` of all windows.. */
      object `skin`: ConfigDefinition {
         /** Compile-time constant equivalent to [name]. */
         const val cname: String = """Skin"""
         /** Compile-time constant equivalent to [info]. */
         const val cinfo: String = """Skin of the application. Determines single stylesheet file applied on `.root` of all windows."""
         /** Compile-time constant equivalent to [group]. */
         const val cgroup: String = """Ui"""
         /** Name of the config. */
         override val name = cname
         /** Group of the config. */
         override val group = cgroup
         /** Description of the config. */
         override val info = cinfo
         /** Editability of the config. */
         override val editable = EditMode.USER
      }
      /** Additional stylesheet files applied on `.root` of all windows. Override styles set by the skin. Applied in the specified order.. */
      object `skinExtensions`: ConfigDefinition {
         /** Compile-time constant equivalent to [name]. */
         const val cname: String = """Skin extensions"""
         /** Compile-time constant equivalent to [info]. */
         const val cinfo: String = """Additional stylesheet files applied on `.root` of all windows. Override styles set by the skin. Applied in the specified order."""
         /** Compile-time constant equivalent to [group]. */
         const val cgroup: String = """Ui"""
         /** Name of the config. */
         override val name = cname
         /** Group of the config. */
         override val group = cgroup
         /** Description of the config. */
         override val info = cinfo
         /** Editability of the config. */
         override val editable = EditMode.USER
      }
      /** Font of the application. Overrides font set by the skin, using `-fx-font-family` and `-fx-font-size` applied `.root` of all windows. Null retains font set by the skin.. */
      object `font`: ConfigDefinition {
         /** Compile-time constant equivalent to [name]. */
         const val cname: String = """Font"""
         /** Compile-time constant equivalent to [info]. */
         const val cinfo: String = """Font of the application. Overrides font set by the skin, using `-fx-font-family` and `-fx-font-size` applied `.root` of all windows. Null retains font set by the skin."""
         /** Compile-time constant equivalent to [group]. */
         const val cgroup: String = """Ui"""
         /** Name of the config. */
         override val name = cname
         /** Group of the config. */
         override val group = cgroup
         /** Description of the config. */
         override val info = cinfo
         /** Editability of the config. */
         override val editable = EditMode.USER
      }
      /** Layout mode use blur effect. */
      object `layoutModeBlurBgr`: ConfigDefinition {
         /** Compile-time constant equivalent to [name]. */
         const val cname: String = """Layout mode blur bgr"""
         /** Compile-time constant equivalent to [info]. */
         const val cinfo: String = """Layout mode use blur effect"""
         /** Compile-time constant equivalent to [group]. */
         const val cgroup: String = """Ui"""
         /** Name of the config. */
         override val name = cname
         /** Group of the config. */
         override val group = cgroup
         /** Description of the config. */
         override val info = cinfo
         /** Editability of the config. */
         override val editable = EditMode.USER
      }
      /** Layout mode use fade effect. */
      object `layoutModeFadeBgr`: ConfigDefinition {
         /** Compile-time constant equivalent to [name]. */
         const val cname: String = """Layout mode fade bgr"""
         /** Compile-time constant equivalent to [info]. */
         const val cinfo: String = """Layout mode use fade effect"""
         /** Compile-time constant equivalent to [group]. */
         const val cgroup: String = """Ui"""
         /** Name of the config. */
         override val name = cname
         /** Group of the config. */
         override val group = cgroup
         /** Description of the config. */
         override val info = cinfo
         /** Editability of the config. */
         override val editable = EditMode.USER
      }
      /** Layout mode fade effect intensity.. */
      object `layoutModeFadeIntensity`: ConfigDefinition {
         /** Compile-time constant equivalent to [name]. */
         const val cname: String = """Layout mode fade intensity"""
         /** Compile-time constant equivalent to [info]. */
         const val cinfo: String = """Layout mode fade effect intensity."""
         /** Compile-time constant equivalent to [group]. */
         const val cgroup: String = """Ui"""
         /** Name of the config. */
         override val name = cname
         /** Group of the config. */
         override val group = cgroup
         /** Description of the config. */
         override val info = cinfo
         /** Editability of the config. */
         override val editable = EditMode.USER
      }
      /** Layout mode blur effect intensity.. */
      object `layoutModeBlurIntensity`: ConfigDefinition {
         /** Compile-time constant equivalent to [name]. */
         const val cname: String = """Layout mode blur intensity"""
         /** Compile-time constant equivalent to [info]. */
         const val cinfo: String = """Layout mode blur effect intensity."""
         /** Compile-time constant equivalent to [group]. */
         const val cgroup: String = """Ui"""
         /** Name of the config. */
         override val name = cname
         /** Group of the config. */
         override val group = cgroup
         /** Description of the config. */
         override val info = cinfo
         /** Editability of the config. */
         override val editable = EditMode.USER
      }
      /** Duration of layout mode transition effects.. */
      object `layoutModeAnimLength`: ConfigDefinition {
         /** Compile-time constant equivalent to [name]. */
         const val cname: String = """Layout mode anim length"""
         /** Compile-time constant equivalent to [info]. */
         const val cinfo: String = """Duration of layout mode transition effects."""
         /** Compile-time constant equivalent to [group]. */
         const val cgroup: String = """Ui"""
         /** Name of the config. */
         override val name = cname
         /** Group of the config. */
         override val group = cgroup
         /** Description of the config. */
         override val info = cinfo
         /** Editability of the config. */
         override val editable = EditMode.USER
      }
      /** Allows snapping feature for windows and controls.. */
      object `snap`: ConfigDefinition {
         /** Compile-time constant equivalent to [name]. */
         const val cname: String = """Snap"""
         /** Compile-time constant equivalent to [info]. */
         const val cinfo: String = """Allows snapping feature for windows and controls."""
         /** Compile-time constant equivalent to [group]. */
         const val cgroup: String = """Ui"""
         /** Name of the config. */
         override val name = cname
         /** Group of the config. */
         override val group = cgroup
         /** Description of the config. */
         override val info = cinfo
         /** Editability of the config. */
         override val editable = EditMode.USER
      }
      /** Distance at which snap feature gets activated. */
      object `snapActivationDistance`: ConfigDefinition {
         /** Compile-time constant equivalent to [name]. */
         const val cname: String = """Snap activation distance"""
         /** Compile-time constant equivalent to [info]. */
         const val cinfo: String = """Distance at which snap feature gets activated"""
         /** Compile-time constant equivalent to [group]. */
         const val cgroup: String = """Ui"""
         /** Name of the config. */
         override val name = cname
         /** Group of the config. */
         override val group = cgroup
         /** Description of the config. */
         override val info = cinfo
         /** Editability of the config. */
         override val editable = EditMode.USER
      }
      /** Locked layout will not enter layout mode.. */
      object `lockLayout`: ConfigDefinition {
         /** Compile-time constant equivalent to [name]. */
         const val cname: String = """Lock layout"""
         /** Compile-time constant equivalent to [info]. */
         const val cinfo: String = """Locked layout will not enter layout mode."""
         /** Compile-time constant equivalent to [group]. */
         const val cgroup: String = """Ui"""
         /** Name of the config. */
         override val name = cname
         /** Group of the config. */
         override val group = cgroup
         /** Description of the config. */
         override val info = cinfo
         /** Editability of the config. */
         override val editable = EditMode.USER
      }
      /** Rating ui component skin. */
      object `ratingSkin`: ConfigDefinition {
         /** Compile-time constant equivalent to [name]. */
         const val cname: String = """Rating skin"""
         /** Compile-time constant equivalent to [info]. */
         const val cinfo: String = """Rating ui component skin"""
         /** Compile-time constant equivalent to [group]. */
         const val cgroup: String = """Ui"""
         /** Name of the config. */
         override val name = cname
         /** Group of the config. */
         override val group = cgroup
         /** Description of the config. */
         override val info = cinfo
         /** Editability of the config. */
         override val editable = EditMode.USER
      }
      /** Number of icons in rating control.. */
      object `ratingIconAmount`: ConfigDefinition {
         /** Compile-time constant equivalent to [name]. */
         const val cname: String = """Rating icon amount"""
         /** Compile-time constant equivalent to [info]. */
         const val cinfo: String = """Number of icons in rating control."""
         /** Compile-time constant equivalent to [group]. */
         const val cgroup: String = """Ui"""
         /** Name of the config. */
         override val name = cname
         /** Group of the config. */
         override val group = cgroup
         /** Description of the config. */
         override val info = cinfo
         /** Editability of the config. */
         override val editable = EditMode.USER
      }
      /** Allow partial values for rating.. */
      object `ratingAllowPartial`: ConfigDefinition {
         /** Compile-time constant equivalent to [name]. */
         const val cname: String = """Rating allow partial"""
         /** Compile-time constant equivalent to [info]. */
         const val cinfo: String = """Allow partial values for rating."""
         /** Compile-time constant equivalent to [group]. */
         const val cgroup: String = """Ui"""
         /** Name of the config. */
         override val name = cname
         /** Group of the config. */
         override val group = cgroup
         /** Description of the config. */
         override val info = cinfo
         /** Editability of the config. */
         override val editable = EditMode.USER
      }
      object `image` {
         /** Name of the group. */
         const val name = "Image"

         /** Preferred hover scale animation duration for thumbnails.. */
         object `thumbnailAnimDuration`: ConfigDefinition {
            /** Compile-time constant equivalent to [name]. */
            const val cname: String = """Thumbnail anim duration"""
            /** Compile-time constant equivalent to [info]. */
            const val cinfo: String = """Preferred hover scale animation duration for thumbnails."""
            /** Compile-time constant equivalent to [group]. */
            const val cgroup: String = """Ui.Image"""
            /** Name of the config. */
            override val name = cname
            /** Group of the config. */
            override val group = cgroup
            /** Description of the config. */
            override val info = cinfo
            /** Editability of the config. */
            override val editable = EditMode.USER
         }
      }
      object `list` {
         /** Name of the group. */
         const val name = "List"

      }
      object `table` {
         /** Name of the group. */
         const val name = "Table"

         /** Orientation of the table. */
         object `tableOrientation`: ConfigDefinition {
            /** Compile-time constant equivalent to [name]. */
            const val cname: String = """Table orientation"""
            /** Compile-time constant equivalent to [info]. */
            const val cinfo: String = """Orientation of the table"""
            /** Compile-time constant equivalent to [group]. */
            const val cgroup: String = """Ui.Table"""
            /** Name of the config. */
            override val name = cname
            /** Group of the config. */
            override val group = cgroup
            /** Description of the config. */
            override val info = cinfo
            /** Editability of the config. */
            override val editable = EditMode.USER
         }
         /** Adds 0s for number length consistency. */
         object `zeropadNumbers`: ConfigDefinition {
            /** Compile-time constant equivalent to [name]. */
            const val cname: String = """Zeropad numbers"""
            /** Compile-time constant equivalent to [info]. */
            const val cinfo: String = """Adds 0s for number length consistency"""
            /** Compile-time constant equivalent to [group]. */
            const val cgroup: String = """Ui.Table"""
            /** Name of the config. */
            override val name = cname
            /** Group of the config. */
            override val group = cgroup
            /** Description of the config. */
            override val info = cinfo
            /** Editability of the config. */
            override val editable = EditMode.USER
         }
         /** Show unfiltered table item index when filter applied. */
         object `searchShowOriginalIndex`: ConfigDefinition {
            /** Compile-time constant equivalent to [name]. */
            const val cname: String = """Search show original index"""
            /** Compile-time constant equivalent to [info]. */
            const val cinfo: String = """Show unfiltered table item index when filter applied"""
            /** Compile-time constant equivalent to [group]. */
            const val cgroup: String = """Ui.Table"""
            /** Name of the config. */
            override val name = cname
            /** Group of the config. */
            override val group = cgroup
            /** Description of the config. */
            override val info = cinfo
            /** Editability of the config. */
            override val editable = EditMode.USER
         }
         /** Show table header with columns. */
         object `showTableHeader`: ConfigDefinition {
            /** Compile-time constant equivalent to [name]. */
            const val cname: String = """Show table header"""
            /** Compile-time constant equivalent to [info]. */
            const val cinfo: String = """Show table header with columns"""
            /** Compile-time constant equivalent to [group]. */
            const val cgroup: String = """Ui.Table"""
            /** Name of the config. */
            override val name = cname
            /** Group of the config. */
            override val group = cgroup
            /** Description of the config. */
            override val info = cinfo
            /** Editability of the config. */
            override val editable = EditMode.USER
         }
         /** Show table controls at the bottom of the table. Displays menu bar and table content information.. */
         object `showTableFooter`: ConfigDefinition {
            /** Compile-time constant equivalent to [name]. */
            const val cname: String = """Show table footer"""
            /** Compile-time constant equivalent to [info]. */
            const val cinfo: String = """Show table controls at the bottom of the table. Displays menu bar and table content information."""
            /** Compile-time constant equivalent to [group]. */
            const val cgroup: String = """Ui.Table"""
            /** Name of the config. */
            override val name = cname
            /** Group of the config. */
            override val group = cgroup
            /** Description of the config. */
            override val info = cinfo
            /** Editability of the config. */
            override val editable = EditMode.USER
         }
      }
      object `grid` {
         /** Name of the group. */
         const val name = "Grid"

         /** Determines horizontal alignment of the grid cells within the grid.. */
         object `cellAlignment`: ConfigDefinition {
            /** Compile-time constant equivalent to [name]. */
            const val cname: String = """Cell alignment"""
            /** Compile-time constant equivalent to [info]. */
            const val cinfo: String = """Determines horizontal alignment of the grid cells within the grid."""
            /** Compile-time constant equivalent to [group]. */
            const val cgroup: String = """Ui.Grid"""
            /** Name of the config. */
            override val name = cname
            /** Group of the config. */
            override val group = cgroup
            /** Description of the config. */
            override val info = cinfo
            /** Editability of the config. */
            override val editable = EditMode.USER
         }
         /** Show grid controls at the bottom of the table. Displays menu bar and table content information.. */
         object `showGridFooter`: ConfigDefinition {
            /** Compile-time constant equivalent to [name]. */
            const val cname: String = """Show grid footer"""
            /** Compile-time constant equivalent to [info]. */
            const val cinfo: String = """Show grid controls at the bottom of the table. Displays menu bar and table content information."""
            /** Compile-time constant equivalent to [group]. */
            const val cgroup: String = """Ui.Grid"""
            /** Name of the config. */
            override val name = cname
            /** Group of the config. */
            override val group = cgroup
            /** Description of the config. */
            override val info = cinfo
            /** Editability of the config. */
            override val editable = EditMode.USER
         }
      }
      object `tabs` {
         /** Name of the group. */
         const val name = "Tabs"

         /** Use discrete (D) and forbid seamless (S) tab switching. Tabs are always aligned. Seamless mode allows any tab position.. */
         object `discreteMode(D)`: ConfigDefinition {
            /** Compile-time constant equivalent to [name]. */
            const val cname: String = """Discrete mode (D)"""
            /** Compile-time constant equivalent to [info]. */
            const val cinfo: String = """Use discrete (D) and forbid seamless (S) tab switching. Tabs are always aligned. Seamless mode allows any tab position."""
            /** Compile-time constant equivalent to [group]. */
            const val cgroup: String = """Ui.Tabs"""
            /** Name of the config. */
            override val name = cname
            /** Group of the config. */
            override val group = cgroup
            /** Description of the config. */
            override val info = cinfo
            /** Editability of the config. */
            override val editable = EditMode.USER
         }
         /** Required length of drag at which tab switch animation gets activated. Tab switch activates if at least one condition is fulfilled min distance or min fraction.. */
         object `switchDragDistance(D)`: ConfigDefinition {
            /** Compile-time constant equivalent to [name]. */
            const val cname: String = """Switch drag distance (D)"""
            /** Compile-time constant equivalent to [info]. */
            const val cinfo: String = """Required length of drag at which tab switch animation gets activated. Tab switch activates if at least one condition is fulfilled min distance or min fraction."""
            /** Compile-time constant equivalent to [group]. */
            const val cgroup: String = """Ui.Tabs"""
            /** Name of the config. */
            override val name = cname
            /** Group of the config. */
            override val group = cgroup
            /** Description of the config. */
            override val info = cinfo
            /** Editability of the config. */
            override val editable = EditMode.USER
         }
         /** Defines distance from edge in percent of tab's width in which the tab switches.. */
         object `switchDragDistanceCoefficient(D)`: ConfigDefinition {
            /** Compile-time constant equivalent to [name]. */
            const val cname: String = """Switch drag distance coefficient (D)"""
            /** Compile-time constant equivalent to [info]. */
            const val cinfo: String = """Defines distance from edge in percent of tab's width in which the tab switches."""
            /** Compile-time constant equivalent to [group]. */
            const val cgroup: String = """Ui.Tabs"""
            /** Name of the config. */
            override val name = cname
            /** Group of the config. */
            override val group = cgroup
            /** Description of the config. */
            override val info = cinfo
            /** Editability of the config. */
            override val editable = EditMode.USER
         }
         /** Inertia of the tab switch animation. Defines distance the dragging will travel after input has been stopped.. */
         object `dragInertia(S)`: ConfigDefinition {
            /** Compile-time constant equivalent to [name]. */
            const val cname: String = """Drag inertia (S)"""
            /** Compile-time constant equivalent to [info]. */
            const val cinfo: String = """Inertia of the tab switch animation. Defines distance the dragging will travel after input has been stopped."""
            /** Compile-time constant equivalent to [group]. */
            const val cgroup: String = """Ui.Tabs"""
            /** Name of the config. */
            override val name = cname
            /** Group of the config. */
            override val group = cgroup
            /** Description of the config. */
            override val info = cinfo
            /** Editability of the config. */
            override val editable = EditMode.USER
         }
         /** Defines distance from edge in percent of tab's width in which the tab auto-aligns. Setting to maximum (0.5) has effect of always snapping the tabs, while setting to minimum (0) has effect of disabling tab snapping.. */
         object `snapDistanceCoefficient(S)`: ConfigDefinition {
            /** Compile-time constant equivalent to [name]. */
            const val cname: String = """Snap distance coefficient (S)"""
            /** Compile-time constant equivalent to [info]. */
            const val cinfo: String = """Defines distance from edge in percent of tab's width in which the tab auto-aligns. Setting to maximum (0.5) has effect of always snapping the tabs, while setting to minimum (0) has effect of disabling tab snapping."""
            /** Compile-time constant equivalent to [group]. */
            const val cgroup: String = """Ui.Tabs"""
            /** Name of the config. */
            override val name = cname
            /** Group of the config. */
            override val group = cgroup
            /** Description of the config. */
            override val info = cinfo
            /** Editability of the config. */
            override val editable = EditMode.USER
         }
         /** Required distance from edge at which tabs align. Tab snap activates if at least one condition is fulfilled min distance or min fraction.. */
         object `snapDistance(S)`: ConfigDefinition {
            /** Compile-time constant equivalent to [name]. */
            const val cname: String = """Snap distance (S)"""
            /** Compile-time constant equivalent to [info]. */
            const val cinfo: String = """Required distance from edge at which tabs align. Tab snap activates if at least one condition is fulfilled min distance or min fraction."""
            /** Compile-time constant equivalent to [group]. */
            const val cgroup: String = """Ui.Tabs"""
            /** Name of the config. */
            override val name = cname
            /** Group of the config. */
            override val group = cgroup
            /** Description of the config. */
            override val info = cinfo
            /** Editability of the config. */
            override val editable = EditMode.USER
         }
         /** Zoom factor. */
         object `zoom`: ConfigDefinition {
            /** Compile-time constant equivalent to [name]. */
            const val cname: String = """Zoom"""
            /** Compile-time constant equivalent to [info]. */
            const val cinfo: String = """Zoom factor"""
            /** Compile-time constant equivalent to [group]. */
            const val cgroup: String = """Ui.Tabs"""
            /** Name of the config. */
            override val name = cname
            /** Group of the config. */
            override val group = cgroup
            /** Description of the config. */
            override val info = cinfo
            /** Editability of the config. */
            override val editable = EditMode.USER
         }
      }
      object `form` {
         /** Name of the group. */
         const val name = "Form"

         /** Initial value of forms' editors' layout.. */
         object `layout`: ConfigDefinition {
            /** Compile-time constant equivalent to [name]. */
            const val cname: String = """Layout"""
            /** Compile-time constant equivalent to [info]. */
            const val cinfo: String = """Initial value of forms' editors' layout."""
            /** Compile-time constant equivalent to [group]. */
            const val cgroup: String = """Ui.Form"""
            /** Name of the config. */
            override val name = cname
            /** Group of the config. */
            override val group = cgroup
            /** Description of the config. */
            override val info = cinfo
            /** Editability of the config. */
            override val editable = EditMode.USER
         }
      }
      object `view` {
         /** Name of the group. */
         const val name = "View"

         /** Covered area. Screen overlay provides more space than window, but it can disrupt work flow.. */
         object `overlayArea`: ConfigDefinition {
            /** Compile-time constant equivalent to [name]. */
            const val cname: String = """Overlay area"""
            /** Compile-time constant equivalent to [info]. */
            const val cinfo: String = """Covered area. Screen overlay provides more space than window, but it can disrupt work flow."""
            /** Compile-time constant equivalent to [group]. */
            const val cgroup: String = """Ui.View"""
            /** Name of the config. */
            override val name = cname
            /** Group of the config. */
            override val group = cgroup
            /** Description of the config. */
            override val info = cinfo
            /** Editability of the config. */
            override val editable = EditMode.USER
         }
         /** Background image source. */
         object `overlayBackground`: ConfigDefinition {
            /** Compile-time constant equivalent to [name]. */
            const val cname: String = """Overlay background"""
            /** Compile-time constant equivalent to [info]. */
            const val cinfo: String = """Background image source"""
            /** Compile-time constant equivalent to [group]. */
            const val cgroup: String = """Ui.View"""
            /** Name of the config. */
            override val name = cname
            /** Group of the config. */
            override val group = cgroup
            /** Description of the config. */
            override val info = cinfo
            /** Editability of the config. */
            override val editable = EditMode.USER
         }
         object `actionViewer` {
            /** Name of the group. */
            const val name = "Action Viewer"

            /** Closes the chooser when action finishes running.. */
            object `closeWhenActionEnds`: ConfigDefinition {
               /** Compile-time constant equivalent to [name]. */
               const val cname: String = """Close when action ends"""
               /** Compile-time constant equivalent to [info]. */
               const val cinfo: String = """Closes the chooser when action finishes running."""
               /** Compile-time constant equivalent to [group]. */
               const val cgroup: String = """Ui.View.Action Viewer"""
               /** Name of the config. */
               override val name = cname
               /** Group of the config. */
               override val group = cgroup
               /** Description of the config. */
               override val info = cinfo
               /** Editability of the config. */
               override val editable = EditMode.USER
            }
         }
         object `shortcutViewer` {
            /** Name of the group. */
            const val name = "Shortcut Viewer"

            /** Displays only shortcuts that have keys assigned. */
            object `hideUnassignedShortcuts`: ConfigDefinition {
               /** Compile-time constant equivalent to [name]. */
               const val cname: String = """Hide unassigned shortcuts"""
               /** Compile-time constant equivalent to [info]. */
               const val cinfo: String = """Displays only shortcuts that have keys assigned"""
               /** Compile-time constant equivalent to [group]. */
               const val cgroup: String = """Ui.View.Shortcut Viewer"""
               /** Name of the config. */
               override val name = cname
               /** Group of the config. */
               override val group = cgroup
               /** Description of the config. */
               override val info = cinfo
               /** Editability of the config. */
               override val editable = EditMode.USER
            }
         }
      }
      object `window` {
         /** Name of the group. */
         const val name = "Window"

      }
   }
}
