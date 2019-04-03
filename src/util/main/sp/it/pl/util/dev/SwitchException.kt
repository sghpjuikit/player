package sp.it.pl.util.dev

/** Java alternative for [sp.it.pl.util.dev.failCase]. */
class SwitchException(switchValue: Any): RuntimeException("Illegal switch case on value: $switchValue")