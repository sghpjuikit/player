package sp.it.util.file.json

import ch.obermuhlner.math.big.BigDecimalMath
import java.io.InputStream
import java.math.BigInteger
import kotlin.text.Charsets.UTF_8

fun parseJson(input: String): JsValue =
   parseJson(input.byteInputStream(UTF_8))

fun parseJson(input: InputStream): JsValue =
   Parser(Lexer(input)).parse()

private class Lexer(private val inputStream: InputStream) {
   var position = -1
   var currentChar: Char = ' '

   private fun getNextChar(): Char {
      val nextByte = inputStream.read()
      position++
      currentChar = if (nextByte != -1) nextByte.toChar() else '\u0000'
      return currentChar
   }

   fun getNextToken(): Token {
      skipWhitespace()

      return when (currentChar) {
         '\u0000' -> {
            Token(TokenType.EOF, "")
         }

         '{' -> {
            getNextChar()
            Token(TokenType.LEFT_BRACE, "{")
         }

         '}' -> {
            getNextChar()
            Token(TokenType.RIGHT_BRACE, "}")
         }

         '[' -> {
            getNextChar()
            Token(TokenType.LEFT_BRACKET, "[")
         }

         ']' -> {
            getNextChar()
            Token(TokenType.RIGHT_BRACKET, "]")
         }

         ':' -> {
            getNextChar()
            Token(TokenType.COLON, ":")
         }

         ',' -> {
            getNextChar()
            Token(TokenType.COMMA, ",")
         }

         'n' -> {
            if (getNextChar()=='u' && getNextChar()=='l' && getNextChar()=='l') {
               getNextChar()
               Token(TokenType.NULL, "null")
            } else
               throw ParseException("Invalid token at position $position")
         }

         't' -> {
            if (getNextChar()=='r' && getNextChar()=='u' && getNextChar()=='e') {
               getNextChar()
               Token(TokenType.TRUE, "true")
            } else
               throw ParseException("Invalid token at position $position")
         }

         'f' -> {
            if (getNextChar()=='a' && getNextChar()=='l' && getNextChar()=='s' && getNextChar()=='e') {
               getNextChar()
               Token(TokenType.FALSE, "false")
            } else
               throw ParseException("Invalid token at position $position")
         }

         '"' -> {
            getNextChar()
            val value = readString()
            Token(TokenType.STRING, value)
         }

         else -> {
            if (currentChar.isDigit() || currentChar == '-') {
               val value = readNumber()
               Token(TokenType.NUMBER, value)
            } else
               throw ParseException("Invalid token at position $position")
         }
      }
   }

   private fun skipWhitespace() {
      while (currentChar.isWhitespace()) {
         getNextChar()
      }
   }

   private fun readString(): String {
      val sb = StringBuilder()
      var escaped = false

      while (currentChar != '\u0000') {
         if (currentChar == '\\' && !escaped) {
            escaped = true
         } else if (currentChar == '"' && !escaped) {
            getNextChar()
            break
         } else {
            if (escaped) {
               when (currentChar) {
                  'b' -> sb.append('\b')
                  't' -> sb.append('\t')
                  'n' -> sb.append('\n')
                  'f' -> sb.append('\u000C')
                  'r' -> sb.append('\r')
                  '"' -> sb.append('"')
                  '\'' -> sb.append('\'')
                  '\\' -> sb.append('\\')
                  'u' -> {
                     val unicode = CharArray(4)
                     for (i in 0 until 4) {
                        getNextChar()
                        unicode[i] = currentChar
                     }
                     val unicodeValue = String(unicode).toIntOrNull(16)
                     if (unicodeValue != null) {
                        sb.append(unicodeValue.toChar())
                     } else {
                        sb.append("\\u")
                        sb.append(unicode)
                     }
                  }

                  else -> sb.append(currentChar)
               }
               escaped = false
            } else {
               sb.append(currentChar)
            }
         }

         getNextChar()
      }

      return sb.toString()
   }

   private fun readNumber(): String {
      val sb = StringBuilder()
      while (currentChar.isDigit() || currentChar == '.' || currentChar == 'e' || currentChar == 'E' || currentChar == '+' || currentChar == '-') {
         sb.append(currentChar)
         getNextChar()
      }
      return sb.toString()
   }

   init {
      getNextChar()
   }
}

private class Parser(private val lexer: Lexer) {
   var currentToken = lexer.getNextToken()

   fun parse(): JsValue {
      return parseValue()
   }

   private fun parseValue(): JsValue {
      return when (currentToken.type) {
         TokenType.NULL -> {
            consumeToken(TokenType.NULL)
            JsNull
         }

         TokenType.TRUE -> {
            consumeToken(TokenType.TRUE)
            JsTrue
         }

         TokenType.FALSE -> {
            consumeToken(TokenType.FALSE)
            JsFalse
         }

         TokenType.STRING -> {
            val value = currentToken.value
            consumeToken(TokenType.STRING)
            JsString(value)
         }

         TokenType.NUMBER -> {
            val value = determineNumberType(currentToken.value)
            consumeToken(TokenType.NUMBER)
            JsNumber(value)
         }

         TokenType.LEFT_BRACKET -> {
            consumeToken(TokenType.LEFT_BRACKET)
            val elements = parseArrayElements()
            consumeToken(TokenType.RIGHT_BRACKET)
            JsArray(elements)
         }

         TokenType.LEFT_BRACE -> {
            consumeToken(TokenType.LEFT_BRACE)
            val properties = parseObjectProperties()
            consumeToken(TokenType.RIGHT_BRACE)
            JsObject(properties)
         }

         else -> throw ParseException("Invalid token at position ${lexer.position}")
      }
   }

   private fun determineNumberType(value: String): Number {
      return when {
         value.contains('.') || value.contains('e', ignoreCase = true) -> BigDecimalMath.toBigDecimal(value)
         else -> {
            val longValue = value.toLongOrNull()
            when {
               // TODO: should we narrow the type down to Byte? Pe
               // longValue != null && longValue >= Byte.MIN_VALUE && longValue <= Byte.MAX_VALUE -> longValue.toByte()
               // longValue != null && longValue >= Short.MIN_VALUE && longValue <= Short.MAX_VALUE -> longValue.toShort()
               longValue!=null && longValue>=Int.MIN_VALUE && longValue<=Int.MAX_VALUE -> longValue.toInt()
               else -> BigInteger(value)
            }
         }
      }
   }

   private fun parseArrayElements(): List<JsValue> {
      val elements = ArrayList<JsValue>()
      if (currentToken.type!=TokenType.RIGHT_BRACKET) {
         while (true) {
            elements += parseValue()

            if (currentToken.type==TokenType.COMMA)
               consumeToken(TokenType.COMMA)
            else
               break
         }
      }
      return elements
   }

   private fun parseObjectProperties(): Map<String, JsValue> {
      val properties = LinkedHashMap<String, JsValue>()
      if (currentToken.type!=TokenType.RIGHT_BRACE) {
         while (true) {
            val key = parseString()
            consumeToken(TokenType.COLON)
            val value = parseValue()
            properties[key] = value

            if (currentToken.type==TokenType.COMMA)
               consumeToken(TokenType.COMMA)
            else
               break
         }
      }
      return properties
   }

   private fun parseString(): String {
      val value = currentToken.value
      consumeToken(TokenType.STRING)
      return value
   }

   private fun consumeToken(expectedType: TokenType) {
      if (currentToken.type==expectedType)
         currentToken = lexer.getNextToken()
      else
         throw ParseException("Unexpected token ${currentToken.type}, expected $expectedType at position ${lexer.position}")
   }
}

enum class TokenType {
   LEFT_BRACE,
   RIGHT_BRACE,
   LEFT_BRACKET,
   RIGHT_BRACKET,
   COLON,
   COMMA,
   NULL,
   TRUE,
   FALSE,
   STRING,
   NUMBER,
   EOF
}

data class Token(val type: TokenType, val value: String)

class ParseException(message: String): RuntimeException(message)