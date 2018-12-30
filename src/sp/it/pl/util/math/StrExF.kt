package sp.it.pl.util.math

import net.objecthunter.exp4j.Expression
import net.objecthunter.exp4j.ExpressionBuilder
import sp.it.pl.util.dev.Dependency
import sp.it.pl.util.functional.Functors.Ƒ1
import sp.it.pl.util.functional.Try

/** Function created from string expression. */
class StrExF(s: String): Ƒ1<Double, Double> {
    private val expression: String
    private val e: Expression

    init {
        try {
            expression = s
            e = ExpressionBuilder(s).variables("x").build()
            val v = e.validate(false)
            if (!v.isValid)
                throw Exception(v.errors[0])
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }
    }

    override fun apply(queryParam: Double): Double = e.setVariable("x", queryParam).evaluate()

    @Dependency("fromString")
    override fun toString() = expression

    companion object {
        @Dependency("toString")
        fun fromString(s: String): Try<StrExF, Exception> {
            return try {
                Try.ok(StrExF(s))
            } catch (e: Exception) {
                Try.error(e)
            }
        }
    }

}