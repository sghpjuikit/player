package sp.it.util;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import sp.it.util.text.Char32;
import sp.it.util.text.StringSplitParser;
import sp.it.util.text.StringSplitParser.Split;
import sp.it.util.text.StringSplitParser.SplitData;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.Math.random;
import static java.lang.Math.sqrt;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toCollection;
import static kotlin.text.StringsKt.capitalize;
import static sp.it.util.Util.StringDirection.FROM_START;
import static sp.it.util.dev.FailKt.failIf;

/**
 * Provides general purpose utility methods.
 */
@SuppressWarnings("unused")
public interface Util {

	/**
	 * Returns empty string if string meets is-empty criteria according to {@link #hasNoReadableText(String)}
	 * <br>
	 * More formally: {@code (hasNoText(str)) ? "" : str;}
	 *
	 * @param s input string
	 * @return "" if string should be empty or the string itself
	 */
	static String emptyOr(String s) {
		return hasNoReadableText(s) ? "" : s;
	}

	/**
	 * Inverse version of {@link #hasNoReadableText(String)}
	 */
	static boolean hasReadableText(String s) {
		return !hasNoReadableText(s);
	}

	/**
	 * Broader check for emptiness of String object.
	 * <br/>
	 * Criteria:
	 * <ul> null
	 * <li> String.isEmpty()
	 * <li> whitespace only
	 * <ul/>
	 *
	 * @param s string to check.
	 * @return true iff any of the criteria is met.
	 */
	static boolean hasNoReadableText(String s) {
		if (s==null || s.isEmpty()) return true;

		for (int i = 0; i<s.length(); i++)
			if (!Character.isWhitespace(s.charAt(i)))
				return false;

		return true;
	}

	static String renameAnime(String s) {
		// remove the super annoying '_'
		s = s.replaceAll("_", " ");

		// remove hash
		if (s.endsWith("]") && s.lastIndexOf('[')==s.length() - 10) s = s.substring(0, s.length() - 10);

		// remove fansub group
		String group = null;
		if (s.startsWith("[")) {
			int i = s.indexOf(']');
			if (i!=-1) {
				group = s.substring(0, i + 1);
				s = s.substring(i + 1);
			}
		}

		// remove leading and trailing shit
		s = s.trim();

		// add fansub groups at the end
		if (group!=null) s = s + "." + group;

		return s;
	}

	static String replace1st(String s, Pattern regex, String n) {
		return regex.matcher(s).replaceFirst(n);
	}

	static String replaceAll(String text, String phrase, String with) {
		return text.replace(phrase, with);
	}

	static String replaceAllRegex(String text, Pattern regex, String with) {
		return regex.matcher(text).replaceAll(with);
	}

	static String remove1st(String text, Pattern regex) {
		return regex.matcher(text).replaceFirst("");
	}

	static String removeAll(String text, String phrase) {
		return text.replace(phrase, "");
	}

	static String removeAllRegex(String text, Pattern regex) {
		return regex.matcher(text).replaceAll("");
	}

	static String addText(String text, String added, StringDirection from) {
		return from==FROM_START ? added + text : text + added;
	}

	static String removeChars(String text, int amount, StringDirection from) {
		return from==FROM_START
				? text.substring(clip(0, amount, text.length() - 1))
				: text.substring(0, max(text.length() - amount, 0));
	}

	static String retainChars(String text, int amount, StringDirection from) {
		return from==FROM_START
				? text.substring(0, min(amount, text.length() - 1))
				: text.substring(clip(0, text.length() - amount, text.length() - 1));
	}

	static SplitData split(String text, StringSplitParser splitter) {
		return splitter.applyM(text).entrySet().stream()
				.map(e -> new Split(e.getKey(), e.getValue()))
				.collect(toCollection(SplitData::new));
	}

	static String splitJoin(String t, StringSplitParser splitter, StringSplitParser joiner) {
		Map<String,String> splits = splitter.applyM(t);
		List<String> keys = joiner.parse_keys;
		List<String> seps = joiner.key_separators;
		StringBuilder o = new StringBuilder();
		for (int i = 0; i<keys.size() - 1; i++) {
			if (!splits.containsKey(keys.get(i))) return null;
			o.append(splits.get(keys.get(i)));
			o.append(seps.get(i));
		}
		if (!splits.containsKey(keys.get(keys.size() - 1))) return null;
		o.append(splits.get(keys.get(keys.size() - 1)));
		return o.toString();
	}

	static Char32 charAt(String x, int i, StringDirection dir) {
		return i<0 || i>=x.length() ? null : new Char32(x.codePointAt(dir==FROM_START ? i : x.length() - 1 - i));
	}

	/**
	 * Checks and formats String so it can be safely used for naming a File.
	 * Replaces any {@code '/', '\', ':', '*', '?', '<', '>', '|'} with '_'.
	 *
	 * @return string with any filename forbidden character replaced by '_'
	 */
	static String filenamizeString(String str) {
		return str.replaceAll("[/\\\\:*?<>|]", "_");
	}

	/**
	 * Converts enum constant to 'human readable' string.
	 * <ul>
	 * <li> first letter upper case
	 * <li> other letters lower case,
	 * <li> '_' into ' '
	 * </ul>
	 */
	static String enumToHuman(Enum e) {
		return enumToHuman(e.name());
	}

	/** Same as {@link #enumToHuman(java.lang.Enum)}, for String. */
	static String enumToHuman(String s) {
		return capitalize(s.replace('_', ' ').toLowerCase());
	}

	/**
	 * Invokes {@link java.net.URLEncoder#encode(String, String)} with {@link java.nio.charset.StandardCharsets#UTF_8}.
	 * UTF-8 is url encoding recommended encoding.
	 */
	static String urlEncodeUtf8(String s) {
		return URLEncoder.encode(s, UTF_8);
	}
	/**
	 * UTF-8 is url encoding recommended encoding.
	 */
	static String urlDecodeUtf8(String s) {
		return URLDecoder.decode(s, UTF_8);
	}

	/**
	 * Logarithm.
	 *
	 * @param base of the log
	 * @param number number to calculate log for
	 * @return logarithm of the number
	 */
	static int log(int base, int number) {
		short p = 0;
		while (pow(base, p)<=number)
			p++;
		return p;
	}

	/**
	 * @return the number of digits of the number.
	 */
	static int digits(int number) {
		int x = number;
		int digits = 0;
		while (x>0) {
			x /= 10;
			digits++;
		}
		return digits;
	}

	/**
	 * Creates left zero-padded string - string of a number with '0' added in to
	 * maintain consistency in number of length.
	 *
	 * @param n number to turn onto zero-padded string
	 * @param max number to zero-pad to
	 * @param ch character to use. Notable characters are: ' ' or '0'
	 * @return left zero-padded string to specified length
	 */
	static String zeroPad(int n, int max, char ch) {
		int diff = digits(max) - digits(n);
		StringBuilder prefix = new StringBuilder();
		for (int i = 1; i<=diff; i++)
			prefix.append(ch);
		return prefix.append(n).toString();
	}

	/**
	 * Examples: 9 for 1-10, 99 for 10-99, 999 for numbers 100-999, etc.
	 *
	 * @return largest number of the same decimal length as specified number.
	 */
	static int decMin1(int n) {
		// normally we would do the below
		// return n==0 ? 0 : (int) (pow(10, 1+digits(n))-1);
		// but why not make this perform faster
		if (n==0) return n;
		n = abs(n);
		if (n<10) return 9;
		else if (n<100) return 99;
		else if (n<1000) return 999;
		else if (n<10000) return 9999;
		else if (n<100000) return 99999;
		else if (n<1000000) return 999999;
		else return (int) (pow(10, 1 + digits(n)) - 1);
	}

	/** @return {@code max(min,min(i,max))} */
	static int clip(short min, short i, short max) {
		return max(min, min(i, max));
	}

	/** @return {@code max(min,min(i,max))} */
	static int clip(int min, int i, int max) {
		return max(min, min(i, max));
	}

	/** @return {@code max(min,min(i,max))} */
	static long clip(long min, long i, long max) {
		return max(min, min(i, max));
	}

	/** @return {@code max(min,min(i,max))} */
	static float clip(float min, float i, float max) {
		return max(min, min(i, max));
	}

	/** @return {@code max(min,min(i,max))} */
	static double clip(double min, double i, double max) {
		return max(min, min(i, max));
	}

	/** Returns {@code sqrt(a^2 + b^2)}. */
	static double pyth(double a, double b) {
		return sqrt(a*a + b*b);
	}

	/**
	 * Pops random element from the list, i.e., returns random element from the list and removes it from the list.
	 *
	 * @return random element from the list.
	 * @throws java.lang.RuntimeException if list empty
	 * @apiNote this method has side effects, i.e., mutates the list
	 */
	static <T> T randPopOf(List<T> list) {
		failIf(list.isEmpty());

		int i = (int) Math.floor(random()*list.size());
		T t = list.get(i);
		list.remove(t);
		return t;
	}

	/**
	 * Returns n random elements from the source list. Source list wont be changed.
	 *
	 * @return specified number of random elements from the list
	 * @throws java.lang.RuntimeException if list does not have enough elements
	 */
	static <T> ArrayList<T> randN(int amount, List<T> source) {
		failIf(amount>=source.size());

		ArrayList<T> all = new ArrayList<>(source); // we need a copy
		ArrayList<T> l = new ArrayList<>();
		for (int i = 0; i<amount; i++) l.add(randPopOf(all));
		return l;
	}

	enum StringDirection {
		FROM_START,
		FROM_END
	}
}