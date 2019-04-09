package sp.it.util.dev

/** Java alternative for [sp.it.util.dev.failCase]. */
class SwitchException(switchValue: Any): RuntimeException("Illegal switch case on value: $switchValue")