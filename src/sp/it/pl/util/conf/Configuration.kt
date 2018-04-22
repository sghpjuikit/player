package sp.it.pl.util.conf

import sp.it.pl.main.isEditableByUser
import sp.it.pl.util.action.Action
import sp.it.pl.util.action.ActionRegistrar
import sp.it.pl.util.collections.mapset.MapSet
import sp.it.pl.util.conf.ConfigurationUtil.configsOf
import sp.it.pl.util.file.Properties
import sp.it.pl.util.file.Properties.Property
import sp.it.pl.util.functional.compose
import sp.it.pl.util.functional.seqOf
import java.io.File
import java.lang.invoke.MethodHandles
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Predicate
import java.util.stream.Stream

/** Provides methods to access configs. */
open class Configuration {

    private val methodLookup = MethodHandles.lookup()
    private val mapper: (String) -> String = { s -> s.replace(' ', '_').toLowerCase() }
    private val configToRawKeyMapper = compose({ c: Config<*> -> "${c.group}.${c.name}" }, mapper)
    private val properties = ConcurrentHashMap<String, String>()
    private val configs = MapSet(ConcurrentHashMap(), configToRawKeyMapper)

    fun getFields(): Collection<Config<*>> = configs

    /**
     * Returns raw key-value ([java.lang.String]) pairs representing the serialized configs.
     *
     * @return modifiable thread safe map of key-value property pairs
     */
    fun rawGetAll(): Map<String, String> = properties

    fun rawGet(key: String): String = properties[key]!!

    fun rawContains(config: String): Boolean = properties.containsKey(config)

    fun rawContains(config: Config<*>): Boolean = properties.containsKey(configToRawKeyMapper(config))

    fun rawAdd(name: String, value: String) = properties.put(name, value)

    fun rawAdd(properties: Map<String, String>) = this.properties.putAll(properties)

    fun rawAdd(file: File) = Properties.load(file).forEach { name, value -> rawAdd(name, value) }

    fun rawRemProperty(key: String) = properties.remove(key)

    fun rawRemProperties(properties: Map<String, String>) = properties.forEach { name, _ -> rawRemProperty(name) }

    fun rawRem(file: File) = Properties.load(file).forEach { name, _ -> rawRemProperty(name) }

    fun <C> collect(c: Configurable<C>) = collect(c.fields)

    fun <C> collect(c: Collection<Config<C>>): Unit = c.forEach(::collect)

    fun <C> collect(vararg cs: Configurable<C>): Unit = cs.forEach { collect(it) }

    fun collectStatic(vararg notAnnotatedClasses: Class<*>): Unit = seqOf(*notAnnotatedClasses)
            .distinct()
            .forEach { collect(configsOf(it, null, true, false)) }


    fun <C> collect(config: Config<C>) {
        configs += config

        // generate boolean toggle actions
        config.takeIf { it.isEditableByUser() && it.type==Boolean::class.javaObjectType }
                ?.let {
                    val name = "${it.guiName} - toggle"
                    val description = "Toggles value ${it.name} between true/false"
                    val r = Runnable { if (it.isEditableByUser()) it.setNextNapplyValue() }
                    val a = Action(name, r, description, it.group, "", false, false)
                    ActionRegistrar.getActions() += a
                    configs += a
                }

        // generate enumerable loopNext actions
        // TODO

        if (config is Action) {
            ActionRegistrar.getActions() += config
            config.register()
        }
    }

    fun <T> drop(config: Config<T>) {
        configs.remove(config)

        if (config is Action) {
            config.unregister()
            ActionRegistrar.getActions() -= config
        }
    }

    @SafeVarargs
    fun <T> drop(vararg configs: Config<T>) = seqOf(*configs).forEach { drop(it) }

    fun <T> drop(configs: Collection<Config<T>>) = configs.forEach { drop(it) }

    fun getFields(condition: Predicate<Config<*>>): Stream<Config<*>> = getFields().stream().filter(condition)

    /** Changes all config fields to their default value and applies them  */
    fun toDefault() = getFields().forEach { it.setNapplyDefaultValue() }

    /**
     * Saves configuration to the file. The file is created if it does not exist,
     * otherwise it is completely overwritten.
     * Loops through Configuration fields and stores them all into file.
     */
    fun save(title: String, file: File) {
        val comment = (" $title property file\n"
                +" Last auto-modified: ${LocalDateTime.now()}\n"
                +"\n"
                +" Properties are in the format: {property path}.{property name}{separator}{property value}\n"
                +" \t{property path}  must be lowercase with '.' as path separator, e.g.: this.is.a.path\n"
                +" \t{property name}  must be lowercase and contain no spaces (use underscores '_' instead)\n"
                +" \t{separator}      must be ' = ' string\n"
                +" \t{property value} can be any string (even empty)\n"
                +" Properties must be separated by (any) combination of '\\n', '\\r' characters\n"
                +"\n"
                +" Ignored lines:\n"
                +" \tcomment lines (start with '#' or '!')\n"
                +" \tempty lines\n"
                +"\n"
                +" Some properties may be read-only or have additional value constraints. Such properties will ignore "
                +"custom or unfit values")

        val propsRaw = properties.asSequence().associateBy({ it.key }, { Property("", it.value) })
        val propsCfg = configs.asSequence()
                .filter { it.type!=Void::class.java }
                .associateBy(configs.keyMapper, { Property(it.info, it.valueS) })

        Properties.saveP(file, comment, propsRaw+propsCfg)
    }

    /**
     * Loads previously saved configuration file and set its values for this.
     *
     * Attempts to load all configuration fields from file. Fields might not be
     * read either through I/O error or parsing errors. Parsing errors are
     * recoverable, meaning corrupted fields will be ignored.
     * Default values will be used for all unread fields.
     *
     * If field of given name does not exist it will be ignored as well.
     */
    fun rawSet() {
        properties.forEach { key, value ->
            val c = configs[mapper(key)]
            if (c!=null && c.isEditable.isByApp) c.valueS = value
        }
    }

    fun rawSet(c: Config<*>) {
        if (c.isEditable.isByApp) {
            val key = configToRawKeyMapper(c)
            if (properties.containsKey(key))
                c.valueS = properties[key]
        }
    }

}