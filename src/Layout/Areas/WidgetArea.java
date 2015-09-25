
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
import gui.pane.IOPane;
import util.graphics.drag.DragUtil;
import util.graphics.fxml.ConventionFxmlLoader;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.EXCHANGE;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.LOCK;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.UNLOCK;
import static gui.GUI.openAndDo;
import static util.async.Async.runLater;
import static util.dev.Util.noØ;
import static util.functional.Util.isInR;
import static util.functional.Util.listRO;
import static util.functional.Util.mapB;
import static util.graphics.Util.setAnchors;
import static util.reactive.Util.maintain;

/**
 * Implementation of Area for UniContainer.
 */
public final class WidgetArea extends Area<Container> {

    @FXML private AnchorPane content;
    @FXML public StackPane content_padding;

    Subscription s;
    private final Widget w;

    /**
     @param c container to make contract with
     @param i index of the child within the container
     */
    public WidgetArea(Container c,  int i, Widget widget) {
        super(c,i);
        w = widget;

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
            e -> isInR(DragUtil.getComponent(e).child, container,widget),
            e -> container.swapChildren(index,DragUtil.getComponent(e))
        );

        loadWidget();

        if(GUI.isLayoutMode()) show(); else hide();
    }


    /** @return currently active widget. */
    public Widget getWidget() {
        return w;
    }
    /**
     * This implementation returns widget of this area.
     */
    @Override
    public Widget getActiveWidget() {
        return w;
    }

    /**
     * This implementation returns widget of this area.
     * @return singleton list of this area's only widget. Never null. Never
 containsKey null.
     */
    @Override
    public List<Widget> getActiveWidgets() {
        return listRO(w);
    }

    private void loadWidget() {
        noØ(w);

        // load widget
        Node wNode = w.load();
        content.getChildren().clear();
        content.getChildren().add(wNode);
        setAnchors(wNode,0d);
        openAndDo(content_root, null);

        // put controls to new widget
        controls.title.setText(w.getInfo().name());
        controls.propB.setDisable(w.getFields().isEmpty());

        // put up activity node
        IOPane an = new gui.pane.IOPane(w.getController());
        an.setUserData(this);
        setActivityContent(an);
        setActivityVisible(false);
        wNode.setUserData(this); // in effect sets container parent to the widget

        // workaround code
        runLater(() -> {
        w.lockedUnder.initLocked(container);
        });
        if(s!=null) s.unsubscribe();
        s = maintain(w.locked, mapB(LOCK,UNLOCK),controls.lockB::icon);
    }

    @Override
    public void refresh() {
        w.getController().refresh();
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