package sp.it.pl.gui.pane

import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER_LEFT
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import sp.it.pl.gui.itemnode.ConfigField
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

class ConfigPane<T: Any?>: VBox {
   private var fields: List<ConfigField<*>> = listOf()
   private var needsLabel: Boolean = true
   private var inLayout = false
   var onChange: Runnable? = null
   val configOrder = compareBy<Config<*>> { 0 }
      .thenBy { it.group.toLowerCase() }
      .thenBy { if (it.type==Action::javaClass) 1.0 else -1.0 }
      .thenBy { it.guiName.toLowerCase() }

   constructor(): super(5.0) {
      styleClass += "form-config-pane"
   }

   constructor(configs: Configurable<T>): this() {
      configure(configs)
   }

   fun configure(configurable: Configurable<*>?) {
      needsLabel = configurable !is Config<*>
      fields = configurable?.getFields().orEmpty().asSequence()
         .filter { it.findConstraint<Constraint.NoUi>()==null }
         .sortedWith(configOrder)
         .map {
            ConfigField.create(it).apply {
               onChange = this@ConfigPane.onChange
            }
         }
         .toList()

      alignment = CENTER_LEFT
      children setTo fields.asSequence().flatMap {
         sequenceOf(
            when {
               needsLabel -> label(it.config.guiName) {
                  styleClass += "form-config-pane-config-name"
               }
               else -> null
            },
            when {
               it.config.info.isEmpty() || it.config.guiName==it.config.info -> null
               else -> Text(it.config.info).apply {
                  isManaged = false
                  styleClass += "form-config-pane-config-description"
               }
            },
            it.buildNode()
         )
      }.filterNotNull()
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
   fun getConfigFields(): List<ConfigField<T>> = fields as List<ConfigField<T>>

   fun getConfigValues(): List<T> = getConfigFields().map { it.configValue }

   fun focusFirstConfigField() = fields.firstOrNull()?.focusEditor()
}