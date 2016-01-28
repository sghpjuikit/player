/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package DirViewer;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.layout.*;

import Configuration.Config;
import Configuration.Config.VarList;
import Configuration.IsConfig;
import Layout.widget.Widget;
import Layout.widget.controller.ClassController;
import gui.objects.grid.ImprovedGridCell;
import gui.objects.grid.ImprovedGridView;
import gui.objects.image.Thumbnail;
import util.File.AudioFileFormat;
import util.File.AudioFileFormat.Use;
import util.File.Environment;
import util.File.FileUtil;
import util.File.ImageFileFormat;
import util.Sort;
import util.Util;
import util.access.FieldValue.FileField;
import util.access.V;
import util.animation.Anim;
import util.async.executor.EventReducer;
import util.async.executor.FxTimer;
import util.async.future.Fut;
import util.graphics.drag.PlaceholderPane;

import static Layout.widget.Widget.Group.OTHER;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.FOLDER_PLUS;
import static javafx.scene.input.KeyCode.BACK_SPACE;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static main.App.APP;
import static util.File.Environment.chooseFile;
import static util.File.FileUtil.getName;
import static util.File.FileUtil.listFiles;
import static util.access.FieldValue.FileField.NAME;
import static util.async.Async.FX;
import static util.async.Async.newSingleDaemonThreadExecutor;
import static util.async.Async.runFX;
import static util.async.Async.runLater;
import static util.functional.Util.*;
import static util.graphics.Util.setAnchor;

/**
 *
 * @author Plutonium_
 */
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
    final VarList<File> files = new VarList<>(() -> new File("C:\\"),f -> Config.forValue(File.class,"File",f));

    final PlaceholderPane placeholder = new PlaceholderPane(FOLDER_PLUS,"Click to view directory", () -> {
        File dir = chooseFile("Choose directory",true, APP.DIR_HOME, APP.windowOwner.getStage());
        if(dir!=null) files.list.setAll(dir);
    });
    Item item = null;   // item, children of which are displayed
    ImprovedGridView<Item> grid = new ImprovedGridView<>(CellSize.NORMAL.width,CellSize.NORMAL.height,5,5);
    ExecutorService executor = newSingleDaemonThreadExecutor();
    boolean initialized = false;
    private volatile boolean isResizing = false;

    @IsConfig(name = "Thumbnail size", info = "Size of the thumbnail.")
    final V<CellSize> cellSize = new V<>(CellSize.NORMAL, s -> s.apply(grid));
    @IsConfig(name = "File filter", info = "Shows only directories and files passing the filter.")
    final V<FFilter> filter = new V<>(FFilter.ALL, f -> visit(new TopItem()));
    @IsConfig(name = "Sort", info = "Sorting effect.")
    final V<Sort> sort = new V<>(Sort.ASCENDING, s -> resort());
    @IsConfig(name = "Sort by", info = "Sorting criteria.")
    final V<FileField> sortBy = new V<>(NAME, f -> resort());

    public DirViewer() {
        files.onListInvalid(list -> visit(new TopItem()));
        files.onListInvalid(list -> placeholder.show(this, list.isEmpty()));
        grid.setCellFactory(grid -> new Cell());
        setAnchor(this,grid,0d);
        placeholder.showFor(this);

        // delay cell loading when content is being resized (increases resize performance)
        double delay = 200; // ms
        FxTimer resizeTimer = new FxTimer(delay, 1, () -> isResizing = false);
        grid.widthProperty().addListener((o,ov,nv) -> isResizing = true);
        grid.heightProperty().addListener((o,ov,nv) -> isResizing = true);
        grid.widthProperty().addListener((o,ov,nv) -> resizeTimer.start(300));
        grid.heightProperty().addListener((o,ov,nv) -> resizeTimer.start(300));

        grid.setOnKeyPressed(e -> {
            if(e.getCode()==ENTER) {
                Item si = grid.selectedItem.get();
                if(si!=null) si.open();
            }
            if(e.getCode()==BACK_SPACE)
                visitUp();
        });
        grid.setOnMouseClicked(e -> {
            if(e.getButton()==SECONDARY)
                visitUp();
        });
        setOnScroll(Event::consume);
    }

    @Override
    public void refresh() {
        initialized = true;
        cellSize.applyValue();
        // temporary bugfix, (we use progress indicator of the window this widget is loaded
        // in, but when this refresh() method is called its just during loading and window is not yet
        // available, so we delay
        runLater(() -> {
            visit(new TopItem());
        });
    }

    // We visit parent, a "back" operation. Note we stop not at top of file hierarchy, but
    // the user source - collection of directories // this should be implemented
    void visitUp() {
//        if(item!=null) {
//            if(item.parent!=null) visitDir(item.parent);
//            else if(item instanceof TopItem && files.list.size()==1) visitDir(new Item(null,files.list.get(0)));
//        }
        if(item!=null && item.parent!=null)
            visit(item.parent);
    }

    public void visit(Item dir) {
        visit(dir, null);
    }

    public void visit(Item dir, Item scrollTo) {
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
                   grid.requestFocus();
               },FX)
               .showProgress(getWidget().getWindow().taskAdd())
               .run();
        }
    }

    /** Resorts grid's items according to current sort criteria. */
    private void resort() {
        grid.getItems().sort(buildSortComparator());
    }

    private Comparator<? super Item> buildSortComparator() {
        Sort s = sort.get();
        FileField by = sortBy.get();
        return s.cmp(by(i -> (Comparable)by.getOf(i.val)));
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
    private class Cell extends ImprovedGridCell<Item>{
        Pane root;
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
                    // we set graphics only once (when creating it)
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
                if(item.cover_loaded && !isResizing)
                    setCover(item);
                else {
                    thumb.loadImage((File)null); // prevent displaying old content before cover loads
                    setCoverLater.push(item);
                }
            }
        }

        private void createGraphics() {
            name = new Label();
            name.setAlignment(Pos.CENTER);
            thumb = new Thumbnail();
            thumb.getPane().setOnMouseClicked(e -> {
                if(e.getButton()==PRIMARY && e.getClickCount()==2) {
                    getItem().open();
                    e.consume();
                }
            });
            thumb.getView().hoverProperty().addListener((o,ov,nv) -> thumb.getView().setEffect(nv ? new ColorAdjust(0,0,0.4,0) : null));
            root = new Pane(thumb.getPane(),name) {
                // Cell layouting should be fast - gets called multiple times when grid resizes.
                // Why not use custom pane for more speed if we can.
                @Override
                protected void layoutChildren() {
                    double w = getWidth();
                    double h = getHeight();
                    thumb.getPane().resizeRelocate(0,0,w,h-CELL_TEXT_HEIGHT);
                    name.resizeRelocate(0,h-CELL_TEXT_HEIGHT,w,CELL_TEXT_HEIGHT);
                }
            };
            root.setPadding(Insets.EMPTY);
            root.setMinSize(-1,-1);
            root.setPrefSize(-1,-1);
            root.setMaxSize(-1,-1);
        }

        private void setCover(Item item) {
            // not sure how to handle this asynchronously, needs some work, particularly if we want
            // to bind this to some progress indicator
            // the below seems to already help, but not entirely? hm. investigate
            executor.execute(() -> {
                item.loadCover((was_loaded,img) -> {
                    runFX(() -> {
                        thumb.loadImage(img);
                        if(!was_loaded && img!=null) {
                            new Anim(thumb.getView()::setOpacity).dur(400).intpl(x -> x*x*x*x).play();
                        }
                    });
                });
            });

//            item.loadCover((was_loaded,img) -> {
//                thumb.loadImage(img);
//                if(!was_loaded && img!=null) {
//                    new Anim(thumb.getView()::setOpacity).dur(400).intpl(x -> x*x*x*x).play();
//                }
//            });
        }
    }
    // File wrapper, content of Cell.
    // We cache various stuff in here, including the cover Image and children files.
    private class Item {

        final File val;
        final FileType valtype;
        final Item parent;
        Set<Item> children = null;      // filtered files
        Set<String> all_children = null; // all files, cache, use instead File.exists to reduce I/O
        Image cover = null;         // cover cache
        File cover_file = null;         // cover file cache
        boolean cover_loaded = false;
        double last_gridposition = -1;

        public Item(Item parent, File value, FileType valtype) {
            this.val = value;
            this.valtype = valtype;
            this.parent = parent;
        }

        public List<Item> children() {
            if (children == null) buildChildren();
            List<Item> l = list(children);
                       l.sort(buildSortComparator());
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
                    if(!f.isDirectory()) children.add(new Item(this,f,FileType.FILE));
                    else                 fils.add(new Item(this,f,FileType.DIRECTORY));
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
                Image imgc = Thumbnail.getCached(img_file, cellSize.get().width,cellSize.get().height-CELL_TEXT_HEIGHT);
                Image img = imgc!=null ? imgc : Util.loadImage(img_file, cellSize.get().width,cellSize.get().height-CELL_TEXT_HEIGHT);
                cover = img;
                cover_file = img_file;
                cover_loaded = true;
            }
            action.accept(was_loaded,cover);
        }

        protected File getCoverFile() {
            if(all_children==null) buildChildren();
            if(valtype==FileType.DIRECTORY)
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

        public void open() {
            if(valtype==FileType.DIRECTORY) visit(this);
            else Environment.open(val);
        }

    }
    private class TopItem extends Item {

        public TopItem() {
            super(null,null,null);
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

    private static final double CELL_TEXT_HEIGHT = 20;
    public static enum CellSize {
        SMALL(80,100+CELL_TEXT_HEIGHT),
        NORMAL(160,200+CELL_TEXT_HEIGHT),
        LARGE(240,300+CELL_TEXT_HEIGHT),
        GIANT(400,500+CELL_TEXT_HEIGHT);

        final double width;
        final double height;

        CellSize(double width, double height) {
            this.width = width;
            this.height = height;
        }

        void apply(ImprovedGridView<?> grid) {
            grid.setCellWidth(width);
            grid.setCellHeight(height);
        }
    }
    public static enum FileType { FILE,DIRECTORY; }

}