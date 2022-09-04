package sp.it.pl.main

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.runBlocking
import sp.it.pl.ui.objects.MdNode
import sp.it.pl.ui.pane.ActionData.Threading.BLOCK
import sp.it.pl.ui.pane.action
import sp.it.util.async.runFX
import sp.it.util.functional.orNull
import sp.it.util.system.saveFile
import sp.it.util.text.decodeBase64
import sp.it.util.text.isBase64

/** Denotes actions for [String] */
object AppActionsString {

   val openMarkdownText = action<String>("Open markdown", "Opens markdown text.", IconOC.MARKDOWN) { mdText ->
      APP.windowManager.createWindow().apply {
         detachLayout()
         setContent(
            MdNode().apply {
               readText(mdText)
            }
         )
         show()
      }
      Unit
   }

   val saveToFile = action<String>("Save to file...", "Saves the text to a specified file.", IconFA.SAVE, BLOCK) { text ->
      runBlocking {
         val f = suspendCoroutine {
            runFX {
               saveFile("Save as...", null, "name", this@action.window)
                  .ifOk { f -> it.resume(f) }
                  .ifError { _ -> it.resumeWithException(Exception("Canceled")) }
            }
         }
         f.writeText(text)
      }
   }

   val decodeBase64 = action<String>("Decode base64", "Decode Base64", IconFA.EXCHANGE, BLOCK, { it.isBase64() }) {
      it.decodeBase64().orNull().detectContent()
   }

}