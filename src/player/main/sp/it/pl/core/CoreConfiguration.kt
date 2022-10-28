package sp.it.pl.core

import sp.it.pl.main.APP
import sp.it.util.async.runVT
import sp.it.util.conf.ConfigurationContext
import sp.it.util.file.readTextTry
import sp.it.util.functional.orNull

object CoreConfiguration {
   init {
      ConfigurationContext.toUiConverter = CoreConverter.ui
      runVT {
         ConfigurationContext.unsealedEnumeratorClasses = APP.location.resources.classes_yml.readTextTry().orNull().orEmpty().lineSequence()
            .filterNot { it.startsWith("#") || it.isBlank() }
            .sorted()
            .toCollection(LinkedHashSet())
      }
   }
}