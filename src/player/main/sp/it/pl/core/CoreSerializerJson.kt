package sp.it.pl.core

import mu.KLogging
import sp.it.pl.ui.objects.window.stage.WindowDb
import sp.it.pl.layout.BiContainerDb
import sp.it.pl.layout.FreeFormContainerDb
import sp.it.pl.layout.NoComponentDb
import sp.it.pl.layout.RootContainerDb
import sp.it.pl.layout.SwitchContainerDb
import sp.it.pl.layout.WidgetDb
import sp.it.pl.layout.container.UniContainer
import sp.it.pl.layout.widget.Widget
import sp.it.util.dev.Blocks
import sp.it.util.dev.fail
import sp.it.util.file.json.JsArray
import sp.it.util.file.json.JsConverter
import sp.it.util.file.json.JsString
import sp.it.util.file.json.JsValue
import sp.it.util.file.json.Json
import sp.it.util.file.json.toPrettyS
import sp.it.util.file.properties.PropVal
import sp.it.util.file.properties.PropVal.PropVal1
import sp.it.util.file.properties.PropVal.PropValN
import sp.it.util.file.writeSafely
import sp.it.util.file.writeTextTry
import sp.it.util.functional.Try
import sp.it.util.functional.Try.Java.error
import java.io.File
import java.io.FileNotFoundException
import kotlin.text.Charsets.UTF_8

class CoreSerializerJson: Core {

   val encoding = UTF_8
   val json = Json()

   override fun init() {
      json.typeAliases {
         // @formatter:off
            "no-container" alias NoComponentDb::class
          "root-container" alias RootContainerDb::class
           "uni-container" alias UniContainer::class
            "bi-container" alias BiContainerDb::class
          "free-container" alias FreeFormContainerDb::class
        "switch-container" alias SwitchContainerDb::class
                  "widget" alias WidgetDb::class
                  "window" alias WindowDb::class
       "component-loading" alias Widget.LoadType::class
             "orientation" alias javafx.geometry.Orientation::class
                "prop-val" alias PropVal::class
              "1-prop-val" alias PropVal1::class
              "n-prop-val" alias PropValN::class
         // @formatter:on
      }

      json.converters {
         PropVal::class convert object: JsConverter<PropVal> {
            override fun toJson(value: PropVal): JsValue = when (value) {
               is PropVal1 -> JsString(value.value)
               is PropValN -> JsArray(value.value.map { JsString(it) })
               else -> fail { "Unexpected value=$value, which is not ${PropVal::class}" }
            }

            override fun fromJson(value: JsValue) = when (value) {
               is JsString -> PropVal1(value.value)
               is JsArray -> value.value.mapNotNull { it.asJsStringValue() }.let { PropValN(it) }
               else -> fail { "Unexpected value=$value, which is not ${JsString::class} or ${JsArray::class}" }
            }
         }
      }
   }

   @Blocks
   inline fun <reified T: Any> toJson(t: T, file: File): Try<Nothing?, Throwable> {
      return file.writeSafely {
         it.writeTextTry(
            json.toJsonValue(t).toPrettyS(),
            encoding
         )
      }.ifError {
         logger.error(it) { "Couldn't serialize " + t.javaClass + " to file=$file" }
      }
   }

   // TODO: error handling on call sites
   @Blocks
   inline fun <reified T> fromJson(file: File): Try<T?, Throwable> {
      return if (!file.exists())
         error<T, Throwable>(Exception("Couldn't deserialize ${T::class} from file $file", FileNotFoundException(file.absolutePath)))
      else
         json.fromJson<T>(file, encoding).ifError {
            logger.error(it) { "Couldn't deserialize ${T::class} from file$file" }
         }
   }

   companion object: KLogging()
}