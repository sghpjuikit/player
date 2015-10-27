/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.widget.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;

import org.controlsfx.control.GridCell;

import Configuration.Config;
import Configuration.Config.VarList;
import Configuration.IsConfig;
import Layout.widget.IsWidget;
import Layout.widget.Widget;
import Layout.widget.controller.ClassController;
import gui.objects.grid.ImprovedGridView;
import gui.objects.image.Thumbnail;
import util.File.AudioFileFormat;
import util.File.AudioFileFormat.Use;
import util.File.Environment;
import util.File.FileUtil;
import util.File.ImageFileFormat;
import util.Util;
import util.access.Ѵ;
import util.animation.Anim;
import util.async.executor.EventReducer;
import util.async.executor.FxTimer;
import util.async.future.Fut;

import static Layout.widget.Widget.Group.OTHER;
import static java.lang.Math.sqrt;
import static javafx.scene.input.KeyCode.BACK_SPACE;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static util.File.FileUtil.getName;
import static util.File.FileUtil.listFiles;
import static util.async.Async.FX;
import static util.async.Async.newSingleDaemonThreadExecutor;
import static util.async.Async.runFX;
import static util.async.Async.runLater;
import static util.functional.Util.*;
import static util.graphics.Util.layAnchor;

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
    version = "0.5",
    year = "2015",
    group = OTHER
)
public class DirViewer extends ClassController {

    @IsConfig(name = "Location", info = "Root directory the contents of to display "
            + "This is not a file system browser, and it is not possible to "
            + "visit parent of this directory.")
    final VarList<File> files = new VarList<>(() -> new File("C:\\"),f -> Config.forValue("File",f));
    @IsConfig(name = "File filter", info = "Shows only directories and files passing the filter.")
    final Ѵ<FFilter> filter = new Ѵ<>(FFilter.ALL, f -> visitDir(new TopItem()));

    Item item = null;   // item, children of which are displayed
    ImprovedGridView<Item> grid = new ImprovedGridView<>();
    ExecutorService executor = newSingleDaemonThreadExecutor();
    boolean initialized = false;
    private volatile boolean isResizing = false;
    private boolean scrollflag = true;

    public DirViewer() {
        files.onListInvalid(list -> visitDir(new TopItem()));
        grid.setCellHeight(220);
        grid.setCellWidth(160);
        grid.setVerticalCellSpacing(5);
        grid.setHorizontalCellSpacing(5);
        grid.setCellFactory(grid -> new Cell());
        layAnchor(this,grid,0d);

        // delay cell loading when content is being resized (increases resize performance)
        FxTimer resizeTimer = new FxTimer(200, 1, () -> {
            isResizing = false;
//            grid.setManaged(true);
        });
        grid.widthProperty().addListener((o,ov,nv) -> isResizing = true);
        grid.heightProperty().addListener((o,ov,nv) -> isResizing = true);
        grid.widthProperty().addListener((o,ov,nv) -> resizeTimer.start(300));
        grid.heightProperty().addListener((o,ov,nv) -> resizeTimer.start(300));
//        grid.heightProperty().addListener((o,ov,nv) -> grid.setManaged(false));
//        grid.widthProperty().addListener((o,ov,nv) -> grid.setManaged(false));
        // decrease scrolling speed (consume scroll events and refire with smaller vertical values)
        grid.addEventFilter(ScrollEvent.ANY, e -> {
            if(scrollflag) {
                Event ne = new ScrollEvent(e.getEventType(),e.getX(),e.getY(),e.getScreenX(),e.getScreenY(),e.isShiftDown(),
                        e.isControlDown(),e.isAltDown(),e.isMetaDown(),e.isDirect(),
                        e.isInertia(),e.getDeltaX(),e.getDeltaY()/3,e.getTextDeltaX(),e.getTextDeltaY()/3,
                        e.getTextDeltaXUnits(),e.getTextDeltaX(),e.getTextDeltaYUnits(),e.getTextDeltaY()/3,
                        e.getTouchCount(),e.getPickResult());
                e.consume();
                scrollflag = false;
                runLater(() -> {
                    if (e.getTarget() instanceof Node) {
                        ((Node) e.getTarget()).fireEvent(ne);
                    }
                    scrollflag = true;
                });
            }
        });

        grid.setOnMouseClicked(e -> {
            if(e.getButton()==SECONDARY)
                visitUp();
        });
        setOnKeyPressed(e -> {
            if(e.getCode()==BACK_SPACE)
                visitUp();
        });
        setOnScroll(Event::consume);
    }

    @Override
    public void refresh() {
        initialized = true;
        runLater(() -> { // temporary bugfix, dont mind
            visitDir(new TopItem());
        });
    }

    // We visit parent, a "back" operation. Note we stop not at top of file hierarchy, but
    // the user source - collection of directories
    void visitUp() {
        if(item!=null && item.parent!=null)
            visitDir(item.parent);
    }

    public void visitDir(Item dir) {
        visitDir(dir, null);
    }

    public void visitDir(Item dir, Item scrollTo) {
        if(!initialized) return;
        // remember last item position
        if(item!=null) item.last_gridposition = grid.getSkinn().getFlow().getPosition();
        // load new item
        item = dir;
        if(item==null) {
            grid.getItems().clear();
        } if(item!=null) {
            Fut.fut(item)
               .map(Item::children,executor)
               .use(newcells -> {
                   grid.getItems().setAll(newcells);
                   if(item.last_gridposition>=0)
                       grid.getSkinn().getFlow().setPosition(item.last_gridposition);
               },FX)
               .showProgress(getWidget().getWindow().taskAdd())
               .run();
        }
    }

    private boolean filter(File f) {
        return !f.isHidden() && f.canRead() && filter.getValue().filter.test(f);
    }
    private static boolean file_exists(Item c, File f) {
        return c!=null && f!=null && c.all_children.contains(f.getPath().toLowerCase());
    }

    // Graphics representing the file. Cells are virtualized just like ListView or TableView does
    // it, but both vertically & horizontally. This avoids loading all files at once and allows
    // unlimited scaling.
    private class Cell extends GridCell<Item>{
        VBox root;
        Label name;
        Thumbnail thumb;
        EventReducer<Item> setCoverLater = EventReducer.toLast(500, item -> executor.execute(() -> {
            try {
                Thread.sleep(10);
                runFX(() -> {
                    if(item==getItem())
                        setCover(item);
                });
            } catch (InterruptedException e) {

            }
        }));

        @Override
        protected void updateItem(Item item, boolean empty) {
            super.updateItem(item, empty);

            if(item==null) {
                // empty cell has no graphics
                // we do not clear the content of the graphics however
                setGraphic(null);
            } else {
                if(root==null) {
                    // we create graphics only once and only when frst requested
                    createGraphics();
                    // we set graphics only once (creating it)
                    setGraphic(root);
                }
                // if cell was previously empty, we set graphics back
                // this improves performance compared to setting it every time
                if(getGraphic()!=root) setGraphic(root);

                // set name
                name.setText(getName(item.val));
                // set cover
                // The item caches the cover Image, so it is only loaded once. That is a heavy op.
                // This method can be called very frequently and:
                //     - block ui thread when scrolling
                //     - reduce ui performance when resizing
                // We solve this by delaying the image loading & drawing. We reduce subsequent
                // invokes into single update (last).
                // When we are
                if(item.cover_loaded && !isResizing)
                    setCover(item);
                else {
                    thumb.loadImage((File)null); // prevent displaying old content before cover loads
                    setCoverLater.push(item);
                }
            }
        }

        private void createGraphics() {
            root = new VBox();
            root.setPrefSize(160,220);
            root.setAlignment(Pos.CENTER);
            name = new Label();
            thumb = new Thumbnail(160,200);
            thumb.getPane().setOnMouseClicked(e -> {
                if(e.getButton()==PRIMARY && e.getClickCount()==2) {
                    if(getItem().val.isDirectory()) visitDir(getItem());
                    else Environment.open(getItem().val);
                    e.consume();
                }
            });
            root.getChildren().addAll(thumb.getPane(),name);
        }

        private void setCover(Item item) {
            item.loadCover((was_loaded,img) -> {
                thumb.loadImage(img);
                if(!was_loaded && img!=null) {
                    new Anim(thumb.getPane()::setOpacity).dur(500).intpl(x -> sqrt(x)).play();
                }
            });
        }
    }
    // File wrapper, content of Cell.
    // We cache various stuff in here, including the cover Image and children files.
    private class Item {

        final File val;
        final Item parent;
        Set<Item> children = null;      // filtered files
        Set<String> all_children = null; // all files, cache, use instead File.exists to reduce I/O
        Image cover = null;         // cover cache
        File cover_file = null;         // cover file cache
        boolean cover_loaded = false;
        double last_gridposition = -1;

        public Item(Item parent, File value) {
            this.val = value;
            this.parent = parent;
        }

        public List<Item> children() {
            if (children == null) buildChildren();
            List<Item> l = list(children);
                       l.sort(by(c -> c.val.getName()));
            return l;
        }

        protected Stream<File> children_files() {
            return listFiles(val);
        }

        private void buildChildren() {
            all_children = new HashSet<>();
            children = new HashSet<>();
            List<Item> fils = new ArrayList<>();
            children_files().forEach(f -> {
                all_children.add(f.getPath().toLowerCase());
                if(DirViewer.this.filter(f)) {
                    if(!f.isDirectory()) children.add(new Item(this,f));
                    else                 fils.add(new Item(this,f));
                }
            });
            children.addAll(fils);
        }

        private File getImage(File dir, String name) {
            if(dir==null) return null;
            for(ImageFileFormat format: ImageFileFormat.values()) {
                if (format.isSupported()) {
                    File f = new File(dir,name + "." + format.toString());
                    if(dir==val ? file_exists(this,f) : file_exists(parent,f)) return f;
                }
            }
            return null;
        }
        private File getImageT(File dir, String name) {
            if(dir==null) return null;

            for(ImageFileFormat format: ImageFileFormat.values()) {
                if (format.isSupported()) {
                    File f = new File(dir,name + "." + format.toString());
                    if(file_exists(this,f)) return f;
                }
            }
            return null;
        }

        public void loadCover(BiConsumer<Boolean,Image> action) {
            boolean was_loaded = cover_loaded;
            if(!cover_loaded) {
                File img_file = getCoverFile();
                Image imgc = Thumbnail.getCached(img_file, 160,220);
                Image img = imgc!=null ? imgc : Util.loadImage(img_file, 160,220);
                cover = img;
                cover_file = img_file;
                cover_loaded = true;
            }
            action.accept(was_loaded,cover);
        }

        protected File getCoverFile() {
            if(all_children==null) buildChildren();
            if(val.isDirectory())
                return getImageT(val,"cover");
            else {
                // image files are their own thumbnail
                if(ImageFileFormat.isSupported(val))
                    return val;
                else {
                    File i = getImage(val.getParentFile(),FileUtil.getName(val));
                    if(i==null && parent!=null) return parent.getCoverFile(); // return the parent image if available, needs some work
                    return i;
                }
            }
        }

    }
    private class TopItem extends Item {

        public TopItem() {
            super(null,null);
        }

        @Override
        protected Stream<File> children_files() {
            return listFiles(files.list.stream());
        }

        @Override
        protected File getCoverFile() {
            return null;
        }

    }
    public static enum FFilter {
        ALL(IS),
        AUDIO(f -> AudioFileFormat.isSupported(f, Use.APP)),
        IMAGE(ImageFileFormat::isSupported),
        NO_IMAGE(f -> !ImageFileFormat.isSupported(f));

        public final Predicate<? super File> filter;

        FFilter(Predicate<? super File> f) {
            filter = f;
        }
    }
}
///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package Layout.widget.impl;
//
//import java.io.File;
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//import java.util.concurrent.ExecutorService;
//import java.util.function.BiConsumer;
//import java.util.function.Predicate;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import java.util.stream.Stream;
//
//import javafx.event.Event;
//import javafx.geometry.Pos;
//import javafx.scene.control.Label;
//import javafx.scene.image.Image;
//import javafx.scene.input.ScrollEvent;
//import javafx.scene.layout.*;
//
//import org.controlsfx.control.GridCell;
//import org.controlsfx.control.GridView;
//
//import Configuration.Config;
//import Configuration.Config.VarList;
//import Configuration.IsConfig;
//import Layout.widget.IsWidget;
//import Layout.widget.Widget;
//import Layout.widget.controller.ClassController;
//import gui.objects.grid.ImprovedGridView;
//import gui.objects.image.Thumbnail;
//import util.File.AudioFileFormat;
//import util.File.AudioFileFormat.Use;
//import util.File.Environment;
//import util.File.FileUtil;
//import util.File.ImageFileFormat;
//import util.Util;
//import util.access.Ѵ;
//import util.animation.Anim;
//import util.async.executor.FxTimer;
//import util.async.future.Fut;
//
//import static Layout.widget.Widget.Group.OTHER;
//import static java.lang.Math.sqrt;
//import static javafx.scene.input.KeyCode.BACK_SPACE;
//import static javafx.scene.input.MouseButton.PRIMARY;
//import static javafx.scene.input.MouseButton.SECONDARY;
//import static util.File.FileUtil.getName;
//import static util.File.FileUtil.listFiles;
//import static util.async.Async.FX;
//import static util.async.Async.newSingleDaemonThreadExecutor;
//import static util.async.Async.runFX;
//import static util.async.Async.runLater;
//import static util.functional.Util.*;
//import static util.graphics.Util.layAnchor;
//
///**
// *
// * @author Plutonium_
// */
//@IsWidget
//@Widget.Info(
//    author = "Martin Polakovic",
//    programmer = "Martin Polakovic",
//    name = "Dir Viewer",
//    description = "Displays directory hierarchy and files as thumbnails in a "
//            + "vertically scrollable grid. Intended as simple library",
//    howto = "",
//    notes = "",
//    version = "0.5",
//    year = "2015",
//    group = OTHER
//)
//public class DirViewer extends ClassController {
//
//    @IsConfig(name = "Location", info = "Root directory the contents of to display "
//            + "This is not a file system browser, and it is not possible to "
//            + "visit parent of this directory.")
//    final VarList<File> files = new VarList<>(() -> new File("C:\\"),f -> Config.forValue("File",f));
//    @IsConfig(name = "File filter", info = "Shows only directories and files passing the filter.")
//    final Ѵ<FFilter> filter = new Ѵ<>(FFilter.ALL, f -> viewDir(new TopItem()));
//
//    Item item = null;
//    GridView<Item> cells = new ImprovedGridView<>();
//    ExecutorService executor = newSingleDaemonThreadExecutor();
//    boolean initialized = false;
//    private volatile boolean isResizing = false;
//
//    public DirViewer() {
//        files.onListInvalid(list -> viewDir(new TopItem()));
//        cells.setCellHeight(220);
//        cells.setCellWidth(160);
//        cells.setVerticalCellSpacing(5);
//        cells.setHorizontalCellSpacing(5);
//        cells.setCellFactory(grid -> new Cell());
//        layAnchor(this,cells,0d);
//
//        // delay cell loading when content is being resized (increases resize performance)
//        FxTimer resizeTimer = new FxTimer(200, 1, () -> isResizing = false);
//        cells.widthProperty().addListener((o,ov,nv) -> isResizing = true);
//        cells.heightProperty().addListener((o,ov,nv) -> isResizing = true);
//        cells.widthProperty().addListener((o,ov,nv) -> resizeTimer.start());
//        cells.heightProperty().addListener((o,ov,nv) -> resizeTimer.start());
//        cells.addEventHandler(ScrollEvent.ANY, e -> isResizing = true);
//        cells.addEventHandler(ScrollEvent.ANY, e -> resizeTimer.start());
//
//
//        cells.setOnMouseClicked(e -> {
//            if(e.getButton()==SECONDARY)
//                visitUp();
//        });
//        setOnKeyPressed(e -> {
//            if(e.getCode()==BACK_SPACE)
//                visitUp();
//        });
//        setOnScroll(Event::consume);
//    }
//
//    @Override
//    public void refresh() {
//        initialized = true;
//        runLater(() -> { // temporary bugfix, dont mind
//            viewDir(new TopItem());
//        });
//    }
//
//    @Override
//    public void onClose() {
//        loading++;
//        super.onClose();
//    }
//
//    void visitUp() {
//        if(item!=null && item.parent!=null)
//            viewDir(item.parent);
//    }
//
//    long loading = 0; // allows canceling of lloading, still think it should be handled more natively
//    public void viewDir(Item dir) {
//        if(!initialized) return; // prevents pointless & inconsistent operations
//
//        loading++;
//        item = dir;
//        if(item==null) {
//            cells.getItems().clear();
//        } if(item!=null) {
//            long l = loading;
//            Fut.fut(item)
//               .map(Item::children,executor)
//               .use(newcells -> cells.getItems().setAll(newcells),FX)
//               .showProgress(getWidget().getWindow().taskAdd())
//               .run();
//        }
//    }
//
//    private boolean filter(File f) {
//        return !f.isHidden() && f.canRead() && filter.getValue().filter.test(f);
//    }
//    private static boolean file_exists(Item c, File f) {
//        return c!=null && f!=null && c.all_children.contains(f.getPath().toLowerCase());
//    }
//
//    private class Cell extends GridCell<Item>{
//            VBox root;
//            Label l;
//            Thumbnail t;
//
//            @Override
//            protected void updateItem(Item item, boolean empty) {
//                Item old = getItem();
//                if(old!=null && item!=old && old.cover!=null) {
////                    old.cover.cancel();
//                }
//
//                super.updateItem(item, empty);
//                if(item==null) {
//                    setGraphic(null);
//                    if(root!=null) {
//                        l.setText("");
//                        t.loadImage((File)null);
//                    }
//                } else {
//                    if(root==null) {
//                        root = new VBox();
//                        root.setPrefSize(160,220);
//                        root.setAlignment(Pos.CENTER);
//                        l = new Label();
//                        t = new Thumbnail(160,200);
//                        root.getChildren().addAll(t.getPane(),l);
//                    }
//                    setGraphic(root);
//
//                    l.setText(getName(item.val));
//                    t.getPane().setOnMouseClicked(e -> {
//                        if(e.getButton()==PRIMARY && e.getClickCount()==2) {
//                            if(item.val.isDirectory()) viewDir(item);
//                            else Environment.open(item.val);
//                            e.consume();
//                        }
//                    });
//                    t.loadImage((File)null);
//                    item.loadCover((image_changed,loaded_image) -> {
//                        if(item==getItem()) {
//                            t.loadImage(loaded_image);
//                            if(image_changed && loaded_image!=null) {
//                                new Anim(t.getPane()::setOpacity).dur(500).intpl(x -> sqrt(x)).play();
//                            }
//                        }
//                    });
//                }
//            }
//    }
//    private class Item {
//
//        public final File val;
//        public final Item parent;
//        private Set<Item> children = null;
//        private Set<String> all_children = null; // cache
//
//        public Item(Item parent, File value) {
//            this.val = value;
//            this.parent = parent;
//        }
//
//        public List<Item> children() {
//            if (children == null) buildChildren();
//            List<Item> l = list(children);
//                       l.sort(by(c -> c.val.getName()));
//            return l;
//        }
//
//
//        public File getCoverFile() {
//            if(all_children==null) buildChildren();
//            if(val.isDirectory())
//                return getImageT(val,"cover");
//            else {
//                if(ImageFileFormat.isSupported(val))
//                    return val;
//                else {
//                    File i = getImage(val.getParentFile(),FileUtil.getName(val));
//                    if(i==null && parent!=null) return parent.getCoverFile();
//                    return i;
//                }
//            }
//        }
//
//        protected Stream<File> children_files() {
//            return listFiles(val);
//        }
//
//        private void buildChildren() {
//            all_children = new HashSet<>();
//            children = new HashSet<>();
//            List<Item> fils = new ArrayList<>();
//            children_files().forEach(f -> {
//                all_children.add(f.getPath().toLowerCase());
//                if(DirViewer.this.filter(f)) {
//                    if(!f.isDirectory()) children.add(new Item(this,f));
//                    else                 fils.add(new Item(this,f));
//                }
//            });
//            children.addAll(fils);
//        }
//
//        private File getImage(File dir, String name) {
//            if(dir==null) return null;
//            for(ImageFileFormat format: ImageFileFormat.values()) {
//                if (format.isSupported()) {
//                    File f = new File(dir,name + "." + format.toString());
//                    if(dir==val ? file_exists(this,f) : file_exists(parent,f)) return f;
//                }
//            }
//            return null;
//        }
//        private File getImageT(File dir, String name) {
//            if(dir==null) return null;
//
//            for(ImageFileFormat format: ImageFileFormat.values()) {
//                if (format.isSupported()) {
//                    File f = new File(dir,name + "." + format.toString());
//                    if(file_exists(this,f)) return f;
//                }
//            }
//            return null;
//        }
//
//        Image cover = null;
//        boolean cover_loaded = false;
//
//        public void loadCover(BiConsumer<Boolean,Image> action) {
//            if(!cover_loaded) {
//                executor.execute(() -> {
//                    int i=0;
////                    while(i==0 || isResizing) {
//                    while(isResizing) {
//                        i++;
//                        try {
//                            Thread.sleep(10);
//                        } catch (InterruptedException ex) {
//                            Logger.getLogger(DirViewer.class.getName()).log(Level.SEVERE, null, ex);
//                        }
//                    }
//
//                    runFX(() -> {
//                        File cf = getCoverFile();
//                        File imgfile = cf!=null && cf.exists() ? cf : null;
//                        Image c = Thumbnail.getCached(imgfile, 160,220);
//                        Image img = c!=null ? c : Util.loadImage(imgfile, 160,220);
//
//                        cover = img;
//                        cover_loaded = true;
//
//                        action.accept(true,img);
//                    });
//
//                });
//            } else {
//                action.accept(false,cover);
//            }
//        }
//    }
//    private class TopItem extends Item {
//
//        public TopItem() {
//            super(null,null);
//        }
//
//        @Override
//        protected Stream<File> children_files() {
//            return listFiles(files.list.stream());
//        }
//
//        @Override
//        public File getCoverFile() {
//            return null;
//        }
//
//    }
//    public static enum FFilter {
//        ALL(IS),
//        AUDIO(f -> AudioFileFormat.isSupported(f, Use.APP)),
//        IMAGE(ImageFileFormat::isSupported),
//        NO_IMAGE(f -> !ImageFileFormat.isSupported(f));
//
//        public final Predicate<? super File> filter;
//
//        FFilter(Predicate<? super File> f) {
//            filter = f;
//        }
//    }
//}

































///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package Layout.widget.impl;
//
//import java.io.File;
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//import java.util.concurrent.ExecutorService;
//import java.util.function.Predicate;
//import java.util.stream.Stream;
//
//import javafx.event.Event;
//import javafx.geometry.Pos;
//import javafx.scene.Node;
//import javafx.scene.control.Label;
//import javafx.scene.image.Image;
//import javafx.scene.layout.*;
//
//import Configuration.Config;
//import Configuration.Config.VarList;
//import Configuration.IsConfig;
//import Layout.widget.IsWidget;
//import Layout.widget.Widget;
//import Layout.widget.controller.ClassController;
//import gui.objects.image.Thumbnail;
//import gui.pane.CellPane;
//import util.File.AudioFileFormat;
//import util.File.AudioFileFormat.Use;
//import util.File.Environment;
//import util.File.FileUtil;
//import util.File.ImageFileFormat;
//import util.access.Ѵ;
//import util.animation.Anim;
//import util.async.future.Fut;
//
//import static Layout.widget.Widget.Group.OTHER;
//import static java.lang.Character.isAlphabetic;
//import static java.lang.Math.sqrt;
//import static javafx.scene.input.KeyCode.BACK_SPACE;
//import static javafx.scene.input.MouseButton.PRIMARY;
//import static javafx.scene.input.MouseButton.SECONDARY;
//import static util.File.FileUtil.getName;
//import static util.File.FileUtil.listFiles;
//import static util.async.Async.newSingleDaemonThreadExecutor;
//import static util.async.Async.runFX;
//import static util.async.Async.runLater;
//import static util.dev.Util.log;
//import static util.functional.Util.*;
//import static util.graphics.Util.layAnchor;
//
///**
// *
// * @author Plutonium_
// */
//@IsWidget
//@Widget.Info(
//    author = "Martin Polakovic",
//    programmer = "Martin Polakovic",
//    name = "Dir Viewer",
//    description = "Displays directory hierarchy and files as thumbnails in a "
//            + "vertically scrollable grid. Intended as simple library",
//    howto = "",
//    notes = "",
//    version = "0.5",
//    year = "2015",
//    group = OTHER
//)
//public class DirViewer extends ClassController {
//
//    @IsConfig(name = "Location", info = "Root directory the contents of to display "
//            + "This is not a file system browser, and it is not possible to "
//            + "visit parent of this directory.")
//    final VarList<File> files = new VarList<>(() -> new File("C:\\"),f -> Config.forValue("File",f));
//    @IsConfig(name = "File filter", info = "Shows only directories and files passing the filter.")
//    final Ѵ<FFilter> filter = new Ѵ<>(FFilter.ALL, f -> viewDir(new TopCell()));
//
//    Cell item = null;
//    CellPane cells = new CellPane(160,220,5);
//    ExecutorService executor = newSingleDaemonThreadExecutor();
//    boolean initialized = false;
//
//    public DirViewer() {
//        files.onListInvalid(list -> viewDir(new TopCell()));
//
//        layAnchor(this,cells.scrollable(),0d);
//
//        setOnMouseClicked(e -> {
//            if(e.getButton()==SECONDARY)
//                visitUp();
//        });
//        setOnKeyPressed(e -> {
//            if(e.getCode()==BACK_SPACE)
//                visitUp();
//        });
//        setOnScroll(Event::consume);
//    }
//
//    @Override
//    public void refresh() {
//        initialized = true;
//        runLater(() -> {
//        viewDir(new TopCell());
//        });
//    }
//
//    @Override
//    public void onClose() {
//        loading++;
//        super.onClose();
//    }
//
//    void visitUp() {
//        if(item!=null && item.parent!=null)
//            viewDir(item.parent);
//    }
//
//    long loading = 0; // allows canceling of lloading, still think it should be handled more natively
//    public void viewDir(Cell dir) {
//        if(!initialized) return; // prevents pointless & inconsistent operations
//
//        loading++;
//        item = dir;
//        if(item==null) {
//            cells.getChildren().clear();
//        } if(item!=null) {
//            long l = loading;
//            Fut.fut(item)
//               .map(Cell::children,executor)
//               // old implementation, which can overload ui thread
//               // .use(newcells ->  cells.getChildren().addAll(map(newcells,Cell::load)),FX)
//               .use(newcells ->  {
//                   runFX(cells.getChildren()::clear);
//                   log(DirViewer.class).info("Cells loading {} started ", l);
//                   List<Cell> sorted = list(newcells);
//                              sorted.sort(by(c -> c.val.getName()));
//                   forEachAfter(10, sorted, c -> {
//                       if(l!=loading) throw new InterruptedException();
//                       Node n = c.load();
//                       runFX(() -> {
//                           cells.getChildren().add(n);
//                           new Anim(n::setOpacity).dur(500).intpl(x -> sqrt(x)).play(); // animate
//                       });
//                   });
//                   log(DirViewer.class).info("Cells loading {} finished", l);
//                },executor)
//               .showProgress(getWidget().getWindow().taskAdd())
//               .run();
//        }
//    }
//
//    private boolean filter(File f) {
//        return !f.isHidden() && f.canRead() && filter.getValue().filter.test(f);
//    }
//    private static boolean file_exists(Cell c, File f) {
//        return c!=null && f!=null && c.all_children.contains(f.getPath().toLowerCase());
//    }
//    public class Cell {
//
//        public final File val;
//        public final Cell parent;
//        private Set<Cell> children = null;
//        private Set<String> all_children = null; // cache
//
//        public Cell(Cell parent, File value) {
//            this.val = value;
//            this.parent = parent;
//        }
//
//        public Set<Cell> children() {
//            if (children == null) buildChildren();
//            return children;
//        }
//
//        boolean isFirstTimeCover = true;
//        Image cover = null;
//        public File getCoverFile() {
//            if(all_children==null) buildChildren();
//            if(val.isDirectory())
//                return getImageT(val,"cover");
//            else {
//                if(ImageFileFormat.isSupported(val))
//                    return val;
//                else {
//                    File i = getImage(val.getParentFile(),FileUtil.getName(val));
//                    if(i==null && parent!=null) return parent.getCoverFile();
//                    return i;
//                }
//            }
//        }
//        public Image getCover() {
//            return cover;
//        }
//        public void setCover(Image i) {
//            cover = i;
//            isFirstTimeCover=false;
//        }
//
//        protected Stream<File> children_files() {
//            return listFiles(val);
//        }
//
//        private void buildChildren() {
//            all_children = new HashSet<>();
//            children = new HashSet<>();
//            List<Cell> fils = new ArrayList<>();
//            children_files().forEach(f -> {
//                all_children.add(f.getPath().toLowerCase());
//                if(DirViewer.this.filter(f)) {
//                    if(!f.isDirectory()) children.add(new Cell(this,f));
//                    else                 fils.add(new Cell(this,f));
//                }
//            });
//            children.addAll(fils);
//        }
//
//        private File getImage(File dir, String name) {
//            if(dir==null) return null;
//            for(ImageFileFormat format: ImageFileFormat.values()) {
//                if (format.isSupported()) {
//                    File f = new File(dir,name + "." + format.toString());
//                    if(dir==val ? file_exists(this,f) : file_exists(parent,f)) return f;
//                }
//            }
//            return null;
//        }
//        private File getImageT(File dir, String name) {
//            if(dir==null) return null;
//
//            for(ImageFileFormat format: ImageFileFormat.values()) {
//                if (format.isSupported()) {
//                    File f = new File(dir,name + "." + format.toString());
//                    if(file_exists(this,f)) return f;
//                }
//            }
//            return null;
//        }
//
//        private VBox root;
//
//        public Node load() {
//            if(root==null) {
//                File f = val;
//                root = new VBox();
//                root.setPrefSize(160,220);
//
//                Thumbnail t = new Thumbnail(160,200);
//                if(isFirstTimeCover) {
//                    File cf = getCoverFile();
//                    t.image.addListener((o,ov,nv) -> setCover(nv));
//                    t.loadImage(cf!=null && cf.exists() ? cf : null); // the exists() check may need to be in Thumbnail itself
//                } else {
//                    t.loadImage(getCover());
//                }
//                t.getPane().setOnMouseClicked(e -> {
//                    if(e.getButton()==PRIMARY && e.getClickCount()==2) {
//                        if(f.isDirectory()) viewDir(this);
//                        else Environment.open(f);
//                        e.consume();
//                    }
//                });
//
//                String n = getName(f);
//                if(n.length()>25) {
//                   boolean single_word = !n.contains(" ");
//                   n = f.isDirectory() || single_word ? n : toS(split(n," "),
//                       s -> s.length()<=1 || !isAlphabetic(s.charAt(0)) ? s : s.substring(0,1),""
//                   );
//                }
//                Label l = new Label(n);
//                root.getChildren().addAll(t.getPane(),l);
//
//                root.setAlignment(Pos.CENTER);
//            }
//            return root;
//        }
//    }
//    public class TopCell extends Cell {
//
//        public TopCell() {
//            super(null,null);
//        }
//
//        @Override
//        protected Stream<File> children_files() {
//            return listFiles(files.list.stream());
//        }
//
//        @Override
//        public File getCoverFile() {
//            return null;
//        }
//
//    }
//    public static enum FFilter {
//        ALL(IS),
//        AUDIO(f -> AudioFileFormat.isSupported(f, Use.APP)),
//        IMAGE(ImageFileFormat::isSupported),
//        NO_IMAGE(f -> !ImageFileFormat.isSupported(f));
//
//        public final Predicate<? super File> filter;
//
//        FFilter(Predicate<? super File> f) {
//            filter = f;
//        }
//    }
//}