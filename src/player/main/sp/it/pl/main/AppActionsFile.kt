package sp.it.pl.main

import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.time.Instant
import java.util.TreeMap
import kotlin.io.path.pathString
import sp.it.pl.ui.pane.ActionData.UiResult
import sp.it.pl.ui.pane.action
import sp.it.util.async.runFX
import sp.it.util.async.runIO
import sp.it.util.collections.setTo
import sp.it.util.conf.Constraint
import sp.it.util.conf.Constraint.FileActor.Companion
import sp.it.util.file.FileType
import sp.it.util.file.FileType.DIRECTORY
import sp.it.util.units.FileSize

/** Denotes actions for [File] */
object AppActionsFile {

   data class LargestFile(val name: String, val size: FileSize)

   val findLargestFiles = action<File>("Find largest files", "Find largest files inside the directory", IconMD.FILE_FIND) { dir ->

      fun traverseFiles(callback: (Collection<LargestFile>) -> Unit) {
         val map = mutableMapOf<String, FileSize>()
         val sortedMap = TreeMap<String, FileSize>(compareByDescending { map[it]!! })
         var count = 0
         var rootPath = dir.toPath()
         Files.walkFileTree(dir.toPath(), object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
               val fileName = rootPath.relativize(file).pathString
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
         }.root
      )
   }.apply { constraintsN += Constraint.FileActor(DIRECTORY) }

   data class LatestFile(val name: String, val creationTime: Instant)

   val findRecentFiles = action<File>("Find latest files", "Find latest files inside the directory", IconMD.FILE_FIND) { dir ->

      fun traverseFiles(callback: (Collection<LatestFile>) -> Unit) {
         val map = mutableMapOf<String, Instant>()
         val sortedMap = TreeMap<String, Instant>(compareByDescending { map[it]!! })
         var count = 0
         var rootPath = dir.toPath()
         Files.walkFileTree(dir.toPath(), object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
               val fileName = rootPath.relativize(file).pathString
               val fileCreated = attrs.creationTime().toInstant()
               map[fileName] = fileCreated
               sortedMap[fileName] = fileCreated
               count++
               if (count % 100 == 0) {
                  val data = sortedMap.entries.take(100).map { (a,b) -> LatestFile(a, b) }
                  runFX { callback(data) }
               }
               return FileVisitResult.CONTINUE
            }
            override fun visitFileFailed(file: Path, exc: IOException) = FileVisitResult.SKIP_SUBTREE
         })
         val data = sortedMap.entries.map { (a,b) -> LatestFile(a, b) }
         runFX { callback(data) }
      }

      UiResult(
         "Largest files in ${dir.absolutePath}:",
         tableViewForClass<LatestFile>() {
            runIO { traverseFiles { itemsRaw setTo it } }
         }.root
      )
   }.apply { constraintsN += Constraint.FileActor(DIRECTORY) }

}