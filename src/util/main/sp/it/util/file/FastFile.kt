package sp.it.util.file

import java.io.File

class FastFile(path: String, private val isDir: Boolean, private val isFil: Boolean): File(path) {
   override fun isDirectory(): Boolean = isDir
   override fun isFile(): Boolean = isFil
}

fun File.toFast(type: FileType) = FastFile(path, type==FileType.DIRECTORY, type==FileType.FILE)
