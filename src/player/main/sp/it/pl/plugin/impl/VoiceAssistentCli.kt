package sp.it.pl.plugin.impl

import sp.it.pl.main.Bool
import sp.it.util.access.V
import sp.it.util.text.concatApplyBackspace
import sp.it.util.text.toPrintableNonWhitespace

class VoiceAssistentCliReader(val isProgress: V<Bool>) {
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
      if (t.isEmpty())
         return
      if ("COM: System::activity-start" in t) {
         process(t.substringBefore("COM: System::activity-start"), onS, onE)
         isProgress.value = true
         process(t.substringAfter("COM: System::activity-start").drop(1), onS, onE)
         return
      }
      if ("COM: System::activity-stop" in t) {
         process(t.substringBefore("COM: System::activity-stop"), onS, onE)
         isProgress.value = false
         process(t.substringAfter("COM: System::activity-stop").drop(1), onS, onE)
         return
      }

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