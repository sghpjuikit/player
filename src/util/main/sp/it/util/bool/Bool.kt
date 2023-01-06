package sp.it.util.bool

import sp.it.util.dev.fail

typealias Bool = Boolean

/** 2-state [Boolean], one of [TRUE], [FALSE] */
sealed interface Bool2 {
   companion object {
      operator fun invoke(bool: Bool): Bool3 = when (bool) {
         true -> TRUE
         false -> FALSE
      }
   }
}

/** 3-state [Boolean], one of [TRUE], [FALSE], [UNKNOWN] */
sealed interface Bool3 {
   companion object {
      operator fun invoke(bool: Bool): Bool3 = when (bool) {
         true -> TRUE
         false -> FALSE
      }
      operator fun invoke(bool: Bool?): Bool3 = when (bool) {
         true -> TRUE
         false -> FALSE
         null -> UNKNOWN
      }
   }
}

/** N-state [Boolean], one of [TRUE], [FALSE], [BoolLValue] */
sealed interface BoolL {
   companion object {
      operator fun invoke(bool: Bool): Bool3 = when (bool) {
         true -> TRUE
         false -> FALSE
      }

      operator fun invoke(bool: Double): BoolL = when {
         bool == 1.0 -> TRUE
         bool == 0.0 -> FALSE
         bool in 0.0..1.0 -> BoolLValue(bool)
         else -> fail { "Not a valid BoolL value=$bool" }
      }
   }
}

/** Represents `true` */
object TRUE: Bool2, Bool3, BoolL

/** Represents `false` */
object FALSE: Bool2, Bool3, BoolL

/** Represents possible `true` or `false` */
object UNKNOWN: Bool3

private data class BoolLValue(val value: Double): BoolL