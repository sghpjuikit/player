package sp.it.util

import java.util.Optional
import sp.it.util.functional.Option
import sp.it.util.functional.orNull

data class Named(val name: String, val value: Any?)

infix fun String.named(value: Any?) = Named(this, value)

infix fun String.namedOrNaIfNull(value: Any?) = Named(this, value ?: Na)

infix fun String.namedOrNaIfEmpty(value: Option<Any?>) = Named(this, value.orNull() ?: Na)

infix fun String.namedOrNaIfEmpty(value: Optional<out Any?>) = Named(this, value.orNull() ?: Na)

object Na