package sp.it.pl.gui.objects.window.stage

import sp.it.pl.main.APP

internal class WindowState(window: Window) {
    val x = window.X.value!!
    val y = window.Y.value!!
    val w = window.W.value!!
    val h = window.H.value!!
    val resizable = window.resizable.value!!
    val minimized = window.s.iconifiedProperty().value!!
    val fullscreen = window.fullscreen.value!!
    val onTop = window.alwaysOnTop.value!!
    val maximized = window.maximized.value!!
    val layout = window.layout!!

    fun toWindow(): Window {
        val window = APP.windowManager.create(WindowManager.canBeMainTemp)
        window.X.set(x)
        window.Y.set(y)
        window.W.set(w)
        window.H.set(h)
        window.s.isIconified = minimized
        window.MaxProp.set(maximized)
        window.FullProp.set(fullscreen)
        window.resizable.set(resizable)
        window.isAlwaysOnTop = onTop
        window.initLayout(layout)
        return window
    }
}