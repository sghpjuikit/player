package sp.it.pl.conf

import java.io.File as FileIo
import sp.it.pl.conf.Command.DoFile.Op
import sp.it.pl.core.Parse
import sp.it.pl.core.Parser
import sp.it.pl.core.ParserOr
import sp.it.pl.core.orMessage
import sp.it.pl.layout.widget.ComponentLoaderStrategy
import sp.it.pl.main.APP
import sp.it.util.action.Action
import sp.it.util.action.ActionRegistrar
import sp.it.util.functional.Try
import sp.it.util.functional.Try.Java.ok
import sp.it.util.functional.asIs
import sp.it.util.functional.net
import sp.it.util.functional.runTry
import sp.it.util.functional.toUnit
import sp.it.util.parsing.ConverterString
import sp.it.util.system.browse
import sp.it.util.system.edit
import sp.it.util.system.open
import sp.it.util.system.recycle
import sp.it.util.text.split2Partial

/**
 * A runnable defined by a string ([Command.toS]/[Command.ofS]).
 * Allows defining arbitrary (supported) actions with parameters provided by user.
 */
sealed class Command: () -> Unit {

   /** Runs this command. */
   abstract override operator fun invoke()

   override fun toString() = toS(this)

   /** [Command] that does nothing. */
   object DoNothing: Command() {
      override fun invoke() = Unit
   }

   /** [Command] that invokes a file operation. */
   data class DoFile(val op: Op, val file: FileIo): Command() {

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

   /** [Command] that opens a [sp.it.pl.layout.Component]. */
   data class DoComponentOpen(val loader: ComponentLoaderStrategy, val id: String): Command() {
      override operator fun invoke() {
         APP.windowManager.instantiateComponent(id)?.net(loader.loader)
      }
   }

   /** [Command] that invokes an [sp.it.util.action.Action]. */
   data class DoAction(val id: String): Command() {
      fun toAction(): Action? = ActionRegistrar.getOrNull(id)
      override operator fun invoke() = toAction()?.run().toUnit()
   }

   companion object: ConverterString<Command> {

      val parser: ParserOr<Command> = Parse.or(
         Parser("command do nothing") { DoNothing },
         Parser("command file", Op::class, java.io.File::class) { DoFile(it[1].asIs(), it[2].asIs()) },
         Parser("command component open", ComponentLoaderStrategy::class, String::class) { DoComponentOpen(it[1].asIs(), it[2].asIs()) },
         Parser("command action", String::class) { DoAction(it[1].asIs()) },
      )

      override fun ofS(s: String): Try<Command, String> = when {
         s=="command do nothing" -> ok(DoNothing)
         s.startsWith("command file ", true) -> s.substring("command file ".length).let {
            runTry {
               val its = it.split2Partial(" ")
               val op = its.first.toUpperCase().net(Op::valueOf)
               val file = its.second.net(::FileIo)
               DoFile(op, file)
            }.orMessage()
         }
         s.startsWith("command component open ", true) -> s.substring("command component open ".length).let {
            runTry {
               val its = it.split2Partial(" ")
               val op = its.first.toUpperCase().net(ComponentLoaderStrategy::valueOf)
               val id = its.second
               DoComponentOpen(op, id)
            }.orMessage()
         }
         s.startsWith("command action ", true) -> s.substring("command action ".length).let {
            Try.ok(DoAction(it))
         }
         else -> Try.error("Not a valid command, text='$s'")
      }

      override fun toS(o: Command): String = when (o) {
         is DoNothing -> "command do nothing"
         is DoFile -> """command file ${o.op.name.toLowerCase()} ${o.file.absolutePath}"""
         is DoComponentOpen -> """command component open ${o.loader.name.toLowerCase()} ${o.id}"""
         is DoAction -> """command action ${o.id}"""
      }

   }

}