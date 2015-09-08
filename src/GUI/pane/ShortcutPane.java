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
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;

import action.Action;

import static java.util.stream.Collectors.groupingBy;
import static javafx.geometry.Pos.CENTER;
import static javafx.scene.layout.GridPane.REMAINING;
import static javafx.scene.layout.Priority.NEVER;
import static util.Util.GR;
import static util.functional.Util.by;
import static util.graphics.Util.layStack;

/**
 *
 * @author Plutonium_
 */
public class ShortcutPane extends OverlayPane {

    private final GridPane g = new GridPane();

    public ShortcutPane() {

        // layout

        ScrollPane sp = new ScrollPane();
                   sp.setOnScroll(Event::consume);
                   sp.setContent(layStack(g, CENTER));
                   sp.setFitToWidth(true);
                   sp.setFitToHeight(false);
                   sp.setHbarPolicy(ScrollBarPolicy.NEVER);
                   sp.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        StackPane layout = layStack(sp, CENTER);
                  layout.setMaxSize(GR*450,450);
                  layout.getStyleClass().add(CONTENT_STYLECLASS);
        getChildren().add(layout);

        double c1w = 250;
        double gw = 10;
        double c2w = layout.getMaxWidth() - c1w - gw;
        g.getColumnConstraints().add(new ColumnConstraints(c1w, c1w, c1w, NEVER, HPos.RIGHT, false));
        g.getColumnConstraints().add(new ColumnConstraints(gw));
        g.getColumnConstraints().add(new ColumnConstraints(c2w, c2w, c2w, NEVER, HPos.LEFT, false));

        int i=-1;
        for(Entry<String,List<Action>> e : action.Action.getActions().stream()
                                            .sorted(by(Action::getGroup))
                                            .collect(groupingBy(Action::getGroup))
                                            .entrySet()) {
            // skip row
            i++;
            g.getRowConstraints().add(new RowConstraints(10));

            // group name
            String group = e.getKey();
            Label groupl = new Label(group);
            g.add(groupl, 0,i);
            GridPane.setColumnSpan(groupl, REMAINING);
            GridPane.setValignment(groupl, VPos.CENTER);
            GridPane.setHalignment(groupl, HPos.CENTER);

            // skip row
            i++;
            g.getRowConstraints().add(new RowConstraints(10));

            // shortcuts
            e.getValue().sort(by(Action::getName));
            for(Action a : e.getValue()) {
                i++;
                g.add(new Label(a.getKeys()), 0,i);
                g.add(new Label(a.getName()), 2,i);
            }
        }

    }

}
