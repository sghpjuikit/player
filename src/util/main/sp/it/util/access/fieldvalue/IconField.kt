package sp.it.util.access.fieldvalue

import de.jensd.fx.glyphs.GlyphIcons
import kotlin.reflect.KClass

class IconField<T: Any>: ObjectFieldBase<GlyphIcons, T> {

   private constructor(name: String, description: String, type: KClass<T>, extractor: (GlyphIcons) -> T?): super(type, extractor, name, description)

   override fun toS(o: T?, substitute: String): String = ""

   companion object: ObjectFieldRegistry<GlyphIcons, IconField<*>>(GlyphIcons::class) {
      val NAME = this + IconField("Name", "Name of the icon glyph", String::class) { it.name() }
   }

}