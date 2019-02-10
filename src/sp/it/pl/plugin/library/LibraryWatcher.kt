package sp.it.pl.plugin.library

import mu.KLogging
import org.reactfx.Subscription
import sp.it.pl.audio.SimpleItem
import sp.it.pl.audio.tagging.MetadataReader
import sp.it.pl.main.APP
import sp.it.pl.main.showAppProgress
import sp.it.pl.plugin.PluginBase
import sp.it.pl.service.notif.Notifier
import sp.it.pl.util.action.IsAction
import sp.it.pl.util.async.executor.EventReducer
import sp.it.pl.util.async.runFX
import sp.it.pl.util.async.runNew
import sp.it.pl.util.collections.materialize
import sp.it.pl.util.conf.EditMode.NONE
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.conf.c
import sp.it.pl.util.conf.cList
import sp.it.pl.util.conf.cv
import sp.it.pl.util.conf.only
import sp.it.pl.util.conf.readOnlyUnless
import sp.it.pl.util.file.AudioFileFormat
import sp.it.pl.util.file.FileMonitor
import sp.it.pl.util.file.FileMonitor.monitorDirectory
import sp.it.pl.util.file.Util
import sp.it.pl.util.file.isAnyChildOf
import sp.it.pl.util.reactive.Subscribed
import sp.it.pl.util.reactive.onItemAdded
import sp.it.pl.util.reactive.onItemRemoved
import sp.it.pl.util.reactive.sync
import sp.it.pl.util.system.Os
import sp.it.pl.util.text.plural
import sp.it.pl.util.validation.Constraint.FileActor.DIRECTORY
import java.io.File
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import kotlin.streams.toList

class LibraryWatcher: PluginBase("Song Library", false) {

    @IsConfig(name = "Location", info = "Locations to find audio.")
    private val sourceDirs by cList<File>().only(DIRECTORY)
    private val sourceDirsChangeHandler = Subscribed {
        sourceDirs.forEach { handleLocationAdded(it) }
        Subscription.multi(
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
            Subscription.multi(
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
            MetadataReader.buildAddItemsToLibTask().apply(toAdd.map { SimpleItem(it) })
            APP.db.removeItems(toRem.map { SimpleItem(it) })
        }.showAppProgress("Updating song library from detected changes")
    }

    @IsAction(name = "Update", desc = "Remove non-existent songs and add new songs from location")
    private fun updateLibrary() {
        runNew {
            val items = Util.getFilesAudio(sourceDirs, AudioFileFormat.Use.APP, Integer.MAX_VALUE).map { SimpleItem(it) }.toList()
            MetadataReader.buildAddItemsToLibTask().apply(items)
            MetadataReader.buildRemoveMissingFromLibTask().run()
        }.showAppProgress("Updating song library from disk")
    }

    companion object: KLogging() {
        private infix fun Int.of(word: String) = "${this} ${word.plural(this)}"
    }

}