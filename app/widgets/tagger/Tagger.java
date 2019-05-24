package tagger;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import java.io.File;
import java.net.URI;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.effect.BoxBlur;
import javafx.scene.image.Image;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import sp.it.pl.audio.Song;
import sp.it.pl.audio.tagging.AudioFileFormat;
import sp.it.pl.audio.tagging.Metadata;
import sp.it.pl.audio.tagging.SongReadingKt;
import sp.it.pl.audio.tagging.SongWritingKt;
import sp.it.pl.gui.itemnode.textfield.MoodItemNode;
import sp.it.pl.gui.objects.autocomplete.AutoCompletion;
import sp.it.pl.gui.objects.icon.CheckIcon;
import sp.it.pl.gui.objects.icon.Icon;
import sp.it.pl.gui.objects.image.ThumbnailWithAdd;
import sp.it.pl.gui.objects.image.cover.Cover;
import sp.it.pl.gui.objects.popover.NodePos;
import sp.it.pl.gui.objects.popover.PopOver;
import sp.it.pl.gui.objects.popover.ScreenPos;
import sp.it.pl.gui.objects.textfield.DecoratedTextField;
import sp.it.pl.layout.widget.Widget;
import sp.it.pl.layout.widget.controller.LegacyController;
import sp.it.pl.layout.widget.controller.SimpleController;
import sp.it.pl.layout.widget.controller.io.Input;
import sp.it.pl.layout.widget.feature.SongReader;
import sp.it.pl.layout.widget.feature.SongWriter;
import sp.it.pl.main.AppProgress;
import sp.it.pl.main.Widgets;
import sp.it.pl.service.notif.Notifier;
import sp.it.util.access.V;
import sp.it.util.collections.mapset.MapSet;
import sp.it.util.conf.IsConfig;
import sp.it.util.functional.Functors.Ƒ1;
import sp.it.util.functional.Util;
import sp.it.util.type.TypeToken;
import sp.it.util.validation.InputConstraints;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static javafx.application.Platform.runLater;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.scene.control.ProgressIndicator.INDETERMINATE_PROGRESS;
import static javafx.scene.input.KeyCode.BACK_SPACE;
import static javafx.scene.input.KeyCode.CONTROL;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseDragEvent.MOUSE_DRAG_RELEASED;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import static javafx.scene.layout.GridPane.REMAINING;
import static javafx.scene.layout.Region.USE_PREF_SIZE;
import static org.atteo.evo.inflector.English.plural;
import static sp.it.pl.audio.tagging.Metadata.Field.ADDED_TO_LIBRARY;
import static sp.it.pl.audio.tagging.Metadata.Field.ALBUM;
import static sp.it.pl.audio.tagging.Metadata.Field.ALBUM_ARTIST;
import static sp.it.pl.audio.tagging.Metadata.Field.ARTIST;
import static sp.it.pl.audio.tagging.Metadata.Field.CATEGORY;
import static sp.it.pl.audio.tagging.Metadata.Field.COMMENT;
import static sp.it.pl.audio.tagging.Metadata.Field.COMPOSER;
import static sp.it.pl.audio.tagging.Metadata.Field.CUSTOM1;
import static sp.it.pl.audio.tagging.Metadata.Field.CUSTOM2;
import static sp.it.pl.audio.tagging.Metadata.Field.CUSTOM3;
import static sp.it.pl.audio.tagging.Metadata.Field.CUSTOM4;
import static sp.it.pl.audio.tagging.Metadata.Field.CUSTOM5;
import static sp.it.pl.audio.tagging.Metadata.Field.DISC;
import static sp.it.pl.audio.tagging.Metadata.Field.DISCS_TOTAL;
import static sp.it.pl.audio.tagging.Metadata.Field.FIRST_PLAYED;
import static sp.it.pl.audio.tagging.Metadata.Field.GENRE;
import static sp.it.pl.audio.tagging.Metadata.Field.LAST_PLAYED;
import static sp.it.pl.audio.tagging.Metadata.Field.LYRICS;
import static sp.it.pl.audio.tagging.Metadata.Field.MOOD;
import static sp.it.pl.audio.tagging.Metadata.Field.PLAYCOUNT;
import static sp.it.pl.audio.tagging.Metadata.Field.PUBLISHER;
import static sp.it.pl.audio.tagging.Metadata.Field.RATING;
import static sp.it.pl.audio.tagging.Metadata.Field.RATING_RAW;
import static sp.it.pl.audio.tagging.Metadata.Field.TAGS;
import static sp.it.pl.audio.tagging.Metadata.Field.TITLE;
import static sp.it.pl.audio.tagging.Metadata.Field.TRACK;
import static sp.it.pl.audio.tagging.Metadata.Field.TRACKS_TOTAL;
import static sp.it.pl.audio.tagging.Metadata.Field.YEAR;
import static sp.it.pl.gui.objects.image.cover.Cover.CoverSource.TAG;
import static sp.it.pl.layout.widget.controller.SimpleControllerKt.fxmlLoaderForController;
import static sp.it.pl.main.AppBuildersKt.appProgressIndicator;
import static sp.it.pl.main.AppBuildersKt.formIcon;
import static sp.it.pl.main.AppBuildersKt.infoIcon;
import static sp.it.pl.main.AppDragKt.getAudio;
import static sp.it.pl.main.AppDragKt.hasAudio;
import static sp.it.pl.main.AppExtensionsKt.scaleEM;
import static sp.it.pl.main.AppFileKt.isAudioEditable;
import static sp.it.pl.main.AppFileKt.isImageJaudiotagger;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.util.Util.startsWithNoCase;
import static sp.it.util.async.AsyncKt.FX;
import static sp.it.util.async.AsyncKt.runNew;
import static sp.it.util.file.Util.getSuffix;
import static sp.it.util.functional.Util.noDups;
import static sp.it.util.functional.Util.split;
import static sp.it.util.functional.UtilKt.consumer;
import static sp.it.util.functional.UtilKt.orNull;
import static sp.it.util.functional.UtilKt.runnable;
import static sp.it.util.reactive.UtilKt.syncC;
import static sp.it.util.ui.UtilKt.containsMouse;
import static sp.it.util.ui.UtilKt.createIcon;
import static sp.it.util.ui.UtilKt.pseudoclass;
import static sp.it.util.ui.UtilKt.setTextAlignment;
import static sp.it.util.ui.drag.DragUtilKt.handlerAccepting;

/**
 * Tagger graphical component.
 * <p/>
 * Can read and write metadata from/into files.
 * Currently supports files only. File types are limited to those supported
 * by the application.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
@Widget.Info(
    name = Widgets.SONG_TAGGER,
    author = "Martin Polakovic",
    howto = "Available actions:\n"
          + "    Drag cover away : Removes cover\n"
          + "    Drop image file : Adds cover\n"
          + "    Drop audio files : Sets files to tagger\n"
          + "    Drop audio files + CTRL:  Adds files to tagger\n"
          + "    Write : Saves the tags\n"
          + "    Loaded items label click : Opens editable source list of items",
    description = "Tag editor for audio files. Supports reading and writing to "
         + "tag. Taggable items can be unselected in selective list mode.",
    notes = "To do: improve tagging performance. Support for .ogg and .flac.",
    version = "0.8.0",
    year = "2015",
    group = Widget.Group.TAGGER
)
@LegacyController
public class Tagger extends SimpleController implements SongWriter, SongReader {

    private static Color EMPTY_COLOR = new Color(0, 0, 0, 0);

    public final Input<List<Song>> inputValue;

    @FXML VBox content;
    @FXML BorderPane header;
    @FXML AnchorPane scrollContent;
    @FXML ScrollPane scrollRoot;
    @FXML GridPane grid;
    @FXML DecoratedTextField titleF, albumF, artistF, albumArtistF, composerF, publisherF, trackF, tracksTotalF,
                     discF, discsTotalF, genreF, categoryF, yearF, ratingF, ratingPF, playcountF, commentF,
                     colorF, custom1F, custom2F, custom3F, custom4F, custom5F, playedFirstF, playedLastF, addedToLibF, tagsF;
          MoodItemNode moodF = new MoodItemNode();
    @FXML ColorPicker colorFPicker;
    @FXML TextArea LyricsA;
    @FXML BorderPane coverContainer;
    @FXML StackPane coverSuperContainer;
    @FXML Label CoverL, noCoverL;
    ThumbnailWithAdd CoverV;
    File new_cover_file = null;
    ProgressIndicator progressI;
    @FXML Label infoL, placeholder, fieldDesc;

    private final ObservableList<Song> allSongs = FXCollections.observableArrayList();
    private final List<Metadata> metadatas = new ArrayList<>();   // currently in gui active
    private final List<TagField> fields = new ArrayList<>();
    private boolean writing = false;    // prevents external data change during writing
    private final List<Validation> validators = new ArrayList<>();

    @IsConfig(name = "Field text alignment", info = "Alignment of the text in fields.")
    public final V<Pos> fieldTextAlignment = new V<>(CENTER_LEFT);
    @IsConfig(name="Mood picker popup position", info = "Position of the mood picker pop up relative to the mood text field.")
    public final V<NodePos> popupPos = moodF.getPickerPosition();

    public Tagger(Widget widget) {
        super(widget);
        root.setMinSize(USE_PREF_SIZE, USE_PREF_SIZE);
        root.setPrefSize(scaleEM(650), scaleEM(700));

        inputValue = io.i.create("Edit", new TypeToken<List<Song>>() {}.getType(), null, consumer(v -> read(v)));

        fxmlLoaderForController(this).loadNoEx();

        Node okB = formIcon(FontAwesomeIcon.CHECK, "Save", runnable(this::write));
        content.getChildren().add(okB);

        CoverV = new ThumbnailWithAdd(FontAwesomeIcon.PLUS,"Add to Tag");
        CoverV.getPane().setPrefSize(200, 200);
        CoverV.onFileDropped = consumer(f -> f.useBy(FX, this::addImg));
        CoverV.onHighlight = consumer(v -> noCoverL.setVisible(!v));
        coverContainer.setCenter(CoverV.getPane());

        progressI = appProgressIndicator();
        progressI.setVisible(false);
        header.setRight(progressI);

        setTextAlignment(LyricsA, TextAlignment.CENTER);

        // add specialized mood text field
        moodF.setMinSize(0, 0);
        moodF.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        grid.add(moodF, 1, 14, 2, 1);
        GridPane.setHalignment(moodF, HPos.LEFT);
        GridPane.setColumnSpan(moodF, REMAINING);

        // style
        onClose.plusAssign(
            syncC(APP.ui.getFont(), f -> {
                grid.getRowConstraints().forEach(it -> it.setPrefHeight(24*f.getSize()/12.0));
            })
        );

        // validators
        Predicate<String> IsBetween0And1 = noEx(false,(String t) -> {
            double i = Double.parseDouble(t);
            return i>=0 && i<=1;
        },NumberFormatException.class)::apply;
        Predicate<String> isPastYearS = noEx(false,(String t) -> {
            int i = Integer.parseInt(t);
            int max = Year.now().getValue();
            return i>0 && i<=max;
        },NumberFormatException.class)::apply;
        Predicate<String> isIntS = noEx(false,(String t) -> {
            int i = Integer.parseInt(t);
            return true;
        },NumberFormatException.class)::apply;

        // initialize fields
        fields.add(new TagField(titleF, TITLE));
        fields.add(new TagField(albumF, ALBUM));
        fields.add(new TagField(artistF, ARTIST));
        fields.add(new TagField(albumArtistF, ALBUM_ARTIST));
        fields.add(new TagField(composerF, COMPOSER));
        fields.add(new TagField(publisherF, PUBLISHER));
        fields.add(new TagField(trackF, TRACK, isIntS));
        fields.add(new TagField(tracksTotalF, TRACKS_TOTAL, isIntS));
        fields.add(new TagField(discF, DISC, isIntS));
        fields.add(new TagField(discsTotalF, DISCS_TOTAL, isIntS));
        fields.add(new TagField(genreF, GENRE));
        fields.add(new TagField(categoryF, CATEGORY));
        fields.add(new TagField(yearF, YEAR, isPastYearS));
        fields.add(new TagField(ratingF, RATING_RAW));
        fields.add(new TagField(ratingPF, RATING, IsBetween0And1));
        fields.add(new TagField(playcountF, PLAYCOUNT));
        fields.add(new TagField(commentF, COMMENT));
        fields.add(new TagField(moodF, MOOD));
        fields.add(new TagField(colorF, CUSTOM1, s -> APP.converter.general.isValid(Color.class, s)));
        fields.add(new TagField(custom1F, CUSTOM1));
        fields.add(new TagField(custom2F, CUSTOM2));
        fields.add(new TagField(custom3F, CUSTOM3));
        fields.add(new TagField(custom4F, CUSTOM4));
        fields.add(new TagField(custom5F, CUSTOM5));
        fields.add(new TagField(playedFirstF, FIRST_PLAYED));
        fields.add(new TagField(playedLastF, LAST_PLAYED));
        fields.add(new TagField(addedToLibF, ADDED_TO_LIBRARY));
        fields.add(new TagField(tagsF, TAGS));
        fields.add(new TagField(LyricsA, LYRICS));
        // associate color picker with custom1 field
        colorFPicker.disableProperty().bind(colorF.disabledProperty());
        colorFPicker.valueProperty().addListener((o,ov,nv) ->
            colorF.setText(nv==null || nv==EMPTY_COLOR ? "" : APP.converter.general.toS(nv))
        );

        // deselect text fields on click
        root.setOnMousePressed(e -> {
            root.requestFocus();
            fields.forEach(TagField::onLooseFocus);
        });

        // write on press enter
        root.setOnKeyPressed( e -> {
            if (e.getCode() == KeyCode.ENTER)
                write();
        });

        // drag & drop content
        root.setOnDragOver(handlerAccepting(e -> hasAudio(e.getDragboard())));
        root.setOnDragDropped(drag_dropped_handler);

        // remove cover on drag exit
        CoverV.getPane().setOnDragDetected( e -> CoverV.getPane().startFullDrag());
        root.addEventFilter(MOUSE_DRAG_RELEASED, e -> {
            if (e.getGestureSource().equals(CoverV.getPane()) && !containsMouse(CoverV.getPane(), e)) {
                addImg(null);
            }
        });

        // bind Rating values absolute<->relative when writing
        ratingF.setOnKeyReleased(e -> setPR());
        ratingF.setOnMousePressed(e -> setPR());
        ratingPF.setOnKeyReleased(e -> setR());
        ratingPF.setOnMousePressed(e -> setR());

        // show metadata list
        infoL.setOnMouseClicked(e -> showItemsPopup());
        infoL.setCursor(Cursor.HAND);

        // maintain add or set
        root.setOnKeyPressed(e -> { if (e.getCode()==CONTROL) add_not_set.set(true); });
        root.setOnKeyReleased(e -> { if (e.getCode()==CONTROL) add_not_set.set(false); });

        populate(null);
    }

    private void setR() {
        if (ratingPF.getText()==null || ratingPF.getText().isEmpty()) {
            ratingF.setPromptText("");
            ratingF.setText("");
            ratingF.setUserData(true);
            return;
        }
        try {
            ratingF.setText("");
            double rat = Double.parseDouble(ratingPF.getText());
            ratingF.setPromptText(String.valueOf(rat*255));
            ratingF.setText(String.valueOf(rat*255));
        } catch (NumberFormatException | NullPointerException e) {
            ratingF.setPromptText(ratingF.getId());
        }
    }
    private void setPR() {
        if (ratingF.getText()==null || ratingF.getText().isEmpty()) {
            ratingPF.setPromptText("");
            ratingPF.setText("");
            ratingPF.setUserData(true);
            return;
        }
        try {
            ratingPF.setText("");
            double rat = Double.parseDouble(ratingF.getText());
            ratingPF.setPromptText(String.valueOf(rat/255));
            ratingPF.setText(String.valueOf(rat/255));
        } catch (NumberFormatException | NullPointerException ex) {
            ratingPF.setPromptText(ratingPF.getId());
        }
    }

    public boolean isEmpty() {
        return allSongs.isEmpty();
    }

/******************************************************************************/

    BooleanProperty add_not_set = new SimpleBooleanProperty(false);

    /**
     * Reads metadata on provided items and fills the data for tagging.
     * If list contains Metadata, reading is skipped.
     * <p/>
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void read(List<? extends Song> items) {
        inputValue.setValue((List<Song>) items);
        if (items==null) return;

        // remove duplicates
        MapSet<URI,? extends Song> unique = new MapSet<>(Song::getUri, items);

        this.allSongs.setAll(unique);
        if (add_not_set.get()) add(unique, false); else set(unique);
    }

    private void set(Collection<? extends Song> set) {
        metadatas.clear();
        if (set.isEmpty()) {
            showProgressReading();
            populate(metadatas);
        }
        else add(set, true);
    }

    private void add(Collection<? extends Song> added, boolean readAll) {
        if (added.isEmpty()) return;

        // show progress, hide when populate ends - in populate()
        showProgressReading();
        // get added
        List<Metadata> ready = new ArrayList<>();
        List<Song> needs_read = new ArrayList<>();
        added.stream()
            .filter(i -> !i.isCorrupt() && i.isFileBased() && isAudioEditable(i.getFile()))
            .forEach(i -> {
                if (!readAll && i instanceof Metadata) ready.add((Metadata)i);
                else needs_read.add(i);
            });

        // read metadata for items

        var task = SongReadingKt.readTask(Song.Companion, needs_read);
        AppProgress.INSTANCE.start(task);
        SongReadingKt.setOnDone(task, (ok, result) -> {
            if (ok) {
                // remove duplicates
                MapSet<URI, Metadata> unique = new MapSet<>(Metadata::getUri);
                unique.addAll(metadatas);
                unique.addAll(ready);
                unique.addAll(result);

                metadatas.clear();
                metadatas.addAll(unique);
                populate(metadatas);
            }
        });
        runNew(task);
    }

    private void rem(Collection<? extends Song> rem) {
        if (rem.isEmpty()) return;
        // show progress, hide when populate ends - in populate()
        showProgressReading();
        metadatas.removeIf(m -> rem.stream().anyMatch(i -> i.same(m)));
        populate(metadatas);
    }

    /**
     * Writes edited data to tag and reloads the data and refreshes gui. The
     * result is new data from tag shown, allowing to confirm the changes really
     * happened.
     * <br/>
     * If no items are loaded then this method is a no-op.
     */
    public void write() {

        Validation v = validators.stream().filter(Validation::isInValid).findFirst().orElse(null);
        if (v!=null) {
            PopOver<?> p = new PopOver<>(new Text(v.text));
            p.show(ScreenPos.APP_CENTER);
            return;
        }

        // pre
        writing = true;
        showProgressWriting();

        // writing
        SongWritingKt.write(
            metadatas,
            consumer(w -> {
                // write to tag if field committable
                if ((boolean) titleF.getUserData())        w.setTitle(titleF.getText());
                if ((boolean) albumF.getUserData())        w.setAlbum(albumF.getText());
                if ((boolean) artistF.getUserData())       w.setArtist(artistF.getText());
                if ((boolean) albumArtistF.getUserData())  w.setAlbum_artist(albumArtistF.getText());
                if ((boolean) composerF.getUserData())     w.setComposer(composerF.getText());
                if ((boolean) publisherF.getUserData())    w.setPublisher(publisherF.getText());
                if ((boolean) trackF.getUserData())        w.setTrack(trackF.getText());
                if ((boolean) tracksTotalF.getUserData())  w.setTracksTotal(tracksTotalF.getText());
                if ((boolean) discF.getUserData())         w.setDisc(discF.getText());
                if ((boolean) discsTotalF.getUserData())   w.setDiscsTotal(discsTotalF.getText());
                if ((boolean) genreF.getUserData())        w.setGenre(genreF.getText());
                if ((boolean) categoryF.getUserData())     w.setCategory(categoryF.getText());
                if ((boolean) yearF.getUserData())         w.setYear(yearF.getText());
                if ((boolean) ratingF.getUserData())       w.setRatingPercent(ratingPF.getText());
                if ((boolean) playcountF.getUserData())    w.setPlaycount(playcountF.getText());
                if ((boolean) commentF.getUserData())      w.setComment(commentF.getText());
                if ((boolean) moodF.getUserData())         w.setMood(moodF.getText());
                colorFPicker.setUserData(true);
                if ((boolean) colorFPicker.getUserData()&&colorFPicker.getValue()!=EMPTY_COLOR)        w.setColor(colorFPicker.getValue());
                if ((boolean) colorF.getUserData())        w.setCustom1(colorF.getText());
                if ((boolean) tagsF.getUserData())         w.setTags(noDups(split(tagsF.getText().replace(", ",","),",")));
                // if ((boolean)playedFirstF.getUserData())  w.setPla(playedFirstF.getText());
                // if ((boolean)playedLastF.getUserData())   w.setCustom1(playedLastF.getText());
                // if ((boolean)addedToLibF.getUserData())   w.setCustom1(addedToLibF.getText());
                if ((boolean) LyricsA.getUserData())       w.setLyrics(LyricsA.getText());
                if ((boolean) CoverL.getUserData())        w.setCover(new_cover_file);
                if ((boolean) custom1F.getUserData())      w.setCustom2(custom1F.getText());
                if ((boolean) custom4F.getUserData())      w.setCustom4(custom4F.getText());
                // enabling the following these has no effect as they are not
                // editable and graphics are disabled, thus will always be empty
                // we comment it out to prevent needless checking
                // if ((boolean)custom2F.getUserData())      w.setCustom2(custom2F.getText());
                // if ((boolean)custom3F.getUserData())      w.setCustom3(custom3F.getText());
                // if ((boolean)custom5F.getUserData())      w.setCustom5(custom5F.getText());
            }),
            consumer(items -> {
                writing = false;
                populate(items);
                APP.services.use(Notifier.class, s -> s.showTextNotification("Tagging complete", Widgets.SONG_TAGGER));
            })
        );

    }

    /** use null to clear gui empty. */
    private void populate(List<Metadata> items) {
        // return if writing active
        if (writing) {
            hideProgress();
            return;
        }

        // totally empty
        boolean totally_empty = allSongs.isEmpty();
        content.setVisible(!totally_empty);
        placeholder.setVisible(totally_empty);
        if (totally_empty) return;

        // empty
        boolean empty = items == null || items.isEmpty();

        // empty previous content
        fields.forEach(TagField::emptyContent);
        CoverV.loadImage((Image)null);
        coverSuperContainer.setDisable(true);
        CoverL.setUserData(false);
        new_cover_file = null;


        // return if no new content
        if (empty) {
            // set info
            infoL.setText("No items loaded");
            infoL.setGraphic(null);
            hideProgress();
        } else {
            // set info
            infoL.setText(items.size() + " " + plural("item", items.size()) + " loaded.");
            infoL.setGraphic(createIcon(items.size()==1 ? FontAwesomeIcon.TAG : FontAwesomeIcon.TAGS));

            fields.forEach(f -> f.setEditable(true));
            coverSuperContainer.setDisable(false);

            runNew(() -> {
                // histogram init
                fields.forEach(TagField::histogramInit);
                    // handle cover separately
                int coverI = 0;
                String covDesS = "";
                Cover CovS = null;
                Set<AudioFileFormat> formats = new HashSet<>();

                // histogram do
                for (Metadata m: items) {
                    int i = items.indexOf(m);
                    fields.forEach(f -> f.histogramDo(m, i));
                    formats.add(m.getFormat());
                    // handle cover separately
                    Cover c = m.getCover(TAG);
                    if (i==0 && !c.isEmpty())
                        { coverI = 1; CovS = c; covDesS = c.getDescription(); }
                    if (coverI == 0 && i != 0 && !c.isEmpty())
                        { coverI = 2; CovS = c; covDesS = c.getDescription(); }
                    if (coverI == 1 && !(!(c.isEmpty()&&CovS.isEmpty())||c.equals(CovS)))
                        coverI = 2;
                }

                // histogram end - set fields prompt text
                final int c = coverI;
                String s = covDesS;
                Cover co = CovS;
                runLater(() -> {
                    fields.forEach(f -> f.histogramEnd(formats));

                    // handle cover separately
                    String coverInfoText = null;
                    if (c==0) coverInfoText = APP.getTextNoVal();
                    if (c==1) coverInfoText = s;
                    if (c==2) coverInfoText = APP.getTextManyVal();
                    CoverL.setText(coverInfoText);
                    CoverV.loadImage(c==1 ? co.getImage() : null);

                    // enable/disable fields
                    ratingF.setDisable(true);
                    custom2F.setDisable(true);
                    custom3F.setDisable(true);
                    custom5F.setDisable(true);
                    playedFirstF.setDisable(true);
                    playedLastF.setDisable(true);
                    addedToLibF.setDisable(true);

                    hideProgress();
                });
            });
        }
    }

    private void showProgressReading() {
        progressI.setProgress(INDETERMINATE_PROGRESS);
        progressI.setVisible(true);
        // make inaccessible during sensitive operation
        scrollContent.setMouseTransparent(true);
        // apply blur to content to hint inaccessibility
        // note: don't apply on root it would also blur the progress indicator!
        scrollContent.setEffect(new BoxBlur(1, 1, 1));
        scrollContent.setOpacity(0.8);
    }
    private void showProgressWriting() {
        progressI.setProgress(INDETERMINATE_PROGRESS);
        progressI.setVisible(true);
        scrollContent.setMouseTransparent(true);
        scrollContent.setEffect(new BoxBlur(1, 1, 1));
        scrollContent.setOpacity(0.8);
    }
    private void hideProgress() {
        progressI.setProgress(0);
        progressI.setVisible(false);
        scrollContent.setMouseTransparent(false);
        scrollContent.setEffect(null);
        scrollContent.setOpacity(1);
    }

    private void addImg(File f) {
        if (isEmpty()) return;

        new_cover_file = f!=null && isImageJaudiotagger(f) ? f : null;
        if (new_cover_file != null) {
            CoverV.loadImage(new_cover_file);
            CoverL.setText(getSuffix(new_cover_file) + " "+(int)CoverV.getImage().getWidth()+"/"+(int)CoverV.getImage().getHeight());
            CoverL.setUserData(true);
        } else {
            CoverV.loadImage((Image)null);
            CoverL.setText(APP.getTextNoVal());
            CoverL.setUserData(true);
        }
    }

    private final class TagField {
        private final TextInputControl c;
        private final Metadata.Field<?> f;
        public String histogramS;
        public int histogramI;

        public TagField(TextInputControl control, Metadata.Field<?> field) {
            this(control, field, null);
        }

        public TagField(TextInputControl control, Metadata.Field<?> field, Predicate<String> valCond) {
            c = control;
            f = field;


            c.getStyleClass().setAll(textFieldStyleClass);
            c.setMinSize(0, 0);
            c.setPrefSize(-1, -1);

            if (valCond!=null && c instanceof DecoratedTextField) {
                Validation v = new Validation(c, valCond , field + " field does not contain valid text.");
                validators.add(v);
                Icon l = new Icon(FontAwesomeIcon.EXCLAMATION_TRIANGLE, 11);
                DecoratedTextField cf = (DecoratedTextField)c;
                c.textProperty().addListener((o,ov,nv) -> {
                    boolean b = v.isValid();
                    l.setVisible(!b);
                    if (b) {
                        if (cf.right.getValue()==l) cf.right.setValue(new Region());
                    } else {
                        if (cf.right.getValue()!=l) cf.right.setValue(l);
                    }
                });
            }

            if (c instanceof TextField)
                ((TextField)c).alignmentProperty().bind(fieldTextAlignment);

            emptyContent();

            // show description
            c.addEventFilter(MOUSE_ENTERED, e -> fieldDesc.setText(field.description()));
            c.addEventFilter(MOUSE_EXITED, e -> fieldDesc.setText(""));

            // restrain input
            if (field.isTypeNumber())
                InputConstraints.numbersOnly(c, !field.isTypeNumberNoNegative(), field.isTypeFloatingNumber());

            // if not committable yet, enable committable & set text to tag value on click
            c.setOnMouseClicked(e -> {
                if (e.getButton()==PRIMARY) {
                    onMouseClicked();
                    e.consume();
                }
            });

            // disable committable if empty and backspace key pressed
            c.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                if (Util.equalsAny(e.getCode(),BACK_SPACE,ESCAPE)) {
                    onBackspacePressed(e); // requires event filter
                }
            });

            // autocompletion
            if (c instanceof TextField && f.isAutoCompletable()) {
               AutoCompletion.Companion.autoComplete(
                   (TextField) c,
                   text -> APP.db.getItemUniqueValuesByField().get(f).stream()
                          .filter(a -> startsWithNoCase(a, text))
                          .sorted()
                          .collect(toList())
               );
            }
        }
        void setEditable(boolean v) {
             c.setDisable(!v);
        }
        public void setSupported(Collection<AudioFileFormat> formats) {
            c.setDisable(formats.stream().anyMatch(f -> !isAudioEditable(f)));
        }
        void emptyContent() {
            c.setText("");              // set empty
            c.setPromptText("");        // set empty
            c.setUserData(false);       // set not committable
            c.setDisable(true);         // set disabled
            c.setId("");                // set empty prompt text backup
        }
        void onLooseFocus() {
            if (c.getText().equals(c.getId())) {
                c.setUserData(false);
                c.setText("");
                c.setPromptText(c.getId());
            }
        }
        void onMouseClicked() {
            if (!(boolean)c.getUserData()) {
                c.setUserData(true);
                c.setText("");
                c.setText("");
                c.setText(Util.equalsAny(c.getPromptText(), APP.getTextNoVal(), APP.getTextManyVal())
                                ? "" : c.getPromptText());
                c.setPromptText("");
                c.selectAll();
            }
        }
        void onBackspacePressed(KeyEvent e) {
            boolean setToInitial = c.getText().isEmpty();
            if (setToInitial) {
                c.setPromptText(c.getId());
                c.setUserData(false);
                root.requestFocus();
                if (c.equals(ratingF)) {  // link this action between related fields
                    ratingPF.setPromptText(ratingPF.getId());
                    ratingPF.setUserData(false);
                }
                if (c.equals(ratingPF)) {  // link this action between related fields
                    ratingF.setPromptText(ratingF.getId());
                    ratingF.setUserData(false);
                }
                e.consume();
            }
        }

        //-------------

        public void histogramInit() {
            // initializing checkers for multiple values
                //0 = no value in all items       write "no assigned value"
                //1 = same value in all items     write actual value
                //2 = multiple value in all items write "multiple value"
            histogramI = 0;
            histogramS = "";
        }
        @SuppressWarnings("unchecked")
        public void histogramDo(Metadata m, int i) {
            // check multiple values to determine general field values
            // the condition goes like this (for every field):
            // -- initializes at 0 = no value streak
            // -- if first value not empty -> break no value streak, start same value streak, otherwise continue
            //    (either all will be empty(0) or all will be same(1) - either way first value determines 0/1 streak)
            // -- if empty streak and (non first) value not empty -> conclusion = multiple values
            //    (empty and non empty values = multiple)
            // -- if same value streak and different value -> conclusion = multiple values
            // -- otherwise this ends as no value or same streak decided by 1st value
            boolean empty = f.isFieldEmpty(m);
            if (i==0 && !empty) {
                histogramI = 1;
                histogramS = String.valueOf(f.getOf(m));
            }
            if (histogramI == 0 && i != 0 && !empty) {
                histogramI = 2;
                histogramS = String.valueOf(f.getOf(m));
            }
            if (histogramI == 1 && !String.valueOf(f.getOf(m)).equals(histogramS)) {
                histogramI = 2;
            }
        }
        public void histogramEnd(Collection<AudioFileFormat> formats) {
            if (f==CUSTOM1) {
                Color c = orNull(APP.converter.general.ofS(Color.class,histogramS));
                colorFPicker.setValue(c==null ? EMPTY_COLOR : c);
                colorF.setText("");
            }

            if      (histogramI == 0)   c.setPromptText(APP.getTextNoVal());
            else if (histogramI == 1)   c.setPromptText(histogramS);
            else if (histogramI == 2)   c.setPromptText(APP.getTextManyVal());

            // remember prompt text
            c.setId(c.getPromptText());
            // disable if unsupported
            setSupported(formats);
        }
    }
    private static final class Validation {
        public final TextInputControl field;
        public final Predicate<String> condition;
        public final String text;

        public Validation(TextInputControl field, Predicate<String> condition, String text) {
            this.field = field;
            this.condition = condition;
            this.text = text;
        }

        public boolean isValid() {
            String s = field.getText();
            return s.isEmpty() || condition.test(s);
        }
        public boolean isInValid() {
            return !isValid();
        }
    }

    static <I, O> Ƒ1<I,O> noEx(O or, Function<I,O> f, Class<?>... ecs) {
        return i -> {
            try {
                return f.apply(i);
            } catch (Exception e) {
                for (Class<?> ec : ecs) if (ec.isAssignableFrom(e.getClass())) return or;
                throw e;
            }
        };
    }

    private final static List<String> textFieldStyleClass = new ArrayList<>(new TextField().getStyleClass());
    private final static PseudoClass corrupt = pseudoclass("corrupt");
    PopOver<?> helpP;

    private PopOver<?> showItemsPopup() {
        // build popup
        ListView<Song> list = new ListView<>();
                       // factory is set dynamically
                       list.setCellFactory(listView -> new ListCell<>() {
                            CheckIcon cb = new CheckIcon();
                            {
                                // allow user to de/activate item
                                cb.setOnMouseClicked(e -> {
                                    Song song = getItem();
                                    // avoid nulls & respect lock
                                    if (song!= null) {
                                        if (cb.selected.getValue()) add(singletonList(song),false);
                                        else rem(singletonList(song));
                                    }
                                });
                            }
                            @Override
                            public void updateItem(Song song, boolean empty) {
                                super.updateItem(song, empty);

                                if (empty || song==null) {
                                    setText(null);
                                    setGraphic(null);
                                } else {
                                    int index = getIndex() + 1;
                                    setText(index + "   " + song.getFilenameFull());
                                    boolean untaggable = song.isCorrupt() || !song.isFileBased() || !isAudioEditable(song.getFile());
                                    pseudoClassStateChanged(corrupt, untaggable);
                                    cb.selected.setValue(!untaggable);
                                    cb.setDisable(untaggable);

                                    if (getGraphic()==null) setGraphic(cb);
                                }
                            }
                        });
                       // list will automatically update now
                       list.setItems(allSongs);
                       // support same drag & drop as tagger
                       list.setOnDragOver(handlerAccepting(e -> hasAudio(e.getDragboard())));
                       list.setOnDragDropped(drag_dropped_handler);


        // build content controls
        Icon helpB = infoIcon(
              "List of all items in the tagger. Highlights untaggable items. Taggable items "
            + "can be unselected filtered.\n\n"
            + "Available actions:\n"
            + "    Drop items : Clears tagger and adds to tagger.\n"
            + "    Drop items + CTRL : Adds to tagger."
        ).size(11);
        // build popup
        PopOver<?> p = new PopOver<>(list);
                   p.title.set("Active Items");
                   p.getHeaderIcons().addAll(helpB);
                   p.showInCenterOf(infoL);
        return p;
    }

    private final EventHandler<DragEvent> drag_dropped_handler = e -> {
        if (hasAudio(e.getDragboard())) {
            List<Song> dropped = getAudio(e.getDragboard());
            //end drag transfer
            e.setDropCompleted(true);
            e.consume();
            // handle result - read data
            if (!dropped.isEmpty()) read(dropped);
        }
    };
}