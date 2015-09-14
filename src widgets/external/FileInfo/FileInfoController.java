package FileInfo;


import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.TilePane;

import AudioPlayer.Item;
import AudioPlayer.Player;
import AudioPlayer.services.Database.DB;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.Metadata.Field;
import AudioPlayer.tagging.MetadataWriter;
import Configuration.Config;
import Configuration.Config.PropertyConfig;
import Configuration.IsConfig;
import Layout.Widgets.Widget;
import Layout.Widgets.controller.FXMLController;
import Layout.Widgets.controller.io.IsInput;
import Layout.Widgets.controller.io.Output;
import Layout.Widgets.feature.SongReader;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import gui.objects.Rater.Rating;
import gui.objects.image.ThumbnailWithAdd;
import gui.objects.image.cover.Cover.CoverSource;
import gui.pane.ActionPane.SlowAction;
import gui.pane.ImageFlowPane;
import main.App;
import util.access.Ѵ;
import util.async.executor.EventReducer;
import util.async.future.Fut;
import util.graphics.drag.DragUtil;

import static AudioPlayer.tagging.Metadata.EMPTY;
import static AudioPlayer.tagging.Metadata.Field.*;
import static Layout.Widgets.Widget.Group.OTHER;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.DETAILS;
import static gui.objects.image.cover.Cover.CoverSource.ANY;
import static java.lang.Double.NaN;
import static java.lang.Double.max;
import static java.lang.Math.ceil;
import static java.lang.Math.floor;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static javafx.geometry.Orientation.VERTICAL;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.geometry.Pos.TOP_LEFT;
import static javafx.scene.control.OverrunStyle.ELLIPSIS;
import static util.File.FileUtil.copyFileSafe;
import static util.File.FileUtil.copyFiles;
import static util.async.Async.FX;
import static util.async.executor.EventReducer.toLast;
import static util.functional.Functors.Ƒ1.f1;
import static util.functional.Util.by;
import static util.functional.Util.list;
import static util.graphics.Util.layAnchor;

/**
 * File info widget controller.
 * <p>
 * @author Plutonium_
 */
@Widget.Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "File Info",
    description = "Displays song metadata. Supports rating change.",
    howto = ""
        + "    Displays metadata of a particular song. Song can be set manually, e.g., by drag & "
         + "drop, or the widget can follow the playing or table selections (playlist, etc.).\n"
        + "\n"
        + "Available actions:\n"
        + "    Cover left click : Browse file system & set cover\n"
        + "    Cover right click : Opens context menu\n"
        + "    Rater left click : Rates displayed song\n"
        + "    Drag&Drop songs : Displays information for the first song\n"
        + "    Drag&Drop image on cover: Sets images as cover\n",
    version = "1.0",
    year = "2015",
    group = OTHER
)
public class FileInfoController extends FXMLController implements SongReader {

    private @FXML AnchorPane root;
    private final ThumbnailWithAdd cover = new ThumbnailWithAdd();
    private final TilePane tiles = new FieldsPane();
    private final ImageFlowPane layout = new ImageFlowPane(cover, tiles);
    private final Rating rater = new Rating();
    private final Label gap1 = new Label(" "),
                        gap2 = new Label(" "),
                        gap3 = new Label(" ");
    private final List<Label> labels = new ArrayList<>();
    private final List<Lfield> fields = list(
        new Lfield(TITLE,0),
        new Lfield(TRACK_INFO,1),
        new Lfield(DISCS_INFO,2),
        new Lfield(LENGTH,3),
        new Lfield(ARTIST,4),
        new Lfield(ALBUM,5),
        new Lfield(ALBUM_ARTIST,6),
        new Lfield(YEAR,7),
        new Lfield(GENRE,8),
        new Lfield(COMPOSER,9),
        new Lfield(PUBLISHER,10),
        new Lfield(CATEGORY,11),
        new Lfield(RATING,12),
        new Lfield(PLAYCOUNT,13),
        new Lfield(COMMENT,14),
        new Lfield(FILESIZE,15),
        new Lfield(FILENAME,16),
        new Lfield(FORMAT,17),
        new Lfield(BITRATE,18),
        new Lfield(ENCODING,19),
        new Lfield(PATH,20)
    );
    private final Lfield rating = fields.get(12);

    private Output<Metadata> data_out;
    private Metadata data = EMPTY;

    // configs
    @IsConfig(name = "Column width", info = "Minimal width for field columns.")
    public final Ѵ<Double> minColumnWidth = new Ѵ<>(150.0, tiles::layout);
    @IsConfig(name = "Cover source", info = "Source for cover image.")
    public final Ѵ<CoverSource> cover_source = new Ѵ<>(ANY, this::setCover);
    @IsConfig(name = "Text clipping method", info = "Style of clipping text when too long.")
    public final Ѵ<OverrunStyle> overrun_style = new Ѵ<>(ELLIPSIS, this::setOverrun);
    @IsConfig(name = "Show cover", info = "Show cover.")
    public final Ѵ<Boolean> showCover = new Ѵ<>(true, layout::setImageVisible);
    @IsConfig(name = "Show fields", info = "Show fields.")
    public final Ѵ<Boolean> showFields = new Ѵ<>(true, layout::setContentVisible);
    @IsConfig(name = "Show empty fields", info = "Show empty fields.")
    public final Ѵ<Boolean> showEmptyFields = new Ѵ<>(true, v -> update());
    @IsConfig(name = "Group fields", info = "Use gaps to separate fields into group.")
    public final Ѵ<Sort> groupFields = new Ѵ<>(Sort.SEMANTIC,this::update);
    @IsConfig(name = "Allow no content", info = "Otherwise shows previous content when the new content is empty.")
    public boolean allowNoContent = false;
    // generate show {field} configs
    private final Map<String,Config> fieldconfigs = fields.stream()
            .map(f -> new PropertyConfig<>("show_"+f.name, "Show " + f.name, f.visibleConfig,
                    "FileInfoController","Show this field",true,NaN,NaN))
            .collect(toMap(c -> c.getName(), c -> c));

    @Override
    public void init() {
        data_out = outputs.create(widget.id, "Displayed", Metadata.class, EMPTY);

        // keep updated contents, we do this directly instead of looking up the Input, same effect
        d(Player.onItemRefresh(refreshed -> refreshed.ifHasE(data, this::read)));

        cover.getPane().setDisable(true); // shoud be handled differently, either init all or none
        cover.setBackgroundVisible(false);
        cover.setBorderToImage(false);
        cover.onFileDropped = fut_file ->
            App.actionPane.show(File.class, fut_file,
                new SlowAction<>("Copy and set as album cover",
                        "Sets image as cover. Copies file to destination and renames it "
                      + "to 'cover' so it is recognized as album cover. Any previous cover file "
                      + "will be preserved by renaming."
                      + "\n\nDestination: " + data.getLocation().getPath(),
                        FontAwesomeIcon.PASTE,
                        f -> setAsCover(f, true)),
                new SlowAction<>("Copy to location",
                        "Copies image to destination. Any such existing file is overwritten."
                      + "\n\nDestination: " + data.getLocation().getPath(),
                        FontAwesomeIcon.COPY,
                        f -> setAsCover(f, false)),
                new SlowAction<>("Write to tag (single)",
                        "Writes image as cover to song tag. Other songs of the song's album remain "
                      + "untouched.",
                        FontAwesomeIcon.TAG,
                        f -> tagAsCover(f,false)),
//                // for debugging purposes to simulate long running actions
//                new SlowAction<>("Long running action test", "", FontAwesomeIcon.ANGELLIST,
//                        f -> f.then(() -> {
//                            try {
//                                Thread.sleep(500);
//                            } catch (InterruptedException ex) {
//                                Logger.getLogger(FileInfoController.class.getName()).log(Level.SEVERE, null, ex);
//                            }
//                        })),
                new SlowAction<>("Write to tag (album)",
                        "Writes image as cover to all songs in this song's album. Only songs in the "
                      + "library are considered. Songs with no album are ignored. At minimum the "
                      + "displayed song will be updated (even if not in library or has no album).",
                        FontAwesomeIcon.TAGS,
                        f -> tagAsCover(f,true))
            );

        layAnchor(root,layout,0d);
        layout.setMinContentSize(200,120);
        layout.setGap(5);
        tiles.setPadding(new Insets(5));

        // align tiles from left top & tile content to center left
        tiles.setAlignment(TOP_LEFT);
        tiles.setTileAlignment(CENTER_LEFT);

        // add rater stars to rating label as graphics
        rating.setGraphic(rater);
        rating.setContentDisplay(ContentDisplay.RIGHT);

        // bind rating to app configs
        rater.icons.bind(App.maxRating);
        rater.partialRating.bind(App.partialRating);
        rater.updateOnHover.bind(App.hoverRating);
        rater.editable.bind(App.allowRatingChange);
        // write metadata on rating change
        rater.setOnRatingChanged( r -> MetadataWriter.useToRate(data, r));

        // drag & drop
        DragUtil.installDrag(
            root, DETAILS, "Display",
            DragUtil::hasAudio,
            e -> DragUtil.getSongs(e)
                         .use(items -> items.findFirst().ifPresent(this::read),FX)
                         .run()
        );
    }

    @Override
    public void refresh() {
        minColumnWidth.applyValue();
        cover_source.applyValue();
        overrun_style.applyValue();
        showCover.applyValue();
        showFields.applyValue();
    }

    @Override
    public boolean isEmpty() {
        return data==null || data == EMPTY;
    }

    @Override
    public Collection<Config<Object>> getFields() {
        Collection<Config<Object>> c = list(super.getFields());
        c.addAll((Collection)fieldconfigs.values());
        return c;
    }

    @Override
    public Config<Object> getField(String n) {
        return Optional.ofNullable((Config)fieldconfigs.get(n))
                       .orElseGet(() -> super.getField(n));
    }

/********************************** FEATURES **********************************/

    /** {@inheritDoc} */
    @Override
    @IsInput("To display")
    public void read(Item item) {
        reading.push(item);
    }

    /** {@inheritDoc} */
    @Override
    public void read(List<? extends Item> items) {
        read(items.isEmpty() ? null : items.get(0));
    }

/********************************* PRIVATE API ********************************/

    private final EventReducer<Item> reading = toLast(200,this::setValue);

    // item -> metadata
    private void setValue(Item i) {
        if(i==null) setValue(EMPTY);
        else if(i instanceof Metadata) setValue((Metadata)i);
        else App.itemToMeta(i, this::setValue);
    }

    private void setValue(Metadata m) {
        // no empty content if desired
        if(!allowNoContent && m==EMPTY) return;

        // remember data
        data = m;
        data_out.setValue(m);

        // gui (fill out data)
        fields.forEach(l -> l.setVal(m));
        rater.rating.set(m==EMPTY ? 0d : m.getRatingPercent());
        setCover(cover_source.getValue());

        update();
    }

    private void update() {
        // initialize
        labels.clear();
        cover.getPane().setDisable(isEmpty());

        // sort
        if (groupFields.getValue()==Sort.SEMANTIC) {
        labels.clear();
            fields.sort(by(l -> l.semantic_index));
            labels.addAll(fields);
            labels.add(4,gap1);
            labels.add(10,gap2);
            labels.add(17,gap3);
        } else {
        labels.clear();
            fields.sort(by(l -> l.name));
            labels.addAll(fields);
        }

        // show visible
        fields.forEach(Lfield::setHide);
        tiles.getChildren().setAll(labels);
        tiles.layout();
    }

    private void setCover(CoverSource source) {
        cover.loadImage(isEmpty() ? null : data.getCover(source));
    }

    private Fut setAsCover(Fut<File> ff, boolean setAsCover) {
        Consumer<File> a = setAsCover
            ? file -> copyFileSafe(file, data.getLocation(), "cover")
            : file -> copyFiles(list(file), data.getLocation(), REPLACE_EXISTING);
        return ff.use(f1(a).passNull())
                 .then(cover_source::applyValue,FX)         // refresh cover
                 .showProgress(App.getWindow().taskAdd());
    }

    private Fut tagAsCover(Fut<File> ff, boolean includeAlbum) {
        Consumer<File> a = f -> {
            Collection<Metadata> items = includeAlbum
                // get all known songs from album
                ? DB.items.o.getValue().stream()
                    // we must not write when album is empty! that could have disastrous consequences!
                    .filter(m -> !m.getAlbum().isEmpty() && m.getAlbum().equals(data.getAlbum()))
                    .collect(toSet())
                : new HashSet<>();
            items.add(data); // make sure the original is included (Set avoids duplicates)

            MetadataWriter.useNoRefresh(items, w -> w.setCover(f));
            Player.refreshItems(items);
        };
        return ff.use(f1(a).passNull())
                 .showProgress(App.getWindow().taskAdd());
    }

    private void setOverrun(OverrunStyle os) {
        fields.forEach(l -> l.setTextOverrun(os));
    }

    private static enum Sort {
        SEMANTIC,
        ALPHANUMERIC;
    }
    private class Lfield extends Label {
        final Field field;
        final Ѵ<Boolean> visibleConfig;
        final String name;
        final int semantic_index;

        public Lfield(Field field, int i) {
            this.field = field;
            this.visibleConfig = new Ѵ<>(true,FileInfoController.this::update);
            this.semantic_index = i;

            if(field==DISCS_TOTAL) name = "disc";
            else if(field==TRACKS_TOTAL) name = "track";
            else if(field==PATH) name = "location";
            else name = field.toStringEnum().toLowerCase();
        }

        void setVal(Metadata m) {
            String v = m==EMPTY || field==RATING ? "" : m.getFieldS(field,"");
                   v = v.replace('\r', ' ');
                   v = v.replace('\n', ',');
            setText(name + ": " + v);
        }

        void setHide() {
            String content = getText().substring(getText().indexOf(": ")+2).trim();
            boolean e = content.isEmpty() ||
                     content.equalsIgnoreCase("?/?") ||
                       content.equalsIgnoreCase("n/a") ||
                         content.equalsIgnoreCase("unknown");
                    e &= field!=RATING;
            setDisable(e);
            if (!visibleConfig.getValue() || (!showEmptyFields.getValue() && e))
                labels.remove(this);
        }
    }
    private class FieldsPane extends TilePane {

        public FieldsPane() {
            super(VERTICAL,10,0);
        }

        @Override
        protected void layoutChildren() {
            double width = getWidth();
            double height = getHeight();

            double cellH = 15+tiles.getVgap();
            int rows = (int)floor(max(height, 5)/cellH);
            if(rows==0) rows=1;
            int columns = 1+(int) ceil(labels.size()/rows);
            double cellW = columns==1 || columns==0
                // dont allow 0 columns & set whole width if 1 column
                // handle 1 column manually - the below caused some problems
                ? width
                // for n elements there is n-1 gaps so we need to add 1 gap width
                // above cell width includes 1 gap width per element so substract it
                : (width + tiles.getHgap())/columns - tiles.getHgap();

            // adhere to requested minimum size
            cellW = max(cellW, minColumnWidth.getValue());

            double w = cellW;
            tiles.setPrefTileWidth(w);
            labels.forEach(l -> l.setMaxWidth(w));

            super.layoutChildren();
        }
    }
}