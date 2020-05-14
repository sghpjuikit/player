package sp.it.pl.plugin

import de.jensd.fx.glyphs.GlyphIcons
import javafx.collections.FXCollections.observableArrayList
import mu.KLogging
import sp.it.pl.main.APP
import sp.it.pl.main.App.Rank.MASTER
import sp.it.pl.main.AppErrors
import sp.it.pl.main.AppSettings
import sp.it.pl.main.run1AppReady
import sp.it.util.Locatable
import sp.it.util.collections.ObservableListRO
import sp.it.util.collections.materialize
import sp.it.util.conf.ConfigDelegator
import sp.it.util.conf.ConfigValueSource.Companion.SimpleConfigValueStore
import sp.it.util.conf.Configurable
import sp.it.util.conf.GlobalConfigDelegator
import sp.it.util.conf.c
import sp.it.util.conf.collectActionsOf
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.noPersist
import sp.it.util.conf.noUi
import sp.it.util.dev.Idempotent
import sp.it.util.dev.fail
import sp.it.util.dev.failIf
import sp.it.util.dev.failIfNotFxThread
import sp.it.util.file.Util
import sp.it.util.file.div
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import sp.it.util.functional.ifFalse
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.functional.toUnit
import sp.it.util.units.version
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmName

class PluginManager: GlobalConfigDelegator {
   /** Plugin management ui. */
   private var settings by c(this).noPersist().def(name = "Plugins", info = "Manage application plugins", group = "Plugin management")
   /** All installed plugins, observable, writable. */
   private val pluginsObservableImpl = observableArrayList<PluginBox<*>>()!!
   /** All installed plugins, observable, readable. */
   val pluginsObservable = ObservableListRO(pluginsObservableImpl)
   /** All installed plugins. */
   val plugins: Sequence<PluginBox<*>> get() = pluginsObservableImpl.materialize().asSequence()

   /** Install the specified plugins. */
   inline fun <reified P: PluginBase> installPlugin(): Unit = installPlugin(P::class)

   /** Install the specified plugins. */
   fun <P: PluginBase> installPlugin(type: KClass<P>) {
      failIf(type in this) { "There already is plugin instance of type=$type" }

      val plugin = PluginBox(type)
      if (APP.rank==MASTER || !plugin.info.isSingleton)
         pluginsObservableImpl += plugin
   }

   operator fun <P: PluginBase> contains(type: KClass<P>) = pluginsObservableImpl.any { it.type==type }

   /** @return running plugin of the type specified by the argument or null if no such instance */
   fun <P: PluginBase> get(type: KClass<P>): P? = getRaw(type)?.plugin

   /** @return running plugin of the type specified by the generic type argument if no such instance */
   inline fun <reified P: PluginBase> get(): P? = get(P::class)

   /** @return plugin of the type specified by the argument or null if no such instance */
   fun <P: PluginBase> getRaw(type: KClass<P>): PluginBox<P>? = pluginsObservableImpl.find { it.type==type }.asIs()

   /** @return plugin of the type specified by the generic type argument if no such instance */
   inline fun <reified P: PluginBase> getRaw(): PluginBox<P>? = getRaw(P::class)

   inline fun <reified P: PluginBase> getOrStart(): P? = getRaw<P>().ifNotNull { it.start() }?.plugin

   /** Invokes the action with the running plugin of the type specified by the argument or does nothing if no such instance */
   inline fun <P: PluginBase> use(type: KClass<P>, action: (P) -> Unit): Unit = get(type).ifNotNull(action).toUnit()

   /** Invokes the action with the running plugin of the type specified by the generic type argument or does nothing if no such instance. */
   inline fun <reified P: PluginBase> use(noinline action: (P) -> Unit) = use(P::class, action)

}

interface PluginInfo: Locatable {
   /** Name of the plugin */
   val name: String
   /** Short (one line) description of the plugin */
   val description: String
   /** Icon of the plugin. Default null. */
   val icon: GlyphIcons? get() = null
   /** Whether this plugin is supported on the current platform */
   val isSupported: Boolean
   /** Whether this plugin can run in multiple application instances */
   val isSingleton: Boolean
   /** Whether this plugin is enabled by default. Only has effect if [PluginBox.isBundled] is true. */
   val isEnabledByDefault: Boolean
   /** Version of the plugin. Default application version. */
   val version: KotlinVersion get() = APP.version
   /** Author of the plugin. Default empty. */
   val author: String get() = ""
   override val location get() = APP.location.plugins/name
   override val userLocation get() = APP.location.user.plugins/name
}

class EmptyPluginInfo(val type: KClass<*>): PluginInfo {
   override val name = type.simpleName ?: type.jvmName
   override val description = ""
   override val icon = null
   override val isSupported = true
   override val isSingleton = false
   override val isEnabledByDefault = false
   override val version = version(0,0,0)
   override val author = ""
}

/** Plugin is configurable start/stoppable component. */
@Suppress("LeakingThis")
open class PluginBase: SimpleConfigValueStore<Any?>(), Configurable<Any?>, ConfigDelegator {
   private val info = this::class.companionObjectInstance.asIf<PluginInfo>() ?: EmptyPluginInfo(this::class)
   val name = info.name
   val description = info.description
   override val configurableGroupPrefix: String? = "${AppSettings.plugins.name}.${info.name}"
   override val configurableValueSource = this

   /** Invoked on JavaFX application thread. */
   open fun start() = Unit
   /** Invoked on JavaFX application thread. */
   open fun stop() = Unit
}

class PluginBox<T: PluginBase>(val type: KClass<T>, val isEnabledByDefault: Boolean = false): GlobalConfigDelegator, Locatable {
   var plugin: T? = null
   val info = type.companionObjectInstance.asIf<PluginInfo>() ?: EmptyPluginInfo(type)
   val isBundled = true

   override val location get() = APP.location.plugins/info.name
   override val userLocation get() = APP.location.user.plugins/info.name
   override val configurableGroupPrefix get() = "${AppSettings.plugins.name}.${info.name}"

   val enabled by cv(isEnabledByDefault).def(name = "Enable", info = "Enable/disable this plugin").noUi() sync {
      APP.run1AppReady { enable(it) }
   }

   private fun enable(isToBeRunning: Boolean) {
      val wasRunning = isRunning()
      if (wasRunning==isToBeRunning) return

      if (isToBeRunning) {
         val canBeRunning = info.isSupported && sequenceOf(location, userLocation).all { Util.isValidatedDirectory(it) }.ifFalse {
            AppErrors.push("Plugin ${info.name} could not start.", "Directory $location or $userLocation can not be used.")
         }

         if (canBeRunning) start()
         else logger.error { "Plugin ${info.name} could not start..." }
      } else {
         stop()
      }
   }

   @Idempotent
   fun start() {
      failIfNotFxThread()
      if (isRunning()) return
      logger.info { "Starting plugin $type" }

      plugin = runTry {
         val constructor = type.primaryConstructor?.takeIf { it.parameters.isEmpty() } ?: fail { "Plugin must have a primary no-arg constructor" }
         constructor.call().apply {
            collectActionsOf(this)
            APP.configuration.collect(this)
            APP.configuration.rawSet(this)
            start()
         }
      }.orNull {
         logger.error(it) { "Instantiating plugin $type failed" }
      }
   }

   fun isRunning(): Boolean = plugin!=null

   @Idempotent
   fun stop() {
      failIfNotFxThread()
      if (!isRunning()) return
      logger.info { "Stopping plugin $type" }

      plugin?.apply {
         plugin = null
         APP.configuration.rawAdd(this)
         APP.configuration.drop(this)
         runTry {
            stop()
         }.orNull {
            logger.error(it) { "Error while stopping plugin $type" }
         }
      }
   }

   companion object: KLogging()

}