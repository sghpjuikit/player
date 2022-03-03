package sp.it.pl.ui.pane

import java.util.Comparator.comparing
import javafx.css.StyleableObjectProperty
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Pos.CENTER_LEFT
import javafx.scene.Node
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.scene.text.TextFlow
import sp.it.pl.main.Css
import sp.it.pl.main.emScaled
import sp.it.pl.ui.itemnode.ConfigEditor
import sp.it.pl.ui.labelForWithClick
import sp.it.pl.ui.pane.ConfigPane.Layout.EXTENSIVE
import sp.it.pl.ui.pane.ConfigPane.Layout.MINI
import sp.it.pl.ui.pane.ConfigPane.Layout.NORMAL
import sp.it.util.access.StyleableCompanion
import sp.it.util.access.enumConverter
import sp.it.util.access.sv
import sp.it.util.access.svMetaData
import sp.it.util.access.v
import sp.it.util.action.Action
import sp.it.util.collections.materialize
import sp.it.util.collections.setTo
import sp.it.util.conf.Config
import sp.it.util.conf.Configurable
import sp.it.util.conf.Constraint
import sp.it.util.conf.Constraint.UiSingleton
import sp.it.util.functional.asIf
import sp.it.util.functional.invoke
import sp.it.util.functional.net
import sp.it.util.functional.nullsFirst
import sp.it.util.math.clip
import sp.it.util.math.max
import sp.it.util.reactive.attach
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncFrom
import sp.it.util.type.propertyNullable
import sp.it.util.type.raw
import sp.it.util.ui.hBox
import sp.it.util.ui.height
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.onNodeDispose
import sp.it.util.ui.pseudoClassChanged
import sp.it.util.ui.text
import sp.it.util.ui.textFlow
import sp.it.util.ui.width

class ConfigPane<T: Any?>: VBox {
   private var editors: List<ConfigEditor<*>> = listOf()
   private var editorNodes: List<Node> = listOf()
   private var needsLabel: Boolean = true
   private val onChangeRaw = Runnable { onChange?.invoke() }
   private val onChangeOrConstraintRaw = Runnable { onChangeOrConstraint?.invoke() }
   val editable = v(true)
   val ui: StyleableObjectProperty<Layout> by sv(UI)
   var onChange: Runnable? = null
   var onChangeOrConstraint: Runnable? = null
   var editorOrder: Comparator<Config<*>>? = compareByDefault
      set(value) {
         field = value
         children setTo children.materialize().sortedByConfigWith(value)
      }

   constructor(): super(5.0) {
      styleClass += "form-config-pane"
      ui sync { Layout.values().forEach { l -> pseudoClassChanged(l.name.lowercase(), l == it) } }
      ui attach { buildUi(true) }
   }

   constructor(configs: Configurable<T>): this() {
      configure(configs)
   }

   fun configure(configurable: Configurable<*>?) {
      alignment = CENTER_LEFT
      isFillWidth = false

      editors = configurable?.getConfigs().orEmpty().asSequence()
         .filter { !it.hasConstraint<Constraint.NoUi>() }
         .map {
            ConfigEditor.create(it).apply {
               onChange = onChangeRaw
               onChangeOrConstraint = onChangeOrConstraintRaw
            }
         }
         .toList()

      val isSingletonConfig = editors.size==1 && editors.first().config.hasConstraint<UiSingleton>()
      needsLabel = !isSingletonConfig

      buildUi(false)
   }

   private fun buildUi(soft: Boolean) {
      val editorNodesOld = children.asSequence()
         .mapNotNull {
            if (buildUiKey in it.properties) it
            else it.asIf<Pane>()?.children?.find { buildUiKey in it.properties }
         }
         .associateBy { it.configEditor ?: it.parent?.configEditor!! }

      fun ConfigEditor<*>.buildNodeForThis() = editorNodesOld[this] ?: buildNode().apply {
         properties[buildUiKey] = buildUiKey
         isEditableAllowed syncFrom this@ConfigPane.editable
      }

      if (!soft)
         editorNodes.forEach { it.onNodeDispose() }

      editorNodes = editors.flatMap { e ->
         when (ui.value!!) {
            MINI -> listOf(
               hBox(20.emScaled, CENTER_LEFT) {
                  if (needsLabel)
                     lay(ALWAYS) += label(e.config.nameUi) {
                        styleClass += "form-config-pane-config-name"
                        isPickOnBounds = false
                        alignment = CENTER_LEFT
                        minWidth = Region.USE_PREF_SIZE
                        labelForWithClick setTo e.editor
                     }
                  lay += e.buildNodeForThis()
               }
            )
            NORMAL -> listOfNotNull(
               when {
                  e.config.info.isEmpty() || e.config.nameUi==e.config.info -> null
                  else -> textFlow {
                     styleClass += Css.DESCRIPTION
                     styleClass += "form-config-pane-config-description"
                     lay += text(e.config.info)
                  }
               },
               hBox(20.emScaled, CENTER_LEFT) {
                  if (needsLabel)
                     lay(ALWAYS) += label(e.config.nameUi) {
                        styleClass += "form-config-pane-config-name"
                        isPickOnBounds = false
                        alignment = CENTER_LEFT
                        minWidth = Region.USE_PREF_SIZE
                        labelForWithClick setTo e.editor
                     }
                  lay += e.buildNodeForThis()
               }
            )
            EXTENSIVE -> listOfNotNull(
               when {
                  needsLabel -> label(e.config.nameUi) {
                     styleClass += "form-config-pane-config-name"
                     isPickOnBounds = false
                     prefWidth = Region.USE_PREF_SIZE
                     labelForWithClick setTo e.editor
                  }
                  else -> null
               },
               when {
                  e.config.info.isEmpty() || e.config.nameUi==e.config.info -> null
                  else -> textFlow {
                     styleClass += Css.DESCRIPTION
                     styleClass += "form-config-pane-config-description"
                     lay += text(e.config.info)
                  }
               },
               e.buildNodeForThis()
            )
         }.onEach { n ->
            n.configEditor = e
         }
      }
      children setTo editorNodes.sortedByConfigWith(editorOrder)

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
         val space = snapSpaceY(spacing)
         val isSingleEditor = editors.size==1 && editors.first().config.hasConstraint<UiSingleton>()
         val lastEditor = children.lastOrNull()
         children.fold(padding.top) { h, n ->
            val p = HBox.getMargin(n) ?: Insets.EMPTY
            val pH = n.prefHeight(contentWidth).clip(n.minHeight(contentWidth), n.maxHeight(contentWidth))
            if (isSingleEditor && n===lastEditor) n.resizeRelocate(contentLeft, h + p.top, contentWidth, height - h - padding.bottom - p.top)
            else n.resizeRelocate(contentLeft, h + p.top, contentWidth, pH)
            h + p.top + pH + p.bottom + space
         }
   }

   // overridden because text nodes would interfere with in height calculation
   // ---
   private fun spacingTotal() = (children.size-1).max(0) * snapSpaceY(spacing)
   override fun computeMinHeight(width: Double) = insets.height + spacingTotal() + children.sumOf { it.minHeight(width) }
   override fun computePrefHeight(width: Double) =  insets.height + spacingTotal() + children.sumOf { n -> (HBox.getMargin(n)?.height ?: 0.0) + n.prefHeight(width).clip(n.minHeight(width), n.maxHeight(width)) }
   override fun computeMaxHeight(width: Double) = Double.MAX_VALUE
   // ---

   @Suppress("UNCHECKED_CAST")
   fun getConfigEditors(): List<ConfigEditor<T>> = editors as List<ConfigEditor<T>>

   fun getConfigValues(): List<T> = getConfigEditors().map { it.config.value }

   fun focusFirstConfigEditor() = editors.firstOrNull()?.focusEditor()

   private fun List<Node>.sortedByConfigWith(comparator: Comparator<Config<*>>?): List<Node> =
      if (comparator == null) sortedBy { it.configEditor?.net(editors::indexOf) ?: 0 }
      else sortedWith(comparing({ it.configEditor?.config }, comparator.nullsFirst()))

   override fun getCssMetaData() = classCssMetaData

   companion object: StyleableCompanion() {
      /** Order by declaration order, i.e., [Configurable.getConfigs]. */
      val compareByDeclaration = null
      /** Order by group, [Config.groupUi]. */
      val compareByGroup: Comparator<Config<*>> = compareBy<Config<*>> { 0 }
         .thenBy { it.group.lowercase() }
      /** Order by application semantics. */
      val compareByApp: Comparator<Config<*>> = compareBy<Config<*>> { 0 }
         .thenBy { it.group.lowercase() }
         .thenBy { if (it.type.raw==Action::class) 1.0 else -1.0 }
         .thenBy { it.nameUi.lowercase() }
      /** Default value of [ConfigPane.editorOrder] */
      val compareByDefault: Comparator<Config<*>> = compareByApp
      /** Default value of [ConfigPane.ui] */
      val uiDefault = EXTENSIVE

      private val buildUiKey = Any()
      private var Node.configEditor by propertyNullable<ConfigEditor<*>>("config")

      val UI by svMetaData<ConfigPane<*>, Layout>("-fx-ui", enumConverter(), EXTENSIVE, ConfigPane<*>::ui)
   }

   enum class Layout { MINI, NORMAL, EXTENSIVE }
}