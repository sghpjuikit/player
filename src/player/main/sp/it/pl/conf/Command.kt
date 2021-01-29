package sp.it.pl.conf

import java.io.File as FileIo
import sp.it.pl.conf.Command.File.Op
import sp.it.pl.layout.widget.ComponentLoaderStrategy
import sp.it.pl.main.APP
import sp.it.util.dev.Blocks
import sp.it.util.file.readTextTry
import sp.it.util.functional.Try
import sp.it.util.functional.Try.Java.ok
import sp.it.util.functional.andAlso
import sp.it.util.functional.net
import sp.it.util.functional.runTry
import sp.it.util.system.browse
import sp.it.util.system.edit
import sp.it.util.system.open
import sp.it.util.system.recycle
import sp.it.util.text.split2Partial

/**
 * A runnable defined by a string ([Command.toS]/[Command.ofS]).
 * Allows defining arbitrary (supported) actions with parameters provided by user.
 */
sealed class Command {

   /** Runs this command. */
   abstract operator fun invoke()

   override fun toString() = toS(this)

   /** [Command] that does nothing. */
   object DoNothing: Command() {
      override fun invoke() = Unit
   }

   /** [Command] that invokes a file operation. */
   data class File(val op: Op, val file: FileIo): Command() {

      override operator fun invoke() {
         when (op) {
            Op.OPEN -> file.open()
            Op.EDIT -> file.edit()
            Op.BROWSE -> file.browse()
            Op.DELETE -> file.deleteRecursively()
            Op.RECYCLE -> file.recycle()
         }
      }

      enum class Op {
         OPEN, EDIT, BROWSE, DELETE, RECYCLE
      }
   }

   /** [Command] that loads a component. */
   data class ComponentOpen(val loader: ComponentLoaderStrategy, val id: String): Command() {
      override fun invoke() {
         APP.windowManager.instantiateComponent(id)?.net(loader.loader)
      }
   }

   companion object {

      fun ofS(s: String): Try<Command, Throwable> = when {
         s=="command do nothing" -> ok(DoNothing)
         s.startsWith("command file ", true) -> s.substring("command file ".length).let {
            runTry {
               val its = it.split2Partial(" ")
               val op = its.first.toUpperCase().net(Op::valueOf)
               val file = its.second.net(::FileIo)
               File(op, file)
            }
         }
         s.startsWith("command component open ", true) -> s.substring("command component open ".length).let {
            runTry {
               val its = it.split2Partial(" ")
               val op = its.first.toUpperCase().net(ComponentLoaderStrategy::valueOf)
               val id = its.second
               ComponentOpen(op, id)
            }
         }
         else -> error(RuntimeException("Not a valid command, text='$s'"))
      }

      fun toS(o: Command): String = when (o) {
         is DoNothing -> "command do nothing"
         is File -> """command file ${o.op.name.toLowerCase()} ${o.file.absolutePath}"""
         is ComponentOpen -> """command component open ${o.loader.name.toLowerCase()} ${o.id}"""
      }

      @Blocks
      fun FileIo.readAsCommand(): Try<Command, Throwable> = readTextTry().andAlso(Command::ofS)

   }

}