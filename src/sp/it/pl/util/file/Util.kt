package sp.it.pl.util.file

import sp.it.pl.util.functional.Try
import java.io.File
import java.io.FileFilter
import java.io.FilenameFilter
import java.util.stream.Stream
import kotlin.streams.asStream


/**
 * For files 'filename.extension' is returned.
 * For directories only name is returned.
 * Root directory returns 'X:\' string.
 *
 * Use instead of [File.getName] which returns empty string for root
 * directories.
 *
 * @return name of the file with suffix
 */
val File.nameOrRoot: String
    get() = name.takeUnless { it.isEmpty() } ?: toString()

/**
 * For files name with no extension is returned.
 * For directories name is returned.
 * Root directory returns 'X:\' string.
 *
 * @return name of the file without suffix
 * @throws NullPointerException if parameter null
 */
//@Suppress("DEPRECATION")
val File.nameWithoutExtensionOrRoot: String
    get() = nameWithoutExtension.takeUnless { it.isEmpty() } ?: toString()

/**
 * Find 1st existing file or existing parent.
 *
 * @returns file itself if exists or its first existing parent recursively or error if null or no parent exists.
 */
fun File.find1stExistingParentFile(): Try<File, Void> = when {
    exists() -> Try.ok(this)
    else -> parentFile?.find1stExistingParentFile() ?: Try.error()
}

/**
 * Find 1st existing parent.
 *
 * @param f nullable file
 * @returns file's first existing parent recursively or error if null or no parent exists.
 */
fun File.find1stExistingParentDir(): Try<File, Void> = when {
    exists() && isDirectory -> Try.ok(this)
    else -> parentFile?.find1stExistingParentDir() ?: Try.error()
}

fun File.childOf(childName: String) = File(this, childName)

fun File.childOf(childName: String, childName2: String) = childOf(childName).childOf(childName2)

fun File.childOf(childName: String, childName2: String, childName3: String) = childOf(childName, childName2).childOf(childName3)

fun File.childOf(vararg childNames: String) = childNames.fold(this, File::childOf)


/**
 * Safe version of [File.listFiles]
 *
 * Normally, the method in File returns null if parameter is not a directory, but also when I/O
 * error occurs. For example when parameter refers to a directory on a non existent partition,
 * e.g., residing on hdd that has been disconnected temporarily. Returning null instead of
 * collection is never a good idea anyway!
 *
 * @return child files of the directory or empty if parameter null, not a directory or I/O error occurs
 */
@Suppress("DEPRECATION")
fun File.listChildren(): Stream<File> = this.listFiles()?.asSequence()?.asStream() ?: Stream.empty()

@Suppress("DEPRECATION")
fun File.listChildren(filter: FileFilter): Stream<File> = this.listFiles(filter)?.asSequence()?.asStream() ?: Stream.empty()

@Suppress("DEPRECATION")
fun File.listChildren(filter: FilenameFilter): Stream<File> = this.listFiles(filter)?.asSequence()?.asStream() ?: Stream.empty()


enum class FileFlatter(@JvmField val flatten: (Collection<File>) -> Stream<File>) {
    NONE({ it.stream().distinct() }),
    FLATTEN_TOP_LVL({ it.stream().distinct().flatMap { it.listChildren() } }),
    FLATTEN_ALL({ it.stream().distinct().flatMap { it.walk().asStream().filter(File::isFile) } })
}