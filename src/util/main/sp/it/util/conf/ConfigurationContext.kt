package sp.it.util.conf

import sp.it.util.dev.ThreadSafe
import sp.it.util.type.atomic

object ConfigurationContext {

   /**
    * Customizable [UnsealedEnumerator] for [Config]s of type [Class] and [kotlin.reflect.KClass].
    * Ideally, classes would be discovered automatically, but due to the nature of JVM, this is rather difficult and left as application responsibility.
    * Default empty set.
    */
   @ThreadSafe
   var unsealedEnumeratorClasses by atomic<Set<String>>(setOf())

}