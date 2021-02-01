package sp.it.pl.ui.pane

import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Pos.CENTER_LEFT
import javafx.scene.Node
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.scene.text.TextFlow
import sp.it.pl.main.Css
import sp.it.pl.ui.itemnode.ConfigEditor
import sp.it.util.action.Action
import sp.it.util.collections.setTo
import sp.it.util.conf.Config
import sp.it.util.conf.Configurable
import sp.it.util.conf.Constraint
import sp.it.util.functional.asIf
import sp.it.util.math.clip
import sp.it.util.math.max
import sp.it.util.math.min
import sp.it.util.type.raw
import sp.it.util.ui.height
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.onNodeDispose
import sp.it.util.ui.text
import sp.it.util.ui.textFlow
import sp.it.util.ui.width

class ConfigPane<T: Any?>: VBox {
   private var editors: List<ConfigEditor<*>> = listOf()
   private var editorNodes: List<Node> = listOf()
   private var needsLabel: Boolean = true
   private var inLayout = false
   var onChange: Runnable? = null
   val configOrder = compareBy<Config<*>> { 0 }
      .thenBy { it.group.toLowerCase() }
      .thenBy { if (it.type.raw==Action::class) 1.0 else -1.0 }
      .thenBy { it.nameUi.toLowerCase() }

   constructor(): super(5.0) {
      styleClass += "form-config-pane"
   }

   constructor(configs: Configurable<T>): this() {
      configure(configs)
   }

   fun configure(configurable: Configurable<*>?) {
      alignment = CENTER_LEFT
      isFillWidth = false
      needsLabel = configurable !is Config<*>
      editors = configurable?.getConfigs().orEmpty().asSequence()
         .filter { !it.hasConstraint<Constraint.NoUi>() }
         .sortedWith(configOrder)
         .map {
            ConfigEditor.create(it).apply {
               onChange = this@ConfigPane.onChange
            }
         }
         .toList()
      editorNodes.forEach { it.onNodeDispose() }
      editorNodes = editors.flatMap {
         listOfNotNull(
            when {
               needsLabel -> label(it.config.nameUi) {
                  styleClass += "form-config-pane-config-name"
               }
               else -> null
            },
            when {
               it.config.info.isEmpty() || it.config.nameUi==it.config.info -> null
               else -> textFlow {
                  lay += text(it.config.info).apply {
                     styleClass += Css.DESCRIPTION
                     styleClass += "form-config-pane-config-description"
                  }
               }
            },
            it.buildNode()
         )
      }
      children setTo editorNodes
   }

   override fun getContentBias() = Orientation.HORIZONTAL

   // overridden because text nodes should not partake in width calculations
   // using TextFlowWithNoWidth would avoid this, but may cause other issues
   // ---
   private var isComputingWidth = false
   override fun computeMinWidth(height: Double): Double {
      isComputingWidth = true
      val x = super.computeMinWidth(height)
      isComputingWidth = false
      return x
   }
   override fun computePrefWidth(height: Double): Double {
      isComputingWidth = true
      val x = super.computePrefWidth(height)
      isComputingWidth = false
      return x
   }
   override fun computeMaxWidth(height: Double): Double {
      isComputingWidth = true
      val x = super.computeMaxWidth(height)
      isComputingWidth = false
      return x
   }
   override fun <E: Node> getManagedChildren(): MutableList<E> {
      val mc = super.getManagedChildren<E>()
      return if (isComputingWidth) return mc.filter { it !is TextFlow }.toMutableList()
      else mc
   }
   // ---

   // overridden because text nodes would not wrap
   override fun layoutChildren() {
      val contentLeft = padding.left
      val contentWidth = if (width>0) width - padding.width else 200.0
      children.fold(0.0) { h, n ->
         val p = n.asIf<Region>()?.padding ?: Insets.EMPTY
         val pH = n.prefHeight(-1.0).clip(n.minHeight(contentWidth), n.maxHeight(contentWidth))
         n.relocate(contentLeft, h + p.top + spacing)
         n.resize(contentWidth, pH)
         h + p.top + pH + p.bottom + spacing
      }
   }

   // overridden because text nodes would interfere with in height calculation
   // ---
   override fun computeMinHeight(width: Double) = insets.height + children.map { it.minHeight(width) }.sum()
   override fun computePrefHeight(width: Double): Double {
      var minY = 0.0
      var maxY = 0.0
      children.forEach { n ->
         val y = n.layoutBounds.minY + n.layoutY
         minY = minY min y
         maxY = maxY max (y + n.prefHeight(width).clip(n.minHeight(width), n.maxHeight(width)))
      }
      return maxY - minY
   }
   override fun computeMaxHeight(width: Double) = Double.MAX_VALUE
   // ---

   @Suppress("UNCHECKED_CAST")
   fun getConfigEditors(): List<ConfigEditor<T>> = editors as List<ConfigEditor<T>>

   fun getConfigValues(): List<T> = getConfigEditors().map { it.config.value }

   fun focusFirstConfigEditor() = editors.firstOrNull()?.focusEditor()
}