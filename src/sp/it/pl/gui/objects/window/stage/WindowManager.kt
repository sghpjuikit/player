package sp.it.pl.gui.objects.window.stage

import javafx.collections.FXCollections.observableArrayList
import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.MouseEvent.MOUSE_ENTERED
import javafx.scene.layout.Region
import javafx.stage.Screen
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.stage.StageStyle.UNDECORATED
import javafx.stage.StageStyle.UTILITY
import javafx.stage.WindowEvent
import javafx.stage.WindowEvent.WINDOW_HIDING
import javafx.stage.WindowEvent.WINDOW_SHOWING
import javafx.util.Duration.ZERO
import mu.KLogging
import sp.it.pl.gui.objects.form.Form
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.gui.objects.popover.PopOver
import sp.it.pl.gui.objects.popover.ScreenPos.APP_CENTER
import sp.it.pl.layout.Component
import sp.it.pl.layout.container.layout.Layout
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.feature.HorizontalDock
import sp.it.pl.layout.widget.orEmpty
import sp.it.pl.main.APP
import sp.it.pl.main.IconFA
import sp.it.pl.main.Settings
import sp.it.pl.main.Widgets.PLAYBACK
import sp.it.pl.util.access.VarEnum
import sp.it.pl.util.access.initAttach
import sp.it.pl.util.access.initSync
import sp.it.pl.util.access.toggle
import sp.it.pl.util.access.v
import sp.it.pl.util.action.IsAction
import sp.it.pl.util.animation.Anim.Companion.anim
import sp.it.pl.util.async.executor.FxTimer.Companion.fxTimer
import sp.it.pl.util.conf.Configurable
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.conf.IsConfigurable
import sp.it.pl.util.conf.between
import sp.it.pl.util.conf.cv
import sp.it.pl.util.conf.readOnlyUnless
import sp.it.pl.util.file.Util.isValidatedDirectory
import sp.it.pl.util.file.div
import sp.it.pl.util.file.seqChildren
import sp.it.pl.util.functional.asIf
import sp.it.pl.util.functional.orNull
import sp.it.pl.util.functional.setTo
import sp.it.pl.util.graphics.Util.addEventHandler1Time
import sp.it.pl.util.graphics.anchorPane
import sp.it.pl.util.graphics.borderPane
import sp.it.pl.util.graphics.fxml.ConventionFxmlLoader
import sp.it.pl.util.graphics.getScreenForMouse
import sp.it.pl.util.graphics.hBox
import sp.it.pl.util.graphics.lay
import sp.it.pl.util.graphics.prefSize
import sp.it.pl.util.graphics.size
import sp.it.pl.util.math.P
import sp.it.pl.util.math.max
import sp.it.pl.util.reactive.on
import sp.it.pl.util.reactive.onChange
import sp.it.pl.util.reactive.onEventDown
import sp.it.pl.util.reactive.onEventUp
import sp.it.pl.util.reactive.onItemAdded
import sp.it.pl.util.reactive.onItemRemoved
import sp.it.pl.util.reactive.sync
import sp.it.pl.util.reactive.syncTo
import sp.it.pl.util.units.millis
import sp.it.pl.util.units.minus
import java.io.File
import java.util.HashSet
import java.util.Optional

@IsConfigurable(value = Settings.Ui.WINDOW)
class WindowManager {

    @JvmField var screenMaxScaling = 0.0
    /** Observable list of all application windows. For list of all windows use [javafx.stage.Stage.getWindows]. */
    @JvmField val windows = observableArrayList<Window>()!!
    /** Dock window or null if none. */
    @JvmField var dockWindow: Window? = null
    /** Main application window, see [sp.it.pl.gui.objects.window.stage.Window.isMain]. */
    private var mainWindow: Window? = null
    private val windowIcons by lazy { APP.getIcons() }

    @IsConfig(name = "Opacity", info = "Window opacity.")
    val windowOpacity by cv(1.0).between(0.0, 1.0)

    @IsConfig(name = "Borderless", info = "Hides window borders.")
    val window_borderless by cv(true)

    @IsConfig(name = "Headerless", info = "Hides window header.")
    val window_headerless by cv(false)

    private var dockIsTogglingWindows = false
    private var dockHiddenWindows = ArrayList<Window>()
    private val dockToggleWindows = v(true).initAttach { v ->
        if (APP.normalLoad) {
            if (v) {
                dockHiddenWindows.forEach { it.show() }
            } else {
                dockHiddenWindows setTo windows.asSequence().filter { it!==dockWindow }
                dockIsTogglingWindows = true
                dockHiddenWindows.forEach { it.hide() }
                dockIsTogglingWindows = false
            }
        }
    }

    @IsConfig(name = "Show delay", group = Settings.Ui.DOCK, info = "Mouse hover time it takes for the dock to show.")
    val dockHoverDelay by cv(700.millis)

    @IsConfig(name = "Hide when inactive", group = Settings.Ui.DOCK, info = "Hide dock when no mouse activity is detected.")
    val dockHideInactive by cv(true)

    @IsConfig(name = "Hide when inactive for", group = Settings.Ui.DOCK, info = "Mouse away time it takes for the dock to hide.")
    val dockHideInactiveDelay by cv(1500.millis).readOnlyUnless(dockHideInactive)

    @IsConfig(name = "Dock content", group = Settings.Ui.DOCK, info = "Widget to use in dock.")
    val dockWidget by cv(PLAYBACK) {
        VarEnum.ofSequence(it) {
            APP.widgetManager.factories.getFactoriesWith<HorizontalDock>().map { it.nameGui() }
        }
    }

    @IsConfig(name = "Dock", group = Settings.Ui.DOCK, info = "Whether application has docked window in the top of the screen.")
    val dockShow by cv(false) { v(it).initSync { showDockImpl(it) } }

    /** @return main window or null if no main window (only possible when no window is open) */
    fun getMain(): Optional<Window> = Optional.ofNullable(mainWindow)

    /** @return focused window or null if none focused */
    fun getFocused(): Optional<Window> = windows.stream().filter { it.focused.value }.findAny()

    /** @return focused window or [getMain] if none focused */
    fun getActive(): Optional<Window> = getFocused().or(::getMain)

    /** @return focused window or [getMain] if none focused or new window if no main window */
    fun getActiveOrNew(): Window = getActive().orElseGet(::createWindow)

    init {
        Stage.getWindows().onItemAdded { w ->
            if (w.properties.containsKey("window") && w.properties["window"]!==dockWindow) {
                windows += w.properties["window"] as Window
            }
        }
        Stage.getWindows().onItemRemoved { w ->
            if (w.properties.containsKey("window")) {
                val window = w.properties["window"] as Window
                if (window.isMain.value && !dockIsTogglingWindows) {
                    APP.close()
                } else {
                    windows -= window
                }
            }
        }

        val computeMaxUsedScaling = {
            screenMaxScaling = Screen.getScreens().asSequence().map { it.outputScaleX max it.outputScaleY }.max() ?: 1.0
        }
        computeMaxUsedScaling()
        Screen.getScreens().onChange { computeMaxUsedScaling() }
    }

    /** Create and open small invisible window with empty content, minimal decoration and no taskbar icon. */
    fun createStageOwner(): Stage {
        return Stage(UTILITY).apply {
            width = 10.0
            height = 10.0
            x = 0.0
            y = 0.0
            opacity = 0.0
            scene = Scene(anchorPane()) // allows child stages (e.g. popup) to receive key events
            show()
        }
    }

    fun create() = create(false)

    fun create(canBeMain: Boolean) = create(null, UNDECORATED, canBeMain)

    fun create(owner: Stage?, style: StageStyle, canBeMain: Boolean): Window {
        val w = Window(owner, style)

        ConventionFxmlLoader(Window::class.java, w.root, w).loadNoEx<Any>()
        if (canBeMain && mainWindow==null) setAsMain(w)

        w.initialize()

        window_borderless syncTo w.isBorderless on w.onClose
        window_headerless syncTo w.isHeaderVisible on w.onClose
        w.stage.title = APP.name
        w.stage.icons setTo windowIcons

        return w
    }

    fun createWindow(canBeMain: Boolean): Window {
        logger.debug { "Creating default window" }

        return create(canBeMain).apply {
            setXYSizeInitial()
            initLayout()
            update()

            show()
            setXYToCenter(getScreenForMouse())
        }
    }

    @IsAction(name = "Open new window", desc = "Opens new application window")
    fun createWindow(): Window = createWindow(false)

    fun setAsMain(w: Window) {
        if (mainWindow===w) return
        mainWindow?.let { it.isMainImpl.value = false }
        mainWindow = w
        mainWindow?.let { it.isMainImpl.value = true }
    }

    @IsAction(name = "Close active window", keys = "CTRL+W", desc = "Opens new application window")
    private fun closeActiveWindow() {
        getActive().orNull()?.close()
    }

    private fun showDockImpl(enable: Boolean) {
        if (!APP.normalLoad) return
        if (!APP.isInitialized.isOk) {
            APP.onStarted += { showDockImpl(enable) }
            return
        }

        if (enable) {
            if (dockWindow!=null && dockWindow!!.isShowing) return

            val mw = dockWindow ?: create(createStageOwner(), UNDECORATED, false).apply {
                dockWindow = this

                resizable.value = true
                isAlwaysOnTop = true

                fun updateSizeAndPos() {
                    val s = Screen.getPrimary()
                    setXYSize(s.bounds.minX, s.bounds.minY, s.bounds.width, height)
                }
                Screen.getScreens().onChange { updateSizeAndPos() } on onClose
                setSize(Screen.getPrimary().bounds.width, 40.0)
                updateSizeAndPos()
            }
            val content = borderPane {
                right = hBox(8.0) {
                    alignment = CENTER_RIGHT
                    isFillHeight = false
                    padding = Insets(5.0, 5.0, 5.0, 25.0)

                    lay += Icon(null, 13.0, "Show/hide other windows").onClickDo { dockToggleWindows.toggle() }.apply {
                        hoverProperty() sync { icon(if (it) IconFA.ANGLE_DOUBLE_DOWN else IconFA.ANGLE_DOWN) } on mw.onClose
                    }
                    lay += Icon(IconFA.CLOSE, 13.0, "Close dock").onClickDo { dockToggleWindows.value = true; dockShow.value = false }
                }
            }
            mw.setContent(content)
            mw.onClose += dockWidget sync {
                val newW = APP.widgetManager.factories.getComponentFactoryByGuiName(it).orEmpty().create()
                content.properties["widget"]?.asIf<Widget>()?.close()
                content.properties["widget"] = newW
                content.center = newW.load()
            }
            mw.onClose += {
                content.properties["widget"]?.asIf<Widget>()?.close()
            }

            // show and apply state
            mw.show()
            mw.isHeaderAllowed.set(false)
            mw.isBorderless.set(true)
            mw.update()
            mw.back.style = "-fx-background-size: cover;" // disallow bgr stretching
            mw.content.style = "-fx-background-color: -fx-pane-color;" // imitate widget area bgr

            // auto-hiding
            val mwh = mw.H.subtract(2) // leave 2 pixels visible
            val mwRoot = mw.stage.scene.root
            val showAnim = anim(200.millis) { mw.setY(-mwh.value*it, false) }

            val hider = fxTimer(ZERO, 1) {
                if (mw.y==0.0) {
                    var d = showAnim.currentTime
                    if (d==ZERO) d = 300.millis-d
                    showAnim.stop()
                    showAnim.rate = 1.0
                    showAnim.playFrom(300.millis-d)
                }
            }
            mwRoot.onEventUp(MouseEvent.ANY) {
                if (mwRoot.isHover) {
                    hider.stop()
                } else {
                    if (dockHideInactive.value)
                        hider.start(dockHideInactiveDelay.value)
                }
            }
            hider.runNow()

            val shower = fxTimer(ZERO, 1) {
                if (mw.y!=0.0 && mwRoot.isHover) {
                    var d = showAnim.currentTime
                    if (d==ZERO) d = 300.millis-d
                    showAnim.stop()
                    showAnim.rate = -1.0
                    showAnim.playFrom(d)
                }
            }
            mwRoot.onEventUp(MOUSE_ENTERED) {
                if (mw.isShowing) shower.start(dockHoverDelay.value)
            }
            mwRoot.onEventDown(MOUSE_CLICKED) {
                if (it.button==PRIMARY) {
                    if (mw.isShowing) shower.runNow()
                }
                if (it.button==SECONDARY) {
                    if (mw.isShowing) hider.runNow()
                }
            }
        } else {
            dockWindow?.close()
            dockWindow = null
        }
    }

    fun serialize() {
        // make sure directory is accessible
        val dir = File(APP.DIR_LAYOUTS, "current")
        if (!isValidatedDirectory(dir)) {
            logger.error { "Serialization of windows and layouts failed. $dir not accessible." }
            return
        }

        val filesOld = dir.seqChildren().toSet()
        val ws = windows.filter { it!==dockWindow }
        logger.info { "Serializing ${ws.size} application windows" }

        // serialize - for now each window to its own file with .ws extension
        val sessionUniqueName = System.currentTimeMillis().toString()
        var isError = false
        val filesNew = HashSet<File>()
        for (i in ws.indices) {
            val w = ws[i]
            val f = dir/"window_${sessionUniqueName}_$i.ws"
            filesNew += f
            isError = isError or APP.serializerXml.toXML(WindowState(w), f).isError
            if (isError) break
        }

        // remove unneeded files, either old or new session will remain
        (if (isError) filesNew else filesOld).forEach { it.delete() }
    }

    fun deserialize(loadNormally: Boolean) {
        val ws = mutableSetOf<Window>()
        if (loadNormally) {
            canBeMainTemp = true

            val dir = File(APP.DIR_LAYOUTS, "current")
            if (isValidatedDirectory(dir)) {
                val fs = dir.seqChildren().filter { it.path.endsWith(".ws") }.toList()
                ws += fs.mapNotNull { APP.serializerXml.fromXML(WindowState::class.java, it).orNull()?.toWindow() }
                canBeMainTemp = false
                logger.info { "Restored ${fs.size}/${ws.size} windows." }
            } else {
                logger.error { "Restoring windows/layouts failed: $dir not accessible." }
                return
            }
        }

        // show windows
        if (ws.isEmpty()) {
            if (loadNormally)
                createWindow(true)
        } else {
            ws.forEach { w -> addEventHandler1Time<WindowEvent>(w.s, WINDOW_SHOWING) { w.update() } }
            ws.forEach { it.show() }
            Widget.deserializeWidgetIO()
        }
    }

    fun showWindow(c: Component): Window {
        return create().apply {
            initLayout()
            setContent(c)
            c.focus()

            show()

            val screen = getScreenForMouse()
            val scrSize = screen.bounds.size
            val initialSize = scrSize/2.0
            val newSize = if (c is Widget) {
                val sizeOld = c.load().asIf<Region>()?.size ?: P(0.0, 0.0)
                val sizePref = c.load().asIf<Region>()?.prefSize ?: P(0.0, 0.0)
                val sizeDiff = sizePref - sizeOld
                P(
                        if (sizePref.x>0) stage.size.x+sizeDiff.x else initialSize.x,
                        if (sizePref.y>0) stage.size.y+sizeDiff.y else initialSize.y
                )
            } else {
                initialSize
            }
            setSize(newSize clipMax scrSize)
            setXYToCenter(screen)
        }
    }

    fun showSettings(c: Configurable<*>, e: MouseEvent) = showSettings(c, e.source as Node)

    fun <T> showSettings(c: Configurable<T>, n: Node) {
        val form = Form.form(c)
        PopOver(form).apply {
            title.value = if (c is Component) "${c.name} Settings" else "Settings"
            arrowSize.value = 0.0 // auto-fix breaks the arrow position, turn off - sux
            isAutoFix = true // we need auto-fix here, because the popup can get rather big
            isAutoHide = true

            showInCenterOf(n)
        }
    }

    fun showFloating(content: Node, title: String): PopOver<*> {
        return PopOver(content).apply {
            this.title.value = title
            this.isAutoFix = false

            val w = getActive().get()
            show(w.stage, w.centerX, w.centerY)
        }
    }

    fun showFloating(c: Widget): PopOver<*> {
        val l = Layout.openStandalone(anchorPane())
        val p = PopOver(l.root).apply {
            title.value = c.info.nameGui()
            isAutoFix = false
        }

        p.onEventUp(WINDOW_HIDING) { l.close() }
        l.child = c
        c.focus()

        p.show(APP_CENTER)
        return p
    }

    fun launchComponent(launcher: File) = instantiateComponent(launcher)
            ?.apply(::launchComponent)

    fun launchComponent(name: String) = APP.widgetManager.factories.getComponentFactoryByGuiName(name)?.create()
            ?.apply(::launchComponent)

    fun launchComponent(w: Component) {
        if (windows.isEmpty()) {
            getActiveOrNew().setContent(w)
        } else {
            showWindow(w)
        }
    }

    fun instantiateComponent(launcher: File): Component? {
        val isLauncherEmpty = launcher.useLines { it.count()==0 }
        val wf = if (isLauncherEmpty) APP.widgetManager.factories.getComponentFactoryByGuiName(launcher.nameWithoutExtension) else null
        return null
                ?: wf?.create()
                ?: APP.serializerXml.fromXML(Component::class.java, launcher).getOr(null)
    }

    companion object: KLogging() {
        @Volatile var canBeMainTemp = false // TODO: make private & best remove altogether
    }

}