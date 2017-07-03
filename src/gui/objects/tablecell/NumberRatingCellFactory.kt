package gui.objects.tablecell

import audio.tagging.Metadata
import javafx.geometry.Pos
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import util.parsing.StringParseStrategy
import util.parsing.StringParseStrategy.From
import util.parsing.StringParseStrategy.To

/** Cell for rating displaying the value as number. */
@StringParseStrategy(from = From.SINGLETON, to = To.CONSTANT, constant = "Number")
object NumberRatingCellFactory: RatingCellFactory {

    override fun apply(param: TableColumn<Metadata, Double>) = object: TableCell<Metadata, Double>() {
        init {
            alignment = Pos.CENTER_RIGHT
        }

        override fun updateItem(item: Double?, empty: Boolean) {
            super.updateItem(item, empty)
            text = if (empty) null else String.format("%.2f", item)
        }
    }

}