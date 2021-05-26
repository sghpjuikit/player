package sp.it.pl.core

import java.time.format.DateTimeParseException as DTPE
import java.util.regex.PatternSyntaxException as PSE
import de.jensd.fx.glyphs.GlyphIcons
import java.io.File
import java.math.BigDecimal
import java.math.BigInteger
import java.net.URI
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Year
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ISO_DATE
import java.time.format.DateTimeFormatter.ISO_DATE_TIME
import java.time.format.DateTimeFormatter.ISO_INSTANT
import java.time.format.DateTimeFormatter.ISO_TIME
import java.util.function.BiFunction
import java.util.regex.Pattern
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.effect.Effect
import javafx.scene.input.MouseButton
import javafx.scene.text.Font
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
import javafx.util.Duration
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.relativeToOrSelf
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KTypeProjection.Companion.STAR
import kotlin.reflect.KVariance
import kotlin.reflect.full.createType
import kotlin.reflect.full.primaryConstructor
import sp.it.pl.audio.playlist.PlaylistSong
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.MetadataGroup
import sp.it.pl.conf.Command
import sp.it.pl.layout.Component
import sp.it.pl.layout.widget.WidgetFactory
import sp.it.pl.layout.widget.feature.Feature
import sp.it.pl.main.APP
import sp.it.pl.main.AppTexts
import sp.it.pl.main.AppUi.SkinCss
import sp.it.pl.main.FileFilter
import sp.it.pl.main.toS
import sp.it.pl.main.toUi
import sp.it.pl.plugin.PluginBase
import sp.it.pl.plugin.PluginBox
import sp.it.pl.ui.objects.icon.Glyphs
import sp.it.pl.ui.objects.icon.id
import sp.it.pl.ui.objects.table.TableColumnInfo
import sp.it.pl.ui.objects.tree.Name
import sp.it.util.Util.enumToHuman
import sp.it.util.access.fieldvalue.ColumnField
import sp.it.util.access.fieldvalue.FileField
import sp.it.util.access.fieldvalue.IconField
import sp.it.util.action.Action
import sp.it.util.conf.Constraint
import sp.it.util.dev.fail
import sp.it.util.file.div
import sp.it.util.file.isAnyParentOrSelfOf
import sp.it.util.file.json.JsValue
import sp.it.util.file.json.toCompactS
import sp.it.util.file.type.MimeExt
import sp.it.util.file.type.MimeGroup
import sp.it.util.file.type.MimeType
import sp.it.util.functional.Functors
import sp.it.util.functional.PF
import sp.it.util.functional.Try
import sp.it.util.functional.Util
import sp.it.util.functional.asIs
import sp.it.util.functional.compose
import sp.it.util.functional.getOr
import sp.it.util.functional.invoke
import sp.it.util.functional.net
import sp.it.util.functional.runTry
import sp.it.util.math.StrExF
import sp.it.util.parsing.ConverterDefault
import sp.it.util.parsing.ConverterFX
import sp.it.util.parsing.ConverterFromString
import sp.it.util.parsing.ConverterString
import sp.it.util.parsing.ConverterToString
import sp.it.util.parsing.Parsers
import sp.it.util.text.StringSplitParser
import sp.it.util.text.keysUi
import sp.it.util.text.nameUi
import sp.it.util.text.nullIfBlank
import sp.it.util.text.splitTrimmed
import sp.it.util.toLocalDateTime
import sp.it.util.type.VType
import sp.it.util.type.enumValues
import sp.it.util.type.isEnum
import sp.it.util.type.isObject
import sp.it.util.type.isPlatformType
import sp.it.util.type.raw
import sp.it.util.type.sealedSubObjects
import sp.it.util.type.type
import sp.it.util.units.Bitrate
import sp.it.util.units.FileSize
import sp.it.util.units.NofX
import sp.it.util.units.durationOfHMSMs
import sp.it.util.units.formatToSmallestUnit
import sp.it.util.units.uri

private typealias NFE = NumberFormatException
private typealias IAE = IllegalArgumentException
private typealias OBE = IndexOutOfBoundsException
private typealias FromS<T> = (String) -> Try<T, String>

object CoreConverter: Core {
   /** Formatter for [LocalTime] */
   val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")!!
   /** Formatter for [LocalDate] */
   val dateFormatter = DateTimeFormatter.ofPattern("yyyy MM dd")!!
   /** Formatter for [LocalDateTime] */
   val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy MM dd HH:mm:ss")!!

   /** Default to/from string converter that uses per class registered converters. */
   @JvmField val general = Parsers.DEFAULT!!

   /** Default ui to string converter. */
   @JvmField val ui = object: ConverterToString<Any?> {
      override fun toS(o: Any?) = when (o) {
         null -> AppTexts.textNoVal
         is Boolean -> if (o) "yes" else "no"
         is Byte -> NumberFormat.getIntegerInstance().format(o)
         is UByte -> NumberFormat.getIntegerInstance().format(o)
         is Short -> NumberFormat.getIntegerInstance().format(o)
         is UShort -> NumberFormat.getIntegerInstance().format(o)
         is Int -> NumberFormat.getIntegerInstance().format(o)
         is UInt -> NumberFormat.getIntegerInstance().format(o)
         is Long -> NumberFormat.getIntegerInstance().format(o)
         is ULong -> NumberFormat.getIntegerInstance().format(o)
         is Float -> NumberFormat.getInstance().format(o)
         is Double -> NumberFormat.getInstance().format(o)
         is BigInteger -> NumberFormat.getIntegerInstance().format(o)
         is BigDecimal -> NumberFormat.getInstance().format(o)
         is Class<*> -> APP.className[o.kotlin]
         is KClass<*> -> APP.className[o]
         is KTypeProjection -> when (o) {
            STAR -> "*"
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
         is Action -> o.nameUi + o.keysUi().nullIfBlank()?.net { " (${o.keysUi()})" }.orEmpty()
         is LocalDateTime -> o.format(dateTimeFormatter)
         is LocalDate -> o.format(dateFormatter)
         is LocalTime -> o.format(timeFormatter)
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
         else -> when {
            o::class.isEnum -> enumToHuman(o as Enum<*>)
            o::class.isObject -> enumToHuman(o::class.simpleName)
            else -> general.toS(o)
         }
      }
   }
   private val fx = ConverterFX()

   override fun init() {
      general.init()
      FileField.toSConverter = ui
   }

   @OptIn(ExperimentalPathApi::class)
   @Suppress("RemoveExplicitTypeArguments")
   private fun ConverterDefault.init() = apply {

      val anyConverter = ConverterDefault()
      parserFallbackToS = BiFunction { type, o ->
         when {
            type.isObject -> Try.ok(o::class.simpleName!!)
            else -> Try.ok(anyConverter.toS(o))
         }
      }
      parserFallbackFromS = BiFunction { type, s ->
         when {
            type.isEnum -> {
               val values = type.enumValues
               val value = null
                  ?: values.find { it.asIs<Enum<*>>().name==s }
                  ?: values.find { it.asIs<Enum<*>>().name.equals(s, ignoreCase = true) }
               value?.net { Try.ok(it) } ?: Try.error("Not a valid value: \"$s\"")
            }
            type.isObject -> Try.ok(type.objectInstance)
            type.isSealed -> type.sealedSubObjects
               .find { it::class.simpleName==s }
               ?.let { Try.ok(it) }
               ?: Try.error("Not a valid value: \"$s\"")
            type.isData -> runTry {
               type.primaryConstructor!!.call(
                  *type.primaryConstructor!!.parameters
                     .windowed(2, 1, true)
                     .map { ps ->
                        val argS = when (ps.size) {
                           2 -> s.substring(s.indexOf("${ps[0].name!!}=") + ps[0].name!!.length + 1, s.indexOf(", ${ps[1].name!!}="))
                           1 -> s.substring(s.indexOf("${ps[0].name!!}=") + ps[0].name!!.length + 1, s.length - 1)
                           else -> fail()
                        }
                        general.ofS(ps[0].type.raw, argS).orThrow
                     }
                     .toTypedArray()
               )
            }.orMessage()
            else -> anyConverter.ofS(type, s)
         }
      }

      val toS: (Any) -> String = defaultTos::invoke
      fun <T> FromS<T>.numberMessage(): FromS<T> = this compose { it.mapError { "Not a number" + it.nullIfBlank()?.let { ": $it" } } }

      addP<Boolean>(toS, { it.toBoolean() })
      addT<Byte>(toS, tryF(NFE::class) { it.toByte() })
      addT<UByte>(toS, tryF(NFE::class) { it.toUByte() })
      addT<Short>(toS, tryF(NFE::class) { it.toShort() }.numberMessage())
      addT<UShort>(toS, tryF(NFE::class) { it.toUShort() }.numberMessage())
      addT<Int>(toS, tryF(NFE::class) { it.toInt() }.numberMessage())
      addT<UInt>(toS, tryF(NFE::class) { it.toUInt() }.numberMessage())
      addT<Long>(toS, tryF(NFE::class) { it.toLong() }.numberMessage())
      addT<ULong>(toS, tryF(NFE::class) { it.toULong() }.numberMessage())
      addT<Float>(toS, tryF(NFE::class) { it.toFloat() }.numberMessage())
      addT<Double>(toS, tryF(NFE::class) { it.toDouble() }.numberMessage())
      addT<BigInteger>(toS, tryF(NFE::class) { it.toBigInteger() }.numberMessage())
      addT<BigDecimal>(toS, tryF(NFE::class) { it.toBigDecimal() }.numberMessage())
      addT<Char>(toS, tryF(OBE::class) { it[0] })
      addP<String>({ it }, { it })
      addT<StringSplitParser>(toS, StringSplitParser::fromString)
      addP<Path>(
         {
            if (APP.location.isAnyParentOrSelfOf(it.toFile())) "<app-dir>" + File.separator + it.relativeToOrSelf(APP.location.toPath())
            else it.toString()
         },
         {
            val appPrefix = "<app-dir>${File.separator}"
            if (it.startsWith(appPrefix)) APP.location.toPath()/it.substringAfter(appPrefix)
            else File(it).toPath()
         }
      )
      addP<File>(
         {
            if (APP.location.isAnyParentOrSelfOf(it)) "<app-dir>" + File.separator + it.relativeToOrSelf(APP.location)
            else it.absoluteFile.toString()
         },
         {
            val appPrefix = "<app-dir>${File.separator}"
            if (it.startsWith(appPrefix)) APP.location/it.substringAfter(appPrefix)
            else File(it)
         }
      )
      addT<URI>(toS, tryF(IAE::class) { uri(it) })
      addT<Pattern>(toS, tryF(PSE::class) { Pattern.compile(it) })
      addP<Bitrate>(Bitrate)
      addT<Duration>(toS, ::durationOfHMSMs)
      addT<Instant>({ ISO_INSTANT.format(it) }, tryF(DTPE::class) { Instant.parse(it) })
      addT<LocalTime>({ ISO_TIME.format(it) }, tryF(DTPE::class) { LocalTime.parse(it, ISO_TIME) })
      addT<LocalDate>({ ISO_DATE.format(it) }, tryF(DTPE::class) { LocalDate.parse(it, ISO_DATE) })
      addT<LocalDateTime>({ ISO_DATE_TIME.format(it) }, tryF(DTPE::class) { LocalDateTime.parse(it, ISO_DATE_TIME) })
      addT<Year>(toS, tryF(DTPE::class) { Year.parse(it) })
      addP<FileSize>(FileSize)
      addP<StrExF>(StrExF)
      addP<NofX>(NofX)
      addP<MimeGroup>(MimeGroup)
      addP<MimeType>(MimeType)
      addP<MimeExt>(MimeExt)
      addP<ColumnField>(ColumnField)
      addP<IconField<*>>(IconField)
      addP<FileFilter>(FileFilter)
      addP<FileField<*>>(FileField)
      addP<PlaylistSong.Field<*>>(PlaylistSong.Field)
      addP<Metadata.Field<*>>(Metadata.Field)
      addP<MetadataGroup.Field<*>>(MetadataGroup.Field)
      addT<TableColumnInfo>(toS, { TableColumnInfo.fromString(it).orMessage() })
      addT<TableColumnInfo.ColumnInfo>(toS, { TableColumnInfo.ColumnInfo.fromString(it).orMessage() })
      addT<TableColumnInfo.ColumnSortInfo>(toS, { TableColumnInfo.ColumnSortInfo.fromString(it).orMessage() })
      addT<Font>(
         { "${it.family}, ${it.style}, ${it.size}" },
         {
            runTry {
               val i = it.indexOf(',')
               val name = it.substring(0, i)
               val style = if (it.lowercase().contains("italic")) FontPosture.ITALIC else FontPosture.REGULAR
               val weight = if (it.lowercase().contains("bold")) FontWeight.BOLD else FontWeight.NORMAL
               val size = it.substringAfterLast(",").trim().toDoubleOrNull() ?: Font.getDefault().size
               val f = Font.font(name, weight, style, size)
               if (f.family==name) f else fail { "Not recognized font" }
            }.orMessage()
         }
      )
      addT<GlyphIcons>({ it.id() }, { Glyphs[it].orMessage() })
      addP<Effect>(fx.toConverterOf<Effect?>().asIs())
      addT<Class<*>>({ it.name }, tryF(Throwable::class) { Class.forName(it) })
      addT<KClass<*>>({ it.javaObjectType.name }, tryF(Throwable::class) {
         val defaultKClassToStringPrefix = "class"
         val sanitized = it.trim().removePrefix(defaultKClassToStringPrefix).trim()
         when (sanitized) {
            "kotlin.Any" -> Any::class
            "kotlin.Unit" -> Unit::class
            "kotlin.Nothing" -> Nothing::class
            else -> Class.forName(sanitized).kotlin
         }

      })
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
               val typeIn = ofS<KClass<*>>(data[1].let { if (!iN) it else it.dropLast(1) }).getOr(null)?.createType(listOf(), iN)
               val typeOut = ofS<KClass<*>>(data[2].let { if (!oN) it else it.dropLast(1) }).getOr(null)?.createType(listOf(), oN)
               if (name==null || typeIn==null || typeOut==null) null
               else Functors.pool.getPF(name, VType<Any?>(typeIn), VType<Any?>(typeOut)).asIs()
            }
         }
      )
      addT<Insets>(
         {
            when {
               it.top==it.right && it.top==it.bottom && it.top==it.left -> "${it.top}"
               else -> "${it.top} ${it.right} ${it.bottom} ${it.left}"
            }
         },
         {
            null
               ?: it.toDoubleOrNull()?.net { Try.ok(Insets(it)) }
               ?: it.splitTrimmed(" ").mapNotNull { it.toDoubleOrNull() }.takeIf { it.size==4 }?.net { Try.ok(Insets(it[0], it[1], it[2], it[3])) }
               ?: Try.error("'$it' is not a valid padding value")
         }
      )
      addP<Command>(Command)
      addP<Command.CommandActionId>(Command.CommandActionId)
      addP<Command.CommandComponentId>(Command.CommandComponentId)
      addT<SkinCss>({ it.file.absolutePath }, { Try.ok(SkinCss(File(it))) })

   }

   private inline fun <reified T: Any> ConverterDefault.addP(converter: ConverterString<T>) = addParser(T::class, converter)

   private inline fun <reified T: Any> ConverterDefault.addP(noinline to: (T) -> String, noinline of: (String) -> T?) = addParserAsF(T::class, to, of)

   private inline fun <reified T: Any> ConverterDefault.addT(noinline to: (T) -> String, noinline of: FromS<T?>) = addParser(T::class, to, of)

   private fun <O> tryF(vararg ecs: KClass<*>, f: (String) -> O): FromS<O> = {
      runTry {
         f(it)
      }.mapError { e ->
         ecs.find { it.isInstance(e) }?.let { e.message ?: "Unknown error" } ?: throw e
      }
   }

}

fun <T> Try<T, Throwable>.orMessage() = mapError { it.message ?: "Unknown error" }

/** Denotes how the object shows as human readable text in UI. */
interface NameUi {
   /** Human readable name of this object displayed in user interface. */
   val nameUi: String
}

class UiStringHelper<T>(val parse: ParserOr<T>): Constraint.MarkerConstraint()

/** parser into a value. Can be used as [Constraint] as well. */
interface Parse<T>: ConverterFromString<T> {

   override fun ofS(s: String): Try<T, String> = parse(s)

   fun parse(text: String): Try<T, String>

   fun toConstraint() = object: Constraint<String?> {
      override fun message() = "Not valid value"
      override fun isValid(value: String?) = validate(value).isOk
      override fun validate(value: String?) = if (value==null) Try.ok() else parse(value).map { null }
   }

   companion object {

      /** @return combined parser composing the specified parsers with [Boolean.or] */
      fun <T> or(vararg parsers: Parser<T>): ParserOr<T> = ParserOr(parsers.toList())

   }
}

data class ParserOr<T>(val parsers: List<Parser<T>>): Parse<T> {
   override fun parse(text: String): Try<T, String> {
      val errors = mutableListOf<Try<T, String>>()
      return parsers.map { it.parse(text) }.onEach(errors::add).find { it.isOk }
         ?: Try.error("Not valid value:" + errors.joinToString("") { "\n${it.errorOrThrow}" })
   }

   fun toUiStringHelper() = UiStringHelper(this)
}

/** Simple [Parse]. Exposes matching parts as [args], which can be used for UX hints or auto autocompletion. */
data class Parser<T>(val type: VType<T>, val args: List<ParserArg<*>>, val builder: (List<Any?>) -> T): Parse<T> {

   companion object {
      @JvmName("invoke2")
      inline operator fun <reified T> invoke(args: List<ParserArg<*>>, noinline builder: (List<Any?>) -> T) = Parser(type(), args, builder)
      @JvmName("invoke1")
      inline operator fun <reified T> invoke(vararg args: Any?, noinline builder: (List<Any?>) -> T) = Parser(
         type(),
         args.map {
            when {
               it is String -> ParserArg.Val(it)
               it is KClass<*> -> ParserArg.Arg(VType(it.createType(it.typeParameters.map { STAR })))
               it is ParserArg<*> -> it
               else -> fail { "" } }
         },
         builder
      )
   }

   override fun parse(text: String): Try<T, String> = parseWithErrorPositions(text).mapError { it.second }

   fun parseWithErrorPositions(text: String): Try<T, Pair<Int, String>> {
      var charAt = 0
      val delimiter = " "
      val output = mutableListOf<Any?>()
      var error: String? = null
      val errorAt = args.asSequence()
         .mapIndexed { i, arg ->
            val isFirst = i==0
            val isLast = i==args.size - 1

            if (error==null) {
               if (!isFirst) {
                  when {
                     text.startsWith(delimiter, charAt) -> charAt += delimiter.length
                     else -> error = "Does not contain '$delimiter' at $charAt"
                  }
               }
            }

            if (error==null) {
               when (arg) {
                  is ParserArg.Val -> when {
                     text.startsWith(arg.value, charAt) -> {
                        charAt += arg.value.length
                        output += arg.value
                     }
                     else -> error = "Does not contain '$arg' at $charAt'"
                  }
                  is ParserArg.Arg<*> -> {
                     val valueAsText = if (isLast) text.substring(charAt) else text.substring(charAt).substringBefore(delimiter)
                     CoreConverter.general.ofS(arg.type, valueAsText).ifOk { output += it }.ifError { error = it }
                     charAt += valueAsText.length
                  }
               }
            }

         }
         .takeWhile { error==null }
         .count()

      return error?.net { Try.error(errorAt to it) } ?: Try.ok(builder(output))
   }

}

sealed class ParserArg<T> {
   abstract val type: VType<T>

   data class Arg<T>(override val type: VType<T>): ParserArg<T>()
   data class Val(val value: String): ParserArg<String>() {
      override val type: VType<String> = VType(String::class.createType())
   }
}