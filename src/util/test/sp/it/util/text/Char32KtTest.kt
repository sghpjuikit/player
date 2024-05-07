package sp.it.util.text

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlin.streams.toList

class Char32KtTest: FreeSpec({

   "assumptions" {
      0x0008.toChar32() shouldBe 8.toChar32()
      Char32(0x0008) shouldBe Char32(8)
      Char32(0x0008) shouldBe 8.toChar32()
   }

   Char32::toPrintableNonWhitespace.name - {
      "when the character is a space" {
         ' '.toChar32().toPrintableNonWhitespace() shouldBe '·'.toChar32()
      }

      "when the character is a control character (e.g., BACKSPACE)" {
         Char32(8).toPrintableNonWhitespace() shouldBe Char32(8592)
      }

      "when the character is a non-printable Unicode character (e.g., ZERO WIDTH NO-BREAK SPACE)" {
         Char32(0xFEFF).toPrintableNonWhitespace() shouldBe Char32('␣')
      }

      "when the character is a printable character" {
         Char32('A').toPrintableNonWhitespace() shouldBe Char32('A')
      }
      
   }
})
