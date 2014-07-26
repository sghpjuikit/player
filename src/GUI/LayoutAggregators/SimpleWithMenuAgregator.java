/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.LayoutAggregators;

import Layout.Layout;
import java.util.Collections;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

/**
 *
 * @author Plutonium_
 */
public class SimpleWithMenuAgregator implements LayoutAggregator {

    Layout l;
    BorderPane root;
    HBox menu;
    
    public SimpleWithMenuAgregator() {
         root = new BorderPane();
         menu = new HBox(5);
         menu.setAlignment(Pos.CENTER_RIGHT);
         root.setRight(menu);
         root.setPrefWidth(1920);
    }
    
    public SimpleWithMenuAgregator(Layout l) {
        this();
        addLayout(l);
    }    
    
    public final void addLayout(Layout l) {
        this.l = l;
        AnchorPane a = new AnchorPane();
                   a.setPadding(new Insets(0, 20, 0, 0)); // gap to right
        l.load(a);
        root.setCenter(a);
    }
    
    
    @Override
    public Map<Integer, Layout> getLayouts() {
        return Collections.singletonMap(1, l);
    }

    @Override
    public Parent getRoot() {
        return root;
    }

    @Override
    public long getCapacity() {
        return 1;
    }

    @Override
    public Layout getActive() {
        return l;
    }
    
    public HBox getMenu() {
        return menu;
    }
    
}
