/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package DirViewer;

import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.layout.*;

import Layout.widget.Widget;
import Layout.widget.controller.ClassController;
import Layout.widget.controller.io.Input;
import gui.objects.grid.ImprovedGridCell;
import gui.objects.grid.ImprovedGridView;
import gui.objects.image.Thumbnail;
import unused.TriConsumer;
import util.SingleR;
import util.Sort;
import util.SwitchException;
import util.Util;
import util.access.FieldValue.FileField;
import util.access.V;
import util.access.VarEnum;
import util.animation.Anim;
import util.async.executor.EventReducer;
import util.async.executor.FxTimer;
import util.async.future.Fut;
import util.conf.Config;
import util.conf.Config.VarList;
import util.conf.IsConfig;
import util.file.AudioFileFormat;
import util.file.AudioFileFormat.Use;
import util.file.Environment;
import util.file.FileUtil;
import util.file.ImageFileFormat;
import util.functional.Functors.PƑ0;
import util.graphics.drag.PlaceholderPane;

import static DirViewer.DirViewer.AnimateOn.IMAGE_CHANGE_1ST_TIME;
import static DirViewer.DirViewer.CellSize.NORMAL;
import static DirViewer.DirViewer.FileSort.DIR_FIRST;
import static Layout.widget.Widget.Group.OTHER;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.FOLDER_PLUS;
import static javafx.scene.input.KeyCode.BACK_SPACE;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static main.App.APP;
import static util.Sort.ASCENDING;
import static util.access.FieldValue.FileField.NAME;
import static util.async.Async.FX;
import static util.async.Async.newSingleDaemonThreadExecutor;
import static util.async.Async.runFX;
import static util.async.Async.runLater;
import static util.async.Async.sleep;
import static util.file.Environment.chooseFile;
import static util.file.FileUtil.getName;
import static util.file.FileUtil.listFiles;
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

    private static final double CELL_TEXT_HEIGHT = 20;

    @IsConfig(name = "Location", info = "Root directory the contents of to display "
            + "This is not a file system browser, and it is not possible to "
            + "visit parent of this directory.")
    final VarList<File> files = new VarList<>(() -> new File("C:\\"),f -> Config.forValue(File.class,"File",f));

    private final ImprovedGridView<Item> grid = new ImprovedGridView<>(NORMAL.width,NORMAL.height,5,5);
    private final ExecutorService executorIO = newSingleDaemonThreadExecutor();
    private final ExecutorService executorThumbs = newSingleDaemonThreadExecutor();
    private final ExecutorService executorImage = newSingleDaemonThreadExecutor(); // 2 threads perform better, but cause bugs
    boolean initialized = false;
    private volatile boolean isResizing = false;
    private volatile long visitId = 0;
    final PlaceholderPane placeholder = new PlaceholderPane(FOLDER_PLUS,"Click to view directory", () -> {
        File dir = chooseFile("Choose directory",true, APP.DIR_HOME, APP.windowOwner.getStage());
        if(dir!=null) files.list.setAll(dir);
    });


    private Input<File> input_Dir;
    private final SingleR<PƑ0<File,Boolean>,?> fff = new SingleR<>(this::buildFilter);

    @IsConfig(name = "Thumbnail size", info = "Size of the thumbnail.")
    final V<CellSize> cellSize = new V<>(NORMAL, s -> s.apply(grid));
    @IsConfig(name = "Animate thumbs on", info = "Determines when the thumbnail image transition is played.")
    final V<AnimateOn> animateThumbOn = new V<>(IMAGE_CHANGE_1ST_TIME);
    @IsConfig(name = "File filter", info = "Shows only directories and files passing the filter.")
    final VarEnum<String> filter = new VarEnum<>("All", () -> map(filters,f -> f.name), v -> buildFilter(), f -> revisitCurrent());
    @IsConfig(name = "Sort", info = "Sorting effect.")
    final V<Sort> sort = new V<>(ASCENDING, this::resort);
    @IsConfig(name = "Sort file", info = "Group directories and files - files first, last or no separation.")
    final V<FileSort> sort_file = new V<>(DIR_FIRST, this::resort);
    @IsConfig(name = "Sort by", info = "Sorting criteria.")
    final V<FileField> sortBy = new V<>(NAME, this::resort);

    @IsConfig(name = "Last visited", info = "Last visited item.", editable = false)
    File lastVisited = null;
    Item item = null;   // item, children of which are displayed

    public DirViewer() {
        files.onListInvalid(list -> revisitTop());
        files.onListInvalid(list -> placeholder.show(this, list.isEmpty()));
        grid.setCellFactory(grid -> new Cell());
        setAnchor(this,grid,0d);
        placeholder.showFor(this);

        input_Dir = inputs.create("Root directory", File.class, null, dir -> {
//            if(dir instanceof File && ((File)dir).isDirectory() && ((File)dir).exists())
//                files.setItems((File)dir);
            if(dir!=null && dir.isDirectory() && dir.exists())
                files.setItems(dir);
        });

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
                if(si!=null) si.visit();
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
        // available, so we delay wit runLater
        runLater(this::revisitCurrent);
    }

    // We visit parent, a "back" operation. Note we stop not at top of file hierarchy, but
    // the user source - collection of directories // implement
    void visitUp() {
//        if(item!=null) {
//            if(item.parent!=null) visitDir(item.parent);
//            else if(item instanceof TopItem && files.list.size()==1) visitDir(new Item(null,files.list.get(0)));
//        }
        if(item!=null && item.parent!=null)
            visit(item.parent);
    }

    void visit(Item dir) {
        visit(dir, null);
    }

    void visit(Item dir, Item scrollTo) {
        if(!initialized) return;
        if(item!=null) item.last_gridposition = grid.getSkinn().getFlow().getPosition();
        if(item==dir) return;
        visitId++;

        item = dir;
        lastVisited = dir.val;
        if(item==null) {
            grid.getItems().clear();
            grid.getSkinn().getFlow().requestFocus(); // fixes focus problem
        } if(item!=null) {
            Fut.fut(item)
                    .map(Item::children,executorIO)
                    .use(newcells -> {
                        grid.getItems().setAll(newcells);
                        if(item.last_gridposition>=0)
                            grid.getSkinn().getFlow().setPosition(item.last_gridposition);
                        grid.getSkinn().getFlow().requestFocus(); // fixes focus problem
                    },FX)
                    .showProgress(getWidget().getWindow().taskAdd())
                    .run();
        }
    }

    /** Visits top/root item. Rebuilds entire hierarchy. */
    private void revisitTop() {
        disposeItems();
        visit(new TopItem());
    }

    /** Visits last visited item. Rebuilds entire hierarchy. */
    private void revisitCurrent() {
        disposeItems();
        Item topitem = new TopItem();
        if(lastVisited==null) {
            visit(topitem);
        } else {
            Stack<File> path = new Stack<>(); // nested items we need to rebuild to get to last visited
            File f = lastVisited;
            while(f!=null && !files.list.contains(f)) {
                path.push(f);
                f = f.getParentFile();
            }
            boolean success = files.list.contains(f);
            if(success) {
                executorIO.execute(() -> {
                    Item item = topitem;
                    while (!path.isEmpty()) {
                        File tmp = path.pop();
                        item = stream(item.children()).findAny(child -> child.val.equals(tmp)).orElse(null);
                    }
                    Item i = item;
                    runFX(() -> visit(i));
                });
            } else {
                visit(topitem);
            }
        }
    }

    private void disposeItems() {
        Item i = item;
        while(i!=null && i.parent!=null)
            i = i.parent;
        if(i!=null) i.dispose();
    }

    /** Resorts grid's items according to current sort criteria. */
    private void resort() {
        grid.getItems().sort(buildSortComparator());
    }

    private Comparator<? super Item> buildSortComparator() {
        Sort sortHetero = sort_file.get().sort, // sorts Files to files and directories
                sortHomo = sort.get(); // sorts each group separately
        FileField by = sortBy.get(); // precompute once for consistency and performance
        Comparator<Item> cmpHetero = sortHetero.cmp(by(i -> i.valtype)),
                cmpHomo = sortHomo.cmp(by(i -> (Comparable)by.getOf(i.val)));
        return cmpHetero.thenComparing(cmpHomo);
    }

    private boolean filter(File f) {
        return !f.isHidden() && f.canRead() && fff.get().apply(f);
    }

    private static boolean file_exists(Item c, File f) {
        return c!=null && f!=null && c.all_children.contains(f.getPath().toLowerCase());
    }

    /**
     * Graphics representing the file. Cells are virtualized just like ListView or TableView does
     * it, but both vertically & horizontally. This avoids loading all files at once and allows
     * unlimited scaling.
     */
    private class Cell extends ImprovedGridCell<Item> {
        Pane root;
        Label name;
        Thumbnail thumb;
        EventReducer<Item> setCoverLater = EventReducer.toLast(500, item -> executorThumbs.execute(task(() -> {
            sleep(10); // gives FX thread some space to avoid lag under intense workload
            runFX(() -> {
                if(item==getItem())
                    setCover(item);
            });
        })));

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
                if(item.cover_loadedFull && !isResizing)
                    setCover(item);
                else {
                    thumb.loadImage((File)null); // prevent displaying old content before cover loads
                    // this should call setCoverPost() to allow animations
                    setCoverLater.push(item);
                }
            }
        }

        @Override
        public void updateSelected(boolean selected) {
            super.updateSelected(selected);
            if(thumb!=null && thumb.image.get()!=null) thumb.animationPlayPause(selected);
        }

        private void createGraphics() {
            name = new Label();
            name.setAlignment(Pos.CENTER);
            thumb = new Thumbnail() {
                @Override
                protected Object getRepresentant() {
                    return getItem()==null ? null : getItem().val;
                }
            };
            thumb.getPane().setOnMouseClicked(e -> {
                if(e.getButton()==PRIMARY && e.getClickCount()==2) {
                    getItem().visit();
                    e.consume();
                }
            });
            thumb.getPane().hoverProperty().addListener((o,ov,nv) -> thumb.getView().setEffect(nv ? new ColorAdjust(0,0,0.2,0) : null));
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

        /**
         * Begins loading cover for the item. If item changes meanwhile, the result is stored
         * (it will not need to load again) to the old item, but not showed.
         * <p>
         * Thumbnail quality may be decreased to achieve good performance, while loading high
         * quality thumbnail in the bgr. Each phase uses its own executor.
         */
        private void setCover(Item item) {
            // load thumbnail
            executorThumbs.execute(task(() ->
                    item.loadCover(false, (was_loaded,file,img) -> setCoverPost(item, was_loaded, file, img))
            ));
            // load high quality thumbnail
            executorImage.execute(task(() ->
                    item.loadCover(true, (was_loaded,file,img) -> setCoverPost(item, was_loaded, file, img))
            ));
        }

        private void setCoverPost(Item item, boolean imgAlreadyLoaded, File imgFile, Image img) {
            runFX(() -> {
                if(item==getItem()) { // prevents content inconsistency
                    boolean animate = animateThumbOn.get().needsAnimation(this,imgAlreadyLoaded,imgFile,img);
                    thumb.loadImage(img);
                    thumb.setFile(imgFile);
                    if(animate)
                        new Anim(thumb.getView()::setOpacity).dur(400).intpl(x -> x*x*x*x).play();
                }
            });
        }
    }

    /**
     * File wrapper, content of Cell.
     * We cache various stuff in here, including the cover Image and children files.
     */
    private class Item {

        final File val;
        final FileType valtype;
        final Item parent;
        Set<Item> children = null;      // filtered files
        Set<String> all_children = null; // all files, cache, use instead File.exists to reduce I/O
        Image cover = null;         // cover cache
        File cover_file = null;         // cover file cache
        boolean coverFile_loaded = false, cover_loadedThumb = false, cover_loadedFull = false;
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

        public void dispose() {
            if(children!=null) children.forEach(Item::dispose);
            cover = null;
            if(children!=null) children.clear();
            if(all_children!=null) all_children.clear();
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
                boolean isfile = !f.isDirectory();
                if(isfile) { if(DirViewer.this.filter(f)) children.add(new Item(this,f,FileType.FILE));}
                else       fils.add(new Item(this,f,FileType.DIRECTORY));
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

        public void loadCover(boolean full, TriConsumer<Boolean,File,Image> action) {
            File file = getCoverFile();
            if(full) {
                boolean was_loaded = cover_loadedFull;
                if(!cover_loadedFull) {
                    Image img = Util.loadImageFull(file, cellSize.get().width,cellSize.get().height-CELL_TEXT_HEIGHT);
                    if(img!=null) {
                        cover = img;
                        action.accept(was_loaded,file,cover);
                    }
                    cover_loadedFull = true;
                }
            } else {
                boolean was_loaded = cover_loadedThumb;
                if(!cover_loadedThumb) {
                    Image imgc = Thumbnail.getCached(file, cellSize.get().width,cellSize.get().height-CELL_TEXT_HEIGHT);
                    Image img = imgc!=null ? imgc : Util.loadImageThumb(file, cellSize.get().width,cellSize.get().height-CELL_TEXT_HEIGHT);
                    cover = img;
                    cover_loadedThumb = true;
                }
                action.accept(was_loaded,file,cover);
            }
        }

        // guaranteed to execute only once
        protected File getCoverFile() {
            if(coverFile_loaded) return cover_file;
            coverFile_loaded = true;

            if(all_children==null) buildChildren();
            if(valtype==FileType.DIRECTORY) {
                cover_file = getImageT(val,"cover");
                return cover_file;
            } else {
                // image files are their own thumbnail
                if(ImageFileFormat.isSupported(val)) {
                    cover_file = val;
                    return cover_file;
                } else {
                    File i = getImage(val.getParentFile(),FileUtil.getName(val));
                    if(i==null && parent!=null) return parent.getCoverFile(); // return the parent image if available, needs some work
                    cover_file = i;
                    return cover_file;
                }
            }
        }

        public void visit() {
            if(valtype==FileType.DIRECTORY) DirViewer.this.visit(this);
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

    // Filter summary:
    // because we can not yet serialize functions (see Functors class and Parser) in  a way that
    // stores state such as negation or function chaining, I dont use predicates from Functors'
    // function pool, but 'hardcoded' the filters instead.
    // We use String config to save which one we use.
    private static final List<PƑ0<File,Boolean>> filters = list(
            new PƑ0<>("All",        File.class,Boolean.class, file -> true),
            new PƑ0<>("None",       File.class,Boolean.class, file -> false),
            new PƑ0<>("Audio",      File.class,Boolean.class, file -> AudioFileFormat.isSupported(file, Use.APP)),
            new PƑ0<>("No Audio",   File.class,Boolean.class, file -> !AudioFileFormat.isSupported(file, Use.APP)),
            new PƑ0<>("Image",      File.class,Boolean.class, file -> ImageFileFormat.isSupported(file)),
            new PƑ0<>("No Image",   File.class,Boolean.class, file -> !ImageFileFormat.isSupported(file))
    );

    static {
        AudioFileFormat.formats().forEach(f -> filters.add(new PƑ0<>("Is " + f.name(), File.class,Boolean.class, file -> AudioFileFormat.of(file.toURI())==f)));
        AudioFileFormat.formats().forEach(f -> filters.add(new PƑ0<>("No " + f.name(), File.class,Boolean.class, file -> AudioFileFormat.of(file.toURI())!=f)));
        ImageFileFormat.formats().forEach(f -> filters.add(new PƑ0<>("Is " + f.name(), File.class,Boolean.class, file -> ImageFileFormat.of(file.toURI())==f)));
        ImageFileFormat.formats().forEach(f -> filters.add(new PƑ0<>("No " + f.name(), File.class,Boolean.class, file -> ImageFileFormat.of(file.toURI())!=f)));
    }

    private PƑ0<File,Boolean> buildFilter() {
        String type = filter.getValue();
        return stream(filters)
                .findAny(f -> type.equals(f.name))
                .orElseGet(() -> stream(filters).findAny(f -> "All".equals(f.name)).get());
    }

    private Runnable task(Runnable r) {
        final long id = visitId;
        return () -> {
            if(id==visitId)
                r.run();
        };
    }

    enum CellSize {
        SMALL (80, 100+CELL_TEXT_HEIGHT),
        NORMAL(160,200+CELL_TEXT_HEIGHT),
        LARGE (240,300+CELL_TEXT_HEIGHT),
        GIANT (400,500+CELL_TEXT_HEIGHT);

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
    enum FileType { FILE,DIRECTORY }
    enum FileSort {
        DIR_FIRST(Sort.DESCENDING),
        FILE_FIRST(Sort.ASCENDING),
        NONE(Sort.NONE);

        private final Sort sort;

        FileSort(Sort s) {
            sort = s;
        }
    }
    enum AnimateOn {
        IMAGE_CHANGE, IMAGE_CHANGE_1ST_TIME, IMAGE_CHANGE_FROM_EMPTY;

        public boolean needsAnimation(Cell cell, boolean imgAlreadyLoaded, File imgFile, Image img) {
            if(this== IMAGE_CHANGE) return cell.thumb.image.get()!=img;
            else if(this==IMAGE_CHANGE_1ST_TIME) return cell.thumb.image.get()==null && img!=null;
            else if(this==IMAGE_CHANGE_FROM_EMPTY) return !imgAlreadyLoaded && img!=null;
            else throw new SwitchException(this);
        }
    }
}