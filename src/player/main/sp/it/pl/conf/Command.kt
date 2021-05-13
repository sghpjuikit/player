package sp.it.pl.conf

import java.io.File as FileIo
import sp.it.pl.conf.Command.DoFile.Op
import sp.it.pl.core.Parse
import sp.it.pl.core.Parser
import sp.it.pl.core.ParserOr
import sp.it.pl.core.orMessage
import sp.it.pl.layout.widget.ComponentFactory
import sp.it.pl.layout.widget.ComponentLoaderStrategy
import sp.it.pl.layout.widget.Widget
import sp.it.pl.main.APP
import sp.it.util.action.Action
import sp.it.util.action.ActionRegistrar
import sp.it.util.conf.UnsealedEnumerator
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

   /** [Command] that opens a [sp.it.pl.layout.Component] referred to by an id. */
   data class DoComponentOpen(val loader: ComponentLoaderStrategy, val id: CommandComponentId): Command() {
      constructor(loader: ComponentLoaderStrategy, componentId: String): this(loader, CommandComponentId(componentId))
      constructor(loader: ComponentLoaderStrategy, component: ComponentFactory<*>): this(loader, component.name)
      constructor(loader: ComponentLoaderStrategy, component: Widget): this(loader, component.factory)

      /** [sp.it.pl.ui.objects.window.stage.WindowManager.instantiateComponent] the component with the specified id */
      override operator fun invoke() {
         APP.windowManager.instantiateComponent(id.id)?.net(loader.loader)
      }
   }

   /** [Command] that invokes an [sp.it.util.action.Action] referred to by an id. */
   data class DoAction(val id: CommandActionId): Command() {
      constructor(actionId: String): this(CommandActionId(actionId))
      constructor(action: Action): this(action.name)

      /** [Action.run] action returned by [toAction] */
      override operator fun invoke() = toAction()?.run().toUnit()

      /** @return [Action] with the specified id or null if not found */
      fun toAction(): Action? = ActionRegistrar.getOrNull(id.id)
   }

   data class CommandComponentId(val id: String) {
      companion object: ConverterString<CommandComponentId>, UnsealedEnumerator<CommandComponentId> {
         override fun toS(o: CommandComponentId) = o.id
         override fun ofS(s: String) = Try.ok(CommandComponentId(s))
         override fun enumerateUnsealed() = APP.widgetManager.factories.getComponentFactories().map { CommandComponentId(it.name) }.toList()
      }
   }

   data class CommandActionId(val id: String) {
      companion object: ConverterString<CommandActionId>, UnsealedEnumerator<CommandActionId> {
         override fun toS(o: CommandActionId) = o.id
         override fun ofS(s: String) = Try.ok(CommandActionId(s))
         override fun enumerateUnsealed() = ActionRegistrar.getActions().map { CommandActionId(it.name) }
      }
   }

   companion object: ConverterString<Command> {

      val parser: ParserOr<Command> = Parse.or(
         Parser("command do nothing") { DoNothing },
         Parser("command file", Op::class, java.io.File::class) { DoFile(it[1].asIs(), it[2].asIs()) },
         Parser("command component open", ComponentLoaderStrategy::class, CommandComponentId::class) { DoComponentOpen(it[1].asIs(), it[2].asIs<CommandComponentId>()) },
         Parser("command action", CommandActionId::class) { DoAction(it[1].asIs<CommandActionId>()) },
      )

      override fun ofS(s: String): Try<Command, String> = when {
         s=="command do nothing" -> ok(DoNothing)
         s.startsWith("command file ", true) -> s.substring("command file ".length).let {
            runTry {
               val its = it.split2Partial(" ")
               val op = its.first.uppercase().net(Op::valueOf)
               val file = its.second.net(::FileIo)
               DoFile(op, file)
            }.orMessage()
         }
         s.startsWith("command component open ", true) -> s.substring("command component open ".length).let {
            runTry {
               val its = it.split2Partial(" ")
               val op = its.first.uppercase().net(ComponentLoaderStrategy::valueOf)
               val id = its.second
               DoComponentOpen(op, CommandComponentId(id))
            }.orMessage()
         }
         s.startsWith("command action ", true) -> s.substring("command action ".length).let {
            Try.ok(DoAction(CommandActionId(it)))
         }
         else -> Try.error("Not a valid command, text='$s'")
      }

      override fun toS(o: Command): String = when (o) {
         is DoNothing -> "command do nothing"
         is DoFile -> """command file ${o.op.name.lowercase()} ${o.file.absolutePath}"""
         is DoComponentOpen -> """command component open ${o.loader.name.lowercase()} ${o.id.id}"""
         is DoAction -> """command action ${o.id.id}"""
      }

   }

}