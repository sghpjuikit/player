package sp.it.pl.layout

import de.jensd.fx.glyphs.GlyphIcons
import java.time.Year
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.jvm.jvmName
import sp.it.pl.layout.feature.Feature
import sp.it.pl.main.APP
import sp.it.pl.main.toUi
import sp.it.pl.ui.pane.ShortcutPane
import sp.it.util.file.properties.PropVal.PropVal1
import sp.it.util.functional.net
import sp.it.util.functional.toUnit
import sp.it.util.text.camelToDotCase
import sp.it.util.text.ifNotEmpty

interface ComponentInfo {

   /** Name of the component for ui */
   val name: String

   /** Component summary description text for ui */
   val summaryUi: String get() = name

}

typealias WidgetTag = String

interface WidgetInfo: ComponentInfo {

   /** Widget type identifier (shared for all widget instances of the same type) */
   val id: String

   /** Icon of the widget */
   val icon: GlyphIcons?

   /** Short (one line) description of the widget */
   val description: String

   /** Full description of the widget */
   val descriptionLong: String

   /** Version of the widget */
   val version: KotlinVersion

   /** Author of the widget, either full name or alias */
   val author: String

   /** Co-developer of the widget. comma separated list of names or aliases of notable contributors */
   val contributor: String

   /** Creation time */
   val year: Year

   /** Whether this widget is supported on the current OS platform */
   val isSupported: Boolean

   /** Tags labeling/categorizing the widget. May be empty. For common tags see [sp.it.pl.main.WidgetTags] */
   val tags: Set<WidgetTag>

   /** Exact type of the widget (also denotes widget's controller type) */
   val type: KClass<*>

   /** All features the widget's controller implements */
   val features
      get() = type.allSuperclasses.mapNotNull { it.findAnnotation<Feature>() }

   /** @return true iff widget's controller implements given feature */
   fun hasFeature(feature: Feature) = hasFeature(feature.type)

   /** @return true iff widget's controller implements feature of given type */
   fun hasFeature(feature: KClass<*>) = feature.isSuperclassOf(type)

   /** @return true iff widget's controller implements all given features */
   fun hasFeatures(vararg features: KClass<*>) = features.all { hasFeature(it) }

   /** @return summary of available UX actions, typically keyboard and mouse shortcuts, displayed in widget help page */
   val summaryActions
      get() = listOf<ShortcutPane.Entry>()

   override val summaryUi: String
      get() = """
         |Component: Widget ${name.toUi()}
         |Version: ${version.toUi()}
         |Year: ${year.toUi()}
         |${description.ifNotEmpty { "Info: $it\n" }}${descriptionLong.ifNotEmpty { "$it\n" }}Tags: ${tags.joinToString(", ")}
         |Features: ${features.net { if (it.isEmpty()) "none" else it.joinToString { "\n\t${it.name} - ${it.description}" } }}
      """.trimMargin()
}

/**
 * Widget controller companion object. Use by having the widget's companion object extend it.
 * * Defines useful widget metadata
 * * Automatically derives [id] and [type].
 * * Allows defining global widget instance state using [appProperty]
 */
interface WidgetCompanion: WidgetInfo {

   override val id
      get() = type.let { it.simpleName ?: it.jvmName }

   override val type: KClass<*>
      get() = this::class.java.enclosingClass!!.kotlin

   /** Invoked by [WidgetFactory.init] */
   fun init() = Unit

   /** Invoked by [WidgetFactory.dispose] */
   fun dispose() = Unit

}

/**
 * Global widget state (shared across all widget instances) get/set/persisted from/to application properties.
 * The key is 'widget.${widgetCompanion.id.camelToDotCase()}.${property.name.camelToDotCase()}'.
 *
 * Because the key uses [WidgetCompanion.id], the global states of widgets of different types do not conflict.
 */
@Suppress("UNCHECKED_CAST", "FINAL_UPPER_BOUND")
fun <T: String> appProperty(initialValue: T) = object: ReadWriteProperty<WidgetCompanion, T> {
   private fun key(c: WidgetCompanion, p: KProperty<*>) = "widget.${c.id.camelToDotCase()}.${p.name.camelToDotCase()}"
   override fun getValue(thisRef: WidgetCompanion, property: KProperty<*>) = APP.configuration.rawGet(key(thisRef, property))?.val1?.net { it as T } ?: initialValue
   override fun setValue(thisRef: WidgetCompanion, property: KProperty<*>, value: T) = APP.configuration.rawAdd(key(thisRef, property), PropVal1(value)).toUnit()
}
