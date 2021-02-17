package sp.it.util.math

import net.objecthunter.exp4j.Expression
import net.objecthunter.exp4j.ExpressionBuilder
import sp.it.util.functional.runTry
import sp.it.util.parsing.ConverterString

/** Mathematical `Double -> Double` Function created from string expression. */
class StrExF private constructor(private val expression: String, private val e: Expression): (Double) -> Double {

   override fun invoke(queryParam: Double): Double = e.setVariable("x", queryParam).evaluate()

   override fun toString() = toS(this)

   companion object: ConverterString<StrExF> {
      operator fun invoke(expression: String): StrExF = ofS(expression).orThrow
      override fun toS(o: StrExF) = o.expression
      override fun ofS(s: String) = runTry {
            val e = ExpressionBuilder(s).variables("x").build()
            val v = e.validate(false)
            if (v.isValid) StrExF(s, e)
            else error(v.errors.joinToString("\n"))
      }.mapError {
         it.message ?: "Unknown error"
      }
   }

}