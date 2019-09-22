@file:Suppress("RemoveRedundantBackticks", "unused", "ClassName", "ObjectPropertyName", "SpellCheckingInspection")

package sp.it.pl.main

import sp.it.util.conf.EditMode

interface ConfigDefinition {
   /** Name of the config. */
   val configName: String
   /** Group of the config. */
   val configGroup: String
   /** Description of the config. */
   val configInfo: String
   /** Editability of the config. */
   val configEditable: EditMode
}

/** Application settings hierarchy. */
object `AppSettings` {

   object `General` {
      /** Name of the group. */
      const val name = "General"

      object `Close app`: ConfigDefinition {
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
      object `Developer mode`: ConfigDefinition {
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
      object `Manage VM options`: ConfigDefinition {
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
   object `Logging` {
      /** Name of the group. */
      const val name = "Logging"

   }
   object `Ui` {
      /** Name of the group. */
      const val name = "Ui"

      object `View` {
         /** Name of the group. */
         const val name = "View"

         object `Overlay area`: ConfigDefinition {
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
         object `Overlay background`: ConfigDefinition {
            /** Name of the config. Compile-time constant. */
            const val name: String = "Overlay background"
            /** Description of the config. Compile-time constant. */
            const val info: String = "Background image source."
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
         object `Action Viewer` {
            /** Name of the group. */
            const val name = "Action Viewer"

         }
         object `Shortcut Viewer` {
            /** Name of the group. */
            const val name = "Shortcut Viewer"

         }
      }
   }
}
