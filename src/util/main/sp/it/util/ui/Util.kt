package sp.it.util.ui

import de.jensd.fx.glyphs.GlyphIcons
import javafx.css.PseudoClass
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Bounds
import javafx.geometry.Insets
import javafx.geometry.Point2D
import javafx.geometry.Pos
import javafx.geometry.Rectangle2D
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.control.Control
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.Menu
import javafx.scene.control.MenuItem
import javafx.scene.control.ScrollPane
import javafx.scene.control.SeparatorMenuItem
import javafx.scene.control.TableView
import javafx.scene.control.TextArea
import javafx.scene.control.Tooltip
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeTableView
import javafx.scene.control.TreeView
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.image.WritableImage
import javafx.scene.input.MouseDragEvent.MOUSE_DRAG_RELEASED
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.DRAG_DETECTED
import javafx.scene.input.MouseEvent.MOUSE_ENTERED
import javafx.scene.input.MouseEvent.MOUSE_EXITED
import javafx.scene.input.MouseEvent.MOUSE_RELEASED
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.Border
import javafx.scene.layout.BorderPane
import javafx.scene.layout.BorderStroke
import javafx.scene.layout.BorderStrokeStyle
import javafx.scene.layout.BorderWidths
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.robot.Robot
import javafx.scene.shape.Rectangle
import javafx.scene.text.Font
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
import javafx.scene.text.Text
import javafx.scene.text.TextAlignment
import javafx.stage.Screen
import javafx.stage.Window
import javafx.util.Callback
import sp.it.util.JavaLegacy
import sp.it.util.functional.asIf
import sp.it.util.functional.traverse
import sp.it.util.math.P
import sp.it.util.math.max
import sp.it.util.math.min
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.sync
import sp.it.util.ui.image.FitFrom
import kotlin.math.abs
import kotlin.math.floor

/* ---------- COLOR ------------------------------------------------------------------------------------------------- */

/** @return color with same r,g,b values but specified opacity */
fun Color.alpha(opacity: Double): Color {
   return Color(red, green, blue, opacity)
}

/* ---------- ICON -------------------------------------------------------------------------------------------------- */

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

/* ---------- NODE -------------------------------------------------------------------------------------------------- */

/** @return true iff this is direct parent of the specified node */
fun Node.isParentOf(child: Node) = child.parent===this

/** @return true iff this is direct or indirect parent of the specified node */
fun Node.isAnyParentOf(child: Node) = generateSequence(child) { it.parent }.drop(1).any { it===this }

/** @return true iff this is direct child of the specified node */
fun Node.isChildOf(parent: Node) = parent.isParentOf(this)

/** @return true iff this is direct or indirect child of the specified node */
fun Node.isAnyChildOf(parent: Node) = parent.isAnyParentOf(this)

/** @return this or direct or indirect parent of this that passes specified filter or null if no element passes */
fun Node.findParent(filter: (Node) -> Boolean) = generateSequence(this) { it.parent }.find(filter)

/** Removes this from its parent's children if possible (if parent is [Pane] or [Group]). Any child's focus is moved to parent. */
fun Node.removeFromParent() {
   val p = parent
   val hasFocusedChild = scene?.focusOwner?.isAnyChildOf(this) ?: false

   // Fixes possible usage of this node after removal from parent, because scene retains reference to focusOwner and
   // removes it when focus changes. Focus listener would invoke when this node is no longer part of scene graph.
   if (hasFocusedChild) p?.requestFocus()

   when (p) {
      is Group -> p.children -= this
      is Pane -> p.children -= this
   }
}

/** @return whether this node shape contains the scene coordinates represented by the specified point */
fun Node.containsScene(scenePoint: Point2D): Boolean = contains(sceneToLocal(scenePoint))

/** @return whether this node shape contains the scene coordinates */
fun Node.containsScene(sceneX: Double, sceneY: Double) = containsScene(Point2D(sceneX, sceneY))

/** @return whether this node shape contains the screen coordinates represented by the specified point */
fun Node.containsScreen(screenPoint: Point2D): Boolean = contains(screenToLocal(screenPoint))

/** @return whether this node shape contains the screen coordinates */
fun Node.containsScreen(screenX: Double, screenY: Double) = containsScene(Point2D(screenX, screenY))

/** @return whether this node shape contains the coordinates of the specified mouse event */
fun Node.containsMouse(event: MouseEvent) = containsScene(Point2D(event.sceneX, event.sceneY))

/** @return whether this node shape contains the coordinates of the mouse */
fun Node.containsMouse() = containsScreen(Robot().mousePosition)

/** @return topmost child node containing the specified scene coordinates optionally testing against the specified test or null if no match */
fun Node.pickTopMostAt(sceneX: Double, sceneY: Double, test: (Node) -> Boolean = { true }): Node? {
   // Groups need to be handled specially - they need to be transparent
   fun Node.isIn(sceneX: Double, sceneY: Double, test: (Node) -> Boolean) =
      if (parent is Group) test(this) && localToScene(layoutBounds).contains(sceneX, sceneY)
      else test(this) && sceneToLocal(sceneX, sceneY, true)?.let { it in this } ?: false

   return if (isIn(sceneX, sceneY, test)) {
      if (this is Parent) {
         for (i in childrenUnmodifiable.indices.reversed()) {
            val child = childrenUnmodifiable[i]

            // traverse into the group as if its children were this node's children
            if (child is Group) {
               for (j in child.childrenUnmodifiable.indices.reversed()) {
                  val ch = child.childrenUnmodifiable[j]
                  if (ch.isIn(sceneX, sceneY, test)) {
                     return ch.pickTopMostAt(sceneX, sceneY, test)
                  }
               }
            }

            if (child.isIn(sceneX, sceneY, test)) {
               return child.pickTopMostAt(sceneX, sceneY, test)
            }
         }
         return this
      } else {
         this
      }
   } else {
      null
   }
}

/** Convenience for [Node.pseudoClassStateChanged]. */
fun Node.pseudoClassChanged(pseudoClass: String, active: Boolean) = pseudoClassStateChanged(pseudoclass(pseudoClass), active)

/** Adds the specified styleclass to [Node.styleClass] of this node, if it has not yet been assigned. */
fun Node.styleclassAdd(styleClass: String) {
   if (styleClass !in this.styleClass)
      this.styleClass += styleClass
}

/** Adds (true) or removes (false) the specified styleclass using [Node.styleclassAdd] and [Node.styleclassRemove]. */
fun Node.styleclassToggle(styleClass: String, enabled: Boolean) {
   if (enabled) styleclassAdd(styleClass)
   else styleclassRemove(styleClass)
}

/** Removes all instances of the specified styleclass from [Node.styleClass] of this node. */
fun Node.styleclassRemove(styleClass: String) {
   this.styleClass.removeIf { it==styleClass }
}

/** Adds the specified stylesheets to [Parent.stylesheets] of this parent, if it has not yet been assigned. */
fun Parent.stylesheetAdd(stylesheet: String) {
   if (stylesheet !in this.stylesheets)
      this.stylesheets += stylesheet
}

/** Adds (true) or removes (false) the specified stylesheets using [Parent.stylesheetAdd] and [Parent.stylesheetRemove]. */
fun Parent.stylesheetToggle(stylesheet: String, enabled: Boolean) {
   if (enabled) stylesheetAdd(stylesheet)
   else stylesheetRemove(stylesheet)
}

/** Removes all instances of the specified stylesheets from [Parent.stylesheets] of this parent. */
fun Parent.stylesheetRemove(stylesheet: String) {
   this.stylesheets.removeIf { it==stylesheet }
}

/**
 * Disposer called in [Node.onNodeDispose].
 * 1st invocation of this property initializes the disposer and stores it in [Node.properties] with a unique key.
 */
val Node.onNodeDispose: Disposer
   get() = properties.getOrPut("onDispose_7bccf7a3-bcae-42ca-a91d-9f95217b942c") { Disposer() } as Disposer

/**
 * Disposes of this node, with the intention of it and all it's children to never again be used in the scene graph.
 *
 * If a disposer is initialized, it will be invoked. Use [Node.onNodeDispose] property to access, initialize and add
 * subscriptions to the underlying disposer.
 *
 * If this is [Control], [Control.skin] will be set to null (which will invoke [javafx.scene.control.Skin.dispose])
 *
 * If this is [Parent], this function will be invoked on each item in [Parent.getChildrenUnmodifiable]. Disposal
 * is recursive, in a top-down depth-first traversal.
 *
 * This is an optional dispose API and as such is not used nor called automatically by the JavaFx. It is up to the
 * developer to call it appropriately, but one may wish to call it immediately after a call to [Node.removeFromParent].
 */
fun Node.onNodeDispose() {
   properties["onDispose_7bccf7a3-bcae-42ca-a91d-9f95217b942c"].asIf<Disposer>()?.invoke()
   if (this is Control) skin = null
   if (this is Parent) childrenUnmodifiable.forEach { it.onNodeDispose() }
}

/* ---------- CONSTRUCTORS ------------------------------------------------------------------------------------------ */

/** @return simple background with specified solid fill color and no radius or insets */
fun bgr(color: Color) = Background(BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY))

/** @return simple border with specified color, solid style, no radius and default width */
@JvmOverloads
fun border(color: Color, radius: CornerRadii = CornerRadii.EMPTY) = Border(BorderStroke(color, BorderStrokeStyle.SOLID, radius, BorderWidths.DEFAULT))

/** @return [PseudoClass.getPseudoClass] */
fun pseudoclass(name: String) = PseudoClass.getPseudoClass(name)

inline fun pane(block: Pane.() -> Unit = {}) = Pane().apply(block)
inline fun pane(vararg children: Node, block: Pane.() -> Unit = {}) = Pane(*children).apply(block)
inline fun stackPane(block: StackPane.() -> Unit = {}) = StackPane().apply(block)
inline fun stackPane(vararg children: Node, block: StackPane.() -> Unit = {}) = StackPane(*children).apply { block() }
inline fun anchorPane(block: AnchorPane.() -> Unit = {}) = AnchorPane().apply(block)
inline fun hBox(spacing: Number = 0.0, alignment: Pos? = null, block: HBox.() -> Unit = {}) = HBox(spacing.toDouble()).apply { this.alignment = alignment; block() }
inline fun vBox(spacing: Number = 0.0, alignment: Pos? = null, block: VBox.() -> Unit = {}) = VBox(spacing.toDouble()).apply { this.alignment = alignment; block() }
inline fun scrollPane(block: ScrollPane.() -> Unit = {}) = ScrollPane().apply(block)
inline fun scrollText(block: () -> Text) = Util.layScrollVText(block())!!
inline fun scrollTextCenter(block: () -> Text) = Util.layScrollVTextCenter(block())!!
inline fun borderPane(block: BorderPane.() -> Unit = {}) = BorderPane().apply(block)
inline fun label(text: String = "", block: Label.() -> Unit = {}) = Label(text).apply(block)
inline fun button(text: String = "", block: Button.() -> Unit = {}) = Button(text).apply(block)
inline fun text(text: String = "", block: Text.() -> Unit = {}) = Text(text).apply(block)
inline fun menu(text: String, graphics: Node? = null, block: (Menu).() -> Unit = {}) = Menu(text, graphics).apply(block)
inline fun menuItem(text: String, crossinline action: (ActionEvent) -> Unit) = MenuItem(text).apply { onAction = EventHandler { action(it) } }
inline fun menuSeparator(block: (SeparatorMenuItem).() -> Unit = {}) = SeparatorMenuItem().apply(block)
inline fun <T> listView(block: (ListView<T>).() -> Unit = {}) = ListView<T>().apply(block)
inline fun <T> tableView(block: (TableView<T>).() -> Unit = {}) = TableView<T>().apply(block)
inline fun <T> treeView(block: (TreeView<T>).() -> Unit = {}) = TreeView<T>().apply(block)
inline fun <T> treeTableView(block: (TreeTableView<T>).() -> Unit = {}) = TreeTableView<T>().apply(block)
inline fun <T> listViewCellFactory(crossinline cellFactory: ListCell<T>.(T, Boolean) -> Unit) = Callback<ListView<T>, ListCell<T>> {
   object: ListCell<T>() {
      @Suppress("PROTECTED_CALL_FROM_PUBLIC_INLINE")
      override fun updateItem(item: T, empty: Boolean) {
         super.updateItem(item, empty)
         cellFactory(item, empty)
      }
   }
}

/* ---------- LAYOUT ------------------------------------------------------------------------------------------------ */

interface Lay {
   /** Lays the specified child onto this */
   operator fun plusAssign(child: Node)

   /** Lays the specified children onto this */
   operator fun plusAssign(children: Collection<Node>) = children.forEach { this += it }

   /** Lays the specified children onto this */
   operator fun plusAssign(children: Sequence<Node>) = children.forEach { this += it }

   /**
    * Lays the child produced by the specified block onto this if block is not null. Allows conditional content using
    * [sp.it.util.functional.supplyIf] and [sp.it.util.functional.supplyUnless].
    */
   operator fun plusAssign(child: (() -> Node)?) {
      if (child!=null) plusAssign(child())
   }
}

class PaneLay(private val pane: Pane): Lay {

   override fun plusAssign(child: Node) {
      pane.children += child
   }
}

class HBoxLay(private val pane: HBox): Lay {

   override fun plusAssign(child: Node) {
      pane.children += child
   }

   operator fun invoke(priority: Priority): Lay = object: Lay {
      override fun plusAssign(child: Node) {
         this@HBoxLay += child
         HBox.setHgrow(child, priority)
      }
   }
}

class VBoxLay(private val pane: VBox): Lay {

   override fun plusAssign(child: Node) {
      pane.children += child
   }

   operator fun invoke(priority: Priority): Lay = object: Lay {
      override fun plusAssign(child: Node) {
         this@VBoxLay += child
         VBox.setVgrow(child, priority)
      }
   }
}

class StackLay(private val pane: StackPane): Lay {

   override fun plusAssign(child: Node) {
      pane.children += child
   }

   operator fun invoke(alignment: Pos): Lay = object: Lay {
      override fun plusAssign(child: Node) {
         this@StackLay += child
         StackPane.setAlignment(child, alignment)
      }
   }

   operator fun invoke(alignment: Pos, margin: Insets): Lay = object: Lay {
      override fun plusAssign(child: Node) {
         this@StackLay += child
         StackPane.setAlignment(child, alignment)
         StackPane.setMargin(child, margin)
      }
   }
}

class AnchorPaneLay(private val pane: AnchorPane): Lay {

   override fun plusAssign(child: Node) {
      pane.children += child
   }

   operator fun invoke(topRightBottomLeft: Number?) = invoke(topRightBottomLeft, topRightBottomLeft, topRightBottomLeft, topRightBottomLeft)

   operator fun invoke(top: Number?, right: Number?, bottom: Number?, left: Number?): Lay = object: Lay {
      override fun plusAssign(child: Node) {
         Util.setAnchor(pane, child, top?.toDouble(), right?.toDouble(), bottom?.toDouble(), left?.toDouble())
      }
   }
}

val Pane.lay get() = PaneLay(this)
val HBox.lay get() = HBoxLay(this)
val VBox.lay get() = VBoxLay(this)
val StackPane.lay get() = StackLay(this)
val AnchorPane.lay get() = AnchorPaneLay(this)
val AnchorPane.layFullArea get() = AnchorPaneLay(this)(0.0)

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

/* ---------- SIZE -------------------------------------------------------------------------------------------------- */

fun Region.setMinWidth(minWidth: Number) = setMinWidth(minWidth.toDouble())
fun Region.setMinHeight(minHeight: Number) = setMinHeight(minHeight.toDouble())
fun Region.setMinSize(minWidth: Number, minHeight: Number) = setMinSize(minWidth.toDouble(), minHeight.toDouble())
fun Region.setMaxWidth(maxWidth: Number) = setMaxWidth(maxWidth.toDouble())
fun Region.setMaxHeight(maxHeight: Number) = setMaxHeight(maxHeight.toDouble())
fun Region.setMaxSize(maxWidth: Number, maxHeight: Number) = setMaxSize(maxWidth.toDouble(), maxHeight.toDouble())
fun Region.setPrefWidth(prefWidth: Number) = setPrefWidth(prefWidth.toDouble())
fun Region.setPrefHeight(prefHeight: Number) = setPrefHeight(prefHeight.toDouble())
fun Region.setPrefSize(prefWidth: Number, prefHeight: Number) = setPrefSize(prefWidth.toDouble(), prefHeight.toDouble())

/**
 * Sets minimal, preferred and maximal width and height of this element to provided values.
 * Any bound property will be ignored. Null value will be ignored.
 */
@JvmOverloads
fun Node.setMinPrefMaxSize(width: Double?, height: Double? = width) {
   minPrefMaxWidth = width
   minPrefMaxHeight = height
}

/**
 * Sets minimal, preferred and maximal width of the node to provided value.
 * Any bound property will be ignored. Null value will be ignored.
 */
var Node.minPrefMaxWidth: Double?
   @Deprecated("Write only") get() = null
   set(width) {
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
var Node.minPrefMaxHeight: Double?
   @Deprecated("Write only") get() = null
   set(height) {
      if (height!=null && this is Region) {
         if (!minHeightProperty().isBound) minHeight = height
         if (!prefHeightProperty().isBound) prefHeight = height
         if (!maxHeightProperty().isBound) maxHeight = height
      }
   }

@JvmOverloads
fun Node.setScaleXY(x: Double, y: Double = x) {
   scaleX = x
   scaleY = y
}

fun Node.setScaleXYByTo(percent: Double, pxFrom: Double, pxTo: Double) {
   val b = boundsInLocal
   val by = (percent*pxTo + (1.0 - percent)*pxFrom)
   if (b.width>0.0 && b.height>0.0) {
      scaleX = 1 + by/b.width max 0.0
      scaleY = 1 + by/b.height max 0.0
   } else {
      scaleX = 1.0
      scaleY = 1.0
   }
}

/* ---------- CLIP -------------------------------------------------------------------------------------------------- */

/** Installs clip mask to prevent displaying content outside of this node. */
@JvmOverloads
fun Node.initClip(padding: Insets = Insets.EMPTY) {
   val clip = Rectangle()

   layoutBoundsProperty() sync {
      clip.x = padding.left
      clip.y = padding.top
      clip.width = it.width - padding.left - padding.right
      clip.height = it.height - padding.top - padding.bottom
   }

   setClip(clip)
}

/* ---------- IMAGE_VIEW -------------------------------------------------------------------------------------------- */

fun ImageView.applyViewPort(i: Image?, fit: FitFrom) {
   if (i!=null) {
      when (fit) {
         FitFrom.INSIDE -> viewport = null
         FitFrom.OUTSIDE -> {
            val ratioIMG = i.width/i.height
            val ratioTHUMB = layoutBounds.width/layoutBounds.height
            when {
               ratioTHUMB<ratioIMG -> {
                  val uiImgWidth = i.height*ratioTHUMB
                  val x = abs(i.width - uiImgWidth)/2.0
                  viewport = Rectangle2D(x, 0.0, uiImgWidth, i.height)
               }
               ratioTHUMB>ratioIMG -> {
                  val uiImgHeight = i.width/ratioTHUMB
                  val y = abs(i.height - uiImgHeight)/2
                  viewport = Rectangle2D(0.0, y, i.width, uiImgHeight)
               }
               ratioTHUMB==ratioIMG -> viewport = null
            }
         }
      }
   }
}

/* ---------- TOOLTIP ----------------------------------------------------------------------------------------------- */

/** Equivalent to [Tooltip.install]. */
@Suppress("DEPRECATION")
infix fun Node.install(tooltip: Tooltip) = Tooltip.install(this, tooltip)

/** Equivalent to [Tooltip.uninstall]. */
@Suppress("DEPRECATION")
infix fun Node.uninstall(tooltip: Tooltip) = Tooltip.uninstall(this, tooltip)

/* ---------- MENU -------------------------------------------------------------------------------------------------- */

/** Create and add to items menu with specified text and graphics. */
inline fun Menu.menu(text: String, graphics: Node? = null, then: (Menu).() -> Unit) {
   items += Menu(text, graphics).apply { then() }
}

/** Create and add to items new menu item with specified text and action. */
fun Menu.item(text: String, action: (ActionEvent) -> Unit) = apply {
   items += menuItem(text, action)
}

/** Create and add to items new menu items with text and action derived from specified source. */
@Suppress("RedundantLambdaArrow")
fun <A> Menu.items(source: Sequence<A>, text: (A) -> String, action: (A) -> Unit) {
   items += source.map { menuItem(text(it)) { _ -> action(it) } }.sortedBy { it.text }
}

/** Create and add to items new menu separator. */
fun Menu.separator() = apply {
   items += menuSeparator()
}

/* ---------- POINT ------------------------------------------------------------------------------------------------- */

/** @return point `[this,y]` in [Double] */
infix fun Number.x(y: Number) = P(this.toDouble(), y.toDouble())

/** @return point `[this,this]` in [Double] */
val Number.x2 get() = P(this.toDouble(), this.toDouble())

/** Size of the bounds represented as point */
val Bounds.size get() = P(width, height)

/** Window-relative position of the centre of this window */
val Bounds.centre get() = P(centerX, centerY)

/** Left top point */
val Bounds.leftTop get() = P(minX, minY)

/** Right top point */
val Bounds.rightTop get() = P(maxX, minY)

/** Left bottom point */
val Bounds.leftBottom get() = P(minX, maxY)

/** Right bottom point */
val Bounds.rightBottom get() = P(maxX, maxY)

/** Size of the bounds represented as point */
val Region.size
   get() = P(width, height)

/** Min size represented as point */
var Region.minSize: P
   get() = P(minWidth, minHeight)
   set(v) {
      setMinSize(v.x, v.y)
   }

/** Pref size represented as point */
var Region.prefSize: P
   get() = P(prefWidth, prefHeight)
   set(v) {
      setPrefSize(v.x, v.y)
   }

/** Max size represented as point */
var Region.maxSize: P
   get() = P(maxWidth, maxHeight)
   set(v) {
      setMaxSize(v.x, v.y)
   }

/** Position using [javafx.stage.Window.x] and [javafx.stage.Window.y] */
val Window.xy get() = P(x, y)

/** Size using [javafx.stage.Window.width] and [javafx.stage.Window.height] */
var Window.size: P
   get() = P(width, height)
   set(value) {
      width = value.x
      height = value.y
   }

/** Window-relative position of the centre of this window */
val Window.centre get() = P(centreX, centreY)

/** Window-relative x position of the centre of this window */
val Window.centreX get() = x + width/2

/** Window-relative y position of the centre of this window */
val Window.centreY get() = y + height/2

/** Size of the bounds represented as point */
val Rectangle2D.size get() = P(width, height)

/** Rectangle-relative position of the centre of this rectangle */
val Rectangle2D.centre get() = P(centreX, centreY)

/** Rectangle-relative x position of the centre of this rectangle */
val Rectangle2D.centreX get() = minX + width/2

/** Rectangle-relative y position of the centre of this rectangle */
val Rectangle2D.centreY get() = minY + height/2

operator fun Point2D.minus(p: Point2D): Point2D = subtract(p)
operator fun Point2D.plus(p: Point2D): Point2D = add(p)
operator fun Point2D.times(p: Double): Point2D = multiply(p)
operator fun Point2D.div(p: Double): Point2D = Point2D(x/p, y/p)

operator fun P.minus(p: Point2D): P = P(x - p.x, y - p.y)
operator fun P.plus(p: Point2D): P = P(x + p.x, y + p.y)

fun Point2D.toP() = P(x, y)
fun P.toPoint2D() = Point2D(x, y)

fun Node.screenToLocal(e: MouseEvent) = screenToLocal(e.screenX, e.screenY)!!
fun Node.sceneToLocal(e: MouseEvent) = sceneToLocal(e.sceneX, e.sceneY)!!

/* ---------- TEXT -------------------------------------------------------------------------------------------------- */

/** Sets alignment of the text of this text area. */
fun TextArea.setTextAlignment(alignment: TextAlignment) {
   pseudoClassChanged("align-left", false)
   pseudoClassChanged("align-right", false)
   pseudoClassChanged("align-center", false)
   pseudoClassChanged("align-justify", false)
   pseudoClassChanged(
      when (alignment) {
         TextAlignment.LEFT -> "align-left"
         TextAlignment.RIGHT -> "align-right"
         TextAlignment.CENTER -> "align-center"
         TextAlignment.JUSTIFY -> "align-justify"
      },
      true
   )
}

object EM {
   fun toDouble() = 12.0
}

/** @return value in [EM] units */
val Number.EM get() = toDouble()/sp.it.util.ui.EM.toDouble()

/** Sets font, overriding css style. */
fun Parent.setFontAsStyle(font: Font) {
   val tmp = font.style.toLowerCase()
   val style = if (tmp.contains("italic")) FontPosture.ITALIC else FontPosture.REGULAR
   val weight = if (tmp.contains("bold")) FontWeight.BOLD else FontWeight.NORMAL
   val styleS = if (style==FontPosture.ITALIC) "italic" else "normal"
   val weightS = if (weight==FontWeight.BOLD) "bold" else "normal"
   setStyle(
      """
        -fx-font-family: "${font.family}";
        -fx-font-style: $styleS;
        -fx-font-weight: $weightS;
        -fx-font-size: ${font.size};
        """.trimIndent()
   )
}

/**
 * Create text interpolator for 'text type' animation.
 *
 * It is possible to preserve length of the string with the specified substitute character. It will substitute
 * non-whitespace characters. Note the possible issues with interfering with layout during an animation
 * relying on padded interpolation. It may be desirable to fix the graphical component size up front or
 * use a padding character of favorable size, `' '` (space) is generally more narrow than avg character, while`'\u2007'`
 * (wider unicode space) is wider.
 *
 * @param text text to interpolate from 0 to full length
 * @param padLength 'empty' value with which to pad the result to preserve original length or null to not preserve it.
 * @return linear text interpolator computing substrings of specified text from beginning
 */
@JvmOverloads
fun typeText(text: String, padLength: Char? = null): (Double) -> String {
   if (text.isEmpty()) return { "" }

   val length = text.length
   val sbOriginal = StringBuilder(text)
   if (padLength!=null) {
      val sbInterpolated = StringBuilder(text).apply {
         text.forEachIndexed { i, c ->
            this[i] = if (c.isWhitespace()) c else padLength
         }
      }
      return { it: Double ->
         val i = floor(length*it).toInt()
         sbOriginal.substring(0, i) + sbInterpolated.substring(i)
      }
   } else {
      return {
         val i = floor(length*it).toInt()
         sbOriginal.substring(0, i)
      }
   }
}

/* ---------- TREE VIEW --------------------------------------------------------------------------------------------- */

/** @return true iff this is direct parent of the specified tree item */
fun <T> TreeItem<T>.isParentOf(child: TreeItem<T>) = child.parent===this

/** @return true iff this is direct or indirect parent of the specified tree item */
fun <T> TreeItem<T>.isAnyParentOf(child: TreeItem<T>) = generateSequence(child) { it.parent }.drop(1).any { it===this }

/** @return true iff this is direct child of the specified tree item */
fun <T> TreeItem<T>.isChildOf(parent: TreeItem<T>) = parent.isParentOf(this)

/** @return true iff this is direct or indirect child of the specified tree item */
fun <T> TreeItem<T>.isAnyChildOf(parent: TreeItem<T>) = parent.isAnyParentOf(this)

val <T> TreeItem<T>.root: TreeItem<T> get() = traverse { it.parent }.first()

fun <T> TreeItem<T>.expandToRoot() = generateSequence(this) { it.parent }.forEach { it.setExpanded(true) }

fun <T> TreeItem<T>.expandToRootAndSelect(tree: TreeView<in T>) = tree.expandToRootAndSelect(this)

@Suppress("UNCHECKED_CAST")
fun <T> TreeView<T>.expandToRootAndSelect(item: TreeItem<out T>) {
   item.expandToRoot()
   this.scrollToCenter(item as TreeItem<T>)
   selectionModel.clearAndSelect(getRow(item))
}

/** Scrolls to the row, so it is visible in the vertical center of the table. Does nothing if index out of bounds.  */
fun <T> TreeView<T>.scrollToCenter(i: Int) {
   var index = i
   val items = expandedItemCount
   if (index<0 || index>=items) return

   val fixedCellHeightNotSet = fixedCellSize==Region.USE_COMPUTED_SIZE
   if (fixedCellHeightNotSet) {
      scrollTo(i)
      // TODO: improve
   } else {
      val rows = height/fixedCellSize
      index -= (rows/2).toInt()
      index = 0 max index min items - rows.toInt() + 1
      scrollTo(index)
   }
}

/** Scrolls to the item, so it is visible in the vertical center of the table. Does nothing if item not in table.  */
fun <T> TreeView<T>.scrollToCenter(item: TreeItem<T>) {
   item.expandToRoot()
   generateSequence(item) { it.parent }.toList().asReversed()
   scrollToCenter(getRow(item))
}

/* ---------- EVENT ------------------------------------------------------------------------------------------------- */

/**
 * Sets an action to execute when this node is hovered or dragged with mouse.
 * More reliable than [MouseEvent.MOUSE_ENTERED]. Use in combination with [Node.onHoverOrDragEnd].
 */
fun Node.onHoverOrDragStart(onStart: () -> Unit): Subscription {
   if (isHover) onStart()

   return Subscription(
      onEventUp(MOUSE_ENTERED) {
         onStart()
      },
      onEventUp(DRAG_DETECTED) {
         properties["isHoverOrDrag"] = true
         if (!isHover)
            onStart()
      }
   )
}

/**
 * Sets an action to execute when hover or drag with mouse on this node ends
 * More reliable than [MouseEvent.MOUSE_EXITED]. Use in combination with [Node.onHoverOrDragStart].
 */
fun Node.onHoverOrDragEnd(onEnd: () -> Unit): Subscription = Subscription(
   onEventUp(MOUSE_EXITED) {
      onEnd()
   },
   onEventUp(MOUSE_DRAG_RELEASED) {
      properties["isHoverOrDrag"] = false
      if (!isHover)
         onEnd()
   },
   onEventUp(MOUSE_RELEASED) {
      properties["isHoverOrDrag"] = false
      if (!isHover)
         onEnd()
   }
)

/* ---------- SCREEN ------------------------------------------------------------------------------------------------ */

/** @return screen containing this point */
fun P.getScreen() = getScreen(x, y)

/** @return screen containing the given coordinates */
fun getScreen(x: Double, y: Double) = Screen.getScreens().find { it.bounds.contains(x, y) } ?: Screen.getPrimary()!!

/** @return screen containing the given coordinates */
fun getScreenForMouse() = Robot().mousePosition.toP().getScreen()

/** @return image representing actual content of this screen */
fun Screen.makeScreenShot(image: WritableImage? = null) = Robot().getScreenCapture(image, bounds)!!

/** @return index of the screen as reported by the underlying os */
val Screen.ordinal: Int get() = JavaLegacy.screenOrdinal(this)

/** @return screen containing the centre of this window */
val Window.screen: Screen get() = getScreen(centreX, centreY)