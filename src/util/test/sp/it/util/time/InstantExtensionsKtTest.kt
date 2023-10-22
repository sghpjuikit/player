package sp.it.util.time

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.time.Instant

class InstantExtensionsKtTest: FreeSpec({

   Instant::isOlderThan.name {
      Instant.now().minus(Duration.ofDays(1)).isOlderThan(Duration.ofHours(12)) shouldBe true
      Instant.now().plus(Duration.ofDays(1)).isOlderThan(Duration.ofHours(12)) shouldBe false
   }

   Instant::isYoungerThan.name {
      Instant.now().plus(Duration.ofDays(1)).isYoungerThan(Duration.ofHours(12)) shouldBe true
      Instant.now().minus(Duration.ofDays(1)).isYoungerThan(Duration.ofHours(12)) shouldBe false
   }

})
