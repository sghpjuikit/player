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
import sp.it.util.text.Char32
import sp.it.util.text.toChar32
import kotlin.reflect.KClass
import sp.it.util.type.enumValues

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
      .flatMap { it.enumValues.asSequence() }
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

   override fun characterToString() = codePoint.toChar32().toString()
   override fun getChar() = codePoint.toChar()
   override fun unicodeToString() = "\\u%04x".format(codePoint)
   override fun name() = "U+%04x".format(codePoint)
   override fun getFontFamily() = Font.getDefault().family.orEmpty()

   companion object {

      data class UnicodeIconRange(val range: IntRange, val name: String)

      @Suppress("SpellCheckingInspection")
      val unicodeRanges = listOf(
         UnicodeIconRange(0x000020..0x00007F, "Basic Latin"),
         UnicodeIconRange(0x0000A0..0x0000FF, "Latin-1 Supplement"),
         UnicodeIconRange(0x000100..0x00017F, "Latin Extended-A"),
         UnicodeIconRange(0x000180..0x00024F, "Latin Extended-B"),
         UnicodeIconRange(0x000250..0x0002AF, "IPA Extensions"),
         UnicodeIconRange(0x0002B0..0x0002FF, "Spacing Modifier Letters"),
         UnicodeIconRange(0x000300..0x00036F, "Combining Diacritical Marks"),
         UnicodeIconRange(0x002800..0x0028FF, "Braille Patterns"),
         UnicodeIconRange(0x000370..0x0003FF, "Greek and Coptic"),
         UnicodeIconRange(0x000400..0x0004FF, "Cyrillic"),
         UnicodeIconRange(0x000500..0x00052F, "Cyrillic Supplementary"),
         UnicodeIconRange(0x000530..0x00058F, "Armenian"),
         UnicodeIconRange(0x000590..0x0005FF, "Hebrew"),
         UnicodeIconRange(0x000600..0x0006FF, "Arabic"),
         UnicodeIconRange(0x000700..0x00074F, "Syriac"),
         UnicodeIconRange(0x000780..0x0007BF, "Thaana"),
         UnicodeIconRange(0x000900..0x00097F, "Devanagari"),
         UnicodeIconRange(0x000980..0x0009FF, "Bengali"),
         UnicodeIconRange(0x000A00..0x000A7F, "Gurmukhi"),
         UnicodeIconRange(0x000A80..0x000AFF, "Gujarati"),
         UnicodeIconRange(0x000B00..0x000B7F, "Oriya"),
         UnicodeIconRange(0x000B80..0x000BFF, "Tamil"),
         UnicodeIconRange(0x000C00..0x000C7F, "Telugu"),
         UnicodeIconRange(0x000C80..0x000CFF, "Kannada"),
         UnicodeIconRange(0x000D00..0x000D7F, "Malayalam"),
         UnicodeIconRange(0x000D80..0x000DFF, "Sinhala"),
         UnicodeIconRange(0x000E00..0x000E7F, "Thai"),
         UnicodeIconRange(0x000E80..0x000EFF, "Lao"),
         UnicodeIconRange(0x000F00..0x000FFF, "Tibetan"),
         UnicodeIconRange(0x001000..0x00109F, "Myanmar"),
         UnicodeIconRange(0x0010A0..0x0010FF, "Georgian"),
         UnicodeIconRange(0x001100..0x0011FF, "Hangul Jamo"),
         UnicodeIconRange(0x001200..0x00137F, "Ethiopic"),
         UnicodeIconRange(0x0013A0..0x0013FF, "Cherokee"),
         UnicodeIconRange(0x001400..0x00167F, "Unified Canadian Aboriginal Syllabics"),
         UnicodeIconRange(0x001680..0x00169F, "Ogham"),
         UnicodeIconRange(0x0016A0..0x0016FF, "Runic"),
         UnicodeIconRange(0x001700..0x00171F, "Tagalog"),
         UnicodeIconRange(0x001720..0x00173F, "Hanunoo"),
         UnicodeIconRange(0x001740..0x00175F, "Buhid"),
         UnicodeIconRange(0x001760..0x00177F, "Tagbanwa"),
         UnicodeIconRange(0x001780..0x0017FF, "Khmer"),
         UnicodeIconRange(0x001800..0x0018AF, "Mongolian"),
         UnicodeIconRange(0x001900..0x00194F, "Limbu"),
         UnicodeIconRange(0x001950..0x00197F, "Tai Le"),
         UnicodeIconRange(0x0019E0..0x0019FF, "Khmer Symbols"),
         UnicodeIconRange(0x001D00..0x001D7F, "Phonetic Extensions"),
         UnicodeIconRange(0x001E00..0x001EFF, "Latin Extended Additional"),
         UnicodeIconRange(0x001F00..0x001FFF, "Greek Extended"),
         UnicodeIconRange(0x002000..0x00206F, "General Punctuation"),
         UnicodeIconRange(0x002070..0x00209F, "Superscripts and Subscripts"),
         UnicodeIconRange(0x0020A0..0x0020CF, "Currency Symbols"),
         UnicodeIconRange(0x0020D0..0x0020FF, "Combining Diacritical Marks for Symbols"),
         UnicodeIconRange(0x002100..0x00214F, "Letterlike Symbols"),
         UnicodeIconRange(0x002150..0x00218F, "Number Forms"),
         UnicodeIconRange(0x002190..0x0021FF, "Arrows"),
         UnicodeIconRange(0x002200..0x0022FF, "Mathematical Operators"),
         UnicodeIconRange(0x002300..0x0023FF, "Miscellaneous Technical"),
         UnicodeIconRange(0x002400..0x00243F, "Control Pictures"),
         UnicodeIconRange(0x002440..0x00245F, "Optical Character Recognition"),
         UnicodeIconRange(0x002460..0x0024FF, "Enclosed Alphanumerics"),
         UnicodeIconRange(0x002500..0x00257F, "Box Drawing"),
         UnicodeIconRange(0x002580..0x00259F, "Block Elements"),
         UnicodeIconRange(0x0025A0..0x0025FF, "Geometric Shapes"),
         UnicodeIconRange(0x002600..0x0026FF, "Miscellaneous Symbols"),
         UnicodeIconRange(0x002700..0x0027BF, "Dingbats"),
         UnicodeIconRange(0x0027C0..0x0027EF, "Miscellaneous Mathematical Symbols-A"),
         UnicodeIconRange(0x0027F0..0x0027FF, "Supplemental Arrows-A"),
         UnicodeIconRange(0x002900..0x00297F, "Supplemental Arrows-B"),
         UnicodeIconRange(0x002980..0x0029FF, "Miscellaneous Mathematical Symbols-B"),
         UnicodeIconRange(0x002A00..0x002AFF, "Supplemental Mathematical Operators"),
         UnicodeIconRange(0x002B00..0x002BFF, "Miscellaneous Symbols and Arrows"),
         UnicodeIconRange(0x002E80..0x002EFF, "CJK Radicals Supplement"),
         UnicodeIconRange(0x002F00..0x002FDF, "Kangxi Radicals"),
         UnicodeIconRange(0x002FF0..0x002FFF, "Ideographic Description Characters"),
         UnicodeIconRange(0x003000..0x00303F, "CJK Symbols and Punctuation"),
         UnicodeIconRange(0x003040..0x00309F, "Hiragana"),
         UnicodeIconRange(0x0030A0..0x0030FF, "Katakana"),
         UnicodeIconRange(0x003100..0x00312F, "Bopomofo"),
         UnicodeIconRange(0x003130..0x00318F, "Hangul Compatibility Jamo"),
         UnicodeIconRange(0x003190..0x00319F, "Kanbun"),
         UnicodeIconRange(0x0031A0..0x0031BF, "Bopomofo Extended"),
         UnicodeIconRange(0x0031F0..0x0031FF, "Katakana Phonetic Extensions"),
         UnicodeIconRange(0x003200..0x0032FF, "Enclosed CJK Letters and Months"),
         UnicodeIconRange(0x003300..0x0033FF, "CJK Compatibility"),
         UnicodeIconRange(0x003400..0x004DBF, "CJK Unified Ideographs Extension A"),
         UnicodeIconRange(0x004DC0..0x004DFF, "Yijing Hexagram Symbols"),
         UnicodeIconRange(0x004E00..0x009FFF, "CJK Unified Ideographs"),
         UnicodeIconRange(0x00A000..0x00A48F, "Yi Syllables"),
         UnicodeIconRange(0x00A490..0x00A4CF, "Yi Radicals"),
         UnicodeIconRange(0x00AC00..0x00D7AF, "Hangul Syllables"),
         UnicodeIconRange(0x00D800..0x00DB7F, "High Surrogates"),
         UnicodeIconRange(0x00DB80..0x00DBFF, "High Private Use Surrogates"),
         UnicodeIconRange(0x00DC00..0x00DFFF, "Low Surrogates"),
         UnicodeIconRange(0x00E000..0x00F8FF, "Private Use Area"),
         UnicodeIconRange(0x00F900..0x00FAFF, "CJK Compatibility Ideographs"),
         UnicodeIconRange(0x00FB00..0x00FB4F, "Alphabetic Presentation Forms"),
         UnicodeIconRange(0x00FB50..0x00FDFF, "Arabic Presentation Forms-A"),
         UnicodeIconRange(0x00FE00..0x00FE0F, "Variation Selectors"),
         UnicodeIconRange(0x00FE20..0x00FE2F, "Combining Half Marks"),
         UnicodeIconRange(0x00FE30..0x00FE4F, "CJK Compatibility Forms"),
         UnicodeIconRange(0x00FE50..0x00FE6F, "Small Form Variants"),
         UnicodeIconRange(0x00FE70..0x00FEFF, "Arabic Presentation Forms-B"),
         UnicodeIconRange(0x00FF00..0x00FFEF, "Halfwidth and Fullwidth Forms"),
         UnicodeIconRange(0x00FFF0..0x00FFFF, "Specials"),
         UnicodeIconRange(0x010000..0x01007F, "Linear B Syllabary"),
         UnicodeIconRange(0x010080..0x0100FF, "Linear B Ideograms"),
         UnicodeIconRange(0x010100..0x01013F, "Aegean Numbers"),
         UnicodeIconRange(0x010300..0x01032F, "Old Italic"),
         UnicodeIconRange(0x010330..0x01034F, "Gothic"),
         UnicodeIconRange(0x010380..0x01039F, "Ugaritic"),
         UnicodeIconRange(0x010400..0x01044F, "Deseret"),
         UnicodeIconRange(0x010450..0x01047F, "Shavian"),
         UnicodeIconRange(0x010480..0x0104AF, "Osmanya"),
         UnicodeIconRange(0x010800..0x01083F, "Cypriot Syllabary"),
         UnicodeIconRange(0x01D000..0x01D0FF, "Byzantine Musical Symbols"),
         UnicodeIconRange(0x01D100..0x01D1FF, "Musical Symbols"),
         UnicodeIconRange(0x01D300..0x01D35F, "Tai Xuan Jing Symbols"),
         UnicodeIconRange(0x01D400..0x01D7FF, "Mathematical Alphanumeric Symbols"),
         UnicodeIconRange(0x020000..0x02A6DF, "CJK Unified Ideographs Extension B"),
         UnicodeIconRange(0x02F800..0x02FA1F, "CJK Compatibility Ideographs Supplement"),
         UnicodeIconRange(0x0E0000..0x0E007F, "Tags"),
      )

      /** @return unicode code points that are printable and assigned */
      fun values(): Sequence<UnicodeIcon> = sequence {
         unicodeRanges.forEach {
            yieldAll(it.range.map(::UnicodeIcon))
         }
      }

      /** @return all unicode code points including those not printable and unassigned */
      fun valuesWithNonPrintable(): Sequence<UnicodeIcon> = sequence {
         yieldAll((Char32.MIN.value..Char32.MAX.value).asSequence().map(::UnicodeIcon))
      }
   }
}

