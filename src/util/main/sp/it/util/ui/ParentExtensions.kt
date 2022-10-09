package sp.it.util.ui

import javafx.scene.Parent

/** Adds the specified stylesheets to [Parent.stylesheets] of this parent, if it has not yet been assigned. */
fun Parent.stylesheetAdd(stylesheet: String) {
   if (stylesheet !in this.stylesheets)
      this.stylesheets += stylesheet
}

/** Adds (true) or removes (false) the specified stylesheets using [Parent.stylesheetAdd] and [Parent.stylesheetRemove]. */
fun Parent.stylesheetToggle(stylesheet: String, enabled: Boolean) {
   if (enabled) stylesheetAdd(stylesheet)
   else stylesheetRemove(stylesheet)
}

/** Removes all instances of the specified stylesheets from [Parent.stylesheets] of this parent. */
fun Parent.stylesheetRemove(stylesheet: String) {
   this.stylesheets.removeIf { it==stylesheet }
}