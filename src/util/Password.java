package util;

public final class Password {
	private String value;

	public Password(String text) {
		set(text);
	}

	public String get() {
		return value;
	}

	public void set(String in) {
		value = in;
	}

	@Override
	public String toString() {
		return value;
	}

	public static Password valueOf(String s) {
		return new Password(s);
	}

}