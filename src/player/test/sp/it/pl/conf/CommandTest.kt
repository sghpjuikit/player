package sp.it.pl.conf

import io.kotest.core.spec.style.FreeSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import java.io.File
import sp.it.pl.conf.Command.File.Op.BROWSE
import sp.it.pl.layout.widget.ComponentLoaderStrategy.DOCK

class CommandTest: FreeSpec({
   "parses" {
      forAll(
         row(Command.DoNothing, "command do nothing"),
         row(Command.File(BROWSE, File("C:/test/test.test")), "command file browse C:\\test\\test.test"),
         row(Command.ComponentOpen(DOCK, "Id"), "command component open dock Id")
      ) { command, commandString ->
         Command.ofS(commandString).orThrow shouldBe command
         Command.toS(command) shouldBe commandString
         Command.ofS(Command.toS(command))
      }
   }
})