package util.units;

import java.util.List;
import javafx.util.Duration;
import util.Util;
import util.dev.Dependency;
import util.parsing.ParsesFromString;
import util.parsing.ParsesToString;
import util.parsing.StringParseStrategy;
import util.parsing.StringParseStrategy.From;
import static util.functional.Util.split;
import static util.parsing.StringParseStrategy.To.TO_STRING_METHOD;

// TODO: remove this entire class, its counter productive

/**
 * Duration with overridden toString method, where it is formatted into
 * minutes:seconds format.* Example: 00:00.
 */
@StringParseStrategy(
		from = From.ANNOTATED_METHOD,
		to = TO_STRING_METHOD,
		exFrom = {IllegalArgumentException.class, NumberFormatException.class}
)
public class Dur extends Duration {
	private static final long serialVersionUID = 11L;

	/** Constructor. Initializes to 0. */
	public Dur() {
		super(0);
	}

	/** Constructor. Initializes to specified value in milliseconds. */
	public Dur(double millis) {
		super(millis);
	}

	/** @return formatted string representation of the duration. */
	@Dependency("Designed to be used in tables and gui. Should be in xx:xx format")
	@ParsesToString
	@Override
	public String toString() {
		return Util.formatDuration(this);
	}

	@ParsesFromString
	public static Dur valueOf(String s) throws NumberFormatException, IllegalArgumentException {

		// try parsing in hh:mm:ss format
		if (s.contains(":")) {
			List<String> ls = split(s, ":");
			int unit = 1000;
			double Σt = 0;
			for (int i = ls.size() - 1; i>=0; i--) {
				if (i<ls.size() - 1) unit *= 60;
				int amount = Integer.parseInt(ls.get(i));
				if ((amount<0) || (unit<=60000 && amount>59))
					throw new IllegalArgumentException("Minutes and seconds must be >0 and <60");
				int t = unit*amount;
				Σt += t;
			}
			return new Dur(Σt);
		}

		// parse normally

		int index = -1;
		for (int i = 0; i<s.length(); i++) {
			char c = s.charAt(i);
			if (!Character.isDigit(c) && c!='.' && c!='-') {
				index = i;
				break;
			}
		}

		double value = Double.parseDouble(index==-1 ? s : s.substring(0, index));

		if (index==-1)
			return new Dur(value);
		else {
			String suffix = s.substring(index);
			if ("ms".equals(suffix)) {
				return new Dur(value);
			} else if ("s".equals(suffix)) {
				return new Dur(1000*value);
			} else if ("m".equals(suffix)) {
				return new Dur(60000*value);
			} else if ("h".equals(suffix)) {
				return new Dur(3600000*value);
			} else {
				throw new IllegalArgumentException("Must have suffix from [ms|s|m|h]");
			}
		}
	}

}