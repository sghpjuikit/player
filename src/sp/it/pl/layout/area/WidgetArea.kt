package sp.it.pl.layout.area

import javafx.fxml.FXML
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.StackPane
import sp.it.pl.layout.Component
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
import sp.it.pl.util.graphics.drag.DragUtil
import sp.it.pl.util.graphics.drag.Placeholder
import sp.it.pl.util.graphics.fxml.ConventionFxmlLoader
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

    @FXML private lateinit var content: AnchorPane
    @FXML lateinit var content_padding: StackPane
    private val widget: Widget<*>
    private val disposer = Disposer()
    private val passiveLoadPane = SingleR<Placeholder, Widget<*>>(
            { Placeholder(IconOC.UNFOLD, "") { loadWidget(true) } },
            { ph, w -> ph.desc.text = "Unfold ${w.custom_name.value} (Left Click)" }
    )

    /**
     * Creates area for the container and its child widget at specified child position.
     *
     * @param c widget's parent container
     * @param i index of the widget within the container
     * @param widget widget that will be managed and displayed
     */
    constructor(c: Container<*>, i: Int, widget: Widget<*>): super(c, i) {
        this.widget = widget
        this.widget.parentTemp = container
        this.widget.areaTemp = this

        ConventionFxmlLoader(WidgetArea::class.java, content_root, this).loadNoEx<Any>()

        controls = AreaControls(this)
        content_padding.children += controls.root

        DragUtil.installDrag(
                root, IconFA.EXCHANGE, "Switch components",
                { e -> DragUtil.hasComponent(e) },
                { e -> DragUtil.getComponent(e).let { it==container || it==widget } },
                { e -> DragUtil.getComponent(e).swapWith(container, index) }
        )

        loadWidget()
        if (APP.ui.isLayoutMode) show() else hide()
    }

    override fun getWidget() = widget

    override fun getActiveWidgets() = listOf(widget)

    private fun loadWidget(forceLoading: Boolean = false) {
        disposer()

        when {
            widget.isLoaded || forceLoading || widget.loadType.get()==AUTOMATIC -> {
                    // load widget
                    animation.openAndDo(content_root, null)
                    content.children.clear()
                    content.layFullArea += widget.load()

                    // put controls to new widget
                    widget.custom_name syncTo controls.title.textProperty() on disposer
                    controls.propB.isDisable = widget.fields.isEmpty()

                    setActivityVisible(false)

                    // workaround code
                    widget.lockedUnder.initLocked(container)
                    widget.locked sync { controls.lockB.icon(if (it) IconFA.LOCK else IconFA.UNLOCK) } on disposer
            }
            widget.loadType.get()==MANUAL -> {
                AppAnimator.closeAndDo(content_root, Runnable {
                    content.children.clear()
                    animation.openAndDo(content_root, null)

                    // put controls to new widget
                    widget.custom_name syncTo controls.title.textProperty() on disposer
                    controls.propB.isDisable = widget.fields.isEmpty()

                    setActivityVisible(false)

                    // workaround code
                    widget.lockedUnder.initLocked(container)
                    widget.locked sync { controls.lockB.icon(if (it) IconFA.LOCK else IconFA.UNLOCK) } on disposer

                    passiveLoadPane.getM(widget).showFor(content)
                })

            }
        }
    }

    override fun refresh() = widget.controller.refresh()

    override fun add(c: Component) = container.addChild(index, c)

    override fun getContent() = content

    //TODO: implement properly through pseudoclasses
    fun setStandaloneStyle() {
        content.styleClass.clear()
        content_padding.styleClass.clear()
    }

    override fun close() = disposer()

    companion object {
        private val animation = DelayAnimator()
    }
}