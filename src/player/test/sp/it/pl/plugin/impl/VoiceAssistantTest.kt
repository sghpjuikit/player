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
      VoiceAssistant.SpeakHandler("", "lights", { null }).regex.toPattern().pattern() shouldBe "lights"
      VoiceAssistant.SpeakHandler("", "lights?", { null }).regex.toPattern().pattern() shouldBe "(lights)?"
      VoiceAssistant.SpeakHandler("", "lights off", { null }).regex.toPattern().pattern() shouldBe "lights off"
      VoiceAssistant.SpeakHandler("", "lights off?", { null }).regex.toPattern().pattern() shouldBe "lights( off)?"
      VoiceAssistant.SpeakHandler("", "lights off|on?", { null }).regex.toPattern().pattern() shouldBe "lights( off| on)?"
      VoiceAssistant.SpeakHandler("", "close|hide window", { null }).regex.toPattern().pattern() shouldBe "(close|hide) window"
   }

})