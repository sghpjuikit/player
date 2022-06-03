package sp.it.util.text

/** Appends sentence. Makes sure both previous and specified sentences end with a '.'. */
fun StringBuilder.appendSent(sentence: String) {
   when {
      endsWith(". ") -> append(sentence)
      endsWith(".") -> append(" ").append(sentence)
      isBlank() -> append(sentence)
      else -> append(". ").append(sentence)
   }

   if (!sentence.endsWith("."))
      append(".")
}