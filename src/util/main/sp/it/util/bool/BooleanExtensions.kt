package sp.it.util.bool

fun Boolean.toByte(t: Byte = 1, f: Byte = 0): Byte = if (this) t else f