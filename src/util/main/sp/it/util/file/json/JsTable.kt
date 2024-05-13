package sp.it.util.file.json

import kotlin.reflect.KClass
import sp.it.util.access.fieldvalue.ObjectField
import sp.it.util.access.fieldvalue.ObjectFieldBase
import sp.it.util.functional.asIs
import sp.it.util.type.VType
import sp.it.util.type.type
import sp.it.util.type.typeNothingNullable
import sp.it.util.type.union

/** Represents a table as json. useful to convert homogeneous [JsArray] of [JsObject] into a types table. */
class JsTable(val jsArray: JsArray, val columns: List<ObjectField<JsValue,*>>) {
    companion object {
        class ObjectFieldOfJsonValue<T: JsValue, R>(name: String, type: VType<R>, val extractor: (T) -> R): ObjectFieldBase<T, R>(
            type = type,
            extractor = extractor,
            name = name,
            description = "",
            toUi = { v, or -> v?.toString() ?: or }
        )

        fun of(js: JsValue): JsTable? {
            if (js !is JsArray) return null
            if (js.value.isEmpty()) return null

            val jsValues = js.value.filter { it !is JsNull }

            val type = jsValues.map { it::class as KClass<*> }.reduce { acc, kClass -> acc union kClass }
            if (type==JsNull::class) return JsTable(js, listOf(ObjectFieldOfJsonValue<JsValue, Nothing?>("value", typeNothingNullable()) { null }))
            if (type==JsTrue::class) return JsTable(js, listOf(ObjectFieldOfJsonValue<JsValue, Boolean?>("value", type<Boolean?>()) { it.asJsTrueValue() }))
            if (type==JsFalse::class) return JsTable(js, listOf(ObjectFieldOfJsonValue<JsValue, Boolean?>("value", type<Boolean?>()) { it.asJsFalseValue() }))
            if (type==JsBool::class) return JsTable(js, listOf(ObjectFieldOfJsonValue<JsValue, Boolean?>("value", type<Boolean?>()) { it.asJsBoolValue() }))
            if (type==JsString::class) return JsTable(js, listOf(ObjectFieldOfJsonValue<JsValue, String?>("value", type<String?>()) { it.asJsStringValue() }))
            if (type==JsNumber::class) return JsTable(js, listOf(ObjectFieldOfJsonValue<JsValue, Number?>("value", type<Number?>()) { it.asJsNumberValue() }))
            if (type==JsArray::class) return null
            if (type==JsObject::class) return run {
                val keys = jsValues.first().asIs<JsObject>().value.keys
                val keysSame = jsValues.asIs<List<JsObject>>().all { it.value.keys == keys }
                if (keysSame) {
                    val columns = keys.map { column -> ObjectFieldOfJsonValue<JsValue, Any?>(column, type<Any?>()) {
                        when (it) {
                            is JsNull -> null
                            is JsTrue -> true
                            is JsFalse -> false
                            is JsString -> it.value
                            is JsNumber -> it.value
                            is JsArray -> it
                            is JsObject -> it.value[column]?.let {
                                when (it) {
                                    is JsNull -> null
                                    is JsTrue -> true
                                    is JsFalse -> false
                                    is JsString -> it.value
                                    is JsNumber -> it.value
                                    is JsArray -> it
                                    is JsObject -> it
                                }
                            }
                        }
                    } }
                    JsTable(js, columns)
                } else
                    null
            }
            return null
        }
    }
}