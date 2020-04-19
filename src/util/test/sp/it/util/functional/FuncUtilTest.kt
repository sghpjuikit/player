package sp.it.util.functional;

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class FuncUtilTest: FreeSpec({

   "Method" - {

      data class Tr(val name: String, val nodes: List<Tr> = listOf())

      val tr = Tr("1", listOf(
         Tr("2", listOf(
            Tr("5", listOf(
               Tr("8", listOf(
                  Tr("9")
               ))
            ))
         )),
         Tr("3", listOf(
            Tr("6"),
            Tr("7")
         )),
         Tr("4")
      ))

      ::recurse.name {
         tr.recurse { it.nodes }.joinToString(", ") { it.name } shouldBe "1, 2, 5, 8, 9, 3, 6, 7, 4"
      }
      ::recurseDF.name {
         tr.recurseDF { it.nodes }.joinToString(", ") { it.name } shouldBe "1, 2, 5, 8, 9, 3, 6, 7, 4"
      }
      ::recurseBF.name {
         tr.recurseBF { it.nodes }.joinToString(", ") { it.name } shouldBe "1, 2, 3, 4, 5, 6, 7, 8, 9"
      }
   }

})
