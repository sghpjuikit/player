package sp.it.util.file.json

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.JsonValue
import com.beust.klaxon.Klaxon
import sp.it.util.collections.map.KClassListMap
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
import sp.it.util.text.escapeJson
import sp.it.util.type.argOf
import sp.it.util.type.isSubclassOf
import sp.it.util.type.toRaw
import sp.it.util.type.jType
import sp.it.util.type.kType
import sp.it.util.type.raw
import sp.it.util.type.typeResolved
import java.io.File
import java.io.InputStream
import java.io.SequenceInputStream
import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.charset.Charset
import java.util.ArrayDeque
import java.util.BitSet
import java.util.Collections
import java.util.Deque
import java.util.Hashtable
import java.util.LinkedList
import java.util.NavigableSet
import java.util.PriorityQueue
import java.util.Queue
import java.util.SortedSet
import java.util.Stack
import java.util.TreeMap
import java.util.TreeSet
import java.util.UUID
import java.util.Vector
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaType
import kotlin.text.Charsets.UTF_8
import sp.it.util.functional.andAlso
import sp.it.util.type.VType
import sp.it.util.type.isEnumClass
import sp.it.util.type.isObject
import sp.it.util.type.type

sealed class JsValue {
   fun asJsNull() = asIs<JsNull>()
   fun asJsTrue() = asIs<JsTrue>()
   fun asJsFalse() = asIs<JsFalse>()
   fun asJsString() = asIs<JsString>()
   fun asJsNumber() = asIs<JsNumber>()
   fun asJsArray() = asIs<JsArray>()
   fun asJsObject() = asIs<JsObject>()

   fun asJsNullValue() = asIs<JsNull>().let { null }
   fun asJsTrueValue() = if (this is JsNull) null else asIs<JsTrue>().let { true }
   fun asJsFalseValue() = if (this is JsNull) null else asIs<JsFalse>().let { false }
   fun asJsStringValue() = if (this is JsNull) null else asIs<JsString>().value
   fun asJsNumberValue() = if (this is JsNull) null else asIs<JsNumber>().value
   fun asJsArrayValue() = if (this is JsNull) null else asIs<JsArray>().value
   fun asJsObjectValue() = if (this is JsNull) null else asIs<JsObject>().value

   override fun toString() = "${this::class} ${toPrettyS()}"
}

interface JsRoot

object JsNull: JsValue()

object JsTrue: JsValue()

object JsFalse: JsValue()

class JsString(val value: String): JsValue()

class JsNumber(val value: Number): JsValue()

class JsArray(val value: List<JsValue>): JsValue(), JsRoot

class JsObject(val value: Map<String, JsValue>): JsValue(), JsRoot {
   constructor(entry: Pair<String, JsValue>): this(mapOf(entry))
}


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
                 "char" alias Char::class
              "boolean" alias Boolean::class
                 "byte" alias Byte::class
                "ubyte" alias UByte::class
                "short" alias Short::class
               "ushort" alias UShort::class
                  "int" alias Int::class
                 "uint" alias UInt::class
                 "long" alias Long::class
                "ulong" alias ULong::class
                "float" alias Float::class
               "double" alias Double::class
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
            override fun fromJson(value: JsValue) = value.asJsStringValue()?.let { UUID.fromString(it) }
         }
      }
   }

   inline fun <reified T: Any> toJsonValue(value: T?): JsValue = toJsonValue(kType<T>(), value)

   fun toJsonValue(typeAs: KType, value: Any?): JsValue {
      return when (value) {
         null -> JsNull
         true -> JsTrue
         false -> JsFalse
         else -> {
            val type = value::class
            val typeAsRaw = typeAs.raw
            val isObject = type.isObject
            val isAmbiguous = typeAsRaw==Any::class || typeAsRaw.isSealed || isObject || type!=typeAsRaw

            fun typeWitness() = "_type" to JsString(typeAliases.byType[type]
               ?: type.qualifiedName
               ?: fail { "Unable to serialize instance of type=$type: Type has no fully qualified name" })

            fun JsValue.withAmbiguity() = if (isAmbiguous) JsObject(mapOf("value" to this, typeWitness())) else this

            when (value) {
               is Number -> JsNumber(value).withAmbiguity()
               is UByte -> JsNumber(value.toByte()).withAmbiguity()
               is UShort -> JsNumber(value.toShort()).withAmbiguity()
               is UInt -> JsNumber(value.toInt()).withAmbiguity()
               is ULong -> JsNumber(value.toLong()).withAmbiguity()
               is String -> JsString(value)
               is Enum<*> -> JsString(value.name).withAmbiguity()
               is Array<*> -> JsArray(value.map { toJsonValue(kType<Any>(), it) })
               is ByteArray -> JsArray(value.map { toJsonValue(kType<Byte>(), it) })
               is CharArray -> JsArray(value.map { toJsonValue(kType<Char>(), it) })
               is ShortArray -> JsArray(value.map { toJsonValue(kType<Short>(), it) })
               is IntArray -> JsArray(value.map { toJsonValue(kType<Int>(), it) })
               is LongArray -> JsArray(value.map { toJsonValue(kType<Long>(), it) })
               is FloatArray -> JsArray(value.map { toJsonValue(kType<Float>(), it) })
               is DoubleArray -> JsArray(value.map { toJsonValue(kType<Double>(), it) })
               is BooleanArray -> JsArray(value.map { toJsonValue(kType<Boolean>(), it) })
               is Collection<*> -> JsArray(value.map { toJsonValue(typeAs.argOf(Collection::class, 0).typeResolved, it) })   // TODO: preserve collection/map type
               is Map<*, *> -> JsObject(value.mapKeys { keyMapConverter.toS(it.key) }.mapValues { toJsonValue(typeAs.argOf(Map::class, 1).typeResolved, it.value) })
               else -> {
                  val converter = converters.byType.getElementOfSuper(value::class)
                     .asIf<JsConverter<Any>>()
                     ?.takeIf { it.canConvert(value) }
                  if (converter!=null) {
                     converter.toJson(value)
                  } else {
                     val values = type.memberProperties.asSequence()
                        .filter { it.javaField!=null }
                        .associate {
                           it.isAccessible = true
                           it.name to toJsonValue(it.returnType, it.getter.call(value))
                        }
                        .let {
                           when {
                              isObject -> mapOf(typeWitness())
                              isAmbiguous -> (it + typeWitness())
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

   fun ast(json: String): Try<JsValue, Throwable> = ast(json.byteInputStream(UTF_8), UTF_8)

   fun ast(json: InputStream, charset: Charset = UTF_8): Try<JsValue, Throwable> = runTry {
      val i = SequenceInputStream(SequenceInputStream(
         "{ \"value\": ".byteInputStream(UTF_8),
         json),
         " }".byteInputStream(UTF_8)
      )
      val klaxonAst = Klaxon().parser().parse(i, charset).asIs<JsonObject>()["value"]
      fromKlaxonAST(klaxonAst)
   }

   fun <T> fromJson(type: VType<T>, json: String): Try<T?, Throwable> = fromJson(type, json.byteInputStream(UTF_8), UTF_8)

   fun <T> fromJson(type: VType<T>, json: File, charset: Charset = UTF_8): Try<T?, Throwable> = fromJson(type, json.inputStream(), charset)

   @Suppress("unchecked_cast")
   fun <T> fromJson(type: VType<T>, json: InputStream, charset: Charset = UTF_8): Try<T?, Throwable> = ast(json, charset).andAlso {
      runTry { fromJsonValueImpl(type.type.javaType, it) as T }
   }

   inline fun <reified T> fromJson(json: String): Try<T?, Throwable> = fromJson(type<T>(), json)

   inline fun <reified T> fromJson(json: File, charset: Charset = UTF_8): Try<T?, Throwable> = fromJson(type<T>(), json, charset)

   inline fun <reified T> fromJson(json: InputStream, charset: Charset = UTF_8): Try<T?, Throwable> = fromJson(type<T>(), json, charset)

   inline fun <reified T> fromJsonValue(value: JsValue): Try<T?, Throwable> = runTry { fromJsonValueImpl<T>(value) }

   fun fromJsonValue(typeTargetJ: Type, value: JsValue): Try<Any?, Throwable> = runTry { fromJsonValueImpl(typeTargetJ, value) }

   inline fun <reified T> fromJsonValueImpl(value: JsValue): T? = fromJsonValueImpl(jType<T>(), value) as T?

   fun fromJsonValueImpl(typeTargetJ: Type, value: JsValue): Any? {
      val typeJ = typeTargetJ.toRaw()
      val typeK = typeJ.kotlin
      val converter = converters.byType.getElementOfSuper(typeK).asIf<JsConverter<Any?>>()
      return when {
         converter!=null -> converter.fromJson(value)
         value::class==typeK -> return value
         else -> {
            when (value) {
               is JsNull -> null
               is JsTrue -> true
               is JsFalse -> false
               is JsNumber -> {
                  when (typeK) {
                     Any::class -> value.value
                     Number::class -> value.value
                     Byte::class -> value.value.toByte()
                     UByte::class -> value.value.toByte().toUByte()
                     Short::class -> value.value.toShort()
                     UShort::class -> value.value.toShort().toUShort()
                     Int::class -> value.value.toInt()
                     UInt::class -> value.value.toInt().toUInt()
                     Long::class -> value.value.toLong()
                     ULong::class -> value.value.toLong().toULong()
                     Float::class -> value.value.toFloat()
                     Double::class -> value.value.toDouble()
                     BigInteger::class -> when (value.value) {
                        is BigInteger -> value.value
                        is BigDecimal -> value.value.toBigInteger()
                        else -> BigInteger.valueOf(value.value.toLong())
                     }
                     BigDecimal::class -> when (value.value) {
                        is BigDecimal -> value.value.toBigInteger()
                        else -> BigDecimal.valueOf(value.value.toDouble())
                     }
                     else -> fail { "Unsupported number type=$typeK" }
                  }
               }
               is JsString -> {
                  if (typeJ.isEnumClass) getEnumValue(typeJ, value.value)
                  else value.value
               }
               is JsArray -> {
                  when {
                     typeK==Any::class -> value.value.map { fromJsonValueImpl(jType<Any>(), it) }
                     Collection::class.isSuperclassOf(typeK) -> {
                        val itemType = typeTargetJ.asIf<ParameterizedType>()?.actualTypeArguments?.get(0) ?: jType<Any>()
                        val values = value.value.map { fromJsonValueImpl(itemType, it) }
                        when (typeK) {
                           Set::class -> HashSet(values)
                           MutableSet::class -> HashSet(values)
                           HashSet::class -> HashSet(values)
                           SortedSet::class -> TreeSet(values)
                           NavigableSet::class -> TreeSet(values)
                           TreeSet::class -> TreeSet(values)
                           List::class -> values
                           MutableList::class -> ArrayList(values)
                           LinkedList::class -> LinkedList(values)
                           ArrayList::class -> ArrayList(values)
                           Vector::class -> Vector(values)
                           Vector::class -> Vector(values)
                           Stack::class -> Stack<Any?>().apply { values.forEach { push(it) } }
                           Queue::class -> ArrayDeque(values)
                           Deque::class -> ArrayDeque(values)
                           ArrayDeque::class -> ArrayDeque(values)
                           PriorityQueue::class -> PriorityQueue(values)
                           else -> fail { "Unsupported collection type=$typeK" }
                        }
                     }
                     typeK===Array<Any?>::class -> {
                        val arrayType = typeTargetJ.asIf<GenericArrayType>()?.genericComponentType ?: jType<Any>()
                        value.value.map { fromJsonValueImpl(arrayType, it) }.toTypedArray()
                     }
                     typeK===ByteArray::class -> value.value.mapNotNull { fromJsonValueImpl<Byte>(it) }.toByteArray()
                     typeK===CharArray::class -> value.value.mapNotNull { fromJsonValueImpl<Char>(it) }.toCharArray()
                     typeK===ShortArray::class -> value.value.mapNotNull { fromJsonValueImpl<Short>(it) }.toShortArray()
                     typeK===IntArray::class -> value.value.mapNotNull { fromJsonValueImpl<Int>(it) }.toIntArray()
                     typeK===LongArray::class -> value.value.mapNotNull { fromJsonValueImpl<Long>(it) }.toLongArray()
                     typeK===FloatArray::class -> value.value.mapNotNull { fromJsonValueImpl<Float>(it) }.toFloatArray()
                     typeK===DoubleArray::class -> value.value.mapNotNull { fromJsonValueImpl<Double>(it) }.toDoubleArray()
                     typeK===BooleanArray::class -> value.value.mapNotNull { fromJsonValueImpl<Boolean>(it) }.toBooleanArray()
                     else -> fail { "Unsupported collection type=$typeK" }
                  }
               }
               is JsObject -> {
                  val instanceType = null
                     ?: value.value["_type"]?.asJsStringValue()?.net { typeAliases.byAlias[it] ?: Class.forName(it).kotlin }
                     ?: typeK
                  when {
                     instanceType.isObject -> instanceType.objectInstance
                     instanceType==Byte::class -> value.value["value"]?.asJsNumberValue()?.toByte()
                     instanceType==UByte::class -> value.value["value"]?.asJsNumberValue()?.toShort()?.toUByte()
                     instanceType==Short::class -> value.value["value"]?.asJsNumberValue()?.toShort()
                     instanceType==UShort::class -> value.value["value"]?.asJsNumberValue()?.toInt()?.toUShort()
                     instanceType==Int::class -> value.value["value"]?.asJsNumberValue()?.toInt()
                     instanceType==UInt::class -> value.value["value"]?.asJsNumberValue()?.toLong()?.toUInt()
                     instanceType==Long::class -> value.value["value"]?.asJsNumberValue()?.toLong()
                     instanceType==ULong::class -> value.value["value"]?.asJsNumberValue()?.toLong()?.toULong()
                     instanceType==Float::class -> value.value["value"]?.asJsNumberValue()?.toFloat()
                     instanceType==Double::class -> value.value["value"]?.asJsNumberValue()?.toDouble()
                     instanceType==Number::class -> value.value["value"]?.asJsNumberValue()
                     instanceType==String::class -> value.value["value"]?.asJsStringValue()
                     instanceType.isSubclassOf<Enum<*>>() -> value.value["value"]?.asJsStringValue()?.let { getEnumValue(instanceType.javaObjectType, it) }
                     instanceType==Any::class || instanceType.isSubclassOf<Map<*, *>>() -> {
                        val mapKeyType = typeTargetJ.asIf<ParameterizedType>()?.actualTypeArguments?.get(0)?.toRaw()?.kotlin ?: String::class
                        val mapValueType = typeTargetJ.asIf<ParameterizedType>()?.actualTypeArguments?.get(1) ?: jType<Any>()
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
                              it to fromJsonValueImpl(it.type.javaType, argJs)
                           }
                        }.toMap()

                        runTry {
                           constructor.isAccessible = true
                           constructor.callBy(arguments)
                        }.getOrSupply {
                           fail(it) { "Failed to instantiate $instanceType\nwith args:${arguments.mapKeys { it.key.name }}\nfrom:$value\n" }
                        }
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
      val byType = KClassListMap<JsConverter<*>> { fail() }

      operator fun invoke(block: TypeConverters.() -> Unit) = block()

      infix fun <T: Any> KClass<T>.convert(converter: JsConverter<T>) = byType.accumulate(this, converter)
   }

}

fun JsObject.values(): Collection<JsValue> = value.values

operator fun JsValue?.div(field: String): JsValue? = when (this) {
   null -> null
   is JsObject -> value[field]
   else -> fail { "Expected ${JsObject::class}, but got $this" }
}

operator fun JsValue?.div(index: Int): JsValue? = when (this) {
   null -> null
   is JsArray -> value[index]
   else -> fail { "Expected ${JsObject::class}, but got $this" }
}

fun JsValue.toCompactS(): String {
   fun String.toJsonString() = "\"${this.escapeJson()}\""
   return when (this) {
      is JsNull -> "null"
      is JsTrue -> "true"
      is JsFalse -> "false"
      is JsString -> value.toJsonString()
      is JsNumber -> value.toString()
      is JsArray ->
         if (value.isEmpty()) "[]"
         else "[" + value.joinToString(",") { it.toPrettyS() } + "]"
      is JsObject ->
         if (value.isEmpty()) "{}"
         else "{" + value.entries.asSequence().sortedBy { it.key }
            .joinToString(",") { it.key.toJsonString() + ":" + it.value.toCompactS() } + "}"
   }
}

fun JsValue.toPrettyS(indent: String = "  ", newline: String = "\n"): String {
   fun String.toJsonString() = "\"${this.escapeJson()}\""
   fun String.reIndent() = replace(newline, newline + indent)
   return when (this) {
      is JsNull -> "null"
      is JsTrue -> "true"
      is JsFalse -> "false"
      is JsString -> value.toJsonString()
      is JsNumber -> value.toString()
      is JsArray ->
         if (value.isEmpty()) "[]"
         else "[$newline$indent" + value.joinToString(",$newline") { it.toPrettyS() }.reIndent() + newline + "]"
      is JsObject ->
         if (value.isEmpty()) "{}"
         else "{$newline$indent" + value.entries.asSequence().sortedBy { it.key }.joinToString(",$newline") { it.key.toJsonString() + ": " + it.value.toPrettyS() }.reIndent() + newline + "}"
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
         ?: ast.inside?.asIf<Char>()?.let { JsString(it.toString()) }
         ?: ast.double?.let { JsNumber(it) }
         ?: ast.float?.let { JsNumber(it) }
         ?: ast.int?.let { JsNumber(it) }
         ?: ast.longValue?.let { JsNumber(it) }
         ?: ast.inside?.asIf<BigInteger>()?.let { JsNumber(it) }
         ?: ast.inside?.asIf<BigDecimal>()?.let { JsNumber(it) }
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