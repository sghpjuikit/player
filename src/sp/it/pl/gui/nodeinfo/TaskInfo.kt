package sp.it.pl.gui.nodeinfo

import javafx.concurrent.Task
import javafx.concurrent.Worker.State.READY
import javafx.concurrent.Worker.State.SCHEDULED
import javafx.scene.control.Labeled
import javafx.scene.control.ProgressIndicator
import sp.it.pl.util.reactive.Disposer
import sp.it.pl.util.reactive.on
import sp.it.pl.util.reactive.sync
import sp.it.pl.util.reactive.syncTo

/** Provides information about the task and its progress. */
open class TaskInfo<T: Task<*>>: NodeInfo<T> {

    /** Title label or null if none. */
    val title: Labeled?
    /** Message label or null if none. */
    val message: Labeled?
    /** Progress indicator or null if none. */
    val progress: ProgressIndicator?

    protected val disposer = Disposer()

    constructor(title: Labeled?, message: Labeled?, progressIndicator: ProgressIndicator?) {
        this.title = title
        this.message = message
        this.progress = progressIndicator
    }

    override fun setVisible(v: Boolean) {
        title?.isVisible = v
        message?.isVisible = v
        progress?.isVisible = v
    }

    override fun bind(bindable: T) {
        unbind()
        val computeProgress = { it: Number -> if (bindable.state==SCHEDULED || bindable.state==READY) 1.0 else it.toDouble() }
        if (title!=null) bindable.titleProperty() syncTo title.textProperty() on disposer
        if (message!=null) bindable.messageProperty() syncTo message.textProperty() on disposer
        if (progress!=null) bindable.progressProperty() sync { progress.progress = computeProgress(it) } on disposer
    }

    override fun unbind() = disposer()

}