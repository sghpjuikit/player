package gui.pane

import javafx.scene.control.ScrollPane
import javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED
import javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER
import javafx.scene.layout.Pane
import util.async.runLater
import util.graphics.setAnchors
import java.lang.Integer.max
import java.lang.Math.ceil
import java.lang.Math.floor

/**
 * Pane displaying a grid of cells of the same size. Similar to [javafx.scene.layout.TilePane], the cells are always
 * of specified size, vertical gap too, but horizontal gap is adjusted so the cells are evenly laid out.
 */
class CellPane: Pane {

    val cellW: Double
    val cellH: Double
    val cellG: Double

    /**
     * @param cellWidth cell width
     * @param cellHeight cell height
     * @param cellGap cell gap. Vertically it will be altered as needed to maintain layout.
     */
    constructor(cellWidth: Double, cellHeight: Double, cellGap: Double): super() {
        cellW = cellWidth
        cellH = cellHeight
        cellG = cellGap
    }

    override fun layoutChildren() {
        val elements = children.size
        if (elements==0) return
        val w = width
        val c = floor((w+cellG)/(cellW+cellG)).toInt()
        val columns = max(1, c)
        val gapX = cellG+(w+cellG-columns*(cellW+cellG))/columns
        val gapY = cellG

        children.withIndex().forEach { (i, n) ->
            val x = i%columns*(cellW+gapX)
            val y = i/columns*(cellH+gapY)
            n.relocate(x, y)
            n.resize(cellW, cellH)
        }

        val rows = ceil(elements.toDouble()/columns).toInt()

        runLater { prefHeight = rows*(cellH+gapY) }
    }

    /**
     * Wrap the content in a scroll pane
     *
     * @return scroll pane wrapper of the content
     */
    fun scrollable(): ScrollPane {
        val s = ScrollPane().apply {
            content = this
            isFitToWidth = true
            isFitToHeight = false
            hbarPolicy = NEVER
            vbarPolicy = AS_NEEDED
        }
        children += s
        s.setAnchors(0.0)
        return s
    }

}