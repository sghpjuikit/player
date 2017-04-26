package util;

/**
 * Runtime exception for switch cases that represent programming error and must never execute. This can defend against
 * code modifications, e.g., adding an enum constant. Use in default case, or case that must never execute.
 */
public class SwitchException extends RuntimeException {

	/**
	 * Constructs new switch exception.
	 *
	 * @param switchValue value of the switch case - the object passed into switch statement.
	 */
	public SwitchException(Object switchValue) {
		super("Illegal switch case (unimplemented or forbidden): " + switchValue);
	}
}
