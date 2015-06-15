/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Areas;

import Layout.*;
import static Layout.Areas.Area.bgr_STYLECLASS;
import static Layout.Areas.Area.draggedPSEUDOCLASS;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.*;
import gui.GUI;
import gui.objects.Icon;
import gui.objects.Window.stage.ContextManager;
import gui.objects.Window.stage.Window;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import static javafx.geometry.NodeOrientation.RIGHT_TO_LEFT;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.input.Dragboard;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import main.App;
import util.Animation.Anim;
import static util.Util.setAnchors;
import static util.functional.Util.mapB;
import util.graphics.drag.DragUtil;
import static util.reactive.Util.maintain;

/**
 *
 * @author Plutonium_
 */
public abstract class ContainerNodeBase<C extends Container> implements ContainerNode {

    private static final String S = "area-control";
    
    protected final C container;
    protected final AnchorPane root = new AnchorPane();
    protected boolean isAltCon = false;
    protected boolean isAlt = false;
    TilePane icons = new TilePane(8, 8);
    AnchorPane ctrls = new AnchorPane(icons);
    
    
    Icon absB;
    
    public ContainerNodeBase(C container) {
        this.container = container;

        root.getChildren().add(ctrls);
        ctrls.getStyleClass().addAll(bgr_STYLECLASS);
        setAnchors(ctrls, 0);

	// build header buttons
	Icon infoB = new Icon(INFO, 12, "Help", ()->{});
	Icon layB = new Icon(ANCHOR, 12, "Close container", () -> {
	    ((FreeFormArea)this).bestLayout();
	});
	Icon detachB = new Icon(EXTERNAL_LINK_SQUARE, 12, "Detach widget to own window", this::detach);
	Icon changeB = new Icon(TH_LARGE, 12, "Change widget", ()->{});
	Icon propB = new Icon(COGS, 12, "Settings", ()->{});
	Icon lockB = new Icon(null, 12, "Lock widget layout", () -> {
	    container.locked.set(!container.locked.get());
	    App.actionStream.push("Widget layout lock");
	});
        maintain(container.locked, mapB(LOCK,UNLOCK),lockB.icon);
	absB = new Icon(LINK, 12, "Resize widget proportionally", e -> {
	    toggleAbsSize();
	    updateAbsB();
	});
	Icon closeB = new Icon(TIMES, 12, "Close widget", e -> {
	    container.close();
	    App.actionStream.push("Close widget");
	});
        Icon dragB = new Icon(MAIL_REPLY, 12, "Move widget by dragging");
        dragB.setOnDragDetected( e -> {
            if (e.getButton()==PRIMARY) {   // primary button drag only
                Dragboard db = root.startDragAndDrop(TransferMode.ANY);
                DragUtil.setComponent(container.getParent(),container,db);
                // signal dragging graphically with css
                root.pseudoClassStateChanged(draggedPSEUDOCLASS, true);
                e.consume();
            }
        });
        root.setOnDragDetected( e -> {
            if (GUI.isLayoutMode() && e.getButton()==PRIMARY) {   // primary button drag only
                Dragboard db = root.startDragAndDrop(TransferMode.ANY);
                DragUtil.setComponent(container.getParent(),container,db);
                // signal dragging graphically with css
                root.pseudoClassStateChanged(draggedPSEUDOCLASS, true);
                e.consume();
            }
        });
        
        icons.getChildren().addAll(closeB, detachB, changeB, propB, lockB, absB, dragB, layB, infoB);
	icons.setNodeOrientation(RIGHT_TO_LEFT);
	icons.setAlignment(Pos.CENTER_LEFT);
        icons.setPrefColumns(10);
        icons.setPrefHeight(25);
        AnchorPane.setTopAnchor(icons,0d);
        AnchorPane.setRightAnchor(icons,0d);
        AnchorPane.setLeftAnchor(icons,0d);
        
        ctrls.setOpacity(0);
        ctrls.mouseTransparentProperty().bind(ctrls.opacityProperty().isEqualTo(0));
        
        root.addEventFilter(MOUSE_CLICKED, e -> {
            if(isAlt && !isAltCon && e.getButton()==SECONDARY)
                setAltCon(true);
        });
        ctrls.addEventHandler(MOUSE_CLICKED, e -> {
            if(isAltCon && e.getButton()==PRIMARY)
                setAltCon(false);
        });
    }
    
    @Override
    public Pane getRoot() {
        return root;
    }

    @Override
    public void show() {
        if(container.getChildren().isEmpty()) {
            setAltCon(true);
        }
        isAlt = true;
        showChildren();
    }

    @Override
    public void hide() {
        if(isAltCon) setAltCon(false);
        isAlt = false;
        hideChildren();
        getAreas().forEach(AltState::hide);
        
    }

    abstract public void showChildren();
    abstract public void hideChildren();
    abstract Collection<WidgetArea> getAreas();
    
    List<Node> getC() {
        List<Node> o = new ArrayList(root.getChildren());
        o.remove(ctrls);
        return o;
    }
    
    
    void setAltCon(boolean b) {
        if(isAltCon==b) return;
        isAltCon = b;
        new Anim(this::applyanim).dur(250).intpl(b ? x->x : x->1-x).play();
        ctrls.toFront();
    }
    
    void applyanim(double at) {
        getC().forEach(c->c.setOpacity(1-0.8*at));
        ctrls.setOpacity(at);
    }
    
    
    
    
    
    
    
    
    
    
    
    private void toggleAbsSize() {
	Container c = container.getParent();
	if (c != null && c instanceof BiContainer) {
	    Splitter s = BiContainer.class.cast(c).getGraphics();
	    s.toggleAbsoluteSizeFor(container.indexInParent());
	}
    }

    void updateAbsB() {
	Container c = container.getParent();
	if (c != null && c instanceof BiContainer) {
	    boolean l = c.properties.getI("abs_size") == container.indexInParent();
            absB.icon.setValue(l ? UNLINK : LINK);
	    if (!icons.getChildren().contains(absB))
		icons.getChildren().add(5, absB);
	} else
	    icons.getChildren().remove(absB);
    }
    
    public void detach() {
        if(!container.hasParent()) return;
        // get first active component
        Component c = container;
        Container p = container.getParent();
        
        // detach into new window
        // create new window with no content (not even empty widget)
        Window w = ContextManager.showWindow(null);
               // put size to that of a source (also add jeader & border space)
               w.setSize(root.getWidth()+10, root.getHeight()+30);
        // change content
        Layout c2 = w.getLayoutAggregator().getActive();
        Component w2 = c2.getChild();
            // watch out indexOf returns null if param null, but that will not happen here
        int i1 = p.indexOf(c);
        p.swapChildren(c2,i1,w2);
    }
    
}
