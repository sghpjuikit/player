package sp.it.util.access.fieldvalue

import de.jensd.fx.glyphs.GlyphIcons
import sp.it.util.type.VType
import sp.it.util.type.type

sealed class IconField<T>: ObjectFieldBase<GlyphIcons, T> {

   private constructor(name: String, description: String, type: VType<T>, extractor: (GlyphIcons) -> T, toUi: (T?, String) -> String): super(type, extractor, name, description, toUi)

   object NAME: IconField<String>("Name", "Name of the icon glyph", type(), { it.name() }, { o, or -> o ?: or })

   companion object: ObjectFieldRegistry<GlyphIcons, IconField<*>>(GlyphIcons::class) {
      init { register(NAME) }
   }

}