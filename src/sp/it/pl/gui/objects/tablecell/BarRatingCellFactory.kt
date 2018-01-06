package sp.it.pl.gui.objects.tablecell

import javafx.scene.control.ContentDisplay.GRAPHIC_ONLY
import javafx.scene.control.ProgressBar
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.util.parsing.StringParseStrategy
import sp.it.pl.util.parsing.StringParseStrategy.From
import sp.it.pl.util.parsing.StringParseStrategy.To

/** Cell for rating displaying the value as progress bar. */
@StringParseStrategy(from = From.SINGLETON, to = To.CONSTANT, constant = "Bar")
object BarRatingCellFactory: RatingCellFactory {

    override fun apply(param: TableColumn<Metadata, Double?>) = object: TableCell<Metadata, Double?>() {
        private var p = ProgressBar()

        init {
            contentDisplay = GRAPHIC_ONLY
        }

        override fun updateItem(item: Double?, empty: Boolean) {
            super.updateItem(item, empty)
            if (empty) {
                graphic = null
            } else {
                if (graphic==null) graphic = p
                p.progress = item!!
            }
        }
    }

}