@file:JvmName("AppUtil")

package sp.it.pl.main

import javafx.application.Application
import sp.it.pl.util.file.Util.isValidatedDirectory
import sp.it.pl.util.file.childOf
import java.io.File
import java.net.URLConnection

lateinit var APP: App

val isAPPInitialized
    get() = ::APP.isInitialized

fun main(args: Array<String>) {
    // Relocate temp & home under working directory
    // It is our principle to leave no trace of ever running on the system
    // User can also better see what the application is doing
    val tmp = File("").absoluteFile.childOf("user", "tmp")
    isValidatedDirectory(tmp)
    System.setProperty("java.io.tmpdir", tmp.absolutePath)
    System.setProperty("user.home", tmp.absolutePath)

    // Disable url caching, which may cause jar files being held in memory
    URLConnection.setDefaultUseCaches("file", false)

    Application.launch(App::class.java, *args)
}