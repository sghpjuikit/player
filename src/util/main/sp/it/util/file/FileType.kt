package sp.it.util.file

import sp.it.util.dev.Blocks
import java.io.File

/** Denotes type of [java.io.File]: directory or file. */
enum class FileType {
   FILE, DIRECTORY;

   companion object {
      @Blocks
      operator fun invoke(f: File): FileType = if (f.isDirectory) DIRECTORY else FILE
   }
}