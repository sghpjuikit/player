package sp.it.pl.util.graphics

import de.jensd.fx.glyphs.GlyphIcons
import javafx.css.PseudoClass
import javafx.event.EventHandler
import javafx.geometry.Bounds
import javafx.geometry.Insets
import javafx.geometry.Point2D
import javafx.geometry.Rectangle2D
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseDragEvent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.Border
import javafx.scene.layout.BorderStroke
import javafx.scene.layout.BorderStrokeStyle
import javafx.scene.layout.BorderWidths
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.scene.text.Font
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
import javafx.scene.text.Text
import javafx.stage.Screen
import org.reactfx.Subscription
import sp.it.pl.gui.objects.image.Thumbnail
import sp.it.pl.gui.objects.window.stage.Window
import sp.it.pl.util.math.P
import sp.it.pl.util.reactive.sync
import java.awt.MouseInfo
import java.awt.Point

/* ---------- CONSTRUCTION ------------------------------------------------------------------------------------------ */

/** @return color with same r,g,b values but specified opacity */
fun Color.alpha(opacity: Double): Color {
    return Color(red, green, blue, opacity)
}

/** @return simple background with specified solid fill color and no radius or insets */
fun bgr(c: Color) = Background(BackgroundFill(c, CornerRadii.EMPTY, Insets.EMPTY))

/** @return simple border with specified color, solid style, no radius and default width */
@JvmOverloads
fun border(c: Color, radius: CornerRadii = CornerRadii.EMPTY) = Border(BorderStroke(c, BorderStrokeStyle.SOLID, radius, BorderWidths.DEFAULT))

fun pseudoclass(name: String) = PseudoClass.getPseudoClass(name)!!

@JvmOverloads fun createIcon(icon: GlyphIcons, iconSize: Double? = null) = Text(icon.characterToString()).apply {
    val fontSize = iconSize?.let { it.EM } ?: 1.0
    style = "-fx-font-family: ${icon.fontFamily}; -fx-font-size: ${fontSize}em;"
    styleClass += "icon"
}

fun createIcon(icon: GlyphIcons, icons: Int, iconSize: Double? = null): Text {
    val fontSize = iconSize?.let { it.EM } ?: 1.0
    val s = icon.characterToString()
    val sb = StringBuilder(icons)
    for (i in 0 until icons) sb.append(s)

    return Text(sb.toString()).apply {
        style = "-fx-font-family: ${icon.fontFamily}; -fx-font-size: ${fontSize}em;"
        styleClass += "icon"
    }
}

inline fun hBox(initialization: HBox.() -> Unit) = HBox().apply { initialization() }

inline fun vBox(initialization: VBox.() -> Unit) = VBox().apply { initialization() }

/* ---------- LAYOUT ------------------------------------------------------------------------------------------------ */

/** @return true iff this is direct parent of the specified node */
fun Node.isParentOf(child: Node) = child.parent==this

/** @return true iff this is direct or indirect parent of the specified node */
fun Node.isAnyParentOf(child: Node) = generateSequence(child, { it.parent }).any { isParentOf(it) }

/** @return true iff this is direct child of the specified node */
fun Node.isChildOf(parent: Node) = parent.isParentOf(this)

/** @return true iff this is direct or indirect child of the specified node */
fun Node.isAnyChildOf(parent: Node) = parent.isAnyParentOf(this)

/** @return this or direct or indirect parent of this that passes specified filter or null if no element passes */
fun Node.findParent(filter: (Node) -> Boolean) = generateSequence(this, { it.parent }).find(filter)

/** Removes this from the parent's children if possible. */
fun Node?.removeFromParent(parent: Node?) {
    if (parent==null || this==null) return
    (parent as? Pane)?.children?.remove(this)
}

/** Removes this from its parent's children if possible. */
fun Node?.removeFromParent() = this?.removeFromParent(parent)

/** Convenience for [AnchorPane.getTopAnchor] & [AnchorPane.setTopAnchor]. */
var Node.topAnchor: Double?
    get() = AnchorPane.getTopAnchor(this)
    set(it) {
        AnchorPane.setTopAnchor(this, it)
    }

/** Convenience for [AnchorPane.getLeftAnchor] & [AnchorPane.setLeftAnchor]. */
var Node.leftAnchor: Double?
    get() = AnchorPane.getLeftAnchor(this)
    set(it) {
        AnchorPane.setLeftAnchor(this, it)
    }

/** Convenience for [AnchorPane.getRightAnchor] & [AnchorPane.setRightAnchor]. */
var Node.rightAnchor: Double?
    get() = AnchorPane.getRightAnchor(this)
    set(it) {
        AnchorPane.setRightAnchor(this, it)
    }

/** Convenience for [AnchorPane.getBottomAnchor] & [AnchorPane.setBottomAnchor]. */
var Node.bottomAnchor: Double?
    get() = AnchorPane.getBottomAnchor(this)
    set(it) {
        AnchorPane.setBottomAnchor(this, it)
    }

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

/**
 * Sets minimal, preferred and maximal width and height of this element to provided values.
 * Any bound property will be ignored. Null value will be ignored.
 */
@JvmOverloads
fun Node.setMinPrefMaxSize(width: Double?, height: Double? = width) {
    setMinPrefMaxWidth(width)
    setMinPrefMaxHeight(height)
}

/**
 * Sets minimal, preferred and maximal width of the node to provided value.
 * Any bound property will be ignored. Null value will be ignored.
 */
fun Node.setMinPrefMaxWidth(width: Double?) {
    if (width!=null && this is Region) {
        if (!minWidthProperty().isBound) minWidth = width
        if (!prefWidthProperty().isBound) prefWidth = width
        if (!maxWidthProperty().isBound) maxWidth = width
    }
}

/**
 * Sets minimal, preferred and maximal height of the node to provided value.
 * Any bound property will be ignored. Null value will be ignored.
 */
fun Node.setMinPrefMaxHeight(height: Double?) {
    if (height!=null && this is Region) {
        if (!minHeightProperty().isBound) minHeight = height
        if (!prefHeightProperty().isBound) prefHeight = height
        if (!maxHeightProperty().isBound) maxHeight = height
    }
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
    val by = (percent*pxTo+(1.0-percent)*pxFrom)
    if (b.width>0.0 && b.height>0.0) {
        scaleX = 1+by/b.width
        scaleY = 1+by/b.height
    } else {
        scaleX = 1.0
        scaleY = 1.0
    }
}

/* ---------- CLIP -------------------------------------------------------------------------------------------------- */

/** Installs clip mask to prevent displaying content outside of this node. */
@JvmOverloads fun Node.initClip(padding: Insets = Insets.EMPTY) {
    val clip = Rectangle()

    layoutBoundsProperty() sync {
        clip.x = padding.left
        clip.y = padding.top
        clip.width = it.width-padding.left-padding.right
        clip.height = it.height-padding.top-padding.bottom
    }

    setClip(clip)
}

fun ImageView.applyViewPort(i: Image?, fit: Thumbnail.FitFrom) {
    if (i!=null) {
        when (fit) {
            Thumbnail.FitFrom.INSIDE -> viewport = null
            Thumbnail.FitFrom.OUTSIDE -> {
                val ratioIMG = i.width/i.width
                val ratioTHUMB = layoutBounds.width/layoutBounds.height
                when {
                    ratioTHUMB<ratioIMG -> {
                        val uiImgWidth = i.height*ratioTHUMB
                        val x = (i.width-uiImgWidth)/2
                        viewport = Rectangle2D(x, 0.0, uiImgWidth, i.height)
                    }
                    ratioTHUMB>ratioIMG -> {
                        val uiImgHeight = i.width/ratioTHUMB
                        val y = (i.height-uiImgHeight)/2
                        viewport = Rectangle2D(0.0, y, i.width, uiImgHeight)
                    }
                    ratioTHUMB==ratioIMG -> viewport = null
                }
            }
        }
    }
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

/** @return size of the bounds represented as point */
val Region.size get() = P(width, height)

/** @return window-relative position of the centre of this window */
val Window.centre get() = P(centreX, centreY)

/** @return window-relative x position of the centre of this window */
val Window.centreX get() = x+width/2

/** @return window-relative y position of the centre of this window */
val Window.centreY get() = y+height/2

/** @return window-relative position of the centre of this window */
val javafx.stage.Window.centre get() = P(centreX, centreY)

/** @return window-relative x position of the centre of this window */
val javafx.stage.Window.centreX get() = x+width/2

/** @return window-relative y position of the centre of this window */
val javafx.stage.Window.centreY get() = y+height/2

/** @return size of the bounds represented as point */
val Rectangle2D.size get() = P(width, height)

/** @return rectangle-relative position of the centre of this rectangle */
val Rectangle2D.centre get() = P(centreX, centreY)

/** @return rectangle-relative x position of the centre of this rectangle */
val Rectangle2D.centreX get() = minX+width/2

/** @return rectangle-relative y position of the centre of this rectangle */
val Rectangle2D.centreY get() = minY+height/2

operator fun Point2D.minus(p: Point2D): Point2D = subtract(p)!!
operator fun Point2D.plus(p: Point2D): Point2D = add(p)!!
operator fun Point2D.times(p: Double): Point2D = multiply(p)!!
operator fun Point2D.div(p: Double): Point2D = Point2D(x/p, y/p)

operator fun P.minus(p: Point2D): P = P(x-p.x, y-p.y)
operator fun P.plus(p: Point2D): P = P(x+p.x, y+p.y)

fun Point.toP() = P(x.toDouble(), y.toDouble())
fun Point2D.toP() = P(x, y)

fun Node.screenToLocal(e: MouseEvent) = screenToLocal(e.screenX, e.screenY)!!
fun Node.sceneToLocal(e: MouseEvent) = sceneToLocal(e.sceneX, e.sceneY)!!

/* ---------- TEXT -------------------------------------------------------------------------------------------------- */

object EM {
    fun toDouble() = 12.0
}

/** @return value in [EM] units */
val Double.EM get() = this/sp.it.pl.util.graphics.EM.toDouble()

/** @return value in [EM] units */
val Int.EM get() = this/sp.it.pl.util.graphics.EM.toDouble()

/** Sets font, overriding css style. */
fun Parent.setFontAsStyle(font: Font) {
    val tmp = font.style.toLowerCase()
    val style = if (tmp.contains("italic")) FontPosture.ITALIC else FontPosture.REGULAR
    val weight = if (tmp.contains("bold")) FontWeight.BOLD else FontWeight.NORMAL
    val styleS = if (style==FontPosture.ITALIC) "italic" else "normal"
    val weightS = if (weight==FontWeight.BOLD) "bold" else "normal"
    setStyle(
            "-fx-font-family: \"${font.family}\";"+
            "-fx-font-style: $styleS;"+
            "-fx-font-weight: $weightS;"+
            "-fx-font-size: ${font.size};"
    )
}

/**
 * @param text text to interpolate from 0 to full length
 * @param padLength 'empty' value with which to pad the result to preserve original length or null to not preserve it.
 * @return linear text interpolator computing substrings of specified text from beginning
 */
@JvmOverloads fun typeText(text: String, padLength: String? = null): (Double) -> String {
    val length = text.length
    val sbOriginal = StringBuilder(text)
    fun mapper(c: Char) = if (c.isWhitespace()) c.toString() else (padLength ?: c.toString())

    var lengthsSum = 0
    val lengths = IntArray(text.length) { i -> lengthsSum += mapper(text[i]).length; lengthsSum-mapper(text[i]).length }

    val sbInterpolated = StringBuilder(lengthsSum)
    generateSequence(0) { it+1 }.take(sbOriginal.length).forEach { sbInterpolated.append(mapper(sbOriginal[it])) }

    return if (padLength!=null) { {
        val i = Math.floor(length*it).toInt().coerceIn(0 until text.length)
        sbOriginal.substring(0, i+1) + sbInterpolated.substring(lengths[i])
    } } else { {
        val i = Math.floor(length*it).toInt().coerceIn(0 until text.length)
        sbOriginal.substring(0, i+1)
    } }
}

/* ---------- TREE VIEW --------------------------------------------------------------------------------------------- */

fun <T> TreeItem<T>.expandToRoot() = generateSequence(this, { it.parent }).forEach { it.setExpanded(true) }

fun <T> TreeView<T>.expandAndSelect(item: TreeItem<T>) {
    item.expandToRoot()
    selectionModel.select(item)
}

/** Bypass consuming ESCAPE key events, which [TreeView] does by default. */
fun TreeView<*>.propagateESCAPE() {
    addEventHandler(KeyEvent.ANY, { e ->
        if (editingItem==null && e.code==KeyCode.ESCAPE) {
            parent?.fireEvent(e)
            e.consume()
        }
    })
}

/* ---------- EVENT ------------------------------------------------------------------------------------------------- */

/**
 * Sets an action to execute when this node is hovered or dragged with mouse.
 * More reliable than [MouseEvent.MOUSE_ENTERED]. Use in combination with [Node.onHoverOrDragEnd].
 */
fun Node.onHoverOrDragStart(onStart: () -> Unit): Subscription {
    if (isHover) onStart()

    val onMouseEnteredEH = EventHandler<MouseEvent> {
        if (properties["isHoverOrDrag"]!=true)
            onStart()
    }
    val onDragEnteredEH = EventHandler<MouseEvent> {
        properties["isHoverOrDrag"] = true
        if (!isHover)
            onStart()
    }

    addEventFilter(MouseEvent.MOUSE_ENTERED, onMouseEnteredEH)
    addEventFilter(MouseEvent.DRAG_DETECTED, onDragEnteredEH)

    return Subscription {
        removeEventFilter(MouseEvent.MOUSE_ENTERED, onMouseEnteredEH)
        removeEventFilter(MouseEvent.DRAG_DETECTED, onDragEnteredEH)
    }
}

/**
 * Sets an action to execute when hover or drag with mouse on this node ends
 * More reliable than [MouseEvent.MOUSE_EXITED]. Use in combination with [Node.onHoverOrDragStart].
 */
fun Node.onHoverOrDragEnd(onEnd: () -> Unit): Subscription {
    val onMouseExitedEH = EventHandler<MouseEvent> {
        if (properties["isHoverOrDrag"]!=true)
            onEnd()
    }
    val onMouseDragReleasedEH = EventHandler<MouseDragEvent> {
        properties["isHoverOrDrag"] = false
        if (!isHover)
            onEnd()
    }
    val onMouseReleasedEH = EventHandler<MouseEvent> {
        properties["isHoverOrDrag"] = false
        if (!isHover)
            onEnd()
    }

    addEventFilter(MouseEvent.MOUSE_EXITED, onMouseExitedEH)
    addEventFilter(MouseDragEvent.MOUSE_DRAG_RELEASED, onMouseDragReleasedEH)
    addEventFilter(MouseEvent.MOUSE_RELEASED, onMouseReleasedEH)

    return Subscription {
        removeEventFilter(MouseEvent.MOUSE_EXITED, onMouseExitedEH)
        removeEventFilter(MouseDragEvent.MOUSE_DRAG_RELEASED, onMouseDragReleasedEH)
        removeEventFilter(MouseEvent.MOUSE_RELEASED, onMouseReleasedEH)
    }

}

/* ---------- SCREEN ------------------------------------------------------------------------------------------------ */

/** @return the latest mouse position */
fun getMousePosition(): Point2D {
    val pi = MouseInfo.getPointerInfo()        // TODO: this can be null sometimes, investigate & fix
    val p = pi?.location ?: Point(0, 0)
    return Point2D(p.getX(), p.getY())
}

/** @return screen containing this point */
fun P.getScreen() = getScreen(x, y)

/** @return screen containing this point */
fun Point.getScreen() = getScreen(x.toDouble(), y.toDouble())

/** @return screen containing the given coordinates */
fun getScreen(x: Double, y: Double) = Screen.getScreens().find { it.bounds.intersects(x, y, 1.0, 1.0) }
        ?: Screen.getPrimary()!!

/** @return screen containing the given coordinates */
fun getScreenForMouse() = getMousePosition().toP().getScreen()

/** @return index of the screen as reported by the underlying os */
val Screen.ordinal: Int
    get() =
    // indexOf() assumption is supported by the ordinals matching screen order, see:
    // com.sun.glass.ui.Screen.getScreens().forEach { s -> println(s.getAdapterOrdinal()+" - "+s.getWidth()+"x"+s.getHeight()) }
    // Screen.getScreens().forEach { s -> println("1"+" - "+s.getBounds().getWidth()+"x"+s.getBounds().getHeight()) }
        Screen.getScreens().indexOf(this)+1

/** @return screen containing the centre of this window */
val javafx.stage.Window.screen: Screen get() = getScreen(centreX, centreY)