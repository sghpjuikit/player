package sp.it.pl.main

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import javafx.animation.Interpolator.LINEAR
import javafx.animation.Transition.INDEFINITE
import javafx.beans.property.ReadOnlyProperty
import javafx.collections.FXCollections.observableArrayList
import javafx.concurrent.Task
import javafx.concurrent.Worker.State
import javafx.geometry.Insets
import javafx.geometry.Pos.BOTTOM_LEFT
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.scene.Cursor.HAND
import javafx.scene.Node
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED
import javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER
import javafx.scene.layout.Region.USE_COMPUTED_SIZE
import sp.it.pl.main.AppTask.State.Active
import sp.it.pl.main.AppTask.State.DoneCancel
import sp.it.pl.main.AppTask.State.DoneError
import sp.it.pl.main.AppTask.State.DoneOk
import sp.it.pl.main.AppTask.State.Scheduled
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.window.NodeShow.RIGHT_UP
import sp.it.pl.ui.objects.window.popup.PopWindow
import sp.it.pl.ui.objects.window.popup.PopWindow.Companion.popWindow
import sp.it.util.access.v
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.animation.Anim.Interpolators.Companion.geomElastic
import sp.it.util.animation.Loop
import sp.it.util.async.FX
import sp.it.util.async.future.Fut
import sp.it.util.async.future.Fut.Result
import sp.it.util.async.future.Fut.Result.ResultFail
import sp.it.util.async.future.Fut.Result.ResultInterrupted
import sp.it.util.async.future.Fut.Result.ResultOk
import sp.it.util.async.future.FutList
import sp.it.util.async.invoke
import sp.it.util.async.runFX
import sp.it.util.async.runLater
import sp.it.util.dev.failCase
import sp.it.util.dev.failIf
import sp.it.util.functional.Try
import sp.it.util.functional.TryList
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.runTry
import sp.it.util.functional.supplyIfNotNull
import sp.it.util.math.max
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.attach
import sp.it.util.reactive.on
import sp.it.util.reactive.onItemSync
import sp.it.util.reactive.sync
import sp.it.util.reactive.sync1If
import sp.it.util.reactive.syncFrom
import sp.it.util.type.atomic
import sp.it.util.ui.anchorPane
import sp.it.util.ui.borderPane
import sp.it.util.ui.hBox
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.minPrefMaxHeight
import sp.it.util.ui.minPrefMaxWidth
import sp.it.util.ui.rectangle
import sp.it.util.ui.scrollPane
import sp.it.util.ui.setScaleXY
import sp.it.util.ui.stackPane
import sp.it.util.ui.vBox
import sp.it.util.units.formatToSmallestUnit
import sp.it.util.units.millis
import sp.it.util.units.uuid

object AppProgress {
   private val tasksActive = ConcurrentHashMap<String, AppTask>()
   private val tasks = observableArrayList<AppTask>()!!
   /** Overall progress value that is [ProgressIndicator.INDETERMINATE_PROGRESS] if [activeTaskCount] > `0` or `1.0` otherwise. */
   val progress = v(1.0)
   /** Number of active tasks. */
   val activeTaskCount = v(0)
   private val loop = Loop(Runnable {
      activeTaskCount.value = tasksActive.size
      tasksActive.values.forEach { it.updateTimeActive() } }
   ).start()

   /**
    * The task starts as [Scheduled], immediately transitions to [Active] and completes on
    * [reportDone][StartAppTaskHandle.reportDone].
    *
    * @return handle to control the created task
    */
   fun start(name: String): StartAppTaskHandle = start(AppTask(name, null, null)).apply { reportActive() }

   /**
    * The task starts as [Scheduled], transitions to [Active] on [reportActive][ScheduleAppTaskHandle.reportActive]
    * and completes on [reportDone][StartAppTaskHandle.reportDone].
    *
    * @return handle to control the created task
    */
   fun schedule(name: String): ScheduleAppTaskHandle = start(AppTask(name, null, null))

   /**
    * Registers the specified task in [AppProgress] using [AppProgress.schedule] with the task's title as name.
    *
    * If specified task is already completed, this method is a noop.
    */
   fun start(task: Task<*>) {
      val disposer = Disposer()
      fun Task<*>.toApp() = AppTask(title, messageProperty(), { cancel() })
      fun Task<*>.doOn(vararg state: State, block: (State) -> Unit) = stateProperty().sync1If({ it in state }, block) on disposer

      when (task.state!!) {
         State.READY -> {
            task.doOn(State.SCHEDULED) {
               start(task)
            }
         }
         State.SCHEDULED, State.RUNNING -> {
            val t = start(task.toApp())
            task.progressProperty() sync { t.reportProgress(it.toDouble()) } on disposer
            task.doOn(State.RUNNING) {
               t.reportActive()
            }
            task.doOn(State.SUCCEEDED, State.CANCELLED, State.FAILED) {
               disposer()
               t.reportProgress(1.0)
               t.reportDone(
                  when (it) {
                     State.SUCCEEDED -> ResultOk(null)
                     State.CANCELLED -> ResultInterrupted(InterruptedException("Cancelled"))
                     State.FAILED -> ResultFail(task.exception)
                     else -> failCase(it)
                  }
               )
            }
         }
         State.SUCCEEDED, State.CANCELLED, State.FAILED -> Unit
      }
   }

   private fun start(task: AppTask): ScheduleAppTaskHandle {
      val id = uuid().toString() + task.name
      runFX {
         tasks += task
      }
      return object: ScheduleAppTaskHandle {
         private var runInvoked by atomic(false)
         private var doneInvoked by atomic(false)

         override fun reportActive() {
            failIf(runInvoked) { "Task can only be activated once" }
            runInvoked = true

            tasksActive[id] = task
            runFX {
               task.activate()
               progress.value = if (tasksActive.isEmpty()) 1.0 else -1.0
            }
         }

         override fun reportProgress(progress: Double01?) {
            task.progress.value = progress ?: -1.0
         }

         override fun reportDone(result: Result<*>) {
            failIf(!runInvoked) { "Task can only be finished if it was activated" }
            failIf(doneInvoked) { "Task can only be finished once" }
            doneInvoked = true

            tasksActive -= id
            runFX {
               task.finish(result)
               progress.value = if (tasksActive.isEmpty()) 1.0 else -1.0
            }
         }
      }
   }

   fun showTasks(target: Node): PopWindow {
      val disposer = Disposer()
      val layout = anchorPane {
         lay(10) += scrollPane {
            hbarPolicy = NEVER
            vbarPolicy = AS_NEEDED
            isFitToWidth = true
            content = vBox(5.0) {

               fun AppTask.toInfo() = vBox {
                  minPrefMaxWidth = USE_COMPUTED_SIZE
                  minPrefMaxHeight = USE_COMPUTED_SIZE
                  prefWidth = 450.0

                  lay += stackPane {
                     lay += borderPane {
                        left = hBox(10, CENTER_LEFT) {
                           lay += label(name) {
                              padding = Insets(0.0, 50.0, 0.0, 0.0)
                           }
                        }
                        right = hBox(10, CENTER_RIGHT) {
                           lay += label {
                              state sync { isVisible = it!=Scheduled }
                              timeActive sync { text = it.formatToSmallestUnit() }
                           }
                           lay += Icon().apply {
                              isMouseTransparent = true
                              isFocusTraversable = false

                              fun updateIcon() {
                                 icon(when (state.value) {
                                    is Scheduled -> IconMD.ROTATE_RIGHT
                                    is Active -> IconFA.REPEAT
                                    is DoneOk -> IconFA.CHECK
                                    is DoneError<*> -> IconFA.WARNING
                                    is DoneCancel -> IconFA.BAN
                                 })
                                 state.value.asIf<DoneError<*>>()?.value.ifNotNull { error ->
                                    cursor = HAND
                                    isMouseTransparent = false
                                    onClickDo { APP.ui.actionPane.orBuild.showAndDetect(error, true) }
                                 }
                              }

                              val ar = anim { rotate = 360*it }.dur(1000.millis).intpl(LINEAR).apply { cycleCount = INDEFINITE }
                              val a = anim { setScaleXY(it*it) }.dur(500.millis).intpl(geomElastic())
                              updateIcon()
                              state sync { if (it is Active) ar.play() } on disposer
                              state attach { if (it is DoneOk || it is DoneError<*> || it is DoneCancel) { ar.stop(); ar.applyAt(0.0) } } on disposer
                              state attach { a.playCloseDoOpen(::updateIcon) } on disposer
                           }
                        }
                     }
                     lay(BOTTOM_LEFT) += rectangle {
                        height = 2.emScaled
                        progress attach { width = this@stackPane.width*(it.toDouble() max 0.0) } on disposer
                        progress attach { isVisible = 0<=it && it<1.0 } on disposer
                     }
                  }

                  lay += supplyIfNotNull(message) { m ->
                     borderPane {
                        right = hBox(10, CENTER_RIGHT) {
                           lay += label {
                              maxWidth = 500.0
                              textProperty() syncFrom m on disposer
                           }
                           lay += Icon(IconFA.BAN).apply {
                              isVisible = cancel!=null
                              if (cancel!=null) state.sync1If({ it!=Active }) { isVisible = false } on disposer
                              onClickDo { cancel?.invoke() }
                           }
                        }
                     }
                  }
               }
               tasks.onItemSync {
                  children.add(it.toInfo())
                  runLater { this@scrollPane.vvalue = height }
               } on disposer
            }
         }
      }
      return popWindow {
         content.value = layout
         title.value = "Tasks"
         isEscapeHide.value = true
         isAutohide.value = false
         onHidden += disposer
         show(RIGHT_UP(target))
      }
   }
}

/** Task handle to update the state of the [AppTask]. */
interface StartAppTaskHandle {
   /** Update progress of the task before calling [reportDone]. The last progress value must be either `1.0` or `null`. */
   fun reportProgress(progress: Double01?)
   /** Update task to be finished with the specified success or error result. */
   fun reportDone(result: Result<*>)
   /** Update task to be finished with the specified success result. */
   fun reportDone(result: Any? = Unit) = reportDone(ResultOk(result))
   /** Update task to be finished with the specified success or error result. */
   fun reportDone(result: Try<*,Throwable>) = result.ifOk { reportDone(ResultOk(it)) }.ifError { reportDone(ResultFail(it)) }
}

/** Task handle to update the state of the [AppTask]. */
interface ScheduleAppTaskHandle: StartAppTaskHandle {
   fun reportActive()
}

/** Task handle to update the state of the [AppTask]. */
inline fun <TASK: StartAppTaskHandle, T> TASK.reportFor(block: (TASK) -> T): T =
   runTry { block(this) }.ifAny(::reportDone).orThrow

private class AppTask(val name: String, val message: ReadOnlyProperty<String>?, val cancel: (() -> Unit)?) {
   val state = v<State>(Scheduled)
   val progress = v((-1.0).asIs<Double01>())
   val timeActive = v(0.millis)
   private var timeStart = 0L

   fun updateTimeActive() {
      if (state.value==Active)
         timeActive.value = (System.currentTimeMillis() - timeStart).millis
   }

   fun activate() {
      timeStart = System.currentTimeMillis()
      state.value = Active
      updateTimeActive()
   }

   fun finish(result: Result<*>) {

      fun computeState(result: Result<*>): State = when (result) {
         is ResultOk<*> -> when (val resultValue = result.value) {
            is Try.Error<*> -> DoneError(resultValue.value)
            is FutList<*> -> if (resultValue.all { it.isOk() }) DoneOk else DoneError(resultValue.map { it.getDone().toTry() })
            is TryList<*,*> -> if (resultValue.all { it.isOk }) DoneOk else DoneError(resultValue)
            is Fut<*> -> computeState(resultValue.getDone())
            else -> DoneOk
         }
         is ResultInterrupted -> DoneCancel
         is ResultFail -> DoneError(result.error)
      }

      updateTimeActive()
      state.value = computeState(result)
   }

   sealed interface State {
      data object Scheduled: State
      data object Active: State
      data object DoneOk: State
      data object DoneCancel: State
      data class DoneError<T>(val value: T): State
   }
}

/**
 * Registers this as a task in [AppProgress] with the specified name.
 *
 * Calls [AppProgress.start] immediately and [StartAppTaskHandle.reportDone] when this future finishes.
 * If this future is already completed, this method is a noop.
 *
 * Note that the task will immediately become [Active], if scheduling should be reported, use [Fut.thenWithAppProgress].
 */
fun <T> Fut<T>.withAppProgress(name: String) = apply {
   var t: StartAppTaskHandle? = null
   FX {
      if (!isDone())
         t = AppProgress.start(name)
   }
   onDone(FX) {
      t?.reportDone(it)
   }
}

/**
 * Invokes [Fut.then] with the specified executor and block, while registering the block as an app task with the
 * specified name.
 *
 * Calls [AppProgress.schedule] immediately, [ScheduleAppTaskHandle.reportActive] when the block starts executing and
 * [StartAppTaskHandle.reportDone] when block finishes executing.
 */
fun <T, R> Fut<T>.thenWithAppProgress(executor: Executor, name: String, block: (T) -> R): Fut<R> = run {
   lateinit var t: ScheduleAppTaskHandle
   FX {
      t = AppProgress.schedule(name)
   }
   then(executor) {
      FX {
         t.reportActive()
      }
      block(it)
   }.onDone(FX) {
      t.reportDone(it)
   }
}

/**
 * Display progress of this future in the specified progress indicator.
 * The progress is immediately set to 1 if done or -1 otherwise and to 1 once this future completes.
 */
fun <T> Fut<T>.withProgress(progressIndicator: ProgressIndicator) = apply {
   runFX {
      progressIndicator.progress = if (isDone()) 1.0 else -1.0
   }
   onDone(FX) {
      progressIndicator.progress = 1.0
   }
}

fun Task<*>.withAppProgress() = AppProgress.start(this)