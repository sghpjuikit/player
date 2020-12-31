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

      object `guide` {
         /** Name of the group. */
         const val name = "Guide"

         object `hint`: ConfigDefinition {
            /** Compile-time constant equivalent to [name]. */
            const val cname: String = """Hint"""
            /** Compile-time constant equivalent to [info]. */
            const val cinfo: String = """Last viewed hint. Showed next time the guide opens."""
            /** Compile-time constant equivalent to [group]. */
            const val cgroup: String = """Plugins.Guide"""
            /** Name of the config. */
            override val name = cname
            /** Group of the config. */
            override val group = cgroup
            /** Description of the config. */
            override val info = cinfo
            /** Editability of the config. */
            override val editable = EditMode.APP
         }
         object `showGuideOnAppStart`: ConfigDefinition {
            /** Compile-time constant equivalent to [name]. */
            const val cname: String = """Show guide on app start"""
            /** Compile-time constant equivalent to [info]. */
            const val cinfo: String = """Show guide when application starts. Default true, but when guide is shown, it is set to false so the guide will never appear again on its own."""
            /** Compile-time constant equivalent to [group]. */
            const val cgroup: String = """Plugins.Guide"""
            /** Name of the config. */
            override val name = cname
            /** Group of the config. */
            override val group = cgroup
            /** Description of the config. */
            override val info = cinfo
            /** Editability of the config. */
            override val editable = EditMode.APP
         }
      }
      object `screenDock` {
         /** Name of the group. */
         const val name = "Screen Dock"

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
      object `table` {
         /** Name of the group. */
         const val name = "Table"

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
         object `showTableControls`: ConfigDefinition {
            /** Compile-time constant equivalent to [name]. */
            const val cname: String = """Show table controls"""
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
      object `tabs` {
         /** Name of the group. */
         const val name = "Tabs"

      }
      object `view` {
         /** Name of the group. */
         const val name = "View"

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
