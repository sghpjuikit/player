package sp.it.util.conf

import java.io.File
import java.lang.invoke.MethodHandles
import java.util.concurrent.ConcurrentHashMap
import mu.KLogging
import sp.it.util.access.toggle
import sp.it.util.action.Action
import sp.it.util.action.ActionRegistrar
import sp.it.util.collections.mapset.MapSet
import sp.it.util.collections.observableList
import sp.it.util.collections.readOnly
import sp.it.util.file.json.JsObject
import sp.it.util.file.json.JsValue
import sp.it.util.file.json.toPrettyS
import sp.it.util.file.properties.PropVal
import sp.it.util.file.properties.readProperties
import sp.it.util.file.writeSafely
import sp.it.util.file.writeTextTry
import sp.it.util.functional.compose
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.type.atomic
import sp.it.util.type.isSubclassOf

/** Persistable [Configurable]. */
open class Configuration(nameMapper: ((Config<*>) -> String) = nameMapperDefault): Configurable<Any?> {
companion object: KLogging() {
   val nameMapperDefault: ((Config<*>) -> String) = { "${it.group}.${it.name}" }
   val namePostMapperDefault: (String) -> String = { s -> s.replace(' ', '_').lowercase() }
   val configToRawKeyMapperDefault = nameMapperDefault compose namePostMapperDefault
}
   private val methodLookup = MethodHandles.lookup()
   private val namePostMapper: (String) -> String = { s -> s.replace(' ', '_').lowercase() }
   private val configToRawKeyMapper = nameMapper compose namePostMapper
   private var propertiesLegacy: Map<String, PropVal>? by atomic(null)
   private var properties = ConcurrentHashMap<String, JsValue>()
   private val configs: MapSet<String, Config<*>> = MapSet(ConcurrentHashMap(), configToRawKeyMapper)
   private val configsObservableImpl = observableList<Config<*>>()
   val configsObservable = configsObservableImpl.readOnly()

   @Suppress("UNCHECKED_CAST")
   override fun getConfig(name: String): Config<Any?> = configs.find { it.name==name } as Config<Any?>

   @Suppress("UNCHECKED_CAST")
   override fun getConfigs(): Collection<Config<Any?>> = configs as Collection<Config<Any?>>

   /**
    * Returns raw key-value ([java.lang.String]) pairs representing the serialized configs.
    *
    * @return modifiable thread safe map of key-value property pairs
    */
   fun rawGetAll(): Map<String, JsValue> = properties

   fun rawGet(key: String): JsValue? = properties[key]

   fun rawGet(config: Config<*>): JsValue? = properties[configToRawKeyMapper(config)]

   fun rawContains(config: String): Boolean = properties.containsKey(config)

   fun rawContains(config: Config<*>): Boolean = properties.containsKey(configToRawKeyMapper(config))

   fun rawAdd(configurable: Configurable<*>): Unit = configurable.getConfigs().forEach(::rawAdd)

   fun rawAdd(config: Config<*>) {
      if (config.isPersistable())
         properties[configToRawKeyMapper(config)] = config.valueAsJson
   }

   fun rawAdd(name: String, value: JsValue) = properties.put(name, value)

   fun rawAdd(props: Map<String, JsValue>): Unit = properties.putAll(props)

   fun rawAdd(file: File) {
      runTry {
         val isLegacyFormat = file.useLines { it.firstOrNull()?.startsWith("#")==true }
         if (isLegacyFormat)
            propertiesLegacy = ConcurrentHashMap<String, PropVal>().also { ps ->
               file.readProperties().orNull().orEmpty().forEach { (name, value) -> ps[name] = value }
            }
         else
            Config.json.fromJson<JsObject>(file).orNull()?.value.orEmpty().forEach { (name, value) -> rawAdd(name, value) }
      }
   }

   fun rawRemProperty(key: String) = properties.remove(key)

   fun rawRemProperties(properties: Map<String, *>) = properties.forEach { (name, _) -> rawRemProperty(name) }

   fun rawRem(file: File) = file.readProperties().orNull().orEmpty().forEach { (name, _) -> rawRemProperty(name) }

   fun <C> collect(c: Configurable<C>): Unit = collect(c.getConfigs())

   fun <C> collect(c: Collection<Config<C>>): Unit = c.forEach(::collect)

   fun <C> collect(vararg cs: Configurable<C>): Unit = cs.forEach { collect(it) }

   fun <C> collect(config: Config<C>) {
      configs += config
      configsObservableImpl += config

      // TODO disabled due to:
      // 1 implemented only for boolean, while it should be for any enumerable config
      // 2 unsolved ui (settings) spam, this creates lots of configs and they should be inline
      // 3 the generated actions must be unregistered if config is dropped
      @Suppress("UNCHECKED_CAST", "ConstantConditionIf")
      if (false)
         config.takeIf { it.type.isSubclassOf<Boolean>() && it.isEditable.isByUser }
            ?.let { it as Config<Boolean> }
            ?.let {
               val name = "${it.nameUi} - toggle"
               val description = "Toggles value ${it.name} between true/false"
               val r = Runnable { if (it.isEditableByUserRightNow()) it.toggle() }
               val a = Action(name, r, description, it.group, "", false, false)
               rawSet(a)
               ActionRegistrar.getActions() += a
               configs += a
               configsObservableImpl += config
            }


      if (config is Action) {
         ActionRegistrar.getActions() += config
         config.register()
      }
   }

   fun <T> drop(config: Config<T>) {
      configs -= config
      configsObservableImpl -= config

      if (config is Action) {
         config.unregister()
         ActionRegistrar.getActions() -= config
      }
   }

   fun <C> drop(c: Configurable<C>): Unit = c.getConfigs().forEach(::drop)

   fun <T> drop(configs: Collection<Config<T>>): Unit = configs.forEach(::drop)

   /** Changes all configs to their default value and applies them  */
   fun toDefault(): Unit = getConfigs().forEach { it.setValueToDefault() }

   /**
    * Saves configuration to the file. The file is created if it does not exist, otherwise it is completely overwritten.
    * Loops through configs and stores them all into file.
    */
   fun save(file: File): Map<String, JsValue> {
      val cfg = configs.filter { it.isPersistable() }
      val propsRaw = properties
      val propsCfg = cfg.associate { configToRawKeyMapper(it) to it.valueAsJson }
      val props = propsRaw + propsCfg

      file.writeSafely {
         it.writeTextTry(JsObject(props).toPrettyS())
      }.ifError {
         logger.error(it) { "Couldn't serialize Configuration to file=$file" }
      }

      return props
   }

   /**
    * Loads previously saved configuration file and set its values for this.
    *
    * Attempts to load all configs from file. Configs might not be read either through I/O error or parsing errors.
    * Parsing errors will be ignored.
    *
    * If config of given name does not exist it will be ignored as well.
    */
   fun rawSet() {
      if (propertiesLegacy!=null)
         propertiesLegacy?.forEach { (key, value) ->
            val c = configs[namePostMapper(key)]
            if (c?.isPersistable()==true)
               c.valueAsProperty = value
         }
      else
         properties.forEach { (key, value) ->
            val c = configs[namePostMapper(key)]
            if (c?.isPersistable()==true)
               c.valueAsJson = value
         }
   }

   fun <C> rawSet(c: Configurable<C>): Unit = c.getConfigs().forEach(::rawSet)

   fun <T> rawSet(configs: Collection<Config<T>>): Unit = configs.forEach(::rawSet)

   fun rawSet(c: Config<*>) {
      if (c.isPersistable()) {
         val key = configToRawKeyMapper(c)
         if (propertiesLegacy!=null) {
            if (propertiesLegacy!!.containsKey(key))
               c.valueAsProperty = propertiesLegacy!![key]!!
         } else {
            if (properties.containsKey(key))
               c.valueAsJson = properties[key]!!
         }
      }
   }

}