
package Layout.Areas;

import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;

import org.reactfx.Subscription;

import Layout.Component;
import Layout.container.Container;
import Layout.widget.Widget;
import gui.GUI;
import util.SingleR;
import util.SwitchException;
import util.graphics.drag.DragUtil;
import util.graphics.drag.PlaceholderPane;
import util.graphics.fxml.ConventionFxmlLoader;

import static Layout.widget.Widget.LoadType.AUTOMATIC;
import static Layout.widget.Widget.LoadType.MANUAL;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.EXCHANGE;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.LOCK;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.UNLOCK;
import static de.jensd.fx.glyphs.octicons.OctIcon.UNFOLD;
import static gui.GUI.openAndDo;
import static util.dev.Util.noØ;
import static util.functional.Util.isInR;
import static util.functional.Util.listRO;
import static util.functional.Util.mapB;
import static util.graphics.Util.setAnchors;
import static util.reactive.Util.maintain;

/**
 * Graphical wrapper for {@link Widget}. Any widget is always contained in this area. It manages
 * widget's lifecycle, provides user interface for interacting (configuration, etc.) with the
 * widget and is a sole entry point for widget loading.
 * <p>
 * Maintains 1:1 relationship with the widget.
 */
public final class WidgetArea extends Area<Container> {

    @FXML private AnchorPane content;
    @FXML public StackPane content_padding;
    private Subscription s;
    private Widget<?> widget;
    private final SingleR<PlaceholderPane,Widget<?>> passiveLoadPane = new SingleR<>(() -> {
            return new PlaceholderPane(UNFOLD, "", () -> {
                widget.loadType.set(AUTOMATIC);
                loadWidget();
                widget.loadType.set(MANUAL);
            });
        },
        (placeholder,w) -> {
            placeholder.desc.setText("Unfold " + w.custom_name.getValue() + " (Left Click)");
        }
    );

    /**
     @param c container to make contract with
     @param i index of the child within the container
     */
    public WidgetArea(Container c,  int i, Widget widget) {
        super(c,i);
        this.widget = widget;
        this.widget.parentTemp = container;
        this.widget.areaTemp = this;

        // fxml
        new ConventionFxmlLoader(WidgetArea.class, content_root, this).loadNoEx();

        // load controls
        controls = new AreaControls(this);
        content_padding.getChildren().addAll(controls.root);

        // support css styling -
//        content.getStyleClass().setAll(Area.bgr_STYLECLASS);

        // drag
        DragUtil.installDrag(
            root, EXCHANGE, "Switch components",
            DragUtil::hasComponent,
            e -> isInR(DragUtil.getComponent(e), container,widget),
            e -> DragUtil.getComponent(e).swapWith(container,index)
        );

        loadWidget();

        if(GUI.isLayoutMode()) show(); else hide();
    }


    /** @return widget of this area */
    @Override
    public Widget getWidget() {
        return widget;
    }

    /**
     * This implementation returns widget of this area.
     * @return singleton list of this area's only widget. Never null. Never
 containsKey null.
     */
    @Override
    public List<Widget> getActiveWidgets() {
        return listRO(widget);
    }

    private void loadWidget() {
        noØ(widget);

        // We load the widget, but uphold loading type settings. Only user can load widget manually.
        // If the widget is loaded already, we are safe. This is important since widget switching
        // wouldnt load already loaded widget (if set to manual), but when it is loaded already
        // it makes no sense for the widget to not stay loaded.
        if(widget.loadType.get()==AUTOMATIC || widget.isLoaded()) {
            // load widget
            Node wNode = widget.load();
            content.getChildren().clear();
            content.getChildren().add(wNode);
            setAnchors(wNode,0d);
            openAndDo(content_root, null);

            // put controls to new widget
            maintain(widget.custom_name, controls.title.textProperty());
            controls.propB.setDisable(widget.getFields().isEmpty());

            setActivityVisible(false);

            // workaround code
            widget.lockedUnder.initLocked(container);
            if(s!=null) s.unsubscribe();
            s = maintain(widget.locked, mapB(LOCK,UNLOCK),controls.lockB::icon);
        } else
        if (widget.loadType.get()==MANUAL) {
            content.getChildren().clear();
            openAndDo(content_root, null);

            // put controls to new widget
            maintain(widget.custom_name, controls.title.textProperty());
            controls.propB.setDisable(widget.getFields().isEmpty());

            setActivityVisible(false);

            // workaround code
            widget.lockedUnder.initLocked(container);
            if(s!=null) s.unsubscribe();
            s = maintain(widget.locked, mapB(LOCK,UNLOCK),controls.lockB::icon);

            passiveLoadPane.getM(widget).showFor(content);
        } else {
            throw new SwitchException(widget.loadType.get());
        }
    }

    @Override
    public void refresh() {
        widget.getController().refresh();
    }

    @Override
    public void add(Component c) {
        container.addChild(index, c);
    }

    @Override
    public AnchorPane getContent() {
        return content;
    }

    @Override
    public void close() {}
}