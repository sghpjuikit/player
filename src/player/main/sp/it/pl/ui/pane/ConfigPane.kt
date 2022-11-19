package sp.it.pl.ui.pane

import java.util.Comparator.comparing
import javafx.css.StyleableObjectProperty
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Pos.CENTER_LEFT
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.VBox
import sp.it.pl.main.Css
import sp.it.pl.main.emScaled
import sp.it.pl.ui.itemnode.ConfigEditor
import sp.it.pl.ui.itemnode.ConfigurableCE
import sp.it.pl.ui.itemnode.ObservableListCE
import sp.it.pl.ui.itemnode.PaginatedObservableListCE
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
import sp.it.util.functional.asIs
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
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.onNodeDispose
import sp.it.util.ui.pseudoClassChanged
import sp.it.util.ui.width

class ConfigPane<T: Any?>: VBox {
   private var editors: List<ConfigEditor<*>> = listOf()
   private var editorNodes: List<Node> = listOf()
   private var needsLabel: Boolean = true
   private val onChangeRaw: () -> Unit = { onChange?.invoke() }
   private val onChangeOrConstraintRaw: () -> Unit = { onChangeOrConstraint?.invoke() }
   val editable = v(true)
   val ui: StyleableObjectProperty<Layout> by sv(UI)
   var onChange: (() -> Unit)? = null
   var onChangeOrConstraint: (() -> Unit)? = null
   var editorOrder: Comparator<Config<*>>? = compareByDefault
      set(value) {
         field = value
         children setTo children.materialize().sortedByConfigWith(value)
      }

   constructor(): super(5.0) {
      styleClass += "form-config-pane"
      // maintain css
      ui sync { Layout.values().forEach { l -> pseudoClassChanged(l.name.lowercase(), l == it) } }
      // maintain layout
      ui attach { buildUi(true) }
      // maintain label wrapping widths
      widthProperty() attach { children.forEach { if (it is Label) it.prefWidth = computeContentWidth() } }
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

      fun ConfigEditor<*>.isNested() = this is ObservableListCE<*> || this is PaginatedObservableListCE || this is ConfigurableCE
      fun ConfigEditor<*>.buildNameLabel() = label(config.nameUi) {
         styleClass += "form-config-pane-config-name"
         isPickOnBounds = false
         alignment = CENTER_LEFT
         minWidth = USE_PREF_SIZE
         labelForWithClick setTo editor
      }
      fun ConfigEditor<*>.buildNodeForThis() = editorNodesOld[this] ?: buildNode().apply {
         properties[buildUiKey] = buildUiKey
         isEditableAllowed syncFrom this@ConfigPane.editable
      }
      fun ConfigEditor<*>.buildDescriptionText() = when {
         config.info.isEmpty() || config.nameUi==config.info -> null
         else -> label(config.info) {
            styleClass += Css.DESCRIPTION
            styleClass += "form-config-pane-config-description"
            isWrapText = true
            prefWidth = computeContentWidth()
         }
      }

      if (!soft)
         editorNodes.forEach { it.onNodeDispose() }

      editorNodes = editors.flatMap { e ->
         when (ui.value!!) {
            MINI -> when {
               !needsLabel -> listOf(
                  hBox(20.emScaled, CENTER_LEFT) {
                     lay += e.buildNodeForThis()
                  }
               )
               e.isNested() -> listOf(
                  e.buildNameLabel().apply { pseudoClassChanged("nested", true) },
                  hBox(20.emScaled, CENTER_LEFT) {
                     lay += e.buildNodeForThis()
                  }
               )
               else -> listOf(
                  hBox(20.emScaled, CENTER_LEFT) {
                     lay(ALWAYS) += e.buildNameLabel()
                     lay += e.buildNodeForThis()
                  }
               )
            }
            NORMAL -> when {
               !needsLabel -> listOfNotNull(
                  e.buildDescriptionText()?.apply { pseudoClassChanged("single", true) },
                  hBox(20.emScaled, CENTER_LEFT) {
                     lay += e.buildNodeForThis()
                  }
               )
               e.isNested() -> listOfNotNull(
                  e.buildDescriptionText()?.apply { pseudoClassChanged("nested", true) },
                  e.buildNameLabel().apply { pseudoClassChanged("nested", true) },
                  hBox(20.emScaled, CENTER_LEFT) {
                     lay += e.buildNodeForThis()
                  }
               )
               else -> listOfNotNull(
                  e.buildDescriptionText(),
                  hBox(20.emScaled, CENTER_LEFT) {
                     lay(ALWAYS) += e.buildNameLabel()
                     lay += e.buildNodeForThis()
                  }
               )
            }
            EXTENSIVE -> listOfNotNull(
               if (needsLabel) e.buildNameLabel() else null,
               e.buildDescriptionText()?.apply { pseudoClassChanged("single", !needsLabel) },
               e.buildNodeForThis()
            )
         }.onEach { n ->
            n.configEditor = e
         }
      }
      children setTo editorNodes.sortedByConfigWith(editorOrder)

   }

   fun isSingleEditor(): Boolean = editors.size==1 && editors.first().config.hasConstraint<UiSingleton>()

   private fun computeContentWidth(): Double = (width - padding.width) max 200.0

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

   // overridden because text nodes should not partake in width calculations
   override fun <E: Node> getManagedChildren(): MutableList<E> {
      val mc = super.getManagedChildren<E>()
      return if (isComputingWidth) mc.filter { it !is Label }.toMutableList() else mc
   }

   override fun getContentBias() = Orientation.HORIZONTAL

   // overridden because text nodes would not wrap
   override fun layoutChildren() {
         val contentLeft = padding.left
         val contentWidth = computeContentWidth()
         val space = snapSpaceY(spacing)
         val isSingleEditor = isSingleEditor()
         val lastEditor = children.lastOrNull()
         children.fold(padding.top) { h, n ->
            val p = HBox.getMargin(n) ?: Insets.EMPTY
            if (n is Label) n.prefWidth = contentWidth
            val pH = n.prefHeight(contentWidth).clip(n.minHeight(contentWidth), n.maxHeight(contentWidth))
            if (isSingleEditor && n===lastEditor) n.resizeRelocate(contentLeft, h + p.top, contentWidth, height - h - padding.bottom - p.top)
            else n.resizeRelocate(contentLeft, h + p.top, contentWidth, pH)
            h + p.top + pH + p.bottom + space
         }
   }

   fun getConfigEditors(): List<ConfigEditor<T>> = editors.asIs()

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