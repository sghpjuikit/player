package sp.it.pl.core

import com.sun.net.httpserver.HttpExchange
import de.jensd.fx.glyphs.GlyphIcons
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import java.io.File
import java.io.FileNotFoundException
import java.net.URI
import java.nio.charset.Charset
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Year
import java.time.ZoneId
import java.util.Locale
import java.util.UUID
import java.util.regex.Pattern
import javafx.geometry.BoundingBox
import javafx.geometry.Bounds
import javafx.geometry.Insets
import javafx.geometry.Point2D
import javafx.geometry.Point3D
import javafx.scene.effect.Effect
import javafx.scene.text.Font
import javafx.util.Duration
import kotlin.reflect.KClass
import kotlin.text.Charsets.UTF_8
import mu.KLogging
import sp.it.pl.audio.playlist.PlaylistSong
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.MetadataGroup
import sp.it.pl.conf.Command
import sp.it.pl.layout.ContainerBiDb
import sp.it.pl.layout.ContainerFreeFormUi
import sp.it.pl.layout.ContainerFreeFormDb
import sp.it.pl.layout.NoComponentDb
import sp.it.pl.layout.ContainerRootDb
import sp.it.pl.layout.ContainerSwitchDb
import sp.it.pl.layout.ContainerUniDb
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetDb
import sp.it.pl.main.APP
import sp.it.pl.main.AppUi
import sp.it.pl.main.FileFilter
import sp.it.pl.ui.objects.table.TableColumnInfo
import sp.it.pl.ui.objects.window.stage.WindowDb
import sp.it.util.access.fieldvalue.ColumnField
import sp.it.util.access.fieldvalue.FileField
import sp.it.util.access.fieldvalue.IconField
import org.jetbrains.annotations.Blocking
import sp.it.pl.layout.ContainerSeqDb
import sp.it.pl.layout.WidgetNodeInstance
import sp.it.pl.main.AppHttp
import sp.it.pl.main.toS
import sp.it.pl.plugin.PluginBox
import sp.it.pl.plugin.PluginInfo
import sp.it.util.conf.Config
import sp.it.util.file.json.JsConverter
import sp.it.util.file.json.JsString
import sp.it.util.file.json.JsValue
import sp.it.util.file.json.toPrettyS
import sp.it.util.file.type.MimeExt
import sp.it.util.file.type.MimeGroup
import sp.it.util.file.type.MimeType
import sp.it.util.file.writeSafely
import sp.it.util.file.writeTextTry
import sp.it.util.functional.PF
import sp.it.util.functional.Try
import sp.it.util.functional.asIs
import sp.it.util.functional.invoke
import sp.it.util.math.StrExF
import sp.it.util.file.json.JsConverterAnyByConfigurableFx
import sp.it.util.file.json.toCompactS
import sp.it.util.text.StringSplitParser
import sp.it.util.units.Bitrate
import sp.it.util.units.FileSize
import sp.it.util.units.NofX

class CoreSerializerJson: Core {

   val encoding = UTF_8
   val json = Config.json

   override fun init() {
      json.typeAliases {
         // @formatter:off
            "no-container" alias NoComponentDb::class
          "root-container" alias ContainerRootDb::class
           "uni-container" alias ContainerUniDb::class
            "bi-container" alias ContainerBiDb::class
           "seq-container" alias ContainerSeqDb::class
          "free-container" alias ContainerFreeFormDb::class
   "free-container-window" alias ContainerFreeFormUi.WindowPosition::class
        "switch-container" alias ContainerSwitchDb::class
                  "widget" alias WidgetDb::class
                  "window" alias WindowDb::class
       "component-loading" alias Widget.LoadType::class
             "orientation" alias javafx.geometry.Orientation::class
         // @formatter:on
      }

      json.converters {
         WidgetNodeInstance::class convert WidgetNodeInstance

         @Suppress("RemoveRedundantQualifierName")
         val classes = setOf(
            StringSplitParser::class,
            Path::class,
            File::class,
            UUID::class,
            URI::class,
            Pattern::class,
            Bitrate::class,
            Duration::class,
            Locale::class,
            Charset::class,
            ZoneId::class,
            Instant::class,
            LocalTime::class,
            LocalDate::class,
            LocalDateTime::class,
            Year::class,
            FileSize::class,
            StrExF::class,
            NofX::class,
            PluginBox::class,
            PluginInfo::class,
            MimeGroup::class,
            MimeType::class,
            MimeExt::class,
            ColumnField::class,
            IconField::class,
            FileFilter::class,
            FileField::class,
            PlaylistSong.Field::class,
            sp.it.pl.audio.Song::class,
            sp.it.pl.audio.tagging.Metadata::class,
            Metadata.Field::class,
            MetadataGroup.Field::class,
            TableColumnInfo::class,
            TableColumnInfo.ColumnInfo::class,
            TableColumnInfo.ColumnSortInfo::class,
            Font::class,
            GlyphIcons::class,
            Class::class,
            KClass::class,
            PF::class,
            javafx.scene.paint.Paint::class,
            javafx.scene.paint.Color::class,
            javafx.scene.paint.RadialGradient::class,
            javafx.scene.paint.LinearGradient::class,
            javafx.scene.paint.ImagePattern::class,
            BoundingBox::class,
            Bounds::class,
            Point2D::class,
            Point3D::class,
            Insets::class,
            Command::class,
            Command.CommandActionId::class,
            Command.CommandComponentId::class,
            AppUi.SkinCss::class,
         )
         classes.forEach { c ->
            c.asIs<KClass<Any>>() convert object: JsConverter<Any> {
               val toS = APP.converter.general.parsersToS[c]!!
               val ofS = APP.converter.general.parsersFromS[c]!!
               override fun canConvert(value: Any) = c.isInstance(value)
               override fun toJson(value: Any): JsValue = JsString(toS(value).orThrow)
               override fun fromJson(value: JsValue): Any? = value.asJsStringValue()?.let { ofS(it).orThrow }
            }
         }

         Effect::class convert JsConverterAnyByConfigurableFx.toConverterOf()

      }
   }

   @Blocking
   inline fun <reified T: Any> toJson(t: T, file: File): Try<Nothing?, Throwable> {
      return file.writeSafely {
         it.writeTextTry(json.toJsonValue(t).toPrettyS())
      }.ifError {
         logger.error(it) { "Couldn't serialize " + t.javaClass + " to file=$file" }
      }
   }

   // TODO: error handling on call sites
   @Blocking
   inline fun <reified T> fromJson(file: File): Try<T, Throwable> =
      json.fromJson<T>(file).mapError {
         if (file.exists()) it
         else FileNotFoundException(file.absolutePath)
      }.ifError {
         logger.error(it) { "Couldn't deserialize ${T::class} from file $file" }
      }

   companion object: KLogging()
}

/** Converts json to value of the type specified by the reified type parameter */
@Throws
inline fun <reified T> JsValue.to() = Config.json.fromJsonValue<T>(this).orThrow

/** Converts response body to json */
@Throws
public suspend fun HttpResponse.bodyAsJs(): JsValue =
   Config.json.ast(bodyAsText(Charsets.UTF_8)).orThrow

/** Converts response body to json */
@Throws
public fun HttpExchange.requestBodyAsJs(): JsValue =
   Config.json.ast(requestBody).orThrow

/** Converts object to json (includes type witness) and sets as body */
public inline infix fun <reified T> HttpRequestBuilder.bodyJs(body: T) =
   apply { setBody(JsContent(body)) }

class JsContent(o: Any?): OutgoingContent.ByteArrayContent() {
   private val bytes = Config.json.toJsonValue(o).toCompactS().toByteArray()
   override val status = null
   override val contentType = ContentType.Application.Json
   override val contentLength = bytes.size.toLong()
   override fun bytes() = bytes
   override fun toString() = "JsContent(byte[...])"
}