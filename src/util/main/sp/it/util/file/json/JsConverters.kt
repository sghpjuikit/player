package sp.it.util.file.json

import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlin.reflect.full.createInstance
import kotlin.reflect.jvm.jvmName
import sp.it.util.conf.toConfigurableFx
import sp.it.util.dev.fail
import sp.it.util.type.VType
import sp.it.util.type.rawJ
import sp.it.util.type.type

object JsConverterZoneId: JsConverter<ZoneId> {
   override fun toJson(value: ZoneId) = JsString(value.toString())
   override fun fromJson(value: JsValue) = value.asJsStringValue()?.let { ZoneId.of(it) }
}

object JsConverterUuid: JsConverter<UUID> {
   override fun toJson(value: UUID) = JsString(value.toString())
   override fun fromJson(value: JsValue) = value.asJsStringValue()?.let { UUID.fromString(it) }
}

object JsConverterInstant: JsConverter<Instant> {
   override fun toJson(value: Instant) = JsString(value.toString())
   override fun fromJson(value: JsValue) = when(value) {
      is JsNull -> null
      is JsNumber -> Instant.ofEpochMilli(value.value.toLong())
      is JsString -> Instant.parse(value.value)
      else -> fail { "Unsupported ${Instant::class} value=$value" }
   }
}

/** Converter for javaFX bean convention */
object JsConverterAnyByConfigurableFx: JsConverter<Any> {

   /** @return converter for specified type utilizing this converter */
   inline fun <reified T: Any> toConverterOf(): JsConverter<T> = toConverterOf(type())

   /** @return converter for specified type utilizing this converter */
   fun <T: Any> toConverterOf(type: VType<T>): JsConverter<T> = object: JsConverter<T> {
      override fun fromJson(value: JsValue): T = type.rawJ.cast(JsConverterAnyByConfigurableFx.fromJson(value))
      override fun toJson(value: T) = JsConverterAnyByConfigurableFx.toJson(value)
   }

   override fun fromJson(value: JsValue): Any = when (value) {
      is JsObject -> {
         val fieldsType = value.value["_type"]!!.asJsStringValue()
         val valueType = Class.forName(fieldsType)
         valueType.kotlin.createInstance().also { v ->
            v.toConfigurableFx().getConfigs().forEach { c -> c.valueAsJson = value.value[c.name]!! }
         }
      }
      else -> fail { "Must be JsObject" }
   }

   override fun toJson(value: Any): JsValue {
      val valueType = value::class
      val fieldsType = mapOf("_type" to JsString(valueType.jvmName))
      val fieldsFx = value.toConfigurableFx().getConfigs().associate {
         it.name to it.valueAsJson
      }
      return JsObject(fieldsType + fieldsFx)
   }

}