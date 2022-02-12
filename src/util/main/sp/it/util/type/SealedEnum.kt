package sp.it.util.type

import kotlin.reflect.KClass
import sp.it.util.dev.failIf

open class SealedEnum<T: Any>(type: KClass<T>) {

   init {
      failIf(!this::class.isCompanion) { "Only companion object can implement sealed enum" }
      failIf(!type.isSealed) { "Only companion object of sealed type can implement sealed enum" }
   }

   val values: List<T> = type.sealedSubclasses.mapNotNull { it.objectInstance }

}