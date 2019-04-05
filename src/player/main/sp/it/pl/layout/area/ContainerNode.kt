package sp.it.pl.layout.area

import javafx.scene.Parent
import javafx.scene.layout.Pane
import sp.it.pl.layout.AltState

/** Container graphics. */
interface ContainerNode: AltState {

    /** Scene graph root of this object. */
    val root: Pane

    @JvmDefault
    fun getParent(): Parent = root.parent

    @JvmDefault
    fun close() {}

}