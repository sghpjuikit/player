package sp.it.pl.layout

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ELLIPSIS_H
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ELLIPSIS_V
import javafx.geometry.Orientation.HORIZONTAL
import javafx.geometry.Orientation.VERTICAL
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import sp.it.pl.main.APP
import sp.it.pl.main.IconFA
import sp.it.pl.ui.objects.icon.Icon
import sp.it.util.access.toggleNext
import sp.it.util.collections.setTo
import sp.it.util.dev.failIf
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.on
import sp.it.util.reactive.sync
import sp.it.util.ui.anchorPane
import sp.it.util.ui.hBox
import sp.it.util.ui.layFullArea
import sp.it.util.ui.minSize
import sp.it.util.ui.pseudoClassChanged
import sp.it.util.ui.stackPane
import sp.it.util.ui.vBox
import sp.it.util.ui.x2

class ContainerSeqUi(c: ContainerSeq): ContainerUi<ContainerSeq>(c) {
   private val roots = LinkedHashMap<Int, Node>()
   private val uis = LinkedHashMap<Int, ComponentUi>()
   private lateinit var box: Pane
   private val disposer = Disposer()
   private val block1 = stackPane { styleClass += "widget-ui"; HBox.setHgrow(this, Priority.ALWAYS); VBox.setVgrow(this, Priority.ALWAYS) }
   private val block2 = stackPane { styleClass += "widget-ui"; HBox.setHgrow(this, Priority.ALWAYS); VBox.setVgrow(this, Priority.ALWAYS) }

   init {
      root.id += "container-seq-ui"
      root.styleClass += "container-seq-ui"

      container.orientation sync {
         box = when (it!!) {
            HORIZONTAL -> hBox { }
            VERTICAL -> vBox { }
         }
         updateFill()
         updateAlignment()
         root.children.clear()
         root.layFullArea += box
         updateChildren()
      }
      container.fill sync ::updateFill
      container.alignment sync ::updateAlignment
      container.joined sync { root.pseudoClassChanged("joined", it) }
   }

   @Suppress("UNUSED_VARIABLE")
   override fun buildControls() = super.buildControls().apply {
      val orientB = Icon(IconFA.MAGIC, -1.0, "Change orientation").addExtraIcon().onClickDo { container.orientation.toggleNext() }.styleclass("header-icon")
      val addB = Icon(IconFA.PLUS, -1.0, "Add child").addExtraIcon().onClickDo { container.addChild(container.getEmptySpot(), null) }.styleclass("header-icon")
      container.orientation sync { orientB.icon(it==VERTICAL, ELLIPSIS_V, ELLIPSIS_H) } on disposer
   }

   fun updateFill(it: Boolean = container.fill.value) = when (val b = box) {
      is HBox -> b.isFillHeight = it
      is VBox -> b.isFillWidth = it
      else -> {}
   }

   fun updateAlignment(it: Pos = container.alignment.value) = when (val b = box) {
      is HBox -> b.alignment = it
      is VBox -> b.alignment = it
      else -> {}
   }

   fun updateChildren() {
      box.children setTo (listOf(block1) + roots.values + listOf(block2))
   }

   fun setComponent(i: Int, c: Component?) {
      failIf(i<0) { "Index=$i must be >=0" }

      fun <T> T.closeUi() = apply { if (uis[i] is Layouter || uis[i] is WidgetUi) uis[i]?.dispose() }
      fun <T: AltState> T.showIfLM() = apply { if (APP.ui.isLayoutMode) show() }

      when (c) {
         is Container<*> -> {
            val r = anchorPane { minSize = 0.x2 }
            uis[i] = uis[i].takeIf { it is ContainerUi<*> && it.container==c } ?: kotlin.run {
               c.load(r)
               c.showIfLM()
               c.ui!!
            }
            roots[i] = r
         }
         is Widget -> {
            uis[i] = uis[i].takeIf { it is WidgetUi && it.widget==c } ?: WidgetUi(container, i, c).closeUi().showIfLM()
            roots[i] = uis[i]!!.root
         }
         null -> {
            uis[i] = uis[i].takeIf { it is Layouter } ?: Layouter(container, i).closeUi().showIfLM()
            roots[i] = uis[i]!!.root
         }
      }

      updateChildren()
   }

   override fun show() {
      super.show()
      uis.forEach { (_, ui) -> ui.show() }
   }

   override fun hide() {
      super.hide()
      uis.forEach { (_, ui) -> ui.hide() }
   }

   override fun dispose() {
      disposer()
      super.dispose()
   }

}