package sp.it.pl.main

import javafx.beans.property.ReadOnlyProperty
import javafx.collections.FXCollections.observableArrayList
import javafx.concurrent.Task
import javafx.concurrent.Worker.State
import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.scene.Node
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED
import javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER
import javafx.scene.layout.Region.USE_COMPUTED_SIZE
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.gui.objects.window.NodeShow.RIGHT_UP
import sp.it.pl.gui.objects.window.popup.PopWindow
import sp.it.pl.main.AppTask.State.ACTIVE
import sp.it.pl.main.AppTask.State.DONE_CANCEL
import sp.it.pl.main.AppTask.State.DONE_ERROR
import sp.it.pl.main.AppTask.State.DONE_OK
import sp.it.pl.main.AppTask.State.SCHEDULED
import sp.it.util.access.v
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.animation.Loop
import sp.it.util.animation.interpolator.ElasticInterpolator
import sp.it.util.async.FX
import sp.it.util.async.future.Fut
import sp.it.util.async.future.Fut.Result
import sp.it.util.async.future.Fut.Result.ResultFail
import sp.it.util.async.future.Fut.Result.ResultInterrupted
import sp.it.util.async.future.Fut.Result.ResultOk
import sp.it.util.async.invoke
import sp.it.util.async.runFX
import sp.it.util.dev.failCase
import sp.it.util.dev.failIf
import sp.it.util.formatToSmallestUnit
import sp.it.util.functional.supplyIf
import sp.it.util.reactive.attach
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
import sp.it.util.ui.scrollPane
import sp.it.util.ui.setScaleXY
import sp.it.util.ui.vBox
import sp.it.util.units.millis
import sp.it.util.units.uuid
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor

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
    * The task starts as [SCHEDULED], immediately transitions to [ACTIVE] and completes on
    * [reportDone][StartAppTaskHandle.reportDone].
    *
    * @return handle to control the created task
    */
   fun start(name: String): StartAppTaskHandle = start(AppTask(name, null, null)).apply { reportActive() }

   /**
    * The task starts as [SCHEDULED], transitions to [ACTIVE] on [reportActive][ScheduleAppTaskHandle.reportActive]
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

      fun Task<*>.toApp() = AppTask(title, messageProperty(), { cancel() })
      fun Task<*>.doOn(vararg state: State, block: (State) -> Unit) = stateProperty().sync1If({ it in state }, block)

      when (task.state!!) {
         State.READY -> {
            task.doOn(State.SCHEDULED) {
               start(task)
            }
         }
         State.SCHEDULED, State.RUNNING -> {
            val t = start(task.toApp())
            task.doOn(State.RUNNING) {
               t.reportActive()
            }
            task.doOn(State.SUCCEEDED, State.CANCELLED, State.FAILED) {
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

   fun showTasks(target: Node) {
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

                  lay += borderPane {
                     left = hBox(10, CENTER_LEFT) {
                        lay += label(name).apply {
                           padding = Insets(0.0, 50.0, 0.0, 0.0)
                        }
                     }
                     right = hBox(10, CENTER_RIGHT) {
                        lay += label {
                           state sync { isVisible = it!=SCHEDULED }
                           timeActive sync { text = it.formatToSmallestUnit() }
                        }
                        lay += Icon().apply {
                           isMouseTransparent = true

                           fun updateIcon() {
                              icon(when (state.value) {
                                 SCHEDULED -> IconMD.ROTATE_RIGHT
                                 ACTIVE -> IconFA.REPEAT
                                 DONE_OK -> IconFA.CHECK
                                 DONE_ERROR -> IconFA.CLOSE
                                 DONE_CANCEL -> IconFA.BAN
                              })
                           }

                           val a = anim { setScaleXY(it*it) }.dur(500.millis).intpl(ElasticInterpolator())
                           updateIcon()
                           state attach { a.playCloseDoOpen(::updateIcon) }
                        }
                     }
                  }
                  lay += supplyIf(message!=null) {
                     borderPane {
                        right = hBox(10, CENTER_RIGHT) {
                           lay += label {
                              maxWidth = 500.0
                              textProperty() syncFrom message!!
                           }
                           lay += Icon(IconFA.BAN).apply {
                              isVisible = cancel!=null
                              if (cancel!=null) state.sync1If({ it!=ACTIVE }) { isVisible = false }
                              onClickDo { cancel?.invoke() }
                           }
                        }
                     }
                  }
               }
               tasks.onItemSync {
                  children.add(0, it.toInfo())
               }
            }
         }
      }
      PopWindow().apply {
         content.value = layout
         title.value = "Tasks"
         isEscapeHide.value = true
         isAutohide.value = true
         show(RIGHT_UP(target))
      }
   }
}

interface StartAppTaskHandle {
   fun reportDone(result: Result<*>)
}

interface ScheduleAppTaskHandle: StartAppTaskHandle {
   fun reportActive()
}

private class AppTask(val name: String, val message: ReadOnlyProperty<String>?, val cancel: (() -> Unit)?) {
   val state = v(SCHEDULED)
   val timeActive = v(0.millis)
   private var timeStart = 0L

   fun updateTimeActive() {
      if (state.value==ACTIVE)
         timeActive.value = (System.currentTimeMillis() - timeStart).millis
   }

   fun activate() {
      timeStart = System.currentTimeMillis()
      state.value = ACTIVE
      updateTimeActive()
   }

   fun finish(result: Result<*>) {
      updateTimeActive()
      state.value = when (result) {
         is ResultOk<*> -> DONE_OK
         is ResultInterrupted -> DONE_CANCEL
         is ResultFail -> DONE_ERROR
      }
   }

   enum class State { SCHEDULED, ACTIVE, DONE_OK, DONE_CANCEL, DONE_ERROR }
}

/**
 * Registers this as a task in [AppProgress] with the specified name.
 *
 * Calls [AppProgress.start] immediately and [StartAppTaskHandle.reportDone] when this future finishes.
 * If this future is already completed, this method is a noop.
 *
 * Note that the task will immediately become [ACTIVE], if scheduling should be reported, use [Fut.thenWithAppProgress].
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
      if (!isDone())
         progressIndicator.progress = if (isDone()) 1.0 else -1.0
   }
   onDone {
      runFX {
         progressIndicator.progress = 1.0
      }
   }
}