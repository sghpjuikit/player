/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.pane;


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
import javafx.scene.layout.VBox;

import org.reactfx.Subscription;

import util.conf.IsConfig;
import util.conf.IsConfigurable;
import util.action.Action;
import gui.objects.icon.CheckIcon;
import gui.objects.icon.Icon;
import util.R;
import util.access.V;

import static util.action.Action.getActions;
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
 * @author Martin Polakovic
 */
@IsConfigurable("Shortcuts.Viewer")
public class ShortcutPane extends OverlayPane {

    private static final String HE_TITLE = "Hide unassigned shortcuts";
    private static final String HE_INFO = "Displays only shortcuts that have keys assigned";
    private static final String STYLECLASS = "shortcut-pane";
    private static final String STYLECLASS_GROUP = "shortcut-pane-group-label";

    @IsConfig(name = HE_TITLE, info = HE_INFO)
    public static final V<Boolean> HIDE_EMPTY_SHORTCUTS = new V<>(true);


    private final GridPane g = new GridPane();
    Subscription rebuilding = maintain(HIDE_EMPTY_SHORTCUTS,v -> build());

    public ShortcutPane() {
        getStyleClass().add(STYLECLASS);

        ScrollPane sp = new ScrollPane();
                   sp.setOnScroll(Event::consume);
                   sp.setContent(layStack(g, CENTER));
                   sp.setFitToWidth(true);
                   sp.setFitToHeight(false);
                   sp.setHbarPolicy(ScrollBarPolicy.NEVER);
                   sp.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        VBox l = layHeaderTop(5, CENTER,
            layHorizontally(5,CENTER_RIGHT, hideI,helpI),
            layStack(sp, CENTER)
        );
        l.setMaxWidth(500);
        l.maxHeightProperty().bind(heightProperty().subtract(100));
        setContent(l);
    }

    @Override
    public void show() {
        rebuilding = maintain(HIDE_EMPTY_SHORTCUTS, v -> build());
        super.show();
    }

    @Override
    public void hide() {
        if (rebuilding!=null) rebuilding.unsubscribe();
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
        R<Integer> i = new R<>(-1);
        getActions().stream()
                    .filter(a -> !HIDE_EMPTY_SHORTCUTS.getValue() || a.hasKeysAssigned())
                    .collect(groupingBy(Action::getGroup))
                    .entrySet().stream()
                    .sorted(by(Entry::getKey))
                    .peek(e -> e.getValue().sort(by(Action::getName)))
                    .forEach(e -> {
            // group title row
            i.setOf(v -> v+1);
            String group = e.getKey();
            Label groupl = new Label(group);
                  groupl.getStyleClass().add(STYLECLASS_GROUP);
            g.add(layVertically(0,Pos.CENTER, new Label(), groupl), 2,i.get());
            GridPane.setValignment(groupl.getParent(), VPos.CENTER);
            GridPane.setHalignment(groupl.getParent(), HPos.LEFT);

            // shortcut rows
            for (Action a : e.getValue()) {
                i.setOf(v -> v+1);
                g.add(new Label(a.getKeys()), 0,i.get());
                g.add(new Label(a.getName()), 2,i.get());
            }
        });
    }


/************************************ HEADER ************************************/

    private final Icon helpI = createInfoIcon("Shortcut viewer"
        + "\n\n"
        + "Displays available shortcuts. Optionally also those that have not been assigned yet."
    );
    private final Icon hideI = new CheckIcon(HIDE_EMPTY_SHORTCUTS)
                                    .tooltip(HE_TITLE+"\n\n"+HE_INFO)
                                    .icons(CHECKBOX_BLANK_CIRCLE_OUTLINE,CLOSE_CIRCLE_OUTLINE);
}
