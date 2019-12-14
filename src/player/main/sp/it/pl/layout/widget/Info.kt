package sp.it.pl.layout.widget

import sp.it.pl.layout.widget.feature.Feature
import sp.it.util.type.isSuperclassOf
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

interface ComponentInfo {

   /** Name of the component for ui */
   val name: String

   /** Component summary description text for ui */
   val summaryUi: String get() = name

}

interface WidgetInfo: ComponentInfo {

   val id: String

   /** @return description of the widget */
   fun description(): String

   /** @return version of the widget */
   fun version(): String

   /** @return author of the widget */
   fun author(): String

   /** @return co-developer of the widget */
   fun contributor(): String

   /** @return last time of change */
   fun year(): String

   /** @return formatted how-to-use text */
   fun howto(): String

   /** @return formatted author notes, generally motivation, bugs or road map, but literally anything */
   fun notes(): String

   /** @return widget group */
   fun group(): Widget.Group

   /** @return exact type of the widget (also denotes widget's controller type) */
   fun type(): Class<*>

   /** @return all features the widget's controller implements */
   fun getFeatures() = type().interfaces.asSequence()
      .mapNotNull { it.kotlin.findAnnotation<Feature>() }
      .distinct()
      .toList()

   /** @return true iff widget's controller implements given feature */
   fun hasFeature(feature: Feature) = hasFeature(feature.type)

   /** @return true iff widget's controller implements feature of given type */
   fun hasFeature(feature: KClass<*>) = hasFeature(feature.java)

   /** @return true iff widget's controller implements feature of given type */
   fun hasFeature(feature: Class<*>) = feature.isSuperclassOf(type())

   /** @return true iff widget's controller implements all given features */
   fun hasFeatures(vararg features: Class<*>) = features.asSequence().all { hasFeature(it) }

   override val summaryUi: String get() {
      val fs = getFeatures()
      return "Component: Widget $name\n" +
         (if (description().isEmpty()) "" else "Info: ${description()}\n") +
         (if (notes().isEmpty()) "" else "${notes()}\n") +
         (if (howto().isEmpty()) "" else "${howto()}\n") +
         "Features: " + (if (fs.isEmpty()) "none" else fs.joinToString { "\n\t${it.name} - ${it.description}" })
   }

}