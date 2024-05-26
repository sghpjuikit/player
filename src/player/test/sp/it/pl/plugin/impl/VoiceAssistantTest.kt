package sp.it.pl.plugin.impl

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.regex.shouldMatch
import io.kotest.matchers.shouldBe
import sp.it.pl.plugin.impl.VoiceAssistant.Companion.sanitize
import sp.it.util.functional.net
import sp.it.util.text.chars32

class VoiceAssistantTest: FreeSpec({

   val arg = "(.+?)"
   fun MatchResult?.arg() = this!!.groupValues.drop(1).first()
   fun MatchResult?.args() = this!!.groupValues.drop(1)

   "sanitize" {
       "Scene, yellow, blue.".sanitize(setOf()) shouldBe "scene, yellow, blue."
       "...".sanitize(setOf()) shouldBe "..."
       "hey-hey\nhello".sanitize(setOf()) shouldBe "hey-hey\nhello"
   }

   "regex quirks" - {
      "matches group" {
         Regex("a(.+)b").matchEntire("awordb").arg() shouldBe "word"
      }
      "matches group with ' ' value" {
         Regex("a(.+)b").matchEntire("a b").arg() shouldBe " "
         Regex("from *$arg *to *$arg").matchEntire("from to 2").args() shouldBe listOf(" ", "2")
      }
      "matches group with '' value" {
         Regex("from *(.*?) *to *(.*?)").matchEntire("from to 2").args() shouldBe listOf("", "2")
      }
      "matcher destructure" {
         Regex("from *(.*?) *to *(.*?)").matchEntire("from 1 to 2")!!.destructured.net { (a, b) -> a to b } shouldBe ("1" to "2")
      }
      "optional group must be non capturing" {
         // else it returns the groups, which is not parameter
         Regex("(prefix)? *(.+) *(?:suffix)?").matchEntire("prefix word suffix").arg() shouldBe "prefix"
         // even if missing, it gives empty result
         Regex("(prefix)? *(.+) *(?:suffix)?").matchEntire("       word suffix").arg() shouldBe ""
      }
      "value group must be non-greedy" {
         Regex("(?:prefix)? *(.+) *(?:suffix)?").matchEntire("prefix word suffix").arg() shouldBe "word suffix"
         Regex("(?:prefix)? *(.+?) *(?:suffix)?").matchEntire("prefix word suffix").arg() shouldBe "word"
      }
      "match result must skip 1st value which is entire match" {
         Regex("A (.*)").matchEntire("A group")!!.groupValues shouldBe listOf("A group", "group")
      }
      "correct regex" - {
         "must use (.+?) non-greedy regex group to capture parameters & other groups are non capturing" {
            // just for documentation
         }
         "handles missing optional groups" {
            Regex("(?:prefix)? *$arg *(?:suffix)?") shouldMatch "word"
            Regex("(?:prefix)? *$arg *(?:suffix)?").matchEntire("word")!!.arg() shouldBe "word"
         }
         "handles single word parameters" {
            Regex("(?:prefix)? *$arg *(?:suffix)?") shouldMatch "prefix word suffix"
            Regex("(?:prefix)? *$arg *(?:suffix)?").matchEntire("prefix word suffix").arg() shouldBe "word"
         }
         "handles multi word parameters with separator ' '" - {
            Regex("(?:prefix)? *$arg *(?:suffix)?") shouldMatch "prefix word1 word2 suffix"
            Regex("(?:prefix)? *$arg *(?:suffix)?").matchEntire("prefix word1 word2 suffix").arg() shouldBe "word1 word2"
         }
         "handles multi word parameters with separator '_'" - {
            Regex("(?:prefix)? *$arg *(?:suffix)?") shouldMatch "prefix word1_word2 suffix"
            Regex("(?:prefix)? *$arg *(?:suffix)?").matchEntire("prefix word1_word2 suffix").arg() shouldBe "word1_word2"
         }
         "handles real world examples" {
            val r = Regex("(?:turn)? *lights *(?:group)? *$arg *(?:on|off)?")

            r.matches("lights group Living_Room on") shouldBe true
            r.matches("lights group Living Room on") shouldBe true
            r.matchEntire("lights group Living_Room on").arg() shouldBe "Living_Room"
            r.matchEntire("lights group Living Room on").arg() shouldBe "Living Room"
         }
      }
   }

   "voiceCommandRegex" - {
      voiceCommandRegex("lights").toString() shouldBe "lights"
      voiceCommandRegex("lights off").toString() shouldBe "lights *off"

      voiceCommandRegex("close|hide window").toString() shouldBe "(?:close|hide) *window"

      voiceCommandRegex("lights?").toString() shouldBe "(?:lights)?"
      voiceCommandRegex("lights off?").toString() shouldBe "lights *(?:off)?"
      voiceCommandRegex("one two? three").toString() shouldBe "one *(?:two)? *three"

      voiceCommandRegex("turn? lights").toString() shouldBe "(?:turn)? *lights"
      voiceCommandRegex("turn? lights on|off?").toString() shouldBe "(?:turn)? *lights *(?:on|off)?"

      voiceCommandRegex("lights off|on?").toString() shouldBe "lights *(?:off|on)?"

      voiceCommandRegex("speak \$text").toString() shouldBe "speak *(.+?)"

      "matching" - {
         "exact match" {
            voiceCommandRegex("yes").matches("yes") shouldBe true
            voiceCommandRegex("yes").matches("no") shouldBe false
         }
         "or" {
            voiceCommandRegex("yes|no").matches("no") shouldBe true
            voiceCommandRegex("yes|no").matches("none") shouldBe false
         }
         "param" - {
            voiceCommandRegex("count from \$number to \$number").matches("count from 1 to 2") shouldBe true
            voiceCommandRegex("count from \$number to \$number").matchEntire("count from 1 to 2").args() shouldBe listOf("1", "2")

            "requires non empty value" {
               voiceCommandRegex("count from \$number to \$number").matches("count from 1 to") shouldBe false
               voiceCommandRegex("count from \$number to \$number").matches("count from to 2") shouldBe true // ideally false, but this is best our regex can do
               voiceCommandRegex("count from \$number to \$number").matchEntire("count from to 2").arg().chars32().toList().map { it.value }
               voiceCommandRegex("count from \$number to \$number").matchEntire("count from to 2").args() shouldBe listOf(" ", "2")
            }
            "hadles '_' and ' ' as word separator" {
               voiceCommandRegex("count from \$number to \$number").matches("count from 1_2 to 3") shouldBe true
               voiceCommandRegex("count from \$number to \$number").matchEntire("count from 1_2 to 3").args() shouldBe listOf("1_2", "3")
               voiceCommandRegex("count from \$number to \$number").matches("count from 1 2 to 3") shouldBe true
               voiceCommandRegex("count from \$number to \$number").matchEntire("count from 1 2 to 3").args() shouldBe listOf("1 2", "3")
            }
         }

      }
   }

   ::voiceCommandNoOptionalParts.name {
      voiceCommandNoOptionalParts("turn lights group \$group_name") shouldBe "turn lights group \$group_name"
      voiceCommandNoOptionalParts("turn? lights group? \$group_name") shouldBe "lights \$group_name"
      voiceCommandNoOptionalParts("turn? lights group? \$group_name? on|off?") shouldBe "lights \$group_name? on|off?"
   }

   ::voiceCommandWithoutComment.name {
      voiceCommandWithoutComment("turn lights group \$group_name") shouldBe "turn lights group \$group_name"
      voiceCommandWithoutComment("turn lights group \$group_name //comment") shouldBe "turn lights group \$group_name"
      voiceCommandWithoutComment("turn lights group \$group_name // comment") shouldBe "turn lights group \$group_name"
      voiceCommandWithoutComment("turn lights group \$group_name // comment // second") shouldBe "turn lights group \$group_name"
   }

   ::voiceCommandCommentOnly.name {
      voiceCommandCommentOnly("turn lights group \$group_name") shouldBe ""
      voiceCommandCommentOnly("turn lights group \$group_name //comment") shouldBe "//comment"
      voiceCommandCommentOnly("turn lights group \$group_name // comment") shouldBe "// comment"
      voiceCommandCommentOnly("turn lights group \$group_name // comment // second") shouldBe "// comment // second"
   }

})