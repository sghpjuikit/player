package sp.it.util.file.json

import ch.obermuhlner.math.big.BigDecimalMath
import java.io.InputStream
import java.io.Reader
import java.math.BigInteger
import java.util.LinkedList
import kotlin.text.Charsets.UTF_8
import sp.it.util.file.json.JsToken.*
import sp.it.util.math.rangeBigInt
import sp.it.util.dev.fail
import sp.it.util.dev.printStacktrace

fun parseJson(input: String): JsValue =
   Parser(Lexer(input.reader())).parse()

fun parseJson(input: InputStream): JsValue =
   Parser(Lexer(input.reader(UTF_8))).parse()

private class Lexer(reader: Reader) {
   private var pos = -1
   private var reader = reader.buffered()
   private var ch: Int = nextCh()

   fun pos() = pos

   fun close() = reader.close()

   fun nextCh(): Int {
      val char = reader.read()
      pos++
      ch = if (char!=-1) char else Chars.EOF
      return ch
   }

   fun nextEOF() {
      while (ch!=Chars.EOF) {
         if (Character.isWhitespace(ch))
            nextCh()
         else
            fail { "Invalid token at $pos" }
      }
   }

   fun nextToken(): JsToken {
      readWhitespace()

      return when (ch) {
         Chars.EOF -> {
            Eof
         }
         Chars.LEFT_BRACE -> {
            nextCh()
            Lbc
         }
         Chars.RIGHT_BRACE -> {
            nextCh()
            Rbc
         }
         Chars.LEFT_BRACKET -> {
            nextCh()
            Lbk
         }
         Chars.RIGHT_BRACKET -> {
            nextCh()
            Rbk
         }
         Chars.COLON -> {
            nextCh()
            Col
         }
         Chars.COMMA -> {
            nextCh()
            Com
         }
         Chars.N -> {
            if (nextCh()==Chars.U && nextCh()==Chars.L && nextCh()==Chars.L) {
               nextCh()
               Nul
            } else
               fail { "Invalid token at position $pos" }
         }
         Chars.T -> {
            if (nextCh()==Chars.R && nextCh()==Chars.U && nextCh()==Chars.E) {
               nextCh()
               Tru
            } else
               fail { "Invalid token at position $pos" }
         }
         Chars.F -> {
            if (nextCh()==Chars.A && nextCh()==Chars.L && nextCh()==Chars.S && nextCh()==Chars.E) {
               nextCh()
               Fal
            } else
               fail { "Invalid token at position $pos" }
         }
         Chars.QUOTE -> {
            nextCh()
            if (ch==Chars.EOF) Eof
            else Str(readString())
         }
         else -> {
            if (Character.isDigit(ch) || ch==Chars.DASH)
               Num(readNumber())
            else
               fail { "Invalid token at position $pos" }
         }
      }
   }

   fun readWhitespace() {
      while (Character.isWhitespace(ch))
         nextCh()
   }

   private fun readString(): String {
      val sb = StringBuilder()
      var escaped = false

      while (ch!=Chars.EOF) {
         if (ch==Chars.BACK_SLASH && !escaped) {
            escaped = true
         } else if (ch==Chars.QUOTE && !escaped) {
            nextCh()
            break
         } else {
            if (escaped) {
               when (ch) {
                  Chars.B -> sb.append('\b')
                  Chars.T -> sb.append('\t')
                  Chars.N -> sb.append('\n')
                  Chars.F -> sb.append('\u000C')
                  Chars.R -> sb.append('\r')
                  Chars.QUOTE -> sb.append('"')
                  Chars.TICK -> sb.append('\'')
                  Chars.BACK_SLASH -> sb.append('\\')
                  Chars.U -> {
                     val unicodeValue = (0..3).fold(0) { v, _ -> (v shl 4) or Character.digit(nextCh(), 16) }
                     sb.appendCodePoint(unicodeValue)
                  }
                  else -> sb.appendCodePoint(ch)
               }
               escaped = false
            } else {
               sb.appendCodePoint(ch)
            }
         }
         nextCh()
      }

      return sb.toString()
   }

   private fun readNumber(): Number {
      val value = buildString {
         while (Character.isDigit(ch) || ch==Chars.DOT || ch==Chars.E || ch==Chars.E_UPPER || ch==Chars.PLUS || ch==Chars.DASH) {
            appendCodePoint(ch)
            nextCh()
         }
      }
      return if (value.startsWith("e") or value.startsWith("E"))
         fail { "Invalid token at position ${pos-value.length}" }
      else if (value.startsWith("-e") or value.startsWith("-E") || value.startsWith("+e") or value.startsWith("+E"))
         fail { "Invalid token at position ${pos-value.length+1}" }
      else if (' ' in value)
         fail { "Invalid token at position ${pos-value.length+value.indexOf(' ')}" }
      else if ('.' in value || value.contains('e', ignoreCase = true))
         BigDecimalMath.toBigDecimal(value)
      else
         when (val num = BigInteger(value)) {
            // Narrow type down to common types
            in Int.rangeBigInt -> num.toInt()
            in Long.rangeBigInt -> num.toLong()
            else -> num
         }
   }

}

private class Parser(lexer: Lexer) {
   private var lexerLastPos = 0
   private val lexer = lexer
   private var currentToken = lexer.nextToken()

   fun parse(): JsValue =
      try {
         parseValueCompletely()
      } finally {
         lexer.close()
      }

   private fun parseValueCompletely(): JsValue =
      parseValue().apply {
         if (currentToken!=Eof) fail { "Invalid token at position $lexerLastPos" }
         lexer.nextEOF()
      }

   private fun parseValue(): JsValue =
      when (val token = currentToken) {
         Nul -> {
            consumeToken(Nul)
            JsNull
         }
         Tru -> {
            consumeToken(Tru)
            JsTrue
         }
         Fal -> {
            consumeToken(Fal)
            JsFalse
         }
         is Str -> {
            consumeToken(token)
            JsString(token.value)
         }
         is Num -> {
            consumeToken(token)
            JsNumber(token.value)
         }
         Lbk -> {
            consumeToken(Lbk)
            val elements = parseArray()
            consumeToken(Rbk)
            JsArray(elements)
         }
         Lbc -> {
            consumeToken(Lbc)
            val properties = parseObject()
            consumeToken(Rbc)
            JsObject(properties)
         }
         else -> fail { "Invalid token at position $lexerLastPos" }
      }

   private fun parseString(): String {
      val token = currentToken
      if (token is Str) {
         val value = token.value
         consumeToken(token)
         return value
      } else
         fail { "Invalid token $currentToken at position $lexerLastPos" }
   }

   private fun parseArray(): List<JsValue> {
      val elements = ArrayList<JsValue>()
      if (currentToken!=Rbk) {
         while (true) {
            elements += parseValue()

            if (currentToken==Com)
               consumeToken(Com)
            else
               break
         }
      }
      return elements
   }

   private fun parseObject(): Map<String, JsValue> {
      val properties = LinkedHashMap<String, JsValue>()
      if (currentToken!=Rbc) {
         while (true) {
            val key = parseString()
            consumeToken(Col)
            val value = parseValue()
            properties[key] = value

            if (currentToken==Com)
               consumeToken(Com)
            else
               break
         }
      }
      return properties
   }

   private fun consumeToken(expectedToken: JsToken) {
      lexerLastPos = lexer.pos()
      if (currentToken===expectedToken)
         currentToken = lexer.nextToken()
      else
         fail { "Invalid token $currentToken at position $lexerLastPos" }
   }
}

object Chars {
   const val EOF = '\u0000'.code
   const val LEFT_BRACE = '{'.code
   const val RIGHT_BRACE = '}'.code
   const val LEFT_BRACKET = '['.code
   const val RIGHT_BRACKET = ']'.code
   const val COLON = ':'.code
   const val DOT = '.'.code
   const val COMMA = ','.code
   const val QUOTE = '"'.code
   const val TICK = '\''.code
   const val DASH = '-'.code
   const val PLUS = '+'.code
   const val BACK_SLASH = '\\'.code
   const val B = 'b'.code
   const val N = 'n'.code
   const val U = 'u'.code
   const val L = 'l'.code
   const val T = 't'.code
   const val R = 'r'.code
   const val E = 'e'.code
   const val E_UPPER = 'E'.code
   const val F = 'f'.code
   const val A = 'a'.code
   const val S = 's'.code
}

sealed interface JsTokenLike
sealed interface JsTokenLiteral { val text: String }
sealed interface JsToken: JsTokenLike {
   data object Eof: JsToken, JsTokenLiteral { override val text = "\u0000" }
   data object Lbc: JsToken, JsTokenLiteral { override val text = "{" }
   data object Rbc: JsToken, JsTokenLiteral { override val text = "}" }
   data object Lbk: JsToken, JsTokenLiteral { override val text = "[" }
   data object Rbk: JsToken, JsTokenLiteral { override val text = "]" }
   data object Col: JsToken, JsTokenLiteral { override val text = ":" }
   data object Com: JsToken, JsTokenLiteral { override val text = "," }
   data object Nul: JsToken, JsTokenLiteral { override val text = "null" }
   data object Tru: JsToken, JsTokenLiteral { override val text = "true" }
   data object Fal: JsToken, JsTokenLiteral { override val text = "false" }
   @JvmInline value class Str(val value: String): JsToken
   @JvmInline value class Num(val value: Number): JsToken
}

fun JsValue.tokens(): Sequence<JsToken> = sequence {
   val stack = LinkedList<JsTokenLike>()
   stack += this@tokens

   while (stack.isNotEmpty()) {
      val current = stack.pop()

      when (current) {
         is JsToken -> yield(current)
         is JsNull -> yield(Nul)
         is JsTrue -> yield(Tru)
         is JsFalse -> yield(Fal)
         is JsString -> yield(Str(current.value))
         is JsNumber -> yield(Num(current.value))
         is JsArray -> {
            yield(Lbk)
            val elementRaw = current.value
            val elements = sequence {
               var i = 1
               elementRaw.forEach {
                  yield(it)
                  if (i<elementRaw.size) yield(Com)
                  i++
               }
               yield(Rbk)
            }
            stack.addAll(0, elements.toList())
         }
         is JsObject -> {
            yield(Lbc)
            val elementRaw = current.value
            val elements = sequence {
               var i = 0
               elementRaw.entries.sortedBy { it.key }.forEach { (key, value) ->
                  yield(Str(key))
                  yield(Col)
                  yield(value)
                  if (i<elementRaw.size-1) yield(Com)
                  i++
               }
               yield(Rbc)
            }
            stack.addAll(0, elements.toList())
         }
      }
   }
}