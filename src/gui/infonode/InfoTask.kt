package gui.infonode

import javafx.concurrent.Task
import javafx.concurrent.Worker.State.READY
import javafx.concurrent.Worker.State.SCHEDULED
import javafx.scene.control.Labeled
import javafx.scene.control.ProgressIndicator
import org.reactfx.Subscription
import util.reactive.maintain
import util.reactive.unsubscribeTry

/** Provides information about the task and its progress. */
open class InfoTask<T: Task<*>>: InfoNode<T> {

    /** Title label or null if none. */
    val title: Labeled?
    /** Message label or null if none. */
    val message: Labeled?
    /** Progress indicator or null if none. */
    val progressIndicator: ProgressIndicator?

    private var titleS: Subscription? = null
    private var messageS: Subscription? = null
    private var progressS: Subscription? = null

    constructor(title: Labeled?, message: Labeled?, progressIndicator: ProgressIndicator?) {
        this.title = title
        this.message = message
        this.progressIndicator = progressIndicator
    }

    override fun setVisible(v: Boolean) {
        title?.isVisible = v
        message?.isVisible = v
        progressIndicator?.isVisible = v
    }

    override fun bind(t: T) {
        unbind()
        if (progressIndicator!=null) t.progressProperty().maintain({ if (t.state==SCHEDULED || t.state==READY) 1.0 else it }, progressIndicator.progressProperty())
        if (title!=null) t.titleProperty().maintain(title.textProperty())
        if (message!=null) t.messageProperty().maintain(message.textProperty())
    }

    override fun unbind() {
        titleS = titleS.unsubscribeTry()
        messageS = messageS.unsubscribeTry()
        progressS = progressS.unsubscribeTry()
    }

}