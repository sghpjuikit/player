/*
 * Implementation based on ControlsFX
 *
 * Copyright (c) 2014, 2015, ControlsFX
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of ControlsFX, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL CONTROLSFX BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package sp.it.pl.gui.objects.popover

import javafx.beans.InvalidationListener
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ChangeListener
import javafx.collections.FXCollections.observableArrayList
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.PopupControl
import javafx.scene.input.KeyCode.ESCAPE
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.MouseEvent.MOUSE_PRESSED
import javafx.scene.input.MouseEvent.MOUSE_RELEASED
import javafx.scene.layout.Pane
import javafx.stage.Window
import javafx.stage.WindowEvent
import javafx.stage.WindowEvent.WINDOW_HIDING
import sp.it.pl.gui.objects.popover.ArrowLocation.BOTTOM_CENTER
import sp.it.pl.gui.objects.popover.ArrowLocation.BOTTOM_LEFT
import sp.it.pl.gui.objects.popover.ArrowLocation.BOTTOM_RIGHT
import sp.it.pl.gui.objects.popover.ArrowLocation.LEFT_BOTTOM
import sp.it.pl.gui.objects.popover.ArrowLocation.LEFT_CENTER
import sp.it.pl.gui.objects.popover.ArrowLocation.LEFT_TOP
import sp.it.pl.gui.objects.popover.ArrowLocation.RIGHT_BOTTOM
import sp.it.pl.gui.objects.popover.ArrowLocation.RIGHT_CENTER
import sp.it.pl.gui.objects.popover.ArrowLocation.RIGHT_TOP
import sp.it.pl.gui.objects.popover.ArrowLocation.TOP_CENTER
import sp.it.pl.gui.objects.popover.ArrowLocation.TOP_LEFT
import sp.it.pl.gui.objects.popover.ArrowLocation.TOP_RIGHT
import sp.it.pl.gui.objects.popover.ScreenUse.APP_WINDOW
import sp.it.pl.main.APP
import sp.it.pl.util.access.v
import sp.it.pl.util.animation.Anim.Companion.anim
import sp.it.pl.util.dev.fail
import sp.it.pl.util.functional.orNull
import sp.it.pl.util.graphics.centre
import sp.it.pl.util.graphics.getScreen
import sp.it.pl.util.graphics.size
import sp.it.pl.util.graphics.toP
import sp.it.pl.util.math.P
import sp.it.pl.util.math.millis
import sp.it.pl.util.reactive.Disposer
import sp.it.pl.util.reactive.onEventUp
import sp.it.pl.util.reactive.sync
import java.util.ArrayList

/**
 * Enhanced popup window.
 *
 * There is a lot of use cases for popover. The content can vary greatly from
 * simple text or icon to a very complex components in which case it is easier
 * to think of the popover as standalone window.
 *
 * Some of the features:
 * * allows specifying the type of content os a generic parameter, which can be changed [PopOver.changeContentType].
 * * has an arrow pointing at the show location
 * * tracking and moving with the parent window or node if it changes position
 * * detached mode. Popover can become detached when dragged by mouse (if it is detachable). Detaching is reversible
 * only if the popover is returned to the original position during the initial mouse drag that detached the popover.
 * Detached popover:
 *     * has no arrow
 *     * can be dragged and moved
 *     * contains a header with title and some controls.
 *     * autohide off
 * * header with automatic visibility. It is visible when at least one of the following applies
 *     * popover is detached
 *     * header icons list is not empty
 *     * title text is not empty
 * * auto-hiding that can be controlled with a pin button
 * * close button with auto-visibility. It is visible when [PopOver.autoHide] is disabled.
 *
 * The popover will hide when window owner hides or when node owner is detached from the
 * scene graph (specifically when its Scene changes) or when node owner's turns
 * invisible.
 *
 * Below are some of the expected use cases along with the recommended
 * configurations:
 * * Informative popover with static content - more sophisticated tooltip
 *     * autohide : true
 *     * hide on click : true
 *     * move with owner : optional
 *     * tip: hide when owner receives any kind of event
 *     * hiding immediately or disabling animations might be considered
 *     * detachable : false
 *     * detached : false or true with no arrow
 * * Small content reactive popover - an alternative to context menus or default popups
 *     * autohide : true
 *     * hide on click : false or false with custom implementation through popover's content
 *     * move with owner : true
 *     * detachable : true
 *     * detached : false
 * * Sophisticated content - alternative to full fledged window
 *     * autohide : false
 *     * hide on click : false
 *     * move with owner : false
 *     * arrow visible : false or depending on context
 *     * detached : true or depending on context
 *
 * Variation tips:
 * * autohide : optional, but opposite of close on click (see [PopOver.hideOnClick])
 * * hide on click : see above or depending on content
 * * move with owner : true
 *
 * @param <N> type of content
 */
open class PopOver<N: Node>(): PopupControl() {


    /** Content of this popover. */
    @JvmField val contentNode = SimpleObjectProperty<N>()
    /**
     * If true, this popover closes on mouse click. Default false.
     *
     * The content of the popover can prevent this behavior by consumes the [MouseEvent.MOUSE_CLICKED] event.
     *
     * Tip: It should only be used when content is static and does not react
     * on mouse events - for example in case of informative popups.
     * Also, it is recommended to use this in conjunction with [.setAutoHide]
     * but with inverted value. One might want to have a popup with reactive
     * content auto-closing when clicked anywhere but on it, or static popup
     * displaying information hovering above its owner, while the owner can be
     * used with the popup open but conveniently closing on mouse click.
     */
    var isHideOnClick: Boolean
        get() = hideOnClick!=null
        set(value) {
            if (value && hideOnClick==null) {
                hideOnClick = EventHandler { if (it.isStillSincePress) hideStrong() }   // close only if was not dragged
                scene.addEventHandler(MOUSE_CLICKED, hideOnClick!!)
            }
            if (!value && hideOnClick!=null) {
                scene.removeEventHandler(MOUSE_CLICKED, hideOnClick!!)
                hideOnClick = null
            }
        }
    private val disposersOnHide = Disposer()

    private var hideOnClick: EventHandler<MouseEvent>? = null
    /** Focus this popover on shown event to receive input events. Use true for interactive content. Default true. */
    @JvmField val focusOnShow = v(true)
    /** Whether this the popover can be detached. */
    @JvmField val detachable = v(true)
    /** Whether this popover is detached so it no longer displays an arrow pointing at the owner node. Default false */
    @JvmField val detached = v(false)
    /** Header visibility. Header contains title and icons. Default true. */
    @JvmField val headerVisible = v(true)
    /** Title text. Default "". */
    @JvmField val title = v("")

    /**
     * Modifiable list of the Nodes displayed in the header.
     * Order is respected (from left to right). Changes will be immediately reflected visually.
     * Use to customize header for example by adding icon or buttons for controlling the content of the popup.
     */
    val headerIcons by lazy { observableArrayList<Node>()!! }

    /** Whether resizing by user is allowed. */
    @JvmField val userResizable = v(true)
    @JvmField var screenPreference = APP_WINDOW
    /**
     * Controls the size of the arrow. 0 effectively disables arrow. Default 12.0.
     *
     * Disabling arrow has an effect of not positioning the popover so the arrow points to the
     * specific point. Rather the popover's upper left corner will.
     */
    @JvmField val arrowSize = SimpleDoubleProperty(9.0)    // TODO: make styleable
    /** Controls the distance between the arrow and the corners of the pop over. Default 12.0. */
    @JvmField val arrowIndent = SimpleDoubleProperty(12.0)   // TODO: make styleable
    /** Returns the corner radius property for the pop over. Default 6.0. */
    @JvmField val cornerRadius = SimpleDoubleProperty(6.0)  // TODO: make styleable
    /** Preferred arrow location. Default [ArrowLocation.LEFT_TOP].*/
    @JvmField val arrowLocation = v(LEFT_TOP)

    /** Whether show/hide is animated. */
    @JvmField var animated = v(true)
    /** Show/hide animation duration. */
    @JvmField var animationDuration = v(300.millis)
    /** Show/hide animation. */
    private val animation by lazy {
        anim {
            getSkinn().node.opacity = it*it
            // getSkinn().node.setScaleXYByTo(it, -20.0, 0.0)  // TODO: causes slight position shift sometimes
        }
    }

    /**
     *  Safely change generic type of this popover. Previous content is cleared.
     *
     * @return this popup with different content type.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T: Node> changeContentType(): PopOver<T> = apply { contentNode.set(null) } as PopOver<T>

    /** @return concrete skin implementation (unlike [PopOver.getSkin]) */
    fun getSkinn(): PopOverSkin = skin as PopOverSkin

    init {
        fun initCloseWithOwner() {
            // JavaFX bug may result in owner being closed before the child (this) -> we need to close immediately
            val d = Disposer()
            disposersOnHide += d
            ownerWindowProperty() sync {
                d()
                if (it!=null) {
                    d += it.onEventUp(WindowEvent.WINDOW_HIDING) { if (isShowing) hideImmediately() }
                    d += it.onEventUp(WindowEvent.WINDOW_CLOSE_REQUEST) { if (isShowing) hideImmediately() }
                }
            }
        }

        styleClass += STYLE_CLASS
        consumeAutoHidingEvents = false
        isAutoFix = false
        skin = createDefaultSkin()
        initCloseWithOwner()
    }

    /**
     * Creates a pop over with the given node as the content node.
     * Sets autoFix and consumeAutoHidingEvents to false.
     *
     * @param content shown by the pop over
     */
    constructor(content: N): this() {
        contentNode.value = content
    }

    /**
     * Creates a pop over with the given node as the content node and provided
     * title text.
     * Sets autoFix and consumeAutoHidingEvents to false.
     *
     * @param content shown by the pop over
     */
    constructor(titleText: String, content: N): this() {
        title.value = titleText
        contentNode.value = content
    }

    public override fun createDefaultSkin() = PopOverSkin(this)

    /**
     * Hides this popup if it is not detached. Otherwise does nothing.
     * Equivalent to: if (!isDetached()) hideStrong();
     * @implNote implementation should not close detached popup
     */
    override fun hide() {
        if (!detached.value) hideStrong()
    }

    /**
     * Hides this popup. If is animated, the hiding animation will be set off,
     * otherwise happens immediately.
     */
    fun hideStrong() {
        if (animated.value) fadeOut()
        else hideImmediately()
    }

    /**
     * Hides this popup immediately.
     * Use when the hiding animation is not desired (regardless the set value)
     * or could cause problems such as delaying application exit.
     *
     * @implSpec This is the default hide implementation overriding the one in the super class. It should not cause any
     * delays and keep changes to hiding mechanism to minimum as otherwise it could cause problems. For example casting
     * javafx.stage.Window to PopOver (when it in fact is an instance of this class) and calling modified hide() method
     * has been observed to cause serious problems.
     */
    fun hideImmediately() {
        disposersOnHide()
        active_popups.remove(this)
        uninstallMoveWith()
        super.hide()
        if (properties.containsKey(KEY_CLOSE_OWNER)) ownerWindow?.hide()
        properties.remove(KEY_CLOSE_OWNER)
    }

    private fun showThis(ownerNode: Node?, ownerWindow: Window) {
        val s = ownerWindow.centre.getScreen()
        maxWidth = s.bounds.width
        maxHeight = s.bounds.height

        // we must set the owners so moving with owner behavior knows which mode should be done
        if (ownerNode!=null) {
            this.ownerMNode = ownerNode
            this.ownerMWindow = ownerNode.scene.window
        } else {
            this.ownerMWindow = ownerWindow
        }

        detached.set(false)

        // show the popup
        super.show(ownerWindow, 0.0, 0.0)
        active_popups.add(this)

        // initialize moving with owner behavior to respect set value
        initializeMovingBehavior(isMoveWithOwner)

        // hideOnESC calls hide(), so there is no effect in detached mode, fix here
        scene.addEventHandler(KEY_PRESSED) {
            if (it.code==ESCAPE && isHideOnEscape) {
                hideStrong()
                it.consume()
            }
        }

        if (animated.value) fadeIn()
    }

    private fun position(position: () -> P) {
        var p = position()
        if (arrowSize.value>0)
            p = p.plus(adjustWindowLocation())

        x = p.x
        y = p.y
        deltaThisX = p.x
        deltaThisY = p.y
    }

    /**
     * Show popup. Makes the pop over visible at the give location and associates it with the specified owner node.
     * The position will be the target location of the arrow of the pop over.
     *
     * @param owner the owning node, which must have a non null getScene().getWindow()
     * @param x the x coordinate for the pop over arrow tip
     * @param y the y coordinate for the pop over arrow tip
     */
    override fun show(owner: Node, x: Double, y: Double) {
        showThis(owner, owner.scene.window)
        position { owner.localToScreen(x, y).toP()+owner.layoutBounds.size/2.0 }
    }

    /** Show popup. Equivalent to: show(owner, 0, 0). */
    fun showInCenterOf(owner: Node) = show(owner, owner.layoutBounds.width/2, owner.layoutBounds.height/2)

    /** Show popup at specified designated position relative to node. */
    fun show(owner: Node, pos: NodePos) {
        showThis(owner, owner.scene.window)

        position({ pos.computeXY(owner, this) })
        x = pos.computeXY(owner, this).x
        y = pos.computeXY(owner, this).y
    }

    /** Show popup at specified screen coordinates. */
    override fun show(window: Window, x: Double, y: Double) {
        showThis(null, window)
        position({ P(x, y) })
    }

    /** Show popup at specified screen coordinates. */
    fun show(window: Window, position: P) = show(window, position.x, position.y)

    /** Show popup at specified screen position. */
    open fun show(pos: ScreenPos) {

        fun ScreenPos.normalize(owner: Window?) = takeIf { owner!=null } ?: toScreenCentric()

        fun grabFocus() {
            ownerWindow?.requestFocus()
            requestFocus()
        }

        fun fixWrongCoordinatesWhenShownWithDifferentContentWhenShowing() {
            if (isShowing) {
                // This solution works but introduces a visual glitch
                // hideImmediately()

                skin.node.applyCss()
                (skin.node as? Pane)?.requestLayout()
                (skin.node as? Pane)?.layout()
                skin.node.autosize()
            }
        }

        arrowSize.value = 0.0
        val ownerF: Window? = APP.windowManager.focused.orNull()?.takeIf { pos.isAppCentric() }?.stage
        val ownerS: Window? = ownerF ?: ownerWindow?.takeIf { it.isShowing }
        val owner: Window = ownerS ?: if (focusOnShow.value) APP.windowManager.createStageOwner() else UNFOCUSED_OWNER
        val wasOwnerCreated = ownerS==null && focusOnShow.value
        val position = pos.normalize(ownerF)

        fixWrongCoordinatesWhenShownWithDifferentContentWhenShowing()
        showThis(null, owner)
        position({ position.computeXY(this) })

        if (focusOnShow.value) grabFocus()
        if (!position.isAppCentric()) uninstallMoveWith()
        if (wasOwnerCreated) properties[KEY_CLOSE_OWNER] = KEY_CLOSE_OWNER
    }

    override fun show(window: Window) {
        showThis(null, window)
        uninstallMoveWith()
    }

    // TODO: fix this computing wrong coordinates
    /* Move the window so that the arrow will end up pointing at the target coordinates. */
    private fun adjustWindowLocation(): P {
        val bounds = this@PopOver.skin.node.layoutBounds
        return when (arrowLocation.value) {
            TOP_CENTER, TOP_LEFT, TOP_RIGHT -> P(
                    computeXOffset(),
                    0.0
            )
            LEFT_TOP, LEFT_CENTER, LEFT_BOTTOM -> P(
                    +bounds.minX+arrowSize.value,
                    +bounds.minY-computeYOffset()
            )
            BOTTOM_CENTER, BOTTOM_LEFT, BOTTOM_RIGHT -> P(
                    +bounds.minX-computeXOffset(),
                    -bounds.minY-bounds.maxY-1.0
            )
            RIGHT_TOP, RIGHT_BOTTOM, RIGHT_CENTER -> P(
                    -bounds.minX-bounds.maxX-1.0,
                    +bounds.minY-computeYOffset()
            )
        }
    }

    fun computeArrowMarginX(): Double = when(arrowLocation.value) {
        LEFT_TOP, LEFT_CENTER, LEFT_BOTTOM -> -arrowSize.value
        RIGHT_TOP, RIGHT_CENTER, RIGHT_BOTTOM -> arrowSize.value
        else -> 0.0
    }

    fun computeArrowMarginY(): Double = when(arrowLocation.value) {
        TOP_LEFT, TOP_CENTER, TOP_RIGHT -> -arrowSize.value
        BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> arrowSize.value
        else -> 0.0
    }

    private fun computeXOffset(): Double = when (arrowLocation.value) {
        TOP_LEFT, BOTTOM_LEFT -> arrowIndent.value+arrowSize.value
        TOP_CENTER, BOTTOM_CENTER -> -this@PopOver.skin.node.layoutBounds.width/2
    //				return getContentNode().prefWidth(-1)/2 + arrowSize.value + arrowIndent.value;
        TOP_RIGHT, BOTTOM_RIGHT -> contentNode.value.prefWidth(-1.0)-arrowIndent.value-arrowSize.value
        else -> 0.0
    }

    private fun computeYOffset(): Double = when (arrowLocation.value) {
        LEFT_TOP, RIGHT_TOP -> arrowIndent.value+arrowSize.value
        LEFT_CENTER, RIGHT_CENTER -> contentNode.value.prefHeight(-1.0)/2+arrowSize.value+arrowIndent.value
        LEFT_BOTTOM, RIGHT_BOTTOM -> contentNode.value.prefHeight(-1.0)-arrowIndent.value-arrowSize.value
        else -> 0.0
    }

    private fun fadeIn() {
        animation.applyNow()
        animation.dur(animationDuration.value)
        animation.playOpenDo(null)
    }

    private fun fadeOut() {
        animation.applyNow()
        animation.dur(animationDuration.value)
        animation.playCloseDo { hideImmediately() }
    }

    /* --------------------- MOVE WITH OWNER ---------------------------------------------------------------------------- */

    /**
     * Sets moving with owner behavior on or off. Default on (true).
     *
     * This functionality is characterized by the popup moving with the owning
     * Window or Node - its positioning relative to its owner becomes absolute.
     *
     * There are two 'modes' decided automatically depending on the way this
     * popover was shown. The position can be absolute relative to the Node or
     * relative to the Window (or none). The first will reposition this popup
     * in order for it to stay with the Node (even when the Node only changes
     * its position within the Window that remained static). Second simply
     * follows the Window and the third does nothing.
     *
     * The 'mode' is decided based on the parameters of the show method that was
     * called.
     * Must never be called before the popover is shown.
     */
    var isMoveWithOwner = true
        set(value) {
            if (value!=isMoveWithOwner) {
                field = value
                if (isShowing) {
                    initializeMovingBehavior(value)
                }
            }
        }

    // owners
    private var ownerMNode: Node? = null
    private var ownerMWindow: Window? = null

    // variables for dragging behavior
    private var deltaX = 0.0
    private var deltaY = 0.0
    private var deltaThisX = 0.0
    private var deltaThisY = 0.0
    private var deltaThisLock = false

    // listeners for relocating
    private val xListener = ChangeListener<Number> { _, _, _ ->
        if (ownerMNode!=null)
            x = deltaThisX+ownerMNode!!.localToScreen(0.0, 0.0).x-deltaX
    }
    private val yListener = ChangeListener<Number> { _, _, _ ->
        if (ownerMNode!=null)
            y = deltaThisY+ownerMNode!!.localToScreen(0.0, 0.0).y-deltaY
    }
    private val winXListener = ChangeListener<Number> { _, _, _ ->
        if (ownerMWindow!=null)
            x = deltaThisX+ownerMWindow!!.x-deltaX
    }
    private val winYListener = ChangeListener<Number> { _, _, _ ->
        if (ownerMWindow!=null)
            y = deltaThisY+ownerMWindow!!.y-deltaY
    }
    private val winHideListener = EventHandler<WindowEvent> { visibilityListener.changed(null, null, null) }

    // monitoring ----------
    // turn lock on/off to prevent dragging to change state
    private var lockOnHandler = EventHandler<MouseEvent> { deltaThisLock = true }
    private var lockOffHandler = EventHandler<MouseEvent> { deltaThisLock = false }
    // remember position for dragging functionality
    private var deltaXListener = InvalidationListener { if (deltaThisLock) deltaThisX = x }
    private var deltaYListener = InvalidationListener { if (deltaThisLock) deltaThisY = y }
    // hide on scene change
    private var visibilityListener = ChangeListener<Scene?> { _, _, _ ->
        uninstallMoveWith()
        hideStrong()
    }
    // hide on node owner setVisible(false)
    private var visibilityListener2 = ChangeListener<Boolean> { _, _, _ ->
        uninstallMoveWith()
        hideStrong()
    }

    private fun initializeMovingBehavior(value: Boolean) {
        if (value) {
            uninstallSoftMoveWith()
            if (ownerMNode!=null) installMoveWithNode(ownerMNode!!)
            else installMoveWithWindow(ownerMWindow!!)
        } else {
            uninstallMoveWith()
        }
    }

    private fun installMoveWithWindow(owner: Window) {
        if (scene.properties.containsKey(KEY_MOVE_WITH_OWNER_WINDOW)) fail("'Move with window' already installed")
        scene.properties.put(KEY_MOVE_WITH_OWNER_WINDOW, KEY_MOVE_WITH_OWNER_WINDOW)

        ownerMNode = null
        ownerMWindow = owner
        ownerMWindow!!.xProperty().addListener(winXListener)
        ownerMWindow!!.yProperty().addListener(winYListener)
        ownerMWindow!!.widthProperty().addListener(winXListener)
        ownerMWindow!!.heightProperty().addListener(winYListener)
        // uninstall just before window gets hidden to prevent possible illegal states
        ownerMWindow!!.addEventFilter(WINDOW_HIDING, winHideListener)
        // remember owner's position to monitor its position change
        deltaX = owner.x
        deltaY = owner.y
        installMonitoring()
    }

    private fun installMoveWithNode(owner: Node) {
        if (scene.properties.containsKey(KEY_MOVE_WITH_OWNER_NODE)) fail("'Move with node' already installed")
        scene.properties.put(KEY_MOVE_WITH_OWNER_NODE, KEY_MOVE_WITH_OWNER_NODE)

        ownerMNode = owner
        ownerMWindow = owner.scene.window
        ownerMWindow!!.xProperty().addListener(xListener)
        ownerMWindow!!.yProperty().addListener(yListener)
        ownerMWindow!!.widthProperty().addListener(xListener)
        ownerMWindow!!.heightProperty().addListener(yListener)
        ownerMNode!!.layoutXProperty().addListener(xListener)
        ownerMNode!!.layoutYProperty().addListener(yListener)
        // uninstall when Node is disconnected from scene graph to prevent possible illegal states
        ownerMNode!!.sceneProperty().addListener(visibilityListener)
        ownerMNode!!.visibleProperty().addListener(visibilityListener2)
        // remember owner's position to monitor its position change
        deltaX = ownerMNode!!.localToScreen(0.0, 0.0).x
        deltaY = ownerMNode!!.localToScreen(0.0, 0.0).y
        installMonitoring()
    }

    private fun uninstallSoftMoveWith() {
        if (ownerMWindow!=null) {
            ownerMWindow!!.xProperty().removeListener(winXListener)
            ownerMWindow!!.yProperty().removeListener(winYListener)
            ownerMWindow!!.widthProperty().removeListener(winXListener)
            ownerMWindow!!.heightProperty().removeListener(winYListener)
            ownerMWindow!!.removeEventFilter(WINDOW_HIDING, winHideListener)
        }
        if (ownerMNode!=null) {
            ownerMNode!!.layoutXProperty().removeListener(xListener)
            ownerMNode!!.layoutYProperty().removeListener(yListener)
            ownerMWindow!!.xProperty().removeListener(xListener)
            ownerMWindow!!.yProperty().removeListener(yListener)
            ownerMWindow!!.widthProperty().removeListener(xListener)
            ownerMWindow!!.heightProperty().removeListener(yListener)
            ownerMNode!!.sceneProperty().removeListener(visibilityListener)
            ownerMNode!!.visibleProperty().removeListener(visibilityListener2)
        }
        scene.properties.remove(KEY_MOVE_WITH_OWNER_NODE)
        scene.properties.remove(KEY_MOVE_WITH_OWNER_WINDOW)
        uninstallMonitoring()
    }

    private fun uninstallMoveWith() {
        uninstallSoftMoveWith()
        ownerMNode = null
        ownerMWindow = null
    }

    private fun installMonitoring() {
        // this should have been handled like this:
        //     deltaThisX.bind(Bindings.when(deltaThisLock).then(xProperty()).otherwise(deltaThisX));
        // but the binding doesn't seem to allow binding to itself or simply 'not
        // do' anything in the 'otherwise' part - there we need to maintain the
        // current value but it does not seem possible - it would really clean
        // this up a bit (way too many listeners for my taste)

        // maintain lock to prevent illegal position monitoring
        scene.addEventHandler(MOUSE_PRESSED, lockOnHandler)
        scene.addEventHandler(MOUSE_RELEASED, lockOffHandler)
        // monitor this' position for change if lock allows
        xProperty().addListener(deltaXListener)
        yProperty().addListener(deltaYListener)
        // fire listeners to initialize the values (listeners dont get fired when added)
        deltaXListener.invalidated(null)
        deltaYListener.invalidated(null)
        deltaThisX = x
        deltaThisY = y
    }

    private fun uninstallMonitoring() {
        scene.removeEventHandler(MOUSE_PRESSED, lockOnHandler)
        scene.removeEventHandler(MOUSE_RELEASED, lockOffHandler)
        xProperty().removeListener(deltaXListener)
        yProperty().removeListener(deltaYListener)
    }

    /* ------------------------------------------------------------------------------------------------------------------ */

    companion object {

        val active_popups = observableArrayList(ArrayList<PopOver<*>>())!!
        private val KEY_CLOSE_OWNER = Any()
        private val KEY_MOVE_WITH_OWNER_NODE = Any()
        private val KEY_MOVE_WITH_OWNER_WINDOW = Any()
        private const val STYLE_CLASS = "popover"

        private val UNFOCUSED_OWNER by lazy { APP.windowManager.createStageOwner() }

    }
}