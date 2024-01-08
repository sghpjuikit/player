package sp.it.pl.plugin.impl

import javafx.scene.input.KeyCode
import javafx.scene.robot.Robot
import sp.it.util.functional.net

// Invoke javafx Robot to invoke ALT+F4
fun invokeAltF4() {
   Robot().apply {
      keyPress(KeyCode.ALT)
      keyPress(KeyCode.F4)
      keyRelease(KeyCode.F4)
      keyRelease(KeyCode.ALT)
   }
}

fun voiceCommandRegex(commandUi: String) = Regex(
   commandUi.net { it ->
      val parts = it.split(" ")
      fun String.ss(i: Int) = if (parts.size<=1 || i==0) "$this" else " $this"
      fun String.rr() = replace("(", "").replace(")", "").replace("?", "")
      parts
         // resolve params
         .map {
            if (it.startsWith("$")) " .*"
            else it
         }
         .mapIndexed { i, p ->
            when {
               p.contains("|") && p.endsWith("?") -> p.rr().net { "(${it.split("|").joinToString("|") { it.ss(i) }})?" }
               p.endsWith("?") -> p.rr().net { "(${it.ss(i)})?" }
               p.contains("|") -> p.rr().net { "($it)".ss(i) }
               else -> p.rr().ss(i)
            }
         }
         .joinToString("").replace("  ", " ")
   }
)