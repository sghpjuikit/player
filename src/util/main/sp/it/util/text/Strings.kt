package sp.it.util.text

interface Strings {
    val strings: Sequence<String>

    fun anyContains(text: String, ignoreCase: Boolean = false): Boolean = strings.any { it.contains(text, ignoreCase) }

    fun isEmpty() = size()==0

    fun size() = strings.count()

}

fun Sequence<String>.toStrings() = object: Strings {
    override val strings get() = this@toStrings
}