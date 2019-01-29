package sp.it.pl.util

/**
 * Runtime exception for switch cases, when expressions or if-else branches that represent programming error and must
 * never execute.
 * This can defend against code modifications, e.g., adding an enum constant.
 */
class SwitchException(switchValue: Any): RuntimeException("Illegal switch case on value: $switchValue")