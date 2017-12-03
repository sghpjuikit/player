package sp.it.pl.util.units;

import java.util.List;
import javafx.util.Duration;
import sp.it.pl.util.Util;
import sp.it.pl.util.dev.Dependency;
import sp.it.pl.util.functional.Try;
import static sp.it.pl.util.functional.Util.split;

// TODO: remove class, its counter productive

/**
 * Duration with overridden toString method, where it is formatted into
 * minutes:seconds format.* Example: 00:00.
 */
public class Dur extends Duration {
	private static final long serialVersionUID = 11L;

	public Dur(double millis) {
		super(millis);
	}

	/** @return formatted string representation of the duration. */
	@Dependency("fromString")
	@Override
	public String toString() {
		return Util.formatDuration(this);
	}

	@Dependency("toString")
	public static Try<Dur,Throwable> fromString(String s) {
		try {
			// try parsing in hh:mm:ss format
			if (s.contains(":")) {
				List<String> ls = split(s, ":");
				int unit = 1000;
				double sumT = 0;
				for (int i = ls.size() - 1; i>=0; i--) {
					if (i<ls.size() - 1) unit *= 60;
					int amount = Integer.parseInt(ls.get(i));
					if ((amount<0) || (unit<=60000 && amount>59))
						throw new IllegalArgumentException("Minutes and seconds must be >0 and <60");
					int t = unit*amount;
					sumT += t;
				}
				return Try.ok(new Dur(sumT));
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
				return Try.ok(new Dur(value));
			else {
				String suffix = s.substring(index);
				if ("ms".equals(suffix)) {
					return Try.ok(new Dur(value));
				} else if ("s".equals(suffix)) {
					return Try.ok(new Dur(1000*value));
				} else if ("m".equals(suffix)) {
					return Try.ok(new Dur(60000*value));
				} else if ("h".equals(suffix)) {
					return Try.ok(new Dur(3600000*value));
				} else {
					throw new IllegalArgumentException("Must have suffix from [ms|s|m|h]");
				}
			}
		} catch (IllegalArgumentException e) {
			return Try.error(e);
		}
	}

}