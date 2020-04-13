package sp.it.util.reactive

/** Allows suppressing an action */
interface Suppressed {
   /** If false, [suppressed] will not run the block */
   val isSuppressed: Boolean

   /** Invokes the block if [isSuppressed] is false */
   fun suppressed(block: () -> Unit) {
      if (!isSuppressed)
         block()
   }

   /** @return not thread-safe suppressed with [isSuppressed] functioning as logical OR between this and the specified suppressed */
   operator fun plus(suppressed: Suppressed) = object: Suppressed {
      override val isSuppressed get() = this@Suppressed.isSuppressed || suppressed.isSuppressed
   }

   /** @return not thread-safe suppressed with [isSuppressed] functioning as logical AND between this and the specified suppressed */
   operator fun times(suppressed: Suppressed) = object: Suppressed {
      override val isSuppressed get() = this@Suppressed.isSuppressed && suppressed.isSuppressed
   }
}

/** Mutable [Suppressed]. Not thread-safe. */
class Suppressor(override var isSuppressed: Boolean = false): Suppressed {

   /** Invokes the block setting [isSuppressed] to true while it is being invoked */
   fun suppressing(block: () -> Unit) {
      if (!isSuppressed) {
         isSuppressed = true
         val r = block()
         isSuppressed = false
         return r
      }
   }

}