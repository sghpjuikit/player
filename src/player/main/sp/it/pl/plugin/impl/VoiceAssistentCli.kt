package sp.it.pl.plugin.impl

import sp.it.util.text.concatApplyBackspace
import sp.it.util.text.toPrintableNonWhitespace

class VoiceAssistentCliReader {
   var state = ""
   var str = StringBuilder("")
   fun String.onS(onS: (String, String?) -> Unit) = if (isNotEmpty()) onS(this, state) else Unit
   fun StringBuilder.determineState(): String {
      return when {
         startsWith("RAW: ") -> "RAW"
         startsWith("USER-RAW: ") -> "USER-RAW"
         startsWith("USER: ") -> "USER"
         startsWith("SYS: ") -> "SYS"
         startsWith("COM: ") -> "COM"
         startsWith("ERR: ") -> "ERR"
         else -> ""
      }
   }
   fun process(t: String, onS: (String, String?) -> Unit, onE: (String, String) -> Unit) {
      var s = t.replace("\r\n", "\n")
      if ("\n" in s) {
         s.split("\n").dropLast(1).forEach { processSingle(it.un(), onS, onE) }
         str.clear()
         str.concatApplyBackspace(s.substringAfterLast("\n").un())
         s.substringAfterLast("\n").un().onS(onS)
      } else {
         val strOld = str.toString()
         str.clear()
         str.concatApplyBackspace(strOld + s.un())
         state = str.determineState()
         s.un().onS(onS)
      }
   }

   fun processSingle(s: String, onS: (String, String?) -> Unit, onE: (String, String) -> Unit) {
      str.concatApplyBackspace(s)
      state = str.determineState()
      if (state.isNotEmpty()) onE(str.toString().substringAfter(": "), state)
      str.clear()
      (s + "\n").onS(onS)
   }

   /** @return this string with unicode newline `\u2028` replaced to `\n` */
   private fun String.un() = replace("\u2028", "\n")
}