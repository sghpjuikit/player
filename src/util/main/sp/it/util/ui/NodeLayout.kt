package sp.it.util.ui

import javafx.collections.ObservableList
import javafx.geometry.HPos
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.geometry.VPos
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.SplitPane
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.BorderPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import sp.it.util.functional.net
import sp.it.util.functional.toUnit

interface Lay {
   /** Lays the specified child onto this */
   operator fun plusAssign(child: Node)

   /** Lays the specified children onto this */
   operator fun plusAssign(children: Collection<Node>) = children.forEach { this += it }

   /** Lays the specified children onto this */
   operator fun plusAssign(children: Sequence<Node>) = children.forEach { this += it }

   /**
    * Lays the child produced by the specified block onto this if block is not null.
    * Allows conditional content using [sp.it.util.functional.supplyIf] and [sp.it.util.functional.supplyUnless].
    */
   operator fun plusAssign(child: (() -> Node)?) = child?.net { plusAssign(it()) }.toUnit()
}

interface ChildrenLay: Lay {
   val children: ObservableList<Node>

   /** Clears [Pane.children] */
   fun clear() = children.clear()

   /** Adds the specified child to [Pane.children] */
   override operator fun plusAssign(child: Node) = this.children.add(child).toUnit()

   /** Adds the specified children to [Pane.children] */
   override operator fun plusAssign(children: Collection<Node>) = this.children.addAll(children).toUnit()

   /** Adds the specified children to [Pane.children] */
   override operator fun plusAssign(children: Sequence<Node>) = this.children.addAll(children.toList()).toUnit()

   /** Removes the specified child from [Pane.children] */
   operator fun minusAssign(child: Node) = this.children.remove(child).toUnit()

   /** Removes the specified children from [Pane.children] */
   operator fun minusAssign(children: Collection<Node>) = this.children.removeAll(children).toUnit()

   /** Removes the specified children from [Pane.children] */
   operator fun minusAssign(children: Sequence<Node>) = this.children.removeAll(children.toList()).toUnit()

}

@JvmInline
value class GroupLay(private val group: Group): ChildrenLay {

   override val children: ObservableList<Node> get() = group.children

}

@JvmInline
value class PaneLay(private val pane: Pane): ChildrenLay {

   override val children: ObservableList<Node> get() = pane.children

}

@JvmInline
value class HBoxLay(private val pane: HBox): ChildrenLay {

   override val children: ObservableList<Node> get() = pane.children

   operator fun invoke(priority: Priority): Lay = object: Lay {
      override fun plusAssign(child: Node) {
         this@HBoxLay += child
         HBox.setHgrow(child, priority)
      }
   }
}

@JvmInline
value class VBoxLay(private val pane: VBox): ChildrenLay {

   override val children: ObservableList<Node> get() = pane.children

   operator fun invoke(priority: Priority): Lay = object: Lay {
      override fun plusAssign(child: Node) {
         this@VBoxLay += child
         VBox.setVgrow(child, priority)
      }
   }
}

@JvmInline
value class StackLay(private val pane: StackPane): ChildrenLay {

   override val children: ObservableList<Node> get() = pane.children

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

@JvmInline
value class AnchorPaneLay(private val pane: AnchorPane): ChildrenLay {

   override val children: ObservableList<Node> get() = pane.children

   operator fun invoke(topRightBottomLeft: Number?) = invoke(topRightBottomLeft, topRightBottomLeft, topRightBottomLeft, topRightBottomLeft)

   operator fun invoke(top: Number?, right: Number?, bottom: Number?, left: Number?): Lay = object: Lay {
      override fun plusAssign(child: Node) {
         Util.setAnchor(pane, child, top?.toDouble(), right?.toDouble(), bottom?.toDouble(), left?.toDouble())
      }
   }
}

@JvmInline
value class BorderPaneLay(private val pane: BorderPane): ChildrenLay {

   override val children: ObservableList<Node> get() = pane.children

   operator fun invoke(alignment: Pos?): Lay = object: Lay {
      override fun plusAssign(child: Node) {
         pane.children += child
         BorderPane.setAlignment(child, alignment)
      }
   }
}

@JvmInline
value class SplitPaneLay(private val pane: SplitPane): ChildrenLay {

   override val children: ObservableList<Node> get() = pane.items

   operator fun invoke(resizableWithParent: Boolean): Lay = object: Lay {
      override fun plusAssign(child: Node) {
         pane.items += child
         SplitPane.setResizableWithParent(child, resizableWithParent)
      }
   }
}

@JvmInline
value class GridPaneLay(private val pane: GridPane): ChildrenLay {

   override val children: ObservableList<Node> get() = pane.children

   operator fun invoke(row: Int, column: Int, rowSpan: Int = 1, colSpan: Int = 1, hAlignment: HPos = HPos.CENTER, vAlignment: VPos = VPos.CENTER): Lay = object: Lay {
      override fun plusAssign(child: Node) {
         pane.add(child, column, row, colSpan, rowSpan)
         GridPane.setHalignment(child, hAlignment)
         GridPane.setValignment(child, vAlignment)
      }
   }
}

val Group.lay get() = GroupLay(this)
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