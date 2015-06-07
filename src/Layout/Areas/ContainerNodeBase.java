/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Areas;

import Layout.AltState;
import static Layout.Areas.Area.bgr_STYLECLASS;
import Layout.Container;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.ANCHOR;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.TIMES;
import gui.objects.Icon;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import static javafx.geometry.Pos.*;
import javafx.scene.Node;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import main.App;
import util.Animation.Anim;
import static util.Util.setAnchors;

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
    HBox icons = new HBox(5);
    StackPane ctrls = new StackPane(icons);
    
    public ContainerNodeBase(C container) {
        this.container = container;

        root.getChildren().add(ctrls);
        ctrls.getStyleClass().addAll(bgr_STYLECLASS);
        setAnchors(ctrls, 0);
        StackPane.setAlignment(icons, TOP_RIGHT);
	Icon closeB = new Icon(TIMES, 12, "Close container", () -> {
	    container.close();
	    App.actionStream.push("Close container");
	});
	Icon layB = new Icon(ANCHOR, 12, "Close container", () -> {
	    ((FreeFormArea)this).bestLayout();
	});
        icons.getChildren().addAll(layB,closeB);
        icons.setAlignment(TOP_RIGHT);
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
}
