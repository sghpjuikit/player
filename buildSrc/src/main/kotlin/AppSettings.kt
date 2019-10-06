val appSetting = Setting.root {
   "General" {
      config("Close app")
      config("Developer mode")
      config("Manage VM options")
   }
   "Logging" {

   }
   "Plugins" {
      "Guide" {
         config("Hint") {
            info = "Last viewed hint. Showed next time the guide opens."
            editable = EditMode.APP
         }
         config("Show guide on app start") {
            info = "Show guide when application starts. Default true, but when guide is shown, it is set to false so the guide will never appear again on its own."
            editable = EditMode.APP
         }
      }
   }
   "Search" {
      config("Sources") {
         info = "Sources providing potential search results"
         editable = EditMode.APP
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
         info = "Application skin"
      }
      config("Font") {
         info = "Application font"
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
         config("Show table controls") {
            info = "Show table controls at the bottom of the table. Displays menu bar and table content information."
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
      "Dock" {

      }
   }
}