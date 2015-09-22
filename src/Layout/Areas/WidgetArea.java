
package Layout.Areas;

import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;

import Layout.Component;
import Layout.container.Container;
import Layout.widget.Widget;
import gui.GUI;
import gui.pane.IOPane;
import util.graphics.drag.DragUtil;
import util.graphics.fxml.ConventionFxmlLoader;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.EXCHANGE;
import static gui.GUI.openAndDo;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static util.functional.Util.isInR;
import static util.graphics.Util.setAnchors;

/**
 * Implementation of Area for UniContainer.
 */
public final class WidgetArea extends Area<Container> {

    @FXML private AnchorPane content;
    @FXML public StackPane content_padding;

    private Widget widget = Widget.EMPTY();     // never null

    /**
     @param c container to make contract with
     @param i index of the child within the container
     */
    public WidgetArea(Container c, int i) {
        super(c,i);

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

        if(GUI.isLayoutMode()) show(); else hide();
    }


    /** @return currently active widget. */
    public Widget getWidget() {
        return widget;
    }
    /**
     * This implementation returns widget of this area.
     */
    @Override
    public Widget getActiveWidget() {
        return widget;
    }

    /**
     * This implementation returns widget of this area.
     * @return singleton list of this area's only widget. Never null. Never
 containsKey null.
     */
    @Override
    public List<Widget> getActiveWidgets() {
        return singletonList(widget);
    }

    public void loadWidget(Widget w) {
        requireNonNull(w,"widget must not be null");

        widget = w;

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
        wNode.setUserData(this);

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