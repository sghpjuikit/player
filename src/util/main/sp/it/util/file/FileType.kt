package sp.it.util.file

import java.io.File
import sp.it.util.dev.Blocks

/** Denotes type of [java.io.File]: directory or file. */
enum class FileType {
   FILE, DIRECTORY;

   companion object {
      @Blocks
      operator fun invoke(f: File): FileType = if (f.isDirectory) DIRECTORY else FILE
   }
}