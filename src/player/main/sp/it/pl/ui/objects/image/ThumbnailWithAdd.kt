package sp.it.pl.ui.objects.image

import de.jensd.fx.glyphs.GlyphIcons
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.DETAILS
import java.io.File
import javafx.scene.input.DragEvent.DRAG_EXITED
import javafx.scene.input.DragEvent.DRAG_OVER
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.MouseEvent.MOUSE_ENTERED
import javafx.scene.input.MouseEvent.MOUSE_EXITED
import sp.it.pl.main.APP
import sp.it.pl.main.getImageFile
import sp.it.pl.main.getImageFileOrUrl
import sp.it.pl.main.hasImageFileOrUrl
import sp.it.pl.main.installDrag
import sp.it.pl.ui.objects.placeholder.DragPane
import sp.it.util.async.future.Fut
import sp.it.util.async.future.Fut.Companion.fut
import sp.it.util.file.FileType.FILE
import sp.it.util.reactive.onEventDown
import sp.it.util.system.chooseFile

/**
 * Thumbnail which can accept an image file. A custom action invoked afterwards can be defined.
 *
 * File can be accepted either by using file chooser opened by clicking on this thumbnail, or by drag&drop.
 */
class ThumbnailWithAdd constructor(dragIcon: GlyphIcons = DETAILS, dragDescription: String = "Set Image"): Thumbnail() {

   /** Action for when image file is dropped or received from file chooser. Default does nothing. */
   var onFileDropped: (Fut<File?>) -> Unit = {}
   /** Action for when image is highlighted. Default does nothing. */
   var onHighlight: (Boolean) -> Unit = {}
   private val dragData = DragPane.Data({ dragDescription }, dragIcon, null)

   init {
      // highlight on hover | drag
      root.onEventDown(MOUSE_EXITED) { highlight(false) }
      root.onEventDown(MOUSE_ENTERED) { highlight(true, false) }
      root.onEventDown(DRAG_OVER) { if (it.dragboard.hasImageFileOrUrl()) onHighlight(true) }
      root.onEventDown(DRAG_EXITED) { onHighlight(false) }

      root.onEventDown(MOUSE_CLICKED, PRIMARY) { doSelectFile() }

      root.installDrag(
         dragIcon,
         dragDescription,
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

   private fun highlight(v: Boolean, focus: Boolean = true) {
      if (v)
         DragPane.PANE.getM(dragData).apply {
            showFor(root, focus)
            animateShow(imageView)
         }
      else DragPane.PANE.ifSet {
         it.hide()
         it.animateHide()
      }

      onHighlight(v)
   }

   fun doSelectFile() {
      chooseFile("Select image file", FILE, APP.location, root.scene.window).ifOk {
         onFileDropped(fut(it))
      }
   }

}