package sp.it.util.file

import java.io.File

class FastFile(path: String, private val isDir: Boolean, private val isFil: Boolean): File(path) {
   override fun isDirectory(): Boolean = isDir
   override fun isFile(): Boolean = isFil
}

fun File.toFast() = when {
   this is FastFile -> this
   else -> when (FileType(this)) {
      FileType.DIRECTORY -> FastFile(path, true, false)
      FileType.FILE -> FastFile(path, false, true)
   }
}

fun File.toFast(type: FileType) = when {
   this is FastFile -> this
   else -> FastFile(path, type==FileType.DIRECTORY, type==FileType.FILE)
}