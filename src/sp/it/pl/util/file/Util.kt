@file:Suppress("unused", "NOTHING_TO_INLINE")

package sp.it.pl.util.file

import sp.it.pl.util.functional.Try
import java.io.File
import java.io.FileFilter
import java.io.FilenameFilter
import java.net.MalformedURLException
import java.net.URI
import java.util.stream.Stream
import kotlin.streams.asStream
import kotlin.streams.toList

/**
 * Returns [File.getName] or [File.toString] if this is the root directory,
 * since that returns an empty string otherwise.
 *
 * @return name of the file with extension
 */
val File.nameOrRoot: String
    get() = name.takeUnless { it.isEmpty() } ?: toString()

/**
 * Returns [File.nameWithoutExtension] or [File.toString] if this is the root directory,
 * since that returns an empty string otherwise.
 *
 * @return name of the file without extension
 */
// TODO problematic for directories containing '.'
val File.nameWithoutExtensionOrRoot: String
    get() = name.takeUnless { it.isEmpty() }?.substringAfterLast('.') ?: toString()

/** @return file itself if exists or its first existing parent or error if no parent exists */
fun File.find1stExistingParentFile(): Try<File, Void> = when {
    exists() -> Try.ok(this)
    else -> parentDir?.find1stExistingParentFile() ?: Try.error()
}

/** @return first existing directory in this file's hierarchy or error if no parent exists */
fun File.find1stExistingParentDir(): Try<File, Void> = when {
    exists() && isDirectory -> Try.ok(this)
    else -> parentDir?.find1stExistingParentDir() ?: Try.error()
}

inline fun File.childOf(childName: String) =
        File(this, childName)

inline fun File.childOf(childName: String, childName2: String) =
        childOf(childName).childOf(childName2)

inline fun File.childOf(childName: String, childName2: String, childName3: String) =
        childOf(childName, childName2).childOf(childName3)

inline fun File.childOf(vararg childNames: String) =
        childNames.fold(this, File::childOf)

inline infix fun File.isChildOf(parent: File) =
        parent.isParentOf(this)

inline fun File.isAnyChildOf(parent: File) =
        parent.isAnyParentOf(this)

inline infix fun File.isParentOf(child: File) =
        child.parentDir==this

fun File.isAnyParentOf(child: File) =
        generateSequence(child) { it.parentDir }.any { isParentOf(it) }

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
fun File.listChildren(): Stream<File> =
        listFiles()?.asSequence()?.asStream() ?: Stream.empty()

@Suppress("DEPRECATION")
fun File.listChildren(filter: FileFilter): Stream<File> =
        listFiles(filter)?.asSequence()?.asStream() ?: Stream.empty()

@Suppress("DEPRECATION")
fun File.listChildren(filter: FilenameFilter): Stream<File> =
        listFiles(filter)?.asSequence()?.asStream() ?: Stream.empty()

@Suppress("DEPRECATION")
fun File.seqChildren(): Sequence<File> =
        listFiles()?.asSequence() ?: emptySequence()

/**
 * Safe version of [File.getParentFile].
 *
 * @return parent file or null if is root
 */
@Suppress("DEPRECATION")
val File.parentDir: File?
    get() = parentFile

/** @return [File.getParentFile] or self if there is no parent */
val File.parentDirOrRoot get() = parentDir ?: this

/** @return true if the file path ends with '.' followed by the specified [suffix] */
infix fun File.hasExtension(suffix: String) = path.endsWith(".$suffix", true)

/** @return true if the file path ends with '.' followed by the one of the specified [suffixes] */
fun File.hasExtension(vararg suffixes: String) = suffixes.any { this hasExtension it }

/** @return file denoting the resource of this uri or null if [IllegalArgumentException] is thrown. */
@Suppress("DEPRECATION")
fun URI.toFileOrNull() =
        try {
            File(this)
        } catch (e: IllegalArgumentException) {
            null
        }

/** @return file denoting the resource of this uri or null if [IllegalArgumentException] is thrown. */
fun File.toURLOrNull() =
        try {
            toURI().toURL()
        } catch (e: MalformedURLException) {
            null
        }

enum class FileFlatter(@JvmField val flatten: (Collection<File>) -> Stream<File>) {
    NONE({ it.stream().distinct() }),
    DIRS({
        it.asSequence().distinct()
                .flatMap { sequenceOf(it).filter { it.isFile }+it.walk().filter { it.isDirectory } }
                .asStream()
    }),
    TOP_LVL({ it.stream().distinct().flatMap { it.listChildren() } }),
    TOP_LVL_AND_DIRS({
        it.stream().distinct()
                .flatMap { it.listChildren() }
                .flatMap { (sequenceOf(it).filter { it.isFile }+it.walk().filter { it.isDirectory }).asStream() }
    }),
    TOP_LVL_AND_DIRS_AND_WITH_COVER({

        class CCFile(f: File): File(f.path) {
            val cIsDirectory = f.isDirectory

            override fun isDirectory(): Boolean = cIsDirectory

            fun hasCover(cache: HashSet<File>): Boolean {
                val p = parentDirOrRoot
                val n = nameWithoutExtension
                return ImageFileFormat.values().asSequence()
                        .filter { it.isSupported }
                        .any { cache.contains(p.childOf("$n.$it")) }
            }
        }

        fun CCFile.walkDirsAndWithCover(cache: HashSet<File>): Sequence<CCFile> {
            val fs = listChildren().map(::CCFile).toList()
            cache += fs
            return sequenceOf(this)+fs.asSequence().flatMap { it.walkDirsAndWithCover(cache) }
        }

        fun File.walkDirsAndWithCover(): Stream<File> {
            val cache = HashSet<File>()
            return CCFile(this).walkDirsAndWithCover(cache).filter { it.isDirectory || it.hasCover(cache) }.asStream()
        }

        it.stream().distinct()
                .flatMap { it.listChildren() }
                .flatMap { it.walkDirsAndWithCover() }
    }),
    ALL({ it.stream().distinct().flatMap { it.walk().asStream().filter(File::isFile) } });
}


