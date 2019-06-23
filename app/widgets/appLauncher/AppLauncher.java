package appLauncher;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import kotlin.sequences.Sequence;
import sp.it.pl.gui.objects.grid.GridFileThumbCell;
import sp.it.pl.gui.objects.grid.GridFileThumbCell.Loader;
import sp.it.pl.gui.objects.grid.GridView;
import sp.it.pl.gui.objects.grid.GridView.CellSize;
import sp.it.pl.gui.objects.hierarchy.Item;
import sp.it.pl.gui.objects.placeholder.Placeholder;
import sp.it.pl.layout.widget.ExperimentalController;
import sp.it.pl.layout.widget.Widget;
import sp.it.pl.layout.widget.controller.LegacyController;
import sp.it.pl.layout.widget.controller.SimpleController;
import sp.it.pl.main.Widgets;
import sp.it.util.Sort;
import sp.it.util.access.V;
import sp.it.util.access.VarEnum;
import sp.it.util.access.fieldvalue.FileField;
import sp.it.util.async.executor.FxTimer;
import sp.it.util.conf.Config.VarList;
import sp.it.util.conf.Config.VarList.Elements;
import sp.it.util.conf.IsConfig;
import sp.it.util.dev.Dependency;
import sp.it.util.file.FileSort;
import sp.it.util.file.FileType;
import sp.it.util.ui.Resolution;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.FOLDER_PLUS;
import static java.util.stream.Collectors.toList;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.util.Duration.millis;
import static kotlin.streams.jdk8.StreamsKt.asSequence;
import static sp.it.pl.gui.objects.grid.GridView.CellSize.NORMAL;
import static sp.it.pl.gui.objects.grid.GridView.SelectionOn.KEY_PRESS;
import static sp.it.pl.gui.objects.grid.GridView.SelectionOn.MOUSE_CLICK;
import static sp.it.pl.gui.objects.grid.GridView.SelectionOn.MOUSE_HOVER;
import static sp.it.pl.layout.widget.Widget.Group.OTHER;
import static sp.it.pl.main.AppDragKt.installDrag;
import static sp.it.pl.main.AppExtensionsKt.scaleEM;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.util.Sort.ASCENDING;
import static sp.it.util.async.AsyncKt.FX;
import static sp.it.util.async.AsyncKt.oneTPExecutor;
import static sp.it.util.async.AsyncKt.runFX;
import static sp.it.util.async.AsyncKt.runIO;
import static sp.it.util.async.executor.FxTimer.fxTimer;
import static sp.it.util.file.FileSort.DIR_FIRST;
import static sp.it.util.file.FileType.FILE;
import static sp.it.util.functional.Util.by;
import static sp.it.util.functional.Util.list;
import static sp.it.util.functional.UtilKt.consumer;
import static sp.it.util.functional.UtilKt.runnable;
import static sp.it.util.math.UtilKt.max;
import static sp.it.util.reactive.UtilKt.attach1IfNonNull;
import static sp.it.util.reactive.UtilKt.sync1IfInScene;
import static sp.it.util.system.EnvironmentKt.chooseFile;
import static sp.it.util.system.EnvironmentKt.open;

@Widget.Info(
    author = "Martin Polakovic",
    name = Widgets.APP_LAUNCHER,
    description = "Launches programs",
    version = "0.8.0",
    year = "2016",
    group = OTHER
)
@ExperimentalController(reason = "DirView widget could be improved to be fulfill this widget's purpose. Also needs better UX.")
@LegacyController
public class AppLauncher extends SimpleController {

    private static final double CELL_TEXT_HEIGHT = 40;

    @IsConfig(name = "Location", info = "Add program")
    final VarList<File> files = new VarList<>(File.class, Elements.NOT_NULL);
    List<File> filesMaterialized = new ArrayList<>(files.list);
    @IsConfig(name = "Thumbnail size", info = "Size of the thumbnail.")
    final V<CellSize> cellSize = new V<>(NORMAL).initAttachC(v -> applyCellSize());
    @IsConfig(name = "Thumbnail size ratio", info = "Size ratio of the thumbnail.")
    final V<Resolution> cellSizeRatio = new V<>(Resolution.R_4x5).initAttachC(v -> applyCellSize());

    private final GridView<Item, File> grid = new GridView<>(File.class, v -> v.value, cellSize.get().width,cellSize.get().width*cellSizeRatio.get().ratio +CELL_TEXT_HEIGHT,5,5);
    private final Loader imageLoader = new Loader(oneTPExecutor());
    boolean initialized = false;
    private volatile boolean isResizing = false;
	private final AtomicLong visitId = new AtomicLong(0);
    private final Placeholder placeholder = new Placeholder(
        FOLDER_PLUS, "Click to add launcher or drag & drop a file",
        runnable(() ->
            chooseFile("Choose program or file", FILE, APP.DIR_HOME, root.getScene().getWindow())
				.ifOkUse(files.list::setAll)
        )
    );

    @IsConfig(name = "Sort", info = "Sorting effect.")
    final V<Sort> sort = new V<>(ASCENDING).initAttachC(v -> applySort());
    @IsConfig(name = "Sort file", info = "Group directories and files - files first, last or no separation.")
    final V<FileSort> sort_file = new V<>(DIR_FIRST).initAttachC(v -> applySort());
    @IsConfig(name = "Sort by", info = "Sorting criteria.")
    final V<FileField<?>> sortBy = new VarEnum<>(FileField.NAME, () -> FileField.FIELDS).initAttachC(v -> applySort());
    @Dependency("name: used by reflection")
    @IsConfig(name = "Close on launch", info = "Close this widget when it launches a program.")
    final V<Boolean> closeOnLaunch = new V<>(false);
    @Dependency("name: used by reflection")
    @IsConfig(name = "Close on right click", info = "Close this widget when right click is detected.")
    final V<Boolean> closeOnRightClick = new V<>(false);

    public AppLauncher(Widget widget) {
        super(widget);
        root.setPrefSize(scaleEM(1000), scaleEM(700));

        files.onListInvalid(list -> filesMaterialized = new ArrayList<>(files.list));
        files.onListInvalid(list -> visit());
        files.onListInvalid(list -> placeholder.show(root, list.isEmpty()));
        grid.search.field = FileField.PATH;
        grid.primaryFilterField = FileField.NAME_FULL;
        grid.selectOn.addAll(list(KEY_PRESS, MOUSE_CLICK, MOUSE_HOVER));
        grid.setCellFactory(grid -> new Cell());
        root.getChildren().add(grid);
        placeholder.showFor(root);

        // delay cell loading when content is being resized (increases resize performance)
        double delay = 200; // ms
        FxTimer resizeTimer = fxTimer(millis(delay), 1, runnable(() -> isResizing = false));
        grid.widthProperty().addListener((o,ov,nv) -> isResizing = true);
        grid.heightProperty().addListener((o,ov,nv) -> isResizing = true);
        grid.widthProperty().addListener((o,ov,nv) -> resizeTimer.start(millis(300)));
        grid.heightProperty().addListener((o,ov,nv) -> resizeTimer.start(millis(300)));
        grid.setOnKeyPressed(e -> {
            if (e.getCode()==ENTER) {
                Item si = grid.selectedItem.get();
                if (si!=null) doubleClickItem(si);
            }
        });
        grid.setOnMouseClicked(e -> {
            if (e.getButton()==SECONDARY && closeOnRightClick.get())
                widget.uiTemp.dispose();  // TODO: this is for overlayPane interop, move out of here and use hide() instead of dispose()
        });

        installDrag(
            root, FontAwesomeIcon.PLUS_SQUARE_ALT, "Add launcher",
            e -> e.getDragboard().hasFiles(),
            consumer(e -> files.list.addAll(e.getDragboard().getFiles()))
        );

        sync1IfInScene(root, runnable(() -> {
            initialized = true;
            applyCellSize();
            visit();
        }));

        onClose.plusAssign(runnable(() -> imageLoader.shutdown()));
    }

    @Override
    public void focus() {
        attach1IfNonNull(grid.skinProperty(), consumer(skin -> grid.implGetSkin().requestFocus()));
    }

    private void visit() {
        if (!initialized) return;
        Item item = new TopItem();
	    visitId.incrementAndGet();
        runIO(() ->
            item.children().stream().sorted(buildSortComparator()).collect(toList())
        ).useBy(FX, cells -> {
            grid.getItemsRaw().setAll(cells);

            grid.implGetSkin().setPosition(max(item.lastScrollPosition, 0.0));
            grid.requestFocus();    // fixes focus problem
        });
    }

    private void doubleClickItem(Item i) {
        if (closeOnLaunch.get()) {
            widget.uiTemp.dispose();
            runFX(millis(250), () -> open(i.value));
        } else {
            open(i.value);
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
        Sort sortHetero = sort_file.get().getSort(), // sorts Files to files and directories
             sortHomo = sort.get(); // sorts each group separately
        FileField<?> field = sortBy.get(); // pre-compute once for consistency and performance
        Comparator<Item> cmpHetero = sortHetero.of(by(i -> i.valType)),
                         cmpHomo = sortHomo.of(by(i -> i.value, field.comparator()));
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
        protected double computeCellTextHeight() {
            return CELL_TEXT_HEIGHT;
        }

        @Override
        protected void onAction(Item i, boolean edit) {
            doubleClickItem(i);
        }
    }

    private static class FItem extends Item {

        public FItem(Item parent, File value, sp.it.util.file.FileType type) {
            super(parent, value, type);
        }

        @Override
        protected FItem createItem(Item parent, File value, sp.it.util.file.FileType type) {
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
	    protected Sequence<File> childrenFiles() {
		    return asSequence(filesMaterialized.stream().distinct());
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