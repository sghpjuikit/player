/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sp.it.pl.layout.area;

import java.util.ArrayList;
import java.util.List;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.TilePane;
import sp.it.pl.gui.objects.icon.Icon;
import sp.it.pl.gui.objects.window.stage.Window;
import sp.it.pl.layout.Component;
import sp.it.pl.layout.container.Container;
import sp.it.pl.layout.container.bicontainer.BiContainer;
import sp.it.pl.layout.widget.Widget;
import sp.it.pl.layout.widget.WidgetLoader;
import sp.it.pl.main.AppAnimator;
import sp.it.pl.main.Df;
import sp.it.util.animation.Anim;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.CLONE;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.EXCHANGE;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.GAVEL;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.LINK;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.LOCK;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.MAIL_REPLY;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.TH_LARGE;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.TIMES;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.UNLINK;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.UNLOCK;
import static javafx.geometry.NodeOrientation.LEFT_TO_RIGHT;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.util.Duration.millis;
import static sp.it.pl.layout.area.Area.PSEUDOCLASS_DRAGGED;
import static sp.it.pl.main.AppBuildersKt.infoIcon;
import static sp.it.pl.main.AppDragKt.contains;
import static sp.it.pl.main.AppDragKt.get;
import static sp.it.pl.main.AppDragKt.installDrag;
import static sp.it.pl.main.AppDragKt.set;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.util.functional.UtilKt.consumer;
import static sp.it.util.functional.UtilKt.runnable;
import static sp.it.util.reactive.UtilKt.maintain;
import static sp.it.util.ui.Util.setAnchors;

public abstract class ContainerNodeBase<C extends Container<?>> implements ContainerNode {

    private static final String actbTEXT = "Actions\n\n"
        + "Opens action chooser for this container. Browse and run additional non-layout actions "
        + "for this container.";

    protected final C container;
    protected final AnchorPane root = new AnchorPane();
    protected final TilePane icons = new TilePane(4.0, 4.0);
    protected final AnchorPane controls = new AnchorPane(icons);
    protected boolean isAltCon = false;
    protected boolean isAlt = false;

    Icon absB;

    public ContainerNodeBase(C container) {
        this.container = container;

        root.getChildren().add(controls);
        setAnchors(controls, 0d);
        controls.getStyleClass().addAll("container-area-controls");

	// build header buttons
	Icon infoB = infoIcon(
		"Container settings. See icon tooltips."
            + "\nActions:"
            + "\n\tLeft click: visit children"
            + "\n\tRight click: visit parent container"
    ).styleclass("header-icon");
	Icon detachB = new Icon(CLONE, -1, "Detach widget to own window", this::detach).styleclass("header-icon");
	Icon changeB = new Icon(TH_LARGE, -1, "Change widget", () -> {}).styleclass("header-icon");
    Icon actB = new Icon(GAVEL, -1, actbTEXT, () ->
        APP.actionPane.show(Container.class, container)
    ).styleclass("header-icon");
	Icon lockB = new Icon(null, -1, "Lock widget layout", () -> {
	    container.locked.set(!container.locked.get());
	    APP.actionStream.invoke("Widget layout lock");
	}).styleclass("header-icon");
    maintain(container.locked, it -> lockB.icon(it ? LOCK : UNLOCK));
	absB = new Icon(LINK, -1, "Resize widget proportionally", () -> {
	    toggleAbsSize();
	    updateAbsB();
	}).styleclass("header-icon");
	Icon closeB = new Icon(TIMES, -1, "Close widget", () -> {
	    container.close();
	    APP.actionStream.invoke("Close widget");
	}).styleclass("header-icon");
    Icon dragB = new Icon(MAIL_REPLY, -1, "Move widget by dragging").styleclass("header-icon");

    // drag
    installDrag(controls, EXCHANGE, "Switch components",
        e -> contains(e.getDragboard(), Df.COMPONENT),
        e -> get(e.getDragboard(), Df.COMPONENT) == container,
        consumer(e -> get(e.getDragboard(), Df.COMPONENT).swapWith(container.getParent(), container.indexInParent()))
    );

    // not that dragging children will drag those, dragging container
    // will drag whole container with all its children
    EventHandler<MouseEvent> dh = e -> {
        if (e.getButton()==PRIMARY) {   // primary button drag only
            Dragboard db = root.startDragAndDrop(TransferMode.ANY);
            set(db, Df.COMPONENT, container);
            // signal dragging graphically with css
            controls.pseudoClassStateChanged(PSEUDOCLASS_DRAGGED, true);
            e.consume();
        }
    };
    dragB.setOnDragDetected(dh);
    controls.setOnDragDetected(dh);
    // return graphics to normal
    root.setOnDragDone(e -> controls.pseudoClassStateChanged(PSEUDOCLASS_DRAGGED, false));

	icons.setNodeOrientation(LEFT_TO_RIGHT);
	icons.setAlignment(Pos.CENTER_RIGHT);
        icons.setPrefColumns(10);
        icons.setPrefHeight(25);
        AnchorPane.setTopAnchor(icons,0d);
        AnchorPane.setRightAnchor(icons,0d);
        AnchorPane.setLeftAnchor(icons,0d);
        icons.getChildren().addAll(infoB, dragB, absB, lockB, actB, detachB, changeB, closeB);

        controls.setOpacity(0);
        controls.mouseTransparentProperty().bind(controls.opacityProperty().isEqualTo(0));

        // switch container/normal layout mode using right/left click
        root.setOnMouseClicked(e -> {
            // close on right click
            if (isAlt && !isAltCon && e.getButton()==SECONDARY && container.getChildren().isEmpty()){
	            AppAnimator.INSTANCE.closeAndDo(root, runnable(container::close));
                e.consume();
                return;
            }
            if (isAlt && !isAltCon && e.getButton()==SECONDARY) {
                setAltCon(true);
                e.consume();
            }
        });
        controls.setOnMouseClicked(e -> {
            if (isAltCon && e.getButton()==PRIMARY) {
                setAltCon(false);
                e.consume();
            }
        });
    }

    @Override
    public Pane getRoot() {
        return root;
    }

    @Override
    public void show() {
        // Interesting idea, but turns out it is not intuitive. Most of the time, user
        // simply wants to add child, so this gets in the way.
        // go to container if children empty
        // if (container.getChildren().isEmpty())
        //     setAltCon(true);

        isAlt = true;

        container.getChildren().values().forEach(c -> {
            if (c instanceof Container) ((Container)c).show();
            if (c instanceof Widget) {
                ContainerNode ct = ((Widget)c).areaTemp;
                if (ct!=null) ct.show();
            }
        });
    }

    @Override
    public void hide() {
        if (isAltCon) setAltCon(false);
        isAlt = false;

        container.getChildren().values().forEach(c -> {
            if (c instanceof Container) ((Container)c).hide();
            if (c instanceof Widget) {
                ContainerNode ct = ((Widget)c).areaTemp;
                if (ct!=null) ct.hide();
            }
        });
    }

    List<Node> getC() {
        List<Node> o = new ArrayList<>(root.getChildren());
        o.remove(controls);
        return o;
    }


    void setAltCon(boolean b) {
        if (isAltCon==b) return;
        isAltCon = b;
        new Anim(this::applyanim).dur(millis(250)).intpl(b ? x->x : x->1-x).play();
        controls.toFront();
    }

    void applyanim(double at) {
        getC().forEach(c->c.setOpacity(1-0.8*at));
        controls.setOpacity(at);
    }











    private void toggleAbsSize() {
	Container c = container;
	if (c != null && c instanceof BiContainer) {
	    Splitter s = BiContainer.class.cast(c).ui;
	    s.toggleAbsoluteSizeFor(container.indexInParent());
	}
    }

    void updateAbsB() {
	Container c = container;
	if (c != null && c instanceof BiContainer) {
	    boolean l = c.properties.getI("abs_size") != 0;
        absB.icon(l ? UNLINK : LINK);
	    if (!icons.getChildren().contains(absB))
		icons.getChildren().add(5, absB);
	} else
	    icons.getChildren().remove(absB);
    }

    // TODO: fix & merge with Area.detach
    public void detach() {
        if (!container.hasParent()) return;

        Component c = container;
        c.getParent().addChild(c.indexInParent(),null);
        Window w = WidgetLoader.WINDOW.INSTANCE.invoke(c);

        w.setSize(root.getWidth()+10, root.getHeight()+30);
    }

}
