package sp.it.util.math

import com.ezylang.evalex.Expression
import com.ezylang.evalex.bigmath.functions.bigdecimalmath.BigDecimalMathFunctions
import com.ezylang.evalex.bigmath.operators.bigdecimalmath.BigDecimalMathOperators
import com.ezylang.evalex.config.ExpressionConfiguration
import com.ezylang.evalex.config.MapBasedFunctionDictionary
import com.ezylang.evalex.config.MapBasedOperatorDictionary
import com.ezylang.evalex.functions.FunctionIfc
import com.ezylang.evalex.operators.OperatorIfc
import java.math.BigDecimal
import org.jetbrains.kotlin.utils.threadLocal
import sp.it.util.dev.fail
import sp.it.util.functional.asIs
import sp.it.util.functional.net
import sp.it.util.functional.runTry
import sp.it.util.parsing.ConverterString
import sp.it.util.text.plural
import sp.it.util.type.Util

/** Mathematical `BigDecimal -> BigDecimal` function created from string expression. */
class StrExF private constructor(private val expression: String, private val e: Expression): (BigDecimal) -> BigDecimal {

   override fun invoke(queryParam: BigDecimal): BigDecimal = e.with("x", queryParam).evaluate().numberValue

   override fun toString() = toS(this)

   companion object: ConverterString<StrExF> {

      /** Configuration for evaluating [StrExF] */
      private val configuration by threadLocal {
         ExpressionConfiguration.builder()
            .arraysAllowed(false)
            .implicitMultiplicationAllowed(true)
            .structuresAllowed(false)
            .build()
            // slows down evaluation significantly
            // .withAdditionalFunctions(*BigDecimalMathFunctions.allFunctions()) // advanced Java BigDecimal math functions using an arbitrary precision from Big-math
            // .withAdditionalOperators(*BigDecimalMathOperators.allOperators()) // advanced Java BigDecimal math functions using an arbitrary precision from Big-math
      }

      /** Ui names of functions available for use for [StrExF] */
      fun funs(): List<String> = configuration.functionDictionary
         .asIs<MapBasedFunctionDictionary>().net { Util.getFieldValue<Map<String, FunctionIfc>>(it, "functions") }
         .entries.map { (name, f) -> name.lowercase() + f.toS() }
         .sorted()

      /** Ui names of prefix operators available for use for [StrExF] */
      fun operatorsPrefix(): List<String> = configuration.operatorDictionary
         .asIs<MapBasedOperatorDictionary>().net { Util.getFieldValue<Map<String, OperatorIfc>>(it, "prefixOperators") }
         .entries.map { (name, _) -> name }
         .sorted()

      /** Ui names of postfix operators available for use for [StrExF] */
      fun operatorsPostfix(): List<String> = configuration.operatorDictionary
         .asIs<MapBasedOperatorDictionary>().net { Util.getFieldValue<Map<String, OperatorIfc>>(it, "postfixOperators") }
         .entries.map { (name, _) -> name }
         .sorted()

      /** Ui names of infix operators available for use for [StrExF] */
      fun operatorsInfix(): List<String> = configuration.operatorDictionary
         .asIs<MapBasedOperatorDictionary>().net { Util.getFieldValue<Map<String, OperatorIfc>>(it, "infixOperators") }
         .entries.map { (name, _) -> name }
         .sorted()

      private fun FunctionIfc.toS(): String {
         val args = functionParameterDefinitions
         return if (args.isEmpty()) "" else args.joinToString(", ", "(", ")") { if (it.isVarArg) it.name.plural() + "..." else it.name }
      }

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