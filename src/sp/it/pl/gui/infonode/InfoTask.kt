package sp.it.pl.gui.infonode

import javafx.concurrent.Task
import javafx.concurrent.Worker.State.READY
import javafx.concurrent.Worker.State.SCHEDULED
import javafx.scene.control.Labeled
import javafx.scene.control.ProgressIndicator
import sp.it.pl.util.reactive.Disposer
import sp.it.pl.util.reactive.sync
import sp.it.pl.util.reactive.syncTo

/** Provides information about the task and its progress. */
open class InfoTask<T: Task<*>>: InfoNode<T> {

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

    override fun bind(t: T) {
        unbind()
        val computeProgress = { it: Number -> if (t.state==SCHEDULED || t.state==READY) 1.0 else it.toDouble() }
        if (title!=null) disposer += t.titleProperty() syncTo title.textProperty()
        if (message!=null) disposer += t.messageProperty() syncTo message.textProperty()
        if (progress!=null) disposer += t.progressProperty() sync { progress.progress = computeProgress(it) }
    }

    override fun unbind() = disposer()

}