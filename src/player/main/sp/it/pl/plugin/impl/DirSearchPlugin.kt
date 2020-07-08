package sp.it.pl.plugin.impl

import javafx.collections.FXCollections.observableArrayList
import mu.KLogging
import sp.it.pl.main.APP
import sp.it.pl.main.AppSearch.Source
import sp.it.pl.main.IconUN
import sp.it.pl.main.withAppProgress
import sp.it.pl.plugin.PluginBase
import sp.it.pl.plugin.PluginInfo
import sp.it.pl.ui.objects.autocomplete.ConfigSearch.Entry
import sp.it.util.action.IsAction
import sp.it.util.async.IO
import sp.it.util.async.runFX
import sp.it.util.async.runIO
import sp.it.util.collections.materialize
import sp.it.util.collections.setTo
import sp.it.util.conf.cList
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.min
import sp.it.util.conf.only
import sp.it.util.dev.failIfFxThread
import sp.it.util.file.FileType.DIRECTORY
import sp.it.util.file.writeTextTry
import sp.it.util.system.browse
import java.io.File
import java.util.concurrent.atomic.AtomicLong

class DirSearchPlugin: PluginBase() {

   private val searchDirs by cList<File>().only(DIRECTORY).def(name = "Location", info = "Locations to find directories in.")
   private val searchDepth by cv(2).min(1).def(name = "Search depth", info = "Max search depth used for each location")

   private val cacheFile = getUserResource("cache.txt")
   private val cacheUpdate = AtomicLong(0)
   private val searchSourceDirs = observableArrayList<File>()
   private val searchSource = Source("Directories ($name plugin)", searchSourceDirs) by { "Open directory: ${it.absolutePath}" } toSource {
      Entry.of(
         "Open directory: ${it.absolutePath}",
         IconUN(0x1f4c1),
         { "Opens directory: ${it.absolutePath}" },
         null,
         { it.browse() }
      )
   }

   override fun start() {
      APP.search.sources += searchSource
      computeFiles()
   }

   override fun stop() {
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
         searchSourceDirs setTo dirs
      }
   }

   private fun writeCache(files: List<File>) {
      failIfFxThread()

      val text = files.joinToString("\n") { it.absolutePath }
      cacheFile.writeTextTry(text)
   }

   @IsAction(
      name = "Re-index",
      info = "Updates locations' cache. The cache avoids searching applications repeatedly, but is not updated automatically."
   )
   private fun updateCache() {
      runFX { searchDirs.materialize() }
         .then(IO) { dirs ->
            val id = cacheUpdate.incrementAndGet()
            dirs.asSequence()
               .distinct()
               .flatMap { findDirectories(it, id) }
               .toList()
               .also { writeCache(it) }
         }.ui {
            searchSourceDirs setTo it
         }
         .withAppProgress("$name: Searching for Directories")
   }

   private fun findDirectories(rootDir: File, id: Long) =
      rootDir.walkTopDown()
         .onEnter { cacheUpdate.get()==id }
         .maxDepth(searchDepth.value)
         .filter { it.isDirectory }

   companion object: KLogging(), PluginInfo {
      override val name = "Dir Search"
      override val description = "Provides directory search capability to application search"
      override val isSupported = true
      override val isSingleton = true
      override val isEnabledByDefault = false
   }
}