package sp.it.pl.gui.objects.image

import de.jensd.fx.glyphs.GlyphIcons
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.DETAILS
import javafx.scene.input.DragEvent.DRAG_EXITED
import javafx.scene.input.DragEvent.DRAG_OVER
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.MouseEvent.MOUSE_ENTERED
import javafx.scene.input.MouseEvent.MOUSE_EXITED
import sp.it.pl.gui.objects.placeholder.DragPane
import sp.it.pl.main.APP
import sp.it.pl.main.getImageFile
import sp.it.pl.main.getImageFileOrUrl
import sp.it.pl.main.hasImageFileOrUrl
import sp.it.pl.main.installDrag
import sp.it.util.async.future.Fut
import sp.it.util.async.future.Fut.Companion.fut
import sp.it.util.file.FileType.FILE
import sp.it.util.reactive.onEventDown
import sp.it.util.system.chooseFile
import java.io.File

/**
 * Thumbnail which can accept an image file. A custom action invoked afterwards can be defined.
 *
 * File can be accepted either by using file chooser opened by clicking on this thumbnail, or by drag&drop.
 */
class ThumbnailWithAdd @JvmOverloads constructor(dragIcon: GlyphIcons = DETAILS, dragDescription: String = "Set Image"): Thumbnail() {

    /** Action for when image file is dropped or received from file chooser. Default does nothing. */
    @JvmField var onFileDropped: (Fut<File?>) -> Unit = {}
    /** Action for when image is highlighted. Default does nothing. */
    @JvmField var onHighlight: (Boolean) -> Unit = {}
    private val dragData = DragPane.Data({ dragDescription }, dragIcon)

    init {
        // highlight on hover | drag
        root.onEventDown(MOUSE_EXITED) { highlight(false) }
        root.onEventDown(MOUSE_ENTERED) { highlight(true) }
        root.onEventDown(DRAG_OVER) { if (it.dragboard.hasImageFileOrUrl()) onHighlight(true) }
        root.onEventDown(DRAG_EXITED) { onHighlight(false) }

        // add image on click
        root.onEventDown(MOUSE_CLICKED, PRIMARY) {
            chooseFile("Select image to add to tag", FILE, APP.DIR_APP, root.scene.window).ifOk {
                onFileDropped(fut(it))
            }
        }

        // drag&drop
        installDrag(
            root, dragIcon, dragDescription,
            { e -> e.dragboard.hasImageFileOrUrl() },
            { e ->
                // Fut<File> fi = getImage(e);
                // File i = fi.isDone() ? fi.getDone() : null;
                // boolean same = i!=null && i.equals(except.get());
                val i = e.dragboard.getImageFile()
                i!=null && i==file  // image of this file is already displayed
            },
            { e -> onFileDropped(e.dragboard.getImageFileOrUrl()) }
        )
    }

    private fun highlight(v: Boolean) {
        if (v) DragPane.PANE.getM(dragData).showFor(root)
        else DragPane.PANE.ifSet { it.hide() }

        onHighlight(v)
    }

}