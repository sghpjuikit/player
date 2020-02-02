package sp.it.util.access.fieldvalue

import de.jensd.fx.glyphs.GlyphIcons
import sp.it.util.type.VType
import sp.it.util.type.type

class IconField<T>: ObjectFieldBase<GlyphIcons, T> {

   private constructor(name: String, description: String, type: VType<T>, extractor: (GlyphIcons) -> T): super(type, extractor, name, description)

   override fun toS(o: T?, substitute: String): String = o?.toString() ?: substitute

   companion object: ObjectFieldRegistry<GlyphIcons, IconField<*>>(GlyphIcons::class) {
      val NAME = this + IconField("Name", "Name of the icon glyph", type<String>()) { it.name() }
   }

}