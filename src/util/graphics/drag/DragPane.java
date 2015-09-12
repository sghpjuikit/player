/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.graphics.drag;

import java.util.function.Predicate;
import java.util.function.Supplier;

import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import de.jensd.fx.glyphs.GlyphIcons;
import gui.objects.icon.Icon;
import util.SingleⱤ;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.CLIPBOARD;
import static javafx.scene.input.DragEvent.DRAG_EXITED;
import static util.graphics.Util.layHeaderBottom;
import static util.graphics.Util.removeFromParent;

/**
 *
 * @author Plutonium_
 */
public class DragPane {

    private static final String ACTIVE = "DRAG_PANE";
    private static final String INSTALLED = "DRAG_PANE_INSTALLED";
    private static final String STYLECLASS = "drag-pane";
    private static final SingleⱤ<Pane,Data> PANE = new SingleⱤ<>(() -> {
            Pane p = new StackPane(new Label("Drag"));
                 p.getStyleClass().add(STYLECLASS);
                 p.setMouseTransparent(true);   // must not interfere with events
                 p.setManaged(false);           // must not interfere with layout
            return p;
        },
        (p,data) -> {
            // we could reuse the layout, but premature optimization...
            p.getChildren().setAll(layHeaderBottom(8, Pos.CENTER,
                new Icon(data.icon == null ? CLIPBOARD : data.icon,25),
                new Label(data.name.get()))
            );
        }
    );

    public static final void installDragSignalPane(Node r, GlyphIcons icon, String name, Predicate<DragEvent> accept) {
        installDragSignalPane(r, icon, () -> name, accept);
    }
    
    public static final void installDragSignalPane(Node r, GlyphIcons icon, Supplier<String> name, Predicate<DragEvent> cond) {
        Data d = new Data(name, icon, cond);
        r.getProperties().put(INSTALLED, d);
        // r.addEventFilter(Event.ANY, e -> System.out.println(e.getEventType())); // debug
        // instead of DRAG_ENTERED we need to use DRAG_ENTERED_TARGET as the former isnt called
        // sometimes (when mouse exits window and reenters (sometimes))
        // this in turn causes wrong node to be activated. Using filters instead of handlers solves it
        r.addEventFilter(DragEvent.DRAG_ENTERED_TARGET, e -> {
            if(d.cond.test(e)) {
                // this may not make sense, one would expect this to ALWAYS be called (before
                // the check above), but we must remove the pane ONLY when new location acceps
                // the drag
                removeFromParent(PANE.get());

                r.getProperties().put(ACTIVE, ACTIVE);
                Pane p = r instanceof Pane ? (Pane)r : (Pane)r.getParent();
                Pane dp = PANE.getM(d);
                if(!p.getChildren().contains(dp))
                    p.getChildren().add(dp);
                Bounds b = r.getLayoutBounds();
                double w = b.getWidth();
                double h = b.getHeight();
                dp.setMaxSize(w,h);
                dp.setPrefSize(w,h);
                dp.setMinSize(w,h);
                dp.resizeRelocate(b.getMinX(),b.getMinY(),w,h);
                dp.toFront();
            }
        });
        r.addEventHandler(DRAG_EXITED, e -> {
            removeFromParent(PANE.get());
            r.getProperties().remove(ACTIVE);

            Parent p = r.getParent();
            do {
                // do not rely on Pane.isHover() it doesnt work during drags
                boolean hover = p.localToScene(p.getLayoutBounds()).contains(e.getSceneX(),e.getSceneY());
                Data dt = (Data) p.getProperties().get(INSTALLED);
                if(dt!=null && dt.cond.test(e) && hover && p instanceof Pane) {
                    Pane dp = PANE.getM(dt);
                    if(!p.getChildrenUnmodifiable().contains(dp))
                        ((Pane)p).getChildren().add(dp);
                    Bounds b = p.getLayoutBounds();
                    double w = b.getWidth();
                    double h = b.getHeight();
                    dp.setMaxSize(w,h);
                    dp.setPrefSize(w,h);
                    dp.setMinSize(w,h);
                dp.resizeRelocate(b.getMinX(),b.getMinY(),w,h);
                    dp.toFront();
                    break;
                }
                p = p.getParent();
            }while(p!=null);
        });
    }

    private static class Data {
        final Supplier<String> name;
        final GlyphIcons icon;
        final Predicate<DragEvent> cond;

        Data(Supplier<String> name, GlyphIcons icon, Predicate<DragEvent> cond) {
            this.name = name;
            this.icon = icon;
            this.cond = cond;
        }
    }
}
