package sp.it.pl.core

import javafx.util.Duration
import org.atteo.evo.inflector.English
import sp.it.pl.audio.Song
import sp.it.pl.main.IconMA
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
import sp.it.util.file.type.MimeExt
import sp.it.util.file.type.MimeType
import sp.it.util.functional.Functors
import sp.it.util.functional.Parameter
import sp.it.util.functional.Util.IS0
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.text.Char16
import sp.it.util.text.Char32
import sp.it.util.text.StringSplitParser
import sp.it.util.text.Strings
import sp.it.util.text.char32At
import sp.it.util.text.isPalindrome
import sp.it.util.type.type
import sp.it.util.units.Bitrate
import sp.it.util.units.FileSize
import sp.it.util.units.NofX
import sp.it.util.units.RangeYear
import java.io.File
import java.math.BigDecimal
import java.math.BigInteger
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.Charset
import java.time.LocalDateTime
import java.time.Year
import java.util.Base64
import java.util.Objects
import java.util.regex.Pattern
import kotlin.text.Charsets.UTF_8

object CoreFunctors: Core {

   private val pool = Functors.pool

   @Suppress("LocalVariableName", "UNUSED_ANONYMOUS_PARAMETER")
   override fun init() {
      pool.apply {
         // Negated predicates are disabled, user interface should provide negation ability
         // or simply generate all the negations when needed (and reuse functors while at it).

         // Adding identity function here is impossible as its type is erased to Object -> Object
         // and we need its proper type X -> X (otherwise it erases type in function chains). Single
         // instance per class is required for Identity function.
         // Unfortunately:
         //     - we cant put it to functor pool, as we do not know which classes will need it
         //     - we cant put it to functor pool on requests, as the returned functors for class X
         //       return functors for X and all superclasses of X, which causes IDENTITY function
         //       to be inserted multiple times, even worse, only one of them has proper signature!
         //     - hence we cant even return Set to prevent duplicates, as the order of class iteration
         //       is undefined. In addition, functors are actually wrapped.
         // Solution is to insert the proper IDENTITY functor into results, after they were
         // collected. This guarantees single instance and correct signature. The downside is that
         // the functor pool does not contain IDENTITY functor at all, meaning the pool must never
         // be accessed directly. Additionally, question arises, whether IDENTITY functor should be
         // inserted when no functors are returned.
         //
         // add("As self", Object.class, Object.class, true, true, true, IDENTITY)

         // symbols can be found at https://www.alt-codes.net/math-symbols-list

         // assumption guarantees
         // We are making some assumptions by not declaring and letting compiler infer the reified type parameters
         failIf(p(StringSplitParser.singular()).type!=type<StringSplitParser>())
         failIf(p(Pattern.compile("")).type!=type<Pattern>())
         failIf(p(0).type!=type<Int>())

         // type aliases
         val B = Boolean::class.java
         val S = String::class.java

         // parameters
         val pNoCase = p("Ignore case", "Ignore case", true, IconConstraint(IconMA.FORMAT_SIZE))
         val pRegex = p("Regex", "Regular expression", Pattern.compile(""))

         add("Is null", Any::class.java, B, IS0)
         add("To String", Any::class.java, String::class.java) { Objects.toString(it) }
         add("To Boolean", S, B) { java.lang.Boolean.parseBoolean(it) }
         add("To Byte", S, Byte::class.java) { it.toByteOrNull() }
         add("To Short", S, Short::class.java) { it.toShortOrNull() }
         add("To Int", S, Int::class.java) { it.toIntOrNull() }
         add("To Long", S, Long::class.java) { it.toLongOrNull() }
         add("To Float", S, Float::class.java) { it.toFloatOrNull() }
         add("To Double", S, Double::class.java) { it.toDoubleOrNull() }
         add("To BigInteger", S, BigInteger::class.java) { it.toBigIntegerOrNull() }
         add("To BigDecimal", S, BigDecimal::class.java) { it.toBigDecimalOrNull() }

         add("Is true", B, B) { it }
         add("Is false", B, B) { !it }
         add("Negate", B, B) { !it }
         add("And", B, B, p(true)) { it, b -> java.lang.Boolean.logicalAnd(it, b) }
         add("Or", B, B, p(true)) { it, b -> java.lang.Boolean.logicalOr(it, b) }
         add("Xor", B, B, p(true)) { it, b -> java.lang.Boolean.logicalXor(it, b) }
         add("To Byte", B, Byte::class.java) { if (it) 1.toByte() else 0.toByte() }

         add("To Bin", Int::class.java, S) { "0b" + Integer.toBinaryString(it) }
         add("To Oct", Int::class.java, S) { "0" + Integer.toOctalString(it) }
         add("To Hex", Int::class.java, S) { "0x" + Integer.toHexString(it) }

         add("To upper case", S, S) { it.toUpperCase() }
         add("To lower case", S, S) { it.toLowerCase() }
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
         add("Re-encode", S, S, p<Charset>(UTF_8), p<Charset>(UTF_8)) { it, c1, c2 -> String(it.toByteArray(c1), c2) }
         add("Add text", S, S, p<String>(""), p<StringDirection>(FROM_START)) { it, added, from -> addText(it, added, from) }
         add("Remove chars", S, S, p(0), p<StringDirection>(FROM_START)) { it, amount, from -> removeChars(it, amount, from) }
         add("Retain chars", S, S, p(0), p<StringDirection>(FROM_START)) { it, amount, from -> retainChars(it, amount, from) }
         add("Trim", S, S) { it.trim() }
         add("Split", S, StringSplitParser.SplitData::class.java, p(StringSplitParser.singular())) { it, splitter -> split(it, splitter) }
         add("Split-join", S, S, p(StringSplitParser.singular()), p(StringSplitParser.singular())) { it, splitter, joiner -> splitJoin(it, splitter, joiner) }
         add("Is", S, B, p<String>(""), pNoCase) { it, phrase, noCase -> it.equals(phrase, noCase) }
         add("Contains", S, B, p<String>(""), pNoCase, false, false, true) { it, phrase, noCase -> it.contains(phrase, noCase) }
         add("Ends with", S, B, p<String>(""), pNoCase) { it, phrase, noCase -> it.endsWith(phrase, noCase) }
         add("Starts with", S, B, p<String>(""), pNoCase) { it, phrase, noCase -> it.startsWith(phrase, noCase) }
         add("Matches regex", S, B, pRegex) { it, r -> r.matcher(it).matches() }
         add("After", S, B, p<String>("")) { it, y -> it>y }
         add("Before", S, B, p<String>("")) { it, y -> it<y }
         add("Char at", S, Char32::class.java, p(0), p<StringDirection>(FROM_START)) { it, i, dir -> runTry { it.char32At(i, dir) }.orNull() }
         add("Length", S, Int::class.java, { it.length })
         add("Length >", S, B, p(0)) { it, l -> it.length>l }
         add("Length <", S, B, p(0)) { it, l -> it.length<l }
         add("Length =", S, B, p(0)) { it, l -> it.length==l }
         add("Is empty", S, B) { it.isEmpty() }
         add("Is palindrome", S, B) { it.isPalindrome() }
         add("To file", S, File::class.java) { File(it) }
         add("Is URI", S, B) { runTry { URI.create(it) }.isOk }
         add("To URI", S, URI::class.java) { runTry { URI.create(it) }.orNull() }
         add("Is base64", S, B) { runTry { String(Base64.getDecoder().decode(it.toByteArray())) }.isOk }
         add("Base64 encode", S, S) { Base64.getEncoder().encodeToString(it.toByteArray()) }
         add("Base64 decode", S, S) { runTry { String(Base64.getDecoder().decode(it.toByteArray())) }.orNull() }
         add("URL encode (UTF-8)", S, S) { URLEncoder.encode(it, UTF_8) }
         add("URL decode (UTF-8)", S, S) { URLDecoder.decode(it, UTF_8) }
         add("To valid file name", S, S) { filenamizeString(it) }
         add("Anime", S, S) { renameAnime(it) }

         add("Any contains", Strings::class.java, B, p<String>(""), pNoCase) { obj, text, noCase -> obj.anyContains(text, noCase) }
         add("Is empty", Strings::class.java, B) { it.isEmpty() }
         add("Elements", Strings::class.java, Int::class.java) { it.size() }

         add("To Int", Char16::class.java, Int::class.java) { it.toInt() }
         add("To Int", Char32::class.java, Int::class.java) { it.toInt() }

         add("Shortcut target", File::class.java, File::class.java) { WindowsShortcut.targetedFile(it).orElse(null) }
         add("Exists", File::class.java, B) { it.exists() }
         add("Rename anime", File::class.java, S) { renameAnime(it.nameWithoutExtension) }

         add("Group", MimeType::class.java, S) { it.group }
         add("Extensions", MimeType::class.java, S) { it.extensions.joinToString(", ") }
         add("Has extension", MimeType::class.java, B, p<String>("")) { it, ext -> it.hasExtension(ext) }

         add("Is", MimeExt::class.java, B, p(MimeExt("mp3"))) { it, y -> it==y }

         add("Less", Bitrate::class.java, B, p(Bitrate(320))) { it, y -> it<y }
         add("Is", Bitrate::class.java, B, p(Bitrate(320))) { it, y -> it==y }
         add("More", Bitrate::class.java, B, p(Bitrate(320))) { it, y -> it>y }
         add("Is good (≥${Bitrate(320).toUi()})", Bitrate::class.java, B) { it.value>=320 }
         add("Is bad (≤${Bitrate(128).toUi()})", Bitrate::class.java, B) { it.value<=128 }
         add("Is variable", Bitrate::class.java, B) { it.isVariable() }
         add("Is constant", Bitrate::class.java, B) { it.isConstant() }
         add("Is known", Bitrate::class.java, B) { !it.isUnknown() }

         add("< Less", Duration::class.java, B, p(Duration(0.0))) { it, y -> it<y }
         add("= Is", Duration::class.java, B, p(Duration(0.0))) { it, y -> it==y }
         add("> More", Duration::class.java, B, p(Duration(0.0)), false, false, true) { it, y -> it>y }

         add("< Less", NofX::class.java, B, p(NofX(1, 1))) { it, y -> it<y }
         add("= Is", NofX::class.java, B, p(NofX(1, 1))) { it, y -> it.compareTo(y)==0 }
         add("> More", NofX::class.java, B, p(NofX(1, 1)), false, false, true) { it, y -> it>y }
         add("≥ Not less", NofX::class.java, B, p(NofX(1, 1))) { it, y -> it>=y }
         add("≤ Not more", NofX::class.java, B, p(NofX(1, 1))) { it, y -> it<=y }

         add("< Less", FileSize::class.java, B, p(FileSize(0)), false, false, true) { it, y -> it<y }
         add("= Is", FileSize::class.java, B, p(FileSize(0))) { it, y -> it.compareTo(y)==0 }
         add("≅ Is approximately", FileSize::class.java, B, p(FileSize(FileSize.Mi))) { it, y -> it.inBytes().let { y.inBytes()/16<=it && it<=y.inBytes()*16 } }
         add("> More", FileSize::class.java, B, p(FileSize(0))) { it, y -> it>y }
         add("Is unknown", FileSize::class.java, B) { it.isUnknown() }
         add("Is known", FileSize::class.java, B) { it.isKnown() }
         add("In bytes", FileSize::class.java, Long::class.java) { it.inBytes() }

         add("Is after", Year::class.java, B, p(Year.now())) { it, y -> it>y }
         add("Is", Year::class.java, B, p(Year.now())) { it, y -> it.compareTo(y)==0 }
         add("Is before", Year::class.java, B, p(Year.now())) { it, y -> it<y }
         add("Is in the future", Year::class.java, B) { it>Year.now() }
         add("Is now", Year::class.java, B) { it.compareTo(Year.now())==0 }
         add("Is in the past", Year::class.java, B) { it<Year.now() }
         add("Is leap", Year::class.java, B, { it.isLeap })

         add("Contains year", RangeYear::class.java, B, p(Year.now())) { it, y -> it.contains(y) }
         add("Is after", RangeYear::class.java, B, p(Year.now())) { it, y -> it.isAfter(y) }
         add("Is before", RangeYear::class.java, B, p(Year.now())) { it, y -> it.isBefore(y) }
         add("Is in the future", RangeYear::class.java, B) { it.contains(Year.now()) }
         add("Is now", RangeYear::class.java, B) { it.isAfter(Year.now()) }
         add("Is in the past", RangeYear::class.java, B) { it.isBefore(Year.now()) }

         add("After", LocalDateTime::class.java, B, p(LocalDateTime.now())) { it, other -> it.isAfter(other) }
         add("Before", LocalDateTime::class.java, B, p(LocalDateTime.now())) { it, other -> it.isBefore(other) }
         add("Is", LocalDateTime::class.java, B, p(LocalDateTime.now())) { it, other -> it.isEqual(other) }

         add("File", Song::class.java, File::class.java) { it.getFile() }
         add("URI", Song::class.java, URI::class.java) { it.uri }

         addPredicatesComparable(Short::class.java, 0.toShort())
         addPredicatesComparable(Int::class.java, 0)
         addPredicatesComparable(Long::class.java, 0L)
         addPredicatesComparable(Double::class.java, 0.0)
         addPredicatesComparable(Float::class.java, 0f)
      }
   }

   private inline fun <reified TYPE> p(defaultValue: TYPE): Parameter<TYPE> =
      Parameter(type(), defaultValue)
   private inline fun <reified TYPE> p(name: String, description: String, defaultValue: TYPE, vararg constraints: Constraint<TYPE>): Parameter<TYPE> =
      Parameter(name, description, type(), defaultValue, constraints.toSet())
}