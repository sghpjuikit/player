package sp.it.util.text

import java.util.Locale

/** Similar to [String.ifEmpty]  */
inline fun String.ifNotEmpty(mapper: (String) -> String) = if (isNotEmpty()) mapper(this) else this

/** Similar to [String.ifBlank]  */
inline fun String.ifNotBlank(mapper: (String) -> String) = if (isNotBlank()) mapper(this) else this

fun String.capitalLower() = lowercase().capital()

fun String.capital() = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

fun String.decapital() = replaceFirstChar { it.lowercase() }

fun String.decapitalUpper() = uppercase().replaceFirstChar { it.lowercase(Locale.getDefault()) }