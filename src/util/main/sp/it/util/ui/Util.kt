package sp.it.util.ui

import de.jensd.fx.glyphs.GlyphIcons
import javafx.css.PseudoClass
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Bounds
import javafx.geometry.HPos
import javafx.geometry.Insets
import javafx.geometry.Point2D
import javafx.geometry.Pos
import javafx.geometry.Rectangle2D
import javafx.geometry.VPos
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ContextMenu
import javafx.scene.control.Control
import javafx.scene.control.Hyperlink
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.Menu
import javafx.scene.control.MenuItem
import javafx.scene.control.ScrollPane
import javafx.scene.control.SeparatorMenuItem
import javafx.scene.control.SplitPane
import javafx.scene.control.TableView
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.control.Tooltip
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeTableView
import javafx.scene.control.TreeView
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.image.WritableImage
import javafx.scene.input.MouseDragEvent.MOUSE_DRAG_EXITED
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
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.FlowPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.RowConstraints
import javafx.scene.layout.StackPane
import javafx.scene.layout.TilePane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.robot.Robot
import javafx.scene.shape.Rectangle
import javafx.scene.text.Font
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
import javafx.scene.text.Text
import javafx.scene.text.TextAlignment
import javafx.scene.text.TextFlow
import javafx.stage.Screen
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.stage.Window
import javafx.util.Callback
import sp.it.util.dev.Dsl
import sp.it.util.dev.fail
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import sp.it.util.functional.net
import sp.it.util.functional.traverse
import sp.it.util.math.P
import sp.it.util.math.max
import sp.it.util.math.min
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.sync
import sp.it.util.ui.image.FitFrom
import sp.it.util.units.toEM
import sp.it.util.units.uuid
import kotlin.math.abs
import kotlin.math.floor

/* ---------- COLOR ------------------------------------------------------------------------------------------------- */

/** @return color with same r,g,b values but specified opacity */
fun Color.alpha(opacity: Double): Color {
   return Color(red, green, blue, opacity)
}

/* ---------- ICON -------------------------------------------------------------------------------------------------- */

fun createIcon(icon: GlyphIcons, iconSize: Double? = null) = Text(icon.characterToString()).apply {
   val fontSize = iconSize?.toEM()
   val fontSizeCss = fontSize?.let { "-fx-font-size: ${fontSize}em;" }.orEmpty()
   style = "-fx-font-family: ${icon.fontFamily};$fontSizeCss"
   styleClass += "icon"
}

fun createIcon(icon: GlyphIcons, icons: Int, iconSize: Double? = null): Text {
   val fontSize = iconSize?.toEM()
   val s = icon.characterToString()
   val sb = StringBuilder(icons)
   for (i in 0 until icons) sb.append(s)

   val fontSizeCss = fontSize?.let { "-fx-font-size: ${fontSize}em;" }.orEmpty()
   return Text(sb.toString()).apply {
      style = "-fx-font-family: ${icon.fontFamily};$fontSizeCss"
      styleClass += "icon"
   }
}

/* ---------- NODE -------------------------------------------------------------------------------------------------- */

/** @return true iff this or any of [Parent.unmodifiableChildren] [Node.isFocused] */
fun Node.hasFocus(): Boolean = scene?.focusOwner?.traverse { it.parent }.orEmpty().any { it==this }

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

   when (p) {
      is Group -> p.children -= this
      is Pane -> p.children -= this
   }

   // Fixes possible usage of this node after removal from parent, because scene retains reference to focusOwner and
   // removes it when focus changes. Focus listener would invoke when this node is no longer part of scene graph.
   if (hasFocusedChild) p?.requestFocus()
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T: Node> Node.lookupId(id: String): T = lookup("#$id").let {
   when (it) {
      null -> fail { "No match for id=$id" }
      is T -> it
      else -> fail { "No match for id=$id, ${it::class} is not ${T::class}" }
   }
}

@Suppress("UNCHECKED_CAST")
fun <T: Node> Node.lookupChildAt(at: Int): T = when (this) {
   is Parent -> childrenUnmodifiable.getOrNull(at) as T
   else -> fail { "${this::class} can not have children" }
}

@Suppress("UNCHECKED_CAST")
fun <T: Node> Node.lookupSiblingUp(by: Int = 1): T = parent!!.asIs<Parent>().childrenUnmodifiable.net { it.getOrNull(it.indexOf(this) - by)!! } as T

@Suppress("UNCHECKED_CAST")
fun <T: Node> Node.lookupSiblingDown(by: Int = 1): T = parent!!.asIs<Parent>().childrenUnmodifiable.net { it.getOrNull(it.indexOf(this) + by)!! } as T

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

/** Adds or removes the specified styleclass using [Node.styleclassAdd] and [Node.styleclassRemove]. */
fun Node.styleclassToggle(styleClass: String) {
   if (styleClass !in this.styleClass) styleclassAdd(styleClass)
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
 * Disposer called in [Node.onNodeDispose] or null if it hasn't been initialized yet.
 */
val Node.onNodeDisposeOrNull: Disposer?
   get() = properties["onDispose_7bccf7a3-bcae-42ca-a91d-9f95217b942c"].asIf()

/**
 * Disposes of this node, with the intention of it and all it's children to never again be used in the scene graph.
 *
 * If a disposer is initialized, it will be invoked. Use [Node.onNodeDispose] property to access, initialize and add
 * subscriptions to the underlying disposer.
 *
 * If this is [Control], [javafx.scene.control.Skin.dispose] will be invoked on its skin.
 *
 * If this is [Parent], this function will be invoked on each item in [Parent.getChildrenUnmodifiable]. Disposal
 * is recursive, in a top-down depth-first traversal.
 *
 * This is an optional dispose API and as such is not used nor called automatically by the JavaFx. It is up to the
 * developer to call it appropriately, but one may wish to call it immediately after a call to [Node.removeFromParent].
 */
fun Node.onNodeDispose() {
   onNodeDisposeOrNull?.invoke()
   if (this is SplitPane) {
      items.forEach { it.onNodeDispose() }
   }
   if (this is ScrollPane) {
      content?.onNodeDispose()
      content = null // avoids `skin = null` calling `requestLayout()` and throwing exception
   }
   if (this is Control) skin?.dispose()
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

inline fun stage(style: StageStyle = StageStyle.DECORATED, block: Stage.() -> Unit = {}) = Stage(style).apply(block)
inline fun scene(root: Parent = StackPane(), block: Scene.() -> Unit = {}) = Scene(root).apply(block)
inline fun pane(block: Pane.() -> Unit = {}) = Pane().apply(block)
inline fun pane(vararg children: Node, block: Pane.() -> Unit = {}) = Pane(*children).apply(block)
inline fun stackPane(block: StackPane.() -> Unit = {}) = StackPane().apply(block)
inline fun stackPane(vararg children: Node, block: StackPane.() -> Unit = {}) = StackPane(*children).apply { block() }
inline fun anchorPane(block: AnchorPane.() -> Unit = {}) = AnchorPane().apply(block)
inline fun tilePane(hgap: Double = 0.0, vgap: Double = 0.0, block: TilePane.() -> Unit = {}) = TilePane(hgap, vgap).apply(block)
inline fun flowPane(hgap: Double = 0.0, vgap: Double = 0.0, block: FlowPane.() -> Unit = {}) = FlowPane(hgap, vgap).apply(block)
inline fun gridPane(block: (GridPane).() -> Unit = {}) = GridPane().apply(block)
inline fun gridPaneRow(block: (RowConstraints).() -> Unit = {}) = RowConstraints().apply(block)
inline fun gridPaneColumn(block: (ColumnConstraints).() -> Unit = {}) = ColumnConstraints().apply(block)
inline fun hBox(spacing: Number = 0.0, alignment: Pos? = null, block: HBox.() -> Unit = {}) = HBox(spacing.toDouble()).apply { this.alignment = alignment; block() }
inline fun vBox(spacing: Number = 0.0, alignment: Pos? = null, block: VBox.() -> Unit = {}) = VBox(spacing.toDouble()).apply { this.alignment = alignment; block() }
inline fun splitPane(block: SplitPane.() -> Unit = {}) = SplitPane().apply(block)
inline fun scrollPane(block: ScrollPane.() -> Unit = {}) = ScrollPane().apply(block)
inline fun scrollText(block: () -> Text) = Util.layScrollVText(block())!!
inline fun scrollTextCenter(block: () -> Text) = Util.layScrollVTextCenter(block())!!
inline fun borderPane(block: BorderPane.() -> Unit = {}) = BorderPane().apply(block)
inline fun label(text: String = "", block: Label.() -> Unit = {}) = Label(text).apply(block)
inline fun hyperlink(text: String = "", block: Hyperlink.() -> Unit = {}) = Hyperlink(text).apply(block)
inline fun button(text: String = "", block: Button.() -> Unit = {}) = Button(text).apply(block)
inline fun text(text: String = "", block: Text.() -> Unit = {}) = Text(text).apply(block)
inline fun textField(text: String = "", block: TextField.() -> Unit = {}) = TextField(text).apply(block)
inline fun textArea(text: String = "", block: TextArea.() -> Unit = {}) = TextArea(text).apply(block)
inline fun textFlow(block: TextFlow.() -> Unit = {}) = TextFlow().apply(block)
inline fun menu(text: String, graphics: Node? = null, block: (Menu).() -> Unit = {}) = Menu(text, graphics).apply(block)
inline fun menuItem(text: String, crossinline action: (ActionEvent) -> Unit) = MenuItem(text).apply { onAction = EventHandler { action(it) } }
inline fun menuSeparator(block: SeparatorMenuItem.() -> Unit = {}) = SeparatorMenuItem().apply(block)
inline fun <T> listView(block: ListView<T>.() -> Unit = {}) = ListView<T>().apply(block)
inline fun <T> tableView(block: TableView<T>.() -> Unit = {}) = TableView<T>().apply(block)
inline fun <T> treeView(block: TreeView<T>.() -> Unit = {}) = TreeView<T>().apply(block)
inline fun <T> treeTableView(block: TreeTableView<T>.() -> Unit = {}) = TreeTableView<T>().apply(block)
fun <T> listViewCellFactory(cellFactory: ListCell<T>.(T?, Boolean) -> Unit) = Callback<ListView<T>, ListCell<T>> {
   object: ListCell<T>() {
      override fun updateItem(item: T?, empty: Boolean) {
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

class BorderPaneLay(private val pane: BorderPane): Lay {

   override fun plusAssign(child: Node) {
      pane.children += child
   }

   operator fun invoke(alignment: Pos?): Lay = object: Lay {
      override fun plusAssign(child: Node) {
         pane.children += child
         BorderPane.setAlignment(child, alignment)
      }
   }
}

class SplitPaneLay(private val pane: SplitPane): Lay {

   override fun plusAssign(child: Node) {
      pane.items += child
   }

   operator fun invoke(resizableWithParent: Boolean): Lay = object: Lay {
      override fun plusAssign(child: Node) {
         pane.items += child
         SplitPane.setResizableWithParent(child, resizableWithParent)
      }
   }
}

class GridPaneLay(private val pane: GridPane): Lay {

   override fun plusAssign(child: Node) {
      pane.children += child
   }

   operator fun invoke(row: Int, column: Int, rowSpan: Int = 1, colSpan: Int = 1, hAlignment: HPos = HPos.CENTER, vAlignment: VPos = VPos.CENTER): Lay = object: Lay {
      override fun plusAssign(child: Node) {
         pane.add(child, column, row, colSpan, rowSpan)
         GridPane.setHalignment(child, hAlignment)
         GridPane.setValignment(child, vAlignment)
      }
   }
}

val Pane.lay get() = PaneLay(this)
val HBox.lay get() = HBoxLay(this)
val VBox.lay get() = VBoxLay(this)
val StackPane.lay get() = StackLay(this)
val AnchorPane.lay get() = AnchorPaneLay(this)
val AnchorPane.layFullArea get() = AnchorPaneLay(this)(0.0)
val BorderPane.lay get() = BorderPaneLay(this)
val SplitPane.lay get() = SplitPaneLay(this)
val GridPane.lay get() = GridPaneLay(this)

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

@Dsl
open class MenuBuilder<M, V>(val owner: M, val value: V) {

   open infix fun add(item: MenuItem) = when (owner) {
      is ContextMenu -> owner.items += item
      is Menu -> owner.items += item
      is MutableList<*> -> owner.asIs<MutableList<MenuItem>>() += item
      else -> fail { "Menu DSL owner must be ${ContextMenu::class} or ${MenuItem::class}, but was $owner" }
   }

   /** Create and add to items menu with specified text and graphics. */
   @Dsl
   inline fun menu(text: String, graphics: Node? = null, crossinline then: @Dsl MenuBuilder<Menu, V>.() -> Unit) = this add Menu(text, graphics).dsl(value) { then() }


   /** Create and add to items new menu item with specified text and action. */
   @Dsl
   inline fun item(text: String, graphics: Node? = null, crossinline action: @Dsl MenuItem.(V) -> Unit) = this add MenuItem(text, graphics).apply { onAction = EventHandler { action(value) } }

   /** Create and add to items new menu items with text and action derived from specified source. */
   @Dsl
   @Suppress("RedundantLambdaArrow")
   inline fun <A> items(source: Sequence<A>, crossinline text: (A) -> String, crossinline action: (A) -> Unit) = source.map { menuItem(text(it)) { _ -> action(it) } }.sortedBy { it.text }.forEach { this add it }

   /** Create and add to items new menu separator. */
   @Dsl
   fun separator() = this add menuSeparator()

}

@Dsl
inline fun ContextMenu.dsl(block: (MenuBuilder<ContextMenu, Nothing?>).() -> Unit) = dsl(null, block)

@Dsl
inline fun <S: Any?> ContextMenu.dsl(selected: S, block: (MenuBuilder<ContextMenu, S>).() -> Unit) = apply { MenuBuilder(this, selected).block() }

@Dsl
fun <M: MenuItem> M.dsl(block: (MenuBuilder<M, Nothing?>).() -> Unit) = dsl(null, block)

@Dsl
fun <M: MenuItem, S> M.dsl(selected: S, block: (MenuBuilder<M, S>).() -> Unit) = apply { MenuBuilder(this, selected).block() }

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

/** Screen coordinates of position of this window */
var Window.xy
   get() = P(x, y)
   set(value) {
      x = value.x
      y = value.y
   }

/** Screen of this window */
var Window.size: P
   get() = P(width, height)
   set(value) {
      width = value.x
      height = value.y
   }

/** Screen coordinates of bounds */
val Window.bounds: Rectangle2D get() = xy areaBy size

/** Screen coordinates of the centre of this window */
var Window.centre
   get() = P(centreX, centreY)
   set(value) {
      xy = value - size/2.0
   }

/** Screen x coordinate of the centre of this window */
var Window.centreX
   get() = x + width/2
   set(value) {
      x = value - width/2
   }

/** Screen y coordinate of the centre of this window */
var Window.centreY
   get() = y + height/2
   set(value) {
      x = value - height/2
   }

/** ([MouseEvent.x],[MouseEvent.y]) */
val MouseEvent.xy get() = P(x, y)

/** ([MouseEvent.sceneX],[MouseEvent.sceneY]) */
val MouseEvent.sceneXy get() = P(sceneX, sceneY)

/** ([MouseEvent.screenX],[MouseEvent.screenY]) */
val MouseEvent.screenXy get() = P(screenX, screenY)

/** Left top corner of the bounds represented as point */
val Rectangle2D.min get() = P(minX, minY)

/** Bottom right corner of the bounds represented as point */
val Rectangle2D.max get() = P(maxX, maxY)

/** PositionLeft top corner of the bounds represented as point */
val Rectangle2D.xy get() = min

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

/** @return rectangle defined by this point and the specified point */
infix fun P.areaTo(p: P) = Rectangle2D(x min p.x, y min p.y, abs(x - p.x), abs(y - p.y))

/** @return rectangle defined by this point and the point shifted by the specified point from this point */
infix fun P.areaBy(p: P) = Rectangle2D(x min (x + p.x), y min (y + p.y), abs(p.x), abs(p.y))

operator fun Insets.plus(insets: Insets) = Insets(top + insets.top, right + insets.right, bottom + insets.bottom, left + insets.left)
operator fun Insets.minus(insets: Insets) = Insets(top - insets.top, right - insets.right, bottom - insets.bottom, left - insets.left)

val Insets.width: Double get() = left + right
val Insets.height: Double get() = top + bottom

fun Node.screenToLocal(e: MouseEvent) = screenToLocal(e.screenX, e.screenY)!!
fun Node.sceneToLocal(e: MouseEvent) = sceneToLocal(e.sceneX, e.sceneY)!!

/* ---------- TEXT -------------------------------------------------------------------------------------------------- */

/** Alignment of the text of this text area. */
var TextArea.textAlignment: TextAlignment
 get() = when {
    pseudoClassStates.any { it.pseudoClassName=="align-left" } -> TextAlignment.LEFT
    pseudoClassStates.any { it.pseudoClassName=="align-right" } -> TextAlignment.RIGHT
    pseudoClassStates.any { it.pseudoClassName=="align-center" } -> TextAlignment.CENTER
    pseudoClassStates.any { it.pseudoClassName=="align-justify" } -> TextAlignment.JUSTIFY
    else -> TextAlignment.LEFT
 }
 set(alignment) {
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

   val fixedCellHeightNotSet = fixedCellSize<=0
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
 * More reliable than [MouseEvent.MOUSE_ENTERED]/[MouseEvent.MOUSE_EXITED], because this takes into consideration mouse drag.
 */
fun Node.onHoverOrDrag(on: (Boolean) -> Unit): Subscription {
   val key = "isHoverOrDrag-" + uuid()

   if (isHover) {
      properties[key] = true
      on(true)
   }

   return Subscription(
      onEventUp(MOUSE_ENTERED) {
         if (key !in properties) {
            properties[key] = true
            on(true)
         }
      },
      onEventUp(DRAG_DETECTED) {
         if (key !in properties) {
            properties[key] = true
            on(true)
         }
      },
      onEventUp(MOUSE_EXITED) {
         if (key in properties) {
            properties -= key
            on(false)
         }
      },
      onEventUp(MOUSE_DRAG_EXITED) {
         if (key in properties) {
            properties -= key
            on(false)
         }
      },
      onEventUp(MOUSE_RELEASED) {
         if (key in properties && !isHover) {
            properties -= key
            on(false)
         }
      }
   )
}

/* ---------- SCREEN ------------------------------------------------------------------------------------------------ */

/** @return screen containing this point or primary screen */
fun P.getScreen() = getScreen(x, y)

/** @return screen containing the given coordinates or primary screen */
fun getScreen(x: Double, y: Double) = Screen.getScreens().find { it.bounds.contains(x, y) } ?: Screen.getPrimary()!!

/** @return screen containing the given coordinates or primary screen */
fun getScreenForMouse() = Robot().mousePosition.toP().getScreen()

/** @return image representing actual content of this screen */
fun Screen.makeScreenShot(image: WritableImage? = null) = Robot().getScreenCapture(image, bounds)!!

/** @return screen containing this window or primary screen */
val Window.screen: Screen get() = null
   ?: Screen.getScreens().find { centre.toPoint2D() in it.bounds }
   ?: Screen.getScreens().find { it.bounds.intersects(bounds) }
   ?: Screen.getPrimary()!!