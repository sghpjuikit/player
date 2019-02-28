package sp.it.pl.gui.objects.popover

import javafx.geometry.Rectangle2D
import javafx.scene.Node
import javafx.stage.Screen
import javafx.stage.Window
import sp.it.pl.main.APP
import sp.it.pl.util.functional.orNull
import sp.it.pl.util.graphics.centreX
import sp.it.pl.util.graphics.centreY
import sp.it.pl.util.graphics.getScreenForMouse
import sp.it.pl.util.graphics.screen
import sp.it.pl.util.graphics.size
import sp.it.pl.util.math.P

// TODO: add css support for the gap value
// gap between screen border and the popover
// note that the practical value is (GAP-padding)/2 so if padding is 4 then real GAP will be 3
private const val GAP = 9.0

/** Popover arrow locations. */
enum class ArrowLocation {
    LEFT_TOP, LEFT_CENTER, LEFT_BOTTOM, RIGHT_TOP, RIGHT_CENTER, RIGHT_BOTTOM,
    TOP_LEFT, TOP_CENTER, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
}

/** Defines position in relation to a node. */
enum class NodePos {
    CENTER,
    UP_LEFT,
    UP_CENTER,
    UP_RIGHT,
    DOWN_LEFT,
    DOWN_CENTER,
    DOWN_RIGHT,
    RIGHT_UP,
    RIGHT_CENTER,
    RIGHT_DOWN,
    LEFT_UP,
    LEFT_CENTER,
    LEFT_DOWN;

    fun computeXY(n: Node, popup: PopOver<*>) = P(computeX(n, popup), computeY(n, popup)) + n.layoutBounds.size/2.0

    private fun computeX(n: Node, popup: PopOver<*>): Double {
        val w = popup.contentNode.value.layoutBounds.width
        val x = n.localToScreen(0.0, 0.0).x
        return when (this) {
            CENTER, DOWN_CENTER, UP_CENTER -> x+n.layoutBounds.width/2-w/2
            LEFT_CENTER, LEFT_UP, LEFT_DOWN -> x-w
            RIGHT_CENTER, RIGHT_UP, RIGHT_DOWN -> x+n.layoutBounds.width
            UP_LEFT, DOWN_LEFT -> x
            UP_RIGHT, DOWN_RIGHT -> x+n.layoutBounds.width-w
        }
    }

    private fun computeY(n: Node, popup: PopOver<*>): Double {
        val h = popup.contentNode.value.layoutBounds.height
        val y = n.localToScreen(0.0, 0.0).y
        return when (this) {
            UP_RIGHT, UP_CENTER, UP_LEFT -> y-h
            DOWN_CENTER, DOWN_LEFT, DOWN_RIGHT -> y+n.layoutBounds.height
            LEFT_UP, RIGHT_UP -> y
            CENTER, LEFT_CENTER, RIGHT_CENTER -> y+n.layoutBounds.height/2-h/2
            LEFT_DOWN, RIGHT_DOWN -> y+n.layoutBounds.height-h
        }
    }
}

/** Defines position within the screen area defined by [ScreenUse]. */
enum class ScreenPos {
    SCREEN_TOP_RIGHT,
    SCREEN_TOP_LEFT,
    SCREEN_CENTER,
    SCREEN_BOTTOM_RIGHT,
    SCREEN_BOTTOM_LEFT,
    APP_TOP_RIGHT,
    APP_TOP_LEFT,
    APP_CENTER,
    APP_BOTTOM_RIGHT,
    APP_BOTTOM_LEFT;

    fun isAppCentric() = ordinal>=5

    fun toScreenCentric(): ScreenPos = enumValues<ScreenPos>()[ordinal%5]

    fun toAppCentric(): ScreenPos = enumValues<ScreenPos>()[5+ordinal%5]

    fun <N: Node> computeXY(popup: PopOver<N>) = P(computeX(popup), computeY(popup))

    private fun <N: Node> computeX(popup: PopOver<N>): Double {
        val width = popup.skinn.node.width
        val screen = if (isAppCentric()) null else getScreenForMouse().bounds
        val window = APP.windowManager.getFocused().orNull()?.stage
        return when (this) {
            APP_TOP_LEFT, APP_BOTTOM_LEFT -> window?.x ?: SCREEN_BOTTOM_LEFT.computeX(popup)
            APP_TOP_RIGHT, APP_BOTTOM_RIGHT -> window?.let { it.x+it.width-width } ?: SCREEN_BOTTOM_RIGHT.computeX(popup)
            APP_CENTER -> window?.let { it.centreX-width/2 } ?: SCREEN_CENTER.computeX(popup)
            SCREEN_TOP_LEFT, SCREEN_BOTTOM_LEFT -> screen!!.minX+GAP
            SCREEN_TOP_RIGHT, SCREEN_BOTTOM_RIGHT -> screen!!.maxX-width-GAP
            SCREEN_CENTER -> screen!!.centreX-width/2
        }
    }

    private fun <N: Node> computeY(popup: PopOver<N>): Double {
        val height = popup.skinn.node.height
        val screen = if (isAppCentric()) null else getScreenForMouse().bounds
        val window = APP.windowManager.getFocused().orNull()
        return when (this) {
            APP_BOTTOM_LEFT, APP_BOTTOM_RIGHT -> window?.let { it.y+it.height-height } ?: SCREEN_BOTTOM_RIGHT.computeY(popup)
            APP_TOP_LEFT, APP_TOP_RIGHT -> window?.y ?: SCREEN_TOP_RIGHT.computeY(popup)
            APP_CENTER -> window?.let { it.centreY-height/2 } ?: SCREEN_CENTER.computeY(popup)
            SCREEN_BOTTOM_LEFT, SCREEN_BOTTOM_RIGHT -> screen!!.maxY-height-GAP
            SCREEN_TOP_LEFT, SCREEN_TOP_RIGHT -> screen!!.minY+GAP
            SCREEN_CENTER -> screen!!.centreY-height/2
        }
    }
}


/** Screen area picking strategy for popover position. Decides a rectangular screen area for popup positioning. */
enum class ScreenUse {
    /** Area of a main screen will always be picked. */
    MAIN,
    /** Area of popover's window owner's screen will be picked. This screen contains the window's centre. */
    APP_WINDOW,
    /**
     * All screens will be used. Resulting area is a rectangle ranging from
     * the left most screen's left edge and topmost screen's top edge to
     * rightmost screen's right edge and bottom-most screen's bottom edge.
     */
    ALL;

    /** @return rectangular screen area. */
    fun getScreenArea(w: Window?, pos: ScreenPos): Rectangle2D? {
        val ps = Screen.getPrimary()
        val psb = ps.bounds

        if (this==MAIN)
            return ps.bounds
        if (this==APP_WINDOW) {
            val s = if (w==null) getScreenForMouse() else w.screen
            return s.bounds
        }

        val ss = Screen.getScreens()
        val left = ss.asSequence().map { f -> f.bounds }.minBy { b -> b.minX } ?: psb
        val right = ss.asSequence().map { f -> f.bounds }.maxBy { b -> b.maxX } ?: psb
        return when (pos) {
            ScreenPos.SCREEN_BOTTOM_LEFT, ScreenPos.SCREEN_TOP_LEFT -> left
            ScreenPos.SCREEN_BOTTOM_RIGHT, ScreenPos.SCREEN_TOP_RIGHT -> right
            ScreenPos.SCREEN_CENTER -> {
                val top = ss.asSequence().map { f -> f.bounds }.minBy { b -> b.minY } ?: psb
                val bottom = ss.asSequence().map { f -> f.bounds }.maxBy { b -> b.maxY } ?: psb
                Rectangle2D(left.minX, top.minY, right.maxX-left.minX, bottom.maxY-top.minY)
            }
            else -> null
        }
    }
}
