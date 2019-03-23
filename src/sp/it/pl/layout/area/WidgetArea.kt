package sp.it.pl.layout.area

import javafx.scene.layout.AnchorPane
import sp.it.pl.gui.objects.placeholder.Placeholder
import sp.it.pl.layout.container.Container
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.Widget.LoadType.AUTOMATIC
import sp.it.pl.layout.widget.Widget.LoadType.MANUAL
import sp.it.pl.main.APP
import sp.it.pl.main.AppAnimator
import sp.it.pl.main.DelayAnimator
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconOC
import sp.it.pl.util.access.ref.SingleR
import sp.it.pl.util.access.toggle
import sp.it.pl.util.graphics.drag.DragUtil
import sp.it.pl.util.graphics.layFullArea
import sp.it.pl.util.reactive.Disposer
import sp.it.pl.util.reactive.on
import sp.it.pl.util.reactive.sync
import sp.it.pl.util.reactive.syncTo

/**
 * UI allowing user to manage [Widget] instances.
 *
 * Manages widget's lifecycle, provides user interface for interacting (configuration, etc.) with the
 * widget and is a sole entry point for widget loading.
 *
 * Maintains final 1:1 relationship with the widget, always contains exactly 1 final widget.
 */
class WidgetArea: Area<Container<*>> {

    private val content = AnchorPane()
    @JvmField val controls: AreaControls
    private val widget: Widget
    private val disposer = Disposer()
    private val passiveLoadPane = SingleR<Placeholder, Widget>(
            { Placeholder(IconOC.UNFOLD, "") { loadWidget(true) } },
            { ph, w -> ph.desc.text = "Unfold ${w.custom_name.value} (Left Click)" }
    )

    /**
     * Creates area for the container and its child widget at specified child position.
     *
     * @param container widget's parent container
     * @param index index of the widget within the container
     * @param widget widget that will be managed and displayed
     */
    constructor(container: Container<*>, index: Int, widget: Widget): super(container, index) {
        this.widget = widget
        this.widget.parentTemp = this.container
        this.widget.areaTemp = this

        controls = AreaControls(this)
        contentRoot.layFullArea += content
        contentRoot.layFullArea += controls.root

        DragUtil.installDrag(
                root, IconFA.EXCHANGE, "Switch components",
                { e -> DragUtil.hasComponent(e) },
                { e -> DragUtil.getComponent(e).let { it==this.container || it==widget } },
                { e -> DragUtil.getComponent(e).swapWith(this.container, this.index) }
        )

        loadWidget()
        if (APP.ui.isLayoutMode) show() else hide()
    }

    override fun getWidget() = widget

    private fun loadWidget(forceLoading: Boolean = false) {
        disposer()

        when {
            widget.isLoaded || forceLoading || widget.loadType.value==AUTOMATIC -> {
                // load widget
                animation.openAndDo(contentRoot, null)
                content.children.clear()
                content.layFullArea += widget.load()

                // put controls to new widget
                widget.custom_name syncTo controls.title.textProperty() on disposer
                controls.propB.isDisable = widget.fields.isEmpty()

                // workaround code
                widget.lockedUnder.initLocked(container)
                widget.locked sync { controls.lockB.icon(if (it) IconFA.LOCK else IconFA.UNLOCK) } on disposer
            }
            widget.loadType.value==MANUAL -> {
                AppAnimator.closeAndDo(contentRoot) {
                    content.children.clear()
                    animation.openAndDo(contentRoot, null)

                    // put controls to new widget
                    widget.custom_name syncTo controls.title.textProperty() on disposer
                    controls.propB.isDisable = widget.fields.isEmpty()

                    // workaround code
                    widget.lockedUnder.initLocked(container)
                    widget.locked sync { controls.lockB.icon(if (it) IconFA.LOCK else IconFA.UNLOCK) } on disposer

                    passiveLoadPane.getM(widget).showFor(content)
                }
            }
        }
    }

    fun getContent() = content

    fun isUnderLock(): Boolean = widget.lockedUnder.value

    fun toggleLocked() = widget.locked.toggle()

    override fun show() = controls.show()

    override fun hide() = controls.hide()

    fun setStandaloneStyle() {
        contentRoot.styleClass.clear()
        content.styleClass.clear()
    }

    override fun close() = disposer()

    companion object {
        private val animation = DelayAnimator()
    }
}