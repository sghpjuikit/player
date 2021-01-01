import EditMode.APP
import EditMode.NONE

val appSetting = Setting.root {
   "App" {
      "Logging" {
         config("Level (stdout)") {
            info = "Logging level for logging to standard output"
         }
         config("Level (file)") {
            info = "Logging level for logging to file"
         }
      }
      "Settings" {
         config("Save settings") {
            info = "Saves settings to the default application properties file"
         }
         config("Save settings to file...") {
            info = "Saves/exports settings to a file"
         }
         config("Load default settings") {
            info = "Loads settings to default values. Discards all non-default settings."
         }
         config("Load settings") {
            info = "Loads settings from default application properties file. Discards any unsaved settings."
         }
         config("Load settings from file...") {
            info = "Loads/imports settings from default application properties file"
         }
      }
      config("Rank") {
         editable = NONE
         info = "Rank of this application instance.\n" +
            "User may wish to run certain components as separate processes. In order to make these lightweight and " +
            "safe, certain features might be disabled or delegated to the primary application instance called `MASTER`.\n" +
            "Other instances are called `SLAVE` instances. They can be thought of as single-purpose short-lived " +
            "one-off programs.\n" +
            "The rank is determined at instance start up. If no other instances (of any rank) are running, the instance " +
            "becomes `MASTER`, otherwise it becomes `SLAVE`. The rank can not be specified at startup or changed later.\n" +
            "Closing `MASTER` instance will not close `SLAVE` instances nor turn them into `MASTER` instance."
      }
      config("Developer mode") {
         info = "Enables certain features. Can be forced to `true` by starting the application with a `--dev` flag." +
            "\nFeatures:" +
            "\n  * Widgets will not recompile when application jars are modified (Prevents recompilation on every application build)" +
            "\n  * Enables menu items that call object's methods using reflection" +
            "\n  * Shows experimental widgets" +
            "\n  * Shows class information about objects in object details"
      }
      config("Close") {
         info = "Closes this application"
      }
      config("Start normally") {
         info = "Loads last application state if not yet loaded"
      }
      config("Run garbage collector") {
         info = "Run JVM garbage collector using 'System.gc()'. Requires developer mode enabled."
      }
   }
   "Plugins" {
      "Guide" {
         config("Hint") {
            info = "Last viewed hint. Showed next time the guide opens."
            editable = APP
         }
         config("Show guide on app start") {
            info = "Show guide when application starts. Default true, but when guide is shown, it is set to false so the guide will never appear again on its own."
            editable = APP
         }
      }
      "Screen Dock" {
         config("Enable") {
            info = "Enable/disable this plugin. Whether application has docked window in the top of the screen"
         }
         config("Content") {
            info = "Component displayed as content in the dock"
         }
         config("Show delay") {
            info = "Mouse hover time it takes for the dock to show"
         }
         config("Hide on idle") {
            info = "Hide dock when no mouse activity is detected"
         }
         config("Hide on idle delay") {
            info = "Mouse away time it takes for the dock to hide"
         }

      }
   }
   "Search" {
      config("Sources") {
         info = "Sources providing potential search results"
         editable = APP
      }
      config("Search algorithm") {
         info = "Algorithm for text matching."
      }
      config("Search ignore case") {
         info = "Algorithm for text matching will ignore case."
      }
      config("Search delay") {
         info = "Maximal time delay between key strokes. Search text is reset after the delay runs out."
      }
      config("Search auto-cancel") {
         info = "Deactivates search after period of inactivity."
      }
      config("Search auto-cancel delay") {
         info = "Period of inactivity after which search is automatically deactivated."
      }
   }
   "Ui" {
      config("Skin") {
         info = "Skin of the application. Determines single stylesheet file applied on `.root` of all windows."
      }
      config("Font") {
         info = "Font of the application. Overrides font set by the skin, using `-fx-font-family` and `-fx-font-size` applied `.root` of all windows. Null retains font set by the skin."
      }
      config("Layout mode blur bgr") {
         info = "Layout mode use blur effect"
      }
      config("Layout mode fade bgr") {
         info = "Layout mode use fade effect"
      }
      config("Layout mode fade intensity") {
         info = "Layout mode fade effect intensity."
      }
      config("Layout mode blur intensity") {
         info = "Layout mode blur effect intensity."
      }
      config("Layout mode anim length") {
         info = "Duration of layout mode transition effects."
      }
      config("Snap") {
         info = "Allows snapping feature for windows and controls."
      }
      config("Snap activation distance") {
         info = "Distance at which snap feature gets activated"
      }
      config("Lock layout") {
         info = "Locked layout will not enter layout mode."
      }
      config("Rating skin") {
         info = "Rating ui component skin"
      }
      config("Rating icon amount") {
         info = "Number of icons in rating control."
      }
      config("Rating allow partial") {
         info = "Allow partial values for rating."
      }
      "Image" {
         config("Thumbnail anim duration") {
            info = "Preferred hover scale animation duration for thumbnails."
         }
      }
      "List" {}
      "Table" {
         config("Table orientation") {
            info = "Orientation of the table"
         }
         config("Zeropad numbers") {
            info = "Adds 0s for number length consistency"
         }
         config("Search show original index") {
            info = "Show unfiltered table item index when filter applied"
         }
         config("Show table header") {
            info = "Show table header with columns"
         }
         config("Show table footer") {
            info = "Show table controls at the bottom of the table. Displays menu bar and table content information."
         }
      }
      "Grid" {
         config("Cell alignment") {
            info = "Determines horizontal alignment of the grid cells within the grid."
         }
         config("Show grid footer") {
            info = "Show grid controls at the bottom of the table. Displays menu bar and table content information."
         }
      }
      "Tabs" {

      }
      "View" {
         config("Overlay area") {
            info = "Covered area. Screen overlay provides more space than window, but it can disrupt work flow."
         }
         config("Overlay background") {
            info = "Background image source"
         }
         "Action Viewer" {
            config("Close when action ends") {
               info = "Closes the chooser when action finishes running."
            }
         }
         "Shortcut Viewer" {
            config("Hide unassigned shortcuts") {
               info = "Displays only shortcuts that have keys assigned"
            }
         }
      }
      "Window" {

      }
   }
}