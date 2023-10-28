package sp.it.util.file.json

import ch.obermuhlner.math.big.BigDecimalMath
import java.io.InputStream
import java.math.BigInteger
import kotlin.text.Charsets.UTF_8
import sp.it.util.math.rangeBigInt

fun parseJson(input: String): JsValue =
   parseJson(input.byteInputStream(UTF_8))

fun parseJson(input: InputStream): JsValue =
   Parser(Lexer(input)).parseValue()

private class Lexer(inputStream: InputStream) {
   private var pos = -1
   private var reader = inputStream.bufferedReader(UTF_8)
   private var ch: Int = nextCh()

   fun pos() = pos

   fun close() = reader.close()

   fun nextCh(): Int {
      val char = reader.read()
      pos++
      ch = if (char!=-1) char else Chars.EOF
      return ch
   }

   fun nextToken(): Token {
      skipWhitespace()

      return when (ch) {
         Chars.EOF -> {
            Token.Eof
         }
         Chars.LEFT_BRACE -> {
            nextCh()
            Token.Lbc
         }
         Chars.RIGHT_BRACE -> {
            nextCh()
            Token.Rbc
         }
         Chars.LEFT_BRACKET -> {
            nextCh()
            Token.Lbk
         }
         Chars.RIGHT_BRACKET -> {
            nextCh()
            Token.Rbk
         }
         Chars.COLON -> {
            nextCh()
            Token.Col
         }
         Chars.COMMA -> {
            nextCh()
            Token.Com
         }
         Chars.N -> {
            if (nextCh()==Chars.U && nextCh()==Chars.L && nextCh()==Chars.L) {
               nextCh()
               Token.Nul
            } else
               throw ParseException("Invalid token at position $pos")
         }
         Chars.T -> {
            if (nextCh()==Chars.R && nextCh()==Chars.U && nextCh()==Chars.E) {
               nextCh()
               Token.Tru
            } else
               throw ParseException("Invalid token at position $pos")
         }
         Chars.F -> {
            if (nextCh()==Chars.A && nextCh()==Chars.L && nextCh()==Chars.S && nextCh()==Chars.E) {
               nextCh()
               Token.Fal
            } else
               throw ParseException("Invalid token at position $pos")
         }
         Chars.QUOTE -> {
            nextCh()
            Token.Str(readString())
         }
         else -> {
            if (Character.isDigit(ch) || ch==Chars.DASH)
               Token.Num(readNumber())
            else
               throw ParseException("Invalid token at position $pos")
         }
      }
   }

   private fun skipWhitespace() {
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

   private fun readNumber(): String {
      val sb = StringBuilder()
      while (Character.isDigit(ch) || ch==Chars.DOT || ch==Chars.E || ch==Chars.E_UPPER || ch==Chars.PLUS || ch==Chars.DASH) {
         sb.appendCodePoint(ch)
         nextCh()
      }
      return sb.toString()
   }

}

private class Parser(lexer: Lexer) {
   private val lexer = lexer
   private var currentToken = lexer.nextToken()

   fun parse(): JsValue {
      try {
         return parseValue()
      } finally {
         lexer.close()
      }
   }

   fun parseValue(): JsValue {
      return when (val token = currentToken) {
         Token.Nul -> {
            consumeToken(Token.Nul)
            JsNull
         }

         Token.Tru -> {
            consumeToken(Token.Tru)
            JsTrue
         }

         Token.Fal -> {
            consumeToken(Token.Fal)
            JsFalse
         }

         is Token.Str -> {
            consumeToken(token)
            JsString(token.value)
         }

         is Token.Num -> {
            val value = parseNumber(token.value)
            consumeToken(token)
            JsNumber(value)
         }

         Token.Lbk -> {
            consumeToken(Token.Lbk)
            val elements = parseArrayElements()
            consumeToken(Token.Rbk)
            JsArray(elements)
         }

         Token.Lbc -> {
            consumeToken(Token.Lbc)
            val properties = parseObjectProperties()
            consumeToken(Token.Rbc)
            JsObject(properties)
         }

         else -> throw ParseException("Invalid token at position ${lexer.pos()}")
      }
   }

   private fun parseString(): String {
      val token = currentToken
      if (token is Token.Str) {
         val value = token.value
         consumeToken(token)
         return value
      } else
         throw ParseException("Unexpected token $currentToken, expected STRING at position ${lexer.pos()}")
   }

   private fun parseNumber(value: String): Number {
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

   private fun parseArrayElements(): List<JsValue> {
      val elements = ArrayList<JsValue>()
      if (currentToken!=Token.Rbk) {
         while (true) {
            elements += parseValue()

            if (currentToken==Token.Com)
               consumeToken(Token.Com)
            else
               break
         }
      }
      return elements
   }

   private fun parseObjectProperties(): Map<String, JsValue> {
      val properties = LinkedHashMap<String, JsValue>()
      if (currentToken!=Token.Rbc) {
         while (true) {
            val key = parseString()
            consumeToken(Token.Col)
            val value = parseValue()
            properties[key] = value

            if (currentToken==Token.Com)
               consumeToken(Token.Com)
            else
               break
         }
      }
      return properties
   }

   private fun consumeToken(expectedType: Token) {
      if (currentToken===expectedType)
         currentToken = lexer.nextToken()
      else
         throw ParseException("Unexpected token $currentToken, expected $expectedType at position ${lexer.pos()}")
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

sealed interface Token {
   data object Eof: Token
   data object Lbc: Token
   data object Rbc: Token
   data object Lbk: Token
   data object Rbk: Token
   data object Col: Token
   data object Com: Token
   data object Nul: Token
   data object Tru: Token
   data object Fal: Token
   @JvmInline value class Str(val value: String): Token
   @JvmInline value class Num(val value: String): Token
}

class ParseException(message: String): RuntimeException(message)