package sp.it.pl.main

import javafx.collections.FXCollections.observableArrayList
import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.scene.Node
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED
import javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER
import javafx.scene.layout.Region.USE_COMPUTED_SIZE
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.gui.objects.popover.NodePos
import sp.it.pl.gui.objects.popover.PopOver
import sp.it.pl.main.AppTask.State.ACTIVE
import sp.it.pl.main.AppTask.State.DONE_ERROR
import sp.it.pl.main.AppTask.State.DONE_OK
import sp.it.pl.util.access.v
import sp.it.pl.util.animation.Loop
import sp.it.pl.util.async.future.Fut
import sp.it.pl.util.async.runFX
import sp.it.pl.util.dev.ThreadSafe
import sp.it.pl.util.formatToSmallestUnit
import sp.it.pl.util.functional.Try
import sp.it.pl.util.graphics.anchorPane
import sp.it.pl.util.graphics.borderPane
import sp.it.pl.util.graphics.hBox
import sp.it.pl.util.graphics.label
import sp.it.pl.util.graphics.lay
import sp.it.pl.util.graphics.minPrefMaxHeight
import sp.it.pl.util.graphics.minPrefMaxWidth
import sp.it.pl.util.graphics.scrollPane
import sp.it.pl.util.graphics.vBox
import sp.it.pl.util.reactive.onItemDo
import sp.it.pl.util.reactive.sync
import sp.it.pl.util.units.millis
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object AppProgress {
    private val tasksActive = ConcurrentHashMap<String, AppTask>()
    private val tasks = observableArrayList<AppTask>()!!
    val progress = v(1.0)
    val loop = Loop(Runnable { tasksActive.values.forEach { it.updateTimeActive() } }).start()

    fun start(text: String): (Try<*,*>) -> Unit {
        val id = UUID.randomUUID().toString()+text
        val task = AppTask(text)
        tasksActive[id] = task
        runFX {
            tasks += task
            progress.value = if (tasksActive.isEmpty()) 1.0 else -1.0
        }
        return { result ->
            tasksActive -= id
            runFX {
                task.finish(result)
                progress.value = if (tasksActive.isEmpty()) 1.0 else -1.0
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
                    fun AppTask.toInfo() = borderPane {
                        minPrefMaxWidth = USE_COMPUTED_SIZE
                        minPrefMaxHeight = USE_COMPUTED_SIZE
                        prefWidth = 450.0
                        left = hBox(10, CENTER_LEFT) {
                            lay += label(name).apply {
                                padding = Insets(0.0, 50.0, 0.0, 0.0)
                            }
                        }
                        right = hBox(10, CENTER_RIGHT) {
                            lay += label {
                                timeActive sync { text = it.formatToSmallestUnit() }
                            }
                            lay += Icon().apply {
                                isMouseTransparent = true
                                state sync {
                                    icon(when (it!!) {
                                        ACTIVE -> IconFA.REPEAT
                                        DONE_OK -> IconFA.CHECK
                                        DONE_ERROR -> IconFA.BAN
                                    })
                                }
                            }
                        }
                    }
                    tasks.onItemDo {
                        children.add(0, it.toInfo())
                    }
                }
            }
        }
        PopOver(layout).apply {
            title.value = "Tasks"
            detached.value = false
            detachable.value = false
            isHideOnEscape = false
            arrowSize.value = 0.0
            arrowIndent.value = 0.0
            cornerRadius.value = 0.0
            isAutoFix = false
            isAutoHide = true
            show(target, NodePos.RIGHT_UP)
        }
    }
}

private class AppTask(val name: String) {
    val state = v(ACTIVE)
    val timeActive = v(0.millis)
    private val timeStart = System.currentTimeMillis()
    private var timeEnd: Long? = null

    fun updateTimeActive() {
        if (state.value==ACTIVE)
            timeActive.value = (System.currentTimeMillis()-timeStart).millis
    }

    fun finish(result: Try<*,*>) {
        updateTimeActive()
        state.value = if (result.isOk) DONE_OK else DONE_ERROR
    }

    enum class State { ACTIVE, DONE_OK, DONE_ERROR }
}

/**
 * Registers this as a task in [AppProgress] using [AppProgress.start] with the specified name.
 * The task starts as [AppTask.State.ACTIVE] and completes when [Fut.onDone] is invoked.
 *
 * If this future is already completed, this method is a noop.
 */
@ThreadSafe
fun <T> Fut<T>.showAppProgress(name: String) = apply {
    var progress: ((Try<*,*>) -> Unit)? = null
    runFX {
        if (!isDone())
            progress = AppProgress.start(name)
    }
    onDone {
        runFX {
            progress?.invoke(it)
        }
    }
}

/**
 * Display progress of this future in the specified progress indicator.
 * The progress is immediately set to 1 if done or -1 otherwise and to 1 once this future completes.
 */
fun <T> Fut<T>.showProgress(progressIndicator: ProgressIndicator) = apply {
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