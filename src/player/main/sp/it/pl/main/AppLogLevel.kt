package sp.it.pl.main

import ch.qos.logback.classic.Level

enum class AppLogLevel(val logback: Level) {
   OFF(Level.OFF),
   ERROR(Level.ERROR),
   WARN(Level.WARN),
   INFO(Level.INFO),
   DEBUG(Level.DEBUG),
   TRACE(Level.TRACE),
   ALL(Level.ALL)
}