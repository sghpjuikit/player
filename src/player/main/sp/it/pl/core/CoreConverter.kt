package sp.it.pl.core

import de.jensd.fx.glyphs.GlyphIcons
import javafx.scene.effect.Effect
import javafx.scene.text.Font
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
import javafx.util.Duration
import sp.it.pl.gui.UiManager.SkinCss
import sp.it.pl.gui.objects.icon.Icon
import sp.it.util.functional.Functors
import sp.it.util.functional.Try
import sp.it.util.functional.Util
import sp.it.util.functional.getOr
import sp.it.util.functional.invoke
import sp.it.util.functional.runTry
import sp.it.util.math.StrExF
import sp.it.util.parsing.ConverterDefault
import sp.it.util.parsing.ConverterFX
import sp.it.util.parsing.Parsers
import sp.it.util.text.StringSplitParser
import sp.it.util.units.Bitrate
import sp.it.util.units.FileSize
import sp.it.util.units.NofX
import java.io.File
import java.net.URI
import java.time.LocalDateTime
import java.time.Year
import java.util.regex.Pattern
import kotlin.reflect.KClass
import java.time.DateTimeException as DTE
import java.time.format.DateTimeParseException as DTPE
import java.util.regex.PatternSyntaxException as PSE

private typealias NFE = NumberFormatException
private typealias IAE = IllegalArgumentException
private typealias OBE = IndexOutOfBoundsException

class CoreConverter: Core {

   /** Default to/from string converter that uses per class registered converters. */
   @JvmField val general = Parsers.DEFAULT!!
   private val fx = ConverterFX()

   override fun init() {
      general.init()
   }

   @Suppress("RemoveExplicitTypeArguments")
   private fun ConverterDefault.init() = apply {

      val toS: (Any) -> String = defaultTos::invoke

      addP<Boolean>(toS, { it.toBoolean() })
      addT<Int>(toS, tryF(NFE::class) { it.toInt() })
      addT<Double>(toS, tryF(NFE::class) { it.toDouble() })
      addT<Short>(toS, tryF(NFE::class) { it.toShort() })
      addT<Long>(toS, tryF(NFE::class) { it.toLong() })
      addT<Float>(toS, tryF(NFE::class) { it.toFloat() })
      addT<Char>(toS, tryF(OBE::class) { it[0] })
      addT<Byte>(toS, tryF(NFE::class) { it.toByte() })
      addP<String>({ it }, { it })
      addT<StringSplitParser>(toS, StringSplitParser::fromString)
      addT<Year>(toS, tryF(DTPE::class) { Year.parse(it) })
      addP<File>(toS, { File(it) })
      addT<URI>(toS, tryF(IAE::class) { URI.create(it) })
      addT<Pattern>(toS, tryF(PSE::class) { Pattern.compile(it) })
      addT<Bitrate>(toS, { Bitrate.fromString(it).orMessage() })
      addT<Duration>(toS, tryF(IAE::class) { Duration.valueOf(it.replace(" ", "")) }) // fixes java's inconsistency
      addT<LocalDateTime>(toS, tryF(DTE::class) { LocalDateTime.parse(it) })
      addT<FileSize>(toS, { FileSize.fromString(it).orMessage() })
      addT<StrExF>(toS, { StrExF.fromString(it).orMessage() })
      addT<NofX>(toS, { NofX.fromString(it).orMessage() })
      addT<Font>(
         { String.format("%s, %s", it.name, it.size) },
         tryF(NFE::class, OBE::class) {
            val i = it.indexOf(',')
            val name = it.substring(0, i)
            val style = if (it.toLowerCase().contains("italic")) FontPosture.ITALIC else FontPosture.REGULAR
            val weight = if (it.toLowerCase().contains("bold")) FontWeight.BOLD else FontWeight.NORMAL
            val size = it.substring(i + 2).toDouble()
            Font.font(name, weight, style, size)
         }
      )
      addT<GlyphIcons>(
         { "${it.fontFamily}.${it.name()}" },
         { Icon.GLYPHS[it]?.let { Try.ok(it) } ?: Try.error("No such icon=$it") }
      )
      addT<Effect>({ fx.toS(it) }, { fx.ofS<Effect>(it) })
      addT<Class<*>>({ it.name }, tryF(Throwable::class) { Class.forName(it) })
      addP<Functors.PƑ<*, *>>(
         { "${it.name},${it.`in`},${it.out}" },
         {
            val data = Util.split(it, ",")
            if (data.size!=3) {
               null
            } else {
               val name = data[0]
               val typeIn = ofS<Class<*>>(data[1]).getOr(null)
               val typeOut = ofS<Class<*>>(data[2]).getOr(null)
               if (name==null || typeIn==null || typeOut==null) null
               else Functors.pool.getPF(name, typeIn, typeOut)
            }
         }
      )
      addT<SkinCss>({ it.file.absolutePath }, { Try.ok(SkinCss(File(it))) })

   }

   private inline fun <reified T: Any> ConverterDefault.addP(noinline to: (T) -> String, noinline of: (String) -> T?) =
      addParserAsF(T::class.java, to, of)

   private inline fun <reified T: Any> ConverterDefault.addT(noinline to: (T) -> String, noinline of: (String) -> Try<T?, String>) =
      addParser(T::class.java, to, of)

   private fun <I, O> tryF(vararg ecs: KClass<*>, f: (I) -> O): (I) -> Try<O, String> = {
      runTry {
         f(it)
      }.mapError { e ->
         ecs.find { it.isInstance(e) }?.let { e.message ?: "Unknown error" } ?: throw e
      }
   }

   private fun <T> Try<T, Throwable>.orMessage() = mapError { it.message ?: "Unknown error" }

}