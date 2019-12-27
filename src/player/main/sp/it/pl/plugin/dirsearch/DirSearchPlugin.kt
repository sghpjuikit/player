package sp.it.pl.plugin.dirsearch

import mu.KLogging
import sp.it.pl.gui.objects.autocomplete.ConfigSearch
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.main.APP
import sp.it.pl.main.AppSearch.Source
import sp.it.pl.main.IconFA
import sp.it.pl.main.withAppProgress
import sp.it.pl.plugin.PluginBase
import sp.it.util.async.NEW
import sp.it.util.async.runFX
import sp.it.util.async.runIO
import sp.it.util.collections.materialize
import sp.it.util.conf.Constraint.FileActor.DIRECTORY
import sp.it.util.conf.cList
import sp.it.util.conf.cr
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.only
import sp.it.util.dev.failIfFxThread
import sp.it.util.file.writeTextTry
import sp.it.util.system.browse
import java.io.File
import java.util.concurrent.atomic.AtomicLong

class DirSearchPlugin: PluginBase("Dir Search", false) {

   private val searchDirs by cList<File>().only(DIRECTORY).def(name = "Location", info = "Locations to find directories in.")
   private val searchDepth by cv(2).def(name = "Search depth")
   private val searchDo by cr { updateCache() }.def(name = "Re-index", info = "Update cache")

   private val cacheFile = getUserResource("dirs.txt")
   private val cacheUpdate = AtomicLong(0)
   private var searchSourceDirs = listOf<File>()
   private val searchSource = Source("$name plugin - Directories") { searchSourceDirs.asSequence().map { it.toOpenDirEntry() } }

   override fun onStart() {
      APP.search.sources += searchSource
      computeFiles()
   }

   override fun onStop() {
      APP.search.sources -= searchSource
   }

   private fun computeFiles() {
      runIO {
         val isCacheInvalid = !cacheFile.exists()
         if (isCacheInvalid) updateCache() else readCache()
      }
   }

   private fun readCache() {
      failIfFxThread()

      val dirs = cacheFile.useLines { it.map { File(it) }.toList() }
      runFX {
         searchSourceDirs = dirs
      }
   }

   private fun updateCache() {
      runFX { searchDirs.materialize() }
         .then(NEW) { dirs ->
            val id = cacheUpdate.getAndIncrement()
            dirs.asSequence()
               .distinct()
               .flatMap { findDirectories(it, id) }
               .toList()
               .also { writeCache(it) }
         }.ui {
            searchSourceDirs = it
         }
         .withAppProgress("$name: Searching for Directories")
   }

   private fun writeCache(files: List<File>) {
      failIfFxThread()

      val text = files.asSequence().map { it.absolutePath }.joinToString("\n")
      cacheFile.writeTextTry(text)
   }

   private fun File.toOpenDirEntry() = ConfigSearch.Entry.of(
      { "Open directory: $absolutePath" },
      { "Opens directory: $absolutePath" },
      { "Open directory: $absolutePath" },
      { Icon(IconFA.FOLDER) },
      { browse() }
   )

   private fun findDirectories(rootDir: File, id: Long) =
      rootDir.walkTopDown()
         .onEnter { file -> cacheUpdate.get()==id && file.isDirectory }
         .onFail { file, e -> logger.warn(e) { "Couldn't not properly read/access file=$file" } }
         .maxDepth(searchDepth.value)

   companion object: KLogging()

}