package sp.it.pl.layout.widget

import de.jensd.fx.glyphs.GlyphIcons
import sp.it.pl.layout.widget.feature.Feature
import sp.it.pl.main.APP
import sp.it.pl.main.toUi
import sp.it.pl.ui.pane.ShortcutPane
import sp.it.util.file.properties.PropVal.PropVal1
import sp.it.util.functional.net
import sp.it.util.functional.toUnit
import sp.it.util.text.camelToDotCase
import sp.it.util.type.isSuperclassOf
import java.time.Year
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.jvmName

interface ComponentInfo {

   /** Name of the component for ui */
   val name: String

   /** Component summary description text for ui */
   val summaryUi: String get() = name

}

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

   /** Author of the widget */
   val author: String

   /** Co-developer of the widget */
   val contributor: String

   /** Creation time */
   val year: Year

   /** Whether this widget is supported on the current platform */
   val isSupported: Boolean

   /** Widget group */
   val group: Widget.Group

   /** Exact type of the widget (also denotes widget's controller type) */
   val type: Class<*>

   /** All features the widget's controller implements */
   val features get() = type.kotlin.allSuperclasses.mapNotNull { it.findAnnotation<Feature>() }

   /** @return true iff widget's controller implements given feature */
   fun hasFeature(feature: Feature) = hasFeature(feature.type)

   /** @return true iff widget's controller implements feature of given type */
   fun hasFeature(feature: KClass<*>) = hasFeature(feature.java)

   /** @return true iff widget's controller implements feature of given type */
   fun hasFeature(feature: Class<*>) = feature.isSuperclassOf(type)

   /** @return true iff widget's controller implements all given features */
   fun hasFeatures(vararg features: Class<*>) = features.all { hasFeature(it) }

   val summaryActions get() = listOf<ShortcutPane.Entry>()

   override val summaryUi: String get() {
      val fs = features
      return "Component: Widget ${name.toUi()}\n" +
         ("Version: ${version.toUi()}\n") +
         ("Year: ${year.toUi()}\n") +
         (if (description.isEmpty()) "" else "Info: $description\n") +
         (if (descriptionLong.isEmpty()) "" else "$descriptionLong\n") +
         "Features: " + (if (fs.isEmpty()) "none" else fs.joinToString { "\n\t${it.name} - ${it.description}" })
   }

}

/**
 * Widget controller companion object.
 * * Defines useful widget metadata
 * * Automatically derives [id] and [type].
 * * Allows defining global widget instance state using [appProperty]
 */
interface WidgetCompanion: WidgetInfo {
   override val id
      get() = type.kotlin.let { it.simpleName ?: it.jvmName }

   override val type: Class<*>
      get() = this::class.java.enclosingClass!!
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
