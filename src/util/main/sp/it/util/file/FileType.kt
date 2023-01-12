package sp.it.util.file

import java.io.File
import org.jetbrains.annotations.Blocking

/** Denotes type of [java.io.File]: directory or file. */
enum class FileType {
   FILE, DIRECTORY;

   companion object {
      @Blocking
      operator fun invoke(f: File): FileType = if (f.isDirectory) DIRECTORY else FILE
   }
}