package library;

import java.io.File;
import java.util.Collection;
import java.util.List;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.input.Dragboard;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Callback;
import sp.it.pl.audio.Item;
import sp.it.pl.audio.Player;
import sp.it.pl.audio.playlist.PlaylistManager;
import sp.it.pl.audio.tagging.Metadata;
import sp.it.pl.audio.tagging.MetadataGroup;
import sp.it.pl.audio.tagging.MetadataReader;
import sp.it.pl.gui.nodeinfo.TaskInfo;
import sp.it.pl.gui.objects.contextmenu.TableContextMenuR;
import sp.it.pl.gui.objects.table.FilteredTable;
import sp.it.pl.gui.objects.table.ImprovedTable.PojoV;
import sp.it.pl.gui.objects.table.TableColumnInfo;
import sp.it.pl.gui.objects.tablerow.ImprovedTableRow;
import sp.it.pl.layout.widget.Widget;
import sp.it.pl.layout.widget.Widget.Info;
import sp.it.pl.layout.widget.controller.LegacyController;
import sp.it.pl.layout.widget.controller.SimpleController;
import sp.it.pl.layout.widget.controller.io.IsInput;
import sp.it.pl.layout.widget.controller.io.Output;
import sp.it.pl.layout.widget.feature.SongReader;
import sp.it.pl.main.Widgets;
import sp.it.pl.util.access.Vo;
import sp.it.pl.util.access.fieldvalue.ColumnField;
import sp.it.pl.util.animation.Anim;
import sp.it.pl.util.animation.interpolator.ElasticInterpolator;
import sp.it.pl.util.conf.Config;
import sp.it.pl.util.conf.EditMode;
import sp.it.pl.util.conf.IsConfig;
import sp.it.pl.util.file.AudioFileFormat;
import sp.it.pl.util.file.AudioFileFormat.Use;
import sp.it.pl.util.file.FileType;
import sp.it.pl.util.graphics.drag.DragUtil;
import sp.it.pl.util.units.Dur;
import sp.it.pl.util.validation.Constraint;
import sp.it.pl.util.validation.Constraint.FileActor;
import static javafx.scene.control.SelectionMode.MULTIPLE;
import static javafx.scene.control.TableView.UNCONSTRAINED_RESIZE_POLICY;
import static javafx.scene.input.KeyCode.DELETE;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.TransferMode.COPY;
import static javafx.util.Duration.millis;
import static javafx.util.Duration.seconds;
import static sp.it.pl.audio.tagging.Metadata.Field.RATING;
import static sp.it.pl.audio.tagging.Metadata.Field.TITLE;
import static sp.it.pl.gui.nodeinfo.TableInfo.DEFAULT_TEXT_FACTORY;
import static sp.it.pl.layout.widget.Widget.Group.LIBRARY;
import static sp.it.pl.main.AppBuildersKt.appProgressIndicator;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.pl.util.animation.Anim.Interpolators.reverse;
import static sp.it.pl.util.async.AsyncKt.FX;
import static sp.it.pl.util.async.future.Fut.fut;
import static sp.it.pl.util.file.Util.getCommonRoot;
import static sp.it.pl.util.functional.Util.map;
import static sp.it.pl.util.functional.UtilKt.runnable;
import static sp.it.pl.util.graphics.Util.menuItem;
import static sp.it.pl.util.graphics.UtilKt.setScaleXY;
import static sp.it.pl.util.reactive.UtilKt.maintain;
import static sp.it.pl.util.system.EnvironmentKt.chooseFile;
import static sp.it.pl.util.system.EnvironmentKt.chooseFiles;

@Info(
    author = "Martin Polakovic",
    name = Widgets.SONG_TABLE,
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
@LegacyController
public class Library extends SimpleController implements SongReader {

    private static final PseudoClass PC_PLAYING = PseudoClass.getPseudoClass("played");
    private static final TableContextMenuR<MetadataGroup> contextMenu = new TableContextMenuR<>();

    private final FilteredTable<Metadata> table = new FilteredTable<>(Metadata.class, Metadata.EMPTY.getMainField());
    private final TaskInfo<Task<?>> taskInfo = new TaskInfo<>(null, new Label(), appProgressIndicator());
    private final Anim hideInfo = new Anim(at -> setScaleXY(taskInfo.getProgress(),at*at))
                                      .dur(millis(500)).intpl(reverse(new ElasticInterpolator()));
    private Output<Metadata> out_sel;

    @IsConfig(name = "Table orientation", info = "Orientation of the table.")
    public final Vo<NodeOrientation> orient = new Vo<>(APP.ui.getTableOrient());
    @IsConfig(name = "Zeropad numbers", info = "Adds 0s for number length consistency.")
    public final Vo<Boolean> zeropad = new Vo<>(APP.ui.getTableZeropad());
    @IsConfig(name = "Search show original index", info = "Show unfiltered table item index when filter applied.")
    public final Vo<Boolean> orig_index = new Vo<>(APP.ui.getTableOrigIndex());
    @IsConfig(name = "Show table header", info = "Show table header with columns.")
    public final Vo<Boolean> show_header = new Vo<>(APP.ui.getTableShowHeader());
    @IsConfig(name = "Show table footer", info = "Show table controls at the bottom of the table. Displays menu bar and table items information.")
    public final Vo<Boolean> show_footer = new Vo<>(APP.ui.getTableShowFooter());
    @IsConfig(editable = EditMode.APP) @Constraint.FileType(FileActor.ANY)
    private File lastFile = null;
    @IsConfig(editable = EditMode.APP) @Constraint.FileType(FileActor.DIRECTORY)
    private File lastDir = null;

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    public Library(Widget widget) {
        super(widget);
        root.setPrefSize(600.0, 850.0);

        out_sel = outputs.create(widget.id, "Selected", Metadata.class, null);
        onClose.plusAssign(Player.librarySelected.i.bind(out_sel));

        root.getChildren().add(table.getRoot());

        // table properties
        table.getSelectionModel().setSelectionMode(MULTIPLE);
        table.search.setColumn(TITLE);
        onClose.plusAssign(maintain(orient,table.nodeOrientationProperty()));
        onClose.plusAssign(maintain(zeropad,table.zeropadIndex));
        onClose.plusAssign(maintain(orig_index,table.showOriginalIndex));
        onClose.plusAssign(maintain(show_header,table.headerVisible));
        onClose.plusAssign(maintain(show_footer,table.footerVisible));

        // add progress indicator to bottom controls
        ((Pane)table.footerPane.getRight()).getChildren().addAll(taskInfo.getMessage(), taskInfo.getProgress());
        taskInfo.setVisible(false);

        // extend table items information
        table.items_info.setTextFactory((all, list) -> {
            double lengthMs = list.stream().mapToDouble(Metadata::getLengthInMs).sum();
            return DEFAULT_TEXT_FACTORY.invoke(all, list) + " - " + new Dur(lengthMs);
        });

        // add more menu items
        table.menuAdd.getItems().addAll(
            menuItem("Add files", e -> addFiles()),
            menuItem("Add directory", e -> addDirectory())
        );
        table.menuRemove.getItems().addAll(
            menuItem("Remove selected songs from library", e -> APP.db.removeItems(table.getSelectedItems())),
            menuItem("Remove all shown songs from library", e -> APP.db.removeItems(table.getItems())),
            menuItem("Remove all songs from library", e -> APP.db.removeItems(table.getItems())),
            menuItem("Remove missing songs from library", e -> removeInvalid())
        );

        // set up table columns
        table.setColumnFactory(f -> {
            TableColumn<Metadata,Object> c = new TableColumn<>(f.toString());
            c.setCellValueFactory(cf -> cf.getValue()==null ? null : new PojoV<>(f.getOf(cf.getValue())));
            c.setCellFactory(f==(Metadata.Field) RATING
                ? (Callback) APP.getRatingCell().getValue()
                : column -> table.buildDefaultCell(f)
            );
            return c;
        });
        // maintain rating column cell style
        onClose.plusAssign(APP.getRatingCell().maintain(cf -> table.getColumn(RATING).ifPresent(c -> c.setCellFactory((Callback)cf))));   // maintain playing item css
        // let resizing as it is
        table.setColumnResizePolicy(resize -> {
            boolean b = UNCONSTRAINED_RESIZE_POLICY.call(resize);
            // resize index column
            table.getColumn(ColumnField.INDEX).ifPresent(i -> i.setPrefWidth(table.computeIndexColumnWidth()));
            return b;
        });

        table.getDefaultColumnInfo();   // TODO remove (this triggers menu initialization)

        // restore column state
        String columnStateS = widget.properties.getS("columns");
        table.setColumnState(columnStateS!=null ? TableColumnInfo.fromString(columnStateS) : table.getDefaultColumnInfo());

        // row behavior
        table.setRowFactory(tbl -> new ImprovedTableRow<Metadata>() {{
                onLeftDoubleClick((r,e) -> PlaylistManager.use(pl->pl.setNplayFrom(table.getItems(), r.getIndex())));
                onRightSingleClick((r,e) -> {
                    // prep selection for context menu
                    if (!r.isSelected())
                        tbl.getSelectionModel().clearAndSelect(r.getIndex());

                    contextMenu.show(MetadataGroup.groupOfUnrelated(table.getSelectedItemsCopy()), table, e);
                });
                styleRuleAdd(PC_PLAYING, m -> Player.playingItem.get().same(m));
            }}
        );
        onClose.plusAssign(Player.playingItem.onUpdate(o -> table.updateStyleRules()));   // maintain playing item css

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
    public Collection<Config<Object>> getFields() {
        widget.properties.put("columns", table.getColumnState().toString());    // store column state
        return super.getFields();
    }

    /** Converts items to Metadata using {@link Item#toMeta()} (using no I/O) and displays them in the table. */
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

    private void addDirectory() {
        Window w = root.getScene().getWindow();
        FileChooser.ExtensionFilter ef = AudioFileFormat.filter(Use.APP);
        chooseFile("Add folder to library", FileType.DIRECTORY, lastDir, w, ef)
            .ifOk(file -> {
                APP.actionPane.show(file);
                lastDir = file.getParentFile();
            });
    }

    private void addFiles() {
        Window w = root.getScene().getWindow();
        FileChooser.ExtensionFilter ef = AudioFileFormat.filter(Use.APP);
        chooseFiles("Add files to library", lastFile, w, ef)
            .ifOk(files -> {
                APP.actionPane.show(files);
                lastFile = getCommonRoot(files);
            });
    }

    private void removeInvalid() {
        Task<Void> t = MetadataReader.buildRemoveMissingFromLibTask();
        fut(t)
            .useBy(FX, taskInfo::showNbind)
            .useBy(Task::run)
            .thenWait(seconds(5))
            .useBy(FX, it -> hideInfo.playOpenDo(runnable(taskInfo::hideNunbind)));
    }

}