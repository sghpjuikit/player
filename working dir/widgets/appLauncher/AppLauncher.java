package appLauncher;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import gui.objects.grid.GridFileThumbCell;
import gui.objects.grid.GridFileThumbCell.AnimateOn;
import gui.objects.grid.GridFileThumbCell.Loader;
import gui.objects.grid.GridView;
import gui.objects.grid.GridView.CellSize;
import gui.objects.hierarchy.Item;
import java.io.File;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import layout.widget.Widget;
import layout.widget.controller.ClassController;
import util.Sort;
import util.access.V;
import util.access.VarEnum;
import util.access.fieldvalue.FileField;
import util.async.executor.FxTimer;
import util.async.future.Fut;
import util.conf.Config.VarList;
import util.conf.Config.VarList.Elements;
import util.conf.IsConfig;
import util.system.Environment;
import util.file.FileSort;
import util.file.FileType;
import util.graphics.Resolution;
import util.graphics.drag.DragUtil;
import util.graphics.drag.Placeholder;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.FOLDER_PLUS;
import static gui.objects.grid.GridFileThumbCell.AnimateOn.IMAGE_CHANGE_1ST_TIME;
import static gui.objects.grid.GridView.CellSize.NORMAL;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.MouseButton.SECONDARY;
import static layout.widget.Widget.Group.OTHER;
import static main.App.APP;
import static util.Sort.ASCENDING;
import static util.async.AsyncKt.*;
import static util.file.FileSort.DIR_FIRST;
import static util.file.FileType.FILE;
import static util.functional.Util.by;
import static util.graphics.Util.setAnchor;
import static util.graphics.drag.DragUtil.installDrag;

// TODO: remove this widget, use DirViewer instead
@Widget.Info(
    author = "Martin Polakovic",
    name = "AppLauncher",
    description = "Launches programs",
//    howto = "",
//    notes = "",
    version = "1",
    year = "2016",
    group = OTHER
)
public class AppLauncher extends ClassController {

    private static final double CELL_TEXT_HEIGHT = 40;

    @IsConfig(name = "Location", info = "Add program")
    final VarList<File> files = new VarList<>(File.class, Elements.NOT_NULL);
    @IsConfig(name = "Thumbnail size", info = "Size of the thumbnail.")
    final V<CellSize> cellSize = new V<>(NORMAL, this::applyCellSize);
    @IsConfig(name = "Thumbnail size ratio", info = "Size ratio of the thumbnail.")
    final V<Resolution> cellSizeRatio = new V<>(Resolution.R_4x5, this::applyCellSize);
    @IsConfig(name = "Thumbnail animate on", info = "Determines when the thumbnail image transition is played.")
    final V<AnimateOn> animateThumbOn = new V<>(IMAGE_CHANGE_1ST_TIME);

    private final GridView<Item, File> grid = new GridView<>(File.class, v -> v.val, cellSize.get().width,cellSize.get().width*cellSizeRatio.get().ratio +CELL_TEXT_HEIGHT,5,5);
    private final ExecutorService executorIO = newSingleDaemonThreadExecutor();
    private final ExecutorService executorThumbs = newSingleDaemonThreadExecutor();
    private final Loader imageLoader = new Loader(executorThumbs, null);
    boolean initialized = false;
    private volatile boolean isResizing = false;
	private final AtomicLong visitId = new AtomicLong(0);
    private final Placeholder placeholder = new Placeholder(
        FOLDER_PLUS, "Click to add launcher or drag & drop a file",
        () -> Environment.chooseFile("Choose program or file", FILE, APP.DIR_HOME, getWidget().getWindow().getStage())
				.ifOk(files.list::setAll)
    );

    @IsConfig(name = "Sort", info = "Sorting effect.")
    final V<Sort> sort = new V<>(ASCENDING, this::applySort);
    @IsConfig(name = "Sort file", info = "Group directories and files - files first, last or no separation.")
    final V<FileSort> sort_file = new V<>(DIR_FIRST, this::applySort);
    @IsConfig(name = "Sort by", info = "Sorting criteria.")
    final VarEnum<FileField<?>> sortBy = new VarEnum<>(FileField.NAME, () -> FileField.FIELDS, f -> applySort());
    @IsConfig(name = "Close on launch", info = "Close this widget when it launches a program.")
    final V<Boolean> closeOnLaunch = new V<>(false);
    @IsConfig(name = "Close on right click", info = "Close this widget when right click is detected.")
    final V<Boolean> closeOnRightClick = new V<>(false);

    public AppLauncher() {
        setPrefSize(500,500);

        files.onListInvalid(list -> visit());
        files.onListInvalid(list -> placeholder.show(this, list.isEmpty()));
        grid.search.field = FileField.PATH;
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
        applyCellSize();
        visit();
    }

    private void visit() {
        if (!initialized) return;
        Item item = new TopItem();
//        item.lastScrollPosition = grid.implGetSkin().getFlow().getPosition(); // can cause null here
	    visitId.incrementAndGet();
        Fut.fut(item)
                .map(executorIO, Item::children)
                .use(executorIO, cells -> cells.sort(buildSortComparator()))
                .use(FX, cells -> {
                    grid.getItemsRaw().setAll(cells);
                    if (item.lastScrollPosition>=0)
                        grid.implGetSkin().setPosition(item.lastScrollPosition);

	                grid.implGetSkin().requestFocus();    // fixes focus problem
                });
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
    private void applySort() {
        grid.getItemsRaw().sort(buildSortComparator());
    }

    void applyCellSize() {
        grid.setCellWidth(cellSize.get().width);
        grid.setCellHeight((cellSize.get().width/cellSizeRatio.get().ratio)+CELL_TEXT_HEIGHT);
    }

    private Comparator<Item> buildSortComparator() {
        Sort sortHetero = sort_file.get().sort, // sorts Files to files and directories
             sortHomo = sort.get(); // sorts each group separately
        FileField<?> field = sortBy.get(); // pre-compute once for consistency and performance
        Comparator<Item> cmpHetero = sortHetero.of(by(i -> i.valType)),
                         cmpHomo = sortHomo.of(by(i -> i.val, field.comparator()));
        return cmpHetero.thenComparing(cmpHomo);
    }

    /**
     * Graphics representing the file. Cells are virtualized just like ListView or TableView does
     * it, but both vertically & horizontally. This avoids loading all files at once and allows
     * unlimited scaling.
     */
    private class Cell extends GridFileThumbCell {
        public Cell() {
            super(imageLoader);
        }

        @Override
        protected AnimateOn computeAnimateOn() {
            return animateThumbOn.get();
        }

        @Override
        protected double computeCellTextHeight() {
            return CELL_TEXT_HEIGHT;
        }

        @Override
        protected void onAction(Item i, boolean edit) {
            doubleClickItem(i);
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
            return files.list.stream().distinct();
        }

        @Override
        protected File getCoverFile() {
            return null;
        }
    }

    public static Optional<File> getPortableAppExe(File f, FileType type) {
    	return type==FileType.DIRECTORY
					? Optional.of(new File(f, f.getName() + ".exe"))
					: Optional.empty();
    }
}