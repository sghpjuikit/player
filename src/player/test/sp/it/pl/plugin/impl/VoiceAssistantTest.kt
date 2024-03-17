package sp.it.pl.plugin.impl

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import sp.it.pl.plugin.impl.VoiceAssistant.Companion.sanitize

class VoiceAssistantTest: FreeSpec({

   "sanitize" {
       "Scene, yellow, blue.".sanitize(setOf()) shouldBe "scene yellow blue"
       "...".sanitize(setOf()) shouldBe ""
       "hey-hey\nhello".sanitize(setOf()) shouldBe "hey-hey hello"
   }

   "voiceCommandRegex" {
      voiceCommandRegex("lights").toPattern().pattern() shouldBe "lights"
      voiceCommandRegex("lights off").toPattern().pattern() shouldBe "lights *off"

      voiceCommandRegex("close|hide window").toPattern().pattern() shouldBe "(close|hide) *window"

      voiceCommandRegex("lights?").toPattern().pattern() shouldBe "(lights)?"
      voiceCommandRegex("lights off?").toPattern().pattern() shouldBe "lights *(off)?"
      voiceCommandRegex("one two? three").toPattern().pattern() shouldBe "one *(two)? *three"

      voiceCommandRegex("turn? lights").toPattern().pattern() shouldBe "(turn)? *lights"
      voiceCommandRegex("turn? lights on|off?").toPattern().pattern() shouldBe "(turn)? *lights *(on|off)?"

      voiceCommandRegex("lights off|on?").toPattern().pattern() shouldBe "lights *(off|on)?"

      voiceCommandRegex("speak \$text").toPattern().pattern() shouldBe "speak *.*"

      voiceCommandRegex("count from \$number to \$number").toPattern().matcher("count from 1 to 10").matches() shouldBe true
   }


})