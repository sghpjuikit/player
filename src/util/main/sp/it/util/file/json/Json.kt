package sp.it.util.file.json

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.JsonValue
import com.beust.klaxon.Klaxon
import sp.it.util.collections.map.ClassListMap
import sp.it.util.dev.fail
import sp.it.util.functional.Try
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import sp.it.util.functional.getOrSupply
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.net
import sp.it.util.functional.runTry
import sp.it.util.parsing.ConverterDefault
import sp.it.util.parsing.Parsers
import sp.it.util.type.isSubclassOf
import sp.it.util.type.toRaw
import sp.it.util.type.typeLiteral
import java.io.File
import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.nio.charset.Charset
import java.util.BitSet
import java.util.Collections
import java.util.Hashtable
import java.util.LinkedList
import java.util.SortedSet
import java.util.TreeMap
import java.util.TreeSet
import java.util.UUID
import java.util.Vector
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaType

interface JsRoot

sealed class JsValue {
   fun asJsNull() = asIs<JsNull>().let { null }
   fun asJsTrue() = if (this is JsNull) null else asIs<JsTrue>().let { true }
   fun asJsFalse() = if (this is JsNull) null else asIs<JsFalse>().let { false }
   fun asJsString() = if (this is JsNull) null else asIs<JsString>().let { it.value }
   fun asJsNumber() = if (this is JsNull) null else asIs<JsNumber>().let { it.value }
   fun asJsArray() = if (this is JsNull) null else asIs<JsArray>().let { it.value }
   fun asJsObject() = if (this is JsNull) null else asIs<JsObject>().let { it.value }

   override fun toString() = "${this::class} ${prettyPrint()}"
}

object JsNull: JsValue()

object JsTrue: JsValue()

object JsFalse: JsValue()

class JsString(val value: String): JsValue()

class JsNumber(val value: Number): JsValue()

class JsArray(val value: List<JsValue>): JsValue(), JsRoot

class JsObject(val value: Map<String, JsValue>): JsValue(), JsRoot


interface JsConverter<T> {
   fun canConvert(value: T): Boolean = true
   fun toJson(value: T): JsValue
   fun fromJson(value: JsValue): T?
}

class Json {

   val converters = TypeConverters()
   val typeAliases = TypeAliases()
   val keyMapConverter: ConverterDefault = Parsers.DEFAULT

   init {
      typeAliases {
         // @formatter:off
                  "int" alias Int::class
                "float" alias Float::class
               "double" alias Double::class
                 "long" alias Long::class
                "short" alias Short::class
                 "char" alias Char::class
                 "byte" alias Byte::class
              "boolean" alias Boolean::class
               "number" alias Number::class
               "object" alias Any::class
              "big-int" alias java.math.BigInteger::class
          "big-decimal" alias java.math.BigDecimal::class
               "string" alias String::class
           "java-class" alias Class::class
         "kotlin-class" alias KClass::class
              "bit-set" alias BitSet::class
                  "map" alias Map::class
                "entry" alias Map.Entry::class
                 "list" alias List::class
                  "set" alias Set::class
           "sorted-set" alias SortedSet::class
          "linked-list" alias LinkedList::class
               "vector" alias Vector::class
             "tree-map" alias TreeMap::class
             "tree-set" alias TreeSet::class
            "hashtable" alias Hashtable::class
           "empty-list" alias Collections.EMPTY_LIST::class
            "empty-map" alias Collections.EMPTY_MAP::class
            "empty-set" alias Collections.EMPTY_SET::class
       "singleton-list" alias Collections.singletonList(null)::class
        "singleton-map" alias Collections.singletonMap(null, null)::class
        "singleton-set" alias Collections.singleton(null)::class
  "concurrent-hash-map" alias ConcurrentHashMap::class
                 "file" alias java.io.File::class
                 "path" alias java.nio.file.Path::class
       "string-builder" alias java.lang.StringBuilder::class
        "string-buffer" alias java.lang.StringBuffer::class
   "gregorian-calendar" alias java.util.Calendar::class
                 "date" alias java.util.Date::class
             "enum-set" alias java.util.EnumSet::class
             "enum-map" alias java.util.EnumMap::class
               "locale" alias java.util.Locale::class
           "properties" alias java.util.Properties::class
                 "uuid" alias java.util.UUID::class
                  "uri" alias java.net.URI::class
                  "url" alias java.net.URL::class
          "day-of-week" alias java.time.DayOfWeek::class
             "duration" alias java.time.Duration::class
              "instant" alias java.time.Instant::class
           "local-date" alias java.time.LocalDate::class
      "local-date-time" alias java.time.LocalDateTime::class
           "local-time" alias java.time.LocalTime::class
                "month" alias java.time.Month::class
            "month-day" alias java.time.MonthDay::class
               "period" alias java.time.Period::class
                 "year" alias java.time.Year::class
           "year-month" alias java.time.YearMonth::class
         // @formatter:on
      }

      converters {
         UUID::class convert object: JsConverter<UUID> {
            override fun toJson(value: UUID) = JsString(value.toString())
            override fun fromJson(value: JsValue) = value.asJsString()?.let { UUID.fromString(it) }
         }
      }
   }

   inline fun <reified T: Any> toJsonValue(value: T?): JsValue = toJsonValue(typeLiteral<T>(), value)

   fun toJsonValue(typeAsJava: Type, value: Any?): JsValue {
      return when (value) {
         null -> JsNull
         true -> JsTrue
         false -> JsFalse
         else -> {
            val type = value::class
            val typeAs = typeAsJava.toRaw().kotlin
            val isObject = type.objectInstance!=null
            val isAmbiguous = typeAs==Any::class || typeAs.isSealed || isObject || type!=typeAs

            fun typeWitness() = "_type" to JsString(typeAliases.byType[type] ?: type.qualifiedName
            ?: fail { "Unable to serialize instance of type=$type: Type has no fully qualified name" })

            fun JsValue.withAmbiguity() = if (isAmbiguous) JsObject(mapOf("value" to this, typeWitness())) else this

            when (value) {
               is Number -> JsNumber(value).withAmbiguity()
               is String -> JsString(value).withAmbiguity()
               is Enum<*> -> JsString(value.name).withAmbiguity()
               is Collection<*> -> JsArray(value.map { toJsonValue(typeLiteral<Any>(), it) })   // TODO: preserve collection/map type
               is Map<*, *> -> JsObject(value.mapKeys { keyMapConverter.toS(it.key) }.mapValues { it.value.let { toJsonValue(typeLiteral<Any>(), it) } })
               else -> {
                  val converter = converters.byType.getElementOfSuper(value::class.javaObjectType)
                     .asIf<JsConverter<Any>>()
                     ?.takeIf { it.canConvert(value) }
                  if (converter!=null) {
                     converter.toJson(value)
                  } else {
                     val values = type.memberProperties.filter { it.javaField!=null }.map {
                        it.isAccessible = true
                        it.name to toJsonValue(it.returnType.javaType, it.getter.call(value))
                     }.toMap().let {
                        when {
                           isObject -> mapOf(typeWitness())
                           isAmbiguous -> it + typeWitness()
                           else -> it
                        }
                     }
                     JsObject(values)
                  }
               }
            }
         }
      }
   }

   inline fun <reified T> fromJson(file: File, charset: Charset = Charsets.UTF_8): Try<T?, Throwable> = runTry {
      val klaxonAst = Klaxon().parseJsonObject(file.bufferedReader(charset))
      val ast = fromKlaxonAST(klaxonAst)
      fromJsonValue<T>(ast)
   }

   inline fun <reified T> fromJsonValue(value: JsValue): T? = fromJsonValueImpl(typeLiteral<T>(), value) as T?

   fun fromJsonValueImpl(typeTargetJ: Type, value: JsValue): Any? {
      val typeJ = typeTargetJ.toRaw()
      val typeK = typeJ.kotlin
      val converter = converters.byType.getElementOfSuper(typeJ).asIf<JsConverter<Any?>>()
      return if (converter!=null) {
         converter.fromJson(value)
      } else {
         when (value) {
            is JsNull -> null
            is JsTrue -> true
            is JsFalse -> false
            is JsNumber -> value.value
            is JsString -> {
               if (typeJ.isEnum) getEnumValue(typeJ, value.value)
               else value.value
            }
            is JsArray -> {
               when {
                  Collection::class.isSuperclassOf(typeK) -> {
                     val itemType = typeTargetJ.asIf<ParameterizedType>()?.actualTypeArguments?.get(0)
                        ?: typeLiteral<Any>()
                     val values = value.value.map { fromJsonValueImpl(itemType, it) }
                     when (typeK) {
                        Set::class -> values.toSet()
                        List::class -> values
                        else -> fail { "Unsupported collection type=$typeK" }
                     }
                  }
                  typeK===Array<Any?>::class -> {
                     val arrayType = typeTargetJ.asIf<GenericArrayType>()?.genericComponentType ?: typeLiteral<Any>()
                     value.value.map { fromJsonValueImpl(arrayType, it) }.toTypedArray()
                  }
                  typeK===ByteArray::class -> value.value.mapNotNull { fromJsonValue<Byte>(it) }.toByteArray()
                  typeK===CharArray::class -> value.value.mapNotNull { fromJsonValue<Char>(it) }.toCharArray()
                  typeK===ShortArray::class -> value.value.mapNotNull { fromJsonValue<Short>(it) }.toShortArray()
                  typeK===IntArray::class -> value.value.mapNotNull { fromJsonValue<Int>(it) }.toIntArray()
                  typeK===LongArray::class -> value.value.mapNotNull { fromJsonValue<Long>(it) }.toLongArray()
                  typeK===FloatArray::class -> value.value.mapNotNull { fromJsonValue<Float>(it) }.toFloatArray()
                  typeK===DoubleArray::class -> value.value.mapNotNull { fromJsonValue<Double>(it) }.toDoubleArray()
                  typeK===BooleanArray::class -> value.value.mapNotNull { fromJsonValue<Boolean>(it) }.toBooleanArray()
                  else -> fail { "Unsupported collection type=$typeK" }
               }
            }
            is JsObject -> {
               val instanceType = null
                  ?: value.value["_type"]?.asJsString()?.net { typeAliases.byAlias[it] ?: Class.forName(it).kotlin }
                  ?: typeK
               when {
                  instanceType.objectInstance!=null -> instanceType.objectInstance
                  instanceType==Short::class -> value.value["value"]?.asJsNumber()?.toShort()
                  instanceType==Int::class -> value.value["value"]?.asJsNumber()?.toInt()
                  instanceType==Long::class -> value.value["value"]?.asJsNumber()?.toLong()
                  instanceType==Float::class -> value.value["value"]?.asJsNumber()?.toFloat()
                  instanceType==Double::class -> value.value["value"]?.asJsNumber()?.toDouble()
                  instanceType==Number::class -> value.value["value"]?.asJsNumber()
                  instanceType==String::class -> value.value["value"]?.asJsString()
                  instanceType.isSubclassOf<Enum<*>>() -> value.value["value"]?.asJsString()?.let { getEnumValue(instanceType.javaObjectType, it) }
                  instanceType.isSubclassOf<Map<*, *>>() -> {
                     val mapKeyType = typeTargetJ.asIf<ParameterizedType>()?.actualTypeArguments?.get(0)?.toRaw()
                        ?: String::class.java
                     val mapValueType = typeTargetJ.asIf<ParameterizedType>()?.actualTypeArguments?.get(1)
                        ?: typeLiteral<Any>()
                     value.value.mapKeys { keyMapConverter.ofS(mapKeyType, it.key).orThrow }.mapValues { fromJsonValueImpl(mapValueType, it.value) }
                  }
                  else -> {
                     val constructor = instanceType.constructors.firstOrNull()
                        ?: fail { "Type=$instanceType has no constructor" }
                     val arguments = constructor.parameters.mapNotNull {
                        val argJs = value.value[it.name]
                        if (argJs==null) {
                           if (it.isOptional) null
                           else fail { "Type=$instanceType constructor parameter=${it.name} is missing" }
                        } else {
                           it to argJs.let { argJsValue -> fromJsonValueImpl(it.type.javaType, argJsValue) }
                        }
                     }.toMap()

                     runTry {
                        constructor.isAccessible = true
                        constructor.callBy(arguments)
                     }.getOrSupply {
                        fail { "Failed to instantiate $instanceType from $value. Reason=${it.message}" }
                     }
                  }
               }
            }
         }
      }
   }

   class TypeAliases {
      val byType: MutableMap<KClass<*>, String> = mutableMapOf()
      val byAlias: MutableMap<String, KClass<*>> = mutableMapOf()

      operator fun invoke(block: TypeAliases.() -> Unit) = block()

      infix fun String.alias(type: KClass<*>) {
         byAlias[this].ifNotNull { fail { "Type alias=$this already exists for type $it" } }
         byType[type] = this
         byAlias[this] = type
      }
   }

   class TypeConverters {
      val byType = ClassListMap<JsConverter<*>> { fail() }

      operator fun invoke(block: TypeConverters.() -> Unit) = block()

      infix fun <T: Any> KClass<T>.convert(converter: JsConverter<T>) = byType.accumulate(this.java, converter)
   }

}

//object JsonDsl {}
//
//fun <T> json(block: JsonDsl.() -> T) : T = JsonDsl.block()

fun JsValue.prettyPrint(indent: String = "  ", newline: String = "\n"): String {
   fun String.jsonEscape() = replace("\\", "\\\\").replace("\"", "\\\"")
   fun String.toJsonString() = "\"${this.jsonEscape()}\""
   fun String.reIndent() = replace(newline, newline + indent)
   return when (this) {
      is JsNull -> "null"
      is JsTrue -> "true"
      is JsFalse -> "false"
      is JsString -> value.toJsonString()
      is JsNumber -> value.toString()
      is JsArray ->
         if (value.isEmpty()) "[]"
         else "[" + newline + indent + value.joinToString(",$newline") { it.prettyPrint() }.reIndent() + newline + "]"
      is JsObject ->
         if (value.isEmpty()) "{}"
         else "{" + newline + indent + value.entries.joinToString(",$newline") { it.key.toJsonString() + ": " + it.value.prettyPrint() }.reIndent() + newline + "}"
   }
}

fun fromKlaxonAST(ast: Any?): JsValue {
   return when (ast) {
      null -> JsNull
      true -> JsTrue
      false -> JsFalse
      is String -> JsString(ast)
      is Number -> JsNumber(ast)
      is JsonObject -> JsObject(ast.map.mapValues { fromKlaxonAST(it.value) })
      is JsonArray<*> -> JsArray(ast.map { fromKlaxonAST(it) })
      is JsonValue -> null
         ?: ast.array?.let { fromKlaxonAST(it) }
         ?: ast.obj?.let { fromKlaxonAST(it) }
         ?: ast.boolean?.let { fromKlaxonAST(it) }
         ?: ast.boolean?.let { fromKlaxonAST(it) }
         ?: ast.char?.let { JsString(it.toString()) }
         ?: ast.double?.let { JsNumber(it) }
         ?: ast.float?.let { JsNumber(it) }
         ?: ast.int?.let { JsNumber(it) }
         ?: ast.longValue?.let { JsNumber(it) }
         ?: ast.string?.let { JsString(it) }
         ?: JsNull
      else -> fail { "Unrecognized klaxon AST representation=$ast" }
   }
}

@Suppress("UNCHECKED_CAST", "DEPRECATION")
fun getEnumValue(enumClass: Class<*>, value: String): Enum<*> {
   val enumConstants = enumClass.enumConstants as Array<out Enum<*>>
   return enumConstants.first { it.name==value }
}