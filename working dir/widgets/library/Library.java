package library;

import audio.Item;
import audio.Player;
import audio.playlist.PlaylistManager;
import audio.tagging.Metadata;
import audio.tagging.MetadataGroup;
import audio.tagging.MetadataReader;
import gui.Gui;
import gui.infonode.InfoTask;
import gui.objects.contextmenu.TableContextMenuR;
import gui.objects.table.FilteredTable;
import gui.objects.table.ImprovedTable.PojoV;
import gui.objects.table.TableColumnInfo;
import gui.objects.tablerow.ImprovedTableRow;
import java.io.File;
import java.util.Collection;
import java.util.List;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.input.Dragboard;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Callback;
import layout.widget.Widget.Info;
import layout.widget.controller.FXMLController;
import layout.widget.controller.io.IsInput;
import layout.widget.controller.io.Output;
import layout.widget.feature.SongReader;
import main.App;
import util.access.Vo;
import util.access.fieldvalue.ColumnField;
import util.animation.Anim;
import util.animation.interpolator.ElasticInterpolator;
import util.async.executor.ExecuteN;
import util.async.future.Fut;
import util.conf.Config;
import util.conf.IsConfig;
import util.conf.IsConfig.EditMode;
import util.file.AudioFileFormat;
import util.file.AudioFileFormat.Use;
import util.file.FileType;
import util.graphics.drag.DragUtil;
import util.units.Dur;
import util.validation.Constraint;
import util.validation.Constraint.FileActor;
import static audio.tagging.Metadata.Field.RATING;
import static audio.tagging.Metadata.Field.TITLE;
import static gui.infonode.InfoTable.DEFAULT_TEXT_FACTORY;
import static javafx.scene.control.SelectionMode.MULTIPLE;
import static javafx.scene.control.TableView.UNCONSTRAINED_RESIZE_POLICY;
import static javafx.scene.input.KeyCode.DELETE;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.TransferMode.COPY;
import static javafx.util.Duration.seconds;
import static layout.widget.Widget.Group.LIBRARY;
import static main.App.APP;
import static main.AppBuildersKt.appProgressIndicator;
import static util.animation.Anim.Interpolators.reverse;
import static util.async.AsyncKt.FX;
import static util.async.AsyncKt.sleeping;
import static util.file.Util.getCommonRoot;
import static util.functional.Util.map;
import static util.graphics.Util.menuItem;
import static util.graphics.Util.setAnchors;
import static util.graphics.UtilKt.setScaleXY;
import static util.reactive.Util.maintain;
import static util.system.EnvironmentKt.chooseFile;
import static util.system.EnvironmentKt.chooseFiles;

@Info(
    author = "Martin Polakovic",
    name = "Library",
    description = "Provides access to database.",
    howto = "Available actions:\n" +
            "    Item left click : Selects item\n" +
            "    Item right click : Opens context menu\n" +
            "    Item double click : Plays item\n" +
            "    Type : search & filter\n" +
            "    Press ENTER : Plays item\n" +
            "    Press ESC : Clear selection & filter\n" +
            "    Scroll : Scroll table vertically\n" +
            "    Scroll + SHIFT : Scroll table horizontally\n" +
            "    Column drag : swap columns\n" +
            "    Column right click: show column menu\n" +
            "    Click column : Sort - ascending | descending | none\n" +
            "    Click column + SHIFT : Sorts by multiple columns\n" +
            "    Menu bar : Opens additional actions\n",
//    notes = "",
    version = "1",
    year = "2015",
    group = LIBRARY
)
public class Library extends FXMLController implements SongReader {

	private static final TableContextMenuR<MetadataGroup> contextMenu = new TableContextMenuR<>();

    private @FXML AnchorPane root;
	private final FilteredTable<Metadata> table = new FilteredTable<>(Metadata.class, Metadata.EMPTY.getMainField());
    private final InfoTask<Task<?>> taskInfo = new InfoTask<>(null, new Label(), appProgressIndicator());
	private final Anim hideInfo = new Anim(at-> setScaleXY(taskInfo.getProgress(),at*at))
		                              .dur(500).intpl(reverse(new ElasticInterpolator()));

	private final ExecuteN runOnce = new ExecuteN(1);
    private Output<Metadata> out_sel;

    @IsConfig(name = "Table orientation", info = "Orientation of the table.")
    public final Vo<NodeOrientation> orient = new Vo<>(Gui.table_orient);
    @IsConfig(name = "Zeropad numbers", info = "Adds 0s for number length consistency.")
    public final Vo<Boolean> zeropad = new Vo<>(Gui.table_zeropad);
    @IsConfig(name = "Search show original index", info = "Show unfiltered table item index when filter applied.")
    public final Vo<Boolean> orig_index = new Vo<>(Gui.table_orig_index);
    @IsConfig(name = "Show table header", info = "Show table header with columns.")
    public final Vo<Boolean> show_header = new Vo<>(Gui.table_show_header);
    @IsConfig(name = "Show table footer", info = "Show table controls at the bottom of the table. Displays menu bar and table items information.")
    public final Vo<Boolean> show_footer = new Vo<>(Gui.table_show_footer);
    @IsConfig(editable = EditMode.APP) @Constraint.FileType(FileActor.ANY)
    private File lastFile = null;
    @IsConfig(editable = EditMode.APP) @Constraint.FileType(FileActor.DIRECTORY)
    private File lastDir = null;

    @Override
    public void init() {
        out_sel = outputs.create(widget.id, "Selected", Metadata.class, null);
        d(Player.librarySelected.i.bind(out_sel));

        // add table to scene graph
        root.getChildren().add(table.getRoot());
        setAnchors(table.getRoot(),0d);

        // table properties
        table.setFixedCellSize(Gui.font.getValue().getSize() + 5);
        table.getSelectionModel().setSelectionMode(MULTIPLE);
        table.search.setColumn(TITLE);
        d(maintain(orient,table.nodeOrientationProperty()));
        d(maintain(zeropad,table.zeropadIndex));
        d(maintain(orig_index,table.showOriginalIndex));
        d(maintain(show_header,table.headerVisible));
        d(maintain(show_footer,table.footerVisible));

        // add progress indicator to bottom controls
        ((Pane)table.footerPane.getRight()).getChildren().addAll(taskInfo.getMessage(), taskInfo.getProgress());
        taskInfo.setVisible(false);

        // extend table items information
        table.items_info.textFactory = (all, list) -> {
            double Σms = list.stream().mapToDouble(Metadata::getLengthInMs).sum();
            return DEFAULT_TEXT_FACTORY.apply(all, list) + " - " + new Dur(Σms);
        };
        // add more menu items
        table.menuAdd.getItems().addAll(
            menuItem("Add files", e -> addFiles()),
            menuItem("Add directory", e -> addDirectory())
        );
        table.menuRemove.getItems().addAll(
            menuItem("Remove selected from library", e -> APP.db.removeItems(table.getSelectedItems())),
            menuItem("Remove all from library", e -> APP.db.removeItems(table.getItems())),
            menuItem("Remove invalid items", e -> removeInvalid())
        );

        // set up table columns
        table.setColumnFactory(f -> {
            TableColumn<Metadata,Object> c = new TableColumn<>(f.toString());
            c.setCellValueFactory(cf -> cf.getValue()==null ? null : new PojoV<>(f.getOf(cf.getValue())));
            c.setCellFactory(f==(Metadata.Field) RATING
                ? (Callback) APP.ratingCell.getValue()
                : column -> table.buildDefaultCell(f)
            );
            return c;
        });

        // let resizing as it is
        table.setColumnResizePolicy(resize -> {
            boolean b = UNCONSTRAINED_RESIZE_POLICY.call(resize);
            // resize index column
			table.getColumn(ColumnField.INDEX).ifPresent(i -> i.setPrefWidth(table.computeIndexColumnWidth()));
            return b;
        });

        // maintain rating column cell style
        APP.ratingCell.addListener((o,ov,nv) -> table.getColumn(RATING).ifPresent(c->c.setCellFactory((Callback)nv)));

        table.getDefaultColumnInfo();

        // row behavior
        table.setRowFactory(tbl -> new ImprovedTableRow<Metadata>()
                .onLeftDoubleClick((r,e) ->
                    PlaylistManager.use(pl->pl.setNplayFrom(table.getItems(), r.getIndex()))
                )
                .onRightSingleClick((r,e) -> {
                    // prep selection for context menu
                    if (!r.isSelected())
                        tbl.getSelectionModel().clearAndSelect(r.getIndex());

                    contextMenu.show(MetadataGroup.groupOfUnrelated(table.getSelectedItemsCopy()), table, e);
                })
                // additional css style classes
                .styleRuleAdd("played", m -> Player.playingItem.get().same(m)) // don't use method reference!
        );
        // maintain playing item css by refreshing column
        d(Player.playingItem.onUpdate(o -> table.updateStyleRules()));

        // maintain outputs
        table.getSelectionModel().selectedItemProperty().addListener((o,ov,nv) -> out_sel.setValue(nv));

        // key actions
        table.setOnKeyPressed(e -> {
			// play selected
            if (e.getCode() == ENTER) {
                if (!table.getSelectionModel().isEmpty()) {
                    PlaylistManager.use(pl -> pl.setNplayFrom(table.getItems(), table.getSelectionModel().getSelectedIndex()));
                }
            }
			// delete selected
            if (e.getCode() == DELETE) {
				APP.db.removeItems(table.getSelectedItems());
			}
        });

        // drag&drop from
        table.setOnDragDetected(e -> {
            if (e.getButton() == PRIMARY && !table.getSelectedItems().isEmpty()
                    && table.isRowFull(table.getRowS(e.getSceneX(), e.getSceneY()))) {
                Dragboard db = table.startDragAndDrop(COPY);
                DragUtil.setItemList(table.getSelectedItemsCopy(),db,true);
            }
            e.consume();
        });

        // prevent volume change
        table.setOnScroll(Event::consume);

        // update library comparator
        maintain(table.itemsComparator, APP.db.getLibraryComparator());
    }

    @Override
    public void refresh() {
        runOnce.execute(() -> {
            String c = getWidget().properties.getS("columns");
            table.setColumnState(c==null ? table.getDefaultColumnInfo() : TableColumnInfo.fromString(c));
        });

        getFields().stream().filter(c -> !c.getName().equals("Library level") && !c.getName().equals("columnInfo")).forEach(Config::applyValue);
        table.getSelectionModel().clearSelection();
    }

	@Override
	public Collection<Config<Object>> getFields() {
		// serialize column state when requested
		getWidget().properties.put("columns", table.getColumnState().toString());
		return super.getFields();
	}

	/**
	 * Converts items to Metadata using {@link Item#toMeta()} (using no I/O)
	 * and displays them in the table.
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public void read(List<? extends Item> items) {
		if (items==null) return;
		table.setItemsRaw(map(items,Item::toMeta));
	}

    @IsInput("To display")
    public void setItems(List<? extends Metadata> items) {
        if (items==null) return;
        table.setItemsRaw(items);
    }

    @FXML private void addDirectory() {
	    Window w = root.getScene().getWindow();
	    FileChooser.ExtensionFilter ef = AudioFileFormat.filter(Use.APP);
	    chooseFile("Add folder to library", FileType.DIRECTORY, lastDir, w, ef)
			.ifOk(file -> {
				App.APP.actionPane.show(file);
				lastDir = file.getParentFile();
			});
    }

    @FXML private void addFiles() {
	    Window w = root.getScene().getWindow();
	    FileChooser.ExtensionFilter ef = AudioFileFormat.filter(Use.APP);
		chooseFiles("Add files to library", lastFile, w, ef)
			.ifOk(files -> {
				App.APP.actionPane.show(files);
				lastFile = getCommonRoot(files);
			});
    }

    private void removeInvalid() {
    	Task<Void> t = MetadataReader.buildRemoveMissingFromLibTask();
	    Fut.fut(t)
			.use(FX, taskInfo::showNbind)
			.use(Task::run)
			.then(sleeping(seconds(5)))
			.then(FX, () -> hideInfo.playOpenDo(taskInfo::hideNunbind))
	        .printExceptions();
    }

}