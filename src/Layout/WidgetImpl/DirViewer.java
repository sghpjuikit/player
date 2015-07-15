/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.WidgetImpl;

import Configuration.Config;
import Configuration.Config.ListAccessor;
import Configuration.IsConfig;
import Layout.Widgets.IsWidget;
import Layout.Widgets.Widget;
import static Layout.Widgets.Widget.Group.OTHER;
import Layout.Widgets.controller.ClassController;
import gui.objects.image.Thumbnail;
import java.io.File;
import static java.lang.Math.*;
import java.util.ArrayList;
import java.util.List;
import static java.util.stream.Collectors.toList;
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
import util.File.Environment;
import util.File.FileUtil;
import util.File.ImageFileFormat;
import static util.Util.setAnchors;
import util.access.Accessor;
import static util.async.Async.runLater;
import static util.functional.Util.by;
import static util.functional.Util.filterMap;
import static util.functional.Util.forEachWithI;
import static util.functional.Util.listRO;
import static util.functional.Util.stream;

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
            + "vertically scrollable grid. Intended as simple library",
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
    final ListAccessor<File> files = new ListAccessor<>(() -> new File("C:\\"),f -> Config.fromProperty("File", new Accessor<File>(f)));
//            .setItems(App.getLocation());
    
    Cell item = null;
    CellPane cells = new CellPane(160,220,5);
    
    public DirViewer() {
        addEventHandler(MOUSE_CLICKED, e -> {
            if(e.getButton()==SECONDARY && item!=null && item.parent!=null) {
                viewDir(item.parent);
            }
        });
        
        files.onInvalid(list -> viewDir(new TopCell()));
        
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
        viewDir(new TopCell());
    }
    
    public void viewDir(Cell dir) {
        item = dir;
        cells.getChildren().clear();
        if(item!=null) 
            cells.getChildren().addAll(filterMap(item.children(),i->
                !ImageFileFormat.isSupported(i.val) && !i.val.isHidden() && i.val.canRead()
            ,Cell::load));
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
                childr.setAll(buildChildren());
                isFirstTimeChildren = false;
            }
            return childr;
        }
        
        boolean isFirstTimeCover = true;
        Image cover = null;
        public File getCoverFile() {
            File f = val;
            File i = f.isDirectory() ? new File(f,"cover.jpg") : filImage(f);
            return i;
        }
        public Image getCover() {
            return cover;
        }
        public void setCover(Image i) {
            cover = i;
            isFirstTimeCover=false;
        }
        
        private File filImage(File f) {
            File i = new File(f.getParent(),FileUtil.getName(f)+".jpg");
            if(!i.exists()) return parent.getCoverFile();
            else return i;
        }

        protected List<Cell> buildChildren() {
            File value = val;
            if (value != null && value.isDirectory()) {
                File[] all = value.listFiles();
                if (all != null) {
                    // we want to sort the items : directories first
                    // we make use of the fact that listFiles() gives us already
                    // sorted list
                    List<Cell> fils = new ArrayList();
                    List<Cell> dirs = new ArrayList();
                    for (File f : all) {
                        if(!f.isDirectory()) dirs.add(new Cell(this,f));
                        else                 fils.add(new Cell(this,f));
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
    public class TopCell extends Cell {

        public TopCell() {
            super(null,null);System.out.println("new");
        }

        @Override
        protected List<Cell> buildChildren() {
            return files.list.stream()
                      .flatMap(f->stream(f.listFiles()))
                      .sorted(by(File::getName))
                      .map(f -> new Cell(this,f))
                      .collect(toList());
        }

        @Override
        public File getCoverFile() {
            return null;
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
            
            int rows = (int) ceil(elements/(double)columns);

            runLater(()->setPrefHeight(rows*(cellh+gapy)));
        }
        
    }
}