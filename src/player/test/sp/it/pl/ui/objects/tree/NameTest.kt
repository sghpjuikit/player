package sp.it.pl.ui.objects.tree

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class NameTest: FreeSpec({

   Name::treeOfPaths.name {
      Name.treeOfPaths("R", setOf("", "A", "A", "B", "B.A", "B.B", "C.A.A")).toString() shouldBe """
         R
           A
           B
             A
             B
           C
             A
               A
      """.trimIndent()
   }

})