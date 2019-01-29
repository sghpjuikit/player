package dirViewer;

import java.io.File;
import java.util.Comparator;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ScrollEvent;
import sp.it.pl.gui.objects.grid.GridFileThumbCell;
import sp.it.pl.gui.objects.grid.GridFileThumbCell.Loader;
import sp.it.pl.gui.objects.grid.GridView;
import sp.it.pl.gui.objects.grid.GridView.CellSize;
import sp.it.pl.gui.objects.hierarchy.Item;
import sp.it.pl.gui.objects.image.Thumbnail.FitFrom;
import sp.it.pl.gui.objects.window.stage.Window;
import sp.it.pl.layout.widget.Widget;
import sp.it.pl.layout.widget.controller.LegacyController;
import sp.it.pl.layout.widget.controller.SimpleController;
import sp.it.pl.util.Sort;
import sp.it.pl.util.access.V;
import sp.it.pl.util.access.VarEnum;
import sp.it.pl.util.access.fieldvalue.CachingFile;
import sp.it.pl.util.access.fieldvalue.FileField;
import sp.it.pl.util.async.future.Fut;
import sp.it.pl.util.conf.Config.VarList;
import sp.it.pl.util.conf.Config.VarList.Elements;
import sp.it.pl.util.conf.EditMode;
import sp.it.pl.util.conf.IsConfig;
import sp.it.pl.util.file.FileFilterValue;
import sp.it.pl.util.file.FileFilters;
import sp.it.pl.util.file.FileFlatter;
import sp.it.pl.util.file.FileSort;
import sp.it.pl.util.file.FileType;
import sp.it.pl.util.graphics.Resolution;
import sp.it.pl.util.graphics.drag.DragUtil;
import sp.it.pl.util.graphics.drag.Placeholder;
import sp.it.pl.util.validation.Constraint;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.FOLDER_PLUS;
import static java.util.Comparator.nullsLast;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.toList;
import static javafx.scene.input.KeyCode.BACK_SPACE;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.util.Duration.millis;
import static sp.it.pl.gui.objects.grid.GridView.CellSize.NORMAL;
import static sp.it.pl.layout.widget.Widget.Group.OTHER;
import static sp.it.pl.main.AppBuildersKt.appTooltipForData;
import static sp.it.pl.main.AppProgressKt.showAppProgress;
import static sp.it.pl.main.AppUtil.APP;
import static sp.it.pl.util.Sort.ASCENDING;
import static sp.it.pl.util.async.AsyncKt.FX;
import static sp.it.pl.util.async.AsyncKt.newThreadPoolExecutor;
import static sp.it.pl.util.async.AsyncKt.oneThreadExecutor;
import static sp.it.pl.util.async.AsyncKt.onlyIfMatches;
import static sp.it.pl.util.async.AsyncKt.runFX;
import static sp.it.pl.util.async.AsyncKt.runLater;
import static sp.it.pl.util.async.AsyncKt.threadFactory;
import static sp.it.pl.util.async.future.Fut.fut;
import static sp.it.pl.util.file.FileSort.DIR_FIRST;
import static sp.it.pl.util.file.FileType.DIRECTORY;
import static sp.it.pl.util.file.Util.getCommonRoot;
import static sp.it.pl.util.functional.Util.by;
import static sp.it.pl.util.functional.Util.list;
import static sp.it.pl.util.functional.Util.max;
import static sp.it.pl.util.functional.UtilKt.consumer;
import static sp.it.pl.util.graphics.Util.setAnchor;
import static sp.it.pl.util.reactive.Util.attach1IfNonNull;
import static sp.it.pl.util.reactive.Util.maintain;
import static sp.it.pl.util.system.EnvironmentKt.chooseFile;
import static sp.it.pl.util.system.EnvironmentKt.edit;
import static sp.it.pl.util.system.EnvironmentKt.open;

@Widget.Info(
        author = "Martin Polakovic",
        name = "Dir Viewer",
        description = "Displays directory hierarchy and files as thumbnails in a "
                + "vertically scrollable grid. Intended as simple library",
        version = "0.5",
        year = "2015",
        group = OTHER
)
@LegacyController
public class DirViewer extends SimpleController {

    private static final double CELL_TEXT_HEIGHT = 20;

	@Constraint.FileType(Constraint.FileActor.DIRECTORY)
    @IsConfig(name = "Location", info = "Root directories of the content.")
    final VarList<File> files = new VarList<>(File.class, Elements.NOT_NULL);
	@IsConfig(name = "Location joiner", info = "Merges location files into a virtual view.")
    final V<FileFlatter> fileFlatter = new V<>(FileFlatter.TOP_LVL, ff -> revisitCurrent());

    @IsConfig(name = "Thumbnail size", info = "Size of the thumbnail.")
    final V<CellSize> cellSize = new V<>(NORMAL, v -> applyCellSize());
    @IsConfig(name = "Thumbnail size ratio", info = "Size ratio of the thumbnail.")
    final V<Resolution> cellSizeRatio = new V<>(Resolution.R_1x1, v -> applyCellSize());
    @IsConfig(name = "Thumbnail fit image from", info = "Determines whether image will be fit from inside or outside.")
    final V<FitFrom> fitFrom = new V<>(FitFrom.OUTSIDE);

    private final GridView<Item, File> grid = new GridView<>(File.class, v -> v.val, cellSize.get().width, cellSize.get().width/cellSizeRatio.get().ratio +CELL_TEXT_HEIGHT, 5, 5);
    private final ExecutorService executorIO = oneThreadExecutor();
    private final ExecutorService executorThumbs = newThreadPoolExecutor(8, 1,MINUTES, threadFactory("dirView-img-thumb", true));
    private final ExecutorService executorImage = newThreadPoolExecutor(8, 1, MINUTES, threadFactory("dirView-img-full", true));
    private final Loader imageLoader = new Loader(executorThumbs, executorImage);
    private boolean initialized = false;
    private AtomicLong visitId = new AtomicLong(0);
    private final Placeholder placeholder = new Placeholder(
    	FOLDER_PLUS, "Click to explore directory",
		() -> chooseFile("Choose directory", DIRECTORY, APP.DIR_HOME, getOwnerWidget().getWindowOrActive().map(Window::getStage).orElse(null))
				.ifOk(files.list::setAll)
    );
    @IsConfig(name = "File filter", info = "Shows only directories and files passing the filter.")
    final FileFilterValue filter = FileFilters.toEnumerableValue(v -> revisitCurrent());
    @IsConfig(name = "Sort", info = "Sorting effect.")
    final V<Sort> sort = new V<>(ASCENDING, v -> applySort());
    @IsConfig(name = "Sort first", info = "Group directories and files - files first, last or no separation.")
    final V<FileSort> sort_file = new V<>(DIR_FIRST, v -> applySort());
    @IsConfig(name = "Sort seconds", info = "Sorting criteria.")
    final VarEnum<FileField<?>> sortBy = new VarEnum<>(FileField.NAME, () -> FileField.FIELDS, v -> applySort());

	@Constraint.FileType(Constraint.FileActor.DIRECTORY)
    @IsConfig(name = "Last visited", info = "Last visited item.", editable = EditMode.APP)
    File lastVisited = null;
    Item item = null;   // item, children of which are displayed

    public DirViewer(Widget<?> widget) {
        super(widget);

        files.onListInvalid(list -> revisitTop());
        files.onListInvalid(list -> placeholder.show(this, list.isEmpty()));
        grid.search.field = FileField.PATH;
	    grid.primaryFilterField = FileField.NAME_FULL;
        grid.setCellFactory(grid -> new Cell());
        setAnchor(this, grid, 0d);
        placeholder.show(this, files.list.isEmpty());

        inputs.create("Root directory", File.class, null, dir -> {
            if (dir != null && dir.isDirectory() && dir.exists())
                files.setItems(dir);
        });

        grid.setOnKeyPressed(e -> {
            if (e.getCode() == ENTER) {
                Item si = grid.selectedItem.get();
                if (si != null) doubleClickItem(si, e.isShiftDown());
            }
            if (e.getCode() == BACK_SPACE)
                visitUp();
        });
        grid.setOnMouseClicked(e -> {
            if (e.getButton() == SECONDARY)
                visitUp();
        });
        grid.addEventFilter(ScrollEvent.SCROLL, e -> {
            if (e.isShortcutDown()) {
                e.consume();
                boolean isInc = e.getDeltaY()<0 || e.getDeltaX()>0;
                boolean useFreeStyle = e.isShiftDown();
                if (useFreeStyle) {
                    boolean preserveAspectRatio  = true;
                    double scaleUnit = 1.2, w = grid.getCellWidth(), h = grid.getCellHeight();
                    double nw = max(50.0, Math.rint(isInc ? w*scaleUnit : w/scaleUnit));
                    double nh = max(50.0, Math.rint(isInc ? h*scaleUnit : h/scaleUnit));
                    if (preserveAspectRatio) nh = nw/cellSizeRatio.get().ratio;
                    applyCellSize(nw, nh);
                } else {
                    if (isInc) cellSize.setPreviousNapplyValue();
                    else cellSize.setNextNapplyValue();
                }
            }
        });

		// drag & drop
		DragUtil.installDrag(
			this, FOLDER_PLUS, "Explore directory",
			DragUtil::hasFiles,
			e -> files.list.setAll(
				DragUtil.getFiles((e)).stream().allMatch(File::isDirectory)
					? DragUtil.getFiles((e))
					: list(getCommonRoot(DragUtil.getFiles((e))))
			)
		);
    }

    @Override
    public void refresh() {
        initialized = true;
        applyCellSizeNoRefresh();
        // temporary bug fix, (we use progress indicator of the window this widget is loaded
        // in, but when this refresh() method is called its just during loading and window is not yet
        // available, so we delay wit runLater
        runLater(() -> revisitCurrent());
    }

    void visitUp() {
        // We visit parent, a "back" operation.
        // Because the hierarchy we traverse is virtual (and may not have non-single parent), we may not go higher
        if (item != null && item.parent != null) {
            Item toDispose = item;
            visit(item.parent);
            toDispose.disposeChildren();
        }
    }

    private void visit(Item dir) {
        if (!initialized) return;
        if (item != null) item.lastScrollPosition = grid.implGetSkin().getPosition();
        if (item == dir) return;
        if (item != null && item.isHChildOf(dir)) item.disposeChildren();
        visitId.incrementAndGet();

        item = dir;
        lastVisited = dir.val;
        showAppProgress(
            fut(item)
                .then(executorIO, it -> it.children().stream().sorted(buildSortComparator()).collect(toList()))
                .useBy(FX, cells -> {
                    grid.getItemsRaw().setAll(cells);
                    if (item.lastScrollPosition>= 0)
                        grid.implGetSkin().setPosition(item.lastScrollPosition);

                    grid.requestFocus();    // fixes focus problem
                    runFX(millis(500), grid::requestFocus);
                }),
            widget.custom_name.getValue() + ": Fetching view"
        );
    }

    @Override
    public void focus() {
        attach1IfNonNull(grid.skinProperty(), consumer(skin -> grid.implGetSkin().requestFocus()));
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

            // Build stack of files representing the visited branch
            Stack<File> path = new Stack<>(); // nested items we need to rebuild to get to last visited
            File f = lastVisited;
            while (f != null && topItem.children().stream().map(c -> c.val).noneMatch(f::equals)) {
                path.push(f);
                f = f.getParentFile();
            }
            File tmpF = f;
            boolean success = topItem.children().stream().map(c -> c.val).anyMatch(c -> c!=null && c.equals(tmpF));
            if (success) {
                path.push(f);
            }

            // Visit the branch
            if (success) {
                executorIO.execute(() -> {
                    Item item = topItem;
                    while (!path.isEmpty()) {
                        File tmp = path.pop();
                        item = item==null ? null : item.children().stream().filter(child -> tmp.equals(child.val))
                            .findAny().orElse(null);
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

    private void doubleClickItem(Item i, boolean edit) {
        if (i.valType== DIRECTORY) DirViewer.this.visit(i);
        else {
            if (edit) edit(i.val);
            else open(i.val);
        }
    }

    void applyCellSizeNoRefresh() {
        grid.setCellSize(cellSize.get().width, cellSize.get().width/cellSizeRatio.get().ratio + CELL_TEXT_HEIGHT);
    }

    void applyCellSize() {
        applyCellSize(cellSize.get().width, cellSize.get().width/cellSizeRatio.get().ratio);
    }

    void applyCellSize(double width, double height) {
        grid.setCellSize(width, height + CELL_TEXT_HEIGHT);
        refresh();
    }

    /**
     * Resorts grid's items according to current sort criteria.
     */
    private void applySort() {
        grid.getItemsRaw().sort(buildSortComparator());
    }

    private Comparator<Item> buildSortComparator() {
        FileField<?> field = sortBy.get();          // pre-compute, do not compute in comparator
        Sort sortHetero = sort_file.get().sort;     // sorts Files to files and directories
        Sort sortHomo = sort.get();                 // sorts each group separately
        Comparator<Item> cmpHetero = sortHetero.of(by(i -> i.valType));
        Comparator<Item> cmpHomo = by(i -> i.val, field.comparator(c -> nullsLast(sortHomo.of(c))));
        return cmpHetero
            .thenComparing(cmpHomo)
            .thenComparing(by(i -> i.val.getPath()));
    }

    private boolean applyFilter(File f) {
        return !f.isHidden() && f.canRead() && filter.getValueAsFilter().apply(f);
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
        protected double computeCellTextHeight() {
            return CELL_TEXT_HEIGHT;
        }

        @Override
        protected void computeGraphics() {
            super.computeGraphics();
            maintain(fitFrom, thumb.fitFrom);
            Tooltip.install(root, appTooltipForData(() -> thumb.getRepresentant()));
        }

        @Override
        protected void onAction(Item i, boolean edit) {
            doubleClickItem(i, edit);
        }

        @Override
        protected Runnable computeTask(Runnable r) {
            return onlyIfMatches(r, visitId);
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
            return DirViewer.this.applyFilter(f);
        }
    }

    private class TopItem extends FItem {

        public TopItem() {
            super(null, null, null);
        }

        @Override
        protected Stream<File> children_files() {
            return fileFlatter.get().flatten.invoke(files.list).map(CachingFile::new);
        }

        @Override
        protected File getCoverFile() {
            boolean allChildrenShareParent = files.list.size()==1;
            if (allChildrenShareParent) {
                File dir = children.stream().findFirst().get().val.getParentFile();
                if (dir!=null) {
                    return getImageT(dir, "cover");
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
    }

}