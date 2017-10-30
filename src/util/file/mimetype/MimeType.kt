package util.file.mimetype

/**
 * Represents a mimetype.
 *
 * See more at:
 *
 *  * http://stackoverflow.com/questions/51438/getting-a-files-mime-type-in-java
 *  * http://stackoverflow.com/questions/7904497/is-there-an-enum-with-mime-types-in-java
 *  * http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/net/MediaType.html
 *
 */
class MimeType(val name: String, vararg extensions: String) {
    val extensions: Array<out String>
    val group: String
    val extension: String?

    init {
        this.group = name.substringBefore("/")
        this.extension = extensions.firstOrNull()
        this.extensions = extensions
    }

    fun getMimeType() = name

    fun isOfType(extension: String): Boolean = extension in extensions

    override fun toString() = name

    companion object {

        val UNKNOWN = MimeType("Unknown")
    }

}