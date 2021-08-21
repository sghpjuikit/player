package sp.it.pl.ui.objects.rating

import javafx.geometry.Pos
import javafx.scene.control.ContentDisplay.GRAPHIC_ONLY
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.util.Callback
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.writeRating

/** Cell factory for cells displaying nullable <0,1> [Double] value in [Rating] control. */
object RatingCellFactory: Callback<TableColumn<Metadata, Double?>, TableCell<Metadata, Double?>> {

   override fun call(c: TableColumn<Metadata, Double?>) = object: TableCell<Metadata, Double?>() {
      val r = Rating()

      init {
         contentDisplay = GRAPHIC_ONLY
         alignment = Pos.CENTER
         r.editable.value = true
         if (c.userData==Metadata.Field.RATING)
            r.onRatingEdited = { c.tableView.items[index].writeRating(it) }
      }

      override fun updateItem(item: Double?, empty: Boolean) {
         super.updateItem(item, empty)
         if (empty) {
            graphic = null
         } else {
            r.rating.set(item)
            if (graphic==null) graphic = r
         }
      }

   }

}