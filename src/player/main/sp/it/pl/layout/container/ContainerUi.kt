package sp.it.pl.layout.container

import javafx.scene.Parent
import javafx.scene.layout.Pane
import sp.it.pl.layout.AltState

/** Ui allowing user to manage [sp.it.pl.layout.Component] instances. */
interface ContainerUi: AltState {

    /** Scene graph root of this object. */
    val root: Pane

    @JvmDefault
    fun getParent(): Parent = root.parent

    @JvmDefault
    fun close() {}

}