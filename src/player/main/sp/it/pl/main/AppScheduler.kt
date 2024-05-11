package sp.it.pl.main

import java.io.Serializable
import java.time.Instant
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javafx.geometry.Orientation.HORIZONTAL
import javafx.geometry.Pos.CENTER
import javafx.geometry.Side
import javafx.scene.input.KeyCode.DELETE
import javafx.scene.input.KeyCode.ESCAPE
import javafx.scene.input.KeyEvent
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.layout.Priority.ALWAYS
import kotlinx.coroutines.job
import org.jetbrains.kotlin.codegen.StackValue.Local
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetFactory
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.plugin.impl.LlmString
import sp.it.pl.plugin.impl.Notifier
import sp.it.pl.plugin.impl.VoiceAssistant
import sp.it.pl.ui.objects.form.Validated
import sp.it.pl.ui.objects.icon.Icon
import sp.it.util.async.VT
import sp.it.util.async.coroutine.FX
import sp.it.util.async.coroutine.asFut
import sp.it.util.async.coroutine.delay
import sp.it.util.async.coroutine.launch
import sp.it.util.async.coroutine.toSubscription
import sp.it.util.async.invoke
import sp.it.util.async.runFX
import sp.it.util.async.sleep
import sp.it.util.collections.mapset.MapSet
import sp.it.util.collections.observableList
import sp.it.util.collections.setTo
import sp.it.util.conf.ConfigurableBase
import sp.it.util.conf.cv
import sp.it.util.conf.cvn
import sp.it.util.conf.def
import sp.it.util.dev.stacktraceAsString
import sp.it.util.file.div
import sp.it.util.functional.Try.Companion.ok
import sp.it.util.functional.Try.Error
import sp.it.util.functional.asIf
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.runTry
import sp.it.util.reactive.Handler1
import sp.it.util.reactive.on
import sp.it.util.reactive.onChangeAndNow
import sp.it.util.reactive.onEventDown
import sp.it.util.type.volatile
import sp.it.util.ui.borderPane
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.screen
import sp.it.util.ui.scrollPane
import sp.it.util.ui.separator
import sp.it.util.ui.textArea
import sp.it.util.ui.vBox
import sp.it.util.units.millis
import sp.it.util.units.seconds
import sp.it.util.units.uuid

/** Asynchronous task scheduler. Tasks run on [sp.it.util.async.FX] thread. */
object AppScheduler {

   private val jobs = MapSet<UUID, Scheduled>(ConcurrentHashMap(), { it.id })
   private var running by volatile(false)
   /** All scheduled jobs (even those that passed). */
   val jobsObservable = observableList<Scheduled>()
   /** Application launcher widget factory. Registered only when this plugin is running. */
   val scheduleWidgetFactory by lazy { WidgetFactory(null, AppSchedulerWidget::class, APP.location.widgets/"AppLauncher") }

   fun start() {
      running = true
      APP.serializer.readSingleStorage(ScheduledNotesDB::class).ifOk {
         jobs += it?.orEmpty() as Collection<Scheduled>
         launch(FX) { jobsObservable += jobs }
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

   infix fun schedule(s: Scheduled) {
      jobs += s
      launch(FX) { jobsObservable += s }
   }

   infix fun delete(s: Scheduled) {
      jobs -= s
      launch(FX) { jobsObservable -= s }
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
      launch(FX) {
         APP.plugins.use<VoiceAssistant>() { p ->
            p.speakEvent(LlmString("In the past user asked you to schedule reminder. Now it is time to remind him. He may not remember, do it so he understands what to do.\n\nReminder was: $note"), "You asked me to remind you of: $note")
         }
         APP.plugins.use<Notifier>() {
            val content = vBox(0.0, CENTER) {
               lay += label("- WARNING -") { styleClass += listOf("h1", "h3p") }
               lay += separator(HORIZONTAL) { maxWidth = 500.emScaled }
               lay += label(note) { styleClass += listOf("h4", "h3p") }
            }
            it.showNotification("Reminder", content, true, CENTER).apply {
               headerVisible.value = false
               lClickAction = { AppScheduler delete this@ScheduledNote }
               onShowing += { content.prefWidth = root.scene.window.screen.bounds.width }
            }
         }
      }
   }
}

class ScheduledNotesDB: ArrayList<ScheduledNote>, Serializable {
   constructor(): super()
   constructor(notes: Collection<Scheduled>): super() { addAll(notes.filterIsInstance<ScheduledNote>()) }

   companion object {
      private const val serialVersionUID: Long = 1
   }
}

class AppSchedulerWidget(widget: Widget): SimpleController(widget) {
   private val groups = listBox { }
   private val content = textArea { isEditable = false }
   private val scheduler = AppScheduler
   private val scheduleds = scheduler.jobsObservable
   private var scheduledSel: Scheduled? = null

   init {
      root.lay += vBox {
         lay += cellCreate()
         lay += scrollPane {
            content = groups
         }
         lay(ALWAYS) += content
      }

      // update list when scheduleds change
      scheduleds.onChangeAndNow { update() } on onClose

      // update list when time changes
      launch(FX) {
         while(true) {
            delay(1.seconds)
            var now = Instant.now()
            groups.children.forEach { it.userData?.asIf<(Instant) -> Unit>()?.invoke(now) }
         }
      }.job.toSubscription() on onClose

      // delete scheduled on DELETE
      root.onEventDown(KEY_PRESSED, DELETE) {
         scheduledSel.ifNotNull { scheduler delete it }
      }
   }

   override fun focus() {
      groups.children.firstOrNull()?.requestFocus()
   }

   fun update() {
      groups.children setTo scheduleds
         .sortedByDescending { it.at }
         .onEach { if (it===scheduledSel) content.text = it.textUi() }
         .map { cellScheduled(it) }
   }

   fun Scheduled.textUi(): String =
      if (this is ScheduledNote) this.note else ""

   fun cellCreate() =
      borderPane {
         left = Icon(IconFA.PLUS).run {
            onClickDo {
               object: ConfigurableBase<Any?>(), Validated {
                  val at by cvn<LocalDateTime>(null).def("At")
                  val text by cv<String>("").def("Text")
                  override fun isValid() =
                     if (at.value==null) Error("Scheduled time not be null")
                     else if (at.value!!.isBefore(LocalDateTime.now())) Error("Scheduled time must be in the future")
                     else ok()
               }.configure("Schedule miminder at...") {
                  scheduler schedule ScheduledNote(uuid(), it.at.value!!.atZone(ZoneOffset.systemDefault()).toInstant(), it.text.value)
               }
            }
            withText(Side.RIGHT, "Create")
         }
      }

   fun cellScheduled(s: Scheduled) =
      borderPane {
         left = listBoxRow(IconMA.ACCESS_TIME, s.at.toUi()) {
            val updater = { at: Instant ->
               var isOverdue = s.at<=at
               if (isOverdue) icon.icon(IconMA.CHECK)
               text = if (isOverdue) s.at.toUi() else "${s.at.toUi()}    in ${(s.at.toEpochMilli()-at.toEpochMilli()).millis.toUi()}"
            }
            icon.onClickDo {
               content.text = s.textUi()
               scheduledSel = s
            }
            this.minWidth = 300.emScaled
            this.select(s===scheduledSel)
            this@borderPane.userData = updater
            updater(Instant.now())
         }
         right = Icon(IconMD.DELETE).apply { isFocusTraversable = false }.onClickDo { scheduler delete s }
         onEventDown(KEY_PRESSED, ESCAPE) { scheduler delete s }
      }
}