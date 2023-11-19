package sp.it.util.text

import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.text.Charsets.UTF_8

class StringSeqKtTest: FreeSpec({

   "InputStream.strings" {
      var unicodeNewline = '\u2028'.toString()
      "\na b\nc\n".byteInputStream(UTF_8).strings(1024).toList() shouldBe listOf("\na b\nc\n")
      "\na b\nc\n".byteInputStream(UTF_8).strings(1).toList() shouldBe listOf("\n", "a", " ","b","\n","c","\n")
      unicodeNewline.byteInputStream(UTF_8).strings(1024).toList() shouldBe listOf(unicodeNewline)
      unicodeNewline.byteInputStream(UTF_8).strings(3).toList() shouldBe listOf(unicodeNewline)
      //unicodeNewline.byteInputStream(UTF_8).strings(1).toList() shouldBe listOf("�", "�", "�")
      "\n$unicodeNewline\n".byteInputStream(UTF_8).strings(1024).toList() shouldBe listOf("\n$unicodeNewline\n")
      "\n$unicodeNewline\n".byteInputStream(UTF_8).strings(1).toList() shouldBe listOf("\n", unicodeNewline, "\n")
   }

   "Sequence<String>.lines" {
      var unicodeNewline = '\u2028'.toString()
      sequenceOf("\nHello\nWorld", "Kotlin\nTest\n").lines().toList() shouldBe listOf("", "Hello", "WorldKotlin", "Test", "")
      sequenceOf("a").lines().toList() shouldBe listOf("a")
      sequenceOf("\n\n").lines().toList() shouldBe listOf("", "", "")
      sequenceOf("\n","\n").lines().toList() shouldBe listOf("", "", "")
      sequenceOf("\n \n").lines().toList() shouldBe listOf("", " ", "")
      sequenceOf("\n ","\n").lines().toList() shouldBe listOf("", " ", "")
      sequenceOf("a", unicodeNewline).lines().toList() shouldBe listOf("a$unicodeNewline")
   }

})
