package sp.it.util.math

import com.udojava.evalex.Expression.ExpressionException
import java.math.BigDecimal
import sp.it.util.functional.runTry
import sp.it.util.parsing.ConverterString
import sp.it.util.type.Util

/** Mathematical `BigDecimal -> BigDecimal` function created from string expression. */
class StrExF private constructor(private val expression: String, private val e: com.udojava.evalex.Expression): (BigDecimal) -> BigDecimal {

   override fun invoke(queryParam: BigDecimal): BigDecimal = e.with("x", queryParam).eval()

   override fun toString() = toS(this)

   companion object: ConverterString<StrExF> {
      operator fun invoke(expression: String): StrExF = ofS(expression).orThrow
      override fun toS(o: StrExF) = o.expression
      override fun ofS(s: String) = runTry {
         val e = com.udojava.evalex.Expression(s).with("x", "0")

         // validate
         e.toRPN()
         e.expressionTokenizer.forEachRemaining { token ->
            if ( Util.getFieldValue<Any>(token, "type").toString()=="VARIABLE") {
               if (!e.declaredVariables.contains(token.surface)) {
                  throw ExpressionException("Unknown operator or function: $token")
               }
            }
         }

         StrExF(s, e)
      }.mapError {
         it.message ?: "Unknown error"
      }
   }

}