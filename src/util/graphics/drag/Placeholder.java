/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.graphics.drag;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import org.reactfx.Subscription;

import de.jensd.fx.glyphs.GlyphIcons;
import gui.objects.icon.Icon;

import static util.graphics.Util.layHeaderBottom;
import static util.graphics.Util.removeFromParent;
import static util.reactive.Util.maintain;

/**
 * Pane with a placeholder, mostly showed instead of content when there is none, e.g., "click to
 * add items".
 * <p>
 * Normally it highlights on hover/mouse over to signal possible interaction.
 *
 * @author Plutonium_
 */
public class Placeholder extends StackPane {

    private static final String STYLECLASS = "placeholder-pane";
    private static final String STYLECLASS_ICON = "placeholder-pane-icon";

    public final Icon icon = new Icon().styleclass(STYLECLASS_ICON);
    public final Label desc = new Label();
    private Subscription s;
//    private Pane parent;

    public Placeholder(GlyphIcons icoN, String text, Runnable onClick) {
        icon.icon(icoN);
        icon.onClick(onClick);
        desc.setText(text);
        getStyleClass().add(STYLECLASS);
        setOnMouseClicked(e -> { onClick.run(); e.consume(); });
        getChildren().add(
            layHeaderBottom(8, Pos.CENTER, icon,desc)
        );
    }

    public void showFor(Node n) {
        Pane p = n instanceof Pane ? (Pane)n : n.getParent()==null ? null : (Pane)n.getParent();
        if(p!=null && !p.getChildren().contains(this)) {
//            parent = p;
//            parent.getChildren().forEach(c -> c.setOpacity(0.2));
            p.getChildren().add(this);
            s = maintain(n.layoutBoundsProperty(),b -> {
                double w = b.getWidth();
                double h = b.getHeight();
                setMaxSize(w,h);
                setPrefSize(w,h);
                setMinSize(w,h);
                resizeRelocate(b.getMinX(),b.getMinY(),w,h);
            });
            toFront();
        }
    }

    public void hide() {
//        if(parent!=null) parent.getChildren().forEach(c -> c.setOpacity(1));
        if(s!=null) s.unsubscribe();
        removeFromParent(this);
    }

    public void show(Node n, boolean visible) {
        if(visible) showFor(n);
        else hide();
    }
}