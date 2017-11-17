package layout.widget

import layout.widget.feature.Feature
import util.functional.Util.toS
import kotlin.reflect.KClass

interface ComponentInfo {

    /** @return name of the widget as displayed in ui */
    fun nameGui(): String

    /** @return component info as string */
    fun toStr() = nameGui()

}

interface WidgetInfo: ComponentInfo {

    fun name(): String

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

    /** @return formatted author notes, generally motivation, bugs or roadmap, but literally anything */
    fun notes(): String

    /** @return widget group */
    fun group(): Widget.Group

    /** @return exact type of the widget (also denotes widget's controller type) */
    fun type(): Class<*>

    /** @return all features the widget's controller implements */
    fun getFeatures() = type().interfaces.asSequence()
            .map { it.getAnnotation(Feature::class.java) }
            .filterNotNull()
            .distinct()
            .toList()

    /** @return true iff widget's controller implements feature of given type */
    fun hasFeature(feature: Class<*>) = feature.isAssignableFrom(type())

    /** @return true iff widget's controller implements feature of given type */
    fun hasFeature(feature: KClass<*>) = hasFeature(feature.java)

    /** @return true iff widget's controller implements given feature */
    fun hasFeature(feature: Feature) = hasFeature(feature.type)

    /** @return true iff widget's controller implements all given features */
    fun hasFeatures(vararg features: Class<*>) = features.asSequence().all { hasFeature(it) }

    override fun toStr(): String {
        val fs = getFeatures()
        return "Component: Widget\nName: ${nameGui()}\n"+
                (if (description().isEmpty()) "" else "Info: ${description()}\n")+
                (if (notes().isEmpty()) "" else "${notes()}\n")+
                (if (howto().isEmpty()) "" else "${howto()}\n")+
                "Features: "+(if (fs.isEmpty()) "none" else toS(fs) { f -> "\n\t${f.name} - ${f.description}" })
    }

}
