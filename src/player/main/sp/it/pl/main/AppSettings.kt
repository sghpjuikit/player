@file:Suppress("RemoveRedundantBackticks", "unused", "ClassName", "ObjectPropertyName", "SpellCheckingInspection")

package sp.it.pl.main

import sp.it.util.conf.ConfigDefinition
import sp.it.util.conf.EditMode

/** Application settings hierarchy. */
object AppSettings {

   object general {
      /** Name of the group. */
      const val name = "General"

      object closeApp: ConfigDefinition {
         /** Name of the config. Compile-time constant. */
         const val name: String = "Close app"
         /** Description of the config. Compile-time constant. */
         const val info: String = ""
         /** Group of the config. Compile-time constant. */
         const val group: String = "General"
         /** Editability of the config. Compile-time constant. */
         val editable: EditMode = EditMode.USER
         /** Equivalent to [name]. */
         override val configName = name
         /** Equivalent to [group]. */
         override val configGroup = group
         /** Equivalent to [info]. */
         override val configInfo = info
         /** Equivalent to [editable]. */
         override val configEditable = editable
      }
      object developerMode: ConfigDefinition {
         /** Name of the config. Compile-time constant. */
         const val name: String = "Developer mode"
         /** Description of the config. Compile-time constant. */
         const val info: String = ""
         /** Group of the config. Compile-time constant. */
         const val group: String = "General"
         /** Editability of the config. Compile-time constant. */
         val editable: EditMode = EditMode.USER
         /** Equivalent to [name]. */
         override val configName = name
         /** Equivalent to [group]. */
         override val configGroup = group
         /** Equivalent to [info]. */
         override val configInfo = info
         /** Equivalent to [editable]. */
         override val configEditable = editable
      }
      object manageVMOptions: ConfigDefinition {
         /** Name of the config. Compile-time constant. */
         const val name: String = "Manage VM options"
         /** Description of the config. Compile-time constant. */
         const val info: String = ""
         /** Group of the config. Compile-time constant. */
         const val group: String = "General"
         /** Editability of the config. Compile-time constant. */
         val editable: EditMode = EditMode.USER
         /** Equivalent to [name]. */
         override val configName = name
         /** Equivalent to [group]. */
         override val configGroup = group
         /** Equivalent to [info]. */
         override val configInfo = info
         /** Equivalent to [editable]. */
         override val configEditable = editable
      }
   }
   object logging {
      /** Name of the group. */
      const val name = "Logging"

   }
   object plugins {
      /** Name of the group. */
      const val name = "Plugins"

      object guide {
         /** Name of the group. */
         const val name = "Guide"

         object hint: ConfigDefinition {
            /** Name of the config. Compile-time constant. */
            const val name: String = "Hint"
            /** Description of the config. Compile-time constant. */
            const val info: String = "Last viewed hint. Showed next time the guide opens."
            /** Group of the config. Compile-time constant. */
            const val group: String = "Plugins.Guide"
            /** Editability of the config. Compile-time constant. */
            val editable: EditMode = EditMode.APP
            /** Equivalent to [name]. */
            override val configName = name
            /** Equivalent to [group]. */
            override val configGroup = group
            /** Equivalent to [info]. */
            override val configInfo = info
            /** Equivalent to [editable]. */
            override val configEditable = editable
         }
         object showGuideOnAppStart: ConfigDefinition {
            /** Name of the config. Compile-time constant. */
            const val name: String = "Show guide on app start"
            /** Description of the config. Compile-time constant. */
            const val info: String = "Show guide when application starts. Default true, but when guide is shown, it is set to false so the guide will never appear again on its own."
            /** Group of the config. Compile-time constant. */
            const val group: String = "Plugins.Guide"
            /** Editability of the config. Compile-time constant. */
            val editable: EditMode = EditMode.APP
            /** Equivalent to [name]. */
            override val configName = name
            /** Equivalent to [group]. */
            override val configGroup = group
            /** Equivalent to [info]. */
            override val configInfo = info
            /** Equivalent to [editable]. */
            override val configEditable = editable
         }
      }
   }
   object search {
      /** Name of the group. */
      const val name = "Search"

      object sources: ConfigDefinition {
         /** Name of the config. Compile-time constant. */
         const val name: String = "Sources"
         /** Description of the config. Compile-time constant. */
         const val info: String = "Sources providing potential search results"
         /** Group of the config. Compile-time constant. */
         const val group: String = "Search"
         /** Editability of the config. Compile-time constant. */
         val editable: EditMode = EditMode.APP
         /** Equivalent to [name]. */
         override val configName = name
         /** Equivalent to [group]. */
         override val configGroup = group
         /** Equivalent to [info]. */
         override val configInfo = info
         /** Equivalent to [editable]. */
         override val configEditable = editable
      }
      object searchAlgorithm: ConfigDefinition {
         /** Name of the config. Compile-time constant. */
         const val name: String = "Search algorithm"
         /** Description of the config. Compile-time constant. */
         const val info: String = "Algorithm for text matching."
         /** Group of the config. Compile-time constant. */
         const val group: String = "Search"
         /** Editability of the config. Compile-time constant. */
         val editable: EditMode = EditMode.USER
         /** Equivalent to [name]. */
         override val configName = name
         /** Equivalent to [group]. */
         override val configGroup = group
         /** Equivalent to [info]. */
         override val configInfo = info
         /** Equivalent to [editable]. */
         override val configEditable = editable
      }
      object searchIgnoreCase: ConfigDefinition {
         /** Name of the config. Compile-time constant. */
         const val name: String = "Search ignore case"
         /** Description of the config. Compile-time constant. */
         const val info: String = "Algorithm for text matching will ignore case."
         /** Group of the config. Compile-time constant. */
         const val group: String = "Search"
         /** Editability of the config. Compile-time constant. */
         val editable: EditMode = EditMode.USER
         /** Equivalent to [name]. */
         override val configName = name
         /** Equivalent to [group]. */
         override val configGroup = group
         /** Equivalent to [info]. */
         override val configInfo = info
         /** Equivalent to [editable]. */
         override val configEditable = editable
      }
      object searchDelay: ConfigDefinition {
         /** Name of the config. Compile-time constant. */
         const val name: String = "Search delay"
         /** Description of the config. Compile-time constant. */
         const val info: String = "Maximal time delay between key strokes. Search text is reset after the delay runs out."
         /** Group of the config. Compile-time constant. */
         const val group: String = "Search"
         /** Editability of the config. Compile-time constant. */
         val editable: EditMode = EditMode.USER
         /** Equivalent to [name]. */
         override val configName = name
         /** Equivalent to [group]. */
         override val configGroup = group
         /** Equivalent to [info]. */
         override val configInfo = info
         /** Equivalent to [editable]. */
         override val configEditable = editable
      }
      object searchAutoCancel: ConfigDefinition {
         /** Name of the config. Compile-time constant. */
         const val name: String = "Search auto-cancel"
         /** Description of the config. Compile-time constant. */
         const val info: String = "Deactivates search after period of inactivity."
         /** Group of the config. Compile-time constant. */
         const val group: String = "Search"
         /** Editability of the config. Compile-time constant. */
         val editable: EditMode = EditMode.USER
         /** Equivalent to [name]. */
         override val configName = name
         /** Equivalent to [group]. */
         override val configGroup = group
         /** Equivalent to [info]. */
         override val configInfo = info
         /** Equivalent to [editable]. */
         override val configEditable = editable
      }
      object searchAutoCancelDelay: ConfigDefinition {
         /** Name of the config. Compile-time constant. */
         const val name: String = "Search auto-cancel delay"
         /** Description of the config. Compile-time constant. */
         const val info: String = "Period of inactivity after which search is automatically deactivated."
         /** Group of the config. Compile-time constant. */
         const val group: String = "Search"
         /** Editability of the config. Compile-time constant. */
         val editable: EditMode = EditMode.USER
         /** Equivalent to [name]. */
         override val configName = name
         /** Equivalent to [group]. */
         override val configGroup = group
         /** Equivalent to [info]. */
         override val configInfo = info
         /** Equivalent to [editable]. */
         override val configEditable = editable
      }
   }
   object ui {
      /** Name of the group. */
      const val name = "Ui"

      object skin: ConfigDefinition {
         /** Name of the config. Compile-time constant. */
         const val name: String = "Skin"
         /** Description of the config. Compile-time constant. */
         const val info: String = "Application skin"
         /** Group of the config. Compile-time constant. */
         const val group: String = "Ui"
         /** Editability of the config. Compile-time constant. */
         val editable: EditMode = EditMode.USER
         /** Equivalent to [name]. */
         override val configName = name
         /** Equivalent to [group]. */
         override val configGroup = group
         /** Equivalent to [info]. */
         override val configInfo = info
         /** Equivalent to [editable]. */
         override val configEditable = editable
      }
      object font: ConfigDefinition {
         /** Name of the config. Compile-time constant. */
         const val name: String = "Font"
         /** Description of the config. Compile-time constant. */
         const val info: String = "Application font"
         /** Group of the config. Compile-time constant. */
         const val group: String = "Ui"
         /** Editability of the config. Compile-time constant. */
         val editable: EditMode = EditMode.USER
         /** Equivalent to [name]. */
         override val configName = name
         /** Equivalent to [group]. */
         override val configGroup = group
         /** Equivalent to [info]. */
         override val configInfo = info
         /** Equivalent to [editable]. */
         override val configEditable = editable
      }
      object layoutModeBlurBgr: ConfigDefinition {
         /** Name of the config. Compile-time constant. */
         const val name: String = "Layout mode blur bgr"
         /** Description of the config. Compile-time constant. */
         const val info: String = "Layout mode use blur effect"
         /** Group of the config. Compile-time constant. */
         const val group: String = "Ui"
         /** Editability of the config. Compile-time constant. */
         val editable: EditMode = EditMode.USER
         /** Equivalent to [name]. */
         override val configName = name
         /** Equivalent to [group]. */
         override val configGroup = group
         /** Equivalent to [info]. */
         override val configInfo = info
         /** Equivalent to [editable]. */
         override val configEditable = editable
      }
      object layoutModeFadeBgr: ConfigDefinition {
         /** Name of the config. Compile-time constant. */
         const val name: String = "Layout mode fade bgr"
         /** Description of the config. Compile-time constant. */
         const val info: String = "Layout mode use fade effect"
         /** Group of the config. Compile-time constant. */
         const val group: String = "Ui"
         /** Editability of the config. Compile-time constant. */
         val editable: EditMode = EditMode.USER
         /** Equivalent to [name]. */
         override val configName = name
         /** Equivalent to [group]. */
         override val configGroup = group
         /** Equivalent to [info]. */
         override val configInfo = info
         /** Equivalent to [editable]. */
         override val configEditable = editable
      }
      object layoutModeFadeIntensity: ConfigDefinition {
         /** Name of the config. Compile-time constant. */
         const val name: String = "Layout mode fade intensity"
         /** Description of the config. Compile-time constant. */
         const val info: String = "Layout mode fade effect intensity."
         /** Group of the config. Compile-time constant. */
         const val group: String = "Ui"
         /** Editability of the config. Compile-time constant. */
         val editable: EditMode = EditMode.USER
         /** Equivalent to [name]. */
         override val configName = name
         /** Equivalent to [group]. */
         override val configGroup = group
         /** Equivalent to [info]. */
         override val configInfo = info
         /** Equivalent to [editable]. */
         override val configEditable = editable
      }
      object layoutModeBlurIntensity: ConfigDefinition {
         /** Name of the config. Compile-time constant. */
         const val name: String = "Layout mode blur intensity"
         /** Description of the config. Compile-time constant. */
         const val info: String = "Layout mode blur effect intensity."
         /** Group of the config. Compile-time constant. */
         const val group: String = "Ui"
         /** Editability of the config. Compile-time constant. */
         val editable: EditMode = EditMode.USER
         /** Equivalent to [name]. */
         override val configName = name
         /** Equivalent to [group]. */
         override val configGroup = group
         /** Equivalent to [info]. */
         override val configInfo = info
         /** Equivalent to [editable]. */
         override val configEditable = editable
      }
      object layoutModeAnimLength: ConfigDefinition {
         /** Name of the config. Compile-time constant. */
         const val name: String = "Layout mode anim length"
         /** Description of the config. Compile-time constant. */
         const val info: String = "Duration of layout mode transition effects."
         /** Group of the config. Compile-time constant. */
         const val group: String = "Ui"
         /** Editability of the config. Compile-time constant. */
         val editable: EditMode = EditMode.USER
         /** Equivalent to [name]. */
         override val configName = name
         /** Equivalent to [group]. */
         override val configGroup = group
         /** Equivalent to [info]. */
         override val configInfo = info
         /** Equivalent to [editable]. */
         override val configEditable = editable
      }
      object snap: ConfigDefinition {
         /** Name of the config. Compile-time constant. */
         const val name: String = "Snap"
         /** Description of the config. Compile-time constant. */
         const val info: String = "Allows snapping feature for windows and controls."
         /** Group of the config. Compile-time constant. */
         const val group: String = "Ui"
         /** Editability of the config. Compile-time constant. */
         val editable: EditMode = EditMode.USER
         /** Equivalent to [name]. */
         override val configName = name
         /** Equivalent to [group]. */
         override val configGroup = group
         /** Equivalent to [info]. */
         override val configInfo = info
         /** Equivalent to [editable]. */
         override val configEditable = editable
      }
      object snapActivationDistance: ConfigDefinition {
         /** Name of the config. Compile-time constant. */
         const val name: String = "Snap activation distance"
         /** Description of the config. Compile-time constant. */
         const val info: String = "Distance at which snap feature gets activated"
         /** Group of the config. Compile-time constant. */
         const val group: String = "Ui"
         /** Editability of the config. Compile-time constant. */
         val editable: EditMode = EditMode.USER
         /** Equivalent to [name]. */
         override val configName = name
         /** Equivalent to [group]. */
         override val configGroup = group
         /** Equivalent to [info]. */
         override val configInfo = info
         /** Equivalent to [editable]. */
         override val configEditable = editable
      }
      object lockLayout: ConfigDefinition {
         /** Name of the config. Compile-time constant. */
         const val name: String = "Lock layout"
         /** Description of the config. Compile-time constant. */
         const val info: String = "Locked layout will not enter layout mode."
         /** Group of the config. Compile-time constant. */
         const val group: String = "Ui"
         /** Editability of the config. Compile-time constant. */
         val editable: EditMode = EditMode.USER
         /** Equivalent to [name]. */
         override val configName = name
         /** Equivalent to [group]. */
         override val configGroup = group
         /** Equivalent to [info]. */
         override val configInfo = info
         /** Equivalent to [editable]. */
         override val configEditable = editable
      }
      object ratingSkin: ConfigDefinition {
         /** Name of the config. Compile-time constant. */
         const val name: String = "Rating skin"
         /** Description of the config. Compile-time constant. */
         const val info: String = "Rating ui component skin"
         /** Group of the config. Compile-time constant. */
         const val group: String = "Ui"
         /** Editability of the config. Compile-time constant. */
         val editable: EditMode = EditMode.USER
         /** Equivalent to [name]. */
         override val configName = name
         /** Equivalent to [group]. */
         override val configGroup = group
         /** Equivalent to [info]. */
         override val configInfo = info
         /** Equivalent to [editable]. */
         override val configEditable = editable
      }
      object ratingIconAmount: ConfigDefinition {
         /** Name of the config. Compile-time constant. */
         const val name: String = "Rating icon amount"
         /** Description of the config. Compile-time constant. */
         const val info: String = "Number of icons in rating control."
         /** Group of the config. Compile-time constant. */
         const val group: String = "Ui"
         /** Editability of the config. Compile-time constant. */
         val editable: EditMode = EditMode.USER
         /** Equivalent to [name]. */
         override val configName = name
         /** Equivalent to [group]. */
         override val configGroup = group
         /** Equivalent to [info]. */
         override val configInfo = info
         /** Equivalent to [editable]. */
         override val configEditable = editable
      }
      object ratingAllowPartial: ConfigDefinition {
         /** Name of the config. Compile-time constant. */
         const val name: String = "Rating allow partial"
         /** Description of the config. Compile-time constant. */
         const val info: String = "Allow partial values for rating."
         /** Group of the config. Compile-time constant. */
         const val group: String = "Ui"
         /** Editability of the config. Compile-time constant. */
         val editable: EditMode = EditMode.USER
         /** Equivalent to [name]. */
         override val configName = name
         /** Equivalent to [group]. */
         override val configGroup = group
         /** Equivalent to [info]. */
         override val configInfo = info
         /** Equivalent to [editable]. */
         override val configEditable = editable
      }
      object image {
         /** Name of the group. */
         const val name = "Image"

         object thumbnailAnimDuration: ConfigDefinition {
            /** Name of the config. Compile-time constant. */
            const val name: String = "Thumbnail anim duration"
            /** Description of the config. Compile-time constant. */
            const val info: String = "Preferred hover scale animation duration for thumbnails."
            /** Group of the config. Compile-time constant. */
            const val group: String = "Ui.Image"
            /** Editability of the config. Compile-time constant. */
            val editable: EditMode = EditMode.USER
            /** Equivalent to [name]. */
            override val configName = name
            /** Equivalent to [group]. */
            override val configGroup = group
            /** Equivalent to [info]. */
            override val configInfo = info
            /** Equivalent to [editable]. */
            override val configEditable = editable
         }
      }
      object table {
         /** Name of the group. */
         const val name = "Table"

         object tableOrientation: ConfigDefinition {
            /** Name of the config. Compile-time constant. */
            const val name: String = "Table orientation"
            /** Description of the config. Compile-time constant. */
            const val info: String = "Orientation of the table"
            /** Group of the config. Compile-time constant. */
            const val group: String = "Ui.Table"
            /** Editability of the config. Compile-time constant. */
            val editable: EditMode = EditMode.USER
            /** Equivalent to [name]. */
            override val configName = name
            /** Equivalent to [group]. */
            override val configGroup = group
            /** Equivalent to [info]. */
            override val configInfo = info
            /** Equivalent to [editable]. */
            override val configEditable = editable
         }
         object zeropadNumbers: ConfigDefinition {
            /** Name of the config. Compile-time constant. */
            const val name: String = "Zeropad numbers"
            /** Description of the config. Compile-time constant. */
            const val info: String = "Adds 0s for number length consistency"
            /** Group of the config. Compile-time constant. */
            const val group: String = "Ui.Table"
            /** Editability of the config. Compile-time constant. */
            val editable: EditMode = EditMode.USER
            /** Equivalent to [name]. */
            override val configName = name
            /** Equivalent to [group]. */
            override val configGroup = group
            /** Equivalent to [info]. */
            override val configInfo = info
            /** Equivalent to [editable]. */
            override val configEditable = editable
         }
         object searchShowOriginalIndex: ConfigDefinition {
            /** Name of the config. Compile-time constant. */
            const val name: String = "Search show original index"
            /** Description of the config. Compile-time constant. */
            const val info: String = "Show unfiltered table item index when filter applied"
            /** Group of the config. Compile-time constant. */
            const val group: String = "Ui.Table"
            /** Editability of the config. Compile-time constant. */
            val editable: EditMode = EditMode.USER
            /** Equivalent to [name]. */
            override val configName = name
            /** Equivalent to [group]. */
            override val configGroup = group
            /** Equivalent to [info]. */
            override val configInfo = info
            /** Equivalent to [editable]. */
            override val configEditable = editable
         }
         object showTableHeader: ConfigDefinition {
            /** Name of the config. Compile-time constant. */
            const val name: String = "Show table header"
            /** Description of the config. Compile-time constant. */
            const val info: String = "Show table header with columns"
            /** Group of the config. Compile-time constant. */
            const val group: String = "Ui.Table"
            /** Editability of the config. Compile-time constant. */
            val editable: EditMode = EditMode.USER
            /** Equivalent to [name]. */
            override val configName = name
            /** Equivalent to [group]. */
            override val configGroup = group
            /** Equivalent to [info]. */
            override val configInfo = info
            /** Equivalent to [editable]. */
            override val configEditable = editable
         }
         object showTableControls: ConfigDefinition {
            /** Name of the config. Compile-time constant. */
            const val name: String = "Show table controls"
            /** Description of the config. Compile-time constant. */
            const val info: String = "Show table controls at the bottom of the table. Displays menu bar and table content information."
            /** Group of the config. Compile-time constant. */
            const val group: String = "Ui.Table"
            /** Editability of the config. Compile-time constant. */
            val editable: EditMode = EditMode.USER
            /** Equivalent to [name]. */
            override val configName = name
            /** Equivalent to [group]. */
            override val configGroup = group
            /** Equivalent to [info]. */
            override val configInfo = info
            /** Equivalent to [editable]. */
            override val configEditable = editable
         }
      }
      object tabs {
         /** Name of the group. */
         const val name = "Tabs"

      }
      object view {
         /** Name of the group. */
         const val name = "View"

         object overlayArea: ConfigDefinition {
            /** Name of the config. Compile-time constant. */
            const val name: String = "Overlay area"
            /** Description of the config. Compile-time constant. */
            const val info: String = "Covered area. Screen overlay provides more space than window, but it can disrupt work flow."
            /** Group of the config. Compile-time constant. */
            const val group: String = "Ui.View"
            /** Editability of the config. Compile-time constant. */
            val editable: EditMode = EditMode.USER
            /** Equivalent to [name]. */
            override val configName = name
            /** Equivalent to [group]. */
            override val configGroup = group
            /** Equivalent to [info]. */
            override val configInfo = info
            /** Equivalent to [editable]. */
            override val configEditable = editable
         }
         object overlayBackground: ConfigDefinition {
            /** Name of the config. Compile-time constant. */
            const val name: String = "Overlay background"
            /** Description of the config. Compile-time constant. */
            const val info: String = "Background image source"
            /** Group of the config. Compile-time constant. */
            const val group: String = "Ui.View"
            /** Editability of the config. Compile-time constant. */
            val editable: EditMode = EditMode.USER
            /** Equivalent to [name]. */
            override val configName = name
            /** Equivalent to [group]. */
            override val configGroup = group
            /** Equivalent to [info]. */
            override val configInfo = info
            /** Equivalent to [editable]. */
            override val configEditable = editable
         }
         object actionViewer {
            /** Name of the group. */
            const val name = "Action Viewer"

            object closeWhenActionEnds: ConfigDefinition {
               /** Name of the config. Compile-time constant. */
               const val name: String = "Close when action ends"
               /** Description of the config. Compile-time constant. */
               const val info: String = "Closes the chooser when action finishes running."
               /** Group of the config. Compile-time constant. */
               const val group: String = "Ui.View.Action Viewer"
               /** Editability of the config. Compile-time constant. */
               val editable: EditMode = EditMode.USER
               /** Equivalent to [name]. */
               override val configName = name
               /** Equivalent to [group]. */
               override val configGroup = group
               /** Equivalent to [info]. */
               override val configInfo = info
               /** Equivalent to [editable]. */
               override val configEditable = editable
            }
         }
         object shortcutViewer {
            /** Name of the group. */
            const val name = "Shortcut Viewer"

            object hideUnassignedShortcuts: ConfigDefinition {
               /** Name of the config. Compile-time constant. */
               const val name: String = "Hide unassigned shortcuts"
               /** Description of the config. Compile-time constant. */
               const val info: String = "Displays only shortcuts that have keys assigned"
               /** Group of the config. Compile-time constant. */
               const val group: String = "Ui.View.Shortcut Viewer"
               /** Editability of the config. Compile-time constant. */
               val editable: EditMode = EditMode.USER
               /** Equivalent to [name]. */
               override val configName = name
               /** Equivalent to [group]. */
               override val configGroup = group
               /** Equivalent to [info]. */
               override val configInfo = info
               /** Equivalent to [editable]. */
               override val configEditable = editable
            }
         }
      }
      object window {
         /** Name of the group. */
         const val name = "Window"

      }
      object dock {
         /** Name of the group. */
         const val name = "Dock"

      }
   }
}
