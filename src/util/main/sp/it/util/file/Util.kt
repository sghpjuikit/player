package sp.it.util.file

import mu.KotlinLogging
import sp.it.util.dev.Blocks
import sp.it.util.dev.fail
import sp.it.util.functional.Try
import sp.it.util.functional.and
import sp.it.util.functional.ifFalse
import sp.it.util.functional.runTry
import java.io.File
import java.io.FileFilter
import java.io.FilenameFilter
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.nio.charset.Charset
import java.nio.file.Files
import java.util.zip.ZipFile

private val logger = KotlinLogging.logger { }

/** @return [File.getName], but partition name for root directories, never empty string */
val File.nameOrRoot: String
   get() = name.takeUnless { it.isEmpty() } ?: toString()

/** @return file itself if exists or its first existing parent or error if null or no parent exists */
@Blocks
fun File.find1stExistingParentFile(): Try<File, Nothing?> = when {
   exists() -> Try.ok(this)
   else -> parentFile?.find1stExistingParentFile() ?: Try.error()
}

/** @return first existing directory in this file's hierarchy or error if no parent exists */
@Blocks
fun File.find1stExistingParentDir(): Try<File, Nothing?> = when {
   exists() && isDirectory -> Try.ok(this)
   else -> parentFile?.find1stExistingParentDir() ?: Try.error()
}

/** Equivalent to [File.child]. Allows for intuitive `File(...)/"..."/"..."` notation for resolving Files. */
operator fun File.div(childName: String) = child(childName)

/** @return child of this file, equivalent to `File(this, childName)` */
fun File.child(childName: String): File = File(this, childName)

/** @return true iff the specified file is [File.getParentFile] of this */
infix fun File.isChildOf(parent: File) = parent.isParentOf(this)

/** @return true iff the specified file is the same as or [File.getParentFile] of this */
infix fun File.isChildOrSelfOf(parent: File) = parent.isParentOrSelfOf(this)

/** @return true iff the specified file is direct or indirect parent of this */
infix fun File.isAnyChildOf(parent: File) = parent.isAnyParentOf(this)

/** @return true iff the specified file is the same as or direct or indirect parent of this */
infix fun File.isAnyChildOrSelfOf(parent: File) = parent.isAnyParentOrSelfOf(this)

/** @return true iff this is [File.getParentFile] of the specified file */
infix fun File.isParentOf(child: File) = child.parentFile==this

/** @return true iff this is the same as or [File.getParentFile] of the specified file */
infix fun File.isParentOrSelfOf(child: File) = child.parentFile==this

/** @return true iff this is direct or indirect parent of the specified file */
infix fun File.isAnyParentOf(child: File) = child.toPath().startsWith(toPath()) && this != child

/** @return true iff this is the same as or direct or indirect parent of the specified file */
infix fun File.isAnyParentOrSelfOf(child: File) = child.toPath().startsWith(toPath())

/** @return true iff this and the specified file have the same [File.getParentFile] */
infix fun File.isSiblingOf(sibling: File) = parentFile==sibling.parentFile

/**
 * Safe version of [File.listFiles]
 *
 * Normally, that method returns null if parameter is not a directory, but also when I/O
 * error occurs. For example when parameter refers to a directory on a non existent partition,
 * e.g., residing on hdd that has been disconnected temporarily.
 *
 * @return child files of the directory or empty if parameter not a directory or I/O error occurs
 */
@Blocks
fun File.children(): Sequence<File> = listFiles()?.asSequence().orEmpty()

/** @see File.children */
@Blocks
fun File.children(filter: FileFilter): Sequence<File> = listFiles(filter)?.asSequence().orEmpty()

/** @see File.children */
@Blocks
fun File.children(filter: FilenameFilter): Sequence<File> = listFiles(filter)?.asSequence().orEmpty()

/** @return [File.getParentFile] or self if there is no parent */
val File.parentDirOrRoot get() = parentFile ?: this

/** @return true if the file path ends with '.' followed by the specified [suffix] */
infix fun File.hasExtension(suffix: String) = path.endsWith(".$suffix", true)

/** @return true if the file path ends with '.' followed by the one of the specified [suffixes] */
fun File.hasExtension(vararg suffixes: String) = suffixes.any { this hasExtension it }

/** @return file denoting the resource of this URI or null if [IllegalArgumentException] is thrown */
@Suppress("DEPRECATION")
fun URI.toFileOrNull() =
   try {
      File(this)
   } catch (e: IllegalArgumentException) {
      null
   }

/** @return URI denoting this URL or null if [URISyntaxException] is thrown */
fun URL.toURIOrNull() =
   try {
      toURI()
   } catch (e: URISyntaxException) {
      null
   }

/** @return file denoting the resource of this URL or null if [URISyntaxException] or [IllegalArgumentException] is thrown */
fun URL.toFileOrNull() = toURIOrNull()?.toFileOrNull()

/** @return URL denoting the resource of this file or null if [MalformedURLException] is thrown */
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
@Blocks
@JvmOverloads
fun File.writeTextTry(text: String, charset: Charset = Charsets.UTF_8) = runTry { writeText(text, charset) }

/**
 * Error-safe writing to file. Uses [File.bufferedWriter] and [java.io.Writer.append]. Error can be:
 * * [java.io.FileNotFoundException] when file is a directory or can not be created or opened
 * * [SecurityException] when security manager exists and file write is not permitted
 * * [java.io.IOException] when error occurs while writing to the file output stream
 */
@Blocks
@JvmOverloads
fun Sequence<String>.writeToFileTry(file: File, charset: Charset = Charsets.UTF_8, bufferSize: Int = DEFAULT_BUFFER_SIZE) = runTry {
   file.bufferedWriter(charset, bufferSize).use { w -> forEach { w.append(it) } }
}

/**
 * Error-safe writing to file. Uses [File.bufferedWriter] and [java.io.Writer.appendln]. Error can be:
 * * [java.io.FileNotFoundException] when file is a directory or can not be created or opened
 * * [SecurityException] when security manager exists and file write is not permitted
 * * [java.io.IOException] when error occurs while writing to the file output stream
 */
fun Sequence<String>.writeLnToFileTry(file: File, charset: Charset = Charsets.UTF_8, bufferSize: Int = DEFAULT_BUFFER_SIZE) = runTry {
   file.bufferedWriter(charset, bufferSize).use { w -> forEach { w.appendln(it) } }
}

/**
 * Error-safe [File.readText]. Error can be:
 * * [java.io.FileNotFoundException] when file is a directory or can not be read or opened
 * * [SecurityException] when security manager exists and file read is not permitted
 * * [java.io.IOException] when error occurs while reading from the file input stream
 *
 * Note this method can throw [OutOfMemoryError] when file is too big
 */
@Blocks
@JvmOverloads
fun File.readTextTry(charset: Charset = Charsets.UTF_8) = runTry { readText(charset) }

/**
 * Invokes the specified block that saves this file (from now on TARGET file) with safe temporary file saving
 * semantics. In order to guarantee no loss of data (not even on on subsequent failures of subsequent repeat of this
 * function) it uses two temporary files WRITE for writing new data and BACKUP for storing original data.
 *
 * Steps:
 * - 1 write to WRITE file (overwrites any existing file)
 *   - 1.1 if 1 fails delete WRITE file (this can also fail, but that is inconsequential, it's mere cleanup)
 * - 2 delete existing BACKUP file (safe, TARGET file contains original data, BACKUP file is from previous invocation)
 *   - if 2 fails we still have original data in TARGET, which we haven't touched
 * - 3 rename TARGET file to BACKUP file, i.e., make backup
 *   - if 3 fails we still have original data in TARGET, which we haven't touched
 * - 4 rename WRITE file to TARGET file
 *   - 4.1 if 4 fails reverse 3 - rename BACKUP back to TARGET, on success everything is well, on fail we at least still
 *     have original data in BACKUP (although user will have to manually recover the data)
 * - 5 delete BACKUP file
 *
 * Guarantees:
 * - TARGET file never contains corrupted data (although it may not exist)
 * - BACKUP file never contains corrupted data (although it may not exist)
 * - TARGET or BACKUP exists, i.e, data is never lost
 *
 * Notable:
 * - WRITE file may contain corrupted data, but may also contain valid, more actual data than BACKUP
 * - If this functions returns Ok, every step succeeded and no temporary files are left behind
 * - If this functions returns Error, data writing still may have succeeded and may even be in the TARGET file, but
 * one of the steps 1-5 failed and there may also be temporary files left behind.
 *
 * Concurrency:
 * Calling this function concurrently (on the same file) will result in unpredictable behavior. It is highly advised
 * to use synchronization, but preferably per file locks to guarantee exclusivity of this call:
 * ```
 * file1Lock.withLock {
 *     file1.writeSafely { ... }
 * }
 * ```
 * This function is blocking and returns only after all io take place, i.e., is thread-safe in single threaded environment.
 *
 * @return ok if steps 1 and 2 and 3 and 4 and 5 succeed, error otherwise
 */
@Blocks
fun File.writeSafely(block: (File) -> Try<*, Throwable>): Try<Nothing?, Throwable> {

   fun File.tryRenameIfExists(to: File, message: () -> String) = if (!exists() || renameTo(to)) Try.ok() else Try.error(Exception(message()))

   fun File.tryDeleteIfExists(message: (Throwable) -> String) = runTry {
      Files.deleteIfExists(toPath())
   }.mapError {
      Exception(message(it), it)
   }

   val f = absoluteFile
   val fW = f.resolveSibling("$name.w.tmp")
   val fR = f.resolveSibling("$name.tmp")

   return run {
      run {
         block(fW)
      }.mapError {
         Exception("Safe writing of `$f` failed. Data was not saved to temporary file=`$fW` as ${it.message}", it)
      }.ifError {
         fW.tryDeleteIfExists { "Deleting $fW failed" }
      }
   }.and {
      fR.tryDeleteIfExists {
         "Safe writing of $f failed. Data was saved to temporary file=`$fW`, but deleting temporary backup file=`$fR` failed"
      }
   }.and {
      f.tryRenameIfExists(fR) {
         "Safe writing of $f failed. Data was saved to temporary file=`$fW`, but renaming file=`$f` to temporary backup file=`$fR` failed"
      }
   }.and {
      fW.tryRenameIfExists(f) {
         "Safe writing of $f failed. Data was saved to temporary file=`$fW`, but renaming it to file=`$f` failed"
      }.ifError {
         fR.tryRenameIfExists(f) { "Renaming file=`$fR` to file=`$f` failed" }
      }
   }.and {
      fR.tryDeleteIfExists {
         "Safe writing of $f failed. Data was saved, but deleting temporary backup file=`$fR` failed"
      }
   }.map {
      null
   }
}

/**
 * Unzips a zip file using [ZipFile] into the specified target directory, retaining the zip structure.
 * Optionally, zip entry path transformer can be used to transform the full entry paths to target-relative file paths.
 */
@Blocks
fun File.unzip(target: File, pathTransformer: (String) -> String = { it }) {
   ZipFile(this).use { zip ->
      zip.entries().asSequence().forEach { entry ->
         val fName = pathTransformer(entry.name)
         if (entry.isDirectory) {
            val f = target/fName
            if (!f.exists()) f.mkdirs().ifFalse { fail { "Failed to create directory=$f" } }
         } else {
            zip.getInputStream(entry).use { input ->
               val f = target/fName
               val fParent = f.parentDirOrRoot
               if (!fParent.exists()) fParent.mkdirs().ifFalse { fail { "Failed to create directory=$fParent" } }
               f.outputStream().use { output ->
                  input.copyTo(output)
               }
            }
         }
      }
   }
}