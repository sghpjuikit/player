/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.WidgetImpl;

import Configuration.IsConfig;
import Layout.Widgets.ClassWidget;
import Layout.Widgets.IsWidget;
import Layout.Widgets.Widget;
import static Layout.Widgets.Widget.Group.OTHER;
import Layout.Widgets.controller.ClassController;
import com.sun.javafx.scene.control.skin.VirtualFlow;
import gui.objects.image.Thumbnail;
import java.io.File;
import static java.lang.Math.*;
import java.util.ArrayList;
import java.util.List;
import static javafx.collections.FXCollections.observableArrayList;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import static javafx.scene.control.ScrollPane.ScrollBarPolicy.*;
import javafx.scene.image.Image;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import javafx.scene.layout.*;
import main.App;
import util.File.Environment;
import util.File.FileUtil;
import util.File.ImageFileFormat;
import static util.Util.setAnchors;
import util.access.Accessor;
import static util.async.Async.runLater;
import static util.functional.Util.filterMap;
import static util.functional.Util.forEachWithI;
import static util.functional.Util.listRO;

/**
 *
 * @author Plutonium_
 */
@IsWidget
@Widget.Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "Dir Viewer",
    description = "Displays directory hierarchy and files as thumbnails in a "
            + "vertically scrollable grid. Intended as simle library",
    howto = "",
    notes = "",
    version = "0.3",
    year = "2015",
    group = OTHER
)
public class DirViewer extends ClassController {
    
    @IsConfig(name = "Location", info = "Root directory the contents of to display "
            + "This is not a file system browser, and it is not possible to "
            + "visit parent of this directory.")
    final Accessor<File> file = new Accessor<>(App.getLocation(),this::viewDir);
    
    Cell item = null;
    CellPane cells = new CellPane(160,220,5);
    
    public DirViewer(ClassWidget widget) {
        super(widget);
        
        addEventFilter(MOUSE_CLICKED, e -> {
            if(e.getButton()==SECONDARY && item!=null && item.parent!=null) {
                viewDir(item.parent);
            }
        });
        
        ScrollPane layout = new ScrollPane();
        layout.setContent(cells);
        layout.setFitToWidth(true);
        layout.setFitToHeight(false);
        layout.setHbarPolicy(NEVER);
        layout.setVbarPolicy(AS_NEEDED);
        getChildren().add(layout);
        setAnchors(layout,0);
                    
        setOnScroll(Event::consume);
    }

    @Override
    public void refresh() {
        file.applyValue();
    }
    
    public void viewDir(File dir) {
        viewDir(new Cell(null,dir));
    }
    
    public void viewDir(Cell dir) {
        item = dir;
        if(item==null) cells.getChildren().clear();
        else cells.getChildren().setAll(filterMap(item.children(),i->!ImageFileFormat.isSupported(i.val),Cell::load));
    }
    
    
    public class Cell {
        
        public final File val;
        public final Cell parent;
        
        private boolean isLeaf;
        private boolean isFirstTimeLeaf = true;
        private boolean isFirstTimeChildren = true;
        private final ObservableList<Cell> childr = observableArrayList();
        
        public Cell(Cell parent, File value) {
            this.val = value;
            this.parent = parent;
        }
        
        public ObservableList<Cell> children() {
            if (isFirstTimeChildren) {
                childr.setAll(buildChildren(this));
                isFirstTimeChildren = false;
            }
            return childr;
        }
        
        boolean isFirstTimeCover = true;
        Image cover = null;
        public File getCoverFile() {
            File f = val;
            File i = f.isDirectory() ? new File(f,"cover.jpg")
                                       : new File(f.getParent(),FileUtil.getName(f)+".jpg");
            return i;
        }
        public Image getCover() {
            return cover;
        }
        public void setCover(Image i) {
            cover = i;
            isFirstTimeCover=false;
        }

//        @Override public boolean isLeaf() {
//            if (isFirstTimeLeaf) {
//                isFirstTimeLeaf = false;
//                isLeaf = getValue().isFile();
//            }
//
//            return isLeaf;
//        }

        private List<Cell> buildChildren(Cell i) {
            File value = i.val;
            if (value != null && value.isDirectory()) {
                File[] all = value.listFiles();
                if (all != null) {
                    // we want to sort the items : directories first
                    List<Cell> fils = new ArrayList();
                    List<Cell> dirs = new ArrayList();
                    for (File f : all) {
                        if(!f.isDirectory()) dirs.add(new Cell(i,f));
                        else                 fils.add(new Cell(i,f));
                    }
                           fils.addAll(dirs);
                    return fils;
                }
            }

            return listRO();
        }
        
        
        private VBox n;
        public Node load() {
            if(n==null) {
                File dir = val;
                n = new VBox();
                n.setPrefSize(160,220);

                Thumbnail t = new Thumbnail(160,200);
                if(isFirstTimeCover) {
                    File cf = getCoverFile();
                    t.image.addListener((o,ov,nv) -> setCover(nv));
                    t.loadImage(cf);
                } else {
                    t.loadImage(getCover());
                }
                t.getPane().setOnMouseClicked(e -> {
                    if(e.getButton()==PRIMARY && e.getClickCount()==2) {
                        if(dir.isDirectory()) viewDir(this);
                        else Environment.open(dir);
                        e.consume();
                    }
                });

                Label l = new Label(dir.getName());
                n.getChildren().addAll(t.getPane(),l);

                n.setAlignment(Pos.CENTER);
            }
            return n;
        }
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

            forEachWithI(cells, (i,n) -> {
                double x = i%columns * (cellw+gapx);
                double y = i/columns * (cellh+gapy);
                n.relocate(x,y);
                n.resize(cellw, cellh);
            });
            
            int rows = (int) ceil(elements/columns);

            runLater(()->setPrefHeight(rows*(cellh+gapy)));
        }
        
    }
}