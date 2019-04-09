package sp.it.pl.plugin.library

import mu.KLogging
import sp.it.pl.audio.SimpleSong
import sp.it.pl.audio.tagging.addSongsToLibTask
import sp.it.pl.audio.tagging.removeMissingSongsFromLibTask
import sp.it.pl.main.APP
import sp.it.pl.main.findAudio
import sp.it.pl.main.showAppProgress
import sp.it.pl.plugin.PluginBase
import sp.it.pl.service.notif.Notifier
import sp.it.util.action.IsAction
import sp.it.util.async.executor.EventReducer
import sp.it.util.async.future.runGet
import sp.it.util.async.runFX
import sp.it.util.async.runNew
import sp.it.util.collections.materialize
import sp.it.util.conf.EditMode.NONE
import sp.it.util.conf.IsConfig
import sp.it.util.conf.c
import sp.it.util.conf.cList
import sp.it.util.conf.cv
import sp.it.util.conf.only
import sp.it.util.conf.readOnlyUnless
import sp.it.util.file.FileMonitor
import sp.it.util.file.FileMonitor.monitorDirectory
import sp.it.util.file.isAnyChildOf
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

    @IsConfig(name = "Location", info = "Locations to find audio.")
    private val sourceDirs by cList<File>().only(DIRECTORY)
    private val sourceDirsChangeHandler = Subscribed {
        sourceDirs.forEach { handleLocationAdded(it) }
        Subscription(
                sourceDirs.onItemAdded { handleLocationAdded(it) },
                sourceDirs.onItemRemoved { handleLocationRemoved(it) }
        )
    }

    @IsConfig(name = "Update on start", info = "Update library when this plugin starts")
    val updateOnStart by cv(false)

    @IsConfig(name = "Monitoring supported", info = "On some system, this file monitoring may be unsupported", editable = NONE)
    val dirMonitoringSupported by c(Os.WINDOWS.isCurrent)

    @IsConfig(name = "Monitor files", info = "Monitors files recursively, notify of changes and update library automatically")
    val dirMonitoringEnabled by cv(false).readOnlyUnless(dirMonitoringSupported)

    private val dirMonitors = HashMap<File, FileMonitor>()
    private val dirMonitoring = when {
        dirMonitoringSupported -> Subscribed {
            Subscription(
                    dirMonitoringEnabled sync { sourceDirsChangeHandler.subscribe(it) },
                    Subscription { sourceDirsChangeHandler.subscribe(false) }
            )
        }
        else -> Subscribed { Subscription { } }
    }
    private val toBeAdded = HashSet<File>()
    private val toBeRemoved = HashSet<File>()
    private val update = EventReducer.toLast<Void>(2000.0) { updateLibraryFromEvents() }

    override fun onStart() {
        dirMonitoring.subscribe(true)
        if (updateOnStart.value) updateLibrary()
    }

    override fun onStop() {
        dirMonitoring.subscribe(false)
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
                    ENTRY_CREATE -> runFX {
                        toBeAdded += file
                        update.push(null)
                    }
                    ENTRY_DELETE -> runFX {
                        toBeRemoved += file
                        update.push(null)
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

        APP.services.use<Notifier> {
            it.showTextNotification(
                    "Some song files in library changed"+
                            "\n\tAdded: ${toAdd.size of "file"}"+
                            "\n\tRemoved: ${toRem.size of "file"}",
                    "Library file change"
            )
        }

        runNew {
            addSongsToLibTask(toAdd.map { SimpleSong(it) }).runGet()
            APP.db.removeSongs(toRem.map { SimpleSong(it) })
        }.showAppProgress("Updating song library from detected changes")
    }

    @IsAction(name = "Update", desc = "Remove non-existent songs and add new songs from location")
    private fun updateLibrary() {
        val dirs = sourceDirs.materialize()
        runNew {
            val songs = findAudio(dirs).map { SimpleSong(it) }.toList()
            addSongsToLibTask(songs).runGet()
            removeMissingSongsFromLibTask().run()
        }.showAppProgress("Updating song library from disk")
    }

    companion object: KLogging() {
        private infix fun Int.of(word: String) = "${this} ${word.plural(this)}"
    }

}