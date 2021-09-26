package sp.it.util.dev

/** Avoids warning of unused for a variable or argument */
@Suppress("UNUSED_PARAMETER")
fun Any?.markUsed(reason: String = "") = Unit

/** Avoids warning of 'unreachable code' for a variable or argument */
@Suppress("UNUSED_PARAMETER")
fun Any?.markUnreachable(reason: String = "", block: () -> Any?) = Unit