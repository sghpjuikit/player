package sp.it.pl.ui.objects.icon

import de.jensd.fx.glyphs.GlyphIcons
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView
import de.jensd.fx.glyphs.materialicons.MaterialIcon
import de.jensd.fx.glyphs.materialicons.MaterialIconView
import de.jensd.fx.glyphs.octicons.OctIcon
import de.jensd.fx.glyphs.octicons.OctIconView
import de.jensd.fx.glyphs.weathericons.WeatherIcon
import de.jensd.fx.glyphs.weathericons.WeatherIconView
import javafx.scene.text.Font
import sp.it.util.collections.mapset.MapSet
import sp.it.util.dev.fail
import sp.it.util.dev.failIf
import sp.it.util.functional.Try
import sp.it.util.functional.asIs
import sp.it.util.functional.runTry
import sp.it.util.type.Util.getEnumConstants
import kotlin.reflect.KClass

object Glyphs {

   private var GLYPH_ENUM_TYPES: Set<KClass<out GlyphIcons>> = setOf(
      FontAwesomeIcon::class,
      WeatherIcon::class,
      MaterialDesignIcon::class,
      MaterialIcon::class,
      OctIcon::class
   )

   /** Collection of all glyphs types. It is not guaranteed that [GlyphIcons] is [Enum]. */
   var GLYPH_TYPES: Set<KClass<out GlyphIcons>> = GLYPH_ENUM_TYPES + UnicodeIcon::class

   /** Collection of enum-based (not all!) glyphs mapped to id that identify them. */
   val GLYPHS: MapSet<String, GlyphIcons> = GLYPH_ENUM_TYPES
      .asSequence()
      .flatMap { getEnumConstants<GlyphIcons>(it.java).asSequence() }
      .toCollection(MapSet { it.javaClass.simpleName + "." + it.name() })

   /** Inverse of [GlyphIcons.id]. Throws if fails. */
   operator fun get(id: String): Try<GlyphIcons, Throwable> = runTry {
      when {
         id.startsWith("UnicodeIcon.") -> UnicodeIcon(id.substringAfter("Unicode.").toInt())
         else -> GLYPHS[id] ?: fail { "No such icon=$id" }
      }
   }

   fun <ICON: GlyphIcons> valuesOf(type: KClass<ICON>): Sequence<ICON> = when (type) {
      UnicodeIcon::class -> UnicodeIcon.values().asIs()
      FontAwesomeIcon::class -> FontAwesomeIcon.values().asSequence().asIs()
      WeatherIcon::class -> WeatherIcon.values().asSequence().asIs()
      MaterialDesignIcon::class -> MaterialDesignIcon.values().asSequence().asIs()
      MaterialIcon::class -> MaterialIcon.values().asSequence().asIs()
      OctIcon::class -> OctIcon.values().asSequence().asIs()
      else -> fail { "Unsupported glyph: $this" }
   }

   init {
      Font.loadFont(FontAwesomeIconView::class.java.getResource(FontAwesomeIconView.TTF_PATH).openStream(), 10.0)
      Font.loadFont(WeatherIconView::class.java.getResource(WeatherIconView.TTF_PATH).openStream(), 10.0)
      Font.loadFont(MaterialDesignIconView::class.java.getResource(MaterialDesignIconView.TTF_PATH).openStream(), 10.0)
      Font.loadFont(MaterialIconView::class.java.getResource(MaterialIconView.TTF_PATH).openStream(), 10.0)
      Font.loadFont(OctIconView::class.java.getResource(OctIconView.TTF_PATH).openStream(), 10.0)
   }
}

/** Inverse of [Glyphs.get] */
fun GlyphIcons.id(): String = when (this) {
   is UnicodeIcon -> "UnicodeIcon.${name()}"
   is FontAwesomeIcon -> "FontAwesomeIcon.${name()}"
   is WeatherIcon -> "WeatherIcon.${name()}"
   is MaterialDesignIcon -> "MaterialDesignIcon.${name()}"
   is MaterialIcon -> "MaterialIcon.${name()}"
   is OctIcon -> "OctIcon.${name()}"
   else -> fail { "Unsupported glyph: $this" }
}

data class UnicodeIcon(val codePoint: Int): GlyphIcons {

   init {
      failIf(codePoint !in 0..0x10FFFF) { "$codePoint is not unicode code point" }
   }

   override fun characterToString() = String(IntArray(1) { codePoint }, 0, 1)
   override fun getChar() = codePoint.toChar()
   override fun unicodeToString() = String.format("\\u%04x", codePoint)
   override fun name() = unicodeToString()
   override fun getFontFamily() = Font.getDefault().family.orEmpty()

   companion object {
      fun values(): Sequence<UnicodeIcon> = sequence {
         yieldAll((0..1114111).asSequence().map(::UnicodeIcon))
      }
   }
}