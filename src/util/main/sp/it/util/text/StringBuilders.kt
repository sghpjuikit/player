package sp.it.util.text

/** @return string of provided sentences separated by newline. Identical to `sentences.joinToString("\n")`. */
fun buildStringSentences(vararg sentences: String) = sentences.joinToString("\n")