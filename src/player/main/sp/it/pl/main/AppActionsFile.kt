package sp.it.pl.main

import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.TreeMap
import sp.it.pl.ui.pane.ActionData.UiResult
import sp.it.pl.ui.pane.action
import sp.it.util.async.runFX
import sp.it.util.async.runIO
import sp.it.util.collections.setTo
import sp.it.util.units.FileSize

/** Denotes actions for [File] */
object AppActionsFile {

   val findLargestFiles = action<File>("Find largest files", "Find largest files inside the directory", IconMD.FILE_FIND) { dir ->

      data class LargestFile(val name: String, val size: FileSize)

      fun traverseFiles(callback: (Collection<LargestFile>) -> Unit) {
         val map = mutableMapOf<String, FileSize>()
         val sortedMap = TreeMap<String, FileSize>(compareByDescending { map[it]!! })
         var count = 0
         Files.walkFileTree(dir.toPath(), object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
               val fileName = file.fileName.toString()
               val fileSize = FileSize(attrs.size())
               map[fileName] = fileSize
               sortedMap[fileName] = fileSize
               count++
               if (count % 100 == 0) {
                  val data = sortedMap.entries.take(100).map { (a,b) -> LargestFile(a, b) }
                  runFX { callback(data) }
               }
               return FileVisitResult.CONTINUE
            }
            override fun visitFileFailed(file: Path, exc: IOException) = FileVisitResult.SKIP_SUBTREE
         })
         val data = sortedMap.entries.map { (a,b) -> LargestFile(a, b) }
         runFX { callback(data) }
      }

      UiResult(
         "Largest files in ${dir.absolutePath}:",
         tableViewForClass<LargestFile>() {
            runIO { traverseFiles { itemsRaw setTo it } }
         }
      )
   }


}