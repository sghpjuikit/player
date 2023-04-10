package sp.it.pl.core

import de.jensd.fx.glyphs.GlyphIcons
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
import sp.it.pl.layout.BiContainerDb
import sp.it.pl.layout.ContainerFreeFormUi
import sp.it.pl.layout.FreeFormContainerDb
import sp.it.pl.layout.NoComponentDb
import sp.it.pl.layout.RootContainerDb
import sp.it.pl.layout.SwitchContainerDb
import sp.it.pl.layout.UniContainerDb
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
import sp.it.pl.layout.WidgetNodeInstance
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
import sp.it.util.functional.Try.Java.error
import sp.it.util.functional.asIs
import sp.it.util.functional.invoke
import sp.it.util.math.StrExF
import sp.it.util.file.json.JsConverterAnyByConfigurableFx
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
          "root-container" alias RootContainerDb::class
           "uni-container" alias UniContainerDb::class
            "bi-container" alias BiContainerDb::class
          "free-container" alias FreeFormContainerDb::class
   "free-container-window" alias ContainerFreeFormUi.WindowPosition::class
        "switch-container" alias SwitchContainerDb::class
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
   inline fun <reified T> fromJson(file: File): Try<T, Throwable> {
      return if (!file.exists())
         error<T, Throwable>(Exception("Couldn't deserialize ${T::class} from file $file", FileNotFoundException(file.absolutePath)))
      else
         json.fromJson<T>(file).ifError {
            logger.error(it) { "Couldn't deserialize ${T::class} from file$file" }
         }
   }

   companion object: KLogging()
}