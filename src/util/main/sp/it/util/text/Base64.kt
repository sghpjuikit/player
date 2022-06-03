package sp.it.util.text

import java.util.Base64
import sp.it.util.functional.Try
import sp.it.util.functional.runTry

val regexBase64: Regex = """^(?:[A-Za-z\d+/]{4})*(?:[A-Za-z\d+/]{3}=|[A-Za-z\d+/]{2}==)?${'$'}""".toRegex()

fun String.isBase64(): Boolean = regexBase64.matches(this)

fun String.encodeBase64(): String = Base64.getEncoder().encodeToString(toByteArray())

fun ByteArray.encodeBase64(): String = Base64.getEncoder().encodeToString(this)

fun String.decodeBase64(): Try<String, Throwable> = runTry { String(Base64.getDecoder().decode(toByteArray())) }

fun String.decodeBase64ToBytes(): Try<ByteArray, Throwable> = runTry { Base64.getDecoder().decode(toByteArray()) }