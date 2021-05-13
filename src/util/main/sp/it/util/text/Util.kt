package sp.it.util.text

import java.util.Locale

fun String.capitalLower() = lowercase().capital()

fun String.capital() = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

fun String.decapital() = replaceFirstChar { it.lowercase() }

fun String.decapitalUpper() = uppercase().replaceFirstChar { it.lowercase(Locale.getDefault()) }