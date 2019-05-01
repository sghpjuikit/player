package sp.it.pl.gui.objects.tablecell

import javafx.geometry.Pos
import javafx.scene.control.ContentDisplay.GRAPHIC_ONLY
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.rate
import sp.it.pl.gui.objects.rating.Rating
import sp.it.pl.main.APP
import sp.it.util.parsing.StringParseStrategy
import sp.it.util.parsing.StringParseStrategy.From
import sp.it.util.parsing.StringParseStrategy.To
import sp.it.util.reactive.syncFrom

/** Cell for rating displaying the value as rating control. */
@StringParseStrategy(from = From.SINGLETON, to = To.CONSTANT, constant = "Stars")
object RatingRatingCellFactory: RatingCellFactory {

    override fun apply(c: TableColumn<Metadata, Double?>) = object : TableCell<Metadata, Double?>() {
        var r = Rating(APP.maxRating.get())

        init {
            contentDisplay = GRAPHIC_ONLY
            alignment = Pos.CENTER
            r.icons syncFrom APP.maxRating
            r.partialRating syncFrom APP.partialRating
            r.editable.value = true
            if (c.userData==Metadata.Field.RATING)
                r.onRatingEdited = { c.tableView.items[index].rate(it) }
        }

        override fun updateItem(item: Double?, empty: Boolean) {
            super.updateItem(item, empty)
            if (empty) {
                graphic = null
            } else {
                r.rating.set(item)
                if (graphic == null) graphic = r
            }
        }

    }

}