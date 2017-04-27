package audio.tagging.chapter;

import java.util.Objects;
import javafx.util.Duration;
import util.units.Dur;

/**
 * Chapter is a text associated with specific point of time in a song item.
 * <p/>
 * Chapter always has time and text assigned to it. Time must always be specified
 * while text is "" by default.
 */
public final class Chapter implements Comparable<Chapter> {

	private Dur time;
	private String info = "";

	public Chapter(Duration time) {
		this(time, "");
	}

	public Chapter(Duration time, String _info) {
		setTime(time);
		info = _info;
	}

	/**
	 * Creates new chapter by parsing the string.
	 *
	 * @param text string to parse. Should be a result of {@link #toString()}.
	 * @throws IllegalArgumentException if not parsable String.
	 */
	public Chapter(String text) {
		int i = text.indexOf('-');
		if (i==-1) throw new IllegalArgumentException("Not parsable chapter string: " + text);
		String s = text.substring(0, i);
		try {
			setTimeInMillis(Double.parseDouble(s));
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Not parsable chapter string: " + text);
		}
		info = text.substring(i + 1, text.length());
	}

	/**
	 * Gets time as Duration with granularity of 1 millisecond.
	 */
	public Dur getTime() {
		// time must always be equivalent to mathematical integer
		assert time.toMillis()==Math.rint(time.toMillis());

		return time;
	}

	/**
	 * Sets time. Time denotes the position within the song this chapter is
	 * associated with. It is also a unique identifier and two chapters with the
	 * same time should not exist. They will be considered equal.
	 */
	public void setTime(Duration time) {
		setTimeInMillis(time.toMillis());
	}

	private void setTimeInMillis(double millis) {
		// round to decimal number before assigning
		this.time = new Dur(Math.rint(millis));
	}

	/** Returns the text. Never null. Default value is "". */
	public String getText() {
		return info;
	}

	/** Sets text. */
	public void setText(String info) {
		this.info = info;
	}

	/**
	 * @return true if and only if both chapters and their time is the same.
	 */
	@Override
	public boolean equals(Object o) {
		return this==o || (o instanceof Chapter && getTime().equals(((Chapter) o).getTime()));
	}

	@Override
	public int hashCode() {
		int hash = 3;
		hash = 19*hash + Objects.hashCode(this.time);
		return hash;
	}

	/**
	 * Compares chapter by natural order - by time.
	 */
	@Override
	public int compareTo(Chapter o) {
		return time.compareTo(o.time);
	}

	/**
	 * Use to convert to String, which can be then reconstructed back to Chapter
	 * with {@link #Chapter(java.lang.String)}.
	 * Example: "9800-New Chapter".
	 */
	@Override
	public String toString() {
		return Math.rint(time.toMillis()) + "-" + info;
	}
}