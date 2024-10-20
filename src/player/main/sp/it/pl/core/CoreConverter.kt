package sp.it.pl.core

import java.time.format.DateTimeParseException as DTPE
import java.util.regex.PatternSyntaxException as PSE
import kotlin.IllegalArgumentException as IAE
import kotlin.IndexOutOfBoundsException as OBE
import kotlin.NumberFormatException as NFE
import de.jensd.fx.glyphs.GlyphIcons
import java.io.File
import java.math.BigDecimal
import java.math.BigInteger
import java.net.URI
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.Year
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ISO_DATE
import java.time.format.DateTimeFormatter.ISO_DATE_TIME
import java.time.format.DateTimeFormatter.ISO_INSTANT
import java.time.format.DateTimeFormatter.ISO_TIME
import java.util.Locale
import java.util.Optional
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.function.BiFunction
import java.util.regex.Pattern
import javafx.geometry.BoundingBox
import javafx.geometry.Bounds
import javafx.geometry.Insets
import javafx.geometry.Point2D
import javafx.geometry.Point3D
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Skin
import javafx.scene.effect.Effect
import javafx.scene.image.Image
import javafx.scene.input.MouseButton
import javafx.scene.paint.Color
import javafx.scene.paint.ImagePattern
import javafx.scene.paint.LinearGradient
import javafx.scene.paint.Paint
import javafx.scene.paint.RadialGradient
import javafx.scene.text.Font
import javafx.util.Duration
import kotlin.io.path.relativeToOrSelf
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KTypeProjection.Companion.STAR
import kotlin.reflect.KVariance
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.createType
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmName
import sp.it.pl.audio.PlayerManager
import sp.it.pl.audio.SimpleSong
import sp.it.pl.audio.Song
import sp.it.pl.audio.playlist.PlaylistSong
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.MetadataGroup
import sp.it.pl.conf.Command
import sp.it.pl.layout.Component
import sp.it.pl.layout.WidgetFactory
import sp.it.pl.layout.feature.Feature
import sp.it.pl.main.APP
import sp.it.pl.main.AppTexts
import sp.it.pl.main.AppUi.SkinCss
import sp.it.pl.main.Events
import sp.it.pl.main.FileFilter
import sp.it.pl.main.toS
import sp.it.pl.main.toUi
import sp.it.pl.plugin.PluginBase
import sp.it.pl.plugin.PluginBox
import sp.it.pl.plugin.PluginInfo
import sp.it.pl.ui.objects.icon.Glyphs
import sp.it.pl.ui.objects.table.TableColumnInfo
import sp.it.pl.ui.objects.tree.Name
import sp.it.util.Util.enumToHuman
import sp.it.util.access.fieldvalue.ColumnField
import sp.it.util.access.fieldvalue.FileField
import sp.it.util.access.fieldvalue.IconField
import sp.it.util.action.Action
import sp.it.util.action.ActionDb
import sp.it.util.conf.Constraint
import sp.it.util.dev.fail
import sp.it.util.file.div
import sp.it.util.file.isAnyParentOrSelfOf
import sp.it.util.file.json.JsValue
import sp.it.util.file.json.toCompactS
import sp.it.util.file.json.toPrettyS
import sp.it.util.file.type.MimeExt
import sp.it.util.file.type.MimeGroup
import sp.it.util.file.type.MimeType
import sp.it.util.functional.Option
import sp.it.util.functional.PF
import sp.it.util.functional.Try
import sp.it.util.functional.Try.Error
import sp.it.util.functional.Try.Ok
import sp.it.util.functional.Util
import sp.it.util.functional.asIs
import sp.it.util.functional.compose
import sp.it.util.functional.getAny
import sp.it.util.functional.getOr
import sp.it.util.functional.invoke
import sp.it.util.functional.net
import sp.it.util.functional.runTry
import sp.it.util.functional.toOption
import sp.it.util.math.StrExF
import sp.it.util.math.toBigInt
import sp.it.util.parsing.ConverterDefault
import sp.it.util.parsing.ConverterFromString
import sp.it.util.parsing.ConverterSerializationBase64
import sp.it.util.parsing.ConverterString
import sp.it.util.parsing.ConverterToString
import sp.it.util.parsing.Parsers
import sp.it.util.text.Jwt
import sp.it.util.text.StringSplitParser
import sp.it.util.text.keysUi
import sp.it.util.text.nameUi
import sp.it.util.text.nullIfBlank
import sp.it.util.toLocalDateTime
import sp.it.util.type.VType
import sp.it.util.type.companionObj
import sp.it.util.type.createTypeStar
import sp.it.util.type.enumValues
import sp.it.util.type.isDataClass
import sp.it.util.type.isEnum
import sp.it.util.type.isObject
import sp.it.util.type.isPlatformType
import sp.it.util.type.objectInstanceSafe
import sp.it.util.type.raw
import sp.it.util.type.sealedSubObjects
import sp.it.util.type.type
import sp.it.util.ui.image.ImageSize
import sp.it.util.units.Bitrate
import sp.it.util.units.FileSize
import sp.it.util.units.NofX
import sp.it.util.units.durationOfHMSMs
import sp.it.util.units.formatToSmallestUnit
import sp.it.util.units.uri

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
   @JvmField val ui = ConverterToString<Any?> { o ->
      when (o) {
         null -> AppTexts.textNoVal
         is String -> o
         is Boolean -> if (o) "yes" else "no"
         is Byte -> NumberFormat.getIntegerInstance(APP.locale.value).format(o)
         is UByte -> NumberFormat.getIntegerInstance(APP.locale.value).format(o.toShort())
         is Short -> NumberFormat.getIntegerInstance(APP.locale.value).format(o)
         is UShort -> NumberFormat.getIntegerInstance(APP.locale.value).format(o.toInt())
         is Int -> NumberFormat.getIntegerInstance(APP.locale.value).format(o)
         is UInt -> NumberFormat.getIntegerInstance(APP.locale.value).format(o.toLong())
         is Long -> NumberFormat.getIntegerInstance(APP.locale.value).format(o)
         is ULong -> NumberFormat.getIntegerInstance(APP.locale.value).format(o.toBigInt())
         is Float -> NumberFormat.getInstance(APP.locale.value).format(o)
         is Double -> NumberFormat.getInstance(APP.locale.value).format(o)
         is BigInteger -> NumberFormat.getIntegerInstance(APP.locale.value).format(o)
         is BigDecimal -> NumberFormat.getInstance(APP.locale.value).format(o)
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
            val suffix = when { o.isPlatformType -> "!"; o.isMarkedNullable -> "?"; else -> "" }
            o.classifier.toUi() + o.arguments.joinToString(", ") { it.toUi() }.nullIfBlank()?.let { "<$it>" }.orEmpty() + suffix
         }
         is VType<*> -> o.type.toUi()
         is NameUi -> o.nameUi
         is MouseButton -> o.nameUi
         is Action -> o.nameUi + o.keysUi().nullIfBlank()?.net { " (${o.keysUi()})" }.orEmpty()
         is Locale -> o.getDisplayName(APP.locale.value)
         is Charset -> o.displayName(APP.locale.value)
         is OffsetDateTime -> o.format(dateTimeFormatter)
         is ZonedDateTime -> o.format(dateTimeFormatter)
         is LocalDateTime -> o.format(dateTimeFormatter)
         is LocalDate -> o.format(dateFormatter)
         is LocalTime -> o.format(timeFormatter)
         is Instant -> o.toLocalDateTime().toUi()
         is Duration -> o.formatToSmallestUnit()
         is FileTime -> o.toInstant().toLocalDateTime().format(dateTimeFormatter)
         is FileSize -> FileSize.toUiS(o, APP.locale.value)
         is GlyphIcons -> o.toS()
         is Effect -> o::class.toUi()
         is Component -> o.name
         is PluginBase -> o.name
         is PluginBox<*> -> o.info.name
         is WidgetFactory<*> -> o.name
         is Node -> o.id?.trim().orEmpty() + ":" + o::class.toUi()
         is Image -> "Image(${o.width} x ${o.height})"
         is Name -> o.value
         is File -> o.path
         is URI -> URLDecoder.decode(o.toASCIIString(), UTF_8)
         is URL -> URLDecoder.decode(o.toExternalForm(), UTF_8)
         is Song -> o.uri.toString()
         is Events.ActionEvent -> o.toString()
         is PlayerManager.Events.PlaybackSongChanged -> o.toString()
         is PlayerManager.Events.PlaybackSongUpdated -> o.toString()
         is Feature -> o.name
         is JsValue -> o.toCompactS()
         is Jwt -> Jwt.toUiS(o, APP.locale.value)
         is Throwable -> o.localizedMessage
         is Optional<*> -> o.toOption().toUi()
         is Option.Some<*> -> "Some(${o.value.toUi()})"
         is Option.None -> "None"
         is Ok<*> -> "Ok(${o.value.toUi()})"
         is Error<*> -> "Error(${o.value.toUi()})"
         else -> when {
            o::class.isEnum -> enumToHuman(o as Enum<*>)
            o::class.isObject -> o::class.simpleName.orEmpty().replace('_', ' ')
            o::class.isDataClass -> runTry {
               (o::class.simpleName ?: o::class.jvmName) + " " + APP.serializerJson.json.toJsonValue(VType<Any?>(o::class.createTypeStar()), o).toPrettyS() }.orMessage().getAny()
            // TODO: good idea but probably reduces performance, put the converters in MapByKClass first
            // o::class.companionObjectInstance is ConverterToUiString<*> -> o::class.companionObjectInstance.asIs<ConverterToUiString<Any>>().toUiS(o, APP.locale.value)
            else -> general.toS(o)
         }
      }
   }

   override fun init() {
      general.init()
      FileField.toSConverter = ui
   }

   private fun ConverterDefault.init() = apply {

      fun <A, B, R> memoized(f: ((B) -> (A) -> R)): BiFunction<B, A, R> {
         val cache = ConcurrentHashMap<B, (A) -> R>()
         return BiFunction { b, a -> cache.getOrPut(b) { f(b) }(a) }
      }

      val anyConverter = ConverterDefault()
      parserFallbackToS = memoized { type ->
            when {
               type.companionObj?.takeIf { it is ConverterToString<*> }!=null ->
                  { o -> Try.ok(type.companionObject.asIs<ConverterToString<Any?>>().toS(o)) }
               type.isObject ->
                  { o -> Try.ok(o::class.simpleName!!) }
               type.isDataClass ->
                  { o -> runTry { APP.serializerJson.json.toJsonValue(VType<Any?>(type.createTypeStar()), o).toCompactS() }.orMessage() }
               else ->
                  { o -> Try.ok(anyConverter.toS(o)) }
            }
         }
      parserFallbackFromS = memoized { type ->
            when {
               type.companionObj?.takeIf { it is ConverterFromString<*> }!=null ->
                  { s -> type.companionObject.asIs<ConverterFromString<*>>().ofS(s) }
               type.isEnum ->
                  { s ->
                     val values = type.enumValues
                     val value = null
                        ?: values.find { it.asIs<Enum<*>>().name==s }
                        ?: values.find { it.asIs<Enum<*>>().name.equals(s, ignoreCase = true) }
                     value?.net { Try.ok(it) } ?: Try.error("Not a valid value: \"$s\"")
                  }
               type.isObject ->
                  { _ -> Try.ok(type.objectInstanceSafe) }
               type.isSealed -> {
                  val sso = type.sealedSubObjects
                  { s -> sso.find { it::class.simpleName==s }?.let { Try.ok(it) } ?: Try.error("Not a valid value: \"$s\"") }
               }
               type.isDataClass ->
                  { s ->
                     val isJsonObject = s.startsWith("{") && s.endsWith("}")
                     if (isJsonObject) APP.serializerJson.json.fromJson(VType<Any?>(type.createTypeStar()), s).orMessage()
                     else runTry {
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
                  }
               else ->
                  { s -> anyConverter.ofS(type, s) }
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
      addT<UUID>(toS, tryF(IAE::class) { UUID.fromString(it) })
      addT<URI>(toS, tryF(IAE::class) { uri(it) })
      addT<Pattern>(toS, tryF(PSE::class) { Pattern.compile(it) })
      addP<Bitrate>(Bitrate)
      addT<Duration>(toS, ::durationOfHMSMs)
      addT<Locale>({ it.toLanguageTag() }, tryF(Throwable::class) { Locale.Builder().setLanguageTag(it).build() })
      addT<Charset>({ it.name() }, tryF(Throwable::class) { Charset.forName(it) })
      addT<ZoneId>(toS, tryF { ZoneId.of(it) })
      addT<Instant>({ ISO_INSTANT.format(it) }, tryF(DTPE::class) { Instant.parse(it) })
      addT<LocalTime>({ ISO_TIME.format(it) }, tryF(DTPE::class) { LocalTime.parse(it, ISO_TIME) })
      addT<LocalDate>({ ISO_DATE.format(it) }, tryF(DTPE::class) { LocalDate.parse(it, ISO_DATE) })
      addT<LocalDateTime>({ ISO_DATE_TIME.format(it) }, tryF(DTPE::class) { LocalDateTime.parse(it, ISO_DATE_TIME) })
      addT<ZonedDateTime>({ ISO_DATE_TIME.format(it) }, tryF(DTPE::class) { ZonedDateTime.parse(it, ISO_DATE_TIME) })
      addT<OffsetDateTime>({ ISO_DATE_TIME.format(it) }, tryF(DTPE::class) { OffsetDateTime.parse(it, ISO_DATE_TIME) })
      addT<Year>(toS, tryF(DTPE::class) { Year.parse(it) })
      addP<ActionDb>(ActionDb)
      addP<FileSize>(FileSize)
      addP<StrExF>(StrExF)
      addP<NofX>(NofX)
      addP<ImageSize>(ImageSize)
      addT<PluginInfo>({ it.name }, tryF(AssertionError::class) { throw AssertionError("") })
      addT<PluginBox<*>>({ it.info.name }, tryF(AssertionError::class) { throw AssertionError("") })
      addP<MimeGroup>(MimeGroup)
      addP<MimeType>(MimeType)
      addP<MimeExt>(MimeExt)
      addP<ColumnField>(ColumnField)
      addP<IconField<*>>(IconField)
      addP<FileFilter>(FileFilter)
      addP<FileField<*>>(FileField)
      addT<Song>({ it.uri.toString() }, tryF(IAE::class) { SimpleSong(uri(it)) })
      addT<Metadata>({ ConverterSerializationBase64.toS(it).getOr("") }, { ConverterSerializationBase64.ofS<Metadata>(it).orMessage() })
      addP<PlaylistSong.Field<*>>(PlaylistSong.Field)
      addP<Metadata.Field<*>>(Metadata.Field)
      addP<MetadataGroup.Field<*>>(MetadataGroup.Field)
      addT<TableColumnInfo>(toS, { TableColumnInfo.fromString(it).orMessage() })
      addT<TableColumnInfo.ColumnInfo>(toS, { TableColumnInfo.ColumnInfo.fromString(it).orMessage() })
      addT<TableColumnInfo.ColumnSortInfo>(toS, { TableColumnInfo.ColumnSortInfo.fromString(it).orMessage() })
      addP<Color>(ConverterColor)
      addP<RadialGradient>(ConverterRadialGradient)
      addP<LinearGradient>(ConverterLinearGradient)
      addP<ImagePattern>(ConverterImagePattern)
      addP<Paint>(ConverterPaint)
      addP<Font>(ConverterFont)
      addParserToS(Node::class) { it::class.jvmName }
      addParserToS(Image::class) { "${it::class.jvmName}(${it.url})" }
      addParserToS(Skin::class) { it::class.jvmName }
      addParserToS(Scene::class) { it::class.jvmName }
      addP<GlyphIcons>(Glyphs)
      addT<Class<*>>({ it.name }, tryF(Throwable::class) { Class.forName(it, false, null) })
      addT<KClass<*>>({ it.javaObjectType.name }, tryF(Throwable::class) {
         val defaultKClassToStringPrefix = "class"
         val nameSanitized = it.trim().removePrefix(defaultKClassToStringPrefix).trim()
         when (nameSanitized) {
            "kotlin.Any" -> Any::class
            "kotlin.Unit" -> Unit::class
            "kotlin.Nothing" -> Nothing::class
            else -> Class.forName(nameSanitized, false, null).kotlin
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
               else CoreFunctors.pool.getPF(name, VType<Any?>(typeIn), VType<Any?>(typeOut)).asIs()
            }
         }
      )
      addP<BoundingBox>(ConverterBoundingBox)
      addP<Bounds>(ConverterBounds)
      addP<Point2D>(ConverterPoint2D)
      addP<Point3D>(ConverterPoint3D)
      addP<Insets>(ConverterInsets)
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

/** Denotes how the object shows as human-readable text in UI. */
interface NameUi {
   /** Human-readable name of this object displayed in user interface. */
   val nameUi: String
}

/** Denotes how the object shows as human-readable description in UI. */
interface InfoUi {
   /** Human-readable description of this object displayed in user interface. */
   val infoUi: String
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
               it is VType<*> -> ParserArg.Arg(it)
               it is KClass<*> -> ParserArg.Arg(VType(it.createType(it.typeParameters.map { STAR }, false)))
               it is ParserArg<*> -> it
               else -> fail { "" } }
         },
         builder
      )
   }

   override fun parse(text: String): Try<T, String> = parseWithErrorPositions(text).toTry()

   fun parseWithErrorPositions(text: String): ParsePart<T> {
      var charAt = 0
      val delimiter = " "
      val valueParts = mutableListOf<Any?>()
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
                        valueParts += arg.value
                     }
                     else -> error = "Does not contain '${arg.value}' at $charAt'"
                  }
                  is ParserArg.Arg<*> -> {
                     val valueAsText = if (isLast) text.substring(charAt) else text.substring(charAt).substringBefore(delimiter)
                     CoreConverter.general.ofS(arg.type, valueAsText).ifOk { valueParts += it }.ifError { error = it }
                     charAt += valueAsText.length
                  }
               }
            }

         }
         .takeWhile { error==null }
         .count()

      return error?.net { ParsePartError(errorAt, it) } ?: ParsePartOk(errorAt, builder(valueParts), valueParts)
   }

   sealed interface ParsePart<T> {
      fun toTry(): Try<T, String> = when (this) {
         is ParsePartOk -> Ok(value)
         is ParsePartError -> Error(message)
      }
   }
   data class ParsePartOk<T>(val at: Int, val value: T, val valueParts: List<Any?>): ParsePart<T>
   data class ParsePartError<T>(val at: Int, val message: String): ParsePart<T>
}

sealed interface ParserArg<T> {
   val type: VType<T>

   data class Arg<T>(override val type: VType<T>): ParserArg<T>
   data class Val(val value: String): ParserArg<String> {
      override val type: VType<String> = type()
   }
}