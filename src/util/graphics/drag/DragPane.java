/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.graphics.drag;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import de.jensd.fx.glyphs.GlyphIcons;
import gui.objects.icon.Icon;
import util.SingleⱤ;
import util.dev.Dependency;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.CLIPBOARD;
import static javafx.scene.input.DragEvent.DRAG_EXITED;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static util.graphics.Util.bgr;
import static util.graphics.Util.layHeaderBottom;

/**
 *
 * @author Plutonium_
 */
public class DragPane {

    private static final SingleⱤ<Pane,Data> DRAGPANE = new SingleⱤ<>(() -> {
            Pane p = new StackPane(new Label("Drag"));
                 p.setBackground(bgr(new javafx.scene.paint.Color(0,0,0,0.5)));
                 p.setMouseTransparent(true);
            return p;
        },
        (p,data) -> {
            // we could reuse the layout, but premature optimization...
            p.getChildren().setAll(layHeaderBottom(8, Pos.CENTER,
                new Icon(data.icon == null ? CLIPBOARD : data.icon,25),
                new Label(data.name))
            );
        }
    );
    private static final String DRAG_ACTIVE = "DRAG";
    private static final String DRAG_INSTALLED = "DRAG_INSTALLED";

    @Dependency("param must be pane")
    public static final void installDragSignalPane(Pane r, GlyphIcons icon, String name) { // Pane required!
        Data d = new Data(name, icon);
        r.getProperties().put(DRAG_INSTALLED, d);
        r.addEventHandler(DragEvent.DRAG_ENTERED, e -> {
            Pane dp = DRAGPANE.getM(d);
            r.getProperties().put(DRAG_ACTIVE, DRAG_ACTIVE);
            if(!r.getChildren().contains(dp))
                r.getChildren().add(dp);
            double w = r.getLayoutBounds().getWidth();
            double h = r.getLayoutBounds().getHeight();
            dp.setMaxSize(w,h);
            dp.setPrefSize(w,h);
            dp.setMinSize(w,h);
            dp.resizeRelocate(0,0,w,h);
            dp.toFront();
        });
        r.addEventHandler(DRAG_EXITED, e -> {
            r.getChildren().remove(DRAGPANE.get());
            r.getProperties().remove(DRAG_ACTIVE);

            Parent p = r.getParent();
            do {
                // do not rely on Pane.isHover() !
                boolean hover = p.localToScene(p.getLayoutBounds()).contains(e.getSceneX(),e.getSceneY());
                Data dt = (Data) p.getProperties().get(DRAG_INSTALLED);
                if(dt!=null && hover) {
                    Pane dp = DRAGPANE.getM(dt);
                    if(!p.getChildrenUnmodifiable().contains(dp))
                        ((Pane)p).getChildren().add(dp);    // the cast is safe
                    double w = p.getLayoutBounds().getWidth();
                    double h = p.getLayoutBounds().getHeight();
                    dp.setMaxSize(w,h);
                    dp.setPrefSize(w,h);
                    dp.setMinSize(w,h);
                    dp.resizeRelocate(0,0,w,h);
                    dp.toFront();
                    break;
                }
                p = p.getParent();
            }while(p!=null);
        });
        // unfortunately, very quick mouse movements may cause DRAG_EXITED not be called
        // (same for DRAG_EXITED_TARGET) and our cleanup is not invoked
        // i cant work around this (because these are drag events using Node.hover() !work, nor
        // do MOUSE_EXIT/ENTER)
        // for now, at least allow user to clean up manually
        //
        // debug below:
        // r.addEventHandler(javafx.event.Event.ANY, e -> System.out.println(e.getEventType()));
        r.addEventHandler(MOUSE_ENTERED, e -> {
            if(r.getProperties().containsKey(DRAG_ACTIVE)) {
                r.getChildren().remove(DRAGPANE.get());
            }
        });
    }

    private static class Data {
        final String name;
        final GlyphIcons icon;

        Data(String name, GlyphIcons icon) {
            this.name = name;
            this.icon = icon;
        }
    }
}
