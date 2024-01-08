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

   "regex" {
      voiceCommandRegex("lights").toPattern().pattern() shouldBe "lights"
      voiceCommandRegex("lights off").toPattern().pattern() shouldBe "lights off"

      voiceCommandRegex("close|hide window").toPattern().pattern() shouldBe "(close|hide) window"

      voiceCommandRegex("lights?").toPattern().pattern() shouldBe "(lights)?"
      voiceCommandRegex("lights off?").toPattern().pattern() shouldBe "lights( off)?"
      voiceCommandRegex("one two? three").toPattern().pattern() shouldBe "one( two)? three"

      voiceCommandRegex("lights off|on?").toPattern().pattern() shouldBe "lights( off| on)?"

      voiceCommandRegex("speak \$text").toPattern().pattern() shouldBe "speak .*"
   }

})