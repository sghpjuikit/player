package fileInfo;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javafx.geometry.Insets;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.TilePane;
import sp.it.pl.audio.Player;
import sp.it.pl.audio.Song;
import sp.it.pl.audio.tagging.Metadata;
import sp.it.pl.audio.tagging.Metadata.Field;
import sp.it.pl.gui.objects.image.ThumbnailWithAdd;
import sp.it.pl.gui.objects.image.cover.Cover.CoverSource;
import sp.it.pl.gui.objects.rating.Rating;
import sp.it.pl.gui.pane.ImageFlowPane;
import sp.it.pl.gui.pane.SlowAction;
import sp.it.pl.layout.widget.Widget;
import sp.it.pl.layout.widget.controller.LegacyController;
import sp.it.pl.layout.widget.controller.SimpleController;
import sp.it.pl.layout.widget.controller.io.Input;
import sp.it.pl.layout.widget.controller.io.Output;
import sp.it.pl.layout.widget.feature.SongReader;
import sp.it.util.access.V;
import sp.it.util.async.executor.EventReducer;
import sp.it.util.async.executor.EventReducer.HandlerLast;
import sp.it.util.conf.Config;
import sp.it.util.conf.Config.PropertyConfig;
import sp.it.util.conf.EditMode;
import sp.it.util.conf.IsConfig;
import static java.lang.Double.max;
import static java.lang.Integer.max;
import static java.lang.Math.ceil;
import static java.lang.Math.floor;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static javafx.geometry.Orientation.VERTICAL;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.geometry.Pos.TOP_LEFT;
import static javafx.scene.control.OverrunStyle.ELLIPSIS;
import static kotlin.collections.CollectionsKt.mapIndexed;
import static sp.it.pl.audio.tagging.Metadata.Field.ALBUM;
import static sp.it.pl.audio.tagging.Metadata.Field.ALBUM_ARTIST;
import static sp.it.pl.audio.tagging.Metadata.Field.ARTIST;
import static sp.it.pl.audio.tagging.Metadata.Field.BITRATE;
import static sp.it.pl.audio.tagging.Metadata.Field.CATEGORY;
import static sp.it.pl.audio.tagging.Metadata.Field.COMMENT;
import static sp.it.pl.audio.tagging.Metadata.Field.COMPOSER;
import static sp.it.pl.audio.tagging.Metadata.Field.DISCS_INFO;
import static sp.it.pl.audio.tagging.Metadata.Field.DISCS_TOTAL;
import static sp.it.pl.audio.tagging.Metadata.Field.ENCODING;
import static sp.it.pl.audio.tagging.Metadata.Field.FILENAME;
import static sp.it.pl.audio.tagging.Metadata.Field.FILESIZE;
import static sp.it.pl.audio.tagging.Metadata.Field.FORMAT;
import static sp.it.pl.audio.tagging.Metadata.Field.GENRE;
import static sp.it.pl.audio.tagging.Metadata.Field.LENGTH;
import static sp.it.pl.audio.tagging.Metadata.Field.PATH;
import static sp.it.pl.audio.tagging.Metadata.Field.PLAYCOUNT;
import static sp.it.pl.audio.tagging.Metadata.Field.PUBLISHER;
import static sp.it.pl.audio.tagging.Metadata.Field.RATING;
import static sp.it.pl.audio.tagging.Metadata.Field.TITLE;
import static sp.it.pl.audio.tagging.Metadata.Field.TRACKS_TOTAL;
import static sp.it.pl.audio.tagging.Metadata.Field.TRACK_INFO;
import static sp.it.pl.audio.tagging.Metadata.Field.YEAR;
import static sp.it.pl.audio.tagging.SongWritingKt.rate;
import static sp.it.pl.audio.tagging.SongWritingKt.writeNoRefresh;
import static sp.it.pl.gui.objects.image.cover.Cover.CoverSource.ANY;
import static sp.it.pl.layout.widget.Widget.Group.OTHER;
import static sp.it.pl.main.AppDragKt.getAudio;
import static sp.it.pl.main.AppDragKt.hasAudio;
import static sp.it.pl.main.AppDragKt.installDrag;
import static sp.it.pl.main.AppExtensionsKt.scaleEM;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.util.async.AsyncKt.FX;
import static sp.it.util.async.AsyncKt.runFX;
import static sp.it.util.async.AsyncKt.runNew;
import static sp.it.util.dev.FailKt.failIfFxThread;
import static sp.it.util.file.Util.copyFileSafe;
import static sp.it.util.file.Util.copyFiles;
import static sp.it.util.functional.Util.by;
import static sp.it.util.functional.Util.list;
import static sp.it.util.functional.UtilKt.consumer;
import static sp.it.util.reactive.UtilKt.syncTo;

@Widget.Info(
    author = "Martin Polakovic",
    name = "File Info",
    description = "Displays song information. Supports rating change.",
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
    version = "1.0.0",
    year = "2015",
    group = OTHER
)
@LegacyController
public class FileInfo extends SimpleController implements SongReader {

    private final ThumbnailWithAdd cover = new ThumbnailWithAdd();
    private final TilePane tiles = new FieldsPane();
    private final ImageFlowPane layout = new ImageFlowPane(cover, tiles);
    private final Rating rater = new Rating();
    private final Label gap1 = new Label(" "),
                        gap2 = new Label(" "),
                        gap3 = new Label(" ");
    private final List<Label> labels = new ArrayList<>();

    public final Input<Song> inputValue;

    @IsConfig(name = "Column width", info = "Minimal width for field columns.")
    public final V<Double> minColumnWidth = new V<>(150.0).initAttachC(v -> tiles.layout());
    @IsConfig(name = "Cover source", info = "Source for cover image.")
    public final V<CoverSource> cover_source = new V<>(ANY).initAttachC(this::setCover);
    @IsConfig(name = "Text clipping method", info = "Style of clipping text when too long.")
    public final V<OverrunStyle> overrun_style = new V<>(ELLIPSIS);
    @IsConfig(name = "Show cover", info = "Show cover.")
    public final V<Boolean> showCover = new V<>(true).initSyncC(layout::setImageVisible);
    @IsConfig(name = "Show fields", info = "Show fields.")
    public final V<Boolean> showFields = new V<>(true).initSyncC(layout::setContentVisible);
    @IsConfig(name = "Show empty fields", info = "Show empty fields.")
    public final V<Boolean> showEmptyFields = new V<>(true).initAttachC(v -> update());
    @IsConfig(name = "Group fields", info = "Use gaps to separate fields into group.")
    public final V<Sort> groupFields = new V<>(Sort.SEMANTIC).initAttachC(v -> update());
    @IsConfig(name = "Allow no content", info = "Otherwise shows previous content when the new content is empty.")
    public boolean allowNoContent = false;

    private final List<LField> fields = mapIndexed(
        list(
            TITLE, TRACK_INFO, DISCS_INFO, LENGTH, ARTIST,
            ALBUM, ALBUM_ARTIST, YEAR, GENRE, COMPOSER,
            PUBLISHER, CATEGORY, RATING, PLAYCOUNT, COMMENT,
            FILESIZE, FILENAME, FORMAT, BITRATE, ENCODING, PATH
        ),
        (i,f) -> new LField(f,i)
    );
    private final LField rating = fields.get(12);
    private final Map<String,Config<Boolean>> fieldConfigs = fields.stream()
        .map(f -> new PropertyConfig<>(Boolean.class, "show_"+f.name, "Show " + f.name, f.visibleConfig, "FileInfo","Show this field", EditMode.USER))
        .collect(toMap(c -> c.getName(), c -> c));
    private Output<Metadata> data_out = io.o.create("Displayed", Metadata.class, Metadata.EMPTY);
    private Metadata data = Metadata.EMPTY;
    private final HandlerLast<Song> dataReading = EventReducer.toLast(200, this::setValue);

    public FileInfo(Widget widget) {
        super(widget);
        root.setPrefSize(scaleEM(400.0), scaleEM(400.0));

        inputValue = io.i.create("To display", Object.class, null, consumer(v -> dataReading.push(v)));

        // keep updated content (unless the content is scheduled for change, then this could cause invalid content)
        onClose.plusAssign(Player.onSongRefresh(refreshed -> {
            if (!dataReading.hasEventsQueued())
                refreshed.ifHasE(data, this::read);
        }));

        cover.getPane().setDisable(true);
        cover.setBackgroundVisible(false);
        cover.setBorderToImage(false);
        cover.onFileDropped = fut_file -> {
        	if (data.isFileBased()) {
	            APP.actionPane.show(File.class, fut_file, true,
	                new SlowAction<>("Copy and set as album cover",
	                        "Sets image as cover. Copies file to destination and renames it "
	                      + "to 'cover' so it is recognized as album cover. Any previous cover file "
	                      + "will be preserved by renaming."
	                      + "\n\nDestination: " + data.getLocation().getPath(),
	                        FontAwesomeIcon.PASTE,
	                        consumer(f -> setAsCover(f, true))
	                ),
	                new SlowAction<>("Copy to location",
	                        "Copies image to destination. Any such existing file is overwritten."
	                      + "\n\nDestination: " + data.getLocation().getPath(),
	                        FontAwesomeIcon.COPY,
	                        consumer(f -> setAsCover(f, false))
	                ),
	                new SlowAction<>("Write to tag (single)",
	                        "Writes image as cover to song tag. Other songs of the song's album remain "
	                      + "untouched.",
	                        FontAwesomeIcon.TAG,
	                        consumer(f -> tagAsCover(f,false))
	                ),
	                new SlowAction<>("Write to tag (album)",
	                        "Writes image as cover to all songs in this song's album. Only songs in the "
	                      + "library are considered. Songs with no album are ignored. At minimum the "
	                      + "displayed song will be updated (even if not in library or has no album).",
	                        FontAwesomeIcon.TAGS,
	                        consumer(f -> tagAsCover(f,true))
	                )
	            );
	        }
        };

        layout.setMinContentSize(200,120);
        layout.setGap(5);
        tiles.setPadding(new Insets(5));

        root.getChildren().add(layout);

        // align tiles from left top & tile content to center left
        tiles.setAlignment(TOP_LEFT);
        tiles.setTileAlignment(CENTER_LEFT);

        // add rater stars to rating label as graphics
        rating.setGraphic(rater);
        rating.setContentDisplay(ContentDisplay.RIGHT);

        // bind rating to app configs
        onClose.plusAssign(syncTo(APP.ui.getMaxRating(),rater.icons));
        onClose.plusAssign(syncTo(APP.ui.getPartialRating(), rater.partialRating));
        rater.editable.set(true);
        rater.onRatingEdited = consumer(it -> rate(data, it));

        // drag & drop
        installDrag(
            root, MaterialIcon.DETAILS, "Display",
            e -> hasAudio(e.getDragboard()),
            consumer(e -> getAudio(e.getDragboard()).stream().findFirst().ifPresent(this::read))
        );
    }

    public boolean isEmpty() {
        return data==null || data==Metadata.EMPTY;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<Config<Object>> getFields() {
        Collection<Config<Object>> c = list(super.getFields());
        c.addAll((Collection) fieldConfigs.values());
        return c;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Config<Object> getField(String n) {
        return Optional.ofNullable((Config) fieldConfigs.get(n))
                       .orElseGet(() -> super.getField(n));
    }

    @Override
    public void read(Song song) {
        inputValue.setValue(song);
    }

    @Override
    public void read(List<? extends Song> items) {
        read(items.isEmpty() ? null : items.get(0));
    }

    private void setValue(Song i) {
        if (i==null) setValue(Metadata.EMPTY);
        else if (i instanceof Metadata) setValue((Metadata)i);
        else APP.db.songToMeta(i, consumer(this::setValue));
    }

    private void setValue(Metadata m) {
        if (!allowNoContent && m==Metadata.EMPTY) return; // no empty content if desired

        // remember data
        data = m;
        data_out.setValue(m);

        // gui (fill out data)
        // note we disallow empty rating so rater remains editable
        fields.forEach(l -> l.setVal(m));
        setCover(cover_source.getValue());
        rater.rating.set(m.getRatingPercent());

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
        fields.forEach(LField::setHide);
        tiles.getChildren().setAll(labels);
        tiles.layout();
    }

    private void setCover(CoverSource source) {
        Metadata id = data;
        runNew(() -> data.getCover(source).getImage())
            .useBy(FX, img -> {
                if (id==data)
                    cover.loadImage(img);
            });
    }

    private void setAsCover(File file, boolean setAsCover) {
        failIfFxThread();
        if (file==null || !data.isFileBased()) return;

        if (setAsCover) copyFileSafe(file, data.getLocation(), "cover");
        else copyFiles(list(file), data.getLocation(), REPLACE_EXISTING);

        runFX(() -> setCover(cover_source.getValue()));
    }

    private void tagAsCover(File file, boolean includeAlbum) {
        failIfFxThread();
        if (file==null || !data.isFileBased()) return;

        Collection<Metadata> items = includeAlbum
            // get all known songs from album
            ? APP.db.getSongs().o.getValue().stream()
                // we must not write when album is empty! that could have disastrous consequences!
                .filter(m -> m.getAlbum()!=null && m.getAlbum().equals(data.getAlbum()))
                .collect(toSet())
            : new HashSet<>();
        items.add(data); // make sure the original is included (Set avoids duplicates)

        writeNoRefresh(items, consumer(w -> w.setCover(file)));
        Player.refreshSongs(items);
    }

    private enum Sort { SEMANTIC, ALPHANUMERIC }

    private class LField extends Label {
        final Field<?> field;
        final V<Boolean> visibleConfig;
        final String name;
        final int semantic_index;

        public LField(Field<?> field, int i) {
            this.field = field;
            this.visibleConfig = new V<>(true).initAttachC(v -> update());
            this.semantic_index = i;
            this.textOverrunProperty().bind(overrun_style);

            if (field==DISCS_TOTAL) name = "disc";
            else if (field==TRACKS_TOTAL) name = "track";
            else if (field==PATH) name = "location";
            else name = field.name().toLowerCase();
        }

        void setVal(Metadata m) {
            String v = m==Metadata.EMPTY || field==RATING ? "" : m.getFieldS(field, "");
                   v = v.replace('\r', ' ');
                   v = v.replace('\n', ',');
            setText(name + ": " + v);
        }

        void setHide() {
            String content = (getText()==null || getText().isEmpty()) ? "" : getText().substring(getText().indexOf(": ")+2).trim();
            boolean e = content.isEmpty() || content.equalsIgnoreCase("?/?") || content.equalsIgnoreCase("n/a") || content.equalsIgnoreCase("unknown");
                    e &= field!=RATING;
            setDisable(e);
            if (!visibleConfig.getValue() || (!showEmptyFields.getValue() && e))
                labels.remove(this);
        }
    }

    private class FieldsPane extends TilePane {

        FieldsPane() {
            super(VERTICAL,10,0);
        }

        @Override
        protected void layoutChildren() {
            double width = getWidth();
            double height = getHeight();

            double cellH = 15+tiles.getVgap();
            int rows = max(1, (int) floor(max(height, 5)/cellH));
            int columns = max(1, (int) ceil(((double)labels.size())/((double) rows)));
            double cellW = columns==1 || columns==0
                // do not allow 0 columns & set whole width if 1 column
                // handle 1 column manually - the below caused some problems
                ? width
                // for n elements there is n-1 gaps so we need to add 1 gap width
                // above cell width includes 1 gap width per element so subtract it
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