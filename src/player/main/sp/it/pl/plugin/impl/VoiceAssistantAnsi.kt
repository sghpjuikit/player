package sp.it.pl.plugin.impl

import java.util.regex.Pattern

/** Ansi escape sequence pattern */
private val ansi = Pattern.compile("\\x1B\\[[0-?]*[ -/]*[@-~]")

/** @return this string without ansi escape sequences */
internal fun String.ansi() = ansi.matcher(this).replaceAll("")

/** Ansi escape sequence pattern */
private val ansiProgress = "\u001B\\[1;32m(.*?)\u001B\\[0m".toRegex()

internal fun String.noAnsiProgress(): String = ansiProgress.replace(this, "")