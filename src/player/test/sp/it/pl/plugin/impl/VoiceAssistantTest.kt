package sp.it.pl.plugin.impl

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import sp.it.pl.plugin.impl.VoiceAssistant.Companion.sanitize
import sp.it.util.text.words

class VoiceAssistantTest: FreeSpec({

   "sanitize" {
       "Scene, yellow, blue.".sanitize(setOf()) shouldBe "scene yellow blue"
       "...".sanitize(setOf()) shouldBe ""
       "hey-hey\nhello".sanitize(setOf()) shouldBe "hey-hey hello"
   }

})