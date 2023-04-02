package sp.it.util.file

import java.io.File
import java.io.IOException
import java.nio.file.FileVisitOption
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.pathString
import org.jetbrains.annotations.Blocking
import sp.it.util.file.FileType.*
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.functional.toUnit
import sp.it.util.functional.traverse

/** @return sequence of this and all parents in bottom to top order */
fun File.traverseParents(): Sequence<File> = traverse { it.parentFile }

/**
 * @return materialized list of files visited by [java.nio.file.Files.find] or empty stream if error. This method
 * catches exceptions and closes the underlying resources.
 * @param maxDepth the maximum number of levels of directories to visit. A value of 0 means that only the starting file
 * is visited. Integer.MAX_VALUE may be used to indicate that all levels should be visited.
 */
fun File.getFilesR(maxDepth: Int = Int.MAX_VALUE, fileType: FileType?, vararg options: FileVisitOption, predicate: (Path, BasicFileAttributes) -> Boolean): List<File> {
   val p:  (Path, BasicFileAttributes) -> Boolean = when (fileType) {
      null -> predicate
      DIRECTORY -> { p, a -> a.isDirectory && predicate(p, a) }
      FILE -> { p, a -> a.isRegularFile && predicate(p, a) }
   }
   val m: (Path) -> File = when (fileType) {
      null -> { it -> it.toFile() }
      DIRECTORY -> { it -> FastFile(it.pathString, true, false) }
      FILE -> { it -> FastFile(it.pathString, false, true) }
   }
   return runTry { Files.find(toPath(), maxDepth, p, *options).use { it.map(m).toList() } }.orNull().orEmpty()
}

/** If [File.exists] calls [File.deleteOrThrow] or does nothing */
@Blocking @Throws(IOException::class)
fun File.del(): Unit = if (exists()) deleteOrThrow() else Unit

/** [File.delete] and throw if returned false. Warning - it throws when file did not exist in the first place */
@Blocking @Throws(IOException::class)
fun File.deleteOrThrow(): Unit = delete().also { if (!it) throw IOException("Failed to delete file=$this") }.toUnit()

@Blocking @Throws(IOException::class)
fun File.deleteRecursivelyOrThrow(): Boolean = deleteRecursively().also { if (!it) throw IOException("Failed to delete file=$this") }

@Blocking @Throws(IOException::class)
fun File.setExecutableOrThrow(executable: Boolean): Boolean = setExecutable(executable).also { if (!it) throw IOException("Failed to set executable=$executable for file=$this") }