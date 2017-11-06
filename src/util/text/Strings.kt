package util.text

interface Strings {
    val strings: Sequence<String>

    fun anyContains(text: String): Boolean = strings.any { it.contains(text) }
}

fun Sequence<String>.toStrings() = object: Strings {
    override val strings get() = this@toStrings
}