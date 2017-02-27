package util.functional;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.ValidationResult;
import util.functional.Functors.Ƒ1;
import util.parsing.ParsesFromString;
import util.parsing.ParsesToString;
import util.parsing.StringParseStrategy;
import util.parsing.StringParseStrategy.From;
import util.parsing.StringParseStrategy.To;

/**
 * String expression function
 *
 * @author Martin Polakovic
 */
@StringParseStrategy(
		from = From.ANNOTATED_METHOD, to = To.TO_STRING_METHOD,
		exFrom = {IllegalStateException.class, IllegalArgumentException.class}
)
public class StrExF implements Ƒ1<Double,Double> {
	private final String ex;
	private final Expression e;

	@ParsesFromString
	public StrExF(String s) {
		try {
			ex = s;
			e = new ExpressionBuilder(s).variables("x").build();
			ValidationResult v = e.validate(false);
			if (!v.isValid())
				throw new Exception(v.getErrors().get(0));
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public Double apply(Double i) {
		return e.setVariable("x", i).evaluate();
	}

	@ParsesToString
	@Override
	public String toString() {
		return ex;
	}

}