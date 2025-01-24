package sp.it.pl.ui.nodeinfo

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import sp.it.pl.ui.nodeinfo.WeatherInfo.Companion.Types.WindDir
import sp.it.pl.ui.objects.tree.Name.Companion.treeOfPaths

class WeatherInfoTest: FreeSpec({

   WindDir::toCD.name {
      WindDir(0.0).toCD() shouldBe "S"
      WindDir(18.0).toCD() shouldBe "S"
      WindDir(45.0).toCD() shouldBe "SW"
      WindDir(90.0).toCD() shouldBe "W"
      WindDir(135.0).toCD() shouldBe "NW"
      WindDir(180.0).toCD() shouldBe "N"
      WindDir(225.0).toCD() shouldBe "NE"
      WindDir(270.0).toCD() shouldBe "E"
      WindDir(315.0).toCD() shouldBe "SE"
      WindDir(360.0).toCD() shouldBe "S" // bounds
      WindDir(400.0).toCD() shouldBe "SW" // Out of bounds
   }

})