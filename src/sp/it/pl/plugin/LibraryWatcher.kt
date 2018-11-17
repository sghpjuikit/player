package sp.it.pl.plugin

import mu.KLogging
import org.reactfx.Subscription
import sp.it.pl.audio.SimpleItem
import sp.it.pl.audio.tagging.MetadataReader
import sp.it.pl.main.APP
import sp.it.pl.service.notif.Notifier
import sp.it.pl.util.async.executor.EventReducer
import sp.it.pl.util.async.runFX
import sp.it.pl.util.async.runNew
import sp.it.pl.util.collections.materialize
import sp.it.pl.util.conf.EditMode
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.conf.c
import sp.it.pl.util.conf.cList
import sp.it.pl.util.conf.cr
import sp.it.pl.util.conf.cv
import sp.it.pl.util.conf.only
import sp.it.pl.util.conf.readOnlyUnless
import sp.it.pl.util.file.FileMonitor
import sp.it.pl.util.file.isChildOf
import sp.it.pl.util.reactive.Subscribed
import sp.it.pl.util.reactive.onItemAdded
import sp.it.pl.util.reactive.onItemRemoved
import sp.it.pl.util.reactive.sync
import sp.it.pl.util.system.Os
import sp.it.pl.util.text.plural
import sp.it.pl.util.validation.Constraint
import java.io.File
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.util.concurrent.atomic.AtomicLong

class LibraryWatcher: PluginBase("Library Watcher", false) {

    @IsConfig(name = "Location", info = "Locations to find audio.")
    private val sourceDirs by cList<File>().only(Constraint.FileActor.DIRECTORY)
    private val sourceDirsChangeHandler = Subscribed {
        sourceDirs.forEach { handleLocationAdded(it) }
        Subscription.multi(
                sourceDirs.onItemAdded { handleLocationAdded(it) },
                sourceDirs.onItemRemoved { handleLocationRemoved(it) }
        )
    }

    @IsConfig(name = "Directory monitoring supported", info = "On some system, this feature may be unsupported", editable = EditMode.NONE)
    val fileMonitoringSupported by c(Os.WINDOWS.isCurrent)

    @IsConfig(name = "Directory monitoring", info = "Monitors content, notifies of changes and updates library automatically")
    val fileMonitoringEnabled by cv(true).readOnlyUnless { fileMonitoringSupported }

    private val fileMonitors = HashMap<File, FileMonitor>()
    private val fileMonitoring = when {
        fileMonitoringSupported -> Subscribed {
            Subscription.multi(
                    fileMonitoringEnabled sync { sourceDirsChangeHandler.subscribe(it) },
                    Subscription { sourceDirsChangeHandler.subscribe(false) }
            )
        }
        else -> Subscribed { Subscription { } }
    }
    private val toBeAdded = HashSet<File>()
    private val toBeRemoved = HashSet<File>()
    private val update = EventReducer.toLast<Void>(2000.0) { it -> updateLibraryFromEvents() }

    @IsConfig(name = "Open help", info = "Open technical usage help")
    private val updateLibrary by cr { updateLibrary() }
    private val updateLibraryId = AtomicLong(0)

    override fun onStart() {
        fileMonitoring.subscribe(true)
    }

    override fun onStop() {
        fileMonitoring.subscribe(false)
        fileMonitors.values.forEach { it.stop() }
        fileMonitors.clear()
        updateLibraryFromEvents()
    }

    private fun handleLocationRemoved(dir: File) {
        if (!fileMonitoringSupported && !fileMonitoringEnabled.value) return

        val wasDuplicate = dir in sourceDirs
        if (!wasDuplicate) {
            fileMonitors.remove(dir)?.stop()
            sourceDirs.forEach { handleLocationAdded(it) }  // starts monitoring previously shadowed directories
        }
    }

    private fun handleLocationAdded(dir: File) {
        if (!fileMonitoringSupported && !fileMonitoringEnabled.value) return

        val isDuplicate = dir in fileMonitors.keys
        val isShadowed = fileMonitors.keys.any { monitoredDir -> dir isChildOf monitoredDir }
        val needsMonitoring = !isDuplicate && !isShadowed
        if (needsMonitoring) {
            fileMonitors[dir] = FileMonitor.monitorDirectory(dir, true, { type, file ->
                when (type) {
                    ENTRY_CREATE -> runFX {
                        toBeAdded.add(file)
                        update.push(null)
                    }
                    ENTRY_DELETE -> runFX {
                        toBeRemoved.add(file)
                        update.push(null)
                    }
                }
            })
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
        }
    }

    private fun updateLibrary() {

    }

    companion object: KLogging() {
        private infix fun Int.of(word: String) = "${this} ${word.plural(this)}"
    }

}