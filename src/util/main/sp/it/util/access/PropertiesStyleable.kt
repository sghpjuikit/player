package sp.it.util.access

import javafx.beans.property.Property
import javafx.css.CssMetaData
import javafx.css.StyleConverter
import javafx.css.Styleable
import javafx.css.StyleableObjectProperty
import javafx.css.StyleableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMemberProperties
import sp.it.util.dev.failIfNot
import sp.it.util.functional.asIs
import sp.it.util.functional.net
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.functional.toUnit
import sp.it.util.functional.traverse
import sp.it.util.type.ConstantReadOnlyProperty
import sp.it.util.type.ConstantReadOnlyPropertyDelegateProvider
import sp.it.util.type.raw

/**
 * Convenience construct for creating styleable JavaFX components' companions.
 * Inherit by the styleable companion object to get [classCssMetaData] with for free.
 */
abstract class StyleableCompanion {

   /** @return all [CssMetaData], including those of the superclass. JavaFX' convention. */
   val classCssMetaData: List<CssMetaData<out Styleable, *>> by lazy {
      failIfNot(this::class.isCompanion) { "${StyleableCompanion::class} must be companion object of ${Styleable::class}" }

      val inherited = this::class.java.declaringClass
         .traverse { it.superclass }.drop(1)
         .mapNotNull { runTry { it.getDeclaredMethod("getClassCssMetaData") }.orNull() }.firstOrNull()
         ?.invoke(null)?.asIs<List<CssMetaData<out Styleable, *>>>()
         .orEmpty()

      val declared = this::class.declaredMemberProperties
         .filter { it.returnType.raw==CssMetaData::class }
         .map { it.call(this).asIs<CssMetaData<out Styleable, *>>() }

      inherited + declared
   }
}

/**
 * Convenience construct for creating styleable JavaFX components' [CssMetaData].
 * Use inside companion object of the [Styleable] component, preferably [StyleableCompanion].
 */
inline fun <S: Styleable, reified T> svMetaData(name: String, converter: StyleConverter<*, T>, initialValue: T, value: KProperty<StyleableProperty<T>>) = ConstantReadOnlyProperty<Any, CssMetaData<S, T>>(
   object: CssMetaData<S, T>(name, converter, initialValue) {
      override fun isSettable(styleable: S) = value.getter.call(styleable).net { it !is Property<*> || !it.isBound }
      override fun getStyleableProperty(styleable: S) = value.getter.call(styleable)
   }
)

/**
 * Convenience construct for creating styleable JavaFX components' [StyleableObjectProperty].
 * Use inside the [Styleable] component, ideally using metadata defined by [svMetaData].
 */
inline fun <reified T> sv(metadata: CssMetaData<out Styleable, T>) = ConstantReadOnlyPropertyDelegateProvider<Styleable, StyleableObjectProperty<T>> { styleable, property ->
   val svInitialValue = metadata.asIs<CssMetaData<Styleable, T>>().getInitialValue(styleable)
   object: StyleableObjectProperty<T>(svInitialValue) {
      override fun invalidated() = get().toUnit()
      override fun getBean() = styleable
      override fun getName() = property.name
      override fun getCssMetaData() = metadata
      override fun set(v: T) {
         when(T::class) {
            Byte::class -> super.set(v?.asIs<Number>()?.toByte() as T)
            Short::class -> super.set(v?.asIs<Number>()?.toShort() as T)
            Int::class -> super.set(v?.asIs<Number>()?.toInt() as T)
            Long::class -> super.set(v?.asIs<Number>()?.toLong() as T)
            Float::class -> super.set(v?.asIs<Number>()?.toFloat() as T)
            Double::class -> super.set(v?.asIs<Number>()?.toDouble() as T)
            else -> super.set(v)
         }
      }
   }
}

inline fun <reified T: Enum<T>> enumConverter(): StyleConverter<String, T> = StyleConverter.getEnumConverter(T::class.java)