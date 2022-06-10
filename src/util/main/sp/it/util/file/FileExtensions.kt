package sp.it.util.file

import java.io.File
import java.nio.file.FileVisitOption
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.pathString
import sp.it.util.file.FileType.*
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry

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