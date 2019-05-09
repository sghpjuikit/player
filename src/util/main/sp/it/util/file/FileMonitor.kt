package sp.it.util.file

import com.sun.nio.file.ExtendedWatchEventModifier.FILE_TREE
import mu.KLogging
import sp.it.util.async.executor.EventReducer
import sp.it.util.async.executor.FxTimer.Companion.fxTimer
import sp.it.util.async.runFX
import sp.it.util.async.runNew
import sp.it.util.async.threadFactory
import sp.it.util.collections.materialize
import sp.it.util.reactive.Subscribed
import sp.it.util.reactive.Subscription
import sp.it.util.system.Os
import sp.it.util.units.millis
import java.io.File
import java.io.IOException
import java.nio.file.ClosedWatchServiceException
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.nio.file.StandardWatchEventKinds.OVERFLOW
import java.nio.file.WatchEvent
import java.nio.file.WatchEvent.Kind
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Predicate

/*
 * Relatively simple to monitor a file? Think again.
 * 1) Dir only!
 * [WatchService] allows us to only monitor a directory. We then must simply ignore other
 * events of files other than the one we monitor. This is really bad if we want to monitor
 * multiple files in a single directory independently.
 * Solvable using a predicate parameter to filter out unwanted events.
 * 2) Modification events.
 * Events can fire multiple times when application use safe-rewrite saving or in various other scenarios! E.g.
 * editing and saving .java file in Netbeans fires 3 events at about 8-13 ms time gap (tested on SSD).
 * Solvable using event reducer & checking modification times
 * 3) Nested events
 * CREATE and DELETE events are fired for files and directories up to level 2 (children of
 * children of the monitored directory, e.g.,  mon_dir/lvl1/file.txt). MODIFIED events will
 * be thrown for any direct child.
 * Furthermore, recursive monitoring is only supported on Windows, this is a platform limitation.
 */
class FileMonitor {
    private lateinit var monitoredFileDir: File
    private lateinit var watchService: WatchService
    private lateinit var filter: Predicate<File>
    private lateinit var action: (Kind<Path>, File) -> Unit
    private val eventAccumulations = ConcurrentHashMap<File, Event>()
    private val eventAccumulating = Subscribed { startCleanup() }
    private val modificationReducer = EventReducer.thatConsumes<Event> { e ->
        when (e.kind) {
            ENTRY_MODIFY -> eventAccumulations[e.file] = e
            ENTRY_DELETE -> {
                eventAccumulations.remove(e.file)?.emit()
                e.emit()
            }
            else -> e.emit()
        }
    }

    private fun Event.emit() = runFX { action(kind, file) }

    private fun startCleanup(): Subscription {
        val timeEvict = 100
        val t = fxTimer(timeEvict.millis, -1) {
            val timeMs = System.currentTimeMillis()
            val files = eventAccumulations.keys.materialize()
            val events = files.asSequence()
                    .filter { timeMs-eventAccumulations[it]!!.timeMs>timeEvict }
                    .map { eventAccumulations.remove(it)!! }
                    .toList()
            events.forEach { it.emit() }
        }
        runFX { t.start() }

        return Subscription {
            eventAccumulations.values.forEach { it.emit() }
            t.stop()
        }
    }

    fun stop() {
        try {
            watchService.close()
        } catch (e: IOException) {
            logger.error(e) { "Error when closing file monitoring $monitoredFileDir" }
        }
        eventAccumulating.unsubscribe()
    }

    private class Event constructor(kind: Kind<Path>, file: File) {
        val file = file
        val kind = kind
        val timeMs = System.currentTimeMillis()
    }

    companion object: KLogging() {

        /**
         * Creates and starts directory monitoring reporting events for single file contained within the directory.
         *
         * Only events for the specified file will be provided.
         *
         * @param monitoredFile file to be monitored
         * @param action handles the event on fx application thread
         * @return directory monitor
         */
        fun monitorFile(monitoredFile: File, action: (Kind<Path>) -> Unit) = monitorDirectory(monitoredFile.parentDir!!, false, { monitoredFile==it }, { type, _ -> action(type) })

        /**
         * Creates and starts directory monitoring for specified directory reporting events for any 1st
         * level child file which passes the predicate filter.
         *
         * Events for the specified directory and its children (up to depth 1 or unlimited) will be provided.
         *
         * @param monitoredDir directory to be monitored
         * @param recursive determines whether events for files in depth more than 1 will be provided
         * @param filter filter narrowing down events, for example any text file
         * @param action handles the event on fx application thread
         * @return directory monitor
         */
        fun monitorDirectory(monitoredDir: File, recursive: Boolean, filter: (File) -> Boolean = { true }, action: (Kind<Path>, File) -> Unit): FileMonitor {
            val fm = FileMonitor()
            fm.monitoredFileDir = monitoredDir
            fm.action = action
            fm.eventAccumulating.subscribe()

            try {
                fm.watchService = FileSystems.getDefault().newWatchService()

                runNew {
                    fun reg(p: File) {
                        try {
                            when {
                                Os.WINDOWS.isCurrent -> {
                                    if (recursive) p.toPath().register(fm.watchService, arrayOf(ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW), FILE_TREE)
                                    else p.toPath().register(fm.watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW)
                                }
                                else -> {
                                    fun reg1(f: File) = f.toPath().register(fm.watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW)
                                    fun regN(f: File) = f.walkTopDown().materialize().onEach { if (it.isDirectory) reg1(it) }

                                    if (recursive) regN(p)
                                    else reg1(p)
                                }
                            }
                        } catch (e: IOException) {
                            logger.error(e) { "Failed to register file watch for $p" }
                        } catch (e: ClosedWatchServiceException) {
                            // watcher is already closed, logging this would pollute log with error-like non errors
                        }
                    }

                    reg(fm.monitoredFileDir)

                    threadFactory("FileMonitor-${monitoredDir.path}", true).newThread {
                        var valid: Boolean
                        var watchKey: WatchKey
                        do {
                            try {
                                watchKey = fm.watchService.take()
                            } catch (e: InterruptedException) {
                                logger.error(e) { "Interrupted monitoring of directory ${fm.monitoredFileDir}" }
                                return@newThread
                            } catch (e: ClosedWatchServiceException) {
                                return@newThread
                            }

                            @Suppress("UNCHECKED_CAST")
                            for (event in watchKey.pollEvents()) {
                                val type = event.kind()
                                if (type!==OVERFLOW) {
                                    val ev = event as WatchEvent<Path>
                                    val modifiedFileName = ev.context().toString()
                                    val modifiedFile = File(fm.monitoredFileDir, modifiedFileName)

                                    if (type===ENTRY_CREATE && !Os.WINDOWS.isCurrent && modifiedFile.isDirectory)
                                        reg(modifiedFile)

                                    if (filter(modifiedFile))
                                        fm.modificationReducer.push(Event(type as Kind<Path>, modifiedFile))
                                }
                            }

                            valid = watchKey.reset()
                        } while (valid)
                    }.start()
                }

            } catch (e: IOException) {
                logger.error(e) { "Error when starting directory monitoring ${fm.monitoredFileDir}" }
            }


            return fm
        }

    }

}