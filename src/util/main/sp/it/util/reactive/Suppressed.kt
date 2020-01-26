package sp.it.util.reactive

interface Suppressed {
   val isSuppressed: Boolean

   fun suppressed(block: () -> Unit) {
      if (!isSuppressed)
         block()
   }

   operator fun plus(suppressed: Suppressed) = object: Suppressed {
      override val isSuppressed = this@Suppressed.isSuppressed || suppressed.isSuppressed
   }
}

class Suppressor(suppressed: Boolean = false): Suppressed {
   @Volatile override var isSuppressed: Boolean = suppressed

   fun suppressing(block: () -> Unit) {
      if (!isSuppressed) {
         isSuppressed = true
         val r = block()
         isSuppressed = false
         return r
      }
   }

}