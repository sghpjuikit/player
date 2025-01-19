package sp.it.util.file.json

import ch.obermuhlner.math.big.BigDecimalMath
import java.io.InputStream
import java.io.Reader
import java.math.BigInteger
import java.util.LinkedList
import kotlin.text.Charsets.UTF_8
import org.jetbrains.kotlin.backend.common.peek
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import sp.it.util.dev.fail
import sp.it.util.file.json.JsToken.Col
import sp.it.util.file.json.JsToken.Com
import sp.it.util.file.json.JsToken.Eof
import sp.it.util.file.json.JsToken.Fal
import sp.it.util.file.json.JsToken.Lbc
import sp.it.util.file.json.JsToken.Lbk
import sp.it.util.file.json.JsToken.Nul
import sp.it.util.file.json.JsToken.Num
import sp.it.util.file.json.JsToken.Rbc
import sp.it.util.file.json.JsToken.Rbk
import sp.it.util.file.json.JsToken.Str
import sp.it.util.file.json.JsToken.Tru
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import sp.it.util.functional.ifNotNull
import sp.it.util.math.rangeBigInt

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
      ch = if (char==0) fail { """Illegal character \0 (NULL) at $pos""" } else if (char!=-1) char else Chars.EOF
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
      while (ch==32 || ch==10 || ch==13 || ch==9)
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
                  Chars.QUOTE -> sb.append('"')
                  Chars.BACK_SLASH -> sb.append('\\')
                  Chars.SLASH -> sb.append('/')
                  Chars.TICK -> sb.append('\'')
                  Chars.B -> sb.append('\b')
                  Chars.F -> sb.append('\u000C')
                  Chars.N -> sb.append('\n')
                  Chars.R -> sb.append('\r')
                  Chars.T -> sb.append('\t')
                  Chars.U -> {
                     val unicodeValue = (0..3).fold(0) { v, _ -> (v shl 4) or Character.digit(nextCh(), 16) }
                     sb.appendCodePoint(unicodeValue)
                  }
                  else -> fail { "Invalid token at position $pos" }
               }
               escaped = false
            } else {
               when (ch) {
                  Chars.TAB -> fail { "Invalid token at position $pos" }
                  Chars.NEWLINE -> fail { "Invalid token at position $pos" }
               }
               sb.appendCodePoint(ch)
            }
         }
         nextCh()
      }

      return sb.toString()
   }

   private fun readNumber(): Number {
      val value = buildString {

         fun rSign() {
            if (ch==Chars.DASH) { appendCodePoint(ch); nextCh() }
            else if (ch==Chars.PLUS) { appendCodePoint(ch); nextCh() }
         }
         fun rDigs() {
            if (ch in 48..57) { appendCodePoint(ch); nextCh() }
            else fail { "Invalid token at position $pos" }
            while (ch in 48..57) { appendCodePoint(ch); nextCh() }
         }
         fun rInt() {
            if (ch==Chars.DASH) {
               appendCodePoint(ch); nextCh()
               if (ch==48) {
                  appendCodePoint(ch); nextCh()
               } else {
                  rDigs()
               }
            } else if (ch==48) {
               appendCodePoint(ch); nextCh()
            } else {
               rDigs()
            }
         }
         fun rExp() {
            if (ch==Chars.E) { appendCodePoint(ch); nextCh(); rSign(); rDigs() }
            else if (ch==Chars.E_UPPER) { appendCodePoint(ch); nextCh(); rSign(); rDigs() }
         }
         fun rFrac() {
            if (ch==Chars.DOT) { appendCodePoint(ch); nextCh(); rDigs() }
         }

         rInt()
         rFrac()
         rExp()
      }
      return if ('.' in value || value.contains('e', ignoreCase = true))
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

   sealed interface JsParseState { fun add(child: JsValue): Unit }
   class JsStartParseState(var value: JsValue?): JsParseState { override fun add(child: JsValue) { value = child } }
   class JsArrayParseState(val value: ArrayList<JsValue>): JsParseState { override fun add(child: JsValue) { value.add(child) } }
   class JsObjectParseState(val value: LinkedHashMap<String, JsValue>): JsParseState { override fun add(child: JsValue) { fail { "Illegal state" } } }
   class JsObjectKeyParseState(val o: JsObjectParseState, val key: String, var value: JsValue?): JsParseState { override fun add(child: JsValue) { value=child; o.value.put(key, child) } }

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

   private fun parseValue(): JsValue {
      val stack = ArrayDeque<JsParseState>()
      var cur: JsParseState = JsStartParseState(null)

      fun JsParseState.com(): JsParseState {
         when (this) {
            is JsArrayParseState -> when (currentToken) {
               Com -> { consumeToken(Com); if (currentToken == Rbk) fail { "Invalid=$currentToken token at position $lexerLastPos" } }
               Rbk -> {}
               Eof -> {}
               else -> fail { "Invalid token=$currentToken at position $lexerLastPos, expected ${Com.text} or ${Rbk.text}" }
            }
            is JsObjectParseState -> when (currentToken) {
               Com ->  { consumeToken(Com); if (currentToken == Rbc) fail { "Invalid token=$currentToken at position $lexerLastPos" } }
               Rbc -> {}
               Eof -> {}
               else -> fail { "Invalid token=$currentToken at position $lexerLastPos, expected ${Com.text} or ${Rbc.text}" }
            }
            is JsObjectKeyParseState -> when (currentToken) {
               Com ->  { consumeToken(Com); if (currentToken == Rbc) fail { "Invalid token=$currentToken at position $lexerLastPos" } }
               Rbc -> {}
               Eof -> {}
               else -> fail { "Invalid token=$currentToken at position $lexerLastPos, expected ${Com.text} or ${Rbc.text}" }
            }
            else -> {}
         }
         return this
      }

      do {
         cur = when (val c = cur) {
            is JsStartParseState ->
               if (c.value!=null) return c.value!!
               else when (val token = currentToken.apply { consumeToken(currentToken) }) {
                  Nul    -> return JsNull
                  Tru    -> return JsTrue
                  Fal    -> return JsFalse
                  is Str -> return JsString(token.value)
                  is Num -> return JsNumber(token.value)
                  Lbk    -> JsArrayParseState(ArrayList()).apply { stack.push(c) }
                  Lbc    -> JsObjectParseState(LinkedHashMap<String, JsValue>()).apply { stack.push(c) }
                  else   -> fail { "Invalid token=$token at position $lexerLastPos" }
               }
            is JsArrayParseState ->
               when (val token = currentToken.apply { consumeToken(currentToken) }) {
                  Nul    -> { c.value.add(JsNull); c.com() }
                  Tru    -> { c.value.add(JsTrue); c.com() }
                  Fal    -> { c.value.add(JsFalse); c.com() }
                  is Str -> { c.value.add(JsString(token.value)); c.com() }
                  is Num -> { c.value.add(JsNumber(token.value)); c.com() }
                  Lbk    -> JsArrayParseState(ArrayList()).apply { stack.push(c) }
                  Lbc    -> JsObjectParseState(LinkedHashMap<String, JsValue>()).apply { stack.push(c) }
                  Rbk    -> {
                     val x = stack.pop().apply { add(JsArray(c.value)) }.com()
                     x.asIf<JsObjectKeyParseState>()?.let { ps ->
                        ps.o.value.put(ps.key, ps.value.asIs())
                        stack.pop()
                     } ?: x
                  }
                  else   -> fail { "Invalid token=$token at position $lexerLastPos" }
               }
            is JsObjectParseState ->
               when (val token = currentToken.apply { consumeToken(currentToken) }) {
                  is Str -> {
                     consumeToken(Col)
                     JsObjectKeyParseState(c, token.value, null).apply { stack.push(c) }
                  }
                  Rbc -> {
                     val x = stack.pop().apply { add(JsObject(c.value)) }.com()
                     x.asIf<JsObjectKeyParseState>()?.let { ps ->
                        ps.o.value.put(ps.key, ps.value.asIs())
                        stack.pop()
                     } ?: x
                  }
                  else -> fail { "Invalid token=$token at position $lexerLastPos" }
               }
            is JsObjectKeyParseState ->
               when (val token = currentToken.apply { consumeToken(currentToken) }) {
                  Nul    -> { c.o.value.put(c.key, JsNull); stack.pop().com() }
                  Tru    -> { c.o.value.put(c.key, JsTrue); stack.pop().com() }
                  Fal    -> { c.o.value.put(c.key, JsFalse); stack.pop().com() }
                  is Str -> { c.o.value.put(c.key, JsString(token.value)); stack.pop().com() }
                  is Num -> { c.o.value.put(c.key, JsNumber(token.value)); stack.pop().com(); }
                  Lbk    -> JsArrayParseState(ArrayList()).apply { stack.push(c) }
                  Lbc    -> JsObjectParseState(LinkedHashMap<String, JsValue>()).apply { stack.push(c) }
                  Rbc    -> { stack.pop().apply { c.o.value.put(c.key, c.value.asIs()) }.com(); stack.pop().apply { add(JsObject(c.o.value)) } }
                  else   -> fail { "Invalid token=$token at position $lexerLastPos" }
               }
         }
      } while (
         cur !is JsStartParseState
      )
      return cur.asIs<JsStartParseState>().value!!

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

   private fun consumeToken(expectedToken: JsToken) {
      lexerLastPos = lexer.pos()
      if (currentToken===expectedToken)
         currentToken = lexer.nextToken()
      else
         fail { "Invalid token $currentToken at position $lexerLastPos expected $expectedToken" }
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
   const val NEWLINE = '\n'.code
   const val TAB = '\t'.code
   const val TICK = '\''.code
   const val DASH = '-'.code
   const val PLUS = '+'.code
   const val SLASH = '/'.code
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