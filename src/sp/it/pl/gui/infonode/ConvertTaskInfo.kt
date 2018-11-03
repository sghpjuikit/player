package sp.it.pl.gui.infonode

import javafx.scene.control.Labeled
import javafx.scene.control.ProgressIndicator
import sp.it.pl.util.Util.enumToHuman
import sp.it.pl.util.async.future.ConvertListTask
import sp.it.pl.util.reactive.sync

class ConvertTaskInfo: InfoTask<ConvertListTask<*, *>> {
    val skipped: Labeled?
    val state: Labeled?

    constructor(title: Labeled?, message: Labeled?, skipped: Labeled?, state: Labeled?, pi: ProgressIndicator?): super(title, message, pi) {
        this.skipped = skipped
        this.state = state
    }

    override fun setVisible(v: Boolean) {
        super.setVisible(v)
        skipped?.isVisible = v
    }

    override fun bind(t: ConvertListTask<*, *>) {
        super.bind(t)
        if (skipped!=null) disposer += t.skippedProperty() sync { skipped.text = "Skipped: $it" }
        if (state!=null) disposer += t.stateProperty() sync { state.text = "State: ${enumToHuman(it)}" }
    }

}