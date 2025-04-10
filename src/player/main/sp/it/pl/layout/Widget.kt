package sp.it.pl.layout

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.time.Instant
import java.util.Objects
import java.util.UUID
import javafx.beans.property.ReadOnlyBooleanWrapper
import javafx.beans.value.ChangeListener
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.layout.Pane
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.ANNOTATION_CLASS
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.system.measureTimeMillis
import org.jetbrains.annotations.Blocking
import sp.it.pl.layout.WidgetIoManager.requestWidgetIOUpdate
import sp.it.pl.layout.controller.Controller
import sp.it.pl.layout.controller.ControllerLoadError
import sp.it.pl.layout.controller.ControllerNoFactory
import sp.it.pl.layout.controller.LegacyController
import sp.it.pl.layout.controller.io.IOLayer
import sp.it.pl.layout.controller.io.Input
import sp.it.pl.main.APP
import sp.it.util.Locatable
import sp.it.util.access.readOnly
import sp.it.util.async.future.Fut
import sp.it.util.async.future.awaitFx
import sp.it.util.conf.Config
import sp.it.util.conf.ConfigDelegator
import sp.it.util.conf.ConfigValueSource
import sp.it.util.conf.Configurable
import sp.it.util.conf.Configuration
import sp.it.util.conf.EditMode
import sp.it.util.conf.c
import sp.it.util.conf.cv
import sp.it.util.conf.cvn
import sp.it.util.conf.cvro
import sp.it.util.conf.def
import sp.it.util.conf.noPersist
import sp.it.util.dev.Experimental
import sp.it.util.dev.Idempotent
import sp.it.util.dev.failIf
import sp.it.util.dev.failIfNotFxThread
import sp.it.util.file.del
import sp.it.util.file.div
import sp.it.util.file.json.JsNull
import sp.it.util.file.json.JsValue
import sp.it.util.functional.Functors.F1
import sp.it.util.functional.asIs
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.math.max
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.Handler0
import sp.it.util.reactive.attach
import sp.it.util.reactive.on
import sp.it.util.reactive.sync1If
import sp.it.util.ui.displayed
import sp.it.util.ui.findParent
import sp.it.util.ui.isAnyParentOf
import sp.it.util.ui.onNodeDispose
import sp.it.util.ui.pseudoClassChanged
import sp.it.util.ui.removeFromParent
import sp.it.util.units.millis

/**
 * Widget graphical component with a functionality.
 *
 *
 * The functionality is handled by widget's [Controller]. The controller
 * is instantiated when widget loads. The widget-controller relationship is 1:1
 * and permanent.
 *
 *
 * Widget can be thought of as a wrapper for controller (which may be used as
 * standalone object if implementation allows). The type of widget influences
 * the lifecycle.
 */
class Widget private constructor(factory: WidgetFactory<*>, isDeserialized: Boolean, state: WidgetDb): Component(state), Configurable<Any?>, ConfigDelegator, Locatable by factory {

   /**
    * Factory that produced this widget.
    *
    * Note that in case the application creates another version of the factory (e.g., when the
    * widget source code has been modified and recompiled in runtime), even though the old factory
    * will be removed from the list of factories (substituted by the new factory), this field will
    * still point to the old factory.
    */
   val factory: WidgetFactory<*> = factory

   /**
    * The controller of the widget. It provides access to public behavior of the widget.
    *
    * The controller is instantiated on [load] and destroyed in [close].
    * Null controller means that either the [load] hasn't been called yet, or [close] has already been called.
    *
    * The controller is provided by the [factory], specifically its [WidgetFactory.create] method. However:
    * * If [load] encounters an error, an instance of [ControllerLoadError] is used.
    * * If [factory] is not available (e.g., when deserializing widget), an instance of [ControllerNoFactory] is used.
    */
   var controller: Controller? = null
      private set

   /**
    * The graphics this widget is loaded in.
    *
    * It is responsibility of the caller of the [load] to set this field right after this widget was loaded.
    * There is no restriction where widget is loaded, so this field may be null.
    *
    * This field allows widget to control its lifecycle and context from its controller.
    */
   var ui: WidgetUi? = null

   /**
    * The graphics of the widget. It provides access to public behavior of the widget.
    *
    * The graphics is instantiated on [load] and destroyed in [close].
    * Null graphics means that either the [load] hasn't been called yet, or [close] has already been called.
    *
    * The graphics is provided by the [controller], specifically its [Controller.uiRoot] method.
    */
   var graphics: Pane? = null
      private set

   init {
      properties += state.properties
      fieldsRaw += state.fields
   }

   private val configs = HashMap<String, Config<Any?>>()
   override val configurableGroupPrefix = null
   override val configurableValueSource by lazy {
      object: ConfigValueSource {

         @Suppress("UNCHECKED_CAST")
         override fun register(config: Config<*>) {
            val key = configToRawKeyMapper(config)
            configs[key] = config as Config<Any?>
         }

         override fun initialize(config: Config<*>) {
            if (config.isPersistable()) {
               val key = configToRawKeyMapper(config)
               val source = fieldsRaw
               if (source.containsKey(key))
                  config.valueAsJson = source[key] ?: JsNull
            }
         }
      }
   }

   /** Content padding or null if left up on skin to decide. */
   val padding by cvn<Insets>(null).def(name = "Padding", group = "Widget", info = "Content padding or null if left up on skin to decide`. ")

   /** Whether this container loads eagerly or lazily by user. */
   val loadTypeCfg by cv(loadType).noPersist().def(name = "Load type", group = "Widget", info = "Whether this container loads eagerly or lazily by user")

   /** Whether this container or any parent is locked. */
   val lockedCfg by cv(locked).noPersist().def(name = "Locked", group = "Widget", info = "Whether this widget is locked")

   /** Whether this container or any parent is locked. */
   val lockedUnderCfg by cvro(lockedUnder).noPersist().def(name = "Locked (under)", group = "Widget", info = "Whether this widget or any parent is locked", editable = EditMode.APP)

   /** Whether this factory will be preferred on widget `find and create` requests. */
   val preferred by cv(false).def(name = "Is preferred", group = "Widget",  info = "Prefer this widget on `find and create`. ")

   /** Whether this factory will be ignored on widget `find and create` requests. */
   val forbidUse by cv(false).def(name = "Is ignored", group = "Widget", info = "Ignore this widget on `find and create`.")

   /** Name displayed in gui. Customizable. Default is component type name. */
   val customName by cv("").def(name = "Custom name", group = "Widget", info = "Name displayed in gui. User can set his own. By default component type name.")

   override val location: File
      get() = factory.location

   override val userLocation: File
      get() = fieldsRaw["node"]?.asJsStringValue()?.let { APP.location.user.widgets/it } ?: factory.userLocation

   /** [location] as a [Config]. */
   private val locationConfig: File by c(location).def(
      name = "Directory",
      group = "widget",
      info = "Directory of this widget. Where the sources are located.",
      editable = EditMode.NONE
   )

   /** [userLocation] as a [Config]. */
   private val locationUserConfig: File by c(userLocation).def(
      name = "Directory (user data)",
      group = "widget",
      info = "Directory of this widget. Where the user data are located.",
      editable = EditMode.NONE
   )

   /** Whether this widget is active/focused. Each window has 0 or 1 active widgets. Default false. */
   private val focusedImpl = ReadOnlyBooleanWrapper(false)

   /**
    * Whether this widget is active/focused.
    *
    * Each window has 0 or 1 active widgets.
    * Focused widget may not contain actual focus (for example when it has no focus traversable content).
    * Default false.
    */
   val focused by cvro(false) { focusedImpl.readOnlyProperty.readOnly() }.def(
      name = "Focused",
      group = "widget",
      info = "Whether widget is active/focused. Each window has 0 or 1 active widgets. Focused widget may not contain actual focus (for example when it widget has no focus traversable content).",
      editable = EditMode.APP
   )

   /**
    * Whether this widget has been created from persisted state or normally by application/user.
    * May be used for sensitive initialization, like setting default values that must not override user settings.
    */
   val isDeserialized: Boolean = isDeserialized

   /** Invoked in [close], after [graphics] and [controller] are disposed. */
   val onClose = Disposer()
   private var isClosed = false
   @Experimental("Temporary") val onLoad = Handler0()

   constructor(id: UUID, factory: WidgetFactory<*>, isDeserialized: Boolean): this(factory, isDeserialized, WidgetDb(id)) {
      forceLoading = false
      customName.value = factory.name
      focused.addListener(computeFocusChangeHandler())
   }

   constructor(state: WidgetDb): this(APP.widgetManager.factories.getFactory(state.factoryId).orNone(), true, state) {
      forceLoading = false
      customName.value = state.nameUi
      focused.addListener(computeFocusChangeHandler())
      properties.entries.stream()
         .filter { e -> e.key.startsWith("io") }
         .map { e -> WidgetIo(this, e.key.substring(2), (e.value as String?)!!) }
         .forEach { e -> WidgetIoManager.ios.add(e!!) }
   }

   override val name: String
      get() = customName.value


   /** Initializes (if not yet) and returns non-null [controller] and [graphics]. */
   @Idempotent
   override fun load(): Node {
      failIfNotFxThread()
      if (graphics==null) {
         controller = controller ?: instantiateController()
         if (controller==null) {
            val c = ControllerLoadError(this)
            graphics = c.uiRoot()
            controller = c
            onLoad()
         } else {
            try {
               graphics = controller!!.uiRoot()
               val isLegacy = controller!!.javaClass.isAnnotationPresent(LegacyController::class.java)
               if (isLegacy) restoreConfigs()
               graphics!!.sceneProperty() attach { IOLayer.componentSceneChanged(this) } on onClose
               updateIO()
            } catch (e: Throwable) {
               val c = ControllerLoadError(this)
               graphics = c.uiRoot()
               controller = c
               logger.error(e) { "$logName graphics creation failed" }
            }
         }
      }
      return graphics!!
   }

   val isLoaded: Boolean
      get() = graphics!=null

   private fun instantiateController(): Controller? {
      restoreDefaultConfigs()

      val c = factory.controllerType
      logger.info { "Instantiating $logName controller" }
      return runTry { c.java.getDeclaredConstructor(Widget::class.java).newInstance(this) }
         .ifError { APP.actionStream(RuntimeException("Instantiating $logName widget controller ($c) failed", it)) }
         .orNull()
   }

   @Idempotent
   override fun close() {
      if (isClosed) return
      isClosed = true
      logger.info { "$logName closing" }
      super.close()

      controller?.let { c ->
         controller = null
         val ips = c.io.i.getInputs()
         val ops = c.io.o.getOutputs()
         ips.forEach { IOLayer.componentPutRemoved(id, it) }
         ops.forEach { IOLayer.componentPutRemoved(id, it) }
         c.close()
         ips.forEach { it.dispose() }
         ops.forEach { it.dispose() }
      }

      graphics?.onNodeDispose()
      graphics?.removeFromParent()
      graphics = null
      ui?.dispose()

      WidgetIoManager.ios.removeIf { it.widget===this }
      onClose.invoke()
   }

   /** Sets state of this widget to that of a target widget. */
   fun setStateFrom(w: Widget) {
      // if widget was loaded we store its latest state, otherwise it contains serialized pme
      if (w.controller!=null) w.storeConfigs()
      properties.clear()
      properties.putAll(w.properties)
      padding.value = w.padding.value
      preferred.value = w.preferred.value
      forbidUse.value = w.forbidUse.value
      customName.value = w.customName.value
      loadType.value = w.loadType.value
      locked.value = w.locked.value

      // if this widget is loaded we apply state, otherwise it is done when it loads
      if (controller!=null) restoreConfigs()
      properties.entries.stream()
         .filter { it.key.startsWith("io") }
         .map { WidgetIo(this, it.key.substring(2), it.value as String) }
         .forEach { WidgetIoManager.ios.add(it) }
      if (controller!=null) updateIO()
   }

   override fun focus() {
      if (isClosed) return
      if (isLoaded) graphics!!.displayed.sync1If({ it }) { controller?.focus() } on onClose
      else onLoad.attach1 { focus() }
   }

   fun focusAndTraverse() {
      if (isLoaded) {
         controller!!.focus()
         focusAndTraverseFromToRoot()
      }
   }

   private fun computeFocusChangeHandler(): ChangeListener<Boolean> = ChangeListener { _, _, nv ->
         if (isLoaded) {
            graphics?.findParent { WidgetUi.STYLECLASS in it.styleClass }
               ?.asIs<Pane?>()
               ?.pseudoClassChanged("active", nv)
         }
      }

   override fun getConfig(name: String): Config<Any?>? = configs.values.find { it.name==name } ?: controller?.getConfig(name)

   override fun getConfigs(): Collection<Config<Any?>> = configs.values.toList() + controller?.getConfigs().orEmpty()

   val fieldsRaw: MutableMap<String, JsValue?>
      get() = properties.computeIfAbsent("fields") { HashMap<String, JsValue?>() }.asIs()

   private val logName: String
      get() = "Widget(name=${customName.value}, factory=${factory.id})"

   override fun toString() = this::class.toString() + " " + factory.id

   override fun toDb(): WidgetDb {
      val isLoaded = controller!=null

      // Prepare input-outputs
      // If widget is loaded, we serialize inputs & outputs
      // Otherwise we still have the deserialized inputs/outputs leave them as they are
      if (isLoaded) {
         controller!!.io.i.getInputs().forEach { i: Input<*> ->
            properties["io" + i.name] = i.getSources().joinToString(":") { it.id.toString() }
         }
      }

      // Prepare configs
      // If widget is loaded, we serialize name:value pairs
      // Otherwise we still have the deserialized name:value pairs and leave them as they are
      if (isLoaded) {
         storeConfigs()
      }

      return WidgetDb(id, factory.id, customName.value, loadType.value, locked.value, properties - "fields" - "factory", fieldsRaw)
   }

   private fun storeConfigs() {
      // We store only Controller configs as configs of this widget should be defined in this
      // class as fields and serialized/deserialized automatically. Only Controller is created
      // manually, Widget configs should be independent and full auto...

      // 1) nothing to serialize
      // 2) serializing empty list could actually rewrite previously serialized properties,
      //    not yet restored due to widget not yet loaded
      // 3) easy optimization
      if (controller==null) return
      val configsRaw = fieldsRaw
      getConfigs().forEach { c ->
         if (c.isPersistable())
            configsRaw[configToRawKeyMapper(c)] = c.valueAsJson
      }
   }

   private fun restoreConfigs() {
      val configsRaw = fieldsRaw
      if (configsRaw.isNotEmpty()) {
         getConfigs().forEach { c ->
            if (c.isPersistable()) {
               val key = configToRawKeyMapper(c)
               if (key in configsRaw)
                  c.valueAsJson = configsRaw[key] ?: JsNull
            }
         }
         configsRaw.clear() // restoration can only ever happen once
      }
   }

   @Blocking
   fun storeDefaultConfigs() {
      failIf(!isLoaded) { "Must be loaded to export default configs" }

      val configFile = userLocation / "default.json"
      val configs = getConfigs().filter { it.isPersistable() && !Objects.deepEquals(it.value, it.defaultValue) }
      val configuration = Configuration(configToRawKeyMapper).apply { collect(configs) }
      val props = configuration.save(configFile)
      APP.widgetManager.factories.defaultConfigs[userLocation] = props
   }

   private fun restoreDefaultConfigs() {
      APP.widgetManager.factories.defaultConfigs[userLocation]?.forEach(fieldsRaw::putIfAbsent)
   }

   @Blocking
   fun clearDefaultConfigs() {
      val configFile = userLocation / "default.json"
      configFile.del()
   }

   // called when widget is loaded/closed (or rather, when inputs or outputs are created/removed)
   // we need to create i/o nodes and i/o connections
   private fun updateIO() {
      // because widget inputs can be bound to other widget outputs, and because widgets can be
      // loaded passively (then its i/o does not exist yet), we need to update all widget i/os
      // because we do not know which bind to this widget
      IOLayer.componentSceneChanged(this)
      requestWidgetIOUpdate()
   }

   /** Widget metadata. Use by annotating [Controller] class. Legacy, use [WidgetCompanion] if possible.  */
   @Retention(RUNTIME)
   @Target(ANNOTATION_CLASS, CLASS)
   annotation class Info(
      /** Name of the widget. "" by default. */
      val name: String = "",
      /** Description of the widget. */
      val description: String = "",
      /** Version of the widget */
      val version: String = "",
      /** Author of the widget */
      val author: String = "",
      /** Co-developer of the widget. */
      val contributor: String = "",
      /** Last time of change. */
      val year: String = "",
      /** Information regarding how to use widget. */
      val howto: String = "",
      /** Any words from the author. */
      val notes: String = "",
      /** Tags categorizing the widget. Default empty. */
      val tags: Array<WidgetTag> = []
   )

   enum class LoadType {
      AUTOMATIC, MANUAL
   }

   companion object {
      private val logger = KotlinLogging.logger { }

      val configToRawKeyMapper = F1 { it: Config<*> -> it.name.replace(' ', '_').lowercase() }

      val focusChangedHandler: (Node?, Boolean) -> Unit = { n, allowTraversal ->
         val window = n?.scene?.window
         if (n!=null && window!=null) {
            val widgets = APP.widgetManager.widgets.findAll(WidgetSource.OPEN).filter { it.window===window }.toList()
            widgets.find {
               it.ui?.root?.isAnyParentOf(n) ?: false
            }.ifNotNull { fw ->
               widgets.forEach { w -> if (w!==fw) w.focusedImpl.value = false }
               fw.focusedImpl.value = true
               if (allowTraversal) fw.focusAndTraverseFromToRoot()
            }
         }
      }

   }
}