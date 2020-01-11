package sp.it.pl.gui.pane

import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER_LEFT
import javafx.scene.Node
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import sp.it.pl.gui.itemnode.ConfigEditor
import sp.it.pl.gui.objects.Text
import sp.it.util.action.Action
import sp.it.util.collections.setTo
import sp.it.util.conf.Config
import sp.it.util.conf.Configurable
import sp.it.util.conf.Constraint
import sp.it.util.functional.asIf
import sp.it.util.math.clip
import sp.it.util.math.max
import sp.it.util.math.min
import sp.it.util.ui.label
import sp.it.util.ui.onNodeDispose

class ConfigPane<T: Any?>: VBox {
   private var editors: List<ConfigEditor<*>> = listOf()
   private var editorNodes: List<Node> = listOf()
   private var needsLabel: Boolean = true
   private var inLayout = false
   var onChange: Runnable? = null
   val configOrder = compareBy<Config<*>> { 0 }
      .thenBy { it.group.toLowerCase() }
      .thenBy { if (it.type==Action::javaClass) 1.0 else -1.0 }
      .thenBy { it.nameUi.toLowerCase() }

   constructor(): super(5.0) {
      styleClass += "form-config-pane"
   }

   constructor(configs: Configurable<T>): this() {
      configure(configs)
   }

   fun configure(configurable: Configurable<*>?) {
      alignment = CENTER_LEFT
      needsLabel = configurable !is Config<*>
      editors = configurable?.getConfigs().orEmpty().asSequence()
         .filter { it.findConstraint<Constraint.NoUi>()==null }
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
               else -> Text(it.config.info).apply {
                  isManaged = false
                  styleClass += "form-config-pane-config-description"
               }
            },
            it.buildNode()
         )
      }
      children setTo editorNodes
   }

   // overridden because we have un-managed nodes description nodes would cause wrong width
   override fun layoutChildren() {
      val contentLeft = padding.left
      val contentWidth = if (width>0) width - padding.left - padding.right else 200.0
      children.asSequence().fold(0.0) { h, n ->
         if (n is Text) n.wrappingWidth = contentWidth
         val p = n.asIf<Region>()?.padding ?: Insets.EMPTY
         n.relocate(contentLeft, h + p.top + spacing)
         n.resize(contentWidth, n.prefHeight(-1.0))
         h + p.top + n.prefHeight(-1.0) + p.bottom + spacing
      }
   }

   override fun computeMinHeight(width: Double) = insets.top + insets.bottom

   // overridden because un-managed description nodes would not partake in height calculation
   override fun computePrefHeight(width: Double): Double {
      var minY = 0.0
      var maxY = 0.0
      children.forEach { n ->
         val y = n.layoutBounds.minY + n.layoutY
         minY = minY min y
         maxY = maxY max (y + n.prefHeight(-1.0).clip(n.minHeight(-1.0), n.maxHeight(-1.0)))
      }
      return maxY - minY
   }

   override fun computeMaxHeight(width: Double) = Double.MAX_VALUE

   @Suppress("UNCHECKED_CAST")
   fun getConfigEditors(): List<ConfigEditor<T>> = editors as List<ConfigEditor<T>>

   fun getConfigValues(): List<T> = getConfigEditors().map { it.configValue }

   fun focusFirstConfigEditor() = editors.firstOrNull()?.focusEditor()
}