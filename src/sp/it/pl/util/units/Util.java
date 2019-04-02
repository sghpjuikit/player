package sp.it.pl.util.units;

import java.util.List;
import javafx.util.Duration;
import sp.it.pl.util.dev.Dependency;
import sp.it.pl.util.functional.Try;
import static sp.it.pl.util.functional.Try.Java.error;
import static sp.it.pl.util.functional.Try.Java.ok;
import static sp.it.pl.util.functional.Util.split;

public class Util {

	@Dependency("sp.it.pl.util.units.UtilKt.toHMSMs")
	public static Try<Duration,Throwable> durationOfHMSMs(String s) {
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
				return ok(new Duration(sumT));
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
				return ok(new Duration(value));
			else {
				String suffix = s.substring(index);
				switch (suffix) {
					case "ms": return ok(new Duration(value));
					case "s": return ok(new Duration(1000*value));
					case "m": return ok(new Duration(60000*value));
					case "h": return ok(new Duration(3600000*value));
					default: throw new IllegalArgumentException("Must have suffix from [ms|s|m|h]");
				}
			}
		} catch (IllegalArgumentException e) {
			return error(e);
		}
	}
}