package sp.it.pl.plugin.library

import mu.KLogging
import sp.it.pl.audio.SimpleSong
import sp.it.pl.audio.Song
import sp.it.pl.audio.tagging.addToLibTask
import sp.it.pl.audio.tagging.removeMissingFromLibTask
import sp.it.pl.main.APP
import sp.it.pl.main.findAudio
import sp.it.pl.main.withAppProgress
import sp.it.pl.plugin.PluginBase
import sp.it.pl.plugin.notif.Notifier
import sp.it.util.action.IsAction
import sp.it.util.async.executor.EventReducer
import sp.it.util.async.future.runGet
import sp.it.util.async.runIO
import sp.it.util.collections.materialize
import sp.it.util.conf.EditMode.NONE
import sp.it.util.conf.IsConfig
import sp.it.util.conf.c
import sp.it.util.conf.cList
import sp.it.util.conf.cv
import sp.it.util.conf.only
import sp.it.util.conf.readOnlyUnless
import sp.it.util.file.FileMonitor
import sp.it.util.file.FileMonitor.Companion.monitorDirectory
import sp.it.util.file.isAnyChildOf
import sp.it.util.functional.invoke
import sp.it.util.reactive.Subscribed
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.onItemAdded
import sp.it.util.reactive.onItemRemoved
import sp.it.util.reactive.sync
import sp.it.util.system.Os
import sp.it.util.text.plural
import sp.it.util.validation.Constraint.FileActor.DIRECTORY
import java.io.File
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE

class LibraryWatcher: PluginBase("Song Library", false) {

   @IsConfig(
      name = "Location",
      info = "Locations to find audio. Directories will be scanned recursively to unlimited depth."
   )
   private val sourceDirs by cList<File>().only(DIRECTORY)
   private val sourceDirsChangeHandler = Subscribed {
      sourceDirs.forEach { handleLocationAdded(it) }
      Subscription(
         sourceDirs.onItemAdded { handleLocationAdded(it) },
         sourceDirs.onItemRemoved { handleLocationRemoved(it) }
      )
   }

   @IsConfig(
      name = "Update on start",
      info = "Update entire library from disc when this plugin starts (which also happens when application starts). " +
         "May incur significant performance cost on the system"
   )
   val updateOnStart by cv(false)

   @IsConfig(name = "Monitoring supported", info = "On some system, this file monitoring may be unsupported", editable = NONE)
   val dirMonitoringSupported by c(Os.WINDOWS.isCurrent)

   @IsConfig(
      name = "Monitor files",
      info = "Monitor all locations recursively and automatically add/remove added/removed songs to/from library."
   )
   val dirMonitoringEnabled by cv(false).readOnlyUnless(dirMonitoringSupported)

   private val dirMonitors = HashMap<File, FileMonitor>()
   private val dirMonitoring = Subscribed {
      when {
         dirMonitoringSupported -> Subscription(
            dirMonitoringEnabled sync { sourceDirsChangeHandler.subscribe(it) },
            Subscription { sourceDirsChangeHandler.unsubscribe() }
         )
         else -> Subscription()
      }
   }
   private val toBeAdded = HashSet<File>()
   private val toBeRemoved = HashSet<File>()
   private val update = EventReducer.toLast<Void>(2000.0) { updateLibraryFromEvents() }

   override fun onStart() {
      dirMonitoring.subscribe()
      if (updateOnStart.value) updateLibrary()
   }

   override fun onStop() {
      dirMonitoring.unsubscribe()
      dirMonitors.values.forEach { it.stop() }
      dirMonitors.clear()
      updateLibraryFromEvents()
   }

   private fun handleLocationRemoved(dir: File) {
      if (!dirMonitoringSupported && !dirMonitoringEnabled.value) return

      val wasDuplicate = dir in sourceDirs
      if (!wasDuplicate) {
         dirMonitors.remove(dir)?.stop()
         sourceDirs.forEach { handleLocationAdded(it) }  // starts monitoring previously shadowed directories
      }
   }

   private fun handleLocationAdded(dir: File) {
      if (!dirMonitoringSupported && !dirMonitoringEnabled.value) return

      val isDuplicate = dir in dirMonitors.keys
      val isShadowed = dirMonitors.keys.any { monitoredDir -> dir isAnyChildOf monitoredDir }
      val needsMonitoring = !isDuplicate && !isShadowed
      if (needsMonitoring) {
         dirMonitors[dir] = monitorDirectory(dir, true) { type, file ->
            when (type) {
               ENTRY_CREATE -> {
                  toBeRemoved -= file
                  toBeAdded += file
                  update()
               }
               ENTRY_DELETE -> {
                  toBeAdded -= file
                  toBeRemoved += file
                  update()
               }
            }
         }
      }
   }

   private fun updateLibraryFromEvents() {
      val toAdd = toBeAdded.materialize()
      val toRem = toBeRemoved.materialize()
      toBeAdded.clear()
      toBeRemoved.clear()

      APP.plugins.use<Notifier> {
         it.showTextNotification(
            "Library file change",
            "Some song files in library changed\n\tAdded: ${toAdd.size of "file"}\n\tRemoved: ${toRem.size of "file"}"
         )
      }

      runIO {
         Song.addToLibTask(toAdd.map { SimpleSong(it) }).runGet()
         APP.db.removeSongs(toRem.map { SimpleSong(it) })
      }.withAppProgress("Updating song library from detected changes")
   }

   @IsAction(name = "Update", desc = "Remove non-existent songs and add new songs from location")
   fun updateLibrary() {
      val dirs = sourceDirs.materialize()
      runIO {
         val songs = findAudio(dirs).map { SimpleSong(it) }.toList()
         Song.addToLibTask(songs).runGet()
         Song.removeMissingFromLibTask().run()
      }.withAppProgress("Updating song library from disk")
   }

   companion object: KLogging() {
      private infix fun Int.of(word: String) = "${this} ${word.plural(this)}"
   }

}