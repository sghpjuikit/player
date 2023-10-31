package sp.it.util.file.json

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
import kotlin.reflect.KTypeParameter
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.jvmName
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
import sp.it.util.math.rangeBigDec
import sp.it.util.math.toBigDec
import sp.it.util.parsing.ConverterDefault
import sp.it.util.parsing.Parsers
import sp.it.util.text.Char16
import sp.it.util.text.Char32
import sp.it.util.text.chars16
import sp.it.util.text.chars32
import sp.it.util.text.escapeJson
import sp.it.util.text.length16
import sp.it.util.text.length32
import sp.it.util.type.VType
import sp.it.util.type.argOf
import sp.it.util.type.dataComponentProperties
import sp.it.util.type.isDataClass
import sp.it.util.type.isEnum
import sp.it.util.type.isEnumClass
import sp.it.util.type.isObject
import sp.it.util.type.isPlatformType
import sp.it.util.type.isSubclassOf
import sp.it.util.type.isValueClass
import sp.it.util.type.kType
import sp.it.util.type.kTypeAnyNullable
import sp.it.util.type.raw
import sp.it.util.type.sealedSubObjects
import sp.it.util.type.type
import sp.it.util.type.typeOrAny

sealed class JsValue: JsTokenLike {
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

data class JsString(val value: String): JsValue()

data class JsNumber(val value: Number): JsValue()

data class JsArray(val value: List<JsValue>): JsValue(), JsRoot {
   constructor(vararg values: JsValue): this(values.toList())
}

data class JsObject(val value: Map<String, JsValue>): JsValue(), JsRoot {
   constructor(vararg entries: Pair<String, JsValue>): this(entries.toMap(LinkedHashMap()))
}

open class JsonAst {

   fun ast(json: String): Try<JsValue, Throwable> = runTry { parseJson(json) }

   fun ast(json: InputStream): Try<JsValue, Throwable> = runTry { parseJson(json) }

}

class Json: JsonAst() {

   val converters = TypeConverters()
   val typeAliases = TypeAliases()
   val keyMapConverter: ConverterDefault = Parsers.DEFAULT

   init {
      keyMapConverter.addParserAsF(String::class, { it }, { it })

      typeAliases {
         // @formatter:off
                 "char" alias Char::class
               "char32" alias Char32::class
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
              "big-int" alias BigInteger::class
          "big-decimal" alias BigDecimal::class
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
         UUID::class convert JsConverterUuid
         Instant::class convert JsConverterInstant
         ZoneId::class convert JsConverterZoneId
      }
   }

   inline fun <reified T: Any> toJsonValue(value: T?): JsValue = toJsonValue(kType<T>(), value)

   fun <T> toJsonValue(typeAs: VType<T>, value: T): JsValue = toJsonValue(typeAs.type, value)

   fun toJsonValue(typeAs: KType, value: Any?): JsValue {
      return when (value) {
         null -> JsNull
         true -> JsTrue
         false -> JsFalse
         is JsValue -> value
         else -> {
            val type = value::class
            val typeAsRaw = typeAs.raw
            val isObject = type.isObject
            val isAmbiguous = typeAsRaw==Any::class || typeAsRaw.isSealed || isObject || type!=typeAsRaw
            val converter = converters.byType.getElementOfSuper(value::class).asIf<JsConverter<Any>>()?.takeIf { it.canConvert(value) }

            fun typeWitness() = "_type" to JsString(
               typeAliases.byType[type]
                  ?: type.qualifiedName
                  ?: fail { "Unable to serialize instance of type=$type: Type has no fully qualified name" }
            )

            fun JsValue.withAmbiguity(a: Boolean = isAmbiguous) = if (a) JsObject(mapOf("value" to this, typeWitness())) else this

            when {
               converter!=null -> converter.toJson(value).withAmbiguity(typeAsRaw==Any::class)
               typeAsRaw.isSealed && isObject -> JsString(type.jvmName)
               isObject -> JsObject(mapOf(typeWitness()))
               else -> when (value) {
                  is Number -> JsNumber(value).withAmbiguity()
                  is UByte -> JsNumber(value.toShort()).withAmbiguity()
                  is UShort -> JsNumber(value.toInt()).withAmbiguity()
                  is UInt -> JsNumber(value.toLong()).withAmbiguity()
                  is ULong -> JsNumber(value.toString().toBigInteger()).withAmbiguity()
                  is Char -> JsString(value.toString())
                  is Char32 -> JsString(value.toString())
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
                  is Collection<*> -> JsArray(
                     // TODO: preserve map type
                     value.map { toJsonValue(if (typeAsRaw.isSubclassOf(Collection::class)) typeAs.argOf(Collection::class, 0).typeOrAny else kType<Any>(), it) }
                  )

                  is Map<*, *> -> JsObject(
                     // TODO: preserve collection type
                     value.mapKeys { keyMapConverter.toS(it.key) }.mapValues {
                        toJsonValue(if (typeAsRaw.isSubclassOf(Map::class)) typeAs.argOf(Map::class, 1).typeOrAny else kType<Any>(), it.value)
                     }
                  )

                  else -> {
                     when {
                        type.isValueClass -> {
                           val p = type.declaredMemberProperties.first()
                           val v = p.getter.call(value)
                           toJsonValue(p.returnType, v).withAmbiguity()
                        }
                        type.isDataClass -> {
                           val typeParams = type.typeParameters.withIndex().associate { (i, p) -> i to p.name }
                           val typeArgs = typeAs.arguments.withIndex().associate { (i, a) -> typeParams[i] to a.typeOrAny }
                           val values = type.dataComponentProperties()
                              .associate {
                                 it.isAccessible = true
                                 val rtRaw = it.returnType
                                 val rt = rtRaw.classifier.asIf<KTypeParameter>()?.let { typeArgs[it.name] } ?: rtRaw
                                 it.name to toJsonValue(rt, it.getter.call(value))
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

                        else -> {
                           val typeParams = type.typeParameters.withIndex().associate { (i, p) -> i to p.name }
                           val typeArgs = typeAs.arguments.withIndex().associate { (i, a) -> typeParams[i] to a.typeOrAny }
                           val values = type.memberProperties.asSequence()
                              .filter { it.javaField!=null }
                              .associate {
                                 it.isAccessible = true
                                 val rtRaw = it.returnType
                                 val rt = rtRaw.classifier.asIf<KTypeParameter>()?.let { typeArgs[it.name] } ?: rtRaw
                                 it.name to toJsonValue(rt, it.getter.call(value))
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
         value is JsNull -> if (typeTarget.isMarkedNullable || typeTarget.isPlatformType) null else fail { "null is not $typeTarget" }
         converter!=null -> converter.fromJson(value)
         typeK==JsValue::class -> value
         typeK==value::class -> value
         typeK.isValue && value !is JsNull && value !is JsObject && typeK!=UByte::class && typeK!=UShort::class && typeK!=UInt::class && typeK!=ULong::class -> {
            val c = typeK.primaryConstructor ?: fail { "Value type=$typeK has no constructor" }
            val v = fromJsonValueImpl(c.parameters[0].type, value)
            runTry {
               c.isAccessible = true
               c.call(v)
            }.getOrSupply {
               fail(it) { "Failed to instantiate $typeK\nwith args:$v\nfrom:$value" }
            }
         }
         else -> {
            when (value) {
               is JsNull -> if (typeTarget.isMarkedNullable || typeTarget.isPlatformType) null else fail { "null is not $typeTarget" }
               is JsTrue -> true
               is JsFalse -> false
               is JsNumber -> {
                  when(typeK) {
                     value.value::class -> value.value
                     Any::class -> value.value
                     Number::class -> value.value
                     Byte::class -> value.value.toBigDec().net { if (it in Byte.rangeBigDec) it.toByte() else fail { "${value.value} not Byte" } }
                     UByte::class -> value.value.toBigDec().net { if (it in UByte.rangeBigDec) it.toShort().toUByte() else fail { "${value.value} not UByte" } }
                     Short::class -> value.value.toBigDec().net { if (it in Short.rangeBigDec) it.toShort() else fail { "${value.value} not Short" } }
                     UShort::class -> value.value.toBigDec().net { if (it in UShort.rangeBigDec) it.toInt().toUShort() else fail { "${value.value} not UShort" } }
                     Int::class -> value.value.toBigDec().net { if (it in Int.rangeBigDec) it.toInt() else fail { "${value.value} not Int" } }
                     UInt::class -> value.value.toBigDec().net { if (it in UInt.rangeBigDec) it.toLong().toUInt() else fail { "${value.value} not UInt" } }
                     Long::class -> value.value.toBigDec().net { if (it in Long.rangeBigDec) it.toLong() else fail { "${value.value} not Long" } }
                     ULong::class -> value.value.toBigDec().net { if (it in ULong.rangeBigDec) it.toString().toULong() else fail { "${value.value} not ULong" } }
                     Float::class -> value.value.toBigDec().net { if (it in Float.rangeBigDec) it.toFloat() else fail { "${value.value} not Float" } }
                     Double::class -> value.value.toBigDec().net { if (it in Double.rangeBigDec) it.toDouble() else fail { "${value.value} not Double" } }
                     BigInteger::class -> value.value.toBigDec().toBigInteger()
                     BigDecimal::class -> value.value.toBigDec()
                     else -> fail { "Unsupported number type=$typeK ${value.toPrettyS()}" }
                  }
               }
               is JsString -> {
                  when {
                     typeJ.isEnumClass -> getEnumValue(typeJ, value.value)
                     typeK==Char16::class -> if (value.value.length16==1) value.value.chars16().first() else fail { "${value.value} is not Char" }
                     typeK==Char32::class -> if (value.value.length32==1) value.value.chars32().first() else fail { "${value.value} is not Char32" }
                     typeK==Float::class -> when (value.value) {
                        "NaN" -> Float.NaN
                        "Infinity" -> Float.POSITIVE_INFINITY
                        "+Infinity" -> Float.POSITIVE_INFINITY
                        "-Infinity" -> Float.NEGATIVE_INFINITY
                        else -> fail { "${value.value} is not $typeTarget" }
                     }
                     typeK==Double::class -> when (value.value) {
                        "NaN" -> Double.NaN
                        "Infinity" -> Double.POSITIVE_INFINITY
                        "+Infinity" -> Double.POSITIVE_INFINITY
                        "-Infinity" -> Double.NEGATIVE_INFINITY
                        else -> fail { "${value.value} is not $typeTarget" }
                     }
                     typeK.isSealed -> {
                        typeK.sealedSubObjects.find { it::class.jvmName==value.value } ?: fail { "${value.value} is not $typeTarget" }
                     }
                     else -> value.value
                  }
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
                     instanceType==String::class ||
                     instanceType==Char::class ||
                     instanceType==Byte::class ||
                     instanceType==UByte::class ||
                     instanceType==Short::class ||
                     instanceType==UShort::class ||
                     instanceType==Int::class ||
                     instanceType==UInt::class ||
                     instanceType==Long::class ||
                     instanceType==ULong::class ||
                     instanceType==Float::class ||
                     instanceType==Double::class ||
                     instanceType==BigInteger::class ||
                     instanceType==BigDecimal::class ||
                     instanceType==Number::class -> value.value["value"]?.let { fromJsonValueImpl(instanceType.createType(nullable = true), it) }
                     instanceType.isObject -> instanceType.objectInstance
                     instanceType.isEnum -> value.value["value"]?.asJsStringValue()?.let { getEnumValue(instanceType.javaObjectType, it) }
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
                     instanceType.isValue -> {
                        val c = instanceType.primaryConstructor ?: fail { "Value type=$instanceType has no constructor" }
                        val v = fromJsonValueImpl(c.parameters[0].type, value.value["value"] ?: value)
                        runTry {
                           c.isAccessible = true
                           c.call(v)
                        }.getOrSupply {
                           fail(it) { "Failed to instantiate $instanceType\nwith args:$v\nfrom:$value" }
                        }
                     }
                     else -> {
                        val typeParams = typeK.typeParameters.withIndex().associate { (i, p) -> i to p.name }
                        val typeArgs = typeTarget.arguments.withIndex().associate { (i, a) -> typeParams[i] to a.typeOrAny }
                        val constructor = instanceType.constructors.firstOrNull() ?: fail { "Type=$instanceType has no constructor" }
                        val arguments = constructor.parameters.mapNotNull {
                           val argJs = value.value[it.name]
                           if (argJs==null) {
                              if (it.isOptional) null
                              else if (it.type.isMarkedNullable || it.type.isPlatformType) it to null
                              else fail { "Type=$instanceType constructor parameter=${it.name} is missing" }
                           } else {
                              val ptRaw = it.type
                              val pt = ptRaw.classifier?.asIf<KTypeParameter>()?.let { typeArgs[it.name] } ?: ptRaw
                              it to fromJsonValueImpl(pt, argJs)
                           }
                        }.toMap()

                        runTry {
                           constructor.isAccessible = true
                           constructor.callBy(arguments)
                        }.getOrSupply {
                           fail(it) { "Failed to instantiate $instanceType\nwith args:${arguments.mapKeys { it.key.name }}\nfrom:$value" }
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

fun JsValue.toCompactS(): String =
   tokens().joinToString(separator = "") { token ->
      when (token) {
         is JsTokenLiteral -> token.text
         is JsToken.Str -> token.value.js()
         is JsToken.Num -> when (token.value) {
            Double.POSITIVE_INFINITY -> "Infinity".js()
            Double.NEGATIVE_INFINITY -> "-Infinity".js()
            Double.NaN -> "NaN".js()
            Float.POSITIVE_INFINITY -> "Infinity".js()
            Float.NEGATIVE_INFINITY -> "-Infinity".js()
            Float.NaN -> "NaN".js()
            else -> token.value.toString()
         }
      }
   }

fun JsValue.toPrettyS(indent: String = "  ", newline: String = "\n"): String =
   buildString {
      var i = 0
      var wasCol = false
      var wasSta = false
      fun String.a() = append(this)
      fun lineAndIndent() { newline.a(); repeat(i) { indent.a() } }
      tokens().forEach { token ->
         val isCom = token is JsToken.Com || token is JsToken.Col
         val isCol = token is JsToken.Col
         val isSta = token == JsToken.Lbc || token == JsToken.Lbk
         val isEnd = token == JsToken.Rbc || token == JsToken.Rbk
         val isEmt = wasSta && isEnd
         if (isEnd) i--
         if (!isCom && !wasCol && !isEmt && isNotEmpty()) lineAndIndent()
         if (isSta) i++
         when (token) {
            is JsToken.Col -> { token.text.a(); " ".a() }
            is JsTokenLiteral -> token.text.a()
            is JsToken.Str -> token.value.js().a()
            is JsToken.Num -> when (token.value) {
               Double.POSITIVE_INFINITY -> "Infinity".js().a()
               Double.NEGATIVE_INFINITY -> "-Infinity".js().a()
               Double.NaN -> "NaN".js().a()
               Float.POSITIVE_INFINITY -> "Infinity".js().a()
               Float.NEGATIVE_INFINITY -> "-Infinity".js().a()
               Float.NaN -> "NaN".js().a()
               else -> token.value.toString().a()
            }
         }
         wasCol = isCol
         wasSta = isSta
      }
   }

private fun String.js() = "\"${this.escapeJson()}\""

@Suppress("UNCHECKED_CAST", "DEPRECATION")
private fun getEnumValue(enumClass: Class<*>, value: String): Enum<*> {
   val enumConstants = enumClass.enumConstants as Array<out Enum<*>>
   return enumConstants.first { it.name==value }
}