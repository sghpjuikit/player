import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import sp.it.util.file.type.MimeType

class MimeTypesTest: FreeSpec({

   "registers ${MimeType::class} companion constants" {
      MimeType.`application∕vnd·bmi`.name shouldBe "application/vnd.bmi"
   }

   "mimes are constant (delegated property)" {
      MimeType.`application∕mpegurl` shouldBeSameInstanceAs MimeType.`application∕mpegurl`
   }

   "m3u mimes are identical" {
      MimeType.`application∕mpegurl` shouldBeSameInstanceAs MimeType.`application∕vnd·apple·mpegurl`
      MimeType.`application∕mpegurl` shouldBeSameInstanceAs MimeType.`application∕vnd·apple·mpegurl·audio`
      MimeType.`application∕mpegurl` shouldBeSameInstanceAs MimeType.`audio∕mpegurl`
      MimeType.`application∕mpegurl` shouldBeSameInstanceAs MimeType.`audio∕x-mpegurl`
   }

})