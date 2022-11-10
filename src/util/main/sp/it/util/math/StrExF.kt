package sp.it.util.math

import com.ezylang.evalex.Expression
import com.ezylang.evalex.config.ExpressionConfiguration
import java.math.BigDecimal
import sp.it.util.dev.fail
import sp.it.util.functional.runTry
import sp.it.util.parsing.ConverterString

/** Mathematical `BigDecimal -> BigDecimal` function created from string expression. */
class StrExF private constructor(private val expression: String, private val e: Expression): (BigDecimal) -> BigDecimal {

   override fun invoke(queryParam: BigDecimal): BigDecimal = e.with("x", queryParam).evaluate().numberValue

   override fun toString() = toS(this)

   companion object: ConverterString<StrExF> {

      private var configuration = ExpressionConfiguration.builder()
         .arraysAllowed(false)
         .implicitMultiplicationAllowed(true)
         .structuresAllowed(false)
         .build()

      operator fun invoke(expression: String): StrExF = ofS(expression).orThrow
      override fun toS(o: StrExF) = o.expression
      override fun ofS(s: String) = runTry {
         val e = Expression(s, configuration).with("x", "0")
         if (e.undefinedVariables.isNotEmpty()) fail { "Unknown operator or function: ${e.undefinedVariables.first()}" }
         e.validate()
         StrExF(s, e)
      }.mapError {
         it.message ?: "Unknown error"
      }
   }

}