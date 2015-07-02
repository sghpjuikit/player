/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.WidgetImpl;

import Layout.Widgets.controller.ClassController;
import Layout.Widgets.IsWidget;
import Layout.Widgets.Widget;
import static Layout.Widgets.Widget.Group.OTHER;
import gui.objects.Thumbnail.Thumbnail;
import gui.objects.tree.TreeItems.FileTreeItem;
import java.io.File;
import static java.lang.Math.*;
import java.util.List;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import static javafx.scene.control.ScrollPane.ScrollBarPolicy.*;
import javafx.scene.control.TreeItem;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import javafx.scene.layout.*;
import util.File.Environment;
import static util.Util.setAnchors;
import static util.async.Async.runLater;
import static util.functional.Util.forEachI;
import static util.functional.Util.map;

/**
 *
 * @author Plutonium_
 */
@IsWidget
@Widget.Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "Dir Viewer",
    description = "",
    howto = "",
    notes = "",
    version = "0.1",
    year = "2015",
    group = OTHER
)
public class DirViewer extends ClassController {
    
    TreeItem<File> item = null;
    CellPane cells = new CellPane(160,220,5);
    
    public DirViewer() {
        addEventFilter(MOUSE_CLICKED, e -> {
            if(e.getButton()==SECONDARY && item!=null) {
                if(item.getParent()!=null) viewDir(item.getParent());
            }
        });
        
        ScrollPane layout = new ScrollPane();
        layout.setContent(cells);
        layout.setFitToWidth(true);
        layout.setFitToHeight(false);
        layout.setHbarPolicy(NEVER);
        layout.setVbarPolicy(AS_NEEDED);
        layout.setVbarPolicy(ALWAYS);
        getChildren().add(layout);
        setAnchors(layout,0);
                    
        setOnScroll(Event::consume);
        
        viewDir(new FileTreeItem(new File("P:\\")));
    }
    
    public void viewDir(File dir) {
        viewDir(new FileTreeItem(dir));
    }
    public void viewDir(TreeItem<File> dir) {
        item = dir;
        if(item==null) cells.getChildren().clear();
        else cells.getChildren().setAll(map(item.getChildren(),Cell::new));
    }
    
    
    public class CellPane extends Pane {
        double cellw = 100;
        double cellh = 100;
        double cellg = 5;
        
        CellPane(double cellw, double cellh, double gap) {
            this.cellw = cellw;
            this.cellh = cellh;
            this.cellg = gap;
            //setBackground(new Background(new BackgroundFill(Color.CADETBLUE, CornerRadii.EMPTY, Insets.EMPTY)));
        }
        
        @Override
        protected void layoutChildren() {
            double width = getWidth();
            List<Node> cells = getChildren();
            
            int elements = cells.size();
            if(elements==0) return;

            int c = (int) floor((width+cellg)/(cellw+cellg));
            int columns = max(1,c);
            double gapx = cellg+(width+cellg-columns*(cellw+cellg))/columns;
            double gapy = cellg;

            forEachI(cells, (i,n) -> {
                double x = i%columns * (cellw+gapx);
                double y = i/columns * (cellh+gapy);
                n.relocate(x,y);
                n.resize(cellw, cellh);
            });
            
            int rows = (int) ceil(elements/columns);

            runLater(()->setPrefHeight(rows*(cellh+gapy)));
        }
        
    }
    public class Cell extends VBox {
        Cell(TreeItem<File> dir_item) {
            File dir = dir_item.getValue();
            setPrefSize(160,220);
            
            File i = new File(dir,"cover.jpg");
            Thumbnail t = new Thumbnail(160,200);
            t.loadImage(i.exists() ? i : null);
            t.getPane().setOnMouseClicked(e -> {
                if(e.getButton()==PRIMARY && e.getClickCount()==2) {
                    if(dir.isDirectory()) viewDir(dir_item);
                    else Environment.open(dir);
                    e.consume();
                }
            });
            
            Label l = new Label(dir.getName());
            getChildren().addAll(t.getPane(),l);
            
            setAlignment(Pos.CENTER);
        }
    }
}
