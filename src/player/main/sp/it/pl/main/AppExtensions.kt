package sp.it.pl.main

import sp.it.pl.layout.widget.ComponentFactory
import sp.it.pl.layout.widget.isExperimental
import sp.it.util.file.Util.isValidFile
import sp.it.util.file.div
import sp.it.util.file.nameWithoutExtensionOrRoot
import sp.it.util.file.parentDir
import sp.it.util.ui.EM
import java.io.File

/** @return whether user can use this factory, exactly: APP.developerMode || ![ComponentFactory.isExperimental] */
fun ComponentFactory<*>.isUsableByUser() = APP.developerMode || !isExperimental()

/**
 * Checks validity of a file to be a skin. True return file means the file
 * can be used as a skin (the validity of the skin itself is not included).
 * For files returning false this application will not allow skin change.
 * Valid skin file checks out the following:
 * - not null
 * - isValidFile()
 * - is located in Skins folder set for this application
 * - is .css
 * - is located in its own folder with the same name
 * example: /Skins/MySkin/MySkin.css
 *
 * @return true if parameter is valid skin file. False otherwise or if null.
 */
fun File.isValidSkinFile(): Boolean {
    val name = nameWithoutExtensionOrRoot
    val skinFile = APP.DIR_SKINS/name/"$name.css"
    return isValidFile(this) && path.endsWith(".css") && this==skinFile
}

fun File.isValidWidgetFile(): Boolean {
    return isValidFile(this) && path.endsWith(".fxml") && parentDir?.parentDir==APP.DIR_WIDGETS
}

/** @return value scaled by font size, i.e., value multiplied by the [Number.EM] of current application font. */
fun Number.scaleEM() = toDouble()*APP.ui.font.value.size.EM

/** Runs the specified block immediately or when application is [initialized](App.onStarted). */
fun App.run1AppReady(block: () -> Unit) {
    if (isInitialized.isOk) {
        block()
    } else {
        onStarted += { run1AppReady(block) }
    }
}