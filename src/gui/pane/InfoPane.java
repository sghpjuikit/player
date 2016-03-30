/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.pane;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javafx.event.Event;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import util.conf.IsConfigurable;
import gui.objects.icon.Icon;
import util.R;

import static gui.objects.icon.Icon.createInfoIcon;
import static java.util.stream.Collectors.groupingBy;
import static javafx.geometry.Pos.CENTER;
import static javafx.geometry.Pos.CENTER_RIGHT;
import static javafx.scene.layout.Priority.ALWAYS;
import static javafx.scene.layout.Priority.NEVER;
import static util.file.Environment.copyToSysClipboard;
import static util.functional.Util.by;
import static util.graphics.Util.layHeaderTop;
import static util.graphics.Util.layHorizontally;
import static util.graphics.Util.layStack;
import static util.graphics.Util.layVertically;

/**
 *
 * @author Martin Polakovic
 */
@IsConfigurable("Shortcut Viewer")
public class InfoPane extends OverlayPane {

    private static final String STYLECLASS = "info-pane";
    private static final String STYLECLASS_GROUP = "info-pane-group-label";
    private static String fixASCII(String s) {
        if(s.length()==1) {
            char c = s.charAt(0);
            if(c=='\n') return "\n";
            if(c=='\r') return "\r";
        } else if(s.length()==2) {
            if(s.charAt(0)=='\n' && s.charAt(1)=='\r') return "\\n\\r";
            if(s.charAt(0)=='\r' && s.charAt(1)=='\n') return "\\r\\n";
        }
        return s;
    }
    private static String getGroup(String property_key) {
        int i = property_key.indexOf('.');
        return i<0 ? property_key : property_key.substring(0,i);
    }


    private final GridPane g = new GridPane();
    private final Icon helpI = createInfoIcon(
          "System information viewer"
        + "\n\n"
        + "Displays available system properties. Click on the property to copy the value."
    );

    public InfoPane() {
        getStyleClass().add(STYLECLASS);

        ScrollPane sp = new ScrollPane();
                   sp.setOnScroll(Event::consume);
                   sp.setContent(layStack(g, CENTER));
                   sp.setFitToWidth(true);
                   sp.setFitToHeight(false);
                   sp.setHbarPolicy(ScrollBarPolicy.NEVER);
                   sp.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        VBox l = layHeaderTop(5, CENTER,
            layHorizontally(5,CENTER_RIGHT, helpI),
            layStack(sp, CENTER)
        );
        l.setMaxWidth(800);
        l.maxHeightProperty().bind(heightProperty().subtract(100));
        setContent(l);
    }

    @Override
    public void show() {
        super.show();
        build();
    }

    @Override
    public void hide() {
        super.hide();
    }

    private void build() {
        // clear content
        g.getChildren().clear();
        g.getRowConstraints().clear();
        g.getColumnConstraints().clear();

        // build columns
        g.getColumnConstraints().add(new ColumnConstraints(550,550,550, NEVER, HPos.RIGHT, false));
        g.getColumnConstraints().add(new ColumnConstraints(10));
        g.getColumnConstraints().add(new ColumnConstraints(-1,-1,-1, ALWAYS, HPos.LEFT, false));

        Map<String,List<Entry<String,String>>> props = (Map) System.getProperties()
                .entrySet().stream()
                .filter(e -> e.getKey() instanceof String && e.getValue() instanceof String)
                .collect(groupingBy(e -> getGroup((String)e.getKey())));

        // build rows
        R<Integer> i = new R<>(-1);
        props.entrySet().stream()
             .sorted(by(Entry::getKey))
             .peek(e -> e.getValue().sort(by(Entry::getKey)))
             .forEach(e -> {
                    // group title row
                    i.setOf(v -> v+1);
                    String group = e.getKey();
                    Label groupl = new Label(group);
                          groupl.getStyleClass().add(STYLECLASS_GROUP);
                    g.add(layVertically(0,Pos.CENTER, new Label(), groupl), 2,i.get());
                    GridPane.setValignment(groupl.getParent(), VPos.CENTER);
                    GridPane.setHalignment(groupl.getParent(), HPos.LEFT);

                    // property rows
                    for(Entry<String,String> a : e.getValue()) {
                        i.setOf(v -> v+1);
                        String name = a.getKey().substring(e.getKey().length()+1);
                        String val = fixASCII(a.getValue());

                        Label nameL = new Label(name);
                        Label valL = new Label(val);
                              valL.setCursor(Cursor.HAND);
                              valL.setOnMouseClicked(ev -> copyToSysClipboard(val));
                        g.add(valL, 0,i.get());
                        g.add(nameL, 2,i.get());
                    }
            });
    }

}