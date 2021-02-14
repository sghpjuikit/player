package sp.it.pl.conf

import io.kotest.core.spec.style.FreeSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import java.io.File
import sp.it.pl.conf.Command.DoFile.Op.BROWSE
import sp.it.pl.core.CoreConverter
import sp.it.pl.layout.widget.ComponentLoaderStrategy.DOCK

class CommandTest: FreeSpec({

   Command.Companion::ofS.name {
      forAll(
         row(Command.DoNothing, "command do nothing"),
         row(Command.DoFile(BROWSE, File("C:/test/test.test")), "command file browse C:\\test\\test.test"),
         row(Command.DoComponentOpen(DOCK, "Id"), "command component open dock Id"),
         row(Command.DoAction("Id"), "command action Id")
      ) { command, commandString ->
         Command.ofS(commandString).orThrow shouldBe command
         Command.toS(command) shouldBe commandString
         Command.ofS(Command.toS(command))
      }
   }

   Command.Companion::parser.name {
      CoreConverter.init()
      forAll(
         row(Command.DoNothing, "command do nothing"),
         row(Command.DoFile(BROWSE, File("C:/test/test.test")), "command file browse C:\\test\\test.test"),
         row(Command.DoComponentOpen(DOCK, "Id"), "command component open dock Id"),
         row(Command.DoAction("Id"), "command action Id")
      ) { command, commandString ->
         Command.parser.parse(commandString).orThrow shouldBe command
         Command.toS(command) shouldBe commandString
         Command.parser.parse(Command.toS(command))
      }
   }

})