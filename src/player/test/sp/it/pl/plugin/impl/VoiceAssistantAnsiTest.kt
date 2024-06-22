package sp.it.pl.plugin.impl

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class VoiceAssistantAnsiTest: FreeSpec({

   String::ansi.name - {
      "should remove ANSI escape sequences from a string" {
         val stringWithANSI = "\u001B[31mRed\u001B[0m Text"
         val stringWithoutANSI = "Red Text"
         stringWithANSI.ansi() shouldBe stringWithoutANSI
      }

      "should leave the string unchanged if it contains no ANSI escape sequences" {
         val normalString = "This is a normal string"
         val unchangedString = "This is a normal string"
         normalString.ansi() shouldBe unchangedString
      }

      "should handle mixed content with and without ANSI escape sequences" {
         val mixedContent = "This is \u001B[33myellow\u001B[0m and this is not."
         val expectedOutput = "This is yellow and this is not."
         mixedContent.ansi() shouldBe expectedOutput
      }

      "should ignore ANSI escape sequences that are not relevant to the removal process" {
         val irrelevantANSI = "This is a string with an irrelevant ANSI escape sequence\u001B[0m."
         val expectedOutput = "This is a string with an irrelevant ANSI escape sequence."
         irrelevantANSI.ansi() shouldBe expectedOutput
      }
   }

   String::noAnsiProgress.name - {

      "String with ANSI progress indicator" {
         val originalText = "Hello\u001B[1;32mWorld\u001B[0m, \u001B[1;32mKotlin\u001B[0mDeveloper!"
         originalText.noAnsiProgress() shouldBe "Hello, Developer!"
      }

      "String without ANSI sequences" {
         val originalText = "This is a normal text"
         originalText.noAnsiProgress() shouldBe "This is a normal text"
      }

      "String with simple ANSI color code" {
         val originalText = "This is a green text\u001B[1;32mGreen\u001B[0m"
         originalText.noAnsiProgress() shouldBe "This is a green text"
      }

      "String with ANSI non progress indicator 1" {
         val originalText = "Processing...\u001B[5m50%\u001B[0m"
         originalText.noAnsiProgress() shouldBe "Processing...\u001B[5m50%\u001B[0m"
      }

      "String with ANSI non progress indicator 2" {
         val originalText = "Downloaded \u001B[1;34m100MB\u001B[0m of 500MB\u001B[0m"
         originalText.noAnsiProgress() shouldBe "Downloaded \u001B[1;34m100MB\u001B[0m of 500MB\u001B[0m"
      }

   }

})