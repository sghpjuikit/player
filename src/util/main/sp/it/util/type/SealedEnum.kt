package sp.it.util.type

import sp.it.util.dev.failIf
import kotlin.reflect.KClass

class SealedEnum<T: Any>(type: KClass<T>) {

   init {
      failIf(!this::class.isCompanion) { "Only companion object can implement sealed enum" }
      failIf(!type.isSealed) { "Only companion object of sealed type can implement sealed enum" }
   }

   val values: List<T> = type.sealedSubclasses.mapNotNull { it.objectInstance }

}