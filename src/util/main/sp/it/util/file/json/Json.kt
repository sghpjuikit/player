package sp.it.util.file.json

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.NumericNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import java.io.File
import java.io.InputStream
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.ZoneId
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
import kotlin.text.Charsets.UTF_8
import sp.it.util.collections.map.KClassListMap
import sp.it.util.dev.fail
import sp.it.util.functional.Try
import sp.it.util.functional.andAlso
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import sp.it.util.functional.getOrSupply
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.net
import sp.it.util.functional.runTry
import sp.it.util.parsing.ConverterDefault
import sp.it.util.parsing.Parsers
import sp.it.util.text.escapeJson
import sp.it.util.type.VType
import sp.it.util.type.argOf
import sp.it.util.type.isEnumClass
import sp.it.util.type.isObject
import sp.it.util.type.isPlatformType
import sp.it.util.type.isSubclassOf
import sp.it.util.type.kType
import sp.it.util.type.kTypeAnyNullable
import sp.it.util.type.raw
import sp.it.util.type.type
import sp.it.util.type.typeOrAny

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

sealed interface JsRoot

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

open class JsonAst {
   fun ast(json: String): Try<JsValue, Throwable> = runTry { fromKlaxonAST(JsonMapper().readTree(json)) }

   fun ast(json: InputStream): Try<JsValue, Throwable> = runTry { fromKlaxonAST(JsonMapper().readTree(json)) }
}

class Json: JsonAst() {

   val converters = TypeConverters()
   val typeAliases = TypeAliases()
   val keyMapConverter: ConverterDefault = Parsers.DEFAULT

   init {
      @Suppress("SpellCheckingInspection")
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
         Instant::class convert object: JsConverter<Instant> {
            override fun toJson(value: Instant) = JsString(value.toString())
            override fun fromJson(value: JsValue) = when(value) {
               is JsNull -> null
               is JsNumber -> Instant.ofEpochMilli(value.value.toLong())
               is JsString -> Instant.parse(value.value)
               else -> fail { "Unsupported ${Instant::class} value=$value" }
            }
         }
         ZoneId::class convert object: JsConverter<ZoneId> {
            override fun toJson(value: ZoneId) = JsString(value.toString())
            override fun fromJson(value: JsValue) = value.asJsStringValue()?.let { ZoneId.of(it) }
         }
      }
   }

   inline fun <reified T: Any> toJsonValue(value: T?): JsValue = toJsonValue(kType<T>(), value)

   fun <T> toJsonValue(typeAs: VType<T>, value: T): JsValue = toJsonValue(typeAs.type, value)

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

            fun typeWitness() = "_type" to JsString(
               typeAliases.byType[type]
                  ?: type.qualifiedName
                  ?: fail { "Unable to serialize instance of type=$type: Type has no fully qualified name" }
            )

            fun JsValue.withAmbiguity(a: Boolean = isAmbiguous) = if (a) JsObject(mapOf("value" to this, typeWitness())) else this

            when (value) {
               is Number -> JsNumber(value).withAmbiguity()
               is UByte -> JsNumber(value.toShort()).withAmbiguity()
               is UShort -> JsNumber(value.toInt()).withAmbiguity()
               is UInt -> JsNumber(value.toLong()).withAmbiguity()
               is ULong -> JsNumber(value.toString().toBigInteger()).withAmbiguity()
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
               is Collection<*> -> JsArray(value.map { toJsonValue(typeAs.argOf(Collection::class, 0).typeOrAny, it) })   // TODO: preserve collection/map type
               is Map<*, *> -> JsObject(value.mapKeys { keyMapConverter.toS(it.key) }.mapValues { toJsonValue(typeAs.argOf(Map::class, 1).typeOrAny, it.value) })
               else -> {
                  val converter = converters.byType.getElementOfSuper(value::class).asIf<JsConverter<Any>>()?.takeIf { it.canConvert(value) }
                  when {
                     converter!=null -> {
                        val isStillAmbiguous = typeAsRaw==Any::class
                        converter.toJson(value).withAmbiguity(isStillAmbiguous)
                     }
                     else -> {
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
   }

   fun <T> fromJson(type: VType<T>, json: String): Try<T, Throwable> = fromJson(type, json.byteInputStream(UTF_8))

   fun <T> fromJson(type: VType<T>, json: File): Try<T, Throwable> = fromJson(type, json.inputStream())

   @Suppress("unchecked_cast")
   fun <T> fromJson(type: VType<T>, json: InputStream): Try<T, Throwable> = ast(json).andAlso {
      runTry { fromJsonValueImpl(type.type, it) as T }
   }

   inline fun <reified T> fromJson(json: String): Try<T, Throwable> = fromJson(type(), json)

   inline fun <reified T> fromJson(json: File): Try<T, Throwable> = fromJson(type(), json)

   inline fun <reified T> fromJson(json: InputStream): Try<T, Throwable> = fromJson(type(), json)

   inline fun <reified T> fromJsonValue(value: JsValue): Try<T, Throwable> = runTry { fromJsonValueImpl(value) }

   @Suppress("unchecked_cast")
   fun <T> fromJsonValue(type: VType<T>, value: JsValue): Try<T, Throwable> = runTry { fromJsonValueImpl(type.type, value) as T }

   fun fromJsonValue(type: KType, value: JsValue): Try<Any?, Throwable> = runTry { fromJsonValueImpl(type, value) }

   inline fun <reified T> fromJsonValueImpl(value: JsValue): T = fromJsonValueImpl(kType<T>(), value) as T

   fun fromJsonValueImpl(typeTarget: KType, value: JsValue): Any? {
      val typeK = typeTarget.raw
      val typeJ = typeK.java
      val converter = converters.byType.getElementOfSuper(typeK).asIf<JsConverter<Any?>>()
      return when {
         converter!=null -> converter.fromJson(value)
         value::class==typeK -> return value
         else -> {
            when (value) {
               is JsNull -> if (typeTarget.isMarkedNullable || typeTarget.isPlatformType) null else fail { "null is not $typeTarget" }
               is JsTrue -> true
               is JsFalse -> false
               is JsNumber -> {
                  when (typeK) {
                     Any::class -> value.value
                     Number::class -> value.value
                     Byte::class -> value.value.toByte()
                     UByte::class -> value.value.toShort().toUByte()
                     Short::class -> value.value.toShort()
                     UShort::class -> value.value.toInt().toUShort()
                     Int::class -> value.value.toInt()
                     UInt::class -> value.value.toLong().toUInt()
                     Long::class -> value.value.toLong()
                     ULong::class -> value.value.toString().toULong()
                     Float::class -> value.value.toFloat()
                     Double::class -> value.value.toDouble()
                     BigInteger::class -> when (value.value) {
                        is BigInteger -> value.value
                        is BigDecimal -> value.value.toBigInteger()
                        else -> BigInteger.valueOf(value.value.toLong())
                     }
                     BigDecimal::class -> when (value.value) {
                        is BigInteger -> value.value.toBigDecimal()
                        is BigDecimal -> value.value
                        else -> BigDecimal(value.value.toString())
                     }
                     else -> fail { "Unsupported number type=$typeK" }
                  }
               }
               is JsString -> {
                  if (typeJ.isEnumClass) getEnumValue(typeJ, value.value)
                  else value.value
               }
               is JsArray -> {
                  val typeKS = typeTarget.toString()
                  when {
                     typeK==Any::class -> value.value.map { fromJsonValueImpl(kTypeAnyNullable(), it) }
                     Collection::class.isSuperclassOf(typeK) -> {
                        val itemType = typeTarget.argOf(Collection::class, 0).typeOrAny
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
                     typeKS=="kotlin.Array<kotlin.Byte>" -> value.value.map { fromJsonValueImpl<Byte>(it) }.toByteArray().toTypedArray()
                     typeK==ByteArray::class -> value.value.map { fromJsonValueImpl<Byte>(it) }.toByteArray()
                     typeKS=="kotlin.Array<kotlin.Char>" -> value.value.map { fromJsonValueImpl<Char>(it) }.toCharArray().toTypedArray()
                     typeK==CharArray::class -> value.value.map { fromJsonValueImpl<Char>(it) }.toCharArray()
                     typeKS=="kotlin.Array<kotlin.Short>" -> value.value.map { fromJsonValueImpl<Short>(it) }.toShortArray().toTypedArray()
                     typeK==ShortArray::class -> value.value.map { fromJsonValueImpl<Short>(it) }.toShortArray()
                     typeKS=="kotlin.Array<kotlin.Int>" -> value.value.map { fromJsonValueImpl<Int>(it) }.toIntArray().toTypedArray()
                     typeK==IntArray::class -> value.value.map { fromJsonValueImpl<Int>(it) }.toIntArray()
                     typeKS=="kotlin.Array<kotlin.Long>" -> value.value.map { fromJsonValueImpl<Long>(it) }.toLongArray().toTypedArray()
                     typeK==LongArray::class -> value.value.map { fromJsonValueImpl<Long>(it) }.toLongArray()
                     typeKS=="kotlin.Array<kotlin.Float>" -> value.value.map { fromJsonValueImpl<Float>(it) }.toFloatArray().toTypedArray()
                     typeK==FloatArray::class -> value.value.map { fromJsonValueImpl<Float>(it) }.toFloatArray()
                     typeKS=="kotlin.Array<kotlin.Double>" -> value.value.map { fromJsonValueImpl<Double>(it) }.toDoubleArray().toTypedArray()
                     typeK==DoubleArray::class -> value.value.map { fromJsonValueImpl<Double>(it) }.toDoubleArray()
                     typeKS=="kotlin.Array<kotlin.Boolean>" -> value.value.map { fromJsonValueImpl<Boolean>(it) }.toBooleanArray().toTypedArray()
                     typeK==BooleanArray::class -> value.value.map { fromJsonValueImpl<Boolean>(it) }.toBooleanArray()
                     typeJ.isArray -> {
                        val arrayType = typeTarget.argOf(Array::class, 0).typeOrAny
                        val array = java.lang.reflect.Array.newInstance(arrayType.raw.java, value.value.size)
                        value.value.map { fromJsonValueImpl(arrayType, it) }.forEachIndexed { i, e -> java.lang.reflect.Array.set(array, i, e) }
                        array
                     }
                     else -> fail { "Unsupported collection type=$typeK" }
                  }
               }
               is JsObject -> {
                  val instanceType = null
                     ?: value.value["_type"]?.asJsStringValue()?.net { typeAliases.byAlias[it] ?: Class.forName(it).kotlin }
                     ?: typeK
                  val converterValue = converters.byType.getElementOfSuper(instanceType).asIf<JsConverter<Any?>>()
                  when {
                     converterValue!=null -> converterValue.fromJson(value.value["value"]!!)
                     instanceType==String::class -> value.value["value"]?.asJsStringValue()
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
                     instanceType==BigInteger::class -> value.value["value"]?.asJsNumberValue()?.toLong()?.toBigInteger()
                     instanceType==BigDecimal::class -> value.value["value"]?.asJsNumberValue()?.toDouble()?.toBigDecimal()
                     instanceType==Number::class -> value.value["value"]?.asJsNumberValue()
                     instanceType.isObject -> instanceType.objectInstance
                     instanceType.isSubclassOf<Enum<*>>() -> value.value["value"]?.asJsStringValue()?.let { getEnumValue(instanceType.javaObjectType, it) }
                     instanceType==Any::class -> {
                        val mapKeyType = type<String>()
                        val mapValueType = kType<Any?>()
                        value.value.mapKeys { keyMapConverter.ofS(mapKeyType, it.key).orThrow }.mapValues { fromJsonValueImpl(mapValueType, it.value) }
                     }
                     instanceType.isSubclassOf<Map<*, *>>() -> {
                        val mapKeyType = typeTarget.argOf(Map::class, 0).typeOrAny.net { VType<Any?>(it) }
                        val mapValueType = typeTarget.argOf(Map::class, 1).typeOrAny
                        value.value.mapKeys { keyMapConverter.ofS(mapKeyType, it.key).orThrow }.mapValues { fromJsonValueImpl(mapValueType, it.value) }
                     }
                     else -> {
                        val constructor = instanceType.constructors.firstOrNull()
                           ?: fail { "Type=$instanceType has no constructor" }
                        val arguments = constructor.parameters.mapNotNull {
                           val argJs = value.value[it.name]
                           if (argJs==null) {
                              if (it.isOptional) null
                              else if (it.type.isMarkedNullable || it.type.isPlatformType) it to null
                              else fail { "Type=$instanceType constructor parameter=${it.name} is missing" }
                           } else {
                              it to fromJsonValueImpl(it.type, argJs)
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
      is NullNode -> JsNull
      is Number -> JsNumber(ast)
      is BooleanNode -> fromKlaxonAST(ast.booleanValue())
      is TextNode -> JsString(ast.textValue())
      is NumericNode -> JsNumber(ast.numberValue())
      is ArrayNode -> JsArray(ast.elements().asSequence().map { fromKlaxonAST(it) }.toList())
      is ObjectNode -> JsObject(ast.fields().asSequence().associate { it.key to fromKlaxonAST(it.value) })
      else -> fail { "Unrecognized klaxon AST representation=$ast" }
   }
}

@Suppress("UNCHECKED_CAST", "DEPRECATION")
fun getEnumValue(enumClass: Class<*>, value: String): Enum<*> {
   val enumConstants = enumClass.enumConstants as Array<out Enum<*>>
   return enumConstants.first { it.name==value }
}