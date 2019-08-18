package sp.it.util

data class Named(val name: String, val value: String)

infix fun String.named(value: String) = Named(this, value)