/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.pane;


import java.util.List;
import java.util.Map.Entry;

import javafx.event.Event;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import org.reactfx.Subscription;

import Configuration.IsConfig;
import Configuration.IsConfigurable;
import action.Action;
import gui.objects.icon.CheckIcon;
import gui.objects.icon.Icon;
import util.access.Ѵ;

import static action.Action.getActions;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.CHECKBOX_BLANK_CIRCLE_OUTLINE;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.CLOSE_CIRCLE_OUTLINE;
import static gui.objects.icon.Icon.createInfoIcon;
import static java.util.stream.Collectors.groupingBy;
import static javafx.geometry.Pos.CENTER;
import static javafx.geometry.Pos.CENTER_RIGHT;
import static javafx.scene.layout.Priority.ALWAYS;
import static javafx.scene.layout.Priority.NEVER;
import static util.functional.Util.by;
import static util.graphics.Util.layHeaderTop;
import static util.graphics.Util.layHorizontally;
import static util.graphics.Util.layStack;
import static util.graphics.Util.layVertically;
import static util.reactive.Util.maintain;

/**
 *
 * @author Plutonium_
 */
@IsConfigurable("Shortcut Viewer")
public class ShortcutPane extends OverlayPane {

    private static final String HE_TITLE = "Hide unassigned shortcuts";
    private static final String HE_INFO = "Displays only shortcuts that have keys assigned";

    @IsConfig(name = HE_TITLE, info = HE_INFO)
    public static final Ѵ<Boolean> hideEmpty = new Ѵ(true);


    private final GridPane g = new GridPane();
    Subscription rebuilding = maintain(hideEmpty,v -> build());

    public ShortcutPane() {
        ScrollPane sp = new ScrollPane();
                   sp.setOnScroll(Event::consume);
                   sp.setContent(layStack(g, CENTER));
                   sp.setFitToWidth(true);
                   sp.setFitToHeight(false);
                   sp.setHbarPolicy(ScrollBarPolicy.NEVER);
                   sp.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        VBox l = layHeaderTop(5, CENTER,
            layStack(controls, CENTER_RIGHT),
            layStack(sp, CENTER)
        );
        l.setMaxWidth(500);
        l.maxHeightProperty().bind(heightProperty().subtract(100));
        setContent(l);
    }

    @Override
    public void show() {
        rebuilding = maintain(hideEmpty,v -> build());
        super.show();
    }

    @Override
    public void hide() {
        if(rebuilding!=null) rebuilding.unsubscribe();
        super.hide();
    }

    private void build() {
        // clear content
        g.getChildren().clear();
        g.getRowConstraints().clear();
        g.getColumnConstraints().clear();

        // build columns
        g.getColumnConstraints().add(new ColumnConstraints(150,150,150, NEVER, HPos.RIGHT, false));
        g.getColumnConstraints().add(new ColumnConstraints(10));
        g.getColumnConstraints().add(new ColumnConstraints(-1,-1,-1, ALWAYS, HPos.LEFT, false));

        // build rows
        int i=-1;
        for(Entry<String,List<Action>> e : getActions().stream()
                                            .filter(a -> !hideEmpty.getValue() || a.hasKeysAssigned())
                                            .sorted(by(Action::getGroup))
                                            .collect(groupingBy(Action::getGroup))
                                            .entrySet()) {
            // group title row
            i++;
            String group = e.getKey();
            Label groupl = new Label(group);
            maintain(groupl.fontProperty(), f -> { // set & maintain bold font
               if(!f.getStyle().toLowerCase().contains("bold"))
                   groupl.setFont(Font.font(f.getFamily(), FontWeight.BOLD, f.getSize()));
            });
            g.add(layVertically(0,Pos.CENTER, new Label(), groupl), 2,i);
            GridPane.setValignment(groupl.getParent(), VPos.CENTER);
            GridPane.setHalignment(groupl.getParent(), HPos.LEFT);

            // shortcut rows
            e.getValue().sort(by(Action::getName));
            for(Action a : e.getValue()) {
                i++;
                g.add(new Label(a.getKeys()), 0,i);
                g.add(new Label(a.getName()), 2,i);
            }
        }
    }


/************************************ CONTROLS ************************************/

    private final Icon helpI = createInfoIcon("Shortcut viewer"
        + "\n\n"
        + "Displays available shortcuts. Optionally also those that have not been assigned yet."
    );
    private final Icon hideI = new CheckIcon(hideEmpty)
                                    .tooltip(HE_TITLE+"\n\n"+HE_INFO)
                                    .icons(CHECKBOX_BLANK_CIRCLE_OUTLINE, CLOSE_CIRCLE_OUTLINE);
    private final HBox controls = layHorizontally(5,CENTER_RIGHT, hideI,helpI);

}
