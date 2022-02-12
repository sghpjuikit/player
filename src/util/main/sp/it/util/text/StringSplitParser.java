package sp.it.util.text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import sp.it.util.dev.Dependency;
import sp.it.util.functional.Try;
import static java.util.Collections.emptyMap;
import static sp.it.util.functional.Try.Java.error;
import static sp.it.util.functional.Try.Java.ok;
import static sp.it.util.functional.Util.forEachBoth;
import static sp.it.util.functional.Util.list;
import static sp.it.util.functional.Util.repeat;

/**
 * Function that transforms strings into list of strings by splitting the original
 * string by delimiters.
 * <p/>
 * Defined by parsing expression, e.g. "%first% - %second%". The 'first'
 * and 'second' are parse keys and the ' - ' is delimiter. There can be any
 * number of parse keys (and delimiters).
 * <p/>
 * The parsing expression must:
 * <ul>
 * <li> start and end with a parse key - must not start or end with delimiter
 * <li> have every two adjacent parse keys separated by delimiter
 * <li> no parse key can contain the {@link #PARSE_KEY_LIMITER} character
 * <li> parse keys don't have to have unique names
 * </ul>
 * <p/>
 * Te output list is always of the
 * same size as number of parsing keys (e.g. "%one% - %two%" will produce
 * list of size 2). If the text parsing failed at any point, the remaining
 * strings will be null. Parsing fails when the delimiter can not be found in
 * the parsed text.
 * <p/>
 * For example parsing expression '%one%:%two%,%three%' will split string
 * 'abc:def,ghi' into list containing elements 'abc','def','ghi' and string
 * 'abc:def' into list containing: 'abc',null,null because the 3rd delimiter is
 * not present.
 */
public class StringSplitParser implements Function<String,List<String>> {
	public static final char PARSE_KEY_LIMITER = '%';

	public final String expression;
	public final List<String> parse_keys = new ArrayList<>();
	public final List<String> key_separators = new ArrayList<>();

	private StringSplitParser(String expression) {
		this.expression = expression;
	}

	/**
	 * Parses text into parts.
	 *
	 * @param text text to parse
	 * @return list of strings representing the splits or empty list if parsing fails
	 */
	@Override
	public List<String> apply(String text) {
		List<String> out = new ArrayList<>(parse_keys.size());
		for (String sep : key_separators) {
			int at = text.indexOf(sep);
			if (at==-1) {
				repeat(parse_keys.size() - out.size(), () -> out.add(null));
				return out;
			}
			String val = text.substring(0, at);
			out.add(val);   // add ith value
			text = text.substring(at + sep.length());
		}
		out.add(text);  //add last value (N values, N-1 separators)

		if (out.size()==parse_keys.size()) {
			return out;
		} else {
			return list();
		}

	}

	/**
	 * Same as {@link #apply(java.lang.String)}, but returns the keys too.
	 *
	 * @param text text to parse
	 * @return map of key-split entries representing the splits or empty map if parsing fails
	 */
	public Map<String,String> applyM(String text) {
		List<String> splits = apply(text);
		if (splits.size()==parse_keys.size()) {
			Map<String,String> m = new HashMap<>();
			forEachBoth(parse_keys, splits, m::put);
			return m;
		} else {
			return emptyMap();
		}
	}

	@Dependency("fromString")
	@Override
	public String toString() {
		return expression;
	}

	public static @NotNull StringSplitParser singular() {
		return StringSplitParser.fromString("%all%").getOrThrow();
	}

	@SuppressWarnings("StringConcatenationInLoop")
	@Dependency("toString")
	public static Try<StringSplitParser, String> fromString(String expression) {
		StringSplitParser ssp = new StringSplitParser(expression);

		int limiter = 0;
		String key = "";
		String sep = "";
		for (int i = 0; i<expression.length(); i++) {
			char c = expression.charAt(i);
			if (c==PARSE_KEY_LIMITER) {
				if (limiter==1) {
					limiter = 0;
					if (!key.isEmpty()) ssp.parse_keys.add(key);
					else
						return error("Cant create split string expression. Wrong format. '" + expression + "'");
					key = "";
				} else {
					limiter = 1;
					if (!sep.isEmpty()) ssp.key_separators.add(sep);
					if (sep.isEmpty() && i!=0)
						return error("Cant create split string expression. Wrong format. '" + expression + "'");
					sep = "";
				}
			} else {
				if (limiter==1) key += c;
				if (limiter==0) sep += c;
			}
		}
		if (limiter==1)
			return error("Cant create split string expression. Wrong format. '" + expression + "'");
		if (ssp.parse_keys.isEmpty())
			return error("Cant create split string expression. Wrong format. '" + expression + "'");

		return ok(ssp);
	}

	public record Split(String parse_key, String split) {
		@Override public String toString() {
			return parse_key + ":" + split;
		}
	}

	public static class SplitData extends ArrayList<Split> {}
}