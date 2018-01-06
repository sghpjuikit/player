package sp.it.pl.gui.objects.tablecell

import javafx.geometry.Pos
import javafx.scene.control.ContentDisplay.GRAPHIC_ONLY
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.MetadataWriter
import sp.it.pl.gui.objects.rating.Rating
import sp.it.pl.main.AppUtil.APP
import sp.it.pl.util.parsing.StringParseStrategy
import sp.it.pl.util.parsing.StringParseStrategy.From
import sp.it.pl.util.parsing.StringParseStrategy.To

/** Cell for rating displaying the value as rating control. */
@StringParseStrategy(from = From.SINGLETON, to = To.CONSTANT, constant = "Stars")
object RatingRatingCellFactory: RatingCellFactory {

    override fun apply(c: TableColumn<Metadata, Double?>) = object : TableCell<Metadata, Double?>() {
        var r = Rating(APP.maxRating.get())

        init {
            contentDisplay = GRAPHIC_ONLY
            alignment = Pos.CENTER
            r.icons.bind(APP.maxRating)
            r.partialRating.bind(APP.partialRating)
            r.editable.bind(APP.allowRatingChange)
            if (c.userData==Metadata.Field.RATING)
                r.onRatingEdited = { it?.let { MetadataWriter.useToRate(c.tableView.items[index], it) } }
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