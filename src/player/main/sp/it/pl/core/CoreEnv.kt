package sp.it.pl.core

import sp.it.pl.main.APP
import sp.it.util.system.EnvironmentContext

object CoreEnv: Core {
   override fun init() {
      EnvironmentContext.defaultChooseFileDir = APP.DIR_APP
   }
}