package appLauncher;

import gui.objects.image.ImageNode.ImageSize;
import java.io.File;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import gui.objects.grid.GridCell;
import gui.objects.grid.GridView;
import gui.objects.hierarchy.Item;
import gui.objects.image.Thumbnail;
import layout.widget.Widget;
import layout.widget.controller.ClassController;
import util.Sort;
import util.SwitchException;
import util.access.V;
import util.access.fieldvalue.FileField;
import util.animation.Anim;
import util.async.executor.EventReducer;
import util.async.executor.FxTimer;
import util.async.future.Fut;
import util.conf.Config;
import util.conf.Config.VarList;
import util.conf.IsConfig;
import util.file.Environment;
import util.file.FileSort;
import util.file.FileType;
import util.graphics.drag.DragUtil;
import util.graphics.drag.Placeholder;
import util.validation.Constraint;

import static appLauncher.AppLauncher.AnimateOn.IMAGE_CHANGE_1ST_TIME;
import static appLauncher.AppLauncher.CellSize.NORMAL;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.FOLDER_PLUS;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static layout.widget.Widget.Group.OTHER;
import static main.App.APP;
import static util.Sort.ASCENDING;
import static util.access.fieldvalue.FileField.NAME;
import static util.async.Async.*;
import static util.dev.Util.throwIfNotFxThread;
import static util.file.FileSort.DIR_FIRST;
import static util.file.FileType.FILE;
import static util.file.Util.getName;
import static util.functional.Util.by;
import static util.graphics.Util.setAnchor;
import static util.graphics.drag.DragUtil.installDrag;
import static util.validation.Constraint.FileActor.DIRECTORY;

@Widget.Info(
    author = "Martin Polakovic",
    name = "AppLauncher",
    description = "Launches programs",
    howto = "",
    notes = "",
    version = "1",
    year = "2016",
    group = OTHER
)
public class AppLauncher extends ClassController {

    private static final double CELL_TEXT_HEIGHT = 20;

	@Constraint.FileType(DIRECTORY)
    @IsConfig(name = "Location", info = "Add program")
    final VarList<File> files = new VarList<>(File.class, () -> new File("X:\\"),f -> Config.forValue(File.class,"File",f));

    private final GridView<Item, File> grid = new GridView<>(File.class, v -> v.val, NORMAL.width,NORMAL.height,5,5);
    private final ExecutorService executorIO = newSingleDaemonThreadExecutor();
    private final ExecutorService executorThumbs = newSingleDaemonThreadExecutor();
    boolean initialized = false;
    private volatile boolean isResizing = false;
	private final AtomicLong visitId = new AtomicLong(0);
    private final Placeholder placeholder = new Placeholder(
        FOLDER_PLUS, "Click to add launcher or drag & drop a file",
        () -> Environment.chooseFile("Choose program or file", FILE, APP.DIR_HOME, getWidget().getWindow().getStage())
				.ifOk(files.list::setAll)
    );

    @IsConfig(name = "Thumbnail size", info = "Size of the thumbnail.")
    final V<CellSize> cellSize = new V<>(NORMAL, s -> s.apply(grid));
    @IsConfig(name = "Animate thumbs on", info = "Determines when the thumbnail image transition is played.")
    final V<AnimateOn> animateThumbOn = new V<>(IMAGE_CHANGE_1ST_TIME);
    @IsConfig(name = "Sort", info = "Sorting effect.")
    final V<Sort> sort = new V<>(ASCENDING, this::resort);
    @IsConfig(name = "Sort file", info = "Group directories and files - files first, last or no separation.")
    final V<FileSort> sort_file = new V<>(DIR_FIRST, this::resort);
    @IsConfig(name = "Sort by", info = "Sorting criteria.")
    final V<FileField> sortBy = new V<>(NAME, this::resort);
    @IsConfig(name = "Close on launch", info = "Close this widget when it launches a program.")
    final V<Boolean> closeOnLaunch = new V<>(false);
    @IsConfig(name = "Close on right click", info = "Close this widget when right click is detected.")
    final V<Boolean> closeOnRightClick = new V<>(false);

    public AppLauncher() {
        setPrefSize(500,500);

        files.onListInvalid(list -> visit());
        files.onListInvalid(list -> placeholder.show(this, list.isEmpty()));
        grid.primaryFilterField = FileField.NAME_FULL;
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
            if (e.getCode()==ENTER) {
                Item si = grid.selectedItem.get();
                if (si!=null) doubleClickItem(si);
            }
        });
        grid.setOnMouseClicked(e -> {
            if (e.getButton()==SECONDARY && closeOnRightClick.get())
                widget.areaTemp.close();
        });

        installDrag(
            this, FontAwesomeIcon.PLUS_SQUARE_ALT, "Add launcher",
            DragUtil::hasFiles,
            e -> files.list.addAll(DragUtil.getFiles(e))
        );
    }

    @Override
    public void refresh() {
        initialized = true;
        cellSize.applyValue();
        visit();
    }

    private void visit() {
        if (!initialized) return;
        Item item = new TopItem();
//        item.lastScrollPosition = grid.implGetSkin().getFlow().getPosition(); // can cause null here
	    visitId.incrementAndGet();
        Fut.fut(item)
                .map(Item::children,executorIO)
                .use(cells -> cells.sort(buildSortComparator()),executorIO)
                .use(cells -> {
                    grid.getItemsRaw().setAll(cells);
                    if (item.lastScrollPosition>=0)
                        grid.implGetSkin().getFlow().setPosition(item.lastScrollPosition);

	                grid.implGetSkin().getFlow().requestFocus();    // fixes focus problem
                },FX)
                .run();
    }

    private void doubleClickItem(Item i) {
        if (closeOnLaunch.get()) {
            widget.areaTemp.close();
            run(250, () -> Environment.open(i.val));
        } else {
            Environment.open(i.val);
        }
    }

    /** Resorts grid's items according to current sort criteria. */
    private void resort() {
        grid.getItemsRaw().sort(buildSortComparator());
    }

    private Comparator<Item> buildSortComparator() {
        Sort sortHetero = sort_file.get().sort, // sorts Files to files and directories
             sortHomo = sort.get(); // sorts each group separately
        FileField field = sortBy.get(); // precompute once for consistency and performance
        Comparator<Item> cmpHetero = sortHetero.cmp(by(i -> i.valType)),
                         cmpHomo = sortHomo.cmp(by(i -> i.val, field.comparator()));
        return cmpHetero.thenComparing(cmpHomo);
    }

    /**
     * Graphics representing the file. Cells are virtualized just like ListView or TableView does
     * it, but both vertically & horizontally. This avoids loading all files at once and allows
     * unlimited scaling.
     */
    private class Cell extends GridCell<Item,File> {
        Pane root;
        Label name;
        Thumbnail thumb;
        EventReducer<Item> setCoverLater = EventReducer.toLast(100, item -> executorThumbs.execute(task(() -> {
            sleep(10); // gives FX thread some space to avoid lag under intense workload
            runFX(() -> {
                if (item==getItem())
                    setCoverNow(item);
            });
        })));

        @Override
        protected void updateItem(Item item, boolean empty) {
            super.updateItem(item, empty);

            if (item==null) {
                // empty cell has no graphics
                // we do not clear the content of the graphics however
                setGraphic(null);
            } else {
                if (root==null) {
                    // we create graphics only once and only when first requested
                    createGraphics();
                    // we set graphics only once (when creating it)
                    setGraphic(root);
                }
                // if cell was previously empty, we set graphics back
                // this improves performance compared to setting it every time
                if (getGraphic()!=root) setGraphic(root);

                // set name
                name.setText(getName(item.val));
                // set cover
                // The item caches the cover Image, so it is only loaded once. That is a heavy op.
                // This method can be called very frequently and:
                //     - block ui thread when scrolling
                //     - reduce ui performance when resizing
                // We solve this by delaying the image loading & drawing. We reduce subsequent
                // invokes into single update (last).
	            boolean loadLater = item.cover_loadedFull; // && !isResizing;
	            if (loadLater) setCoverNow(item);
	            else setCoverLater(item);
            }
        }

        @Override
        public void updateSelected(boolean selected) {
            super.updateSelected(selected);
            if (thumb!=null && thumb.image.get()!=null) thumb.animationPlayPause(selected);
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
                if (e.getButton()==PRIMARY && e.getClickCount()==2) {
                    doubleClickItem(getItem());
                    e.consume();
                }
            });
            thumb.getPane().hoverProperty().addListener((o,ov,nv) -> thumb.getView().setEffect(nv ? new ColorAdjust(0,0,0.2,0) : null));
            root = new Pane(thumb.getPane(),name) {
                // Cell layout should be fast - gets called multiple times on grid resize.
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
         * <p/>
         * Thumbnail quality may be decreased to achieve good performance, while loading high
         * quality thumbnail in the bgr. Each phase uses its own executor.
         */
        private void setCoverNow(Item item) {
	        throwIfNotFxThread();
	        if (item.cover_loadedFull) {
		        setCoverPost(item, true, item.cover_file, item.cover);
	        } else {
	            ImageSize size = thumb.calculateImageLoadSize();
		        //            executorThumbs.execute(task(() ->
		        item.loadCover(false, size.width, size.height, (was_loaded,file,img) -> setCoverPost(item,was_loaded,file,img));
		        //            ));
	        }
        }

	    private void setCoverLater(Item item) {
		    throwIfNotFxThread();
		    thumb.loadImage((File) null); // prevent displaying old content before cover loads
		    setCoverLater.push(item);
	    }

        private void setCoverPost(Item item, boolean imgAlreadyLoaded, File imgFile, Image img) {
            runFX(() -> {
                if (item==getItem()) { // prevents content inconsistency
                    boolean animate = animateThumbOn.get().needsAnimation(this,imgAlreadyLoaded,imgFile,img);
                    thumb.loadImage(img, imgFile);
                    if (animate)
                        new Anim(thumb.getView()::setOpacity).dur(400).intpl(x -> x*x*x*x).play();
                }
            });
        }
    }

    private static class FItem extends Item {

        public FItem(Item parent, File value, util.file.FileType type) {
            super(parent, value, type);
        }

        @Override
        protected FItem createItem(Item parent, File value, util.file.FileType type) {
			return getPortableAppExe(value, type)
				.map(f -> new FItem(parent, getPortableAppExe(value, type).orElse(null), FileType.FILE))
				.orElseGet(() -> new FItem(parent, value, type));
        }

    }
    private class TopItem extends FItem {

        public TopItem() {
            super(null,null,null);
        }

        @Override
        protected Stream<File> children_files() {
            return files.list.stream();
        }

        @Override
        protected File getCoverFile() {
            return null;
        }
    }

    private Runnable task(Runnable r) {
        final long id = visitId.get();
        return () -> {
            if (id==visitId.get())
                r.run();
        };
    }

    public static Optional<File> getPortableAppExe(File f, FileType type) {
    	return type==FileType.DIRECTORY
					? Optional.of(new File(f, f.getName() + ".exe"))
					: Optional.empty();
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

        void apply(GridView<?,?> grid) {
            grid.setCellWidth(width);
            grid.setCellHeight(height);
        }
    }
    enum AnimateOn {
        IMAGE_CHANGE, IMAGE_CHANGE_1ST_TIME, IMAGE_CHANGE_FROM_EMPTY;

        public boolean needsAnimation(Cell cell, boolean imgAlreadyLoaded, File imgFile, Image img) {
            if (this== IMAGE_CHANGE) return cell.thumb.image.get()!=img;
            else if (this==IMAGE_CHANGE_FROM_EMPTY) return cell.thumb.image.get()==null && img!=null;
            else if (this==IMAGE_CHANGE_1ST_TIME) return !imgAlreadyLoaded && img!=null;
            else throw new SwitchException(this);
        }
    }
}