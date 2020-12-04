package sp.it.pl.core

import de.jensd.fx.glyphs.GlyphIcons
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.effect.Effect
import javafx.scene.input.MouseButton
import javafx.scene.text.Font
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
import javafx.util.Duration
import sp.it.pl.layout.Component
import sp.it.pl.layout.widget.WidgetFactory
import sp.it.pl.layout.widget.feature.Feature
import sp.it.pl.main.APP
import sp.it.pl.main.AppTexts
import sp.it.pl.main.AppUi.SkinCss
import sp.it.pl.main.toS
import sp.it.pl.main.toUi
import sp.it.pl.plugin.PluginBase
import sp.it.pl.plugin.PluginBox
import sp.it.pl.ui.objects.icon.Glyphs
import sp.it.pl.ui.objects.icon.id
import sp.it.pl.ui.objects.table.TableColumnInfo
import sp.it.pl.ui.objects.tree.Name
import sp.it.util.Util.enumToHuman
import sp.it.util.access.fieldvalue.FileField
import sp.it.util.action.Action
import sp.it.util.file.json.JsValue
import sp.it.util.file.json.toCompactS
import sp.it.util.formatToSmallestUnit
import sp.it.util.functional.Functors
import sp.it.util.functional.PF
import sp.it.util.functional.Try
import sp.it.util.functional.Util
import sp.it.util.functional.asIs
import sp.it.util.functional.compose
import sp.it.util.functional.getOr
import sp.it.util.functional.invoke
import sp.it.util.functional.runTry
import sp.it.util.math.StrExF
import sp.it.util.parsing.ConverterDefault
import sp.it.util.parsing.ConverterFX
import sp.it.util.parsing.ConverterToString
import sp.it.util.parsing.Parsers
import sp.it.util.text.StringSplitParser
import sp.it.util.text.keysUi
import sp.it.util.text.nameUi
import sp.it.util.text.nullIfBlank
import sp.it.util.toLocalDateTime
import sp.it.util.type.Util.isEnum
import sp.it.util.type.VType
import sp.it.util.type.isPlatformType
import sp.it.util.type.raw
import sp.it.util.units.Bitrate
import sp.it.util.units.FileSize
import sp.it.util.units.NofX
import sp.it.util.units.uri
import java.io.File
import java.net.URI
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.attribute.FileTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Year
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.full.createType
import java.time.DateTimeException as DTE
import java.time.format.DateTimeParseException as DTPE
import java.util.regex.PatternSyntaxException as PSE

private typealias NFE = NumberFormatException
private typealias IAE = IllegalArgumentException
private typealias OBE = IndexOutOfBoundsException
private typealias FromS<T> = (String) -> Try<T, String>

class CoreConverter: Core {

   private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy MM dd HH:mm:ss")

   /** Default to/from string converter that uses per class registered converters. */
   @JvmField val general = Parsers.DEFAULT!!

   /** Default ui to string converter. */
   @JvmField val ui = object: ConverterToString<Any?> {
      override fun toS(o: Any?) = when (o) {
         null -> AppTexts.textNoVal
         is Boolean -> if (o) "yes" else "no"
         is Class<*> -> APP.className[o.kotlin]
         is KClass<*> -> APP.className[o]
         is KTypeProjection -> when (o) {
            KTypeProjection.STAR -> "*"
            else -> {
               val v = when (o.variance!!) {
                  KVariance.INVARIANT -> ""
                  KVariance.IN -> "in "
                  KVariance.OUT -> "out "
               }
               v + o.type.toUi()
            }
         }
         is KType -> {
            val suffix = when {
               o.isPlatformType -> "!"; o.isMarkedNullable -> "?"; else -> ""
            }
            o.classifier.toUi() + o.arguments.joinToString(", ") { it.toUi() }.nullIfBlank()?.let { "<$it>" }.orEmpty() + suffix
         }
         is VType<*> -> o.type.toUi()
         is NameUi -> o.nameUi
         is MouseButton -> o.nameUi
         is Action -> "${o.nameUi} (${o.keysUi()})"
         is LocalDateTime -> o.format(dateTimeFormatter)
         is LocalDate -> o.format(dateTimeFormatter)
         is LocalTime -> o.format(dateTimeFormatter)
         is Duration -> o.formatToSmallestUnit()
         is FileTime -> o.toInstant().toLocalDateTime().format(dateTimeFormatter)
         is Effect -> o::class.toUi()
         is Component -> o.name
         is PluginBase -> o.name
         is PluginBox<*> -> o.info.name
         is WidgetFactory<*> -> o.name
         is Node -> o.id?.trim().orEmpty() + ":" + o::class.toUi()
         is Name -> o.value
         is File -> o.path
         is URI -> URLDecoder.decode(o.toASCIIString(), UTF_8)
         is URL -> URLDecoder.decode(o.toExternalForm(), UTF_8)
         is Feature -> o.name
         is JsValue -> o.toCompactS()
         is Throwable -> o.localizedMessage
         else -> if (isEnum(o::class.java)) enumToHuman(o as Enum<*>) else general.toS(o)
      }
   }
   private val fx = ConverterFX()

   override fun init() {
      general.init()
      FileField.toSConverter = ui
   }

   @Suppress("RemoveExplicitTypeArguments")
   private fun ConverterDefault.init() = apply {

      val toS: (Any) -> String = defaultTos::invoke
      fun <T: Number> FromS<T>.numberMessage(): FromS<T> = this compose { it.mapError { "Not a number" + it.nullIfBlank()?.let { ": $it" } } }

      addP<Boolean>(toS, { it.toBoolean() })
      addT<Int>(toS, tryF(NFE::class) { it.toInt() }.numberMessage())
      addT<Double>(toS, tryF(NFE::class) { it.toDouble() }.numberMessage())
      addT<Short>(toS, tryF(NFE::class) { it.toShort() }.numberMessage())
      addT<Long>(toS, tryF(NFE::class) { it.toLong() }.numberMessage())
      addT<Float>(toS, tryF(NFE::class) { it.toFloat() }.numberMessage())
      addT<Char>(toS, tryF(OBE::class) { it[0] })
      addT<Byte>(toS, tryF(NFE::class) { it.toByte() })
      addP<String>({ it }, { it })
      addT<StringSplitParser>(toS, StringSplitParser::fromString)
      addT<Year>(toS, tryF(DTPE::class) { Year.parse(it) })
      addP<File>(toS, { File(it) })
      addT<URI>(toS, tryF(IAE::class) { uri(it) })
      addT<Pattern>(toS, tryF(PSE::class) { Pattern.compile(it) })
      addT<Bitrate>(toS, { Bitrate.fromString(it).orMessage() })
      addT<Duration>(toS, tryF(IAE::class) { Duration.valueOf(it.replace(" ", "")) }) // fixes java's inconsistency
      addT<LocalDateTime>(toS, tryF(DTE::class) { LocalDateTime.parse(it) })
      addT<FileSize>(toS, { FileSize.fromString(it).orMessage() })
      addT<StrExF>(toS, { StrExF.fromString(it).orMessage() })
      addT<NofX>(toS, { NofX.fromString(it).orMessage() })
      addT<TableColumnInfo>(toS, { TableColumnInfo.fromString(it).orMessage() })
      addT<TableColumnInfo.ColumnInfo>(toS, { TableColumnInfo.ColumnInfo.fromString(it).orMessage() })
      addT<TableColumnInfo.ColumnSortInfo>(toS, { TableColumnInfo.ColumnSortInfo.fromString(it).orMessage() })
      addT<Font>(
         { "${it.name}, ${it.size}" },
         tryF(NFE::class, OBE::class) {
            val i = it.indexOf(',')
            val name = it.substring(0, i)
            val style = if (it.toLowerCase().contains("italic")) FontPosture.ITALIC else FontPosture.REGULAR
            val weight = if (it.toLowerCase().contains("bold")) FontWeight.BOLD else FontWeight.NORMAL
            val size = it.substring(i + 2).toDouble()
            Font.font(name, weight, style, size)
         }
      )
      addT<GlyphIcons>({ it.id() }, { Glyphs[it].orMessage() })
      addT<Effect>({ fx.toS(it) }, { fx.ofS<Effect?>(it) })
      addT<Class<*>>({ it.name }, tryF(Throwable::class) { Class.forName(it) })
      addT<KClass<*>>({ it.javaObjectType.name }, tryF(Throwable::class) { Class.forName(it).kotlin })
      addP<PF<*, *>>(
         {
            val iN = if (it.`in`.isNullable) "?" else ""
            val oN = if (it.out.isNullable) "?" else ""
            "${it.name},${it.`in`.raw.toS()}$iN,${it.out.raw.toS()}$oN"
         },
         {
            val data = Util.split(it, ",")
            if (data.size!=3) {
               null
            } else {
               val name = data[0]
               val iN = data[1].endsWith("?")
               val oN = data[2].endsWith("?")
               val typeIn = ofS<KClass<*>>(data[1].let { if (!iN) it else it.dropLast(1) } ).getOr(null)?.createType(listOf(), iN)
               val typeOut = ofS<KClass<*>>(data[2].let { if (!oN) it else it.dropLast(1) } ).getOr(null)?.createType(listOf(), oN)
               if (name==null || typeIn==null || typeOut==null) null
               else Functors.pool.getPF(name, VType<Any?>(typeIn), VType<Any?>(typeOut)).asIs()
            }
         }
      )
      addT<Insets>(
         { "${it.top} ${it.right} ${it.bottom} ${it.left}" },
         tryF(Throwable::class) {
            it.split(" ").let { Insets(it[0].toDouble(), it[1].toDouble(), it[2].toDouble(), it[3].toDouble()) }
         }
      )
      addT<SkinCss>({ it.file.absolutePath }, { Try.ok(SkinCss(File(it))) })

   }

   private inline fun <reified T: Any> ConverterDefault.addP(noinline to: (T) -> String, noinline of: (String) -> T?) = addParserAsF(T::class, to, of)

   private inline fun <reified T: Any> ConverterDefault.addT(noinline to: (T) -> String, noinline of: FromS<T?>) = addParser(T::class, to, of)

   private fun <O> tryF(vararg ecs: KClass<*>, f: (String) -> O): FromS<O> = {
      runTry {
         f(it)
      }.mapError { e ->
         ecs.find { it.isInstance(e) }?.let { e.message ?: "Unknown error" } ?: throw e
      }
   }

   private fun <T> Try<T, Throwable>.orMessage() = mapError { it.message ?: "Unknown error" }

}

interface NameUi {
   /** Human readable name of this object displayed in user interface. */
   val nameUi: String
}
