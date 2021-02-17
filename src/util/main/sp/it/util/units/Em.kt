package sp.it.util.units

/** Font-relative size Unit. 1EM == 12.0. */
object EM {
   fun size() = 12.0
}

/** @return value in [EM] units, equivalent to `this/EM.size()` */
fun Number.toEM() = toDouble()/EM.size()

/** Returns value of this number of [EM]s, equivalent to `this*EM.size()` */
val Number.em: Double get() = toDouble()*EM.size()