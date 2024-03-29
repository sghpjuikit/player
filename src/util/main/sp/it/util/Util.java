package sp.it.util;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import sp.it.util.text.StringSplitParser;
import sp.it.util.text.StringSplitParser.Split;
import sp.it.util.text.StringSplitParser.SplitData;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toCollection;
import static sp.it.util.Util.StringDirection.FROM_START;
import static sp.it.util.text.StringExtensionsKt.capitalLower;

/** Provides general purpose utility methods. */
@SuppressWarnings("unused")
public interface Util {

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

	/**
	 * Checks and formats String, so it can be safely used for naming a File.
	 * Replaces any {@code '/', '\', ':', '*', '?', '<', '>', '|'} with '_'.
	 *
	 * @return string with any filename forbidden character replaced by '_'
	 */
	static String filenamizeString(String str) {
		return str.replaceAll("[/\\\\:*?<>|]", "_");
	}

	/**
	 * Converts enum constant to more human-readable string.
	 * <ul>
	 * <li> first letter upper case
	 * <li> other letters lower case,
	 * <li> '_' into ' '
	 * </ul>
	 */
	static String enumToHuman(Enum<?> e) {
		return enumToHuman(e.name());
	}

	/** Same as {@link #enumToHuman(java.lang.Enum)}, for String. */
	static String enumToHuman(String s) {
		return capitalLower(s.replace('_', ' '));
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
	 * @param len length to zero-pad to number to zero-pad to
	 * @param ch character to use. Notable characters are: ' ' or '0'
	 * @return left zero-padded string to specified length
	 */
	static String zeroPad(int n, int len, char ch) {
		int diff = len - digits(n);
		if (diff<1) return String.valueOf(n);
		else return String.valueOf(ch).repeat(diff) + n;
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

	enum StringDirection {
		FROM_START,
		FROM_END
	}
}