package dirViewer;

import gui.objects.grid.GridFileThumbCell;
import gui.objects.grid.GridFileThumbCell.AnimateOn;
import gui.objects.grid.GridFileThumbCell.ImageLoader;
import gui.objects.grid.GridView;
import gui.objects.grid.GridView.CellSize;
import gui.objects.hierarchy.Item;
import gui.objects.image.Thumbnail.FitFrom;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ScrollEvent;
import layout.widget.Widget;
import layout.widget.controller.ClassController;
import util.LazyR;
import util.Sort;
import util.access.V;
import util.access.VarEnum;
import util.access.fieldvalue.FileField;
import util.async.future.Fut;
import util.conf.Config.VarList;
import util.conf.Config.VarList.Elements;
import util.conf.IsConfig;
import util.conf.IsConfig.EditMode;
import util.file.Environment;
import util.file.FileSort;
import util.functional.Functors.PƑ0;
import util.graphics.Resolution;
import util.graphics.drag.DragUtil;
import util.graphics.drag.Placeholder;
import util.validation.Constraint;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.FOLDER_PLUS;
import static gui.objects.grid.GridFileThumbCell.AnimateOn.IMAGE_CHANGE_1ST_TIME;
import static gui.objects.grid.GridView.CellSize.NORMAL;
import static java.util.Comparator.nullsLast;
import static javafx.scene.input.KeyCode.BACK_SPACE;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.util.Duration.millis;
import static layout.widget.Widget.Group.OTHER;
import static main.App.APP;
import static util.Sort.ASCENDING;
import static util.Util.capitalize;
import static util.async.Async.*;
import static util.file.FileSort.DIR_FIRST;
import static util.file.FileType.DIRECTORY;
import static util.file.Util.*;
import static util.functional.Util.*;
import static util.graphics.Util.layVertically;
import static util.graphics.Util.setAnchor;
import static util.reactive.Util.maintain;

@Widget.Info(
        author = "Martin Polakovic",
        name = "Dir Viewer",
        description = "Displays directory hierarchy and files as thumbnails in a "
                + "vertically scrollable grid. Intended as simple library",
//        howto = "",
//        notes = "",
        version = "0.5",
        year = "2015",
        group = OTHER
)
public class DirViewer extends ClassController {

    private static final double CELL_TEXT_HEIGHT = 20;

	@Constraint.FileType(Constraint.FileActor.DIRECTORY)
    @IsConfig(name = "Location", info = "Root directory the contents of to display "
            + "This is not a file system browser, and it is not possible to "
            + "visit parent of this directory.")
    final VarList<File> files = new VarList<>(File.class, Elements.NOT_NULL);
	@IsConfig(name = "Location merge", info = "Merges all locations into single union location")
	final V<Boolean> filesJoin = new V<>(true, f -> revisitCurrent());
	@IsConfig(name = "Location recursive", info = "Merges all location content recursively into single union location")
	final V<Boolean> filesAll = new V<>(false, f -> revisitCurrent());

    @IsConfig(name = "Thumbnail size", info = "Size of the thumbnail.")
    final V<CellSize> cellSize = new V<>(NORMAL, this::applyCellSize);
    @IsConfig(name = "Thumbnail size ratio", info = "Size ratio of the thumbnail.")
    final V<Resolution> cellSizeRatio = new V<>(Resolution.R_4x5, this::applyCellSize);
    @IsConfig(name = "Animate thumbs on", info = "Determines when the thumbnail image transition is played.")
    final V<AnimateOn> animateThumbOn = new V<>(IMAGE_CHANGE_1ST_TIME);
    @IsConfig(name = "Fit image from", info = "Determines whether image will be fit from inside or outside.")
    final V<FitFrom> fitFrom = new V<>(FitFrom.INSIDE);

    private final GridView<Item, File> grid = new GridView<>(File.class, v -> v.val, cellSize.get().width, cellSize.get().width/cellSizeRatio.get().ratio+CELL_TEXT_HEIGHT, 5, 5);
    private final ExecutorService executorIO = newSingleDaemonThreadExecutor();
    private final ExecutorService executorThumbs = newSingleDaemonThreadExecutor();
    private final ExecutorService executorImage = newSingleDaemonThreadExecutor(); // TODO: experiment with multiple threads
    private final ImageLoader imageLoader = new ImageLoader(executorThumbs, executorImage);
    boolean initialized = false;
    private AtomicLong visitId = new AtomicLong(0);
    private final Placeholder placeholder = new Placeholder(
    	FOLDER_PLUS, "Click to explore directory",
		() -> Environment.chooseFile("Choose directory", DIRECTORY, APP.DIR_HOME, getWidget().getWindow().getStage())
				.ifOk(files.list::setAll)
    );
    private final LazyR<PƑ0<File, Boolean>> filterPredicate = new LazyR<>(this::buildFilter);


    @IsConfig(name = "File filter", info = "Shows only directories and files passing the filter.")
    final VarEnum<String> filter = new VarEnum<String>("File - all", () -> map(filters, f -> f.name), v -> {
	    filterPredicate.set(buildFilter());
	    revisitCurrent();
    });
    @IsConfig(name = "Sort", info = "Sorting effect.")
    final V<Sort> sort = new V<>(ASCENDING, this::applySort);
    @IsConfig(name = "Sort file", info = "Group directories and files - files first, last or no separation.")
    final V<FileSort> sort_file = new V<>(DIR_FIRST, this::applySort);
    @IsConfig(name = "Sort by", info = "Sorting criteria.")
    final VarEnum<FileField<?>> sortBy = new VarEnum<FileField<?>>(FileField.NAME, () -> FileField.FIELDS, f -> applySort());

	@Constraint.FileType(Constraint.FileActor.DIRECTORY)
    @IsConfig(name = "Last visited", info = "Last visited item.", editable = EditMode.APP)
    File lastVisited = null;
    Item item = null;   // item, children of which are displayed

    public DirViewer() {
        files.onListInvalid(list -> revisitTop());
        files.onListInvalid(list -> placeholder.show(this, list.isEmpty()));
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
        applyCellSize();
        // temporary bug fix, (we use progress indicator of the window this widget is loaded
        // in, but when this refresh() method is called its just during loading and window is not yet
        // available, so we delay wit runLater
        runLater(this::revisitCurrent);
    }

    void visitUp() {
        // We visit parent, a "back" operation. Note we stop not at top of file hierarchy, but
        // the user source - collection of directories << TODO
        // if (item!=null) {
        //     if (item.parent!=null) visitDir(item.parent);
        //     else if (item instanceof TopItem && files.list.size()==1) visitDir(new Item(null,files.list.get(0)));
        // }
        if (item != null && item.parent != null) {
            Item toDispose = item;
            visit(item.parent);
            toDispose.disposeChildren(); // item.parent has become item
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
                    run(millis(500), grid::requestFocus);
                })
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

    private void doubleClickItem(Item i, boolean edit) {
        if (i.valType== DIRECTORY) DirViewer.this.visit(i);
        else {
            if (edit) Environment.edit(i.val);
            else     Environment.open(i.val);
        }
    }

    void applyCellSize() {
        grid.setCellSize(cellSize.get().width, cellSize.get().width/cellSizeRatio.get().ratio + CELL_TEXT_HEIGHT);
    }

    void applyCellSize(double width, double height) {
        grid.setCellSize(width, height + CELL_TEXT_HEIGHT);
    }

    /**
     * Resorts grid's items according to current sort criteria.
     */
    private void applySort() {
        grid.getItemsRaw().sort(buildSortComparator());
    }

    private Comparator<Item> buildSortComparator() {
        Sort sortHetero = sort_file.get().sort,     // sorts Files to files and directories
	         sortHomo = sort.get();                 // sorts each group separately
        FileField<?> field = sortBy.get();          // pre-compute, do not compute in comparator
        Comparator<Item> cmpHetero = sortHetero.cmp(by(i -> i.valType)),
                         cmpHomo = by(i -> i.val, field.comparator(c -> nullsLast(sortHomo.cmp(c))));
        return cmpHetero.thenComparing(cmpHomo);
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

            String KEY_TOOLTIP_SHOWN = "";
            Label nameL = new Label();
            Label sizeL = new Label();
            Label resL = new Label();
            Tooltip t = new Tooltip();
            Tooltip.install(root, t);
            t.setOnShowing(e -> {
                if (!root.getProperties().containsKey(KEY_TOOLTIP_SHOWN));  // TODO: WTF
                    t.setGraphic(layVertically(5, Pos.CENTER_LEFT, nameL, sizeL, resL));
                nameL.setText("Name: " + Optional.ofNullable(thumb.getFile()).map(File::getName).orElse("unknown"));
                resL.setText("Resolution: " + Optional.ofNullable(thumb.getImage()).map(i -> i.getWidth() + "x" + i.getHeight()).orElse("unknown"));
                sizeL.setText("Size: " + Optional.ofNullable(thumb.getFile()).map(f -> FileField.SIZE.getOf(f).toString()).orElse("unknown"));
            });
        }

        @Override
        protected void onAction(Item i, boolean edit) {
            doubleClickItem(i, edit);
        }

        @Override
        protected Runnable computeTask(Runnable r) {
            final long id = visitId.get();
            return () -> {
                if (id == visitId.get())
                    r.run();
            };
        }
    }

    private class FItem extends Item {

        public FItem(Item parent, File value, util.file.FileType type) {
            super(parent, value, type);
        }

        @Override
        protected Item createItem(Item parent, File value, util.file.FileType type) {
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
            return filesAll.get()
	                ? listFiles(files.list.stream())
		                  .flatMap(f -> {
		                  	try {
			                    return Files.walk(f.toPath(), Integer.MAX_VALUE)
						                  .map(Path::toFile)
						                  .filter(File::isFile);
		                    } catch (IOException e) {
		                    	return stream();
		                    }
		                  })
                          .distinct()
	                : filesJoin.get()
		                  ? listFiles(files.list.stream().distinct())
		                  : files.list.stream().distinct();
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
    private static final List<PƑ0<File,Boolean>> filters = list();

    static {
        filters.add(new PƑ0<>("File - all", File.class, Boolean.class, file -> true));
        filters.add(new PƑ0<>("File - none", File.class, Boolean.class, file -> false));
        filters.add(new PƑ0<>("File type - file", File.class, Boolean.class, File::isFile));
        filters.add(new PƑ0<>("File type - directory", File.class, Boolean.class, File::isDirectory));
        APP.mimeTypes.setOfGroups().forEach(group -> {
            filters.add(new PƑ0<>("Mime type group - is " + capitalize(group), File.class, Boolean.class, file -> group.equals(APP.mimeTypes.ofFile(file).getGroup())));
            filters.add(new PƑ0<>("Mime type group - no " + capitalize(group), File.class, Boolean.class, file -> !group.equals(APP.mimeTypes.ofFile(file).getGroup())));
        });
        APP.mimeTypes.setOfMimeTypes().forEach(mime -> {
            filters.add(new PƑ0<>("Mime type - is " + mime.getName(), File.class, Boolean.class, file -> APP.mimeTypes.ofFile(file)==mime));
            filters.add(new PƑ0<>("Mime type - no " + mime.getName(), File.class, Boolean.class, file -> APP.mimeTypes.ofFile(file)!=mime));
        });
        APP.mimeTypes.setOfExtensions().forEach(extension -> {
            filters.add(new PƑ0<>("Type - is " + extension, File.class, Boolean.class, file -> getSuffix(file).equalsIgnoreCase(extension)));
            filters.add(new PƑ0<>("Type - no " + extension, File.class, Boolean.class, file -> !getSuffix(file).equalsIgnoreCase(extension)));
        });
    }

    private PƑ0<File,Boolean> buildFilter() {
        String type = filter.getValue();
        return stream(filters)
                .findAny(f -> type.equals(f.name))
                .orElseGet(() -> stream(filters).findAny(f -> "File - all".equals(f.name)).get());
    }
}