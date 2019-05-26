package sp.it.pl.main

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.clikt.parameters.types.file
import sp.it.pl.layout.widget.WidgetUse.NEW
import sp.it.pl.layout.widget.feature.ImageDisplayFeature
import sp.it.pl.layout.widget.feature.PlaylistFeature
import sp.it.util.file.hasExtension
import sp.it.util.file.toFileOrNull
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.math.times
import java.io.File
import java.net.URI
import java.net.URLEncoder
import kotlin.text.Charsets.UTF_8

class AppCli {

    fun process(args: List<String>) {
        val allArgs = args.toMutableList()
        val isAllFiles = args.isNotEmpty() && args.all { it.toURIFlexible()!=null }
        if (isAllFiles) allArgs.addAll(0, listOf("--stateless", "open"))

        Cli().main(allArgs)
    }

}

private class Cli: CliktCommand(name = "PlayerFx", invokeWithoutSubcommand = true) {
    val version = versionOption("Application version=${APP.version}")
    val stateless by option(help = "Whether application starts with a state. If true, state is not restored on start or stored on close")
            .flag("stateless-off", default = false)
    val singleton by option(help = "Whether application should close and delegate arguments if there is already running instance")
            .flag("singleton-off", default = true)

    override fun run() {
        APP.loadSingleton = singleton
        APP.loadStateful *= !stateless
    }

    init {
        subcommands(
                object: CliktCommand(name = "open") {
                    val open by option(help = "Open the specified files by this application.")
                    val files by argument(help = "0-N absolute paths to a file").file().multiple()

                    override fun run() {
                        APP.run1AppReady {
                            when {
                                files.all { it.isAudio() } -> {
                                    APP.widgetManager.widgets.use<PlaylistFeature>(NEW) { it.playlist.addFiles(files); it.playlist.playFirstItem() }
                                }
                                files.all { it.isImage() } -> {
                                    if (files.size==1) APP.actions.openImageFullscreen(files.first())
                                    else APP.widgetManager.widgets.use<ImageDisplayFeature>(NEW) { it.showImages(files) }
                                }
                                files.all { it hasExtension "fxwl" } -> {
                                    files.forEach { APP.windowManager.launchComponent(it) }
                                }
                                else -> APP.ui.actionPane.orBuild.show(files)
                            }
                        }
                    }
                }
        )
    }
}

private fun String.toURIFlexible(): File? = null
        ?: runTry { URI.create("file:///"+URLEncoder.encode(this, UTF_8).replace("+", "%20")).toFileOrNull()?.takeIf { it.isAbsolute } }.orNull()
        ?: runTry { URI.create(URLEncoder.encode(this, UTF_8).replace("+", "%20")).toFileOrNull()?.takeIf { it.isAbsolute } }.orNull()
