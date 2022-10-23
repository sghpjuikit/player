package sp.it.util.system

import sp.it.util.functional.asIs

/** Equivalent to [Runtime.exec] */
fun Runtime.exec(vararg elements: String): Process = exec(arrayOf(elements).asIs<Array<String>>())

/** Equivalent to [Runtime.exec] */
fun Runtime.exec(vararg elements: String, env: Array<String>): Process = exec(arrayOf(elements).asIs<Array<String>>(), env)

/** Equivalent to deprecated [Runtime.exec] that takes entire command as a text */
@Suppress("DEPRECATION")
fun Runtime.execRaw(command: String): Process = exec(command)