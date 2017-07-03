package gui.objects.tablecell

import audio.tagging.Metadata
import javafx.geometry.Pos
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import util.parsing.StringParseStrategy
import util.parsing.StringParseStrategy.From
import util.parsing.StringParseStrategy.To
import java.lang.Math.round

/** Cell for rating displaying the value as text from '' to '*****'. */
@StringParseStrategy(from = From.SINGLETON, to = To.CONSTANT, constant = "Hyphen")
object HyphenRatingCellFactory: RatingCellFactory {
    private val s0 = ""
    private val s1 = "-"
    private val s2 = "--"
    private val s3 = "---"
    private val s4 = "----"
    private val s5 = "-----"

    override fun apply(param: TableColumn<Metadata, Double>) = object: TableCell<Metadata, Double>() {

        init {
            alignment = Pos.CENTER
        }

        override fun updateItem(item: Double?, empty: Boolean) {
            super.updateItem(item, empty)
            if (empty) {
                text = null
            } else {
                val r = round(item!!/0.2).toInt()
                text = when (r) {
                    0 -> s0
                    1 -> s1
                    2 -> s2
                    3 -> s3
                    4 -> s4
                    5 -> s5
                    else -> null
                }
            }
        }
    }

}