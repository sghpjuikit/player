package sp.it.pl.main

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.output.TermUi.echo
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.validate
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.clikt.parameters.types.file
import mu.KLogging
import sp.it.pl.layout.widget.WidgetUse.NEW
import sp.it.pl.layout.widget.feature.ImageDisplayFeature
import sp.it.pl.layout.widget.feature.PlaylistFeature
import sp.it.util.file.hasExtension
import sp.it.util.file.toFileOrNull
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.math.times
import sp.it.util.units.uri
import java.io.File
import java.net.URI
import java.net.URLEncoder
import kotlin.system.exitProcess
import kotlin.text.Charsets.UTF_8

class AppCli {
   val cli = Cli()

   fun process(args: List<String>) {
      val allArgs = args.toMutableList()
      val isAllFiles = args.isNotEmpty() && args.all { it.toURIFlexible()!=null }
      if (isAllFiles) allArgs.addAll(0, listOf("--stateless=true", "open-files"))

      if (APP.isInitialized.isOk) {
         try {
            cli.parse(allArgs)
         } catch (e: PrintHelpMessage) {
            echo(e.command.getFormattedHelp())
         } catch (e: PrintMessage) {
            echo(e.message)
         } catch (e: UsageError) {
            echo(e.helpMessage(), err = true)
         } catch (e: CliktError) {
            echo(e.message, err = true)
         } catch (e: Abort) {
            echo("Aborted!", err = true)
         }
      } else {
         cli.main(allArgs)
      }
   }

}

class Cli: CliktCommand(
   name = APP.name,
   invokeWithoutSubcommand = true,
   help = "${APP.name} application command-line interface"

) {
   val version = versionOption("Application version=${APP.version}, JDK version=${Runtime.version()}, Kotlin version=${KotlinVersion.CURRENT}")
   val dev by option(help = "Forces development mode to 'true' regardless of the setting")
      .flag()
   val uiless by option(help = "Whether application can run with no ui. If false, at least one window must be open and closing last one will close the app.")
      .convert { it.toBoolean() }.default(false)
   val stateless by option(help = "Whether application starts with a state. If true, state is not restored on start or stored on close.")
      .convert { it.toBoolean() }.default(false)
   val singleton by option(help = "Whether application should close and delegate arguments if there is already running instance")
      .convert { it.toBoolean() }.default(true)

   init {
      context {
         helpFormatter = CliktHelpFormatter(
            showDefaultValues = true,
            requiredOptionMarker = "*",
            showRequiredTag = true
         )
      }
   }

   override fun run() {
      if (!APP.isInitialized.isOk) {
         APP.isSingleton = singleton
         APP.isStateful *= !stateless
         APP.isUiApp *= !uiless
      }
   }

   init {
      subcommands(
         object: CliktCommand(
            name = "open-files",
            help = """
                            ```
                            Open the specified files by this application.
                            - if `user-action-only` is present, an ui choice will be displayed. Otherwise:
                            - if no files is specified"
                              -  if `stateless=false`, application will close
                              -  if `stateless=true`, application will stay open
                            - if all files are audio files, they will be played in a new playlist
                            - if all files are image files, they will be displayed
                            - if all files are .fxwl files, they will be opened as per `open-component-file` command
                            - otherwise `user-action-only` will be considered true
                            ```
                        """
         ) {
            val userActionOnly by option(help = "").flag(default = false)
            val files by argument(help = "0-N absolute paths to a file.").file().multiple().validate {
               it.forEach { it.requireAbsolute() }
            }

            override fun run() = APP.run1AppReady {
               when {
                  userActionOnly -> {
                     APP.ui.actionPane.orBuild.show(files)
                  }
                  files.isEmpty() -> {
                     if (!APP.isInitialized.isOk && !APP.isStateful)
                        exitProcess(0)
                  }
                  files.all { it.isAudio() } -> {
                     APP.widgetManager.widgets.use<PlaylistFeature>(NEW) {
                        it.playlist.addFiles(files)
                        it.playlist.playFirstItem()
                     }
                  }
                  files.all { it.isImage() } -> {
                     if (files.size==1) APP.actions.openImageFullscreen(files.first())
                     else APP.widgetManager.widgets.use<ImageDisplayFeature>(NEW) { it.showImages(files) }
                  }
                  files.all { it hasExtension "fxwl" } -> {
                     files.forEach { APP.windowManager.launchComponent(it) }
                  }
                  else -> {
                     APP.ui.actionPane.orBuild.show(files)
                  }
               }
            }
         },
         object: CliktCommand(
            name = "open-component",
            help = "Open the application component (widget or template) specified by its `factoryId`."
         ) {
            val factoryId by argument(help = "Name of the component.")

            override fun run() = APP.run1AppReady {
               APP.windowManager.launchComponent(factoryId) ?: run {
                  logger.error { "Component with $factoryId not available" }
               }
            }
         },
         object: CliktCommand(
            name = "open-component-file",
            help = "Open the application component (widget or template) specified by launcher file."
         ) {
            val file by argument(
               help = """
                            ```
                            Absolute path to the .fxwl file of the component.
                            The file content is either serialized component or single line with the component's name
                            ```
                        """
            ).file(folderOkay = false, readable = true).validate {
               it.requireAbsolute()
            }

            override fun run() = APP.run1AppReady { APP.windowManager.launchComponent(file) }
         }
      )
   }

   companion object: KLogging()
}

private fun File.requireAbsolute() = require(isAbsolute) { "File must be absolute" }

private fun String.toURIFlexible(): File? = null
   ?: runTry { uri("file:///$this").toAbsoluteFileOrNull() }.orNull()
   ?: runTry { uri("file:///" + URLEncoder.encode(this, UTF_8).replace("+", "%20")).toAbsoluteFileOrNull() }.orNull()
   ?: runTry { uri(this).toAbsoluteFileOrNull() }.orNull()
   ?: runTry { uri(URLEncoder.encode(this, UTF_8).replace("+", "%20")).toAbsoluteFileOrNull() }.orNull()

private fun URI.toAbsoluteFileOrNull() = toFileOrNull()?.takeIf { it.isAbsolute }