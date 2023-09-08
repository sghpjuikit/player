package sp.it.util.type;

public class KClassExtensionsKT41373 {
	public static Runnable method() {
		// We use lambda here but the same error persists if using anonymous class
		return () -> {};
	}
}