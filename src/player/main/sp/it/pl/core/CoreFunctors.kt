package sp.it.pl.core

import javafx.util.Duration
import org.atteo.evo.inflector.English
import sp.it.pl.audio.Song
import sp.it.util.Util.StringDirection
import sp.it.util.Util.StringDirection.FROM_START
import sp.it.util.Util.addText
import sp.it.util.Util.charAt
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
import sp.it.util.file.FileType
import sp.it.util.file.WindowsShortcut
import sp.it.util.file.nameOrRoot
import sp.it.util.file.nameWithoutExtensionOrRoot
import sp.it.util.file.type.MimeExt
import sp.it.util.file.type.MimeType
import sp.it.util.file.type.mimeType
import sp.it.util.functional.Functors
import sp.it.util.functional.Util.ISØ
import sp.it.util.text.StringSplitParser
import sp.it.util.text.Strings
import sp.it.util.text.isPalindrome
import sp.it.util.units.Bitrate
import sp.it.util.units.FileSize
import sp.it.util.units.NofX
import sp.it.util.units.RangeYear
import java.io.File
import java.net.URI
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
         /*
          * (1) Negated predicates are disabled, user interface should provide negation ability
          * or simply generate all the negations when needed (and reuse functors while at it).
          *
          * (2) Adding identity function here is impossible as its type is erased to Object -> Object
          * and we need its proper type X -> X (otherwise it erases type in function chains). Single
          * instance per class is required for Identity function.
          * Unfortunately:
          *     - we cant put it to functor pool, as we do not know which classes will need it
          *     - we cant put it to functor pool on requests, as the returned functors for class X
          *       return functors for X and all superclasses of X, which causes IDENTITY function
          *       to be inserted multiple times, even worse, only one of them has proper signature!
          *     - hence we cant even return Set to prevent duplicates, as the order of class iteration
          *       is undefined. In addition, functors are actually wrapped.
          * Solution is to insert the proper IDENTITY functor into results, after they were
          * collected. This guarantees single instance and correct signature. The downside is that
          * the functor pool does not contain IDENTITY functor at all, meaning the pool must never
          * be accessed directly. Additionally, question arises, whether IDENTITY functor should be
          * inserted when no functors are returned.
          *
          * add("As self",      Object.class, Object.class, IDENTITY, true, true, true);
          */

         val B = Boolean::class.java
         val S = String::class.java

         add("Is null", Any::class.java, B, ISØ)
         //  add("Isn't null", Object.class, BOOL, ISNTØ);
         add("As String", Any::class.java, String::class.java) { Objects.toString(it) }
         add("As Boolean", String::class.java, B) { java.lang.Boolean.parseBoolean(it) }

         add("Is true", B, B) { it }
         add("Is false", B, B) { !it }
         add("Negate", B, B) { b -> !b }
         add("And", B, B, { a, b -> java.lang.Boolean.logicalAnd(a, b) }, B, true)
         add("Or", B, B, { a, b -> java.lang.Boolean.logicalOr(a, b) }, B, true)
         add("Xor", B, B, { a, b -> java.lang.Boolean.logicalXor(a, b) }, B, true)

         add("'_' -> ' '", S, S) { it.replace("_", " ") }
         add("-> file name", S, S) { filenamizeString(it) }
         add("Anime", S, S) { renameAnime(it) }
         add("To upper case", S, S) { it.toUpperCase() }
         add("To lower case", S, S) { it.toLowerCase() }
         add("Plural", S, S) { English.plural(it) }
         add("Replace 1st (regex)", S, S, { s, regex, n -> replace1st(s, regex, n) }, Pattern::class.java, S, Pattern.compile(""), "")
         add("Remove 1st (regex)", S, S, { text, regex -> remove1st(text, regex) }, Pattern::class.java, Pattern.compile(""))
         add("Replace all", S, S, { text, phrase, with -> replaceAll(text, phrase, with) }, S, S, "", "")
         add("Replace all (regex)", S, S, { text, regex, with -> replaceAllRegex(text, regex, with) }, Pattern::class.java, S, Pattern.compile(""), "")
         add("Remove all", S, S, { text, phrase -> removeAll(text, phrase) }, S, "")
         add("Remove all (regex)", S, S, { text, regex -> removeAllRegex(text, regex) }, Pattern::class.java, Pattern.compile(""))
         add("Text", S, S, { s, r -> r }, S, "")
         add("Re-encode", S, S, { s, c1, c2 -> String(s.toByteArray(c1), c2) }, Charset::class.java, Charset::class.java, UTF_8, UTF_8)
         add("Add text", S, S, { text, added, from -> addText(text, added, from) }, S, StringDirection::class.java, "", FROM_START)
         add("Remove chars", S, S, { text, amount, from -> removeChars(text, amount, from) }, Int::class.java, StringDirection::class.java, 0, FROM_START)
         add("Retain chars", S, S, { text, amount, from -> retainChars(text, amount, from) }, Int::class.java, StringDirection::class.java, 0, FROM_START)
         add("Trim", S, S, { it.trim() })
         add("Split", S, StringSplitParser.SplitData::class.java, { text, splitter -> split(text, splitter) }, StringSplitParser::class.java, StringSplitParser.singular())
         add("Split-join", S, S, { t, splitter, joiner -> splitJoin(t, splitter, joiner) }, StringSplitParser::class.java, StringSplitParser::class.java, StringSplitParser.singular(), StringSplitParser.singular())
         add("Is", S, B, { text, phrase, ignore -> text.equals(phrase, ignore) }, S, B, "", true)
         add("Contains", S, B, { text, phrase, ignore -> text.contains(phrase, ignore) }, S, B, "", true, false, false, true)
         add("Ends with", S, B, { text, phrase, ignore -> text.endsWith(phrase, ignore) }, S, B, "", true)
         add("Starts with", S, B, { text, phrase, ignore -> text.startsWith(phrase, ignore) }, S, B, "", true)
         add("Matches regex", S, B, { text, r -> r.matcher(text).matches() }, Pattern::class.java, Pattern.compile(""))
         add("After", S, B, { x, y -> x>y }, S, "")
         add("Before", S, B, { x, y -> x<y }, S, "")
         add("Char at", S, Char::class.java, { x, i, dir -> charAt(x, i, dir) }, Int::class.java, StringDirection::class.java, 0, FROM_START)
         add("Length", S, Int::class.java, { it.length })
         add("Length >", S, B, { x, l -> x.length>l }, Int::class.java, 0)
         add("Length <", S, B, { x, l -> x.length<l }, Int::class.java, 0)
         add("Length =", S, B, { x, l -> x.length==l }, Int::class.java, 0)
         add("Is empty", S, B) { it.isEmpty() }
         add("Is palindrome", S, B) { it.isPalindrome() }
         add("Base64 encode", S, S) { Base64.getEncoder().encodeToString(it.toByteArray()) }
         add("Base64 decode", S, S) {
            try {
               return@add String(Base64.getDecoder().decode(it.toByteArray()))
            } catch (e: IllegalArgumentException) {
               return@add null
            }
         }
         add("To file", S, File::class.java) { File(it) }

         add("Any contains", Strings::class.java, B, { obj, text, ignoreCase -> obj.anyContains(text, ignoreCase) }, S, B, "", true)
         add("Is empty", Strings::class.java, B) { it.isEmpty() }
         add("Elements", Strings::class.java, Int::class.java) { it.size() }

         add("to ASCII", Char::class.java, Int::class.java) { it.toInt() }

         add("Path", File::class.java, String::class.java) { it.absolutePath }
         add("Size", File::class.java, FileSize::class.java) { FileSize(it) }
         add("Name", File::class.java, String::class.java, { it.nameWithoutExtensionOrRoot }, true, true, true)
         add("Name.Suffix", File::class.java, String::class.java) { it.nameOrRoot }
         add("Suffix", File::class.java, String::class.java) { it.extension }
         add("MimeType", File::class.java, MimeType::class.java) { it.mimeType() }
         add("MimeGroup", File::class.java, String::class.java) { it.mimeType().group }
         add("Shortcut of", File::class.java, File::class.java) { WindowsShortcut.targetedFile(it).orElse(null) }
         add("Type", File::class.java, FileType::class.java) { FileType(it) }
         add("Exists", File::class.java, B) { it.exists() }
         add("Anime", File::class.java, S) { renameAnime(it.nameWithoutExtensionOrRoot) }

         add("Group", MimeType::class.java, S) { it.group }
         add("Extensions", MimeType::class.java, S) { it.extensions.joinToString(", ") }

         add("Is", MimeExt::class.java, B, { x, y -> x==y }, MimeExt::class.java, MimeExt("mp3"))

         add("Less", Bitrate::class.java, B, { x, y -> x<y }, Bitrate::class.java, Bitrate(320))
         add("Is", Bitrate::class.java, B, { x, y -> x.compareTo(y)==0 }, Bitrate::class.java, Bitrate(320))
         add("More", Bitrate::class.java, B, { x, y -> x>y }, Bitrate::class.java, Bitrate(320))
         add("Is good", Bitrate::class.java, B) { (value) -> value>=320 }
         add("Is bad", Bitrate::class.java, B) { (value) -> value<=128 }
         add("Is variable", Bitrate::class.java, B) { x -> x.isVariable() }
         add("Is constant", Bitrate::class.java, B) { x -> x.isConstant() }
         add("Is known", Bitrate::class.java, B) { x -> !x.isUnknown() }

         add("Less", Duration::class.java, B, { x, y -> x<y }, Duration::class.java, Duration(0.0))
         add("Is", Duration::class.java, B, { x, y -> x.compareTo(y)==0 }, Duration::class.java, Duration(0.0))
         add("More", Duration::class.java, B, { x, y -> x>y }, Duration::class.java, Duration(0.0), false, false, true)

         add("<  Less", NofX::class.java, B, { x, y -> x<y }, NofX::class.java, NofX(1, 1))
         add("=  Is", NofX::class.java, B, { x, y -> x.compareTo(y)==0 }, NofX::class.java, NofX(1, 1))
         add(">  More", NofX::class.java, B, { x, y -> x>y }, NofX::class.java, NofX(1, 1), false, false, true)
         add(">= Not less", NofX::class.java, B, { x, y -> x>=y }, NofX::class.java, NofX(1, 1))
         add("<> Is not", NofX::class.java, B, { x, y -> x.compareTo(y)!=0 }, NofX::class.java, NofX(1, 1))
         add("<= Not more", NofX::class.java, B, { x, y -> x<=y }, NofX::class.java, NofX(1, 1))

         add("<  Less", FileSize::class.java, B, { x, y -> x<y }, FileSize::class.java, FileSize(0), false, false, true)
         add("=  Is", FileSize::class.java, B, { x, y -> x.compareTo(y)==0 }, FileSize::class.java, FileSize(0))
         add(">  More", FileSize::class.java, B, { x, y -> x>y }, FileSize::class.java, FileSize(0))
         add("Is unknown", FileSize::class.java, B) { it.isUnknown() }
         add("Is known", FileSize::class.java, B) { it.isKnown() }
         add("In bytes", FileSize::class.java, Long::class.java) { it.inBytes() }

         add("Is after", Year::class.java, B, { x, y -> x>y }, Year::class.java, Year.now())
         add("Is", Year::class.java, B, { x, y -> x.compareTo(y)==0 }, Year::class.java, Year.now())
         add("Is before", Year::class.java, B, { x, y -> x<y }, Year::class.java, Year.now())
         add("Is in the future", Year::class.java, B) { x -> x>Year.now() }
         add("Is now", Year::class.java, B) { x -> x.compareTo(Year.now())==0 }
         add("Is in the past", Year::class.java, B) { x -> x<Year.now() }
         add("Is leap", Year::class.java, B, { it.isLeap })

         add("Contains year", RangeYear::class.java, B, { obj, y -> obj.contains(y) }, Year::class.java, Year.now())
         add("Is after", RangeYear::class.java, B, { obj, y -> obj.isAfter(y) }, Year::class.java, Year.now())
         add("Is before", RangeYear::class.java, B, { obj, y -> obj.isBefore(y) }, Year::class.java, Year.now())
         add("Is in the future", RangeYear::class.java, B) { x -> x.contains(Year.now()) }
         add("Is now", RangeYear::class.java, B) { x -> x.isAfter(Year.now()) }
         add("Is in the past", RangeYear::class.java, B) { x -> x.isBefore(Year.now()) }

         add("After", LocalDateTime::class.java, B, { obj, other -> obj.isAfter(other) }, LocalDateTime::class.java, LocalDateTime.now())
         add("Before", LocalDateTime::class.java, B, { obj, other -> obj.isBefore(other) }, LocalDateTime::class.java, LocalDateTime.now())
         add("Is", LocalDateTime::class.java, B, { obj, other -> obj.isEqual(other) }, LocalDateTime::class.java, LocalDateTime.now())

         add("File", Song::class.java, File::class.java) { it.getFile() }
         add("URI", Song::class.java, URI::class.java) { it.uri }

         addPredicatesComparable(Short::class.java, 0.toShort())
         addPredicatesComparable(Int::class.java, 0)
         addPredicatesComparable(Long::class.java, 0L)
         addPredicatesComparable(Double::class.java, 0.0)
         addPredicatesComparable(Float::class.java, 0f)
      }
   }

}