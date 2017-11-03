package util.text

/** @return plural of this word if count is more than 1 or this word otherwise */
fun String.plural(count: Int = 2) = org.atteo.evo.inflector.English.plural(this, count)