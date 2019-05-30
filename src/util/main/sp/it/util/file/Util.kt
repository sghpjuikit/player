package sp.it.util.file

import mu.KotlinLogging
import sp.it.util.functional.Try
import sp.it.util.functional.and
import sp.it.util.functional.andAlso
import sp.it.util.functional.runTry
import java.io.File
import java.io.FileFilter
import java.io.FilenameFilter
import java.net.MalformedURLException
import java.net.URI
import java.nio.charset.Charset
import java.nio.file.Files

private val logger = KotlinLogging.logger { }

/**
 * @return File.getName], but partition name for root directories, never empty string
 */
val File.nameOrRoot: String
    get() = name.takeUnless { it.isEmpty() } ?: toString()

/**
 * Returns [File.nameWithoutExtension] or [File.toString] if this is the root directory,
 * since that returns an empty string otherwise.
 *
 * @return name of the file without extension
 */
// TODO: does not work for directories with '.'
val File.nameWithoutExtensionOrRoot: String get() = nameWithoutExtension.takeUnless { it.isEmpty() } ?: toString()

/** @return file itself if exists or its first existing parent or error if null or no parent exists */
fun File.find1stExistingParentFile(): Try<File, Nothing?> = when {
    exists() -> Try.ok(this)
    else -> parentDir?.find1stExistingParentFile() ?: Try.error()
}

/** @return first existing directory in this file's hierarchy or error if no parent exists */
fun File.find1stExistingParentDir(): Try<File, Nothing?> = when {
    exists() && isDirectory -> Try.ok(this)
    else -> parentDir?.find1stExistingParentDir() ?: Try.error()
}

/** Equivalent to [File.childOf]. Allows for intuitive `File(...)/"..."/"..."` notation for resolving Files. */
operator fun File.div(childName: String) = childOf(childName)

/** @return child of this file, equivalent to `File(this, childName)`. */
fun File.childOf(childName: String) = File(this, childName)

fun File.childOf(vararg childNames: String) = childNames.fold(this, File::childOf)

infix fun File.isChildOf(parent: File) = parent.isParentOf(this)

infix fun File.isAnyChildOf(parent: File) = parent.isAnyParentOf(this)

infix fun File.isParentOf(child: File) = child.parentDir==this

infix fun File.isAnyParentOf(child: File) = generateSequence(child) { it.parentDir }.any { isParentOf(it) }

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
fun File.listChildren(): Sequence<File> = listFiles()?.asSequence().orEmpty()

/** @see File.listChildren */
@Suppress("DEPRECATION")
fun File.listChildren(filter: FileFilter): Sequence<File> = listFiles(filter)?.asSequence().orEmpty()

/** @see File.listChildren */
@Suppress("DEPRECATION")
fun File.listChildren(filter: FilenameFilter): Sequence<File> = listFiles(filter)?.asSequence().orEmpty()

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

/** @return file denoting the resource of this uri or null if [IllegalArgumentException] is thrown */
@Suppress("DEPRECATION")
fun URI.toFileOrNull() =
        try {
            File(this)
        } catch (e: IllegalArgumentException) {
            null
        }

/** @return file denoting the resource of this uri or null if [MalformedURLException] is thrown */
fun File.toURLOrNull() =
        try {
            toURI().toURL()
        } catch (e: MalformedURLException) {
            null
        }

/**
 * Error-safe [File.writeText]. Error can be:
 * * [java.io.FileNotFoundException] when file is a directory or can not be created or opened
 * * [SecurityException] when security manager exists and file write is not permitted
 * * [java.io.IOException] when error occurs while writing to the file output stream
 */
@JvmOverloads
fun File.writeTextTry(text: String, charset: Charset = Charsets.UTF_8) = runTry { writeText(text, charset) }


/**
 * Error-safe [File.readText]. Error can be:
 * * [java.io.FileNotFoundException] when file is a directory or can not be read or opened
 * * [SecurityException] when security manager exists and file read is not permitted
 * * [java.io.IOException] when error occurs while reading from the file input stream
 * * [OutOfMemoryError] when file is too big
 */
@JvmOverloads
fun File.readTextTry(charset: Charset = Charsets.UTF_8) = runTry { readText(charset) }

/**
 *  Invokes the specified block that saves the file with safe temporary file saving semantics:
 *  - 1 remove temporary file if exists
 *  - 2 rename existing file to temporary file
 *  - 3 invoke block to save the file
 *  - if 3 succeeds remove the temporary file
 *  - if 3 fails rename the temporary file to original file
 *
 *  Calling this method concurrently (on the same file) will result in unpredictable behavior.
 *
 *  @return ok if all steps succeed, error otherwise
 */
fun <OK> File.writeSafely(block: (File) -> Try<OK, Throwable>): Try<OK, Throwable> {
    val f = absoluteFile
    val fTmp = f.resolveSibling("$name.tmp")

    return runTry {
        Files.deleteIfExists(fTmp.toPath())
    }.mapError {
        Exception("Safe writing of $f failed. Data was not saved as deleting the previous temporary backup file='$fTmp' failed", it)
    }.and {
        if (!f.exists() || f.renameTo(fTmp)) Try.ok()
        else Try.error(Exception("Safe writing of $f failed. Data was not saved as renaming previous file to temporary backup file=`$fTmp` failed"))
    }.andAlso {
        block(
            f
        ).mapError {
            Exception("Safe writing of $f failed. Data was not saved as ${it.message}", it)
        }.and {
            runTry {
                Files.deleteIfExists(fTmp.toPath())
            }.mapError {
                Exception("Safe writing of $f failed. Data was saved, but deleting temporary backup file '$fTmp' failed", it)
            }
        }.ifError {
            runTry {
                Files.deleteIfExists(f.toPath())
            }.ifOk {
                fTmp.renameTo(f)
            }
        }
    }
}