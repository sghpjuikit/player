package util.graphics

import gui.objects.window.stage.Window
import javafx.css.PseudoClass
import javafx.geometry.Bounds
import javafx.geometry.Point2D
import javafx.geometry.Rectangle2D
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.layout.AnchorPane
import javafx.scene.shape.Rectangle
import javafx.scene.text.Font
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
import javafx.stage.Screen
import util.math.P
import util.reactive.maintain
import java.awt.MouseInfo
import java.awt.Point
import java.util.function.Consumer

fun pseudoclass(name: String) = PseudoClass.getPseudoClass(name)!!

/** Convenience for [AnchorPane.getTopAnchor] & [AnchorPane.setTopAnchor]. */
var Node.topAnchor: Double?
    get() = AnchorPane.getTopAnchor(this)
    set(it) { AnchorPane.setTopAnchor(this, it) }

/** Convenience for [AnchorPane.getLeftAnchor] & [AnchorPane.setLeftAnchor]. */
var Node.leftAnchor: Double?
    get() = AnchorPane.getLeftAnchor(this)
    set(it) { AnchorPane.setLeftAnchor(this, it) }

/** Convenience for [AnchorPane.getRightAnchor] & [AnchorPane.setRightAnchor]. */
var Node.rightAnchor: Double?
    get() = AnchorPane.getRightAnchor(this)
    set(it) { AnchorPane.setRightAnchor(this, it) }

/** Convenience for [AnchorPane.getBottomAnchor] & [AnchorPane.setBottomAnchor]. */
var Node.bottomAnchor: Double?
    get() = AnchorPane.getBottomAnchor(this)
    set(it) { AnchorPane.setBottomAnchor(this, it) }

/** Sets [AnchorPane] anchors to the same value. Null clears all anchors. */
fun Node.setAnchors(a: Double?) {
    if (a==null) {
        AnchorPane.clearConstraints(this)
    } else {
        this.setAnchors(a, a, a, a)
    }
}

/** Sets [AnchorPane] anchors. Null clears the respective anchor. */
fun Node.setAnchors(top: Double?, right: Double?, bottom: Double?, left: Double?) {
    AnchorPane.setTopAnchor(this, top)
    AnchorPane.setRightAnchor(this, right)
    AnchorPane.setBottomAnchor(this, bottom)
    AnchorPane.setLeftAnchor(this, left)
}


fun Node.setScaleXY(xy: Double) {
    scaleX = xy
    scaleY = xy
}

fun Node.setScaleXY(x: Double, y: Double) {
    scaleX = x
    scaleY = y
}

fun Node.setScaleXYByTo(percent: Double, pxFrom: Double, pxTo: Double) {
    val b = boundsInLocal
    val by = (percent*pxTo + (1.0 - percent)*pxFrom)
    if (b.width>0.0 && b.height>0.0) {
        scaleX = 1+by/b.width
        scaleY = 1+by/b.height
    } else {
        scaleX = 1.0
        scaleY = 1.0
    }
}

/**
 * Text interpolator for 'text typing effect'. Creates function returning string substrings
 * of all lengths from 0 to string length. Linear and uses rounding (Math.floor).
 *
 * @return function transforming `<0,1>` double input into substrings of the provided string, from
 * beginning to character at the position best reflected by the input.
 */
fun typeText(text: String): (Double) -> String {
    val length = text.length
    return { text.substring(0, Math.floor(length*it).toInt()) }
}

/* ---------- CLIP -------------------------------------------------------------------------------------------------- */

fun Node.initClip() {
    val clip = Rectangle()
    layoutBoundsProperty().maintain(Consumer { size ->
        clip.width = size.width
        clip.height = size.height
    })
    setClip(clip)
}

/* ---------- POINT ------------------------------------------------------------------------------------------------- */

/** @return size of the bounds represented as point */
val Bounds.size get() = P(width, height)

/** @return left top point */
val Bounds.leftTop get() = P(minX, minY)

/** @return right top point */
val Bounds.rightTop get() = P(maxX, minY)

/** @return left bottom point */
val Bounds.leftBottom get() = P(minX, maxY)

/** @return right bottom point */
val Bounds.rightBottom get() = P(maxX, maxY)

/** @return window-relative position of the centre of this window */
val Window.centre get() = P(centreX, centreY)

/** @return window-relative x position of the centre of this window */
val Window.centreX get() = x + width/2

/** @return window-relative y position of the centre of this window */
val Window.centreY get() = y + height/2

/** @return window-relative position of the centre of this window */
val javafx.stage.Window.centre get() = P(centreX, centreY)

/** @return window-relative x position of the centre of this window */
val javafx.stage.Window.centreX get() = x + width/2

/** @return window-relative y position of the centre of this window */
val javafx.stage.Window.centreY get() = y + height/2

/** @return size of the bounds represented as point */
val Rectangle2D.size get() = P(width, height)

/** @return rectangle-relative position of the centre of this rectangle */
val Rectangle2D.centre get() = P(centreX, centreY)

/** @return rectangle-relative x position of the centre of this rectangle */
val Rectangle2D.centreX get() = minX + width/2

/** @return rectangle-relative y position of the centre of this rectangle */
val Rectangle2D.centreY get() = minY + height/2

/* ---------- FONT -------------------------------------------------------------------------------------------------- */

/** Sets font, overriding css style. */
fun Parent.setFontAsStyle(font: Font) {
    val tmp = font.style.toLowerCase()
    val style = if (tmp.contains("italic")) FontPosture.ITALIC else FontPosture.REGULAR
    val weight = if (tmp.contains("bold")) FontWeight.BOLD else FontWeight.NORMAL
    val styleS = if (style == FontPosture.ITALIC) "italic" else "normal"
    val weightS = if (weight == FontWeight.BOLD) "bold" else "normal"
    setStyle(
        "-fx-font-family: \"" + font.family+ "\";" +
        "-fx-font-style: " + styleS + ";" +
        "-fx-font-weight: " + weightS + ";" +
        "-fx-font-size: " + font.size+ ";"
    )
}

/* ---------- SCREEN ------------------------------------------------------------------------------------------------ */

/** @return the latest mouse position */
fun getMousePosition(): Point2D {
    val pi = MouseInfo.getPointerInfo()        // TODO: return Try, since this can be null sometimes for some reason
    val p = if (pi==null) Point(0, 0) else pi.location
    return Point2D(p.getX(), p.getY())
}

/** @return screen containing this point */
fun P.getScreen() = getScreen(x, y)

/** @return screen containing this point */
fun Point2D.getScreen() = getScreen(x, y)

/** @return screen containing this point */
fun Point.getScreen() = getScreen(x.toDouble(), y.toDouble())

/** @return screen containing the given coordinates */
    // See com.sun.javafx.util.Utils.getScreenForPoint(x, y);
fun getScreen(x: Double, y: Double) = Screen.getScreens().find { it.bounds.intersects(x, y, 1.0, 1.0) } ?: Screen.getPrimary()!!

/** @return screen containing the given coordinates */
fun getScreenForMouse() = getMousePosition().getScreen()

val javafx.stage.Window.screen: Screen get() = getScreen(centreX, centreY)