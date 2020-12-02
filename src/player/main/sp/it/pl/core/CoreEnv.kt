package sp.it.pl.core

import sp.it.pl.main.APP
import sp.it.pl.main.showFloating
import sp.it.pl.main.textColon
import sp.it.pl.main.toUi
import sp.it.util.async.runFX
import sp.it.util.functional.traverse
import sp.it.util.system.EnvironmentContext
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.stackPane
import sp.it.util.ui.vBox

object CoreEnv: Core {
   override fun init() {
      EnvironmentContext.defaultChooseFileDir = APP.location
      EnvironmentContext.onNonExistentFileBrowse = { f ->
         val existingParent = f.traverse { it.parentFile }.filter { it.exists() }.firstOrNull()
         runFX {
            showFloating("Browsing not possible") {
               vBox {
                  lay += stackPane {
                     lay += label("File or directory does not exist.") {
                        styleClass += "h4p-bottom"
                     }
                  }
                  lay += textColon("File", f.toUi())
                  lay += textColon("First parent that exists", existingParent)
               }
            }
         }
      }
   }
}