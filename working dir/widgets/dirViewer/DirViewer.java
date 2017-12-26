package dirViewer;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ScrollEvent;
import sp.it.pl.gui.objects.grid.GridFileThumbCell;
import sp.it.pl.gui.objects.grid.GridFileThumbCell.AnimateOn;
import sp.it.pl.gui.objects.grid.GridFileThumbCell.Loader;
import sp.it.pl.gui.objects.grid.GridView;
import sp.it.pl.gui.objects.grid.GridView.CellSize;
import sp.it.pl.gui.objects.hierarchy.Item;
import sp.it.pl.gui.objects.image.Thumbnail.FitFrom;
import sp.it.pl.layout.widget.Widget;
import sp.it.pl.layout.widget.controller.ClassController;
import sp.it.pl.util.Sort;
import sp.it.pl.util.access.V;
import sp.it.pl.util.access.VarEnum;
import sp.it.pl.util.access.fieldvalue.FileField;
import sp.it.pl.util.access.ref.LazyR;
import sp.it.pl.util.async.future.Fut;
import sp.it.pl.util.conf.Config.VarList;
import sp.it.pl.util.conf.Config.VarList.Elements;
import sp.it.pl.util.conf.IsConfig;
import sp.it.pl.util.conf.IsConfig.EditMode;
import sp.it.pl.util.file.FileFlatter;
import sp.it.pl.util.file.FileSort;
import sp.it.pl.util.file.mimetype.MimeTypes;
import sp.it.pl.util.functional.Functors.PƑ0;
import sp.it.pl.util.graphics.Resolution;
import sp.it.pl.util.graphics.drag.DragUtil;
import sp.it.pl.util.graphics.drag.Placeholder;
import sp.it.pl.util.validation.Constraint;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.FOLDER_PLUS;
import static java.util.Comparator.nullsLast;
import static java.util.concurrent.TimeUnit.MINUTES;
import static javafx.scene.input.KeyCode.BACK_SPACE;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.util.Duration.millis;
import static sp.it.pl.gui.objects.grid.GridFileThumbCell.AnimateOn.IMAGE_CHANGE_1ST_TIME;
import static sp.it.pl.gui.objects.grid.GridView.CellSize.NORMAL;
import static sp.it.pl.layout.widget.Widget.Group.OTHER;
import static sp.it.pl.main.AppBuildersKt.appTooltipForData;
import static sp.it.pl.main.AppUtil.APP;
import static sp.it.pl.util.Sort.ASCENDING;
import static sp.it.pl.util.Util.capitalize;
import static sp.it.pl.util.async.AsyncKt.FX;
import static sp.it.pl.util.async.AsyncKt.newSingleDaemonThreadExecutor;
import static sp.it.pl.util.async.AsyncKt.newThreadPoolExecutor;
import static sp.it.pl.util.async.AsyncKt.onlyIfMatches;
import static sp.it.pl.util.async.AsyncKt.runAfter;
import static sp.it.pl.util.async.AsyncKt.runFX;
import static sp.it.pl.util.async.AsyncKt.runLater;
import static sp.it.pl.util.async.AsyncKt.threadFactory;
import static sp.it.pl.util.file.FileSort.DIR_FIRST;
import static sp.it.pl.util.file.FileType.DIRECTORY;
import static sp.it.pl.util.file.Util.getCommonRoot;
import static sp.it.pl.util.file.Util.getSuffix;
import static sp.it.pl.util.file.mimetype.MimeTypesKt.mimeType;
import static sp.it.pl.util.functional.Util.by;
import static sp.it.pl.util.functional.Util.list;
import static sp.it.pl.util.functional.Util.map;
import static sp.it.pl.util.functional.Util.max;
import static sp.it.pl.util.functional.Util.stream;
import static sp.it.pl.util.graphics.Util.setAnchor;
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
public class DirViewer extends ClassController {

    private static final double CELL_TEXT_HEIGHT = 20;

	@Constraint.FileType(Constraint.FileActor.DIRECTORY)
    @IsConfig(name = "Location", info = "Root directories of the content.")
    final VarList<File> files = new VarList<>(File.class, Elements.NOT_NULL);
	@IsConfig(name = "Location joiner", info = "Merges location files into a virtual view.")
    final V<FileFlatter> fileFlatter = new V<>(FileFlatter.TOP_LVL, ff -> revisitCurrent());

    @IsConfig(name = "Thumbnail size", info = "Size of the thumbnail.")
    final V<CellSize> cellSize = new V<>(NORMAL, this::applyCellSize);
    @IsConfig(name = "Thumbnail size ratio", info = "Size ratio of the thumbnail.")
    final V<Resolution> cellSizeRatio = new V<>(Resolution.R_1x1, this::applyCellSize);
    @IsConfig(name = "Thumbnail fit image from", info = "Determines whether image will be fit from inside or outside.")
    final V<FitFrom> fitFrom = new V<>(FitFrom.OUTSIDE);
    @IsConfig(name = "Thumbnail animate on", info = "Determines when the thumbnail image transition is played.")
    final V<AnimateOn> animateThumbOn = new V<>(IMAGE_CHANGE_1ST_TIME);

    private final GridView<Item, File> grid = new GridView<>(File.class, v -> v.val, cellSize.get().width, cellSize.get().width/cellSizeRatio.get().ratio +CELL_TEXT_HEIGHT, 5, 5);
    private final ExecutorService executorIO = newSingleDaemonThreadExecutor();
    private final ExecutorService executorThumbs = newThreadPoolExecutor(8, 1,MINUTES, threadFactory("dirView-img-thumb", true));
    private final ExecutorService executorImage = newThreadPoolExecutor(8, 1, MINUTES, threadFactory("dirView-img-full", true));
    private final Loader imageLoader = new Loader(executorThumbs, executorImage);
    boolean initialized = false;
    private AtomicLong visitId = new AtomicLong(0);
    private final Placeholder placeholder = new Placeholder(
    	FOLDER_PLUS, "Click to explore directory",
		() -> chooseFile("Choose directory", DIRECTORY, APP.DIR_HOME, getWidget().getWindow().getStage())
				.ifOk(files.list::setAll)
    );
    private final LazyR<PƑ0<File, Boolean>> filterPredicate = new LazyR<>(this::buildFilter);


    @IsConfig(name = "File filter", info = "Shows only directories and files passing the filter.")
    final VarEnum<String> filter = new VarEnum<>("File - all", () -> map(filters, f -> f.name), v -> {
	    filterPredicate.set(buildFilter());
	    revisitCurrent();
    });
    @IsConfig(name = "Sort", info = "Sorting effect.")
    final V<Sort> sort = new V<>(ASCENDING, this::applySort);
    @IsConfig(name = "Sort file", info = "Group directories and files - files first, last or no separation.")
    final V<FileSort> sort_file = new V<>(DIR_FIRST, this::applySort);
    @IsConfig(name = "Sort by", info = "Sorting criteria.")
    final VarEnum<FileField<?>> sortBy = new VarEnum<>(FileField.NAME, () -> FileField.FIELDS, f -> applySort());

	@Constraint.FileType(Constraint.FileActor.DIRECTORY)
    @IsConfig(name = "Last visited", info = "Last visited item.", editable = EditMode.APP)
    File lastVisited = null;
    Item item = null;   // item, children of which are displayed

    public DirViewer() {
        files.onListInvalid(list -> revisitTop());
        files.onListInvalid(list -> placeholder.show(this, list.isEmpty()));
        grid.search.field = FileField.PATH;
	    grid.primaryFilterField = FileField.NAME_FULL;
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
        Fut.fut(item)
                .map(executorIO, Item::children)
                .use(executorIO, cells -> cells.sort(buildSortComparator()))
                .use(FX, cells -> {
                    grid.getItemsRaw().setAll(cells);
                    if (item.lastScrollPosition>= 0)
                        grid.implGetSkin().setPosition(item.lastScrollPosition);

                    grid.requestFocus();    // fixes focus problem
                    runAfter(millis(500), grid::requestFocus);
                })
                .showProgress(getWidget().getWindow().taskAdd());
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
        return !f.isHidden() && f.canRead() && filterPredicate.get().apply(f);
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

        public FItem(Item parent, File value, sp.it.pl.util.file.FileType type) {
            super(parent, value, type);
        }

        @Override
        protected Item createItem(Item parent, File value, sp.it.pl.util.file.FileType type) {
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
            return fileFlatter.get().flatten.invoke(files.list);
        }

        @Override
        protected File getCoverFile() {
            return null;
        }
    }

    /**
     * Filter summary: because we can not yet serialize functions (see {@link sp.it.pl.util.functional.Functors} and
     * {@link sp.it.pl.util.parsing.Converter}) in  a way that stores (e.g. negation or function chaining), we do not use
     * predicates from function pool, but hardcoded filters, which we look up by name.
     * <p/>
     * We use String config field to save which filter we use. Of course, we give up filter chaining and other stuff...
     * For now, it is good enough.
     */
    private static final List<PƑ0<File,Boolean>> filters = list();

    static {
        filters.add(new PƑ0<>("File - all", File.class, Boolean.class, file -> true));
        filters.add(new PƑ0<>("File - none", File.class, Boolean.class, file -> false));
        filters.add(new PƑ0<>("File type - file", File.class, Boolean.class, File::isFile));
        filters.add(new PƑ0<>("File type - directory", File.class, Boolean.class, File::isDirectory));
        MimeTypes.INSTANCE.setOfGroups().forEach(group -> {
            filters.add(new PƑ0<>("Mime type group - is " + capitalize(group), File.class, Boolean.class, file -> group.equals(mimeType(file).getGroup())));
            filters.add(new PƑ0<>("Mime type group - no " + capitalize(group), File.class, Boolean.class, file -> !group.equals(mimeType(file).getGroup())));
        });
        MimeTypes.INSTANCE.setOfMimeTypes().forEach(mime -> {
            filters.add(new PƑ0<>("Mime type - is " + mime.getName(), File.class, Boolean.class, file -> mimeType(file)==mime));
            filters.add(new PƑ0<>("Mime type - no " + mime.getName(), File.class, Boolean.class, file -> mimeType(file)!=mime));
        });
        MimeTypes.INSTANCE.setOfExtensions().forEach(extension -> {
            filters.add(new PƑ0<>("Type - is " + extension, File.class, Boolean.class, file -> getSuffix(file).equalsIgnoreCase(extension)));
            filters.add(new PƑ0<>("Type - no " + extension, File.class, Boolean.class, file -> !getSuffix(file).equalsIgnoreCase(extension)));
        });
    }

    private PƑ0<File,Boolean> buildFilter() {
        String type = filter.getValue();
        return stream(filters)
                .filter(f -> type.equals(f.name))
                .findAny()
                .orElseGet(() -> stream(filters).filter(f -> "File - all".equals(f.name)).findAny().get());
    }
}