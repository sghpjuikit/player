package sp.it.pl.core

import com.github.f4b6a3.uuid.UuidCreator
import com.github.f4b6a3.uuid.enums.UuidLocalDomain
import com.github.f4b6a3.uuid.enums.UuidNamespace
import java.io.File
import java.math.BigDecimal
import java.math.BigInteger
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.Charset
import java.time.LocalDateTime
import java.time.Year
import java.util.Objects
import java.util.UUID
import java.util.regex.Pattern
import javafx.util.Duration
import kotlin.reflect.KClass
import kotlin.text.Charsets.UTF_8
import org.atteo.evo.inflector.English
import sp.it.pl.audio.Song
import sp.it.pl.main.APP
import sp.it.pl.main.IconMA
import sp.it.pl.main.detectContent
import sp.it.pl.main.toS
import sp.it.pl.main.toUi
import sp.it.util.Util.StringDirection
import sp.it.util.Util.StringDirection.FROM_START
import sp.it.util.Util.addText
import sp.it.util.Util.filenamizeString
import sp.it.util.Util.remove1st
import sp.it.util.Util.removeAll
import sp.it.util.Util.removeAllRegex
import sp.it.util.Util.removeChars
import sp.it.util.Util.renameAnime
import sp.it.util.Util.replace1st
import sp.it.util.Util.replaceAll
import sp.it.util.Util.replaceAllRegex
import sp.it.util.Util.retainChars
import sp.it.util.Util.split
import sp.it.util.Util.splitJoin
import sp.it.util.conf.Constraint
import sp.it.util.conf.Constraint.IconConstraint
import sp.it.util.dev.failIf
import sp.it.util.file.WindowsShortcut
import sp.it.util.file.json.JsValue
import sp.it.util.file.json.toCompactS
import sp.it.util.file.json.toPrettyS
import sp.it.util.file.type.MimeExt
import sp.it.util.file.type.MimeType
import sp.it.util.functional.Functors
import sp.it.util.functional.Parameter
import sp.it.util.functional.Try
import sp.it.util.functional.Util.IS0
import sp.it.util.functional.getOrSupply
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.math.StrExF
import sp.it.util.text.Char16
import sp.it.util.text.Char32
import sp.it.util.text.Grapheme
import sp.it.util.text.StringSplitParser
import sp.it.util.text.Strings
import sp.it.util.text.char32At
import sp.it.util.text.chars16
import sp.it.util.text.chars32
import sp.it.util.text.decodeBase64
import sp.it.util.text.encodeBase64
import sp.it.util.text.escapeCsv
import sp.it.util.text.escapeEcmaScript
import sp.it.util.text.escapeHtml3
import sp.it.util.text.escapeHtml4
import sp.it.util.text.escapeJava
import sp.it.util.text.escapeJson
import sp.it.util.text.escapeXSI
import sp.it.util.text.escapeXml10
import sp.it.util.text.escapeXml11
import sp.it.util.text.graphemes
import sp.it.util.text.isBase64
import sp.it.util.text.isPalindrome
import sp.it.util.text.lengthInCodePoints
import sp.it.util.text.sentences
import sp.it.util.text.toChar32
import sp.it.util.text.toPrintableNonWhitespace
import sp.it.util.text.unescapeCsv
import sp.it.util.text.unescapeEcmaScript
import sp.it.util.text.unescapeHtml3
import sp.it.util.text.unescapeHtml4
import sp.it.util.text.unescapeJava
import sp.it.util.text.unescapeJson
import sp.it.util.text.unescapeXSI
import sp.it.util.text.unescapeXml
import sp.it.util.text.words
import sp.it.util.type.VType
import sp.it.util.type.estimateRuntimeType
import sp.it.util.type.type
import sp.it.util.type.withPlatformTypeNullability
import sp.it.util.units.Bitrate
import sp.it.util.units.FileSize
import sp.it.util.units.NofX
import sp.it.util.units.RangeYear
import sp.it.util.units.uri

object CoreFunctors: Core {

   private val pool = Functors.pool

   @Suppress("LocalVariableName", "UNUSED_ANONYMOUS_PARAMETER", "RemoveExplicitTypeArguments")
   override fun init() {
      pool.apply {
         // Negated predicates are disabled, user interface should provide negation ability
         // or simply generate all the negations when needed (and reuse functors while at it).

         // Adding identity function here is impossible as its type is erased to Object -> Object,
         // and we need its proper type X -> X (otherwise it erases type in function chains). Single
         // instance per class is required for Identity function.
         // Unfortunately:
         //     - we can't put it to functor pool, as we do not know which classes will need it
         //     - we can't put it to functor pool on requests, as the returned functors for class X
         //       return functors for X and all superclasses of X, which causes IDENTITY function
         //       to be inserted multiple times, even worse, only one of them has proper signature!
         //     - hence we can't even return Set to prevent duplicates, as the order of class iteration
         //       is undefined. In addition, functors are actually wrapped.
         // Solution is to insert the proper IDENTITY functor into results, after they were
         // collected. This will guarantee single instance and correct signature. The downside is that
         // the functor pool does not contain IDENTITY functor at all, meaning the pool must never
         // be accessed directly. Additionally, question arises, whether IDENTITY functor should be
         // inserted when no functors are returned.
         //
         // add("As self", Object.class, Object.class, true, true, true, IDENTITY)

         // symbols can be found at https://www.alt-codes.net/math-symbols-list

         // assumption guarantees
         failIf(p(StringSplitParser.singular()).type!=type<StringSplitParser>())
         failIf(p(Pattern.compile("")).type!=type<Pattern>())
         failIf(p(0).type!=type<Int>())

         // type aliases
         val U = type<Unit>()
         val B = type<Boolean>()
         val S = type<String>()

         // parameters
         val pNoCase = p("Ignore case", "Ignore case", true, IconConstraint(IconMA.FORMAT_SIZE))
         val pRegex = p("Regex", "Regular expression", Pattern.compile(""))

         add("Is null", type<Any?>(), B, IS0)
         add("Class (Kotlin)", type<Any?>(), type<KClass<*>>()) { if (it==null) Nothing::class else it::class }
         add("Class (Java)", type<Any?>(), type<Class<*>>()) { if (it==null) Void::class.java else it::class.java }
         add("Is", type<Class<*>>(), B, p<Class<*>>(Unit::class.java)) { it, c -> it===c }
         add("Is", type<KClass<*>>(), B, p<KClass<*>>(Unit::class)) { it, c -> it===c }
         add("Type", type<Any?>(), type<VType<Any?>>()) { it.estimateRuntimeType() }
         add("To String", type<Any?>(), S) { Objects.toString(it) }
         add("To application UI text", type<Any?>(), S) { it.toUi() }
         add("To application DB text", type<Any?>(), S) { it.toS() }
         add("To best content", type<Any?>(), type<Any?>()) { it.detectContent() }
         add("Detect content", type<Any?>(), type<Any?>()) { it.detectContent() }

         add("To Boolean", S, type<Boolean?>()) { when (it) { "true" -> true "false" -> false else -> null } }
         add("To Byte", S, type<Byte?>()) { it.toByteOrNull() }
         add("To UByte", S, type<UByte?>()) { it.toUByteOrNull() }
         add("To Short", S, type<Short?>()) { it.toShortOrNull() }
         add("To UShort", S, type<UShort?>()) { it.toUShortOrNull() }
         add("To Int", S, type<Int?>()) { it.toIntOrNull() }
         add("To UInt", S, type<UInt?>()) { it.toUIntOrNull() }
         add("To Long", S, type<Long?>()) { it.toLongOrNull() }
         add("To ULong", S, type<ULong?>()) { it.toULongOrNull() }
         add("To Float", S, type<Float?>()) { it.toFloatOrNull() }
         add("To Double", S, type<Double?>()) { it.toDoubleOrNull() }
         add("To BigInteger", S, type<BigInteger?>()) { it.toBigIntegerOrNull() }
         add("To BigDecimal", S, type<BigDecimal?>()) { it.toBigDecimalOrNull() }

         add("Uuid 1", U, type<UUID>()) { UuidCreator.getTimeBased() }
         add("Uuid 2", U, type<UUID>(), p(UuidLocalDomain.LOCAL_DOMAIN_PERSON), p(1701)) { u, ld, id -> UuidCreator.getDceSecurity(ld, id) }
         add("Uuid 3", U, type<UUID>(), p<UuidNamespace?>(null), p("")) { u, ns, name -> UuidCreator.getNameBasedMd5(ns, name) }
         add("Uuid 4", U, type<UUID>()) { UuidCreator.getRandomBased() }
         add("Uuid 5", U, type<UUID>(), p<UuidNamespace?>(null), p("")) { u, ns, name -> UuidCreator.getNameBasedSha1(ns, name) }
         add("Uuid 6 (mac node id)", U, type<UUID>()) { UuidCreator.getTimeOrderedWithMac() }
         add("Uuid 6 (hash node id)", U, type<UUID>()) { UuidCreator.getTimeOrderedWithHash() }
         add("Uuid 6 (random node id)", U, type<UUID>()) { UuidCreator.getTimeOrderedWithRandom() }
         add("Random Boolean", U, type<Boolean>()) { Math.random()>0.5 }
         add("Random Byte", U, type<Byte>()) { (Math.random()*Byte.MAX_VALUE).toInt().toByte() }
         add("Random Short", U, type<Short>()) { (Math.random()*Short.MAX_VALUE).toInt().toShort() }
         add("Random Int", U, type<Int>()) { (Math.random()*Int.MAX_VALUE).toInt() }
         add("Random Long", U, type<Long>()) { (Math.random()*Long.MAX_VALUE).toLong() }
         add("Random Float", U, type<Float>()) { (Math.random()*Float.MAX_VALUE).toFloat() }
         add("Random Double", U, type<Double>()) { Math.random()*Double.MAX_VALUE }

         add("Is true", B, B) { it }
         add("Is false", B, B) { !it }
         add("Negate", B, B) { !it }
         add("And", B, B, p(true)) { it, b -> java.lang.Boolean.logicalAnd(it, b) }
         add("Or", B, B, p(true)) { it, b -> java.lang.Boolean.logicalOr(it, b) }
         add("Xor", B, B, p(true)) { it, b -> java.lang.Boolean.logicalXor(it, b) }
         add("To Byte", B, type<Byte>()) { if (it) 1.toByte() else 0.toByte() }

         add("To Bin", type<Byte>(), S) { "0b" + it.toString(2) }
         add("To Oct", type<Byte>(), S) { "0" + it.toString(8) }
         add("To Hex", type<Byte>(), S) { "0x" + it.toString(16) }
         add("To Bin", type<UByte>(), S) { "0b" + it.toString(2) }
         add("To Oct", type<UByte>(), S) { "0" + it.toString(8) }
         add("To Hex", type<UByte>(), S) { "0x" + it.toString(16) }
         add("To Bin", type<Short>(), S) { "0b" + it.toString(2) }
         add("To Oct", type<Short>(), S) { "0" + it.toString(8) }
         add("To Hex", type<Short>(), S) { "0x" + it.toString(16) }
         add("To Bin", type<UShort>(), S) { "0b" + it.toString(2) }
         add("To Oct", type<UShort>(), S) { "0" + it.toString(8) }
         add("To Hex", type<UShort>(), S) { "0x" + it.toString(16) }
         add("To Bin", type<Int>(), S) { "0b" + it.toString(2) }
         add("To Oct", type<Int>(), S) { "0" + it.toString(8) }
         add("To Hex", type<Int>(), S) { "0x" + it.toString(16) }
         add("To Bin", type<UInt>(), S) { "0b" + it.toString(2) }
         add("To Oct", type<UInt>(), S) { "0" + it.toString(8) }
         add("To Hex", type<UInt>(), S) { "0x" + it.toString(16) }
         add("To Bin", type<Long>(), S) { "0b" + it.toString(2) }
         add("To Oct", type<Long>(), S) { "0" + it.toString(8) }
         add("To Hex", type<Long>(), S) { "0x" + it.toString(16) }
         add("To Bin", type<ULong>(), S) { "0b" + it.toString(2) }
         add("To Oct", type<ULong>(), S) { "0" + it.toString(8) }
         add("To Hex", type<ULong>(), S) { "0x" + it.toString(16) }
         add("To Bin", type<BigInteger>(), S) { "0b" + it.toString(2) }
         add("To Oct", type<BigInteger>(), S) { "0" + it.toString(8) }
         add("To Hex", type<BigInteger>(), S) { "0x" + it.toString(16) }
         add("To Bin", type<List<Byte>>(), S) { it.map { "0b" + it.toString(2) } }
         add("To Oct", type<List<Byte>>(), S) { it.map { "0" + it.toString(8) } }
         add("To Hex", type<List<Byte>>(), S) { it.map { "0x" + it.toString(16) } }
         add("To Bin", type<List<UByte>>(), S) { it.map { "0b" + it.toString(2) } }
         add("To Oct", type<List<UByte>>(), S) { it.map { "0" + it.toString(8) } }
         add("To Hex", type<List<UByte>>(), S) { it.map { "0x" + it.toString(16) } }
         add("To Bin", type<List<Short>>(), S) { it.map { "0b" + it.toString(2) } }
         add("To Oct", type<List<Short>>(), S) { it.map { "0" + it.toString(8) } }
         add("To Hex", type<List<Short>>(), S) { it.map { "0x" + it.toString(16) } }
         add("To Bin", type<List<UShort>>(), S) { it.map { "0b" + it.toString(2) } }
         add("To Oct", type<List<UShort>>(), S) { it.map { "0" + it.toString(8) } }
         add("To Hex", type<List<UShort>>(), S) { it.map { "0x" + it.toString(16) } }
         add("To Bin", type<List<Int>>(), S) { it.map { "0b" + it.toString(2) } }
         add("To Oct", type<List<Int>>(), S) { it.map { "0" + it.toString(8) } }
         add("To Hex", type<List<Int>>(), S) { it.map { "0x" + it.toString(16) } }
         add("To Bin", type<List<UInt>>(), S) { it.map { "0b" + it.toString(2) } }
         add("To Oct", type<List<UInt>>(), S) { it.map { "0" + it.toString(8) } }
         add("To Hex", type<List<UInt>>(), S) { it.map { "0x" + it.toString(16) } }
         add("To Bin", type<List<Long>>(), S) { it.map { "0b" + it.toString(2) } }
         add("To Oct", type<List<Long>>(), S) { it.map { "0" + it.toString(8) } }
         add("To Hex", type<List<Long>>(), S) { it.map { "0x" + it.toString(16) } }
         add("To Bin", type<List<ULong>>(), S) { it.map { "0b" + it.toString(2) } }
         add("To Oct", type<List<ULong>>(), S) { it.map { "0" + it.toString(8) } }
         add("To Hex", type<List<ULong>>(), S) { it.map { "0x" + it.toString(16) } }
         add("To Bin", type<List<BigInteger>>(), S) { it.map { "0b" + it.toString(2) } }
         add("To Oct", type<List<BigInteger>>(), S) { it.map { "0" + it.toString(8) } }
         add("To Hex", type<List<BigInteger>>(), S) { it.map { "0x" + it.toString(16) } }

         add("Function", type<Number>(), type<BigDecimal>(), p<StrExF>(StrExF("x"))) { it, f ->
            runTry {
               val x = when(it) { is BigDecimal -> it; is BigInteger -> BigDecimal(it); else -> it.toDouble().toBigDecimal() }
               f(x)
            }.getOrSupply { Double.NaN }
         }

         add("To Char (16)", type<Int>(), type<Char16?>()) { runTry { Char(it)}.orNull() }
         add("To Char (32)", type<Int>(), type<Char32?>()) { runTry { Char32(it)}.orNull() }
         add("To Char (32)", type<Char16>(), type<Char32>()) { Char32(it.code) }
         add("To Int", type<Char16>(), type<Int>()) { it.code }
         add("To Int", type<Char32>(), type<Int>()) { it.value }
         add("View non-printable", type<Char16>(), type<Char32>()) { it.toChar32().toPrintableNonWhitespace() }
         add("View non-printable", type<Char32>(), type<Char32>()) { it.toPrintableNonWhitespace() }

         add("To upper case", S, S) { it.uppercase() }
         add("To lower case", S, S) { it.lowercase() }
         add("Plural", S, S) { English.plural(it) }
         add("Is regex", S, B) { runTry { Pattern.compile(it) }.isOk }
         add("Replace 1st (regex)", S, S, pRegex, p<String>("")) { it, regex, n -> replace1st(it, regex, n) }
         add("Remove 1st (regex)", S, S, pRegex) { it, regex -> remove1st(it, regex) }
         add("Replace all", S, S, p<String>(""), p<String>("")) { it, phrase, with -> replaceAll(it, phrase, with) }
         add("Replace all (regex)", S, S, pRegex, p<String>("")) { it, regex, with -> replaceAllRegex(it, regex, with) }
         add("Replace '_' with ' '", S, S) { it.replace("_", " ") }
         add("Remove all", S, S, p<String>("")) { it, phrase -> removeAll(it, phrase) }
         add("Remove all (regex)", S, S, pRegex) { it, regex -> removeAllRegex(it, regex) }
         add("Text", S, S, p<String>("text")) { it, text -> text }
         add("Add text", S, S, p<String>(""), p<StringDirection>(FROM_START)) { it, added, from -> addText(it, added, from) }
         add("Remove chars", S, S, p(0), p<StringDirection>(FROM_START)) { it, amount, from -> removeChars(it, amount, from) }
         add("Retain chars", S, S, p(0), p<StringDirection>(FROM_START)) { it, amount, from -> retainChars(it, amount, from) }
         add("Trim", S, S) { it.trim() }
         add("Split to bytes", S, type<List<UByte>>(), p<Charset>(UTF_8)) { it, c -> it.toByteArray(c).toList().map { it.toUByte() } }
         add("Split to chars (16)", S, type<List<Char16>>()) { it.chars16().toList() }
         add("Split to chars (32)", S, type<List<Char32>>()) { it.chars32().toList() }
         add("Split to graphemes", S, type<List<Grapheme>>()) { it.graphemes().toList() }
         add("Split to words", S, type<List<String>>()) { it.words().map { it.trim() }.filter { it.isNotEmpty() }.toList() }
         add("Split to lines", S, type<List<String>>()) { it.lines() }
         add("Split to sentences", S, type<List<String>>()) { it.sentences().map { it.trim() }.filter { it.isNotEmpty() }.toList() }
         add("Split", S, type<List<String>>(), p(" ")) { it, d -> it.split(d) }
         add("Split*", S, type<StringSplitParser.SplitData>(), p(StringSplitParser.singular())) { it, splitter -> split(it, splitter) }
         add("Split*Join", S, S, p(StringSplitParser.singular()), p(StringSplitParser.singular())) { it, splitter, joiner -> splitJoin(it, splitter, joiner) }
         add("Is", S, B, p<String>(""), pNoCase) { it, phrase, noCase -> it.equals(phrase, noCase) }
         add("Contains", S, B, p<String>(""), pNoCase, false, false, true) { it, phrase, noCase -> it.contains(phrase, noCase) }
         add("Ends with", S, B, p<String>(""), pNoCase) { it, phrase, noCase -> it.endsWith(phrase, noCase) }
         add("Starts with", S, B, p<String>(""), pNoCase) { it, phrase, noCase -> it.startsWith(phrase, noCase) }
         add("Matches regex", S, B, pRegex) { it, r -> r.matcher(it).matches() }
         add("After", S, B, p<String>("")) { it, y -> it>y }
         add("Before", S, B, p<String>("")) { it, y -> it<y }
         add("Char at", S, type<Char32>(), p(0), p<StringDirection>(FROM_START)) { it, i, dir -> runTry { it.char32At(i, dir) }.orNull() }
         add("View non-printable", S, S) { it.chars32().map { it.toPrintableNonWhitespace().toString() }.joinToString("") }
         add("Length", S, type<Int>()) { it.lengthInCodePoints }
         add("Is empty", S, B) { it.isEmpty() }
         add("Is palindrome", S, B) { it.isPalindrome() }
         add("To file", S, type<File>()) { File(it) }
         add("Is URI", S, B) { runTry { uri(it) }.isOk }
         add("To URI", S, type<URI>()) { runTry { uri(it) }.orNull() }
         add("Is base64", S, B) { it.isBase64() }
         add("Encode in", S, S, p<Charset>(UTF_8), p<Charset>(UTF_8)) { it, c1, c2 -> String(it.toByteArray(c1), c2) }
         add("Encode Base64", S, S) { it.encodeBase64() }
         add("Decode Base64", S, S) { it.decodeBase64().orNull() }
         add("Encode URL (UTF-8)", S, S) { URLEncoder.encode(it, UTF_8) }
         add("Decode URL (UTF-8)", S, S) { URLDecoder.decode(it, UTF_8) }
         add("Escape Java", S, S) { it.escapeJava() }
         add("Unescape Java", S, S) { it.unescapeJava() }
         add("Escape EcmaScript", S, S) { it.escapeEcmaScript() }
         add("Unescape EcmaScript", S, S) { it.unescapeEcmaScript() }
         add("Escape Json", S, S) { it.escapeJson() }
         add("Unescape Json", S, S) { it.unescapeJson() }
         add("Escape Html 3.0", S, S) { it.escapeHtml3() }
         add("Unescape Html 3.0", S, S) { it.unescapeHtml3() }
         add("Escape Html 4.0", S, S) { it.escapeHtml4() }
         add("Unescape Html 4.0", S, S) { it.unescapeHtml4() }
         add("Escape Xml 1.0", S, S) { it.escapeXml10() }
         add("Escape Xml 1.1", S, S) { it.escapeXml11() }
         add("Unescape Xml", S, S) { it.unescapeXml() }
         add("Escape Csv", S, S) { it.escapeCsv() }
         add("Unescape Csv", S, S) { it.unescapeCsv() }
         add("Escape XSI", S, S) { it.escapeXSI() }
         add("Unescape XSI", S, S) { it.unescapeXSI() }
         add("To valid file name", S, S) { filenamizeString(it) }
         add("Anime", S, S) { renameAnime(it) }

         add("To Json", S, type<Try<JsValue, Throwable>>()) { APP.serializerJson.json.ast(it) }
         add("Format to pretty json", type<JsValue>(), S) { it.toPrettyS() }
         add("Format to compact json", type<JsValue>(), S) { it.toCompactS() }

         add("Any contains", type<Strings>(), B, p<String>(""), pNoCase) { obj, text, noCase -> obj.anyContains(text, noCase) }
         add("Is empty", type<Strings>(), B) { it.isEmpty() }
         add("Elements", type<Strings>(), type<Int>()) { it.size() }

         add("To Int", type<Char16>(), type<Int>()) { it.code }
         add("To Int", type<Char32>(), type<Int>()) { it.toInt() }

         add("Shortcut target", type<File>(), type<File>()) { WindowsShortcut.targetedFile(it).orElse(null) }
         add("Exists", type<File>(), B) { it.exists() }
         add("Rename anime", type<File>(), S) { renameAnime(it.nameWithoutExtension) }

         add("Group", type<MimeType>(), S) { it.group }
         add("Extensions", type<MimeType>(), S) { it.extensions.joinToString(", ") }
         add("Has extension", type<MimeType>(), B, p<String>("")) { it, ext -> it.hasExtension(ext) }

         add("Is", type<MimeExt>(), B, p(MimeExt("mp3"))) { it, y -> it==y }

         add("Less", type<Bitrate>(), B, p(Bitrate(320))) { it, y -> it<y }
         add("Is", type<Bitrate>(), B, p(Bitrate(320))) { it, y -> it==y }
         add("More", type<Bitrate>(), B, p(Bitrate(320))) { it, y -> it>y }
         add("Is good (≥${Bitrate(320).toUi()})", type<Bitrate>(), B) { it.value>=320 }
         add("Is bad (≤${Bitrate(128).toUi()})", type<Bitrate>(), B) { it.value<=128 }
         add("Is variable", type<Bitrate>(), B) { it.isVariable() }
         add("Is constant", type<Bitrate>(), B) { it.isConstant() }
         add("Is known", type<Bitrate>(), B) { !it.isUnknown() }

         add("< Less", type<Duration>(), B, p(Duration(0.0))) { it, y -> it<y }
         add("= Is", type<Duration>(), B, p(Duration(0.0))) { it, y -> it==y }
         add("> More", type<Duration>(), B, p(Duration(0.0)), false, false, true) { it, y -> it>y }

         add("< Less", type<NofX>(), B, p(NofX(1, 1))) { it, y -> it<y }
         add("= Is", type<NofX>(), B, p(NofX(1, 1))) { it, y -> (it compareTo y)==0 }
         add("> More", type<NofX>(), B, p(NofX(1, 1)), false, false, true) { it, y -> it>y }
         add("≥ Not less", type<NofX>(), B, p(NofX(1, 1))) { it, y -> it>=y }
         add("≤ Not more", type<NofX>(), B, p(NofX(1, 1))) { it, y -> it<=y }

         add("< Less", type<FileSize>(), B, p(FileSize(0)), false, false, true) { it, y -> it<y }
         add("= Is", type<FileSize>(), B, p(FileSize(0))) { it, y -> (it compareTo y)==0 }
         add("≅ Is approximately", type<FileSize>(), B, p(FileSize(FileSize.Mi))) { it, y -> it.inBytes().let { y.inBytes()/16<=it && it<=y.inBytes()*16 } }
         add("> More", type<FileSize>(), B, p(FileSize(0))) { it, y -> it>y }
         add("Is unknown", type<FileSize>(), B) { it.isUnknown() }
         add("Is known", type<FileSize>(), B) { it.isKnown() }
         add("In bytes", type<FileSize>(), type<Long>()) { it.inBytes() }

         add("Is after", type<Year>(), B, p(Year.now())) { it, y -> it>y }
         add("Is", type<Year>(), B, p(Year.now())) { it, y -> (it compareTo y)==0 }
         add("Is before", type<Year>(), B, p(Year.now())) { it, y -> it<y }
         add("Is in the future", type<Year>(), B) { it>Year.now() }
         add("Is now", type<Year>(), B) { (it compareTo Year.now())==0 }
         add("Is in the past", type<Year>(), B) { it<Year.now() }
         add("Is leap", type<Year>(), B) { it.isLeap }

         add("Contains year", type<RangeYear>(), B, p(Year.now())) { it, y -> it.contains(y) }
         add("Is after", type<RangeYear>(), B, p(Year.now())) { it, y -> it.isAfter(y) }
         add("Is before", type<RangeYear>(), B, p(Year.now())) { it, y -> it.isBefore(y) }
         add("Is in the future", type<RangeYear>(), B) { it.contains(Year.now()) }
         add("Is now", type<RangeYear>(), B) { it.isAfter(Year.now()) }
         add("Is in the past", type<RangeYear>(), B) { it.isBefore(Year.now()) }

         add("After", type<LocalDateTime>(), B, p(LocalDateTime.now())) { it, other -> it.isAfter(other) }
         add("Before", type<LocalDateTime>(), B, p(LocalDateTime.now())) { it, other -> it.isBefore(other) }
         add("Is", type<LocalDateTime>(), B, p(LocalDateTime.now())) { it, other -> it.isEqual(other) }

         add("File", type<Song>(), type<File>()) { it.getFile() }
         add("URI", type<Song>(), type<URI>()) { it.uri }

         add("Size", type<Collection<*>>(), type<Int>()) { it.size }
         add("Is empty", type<Collection<*>>(), type<Boolean>()) { it.isEmpty() }
         add("Element at", type<List<*>>(), type<Any?>(), p<Int>(0)) { it, i -> it[i] }
         add("Distinct", type<Iterable<*>>(), type<Set<*>>()) { it.toHashSet() }
         add("Histogram", type<Iterable<*>>(), type<Map<*, Int>>()) { it.groupBy { it }.mapValues { (_,v) -> v.size } }
         add("Size", type<Map<*, *>>(), type<Int>()) { it.size }
         add("Is empty", type<Map<*, *>>(), type<Boolean>()) { it.isEmpty() }

         addComparisons(type<Byte>(), 0.toByte())
         addComparisons(type<UByte>(), 0.toUByte())
         addComparisons(type<Short>(), 0.toShort())
         addComparisons(type<UShort>(), 0.toUShort())
         addComparisons(type<Int>(), 0)
         addComparisons(type<UInt>(), 0.toUInt())
         addComparisons(type<Float>(), 0f)
         addComparisons(type<Long>(), 0L)
         addComparisons(type<ULong>(), 0L.toULong())
         addComparisons(type<Double>(), 0.0)

         // Else function should be of dynamic type and just one, but then we can't provide default values for parameters,
         // theses are, for now, mandatory
         add("else", type<String?>(), type<String>(), p("")) { it, or -> it ?: or }
         add("else", type<Byte?>(), type<Byte>(), p(0.toByte())) { it, or -> it ?: or }
         add("else", type<Short?>(), type<Short>(), p(0.toShort())) { it, or -> it ?: or }
         add("else", type<Int?>(), type<Int>(), p(0)) { it, or -> it ?: or }
         add("else", type<Long?>(), type<Long>(), p(0.toLong())) { it, or -> it ?: or }
         add("else", type<BigInteger?>(), type<BigInteger>(), p(0.toBigInteger())) { it, or -> it ?: or }
         add("else", type<Float?>(), type<Float>(), p(0.toFloat())) { it, or -> it ?: or }
         add("else", type<Double?>(), type<Double>(), p(0.toDouble())) { it, or -> it ?: or }
         add("else", type<BigDecimal?>(), type<BigDecimal>(), p(0.toBigDecimal())) { it, or -> it ?: or }
      }
   }

   private inline fun <reified TYPE> p(defaultValue: TYPE): Parameter<TYPE> =
      Parameter(VType(type<TYPE>().type.withPlatformTypeNullability(false)), defaultValue)
   private inline fun <reified TYPE> p(name: String, description: String, defaultValue: TYPE, vararg constraints: Constraint<TYPE>): Parameter<TYPE> =
      Parameter(name, description, type(), defaultValue, constraints.toSet())
}