package gui.objects.popover

import javafx.geometry.Rectangle2D
import javafx.scene.Node
import javafx.stage.Screen
import javafx.stage.Window
import main.App
import util.graphics.P
import util.graphics.centreX
import util.graphics.centreY
import util.graphics.getScreenForMouse
import util.graphics.screen
import util.graphics.size


enum class NodePos {
    Center,
    UpLeft,
    UpCenter,
    UpRight,
    DownLeft,
    DownCenter,
    DownRight,
    RightUp,
    RightCenter,
    RightDown,
    LeftUp,
    LeftCenter,
    LeftDown;

    fun reverse() = when (this) {
        Center -> Center
        UpLeft -> DownRight
        UpCenter -> DownCenter
        UpRight -> DownLeft
        DownLeft -> UpRight
        DownCenter -> UpCenter
        DownRight -> UpLeft
        RightUp -> LeftDown
        RightCenter -> LeftCenter
        RightDown -> LeftUp
        LeftUp -> RightDown
        LeftCenter -> RightCenter
        LeftDown -> RightUp
    }

    fun computeXY(n: Node, popup: PopOver<*>) = P(computeX(n, popup), computeY(n, popup))+n.boundsInParent.size/2.0

    private fun computeX(n: Node, popup: PopOver<*>): Double {
        val w = popup.contentNode.boundsInParent.width
        val x = n.localToScreen(0.0, 0.0).x
        return when (this) {
            Center, DownCenter, UpCenter -> x+n.boundsInParent.width/2-w/2
            LeftCenter, LeftUp, LeftDown -> x-w
            RightCenter, RightUp, RightDown -> x+n.boundsInParent.width
            UpLeft, DownLeft -> x
            UpRight, DownRight -> x+n.boundsInParent.width-w
        }
    }

    private fun computeY(n: Node, popup: PopOver<*>): Double {
        val h = popup.contentNode.boundsInParent.height
        val y = n.localToScreen(0.0, 0.0).y
        return when (this) {
            UpRight, UpCenter, UpLeft -> y-h
            DownCenter, DownLeft, DownRight -> y+n.boundsInParent.height
            LeftUp, RightUp -> y
            Center, LeftCenter, RightCenter -> y+n.boundsInParent.height/2-h/2
            LeftDown, RightDown -> y+n.boundsInParent.height-h
        }
    }
}


// TODO: add css support for the gap value
// gap between screen border and the popover
// note that the practical value is (GAP-padding)/2 so if padding is 4 then real GAP will be 3
const val GAP = 9.0

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
        val w = popup.skinn.node.width
        val screen = if (isAppCentric()) null else getScreenForMouse().bounds
        //				 : APP.windowManager.getFocused().map(w -> w.getStage()).map(w -> popup.screen_preference.getScreenArea(w, this)).orElseGet(() -> getScreenForMouse().getBounds()); // alternative
        val app = App.APP.windowManager.main.orElse(null)
        return when (this) {
            APP_TOP_LEFT, APP_BOTTOM_LEFT -> app?.x ?: SCREEN_BOTTOM_LEFT.computeX(popup)
            APP_TOP_RIGHT, APP_BOTTOM_RIGHT -> app?.let { it.x+it.width-w } ?: SCREEN_BOTTOM_RIGHT.computeX(popup)
            APP_CENTER -> app?.let { it.centreX-w/2 } ?: SCREEN_CENTER.computeX(popup)
            SCREEN_TOP_LEFT, SCREEN_BOTTOM_LEFT -> screen!!.minX+GAP
            SCREEN_TOP_RIGHT, SCREEN_BOTTOM_RIGHT -> screen!!.maxX-w-GAP
            SCREEN_CENTER -> screen!!.centreX-w/2
        }
    }

    private fun <N: Node> computeY(popup: PopOver<N>): Double {
        val h = popup.skinn.node.height
        val screen = if (isAppCentric()) null else getScreenForMouse().bounds
        //				 : APP.windowManager.getFocused().map(w -> w.getStage()).map(w -> popup.screen_preference.getScreenArea(w, this)).orElseGet(() -> getScreenForMouse().getBounds()); // alternative
        val app = App.APP.windowManager.main.orElse(null)
        return when (this) {
            APP_BOTTOM_LEFT, APP_BOTTOM_RIGHT -> app?.let { it.y+it.height-h } ?: SCREEN_BOTTOM_RIGHT.computeY(popup)
            APP_TOP_LEFT, APP_TOP_RIGHT -> app?.y ?: SCREEN_TOP_RIGHT.computeY(popup)
            APP_CENTER -> app?.let { it.centreY-h/2 } ?: SCREEN_CENTER.computeY(popup)
            SCREEN_BOTTOM_LEFT, SCREEN_BOTTOM_RIGHT -> screen!!.maxY-h-GAP
            SCREEN_TOP_LEFT, SCREEN_TOP_RIGHT -> screen!!.minY+GAP
            SCREEN_CENTER -> screen!!.centreY-h/2
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
