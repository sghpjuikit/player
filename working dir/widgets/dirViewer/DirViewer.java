package dirViewer;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;

import gui.objects.grid.GridCell;
import gui.objects.grid.GridView;
import gui.objects.hierarchy.Item;
import gui.objects.image.Thumbnail;
import layout.widget.Widget;
import layout.widget.controller.ClassController;
import util.SingleR;
import util.Sort;
import util.SwitchException;
import util.access.V;
import util.access.VarEnum;
import util.access.fieldvalue.FileField;
import util.animation.Anim;
import util.async.executor.EventReducer;
import util.async.future.Fut;
import util.conf.Config;
import util.conf.Config.VarList;
import util.conf.IsConfig;
import util.file.Environment;
import util.file.FileSort;
import util.file.FileType;
import util.functional.Functors.PƑ0;
import util.graphics.drag.Placeholder;

import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.FOLDER_PLUS;
import static dirViewer.DirViewer.AnimateOn.IMAGE_CHANGE_1ST_TIME;
import static dirViewer.DirViewer.CellSize.NORMAL;
import static javafx.scene.input.KeyCode.BACK_SPACE;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static layout.widget.Widget.Group.OTHER;
import static main.App.APP;
import static util.Sort.ASCENDING;
import static util.Util.capitalize;
import static util.access.fieldvalue.FileField.NAME;
import static util.async.Async.*;
import static util.file.Environment.chooseFile;
import static util.file.FileSort.DIR_FIRST;
import static util.file.FileType.DIRECTORY;
import static util.file.Util.getName;
import static util.file.Util.listFiles;
import static util.functional.Util.*;
import static util.graphics.Util.setAnchor;

/**
 *
 * @author Martin Polakovic
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
    final VarList<File> files = new VarList<>(() -> new File("C:\\"), f -> Config.forValue(File.class, "File", f));

    private final GridView<Item, File> grid = new GridView<>(File.class, v -> v.val, NORMAL.width, NORMAL.height, 5, 5);
    private final ExecutorService executorIO = newSingleDaemonThreadExecutor();
    private final ExecutorService executorThumbs = newSingleDaemonThreadExecutor();
    private final ExecutorService executorImage = newSingleDaemonThreadExecutor(); // 2 threads perform better, but cause bugs
    boolean initialized = false;
    private volatile long visitId = 0;
    private final Placeholder placeholder = new Placeholder(FOLDER_PLUS, "Click to view directory", () -> {
        File dir = chooseFile("Choose directory", DIRECTORY, APP.DIR_HOME, APP.windowOwner.getStage());
        if (dir != null) files.list.setAll(dir);
    });
    private final SingleR<PƑ0<File, Boolean>, ?> filterPredicate = new SingleR<>(this::buildFilter);

    @IsConfig(name = "Thumbnail size", info = "Size of the thumbnail.")
    final V<CellSize> cellSize = new V<>(NORMAL, s -> s.apply(grid));
    @IsConfig(name = "Animate thumbs on", info = "Determines when the thumbnail image transition is played.")
    final V<AnimateOn> animateThumbOn = new V<>(IMAGE_CHANGE_1ST_TIME);
    @IsConfig(name = "File filter", info = "Shows only directories and files passing the filter.")
    final VarEnum<String> filter = new VarEnum<>("File - all", () -> map(filters, f -> f.name), v -> buildFilter(), f -> revisitCurrent());
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
        setAnchor(this, grid, 0d);
        placeholder.showFor(this);

        inputs.create("Root directory", File.class, null, dir -> {
            if (dir != null && dir.isDirectory() && dir.exists())
                files.setItems(dir);
        });

        grid.setOnKeyPressed(e -> {
            if (e.getCode() == ENTER) {
                Item si = grid.selectedItem.get();
                if (si != null) doubleClickItem(si);
            }
            if (e.getCode() == BACK_SPACE)
                visitUp();
        });
        grid.setOnMouseClicked(e -> {
            if (e.getButton() == SECONDARY)
                visitUp();
        });
        setOnScroll(Event::consume);
    }

    @Override
    public void refresh() {
        initialized = true;
        cellSize.applyValue();
        // temporary bug fix, (we use progress indicator of the window this widget is loaded
        // in, but when this refresh() method is called its just during loading and window is not yet
        // available, so we delay wit runLater
        runLater(this::revisitCurrent);
    }

    void visitUp() {
        // We visit parent, a "back" operation. Note we stop not at top of file hierarchy, but
        // the user source - collection of directories << TODO
        // if(item!=null) {
        //     if(item.parent!=null) visitDir(item.parent);
        //     else if(item instanceof TopItem && files.list.size()==1) visitDir(new Item(null,files.list.get(0)));
        // }
        if (item != null && item.parent != null) {
            Item toDispose = item;
            visit(item.parent);
            toDispose.disposeChildren(); // item.parent has become item
        }
    }

    private void visit(Item dir) {
        visit(dir, null);
    }

    private void visit(Item dir, Item scrollTo) {
        if (!initialized) return;
        if (item != null) item.last_gridposition = grid.implGetSkin().getFlow().getPosition();
        if (item == dir) return;
        if (item != null && item.isHChildOf(dir)) item.disposeChildren();
        visitId++;

        item = dir;
        lastVisited = dir.val;
        Fut.fut(item)
                .map(Item::children, executorIO)
                .use(cells -> cells.sort(buildSortComparator()), executorIO)
                .use(cells -> {
                    grid.getItemsRaw().setAll(cells);
                    if (item.last_gridposition >= 0)
                        grid.implGetSkin().getFlow().setPosition(item.last_gridposition);

                    grid.requestFocus();    // fixes focus problem
                }, FX)
                .showProgress(getWidget().getWindow().taskAdd())
                .run();
    }

    /**
     * Visits top/root item. Rebuilds entire hierarchy.
     */
    private void revisitTop() {
        disposeItems();
        visit(new TopItem());
    }

    /**
     * Visits last visited item. Rebuilds entire hierarchy.
     */
    private void revisitCurrent() {
        disposeItems();
        Item topItem = new TopItem();
        if (lastVisited == null) {
            visit(topItem);
        } else {
            Stack<File> path = new Stack<>(); // nested items we need to rebuild to get to last visited
            File f = lastVisited;
            while (f != null && !files.list.contains(f)) {
                path.push(f);
                f = f.getParentFile();
            }
            boolean success = files.list.contains(f);
            if (success) {
                executorIO.execute(() -> {
                    Item item = topItem;
                    while (!path.isEmpty()) {
                        File tmp = path.pop();
                        item = stream(item.children()).findAny(child -> child.val.equals(tmp)).orElse(null);
                    }
                    Item i = item;
                    runFX(() -> visit(i));
                });
            } else {
                visit(topItem);
            }
        }
    }

    private void disposeItems() {
        Item i = item==null ? null : item.getHRoot();
        if (i != null) i.dispose();
    }

    private void doubleClickItem(Item i) {
        if (i.valtype == DIRECTORY) DirViewer.this.visit(i);
        else Environment.open(i.val);
    }

    /**
     * Resorts grid's items according to current sort criteria.
     */
    private void resort() {
        grid.getItemsRaw().sort(buildSortComparator());
    }

    private Comparator<Item> buildSortComparator() {
        Sort sortHetero = sort_file.get().sort,     // sorts Files to files and directories
	         sortHomo = sort.get();                 // sorts each group separately
        FileField field = sortBy.get();             // precompute once for consistency and performance
        Comparator<Item> cmpHetero = sortHetero.cmp(by(i -> i.valtype)),
                         cmpHomo = sortHomo.cmp(by(i -> i.val, field.comparator()));
        return cmpHetero.thenComparing(cmpHomo);
    }

    private boolean filter(File f) {
        return !f.isHidden() && f.canRead() && filterPredicate.get().apply(f);
    }

    /**
     * Graphics representing the file. Cells are virtualized just like ListView or TableView does
     * it, but both vertically & horizontally. This avoids loading all files at once and allows
     * unlimited scaling.
     */
    private class Cell extends GridCell<Item, File> {
        Pane root;
        Label name;
        Thumbnail thumb;
        EventReducer<Item> setCoverLater = EventReducer.toLast(100, item -> executorThumbs.execute(task(() -> {
            sleep(10); // gives FX thread some space to avoid lag under intense workload
            runFX(() -> {
                if (item == getItem())
                    setCoverNow(item);
            });
        })));

        @Override
        protected void updateItem(Item item, boolean empty) {
            if(getItem() == item) return;
            super.updateItem(item, empty);

            if (item == null) {
                // empty cell has no graphics
                // we do not clear the content of the graphics however
                setGraphic(null);
            } else {
                if (root == null) {
                    // we create graphics only once and only when first requested
                    createGraphics();
                    // we set graphics only once (when creating it)
                    setGraphic(root);
                }
                // if cell was previously empty, we set graphics back
                // this improves performance compared to setting it every time
                if (getGraphic() != root) setGraphic(root);

                // set name
                name.setText(getName(item.val));
                // set cover
                // The item caches the cover Image, so it is only loaded once. That is a heavy op.
                // This method can be called very frequently and:
                //     - block ui thread when scrolling
                //     - reduce ui performance when resizing
                // Solved by delaying the image loading & drawing, which reduces subsequent
                // invokes into single update (last).
	            boolean loadLater = item.cover_loadedFull; // && !isResizing;
                if (loadLater) setCoverNow(item);
                else setCoverLater(item);
            }
        }

        @Override
        public void updateSelected(boolean selected) {
            super.updateSelected(selected);
            if (thumb != null && thumb.image.get() != null) thumb.animationPlayPause(selected);
        }

        private void createGraphics() {
            name = new Label();
            name.setAlignment(Pos.CENTER);
            thumb = new Thumbnail() {
                @Override
                protected Object getRepresentant() {
                    return getItem() == null ? null : getItem().val;
                }
            };
            thumb.getPane().setOnMouseClicked(e -> {
                if (e.getButton() == PRIMARY && e.getClickCount() == 2) {
                    doubleClickItem(getItem());
                    e.consume();
                }
            });
            thumb.getPane().hoverProperty().addListener((o, ov, nv) -> thumb.getView().setEffect(nv ? new ColorAdjust(0, 0, 0.2, 0) : null));
            root = new Pane(thumb.getPane(), name) {
                // Cell layout should be fast - gets called multiple times on grid resize.
                // Why not use custom pane for more speed if we can.
                @Override
                protected void layoutChildren() {
                    double w = getWidth();
                    double h = getHeight();
                    thumb.getPane().resizeRelocate(0, 0, w, h - CELL_TEXT_HEIGHT);
                    name.resizeRelocate(0, h - CELL_TEXT_HEIGHT, w, CELL_TEXT_HEIGHT);
                }
            };
            root.setPadding(Insets.EMPTY);
            root.setMinSize(-1, -1);
            root.setPrefSize(-1, -1);
            root.setMaxSize(-1, -1);
        }

        /**
         * Begins loading cover for the item. If item changes meanwhile, the result is stored
         * (it will not need to load again) to the old item, but not showed.
         * <p/>
         * Thumbnail quality may be decreased to achieve good performance, while loading high
         * quality thumbnail in the bgr. Each phase uses its own executor.
         * <p/>
         * Must be called on FX thread.
         */
        private void setCoverNow(Item item) {
	        if(!Platform.isFxApplicationThread()) throw new IllegalStateException("Must be on FX thread");

	        if(item.cover_loadedFull) {
		        setCoverPost(item, true, item.cover_file, item.cover);
	        } else {
	            double width = cellSize.get().width,
	                   height = cellSize.get().height - CELL_TEXT_HEIGHT;
	            // load thumbnail
	            executorThumbs.execute(task(() ->
	                item.loadCover(false, width, height, (was_loaded, file, img) -> setCoverPost(item, was_loaded, file, img))
	            ));
	            // load high quality thumbnail
	            executorImage.execute(task(() ->
	                item.loadCover(true, width, height, (was_loaded, file, img) -> setCoverPost(item, was_loaded, file, img))
	            ));
	        }
        }

	    private void setCoverLater(Item item) {
		    if(!Platform.isFxApplicationThread()) throw new IllegalStateException("Must be on FX thread");

		    thumb.loadImage((File) null); // prevent displaying old content before cover loads
		    setCoverLater.push(item);
	    }

	    /** Finished loading of the cover. */
        private void setCoverPost(Item item, boolean imgAlreadyLoaded, File imgFile, Image img) {
            runFX(() -> {
                if (item == getItem()) { // prevents content inconsistency
                    boolean animate = animateThumbOn.get().needsAnimation(this, imgAlreadyLoaded, imgFile, img);
                    thumb.loadImage(img, imgFile);
                    if (animate)
                        new Anim(thumb.getView()::setOpacity).dur(400).intpl(x -> x * x * x * x).play();
                }
            });
        }
    }

    private class FItem extends Item {

        public FItem(Item parent, File value, FileType type) {
            super(parent, value, type);
        }

        @Override
        protected Item createItem(Item parent, File value, FileType type) {
            return new FItem(parent, value, type);
        }

        @Override
        protected boolean filterChildFile(File f) {
            return DirViewer.this.filter(f);
        }
    }

    private class TopItem extends FItem {

        public TopItem() {
            super(null, null, null);
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

    /**
     * Filter summary: because we can not yet serialize functions (see {@link util.functional.Functors} and
     * {@link util.parsing.Parser}) in  a way that stores (e.g. negation or function chaining), we do not use
     * predicates from function pool, but hardcoded filters, which we look up by name.
     * <p/>
     * We use String config field to save which filter we use. Of course, we give up filter chaining and other stuff...
     * For now, it is good enough.
     */
    private static final List<PƑ0<File, Boolean>> filters = list();

    static {
        filters.add(new PƑ0<>("File - all", File.class, Boolean.class, file -> true));
        filters.add(new PƑ0<>("File - none", File.class, Boolean.class, file -> false));
        APP.mimeTypes.setOfGroups().forEach(group -> {
            filters.add(new PƑ0<>("Mime - is " + capitalize(group), File.class, Boolean.class, file -> group.equals(APP.mimeTypes.ofFile(file).getGroup())));
            filters.add(new PƑ0<>("Mime - no " + capitalize(group), File.class, Boolean.class, file -> !group.equals(APP.mimeTypes.ofFile(file).getGroup())));
        });
        APP.mimeTypes.setOfExtensions().forEach(extension -> {
            filters.add(new PƑ0<>("Type - is " + extension, File.class, Boolean.class, file -> APP.mimeTypes.ofFile(file).isOfType(extension)));
            filters.add(new PƑ0<>("Type - no " + extension, File.class, Boolean.class, file -> !APP.mimeTypes.ofFile(file).isOfType(extension)));
        });
    }

    private PƑ0<File, Boolean> buildFilter() {
        String type = filter.getValue();
        return stream(filters)
                .findAny(f -> type.equals(f.name))
                .orElseGet(() -> stream(filters).findAny(f -> "File - all".equals(f.name)).get());
    }

    private Runnable task(Runnable r) {
        final long id = visitId;
        return () -> {
            if (id == visitId)
                r.run();
        };
    }

    enum CellSize {
        SMALL(80, 100 + CELL_TEXT_HEIGHT),
        NORMAL(160, 200 + CELL_TEXT_HEIGHT),
        LARGE(240, 300 + CELL_TEXT_HEIGHT),
        GIANT(400, 500 + CELL_TEXT_HEIGHT);

        final double width;
        final double height;

        CellSize(double width, double height) {
            this.width = width;
            this.height = height;
        }

        void apply(GridView<?, ?> grid) {
            grid.setCellWidth(width);
            grid.setCellHeight(height);
        }
    }
    enum AnimateOn {
        IMAGE_CHANGE, IMAGE_CHANGE_1ST_TIME, IMAGE_CHANGE_FROM_EMPTY;

        public boolean needsAnimation(Cell cell, boolean imgAlreadyLoaded, File imgFile, Image img) {
            if (this == IMAGE_CHANGE)
                return cell.thumb.image.get() != img;
            else if (this == IMAGE_CHANGE_FROM_EMPTY)
                return cell.thumb.image.get() == null && img != null;
            else if (this == IMAGE_CHANGE_1ST_TIME)
                return !imgAlreadyLoaded && img != null;
            else
                throw new SwitchException(this);
        }
    }
}