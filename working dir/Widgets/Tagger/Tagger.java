package Tagger;

import java.io.File;
import java.net.URI;
import java.time.Year;
import java.util.*;
import java.util.function.Predicate;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.effect.BoxBlur;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;

import AudioPlayer.Item;
import AudioPlayer.services.Database.DB;
import AudioPlayer.services.notif.Notifier;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.MetadataReader;
import AudioPlayer.tagging.MetadataWriter;
import Configuration.IsConfig;
import Layout.widget.Widget;
import Layout.widget.controller.FXMLController;
import Layout.widget.controller.io.IsInput;
import Layout.widget.feature.SongReader;
import Layout.widget.feature.SongWriter;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import gui.itemnode.TextFieldItemNode.MoodItemNode;
import gui.itemnode.TextFieldItemNode.TextFieldItemNode;
import gui.objects.PopOver.PopOver;
import gui.objects.PopOver.PopOver.NodePos;
import gui.objects.icon.CheckIcon;
import gui.objects.icon.Icon;
import gui.objects.image.ThumbnailWithAdd;
import gui.objects.image.cover.Cover;
import gui.objects.textfield.DecoratedTextField;
import util.File.AudioFileFormat;
import util.File.AudioFileFormat.Use;
import util.File.ImageFileFormat;
import util.InputConstraints;
import util.access.V;
import util.async.future.Fut;
import util.collections.mapset.MapSet;
import util.graphics.Icons;
import util.graphics.drag.DragUtil;
import util.parsing.Parser;

import static AudioPlayer.tagging.Metadata.Field.*;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.EXCLAMATION_TRIANGLE;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.TAGS;
import static gui.objects.PopOver.PopOver.NodePos.DownCenter;
import static gui.objects.icon.Icon.createInfoIcon;
import static gui.objects.image.cover.Cover.CoverSource.TAG;
import static java.util.Collections.singletonList;
import static javafx.application.Platform.runLater;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.scene.control.ProgressIndicator.INDETERMINATE_PROGRESS;
import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseDragEvent.MOUSE_DRAG_RELEASED;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import static main.App.APP;
import static org.atteo.evo.inflector.English.plural;
import static util.File.FileUtil.EMPTY_COLOR;
import static util.async.Async.FX;
import static util.async.Async.runFX;
import static util.functional.Util.*;

/**
 * Tagger graphical component.
 * <p>
 * Can read and write metadata from/into files.
 * Currently supports files only. File types are limited to those supported
 * by the application.
 *
 * @author Plutonium_
 */
@Widget.Info(
    name = "Tagger",
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    howto = "Available actions:\n"
          + "    Drag cover away : Removes cover\n"
          + "    Drop image file : Adds cover\n"
          + "    Drop audio files : Sets files to tagger\n"
          + "    Drop audio files + CTRL:  Adds files to tagger\n"
          + "    Write : Saves the tags\n"
          + "    Loaded items label click : Opens editable source list of items",
    description = "Tag editor for audio files. Supports reading and writing to "
         + "tag. Taggable items can be unselected in selective list mode.",
    notes = "To do: improve tagging performance. Support for ogg and flac.",
    version = "0.8",
    year = "2015",
    group = Widget.Group.TAGGER)
public class Tagger extends FXMLController implements SongWriter, SongReader {

    @FXML AnchorPane root;
    @FXML VBox content;
    @FXML BorderPane header;
    @FXML AnchorPane scrollContent;
    @FXML GridPane grid;
    @FXML DecoratedTextField TitleF;
    @FXML DecoratedTextField AlbumF;
    @FXML DecoratedTextField ArtistF;
    @FXML DecoratedTextField AlbumArtistF;
    @FXML DecoratedTextField ComposerF;
    @FXML DecoratedTextField PublisherF;
    @FXML DecoratedTextField TrackF;
    @FXML DecoratedTextField TracksTotalF;
    @FXML DecoratedTextField DiscF;
    @FXML DecoratedTextField DiscsTotalF;
    @FXML DecoratedTextField GenreF;
    @FXML DecoratedTextField CategoryF;
    @FXML DecoratedTextField YearF;
    @FXML DecoratedTextField RatingF;
    @FXML DecoratedTextField RatingPF;
    @FXML DecoratedTextField PlaycountF;
    @FXML DecoratedTextField CommentF;
          MoodItemNode MoodF = new MoodItemNode();
    @FXML ColorPicker ColorFPicker;
    @FXML DecoratedTextField ColorF;
    @FXML DecoratedTextField Custom1F,Custom2F,Custom3F,Custom4F,Custom5F;
    @FXML DecoratedTextField PlayedFirstF,PlayedLastF,AddedToLibF,TagsF;
    @FXML TextArea LyricsA;
    @FXML BorderPane coverContainer;
    @FXML StackPane coverSuperContainer;
    @FXML Label CoverL;
    @FXML Label noCoverL;
    ThumbnailWithAdd CoverV;
    File new_cover_file = null;
    ProgressIndicator progressI;
    @FXML Label infoL;
    @FXML Label placeholder;
    @FXML StackPane fieldDescPane;
    Text fieldDesc;

    //global variables
    ObservableList<Item> allitems = FXCollections.observableArrayList();
    List<Metadata> metas = new ArrayList();   // currently in gui active
    final List<TagField> fields = new ArrayList<>();
    boolean writing = false;    // prevents external data chagnge during writing
    private final List<Validation> validators = new ArrayList();

    // properties
    @IsConfig(name = "Field text alignement", info = "Alignment of the text in fields.")
    public final V<Pos> field_text_alignment = new V<>(CENTER_LEFT, v->fields.forEach(f->f.setVerticalAlignment(v)));
    @IsConfig(name="Mood picker popup position", info = "Position of the mood picker pop up relative to the mood text field.")
    public final V<NodePos> popupPos = new V<>(DownCenter, MoodF::setPos);
    @IsConfig(name = "Allow change of playcount", info = "Change editability of playcount field. Generally to prevent change to non customary values.")
    public final V<Boolean> allow_playcount_change = new V<>(false, v -> {
        if(!isEmpty()) PlaycountF.setDisable(!v);
    });


    @Override
    public void init() {

        loadSkin("skin.css",root);

        CoverV = new ThumbnailWithAdd(FontAwesomeIcon.PLUS,"Add to Tag");
        CoverV.getPane().setPrefSize(200, 200);
        CoverV.onFileDropped = f -> f.use(this::addImg,FX);
        CoverV.onHighlight = v -> noCoverL.setVisible(!v);
        coverContainer.setCenter(CoverV.getPane());

        progressI = new gui.objects.spinner.Spinner();
        progressI.setVisible(false);
        header.setRight(progressI);

        fieldDesc = new gui.objects.Text();
        fieldDescPane.getChildren().add(fieldDesc);
        StackPane.setAlignment(fieldDesc, Pos.CENTER);

        // add specialized mood text field
        grid.add(MoodF, 1, 14, 2, 1);

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
        fields.add(new TagField(TitleF,TITLE));
        fields.add(new TagField(AlbumF,ALBUM));
        fields.add(new TagField(ArtistF,ARTIST));
        fields.add(new TagField(AlbumArtistF,ALBUM_ARTIST));
        fields.add(new TagField(ComposerF,COMPOSER));
        fields.add(new TagField(PublisherF,PUBLISHER));
        fields.add(new TagField(TrackF,TRACK,isIntS));
        fields.add(new TagField(TracksTotalF,TRACKS_TOTAL,isIntS));
        fields.add(new TagField(DiscF,DISC,isIntS));
        fields.add(new TagField(DiscsTotalF,DISCS_TOTAL,isIntS));
        fields.add(new TagField(GenreF,GENRE));
        fields.add(new TagField(CategoryF,CATEGORY));
        fields.add(new TagField(YearF,YEAR,isPastYearS));
        fields.add(new TagField(RatingF,RATING_RAW));
        fields.add(new TagField(RatingPF,RATING,IsBetween0And1));
        fields.add(new TagField(PlaycountF,PLAYCOUNT));
        fields.add(new TagField(CommentF,Metadata.Field.COMMENT));
        fields.add(new TagField(MoodF,MOOD));
        fields.add(new TagField(ColorF,CUSTOM1,Parser.isParsable(Color.class)));
        fields.add(new TagField(Custom1F,CUSTOM1));
        fields.add(new TagField(Custom2F,CUSTOM2));
        fields.add(new TagField(Custom3F,CUSTOM3));
        fields.add(new TagField(Custom4F,CUSTOM4));
        fields.add(new TagField(Custom5F,CUSTOM5));
        fields.add(new TagField(PlayedFirstF,FIRST_PLAYED));
        fields.add(new TagField(PlayedLastF,LAST_PLAYED));
        fields.add(new TagField(AddedToLibF,ADDED_TO_LIBRARY));
        fields.add(new TagField(TagsF,Metadata.Field.TAGS));
        fields.add(new TagField(LyricsA,LYRICS));
        // associate color picker with custom1 field
        ColorFPicker.disableProperty().bind(ColorF.disabledProperty());
        ColorFPicker.valueProperty().addListener((o,ov,nv) ->
            ColorF.setText(nv==null || nv==EMPTY_COLOR ? "" : Parser.toS(nv))
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
        root.setOnDragOver(DragUtil.audioDragAccepthandler);
        root.setOnDragDropped(drag_dropped_handler);

        // remove cover on drag exit
        CoverV.getPane().setOnDragDetected( e -> CoverV.getPane().startFullDrag());
        root.addEventFilter(MOUSE_DRAG_RELEASED, e-> {
            Point2D click = CoverV.getPane().sceneToLocal(e.getSceneX(),e.getSceneY());
            // only if drag starts on the cover and ends outside of it
            if(e.getGestureSource().equals(CoverV.getPane()) && !CoverV.getPane().contains(click)) {
                addImg(null); // removes image
            }
        });

        // bind Rating values absolute<->relative when writing
        RatingF.setOnKeyReleased(e -> setPR());
        RatingF.setOnMousePressed(e -> setPR());
        RatingPF.setOnKeyReleased(e -> setR());
        RatingPF.setOnMousePressed(e -> setR());

        // show metadata list
        infoL.setOnMouseClicked(e -> showItemsPopup());
        infoL.setCursor(Cursor.HAND);

        // maintain add or set
        root.setOnKeyPressed(e -> { if(e.getCode()==CONTROL) add_not_set.set(true); });
        root.setOnKeyReleased(e -> { if(e.getCode()==CONTROL) add_not_set.set(false); });

        populate(null);
    }


    private void setR() {
        if (RatingPF.getText()==null || RatingPF.getText().isEmpty()) {
            RatingF.setPromptText("");
            RatingF.setText("");
            RatingF.setUserData(true);
            return;
        }
        try {
            RatingF.setText("");
            double rat = Double.parseDouble(RatingPF.getText());
            RatingF.setPromptText(String.valueOf(rat*255));
            RatingF.setText(String.valueOf(rat*255));
        } catch (NumberFormatException | NullPointerException e) {
            RatingF.setPromptText(RatingF.getId());
        }
    }
    private void setPR() {
        if (RatingF.getText()==null || RatingF.getText().isEmpty()) {
            RatingPF.setPromptText("");
            RatingPF.setText("");
            RatingPF.setUserData(true);
            return;
        }
        try {
            RatingPF.setText("");
            double rat = Double.parseDouble(RatingF.getText());
            RatingPF.setPromptText(String.valueOf(rat/255));
            RatingPF.setText(String.valueOf(rat/255));
        } catch (NumberFormatException | NullPointerException ex) {
            RatingPF.setPromptText(RatingPF.getId());
        }
    }

    @Override
    public void refresh() {
        field_text_alignment.applyValue();
        popupPos.applyValue();
        allow_playcount_change.applyValue();
    }

    /**
     * This widget is empty if it has no data.
     * @return
     */
    @Override
    public boolean isEmpty() {
        return allitems.isEmpty();
    }


/******************************************************************************/
    BooleanProperty add_not_set = new SimpleBooleanProperty(false);

    /**
     * Reads metadata on provided items and fills the data for tagging.
     * If list contains Metadata, reading is skipped.
     * <p>
     * {@inheritDoc}
     */
    @Override
    @IsInput("Edit")
    public void read(List<? extends Item> items) {
        if(items==null) return;

        // remove duplicates
        MapSet<URI,? extends Item> unique = new MapSet<>(Item::getURI, items);

        this.allitems.setAll(unique);
        if(add_not_set.get()) add(unique, false); else set(unique);
    }

    private void set(Collection<? extends Item> set) {
        metas.clear();
        if(set.isEmpty()) {
            showProgressReading();
            populate(metas);
        }
        else add(set, true);
    }
    private void add(Collection<? extends Item> added, boolean readAll) {
        if(added.isEmpty()) return;

        // show progress, hide when populate ends - in populate()
        showProgressReading();
        // get added
        List<Metadata> ready = new ArrayList();
        List<Item> needs_read = new ArrayList();
        added.stream()
            // filter out untaggable
            .filter(i -> !i.isCorrupt(Use.DB) && i.isFileBased())
            .forEach(i -> {
                if(!readAll && i instanceof Metadata) ready.add((Metadata)i);
                else needs_read.add(i);
            });

        // read metadata for items
        MetadataReader.readMetadata(needs_read, (ok,result) -> {
            if(ok) {
                // remove duplicates
                MapSet<URI, Metadata> unique = new MapSet<>(Metadata::getURI);
                                      unique.addAll(metas);
                                      unique.addAll(ready);
                                      unique.addAll(result);

                metas.clear();
                metas.addAll(unique);
                populate(metas);
            }
        });
    }
    private void rem(Collection<? extends Item> rem) {
        if(rem.isEmpty()) return;
        // show progress, hide when populate ends - in populate()
        showProgressReading();
        metas.removeIf( m -> rem.stream().anyMatch(i -> i.same(m)));
        populate(metas);
    }

    /**
     * Writes edited data to tag and reloads the data and refreshes gui. The
     * result is new data from tag shown, allowing to confirm the changes really
     * happened.
     * If no items are loaded then this method is a no-op.
     */
    @FXML
    public void write() {

        Validation v = validators.stream().filter(Validation::isInValid).findFirst().orElse(null);
        if(v!=null) {
            PopOver p = new PopOver(new Text(v.text));
            p.show(PopOver.ScreenPos.App_Center);
            return;
        }

        // pre
        writing = true;
        showProgressWriting();

        // writing
        MetadataWriter.use(metas, w -> {
            // write to tag if field commitable
            if ((boolean)TitleF.getUserData())        w.setTitle(TitleF.getText());
            if ((boolean)AlbumF.getUserData())        w.setAlbum(AlbumF.getText());
            if ((boolean)ArtistF.getUserData())       w.setArtist(ArtistF.getText());
            if ((boolean)AlbumArtistF.getUserData())  w.setAlbum_artist(AlbumArtistF.getText());
            if ((boolean)ComposerF.getUserData())     w.setComposer(ComposerF.getText());
            if ((boolean)PublisherF.getUserData())    w.setPublisher(PublisherF.getText());
            if ((boolean)TrackF.getUserData())        w.setTrack(TrackF.getText());
            if ((boolean)TracksTotalF.getUserData())  w.setTracks_total(TracksTotalF.getText());
            if ((boolean)DiscF.getUserData())         w.setDisc(DiscF.getText());
            if ((boolean)DiscsTotalF.getUserData())   w.setDiscs_total(DiscF.getText());
            if ((boolean)GenreF.getUserData())        w.setGenre(GenreF.getText());
            if ((boolean)CategoryF.getUserData())     w.setCategory(CategoryF.getText());
            if ((boolean)YearF.getUserData())         w.setYear(YearF.getText());
            if ((boolean)RatingF.getUserData())       w.setRatingPercent(RatingPF.getText());
            if ((boolean)PlaycountF.getUserData())    w.setPlaycount(PlaycountF.getText());
            if ((boolean)CommentF.getUserData())      w.setComment(CommentF.getText());
            if ((boolean)MoodF.getUserData())         w.setMood(MoodF.getText());
            ColorFPicker.setUserData(true);
            if ((boolean)ColorFPicker.getUserData()&&ColorFPicker.getValue()!=EMPTY_COLOR)        w.setColor(ColorFPicker.getValue());
            if ((boolean)ColorF.getUserData())        w.setCustom1(ColorF.getText());
            if ((boolean)TagsF.getUserData())         w.setTags(noDups(split(TagsF.getText().replace(", ",","),",")));
//            if ((boolean)PlayedFirstF.getUserData())  w.setPla(PlayedFirstF.getText());
//            if ((boolean)PlayedLastF.getUserData())   w.setCustom1(PlayedLastF.getText());
//            if ((boolean)AddedToLibF.getUserData())   w.setCustom1(AddedToLibF.getText());
            if ((boolean)LyricsA.getUserData())       w.setLyrics(LyricsA.getText());
            if ((boolean)CoverL.getUserData())        w.setCover(new_cover_file);
            if ((boolean)Custom1F.getUserData())      w.setCustom2(Custom1F.getText());
            if ((boolean)Custom4F.getUserData())      w.setCustom4(Custom4F.getText());
            // enabling the following these has no effect as they are not
            // editable and graphics are disabled, thus will always be empty
            // we comment it out to prevent needless checking
            // if ((boolean)Custom2F.getUserData())      w.setCustom2(Custom2F.getText());
            // if ((boolean)Custom3F.getUserData())      w.setCustom3(Custom3F.getText());
            // if ((boolean)Custom5F.getUserData())      w.setCustom5(Custom5F.getText());
        }, items -> {
            // post (make sure its on FX)
            runFX(() -> {
                writing = false;
                populate(items);
                APP.use(Notifier.class, s -> s.showTextNotification("Tagging complete", "Tagger"));
            });
        });

    }

/******************************************************************************/

    /** use null to clear gui empty. */
    private void populate(List<Metadata> items) {
        // return if writing active
        if (writing) {
            hideProgress(); return; }

        // totally empty
        boolean totally_empty = allitems.isEmpty();
        content.setVisible(!totally_empty);
        placeholder.setVisible(totally_empty);
        if(totally_empty) return;

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
            return;
        } else {
            // set info
            infoL.setText(items.size() + " " + plural("item", items.size()) + " loaded.");
            infoL.setGraphic(Icons.createIcon(items.size()==1 ? FontAwesomeIcon.TAG : TAGS));

            fields.forEach(f -> f.setEditable(true));
            coverSuperContainer.setDisable(false);

            Fut.fut()
               .then(() -> {
                    // histogram init
                    fields.forEach(TagField::histogramInit);
                        // handle cover separately
                    int coverI = 0;
                    String covDesS = "";
                    Cover CovS = null;
                    Set<AudioFileFormat> formats = new HashSet();

                    // histogram do
                    for(Metadata m: items) {
                        int i = items.indexOf(m);
                        fields.forEach(f -> f.histogramDo(m, i));
                        formats.add(m.getFormat());
                        // handle cover separately
                        Cover c = m.getCover(TAG);
                        if (i==0 && !c.isEmpty())
                            { coverI = 1; CovS = c; covDesS = c.getDestription(); }
                        if (coverI == 0 && i != 0 && !c.isEmpty())
                            { coverI = 2; CovS = c; covDesS = c.getDestription(); }
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
                            // set image info
                             if (c == 0)    CoverL.setText(APP.TAG_NO_VALUE);
                        else if (c == 1)    CoverL.setText(s);
                        else if (c == 2)    CoverL.setText(APP.TAG_MULTIPLE_VALUE);
                            // set image
                        if (c == 1)         CoverV.loadImage(co.getImage());
                        else                CoverV.loadImage((Image)null);

                        // enable/disable fields
                        if(!allow_playcount_change.getValue()) PlaycountF.setDisable(true);
                        RatingF.setDisable(true);
                        Custom2F.setDisable(true);
                        Custom3F.setDisable(true);
                        Custom5F.setDisable(true);
                        PlayedFirstF.setDisable(true);
                        PlayedLastF.setDisable(true);
                        AddedToLibF.setDisable(true);

                        hideProgress();
                    });
                })
            .run();
        }
    }

    private void showProgressReading() {
        progressI.setProgress(INDETERMINATE_PROGRESS);
        progressI.setVisible(true);
        // make inaccessible during sensitive operation
        scrollContent.setMouseTransparent(true);
        // apply blur to content to hint inaccessibility
        // note: dont apply on root it would also blur the progres indicator!
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

        new_cover_file = f!=null && ImageFileFormat.isSupported(f) ? f : null;
        if (new_cover_file != null) {
            CoverV.loadImage(new_cover_file);
            CoverL.setText(ImageFileFormat.of(new_cover_file.toURI()) + " "
                    +(int)CoverV.getImage().getWidth()+"/"+(int)CoverV.getImage().getHeight());
            CoverL.setUserData(true);
        } else {
            CoverV.loadImage((Image)null);
            CoverL.setText(APP.TAG_NO_VALUE);
            CoverL.setUserData(true);
        }
    }



/******************************************************************************/

    private final class TagField {
        private final TextInputControl c;
        private final Metadata.Field f;
        public String histogramS;
        public int histogramI;

        public TagField(TextInputControl control, Metadata.Field field) {
            this(control, field, null);
        }

        public TagField(TextInputControl control, Metadata.Field field, Predicate<String> valCond) {
            c = control;
            f = field;

            c.getStyleClass().setAll(TextFieldItemNode.STYLECLASS());
            c.setMinSize(0, 0);
            c.setPrefSize(-1, -1);

            if(valCond!=null && c instanceof CustomTextField) {
                Validation v = new Validation(c, valCond , field + " field doeas not contain valid text.");
                validators.add(v);
                Icon l = new Icon(EXCLAMATION_TRIANGLE, 11);
                CustomTextField cf = (CustomTextField)c;
                c.textProperty().addListener((o,ov,nv) -> {
                    boolean b = v.isValid();
                    l.setVisible(!b);
                    if(b) if(cf.getRight()==l) cf.setRight(new Region());
                    else if(cf.getRight()!=l) cf.setRight(l);
                });
            }

            emptyContent();

            // show description
            c.addEventFilter(MOUSE_ENTERED,e -> fieldDesc.setText(field.description()));
            c.addEventFilter(MOUSE_EXITED,e -> fieldDesc.setText(""));

            // restrain input
            if(field.isTypeNumber())
                InputConstraints.numbersOnly(c, !field.isTypeNumberNonegative(), field.isTypeFloatingNumber());

            // if not commitable yet, enable commitable & set text to tag value on click
            c.setOnMouseClicked(e -> {
                if(e.getButton()==PRIMARY)
                    OnMouseClicked();
            });

            // disable commitable if empty and backspace key pressed
            c.setOnKeyPressed( e -> {
                if (isIn(e.getCode(),BACK_SPACE,ESCAPE))
                    OnBackspacePressed();
            });

            // autocompletion
            if(c instanceof TextField && !isIn(f, TITLE,RATING_RAW,COMMENT,LYRICS,COLOR)) {
               String fn = f.name();
               TextFields.bindAutoCompletion((TextField)c, p -> filter(DB.string_pool.getStrings(fn),t -> t.startsWith(p.getUserText())));
            }
        }
        void setEditable(boolean v) {
             c.setDisable(!v);
        }
        public void setSupported(Collection<AudioFileFormat> formats) {
            boolean v = formats.stream().map(frm->frm.isTagWriteSupported(f))
                               .reduce(Boolean::logicalAnd).orElse(false);
            c.setDisable(!v);
        }
        void emptyContent() {
            c.setText("");              // set empty
            c.setPromptText("");        // set empty
            c.setUserData(false);       // set uncommitable
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
        void OnMouseClicked() {
            if (!(boolean)c.getUserData()) {
                c.setUserData(true);
                c.setText("");
                c.setText("");
                c.setText(isIn(c.getPromptText(), APP.TAG_NO_VALUE, APP.TAG_MULTIPLE_VALUE)
                                ? "" : c.getPromptText());
                c.setPromptText("");
                c.selectAll();
            }
        }
        void OnBackspacePressed() {
            if (c.getText().isEmpty()) {
                c.setPromptText(c.getId());
                c.setUserData(false);
                root.requestFocus();
                if (c.equals(RatingF)) {  // link this action between related fields
                    RatingPF.setPromptText(RatingPF.getId());
                    RatingPF.setUserData(false);
                }
                if (c.equals(RatingPF)) {  // link this action between related fields
                    RatingF.setPromptText(RatingF.getId());
                    RatingF.setUserData(false);
                }
            }
        }
        void setVerticalAlignment(Pos alignment) {
            if (c instanceof TextField)
                ((TextField)c).setAlignment(alignment);
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
                histogramS = String.valueOf(m.getField(f));
            }
            if (histogramI == 0 && i != 0 && !empty) {
                histogramI = 2;
                histogramS = String.valueOf(m.getField(f));
            }
            if (histogramI == 1 && !String.valueOf(m.getField(f)).equals(histogramS)) {
                histogramI = 2;
            }
        }
        public void histogramEnd(Collection<AudioFileFormat> formats) {
            if(f==CUSTOM1) {
                Color c = Parser.fromS(Color.class,histogramS);
                ColorFPicker.setValue(c==null ? EMPTY_COLOR : c);
                ColorF.setText("");
            }

            if      (histogramI == 0)   c.setPromptText(APP.TAG_NO_VALUE);
            else if (histogramI == 1)   c.setPromptText(histogramS);
            else if (histogramI == 2)   c.setPromptText(APP.TAG_MULTIPLE_VALUE);

            // remember prompt text
            c.setId(c.getPromptText());
            // disable if unsuported
            setSupported(formats);
        }
    }
    private final class Validation {
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

/**************************** active items popup ******************************/

    private static PseudoClass corrupt = PseudoClass.getPseudoClass("corrupt");
    PopOver helpP;

    private PopOver showItemsPopup() {
        // build popup
        ListView<Item> list = new ListView();
                       // factory is set dynamically
                       list.setCellFactory(listView -> new ListCell<Item>() {
                            CheckIcon cb = new CheckIcon();
                            {
                                // allow user to de/activate item
                                cb.setOnMouseClicked(e -> {
                                    Item item = getItem();
                                    // avoid nulls & respect lock
                                    if(item != null) {
                                        if(cb.selected.getValue()) add(singletonList(item),false);
                                        else rem(singletonList(item));
                                    }
                                });
                            }
                            @Override
                            protected void updateItem(Item item, boolean empty) {
                                super.updateItem(item, empty);
                                if(!empty) {
                                    int index = getIndex() + 1;
                                    setText(index + "   " + item.getFilenameFull());
                                    // handle untaggable
                                    boolean untaggable = item.isCorrupt(Use.DB) || !item.isFileBased();
                                    pseudoClassStateChanged(corrupt, untaggable);
                                    cb.selected.setValue(!untaggable);
                                    cb.setDisable(untaggable);

                                    if (getGraphic()==null) setGraphic(cb);
                                } else {
                                    setText(null);
                                    setGraphic(null);
                                }
                            }
                        });
                       // list will atomatically update now
                       list.setItems(allitems);
                       // support same drag & drop as tagger
                       list.setOnDragOver(DragUtil.audioDragAccepthandler);
                       list.setOnDragDropped(drag_dropped_handler);


        // build content controls
        Icon helpB = createInfoIcon(
              "List of all items in the tagger. Highlights untaggable items. Taggable items "
            + "can be unselected filtered.\n\n"
            + "Available actions:\n"
            + "    Drop items : Clears tagget and adds to tagger.\n"
            + "    Drop items + CTRL : Adds to tagger."
        ).size(11);
        // build popup
        PopOver p = new PopOver(list);
                p.title.set("Active Items");
                p.getHeaderIcons().addAll(helpB);
                p.show(infoL);
        return p;
    }

    private final EventHandler<DragEvent> drag_dropped_handler = e -> {
        if (DragUtil.hasAudio(e)) {
            List<Item> dropped = DragUtil.getAudioItems(e);
            //end drag transfer
            e.setDropCompleted(true);
            e.consume();
            // handle result - read data
            if (!dropped.isEmpty()) read(dropped);
        }
    };
}