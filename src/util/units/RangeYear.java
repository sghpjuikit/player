package util.units;

import java.time.Year;
import static java.lang.Integer.max;
import static java.lang.Integer.min;

/**
 * Represents range of years. Used for its text representation.
 * Accumulates values into range and stores minimum, maximum and specificness - whether any value
 * the range was created for was unspecified/empty, in this case -1.
 */
public class RangeYear {
	private int min = Integer.MAX_VALUE;
	private int max = Integer.MIN_VALUE;
	private boolean hasUnspecified = false;

	public void accumulate(Integer year) {
		if (year==null) {
			hasUnspecified = true;
		} else {
			min = min(min, year);
			max = max(max, year);
		}
	}

	public boolean isAfter(Year y) {
		return hasSpecific() && min>y.getValue();
	}

	public boolean isBefore(Year y) {
		return hasSpecific() && max<y.getValue();
	}

	public boolean contains(Year y) {
		return hasSpecific() && min<=y.getValue() && max>=y.getValue();
	}

	public boolean isEmpty() {
		return !hasSpecific() && !hasUnSpecific();
	}

	public boolean hasUnSpecific() {
		return hasUnspecified;
	}

	public boolean hasSpecific() {
		return max!=Integer.MIN_VALUE;
	}

	@Override
	public String toString() {
		// has no value
		if (isEmpty()) return "<none>";

		// has 1 specific value
		if (hasSpecific()) {
			if (min==max)
				return (hasUnspecified ? "? " : "") + max;

				// has >1 specific value
			else {
				return min + (hasUnspecified ? " ? " : " - ") + max;
			}
		}

		// has no specific value
		return "";
	}

}