package sp.it.pl.core

import sp.it.pl.main.APP
import sp.it.util.async.runIO
import sp.it.util.conf.ConfigurationContext
import sp.it.util.file.readTextTry
import sp.it.util.functional.orNull

object CoreConfiguration {
   init {
      runIO {
         ConfigurationContext.unsealedEnumeratorClasses = APP.location.resources.classes_yml.readTextTry().orNull().orEmpty().lineSequence()
            .filterNot { it.startsWith("#") || it.isBlank() }
            .sorted()
            .toCollection(LinkedHashSet());
      }
   }
}