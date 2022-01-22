package sp.it.util.reactive

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.AT_MOST_ONCE
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract

/** Allows suppressing an action */
interface Suppressed {
   /** If false, [suppressed] will not run the block */
   val isSuppressed: Boolean

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
class Suppressor(override var isSuppressed: Boolean = false): Suppressed

/** Invokes the block if [Suppressor.isSuppressed] is false */
@OptIn(ExperimentalContracts::class)
inline fun Suppressed.suppressed(block: () -> Unit) {
   contract {
      callsInPlace(block, AT_MOST_ONCE)
   }

   if (!isSuppressed)
      block()
}

/** Invokes the block, setting [Suppressor.isSuppressed] to true while it is being invoked */
@OptIn(ExperimentalContracts::class)
inline fun <R> Suppressor.suppressingAlways(block: () -> R): R {
   contract {
      callsInPlace(block, EXACTLY_ONCE)
   }

   isSuppressed = true
   val r = block()
   isSuppressed = false
   return r
}

/** Invokes the block if [Suppressor.isSuppressed] is false, setting [Suppressor.isSuppressed] to true while it is being invoked */
@OptIn(ExperimentalContracts::class)
inline fun Suppressor.suppressing(block: () -> Unit) {
   contract {
      callsInPlace(block, AT_MOST_ONCE)
   }

   if (!isSuppressed)
      suppressingAlways { block() }
}