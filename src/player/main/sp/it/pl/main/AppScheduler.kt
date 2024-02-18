package sp.it.pl.main

import java.io.Serializable
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.plugin.impl.Notifier
import sp.it.pl.plugin.impl.VoiceAssistant
import sp.it.util.async.VT
import sp.it.util.async.invoke
import sp.it.util.async.runFX
import sp.it.util.async.sleep
import sp.it.util.collections.mapset.MapSet
import sp.it.util.dev.stacktraceAsString
import sp.it.util.functional.runTry
import sp.it.util.type.volatile

/** Asynchronous task scheduler. Tasks run on [sp.it.util.async.FX] thread. */
object AppScheduler {

   val jobs = MapSet<UUID, Scheduled>(ConcurrentHashMap(), { it.id })
   var running by volatile(false)

   fun start() {
      running = true
      APP.serializer.readSingleStorage(ScheduledNotesDB::class).ifOk {
         jobs += it?.orEmpty() as Collection<Scheduled>
      }
      VT {
         while (running) {
            val now = Instant.now()
            val jobsToRun = jobs.filter { it.at <= now }
            jobs -= jobsToRun
            runFX {
               jobsToRun.forEach { runTry { it.run() }.ifErrorNotify { e -> AppError("Failed to run scheduled $it", "Reason:\n${e.stacktraceAsString}", null) } }
            }
            sleep(1000)
         }
      }
   }

   fun stop() {
      running = false
      removeDanglingJobs()
      APP.serializer.writeSingleStorage(ScheduledNotesDB(jobs.values))
   }

   private fun removeDanglingJobs() {
      val now = Instant.now()
      jobs.removeIf { it.at < now }
   }
}

/** Task scheduled to [run] at [at] time. */
interface Scheduled {
   val id: UUID
   val at: Instant
   fun run(): Unit
}

/** [Scheduled] that [Notifier.showTextNotification] and [VoiceAssistant.speak] the specified note. */
data class ScheduledNote(override val id: UUID, override val at: Instant, val note: String): Scheduled, Serializable {
   override fun run() {
      APP.plugins.use<VoiceAssistant>() { it.speak("You asked me to remind you of: $note"); }
      APP.plugins.use<Notifier>() { it.showTextNotification("Reminder", note, true) }
   }
}

class ScheduledNotesDB: ArrayList<ScheduledNote>, Serializable {
   constructor(): super()
   constructor(notes: Collection<Scheduled>): super() { addAll(notes.filterIsInstance<ScheduledNote>()) }

   companion object {
      private const val serialVersionUID: Long = 1
   }
}