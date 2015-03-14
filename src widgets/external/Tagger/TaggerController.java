
package Tagger;

import AudioPlayer.Player;
import AudioPlayer.playlist.Item;
import AudioPlayer.services.Notifier.Notifier;
import AudioPlayer.tagging.Cover.Cover;
import static AudioPlayer.tagging.Cover.Cover.CoverSource.TAG;
import AudioPlayer.tagging.Metadata;
import static AudioPlayer.tagging.Metadata.Field.*;
import AudioPlayer.tagging.MetadataReader;
import AudioPlayer.tagging.MetadataWriter;
import Configuration.IsConfig;
import GUI.DragUtil;
import GUI.ItemNode.ItemTextFields.MoodTextField;
import GUI.objects.CheckIcon;
import GUI.objects.GraphicalTextField;
import GUI.objects.Icon;
import GUI.objects.PopOver.PopOver;
import GUI.objects.PopOver.PopOver.NodeCentricPos;
import static GUI.objects.PopOver.PopOver.NodeCentricPos.DownCenter;
import GUI.objects.Thumbnail;
import Layout.Widgets.FXMLController;
import Layout.Widgets.Features.TaggingFeature;
import Layout.Widgets.Widget;
import PseudoObjects.ReadMode;
import static PseudoObjects.ReadMode.*;
import static PseudoObjects.ReadMode.CUSTOM;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.EXCLAMATION_TRIANGLE;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.TAGS;
import java.io.File;
import java.net.URI;
import java.util.*;
import static java.util.Collections.singletonList;
import java.util.function.Predicate;
import static javafx.application.Platform.runLater;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import static javafx.geometry.Pos.CENTER_LEFT;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import static javafx.scene.control.ProgressIndicator.INDETERMINATE_PROGRESS;
import javafx.scene.effect.BoxBlur;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseDragEvent.MOUSE_DRAG_RELEASED;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Callback;
import main.App;
import static main.App.TAG_MULTIPLE_VALUE;
import static main.App.TAG_NO_VALUE;
import static org.atteo.evo.inflector.English.plural;
import org.controlsfx.control.textfield.CustomTextField;
import org.reactfx.Subscription;
import util.File.AudioFileFormat;
import util.File.AudioFileFormat.Use;
import util.File.Enviroment;
import static util.File.FileUtil.EMPTY_COLOR;
import util.File.ImageFileFormat;
import util.InputConstraints;
import util.access.Accessor;
import util.async.Async;
import util.collections.MapSet;
import util.dev.Log;
import static util.functional.Util.isIn;
import static util.functional.impl.Validator.*;
import util.graphics.Icons;
import util.parsing.impl.ColorParser;

/**
 * TaggerController graphical component.
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
    howto = "Available actions:\n" +
            "    Drag cover away : Removes cover\n" +
            "    Drop image file : Adds cover\n" +
            "    Drop audio files : Adds files to tagger\n" +
            "    Write : Saves the tags\n" +
            "    Loaded items label click : Opens editable source list of items",
    description = "Tag editor for audio files. Supports reading and writing to tag." +
                  "Taggable items can be unselected in selective list mode.",
    notes = "To do: improve tagging performance. Support for ogg and flac.",
    version = "0.8",
    year = "2015",
    group = Widget.Group.TAGGER)
public class TaggerController extends FXMLController implements TaggingFeature {
    
    @FXML AnchorPane entireArea;
    @FXML AnchorPane scrollContent;
    @FXML GridPane grid;
    @FXML CustomTextField TitleF;
    @FXML CustomTextField AlbumF;
    @FXML CustomTextField ArtistF;
    @FXML CustomTextField AlbumArtistF;
    @FXML CustomTextField ComposerF;
    @FXML CustomTextField PublisherF;
    @FXML CustomTextField TrackF;
    @FXML CustomTextField TracksTotalF;
    @FXML CustomTextField DiscF;
    @FXML CustomTextField DiscsTotalF;
    @FXML CustomTextField GenreF;
    @FXML CustomTextField CategoryF;
    @FXML CustomTextField YearF;
    @FXML CustomTextField RatingF;
    @FXML CustomTextField RatingPF;
    @FXML CustomTextField PlaycountF;
    @FXML CustomTextField CommentF;
          MoodTextField MoodF = new MoodTextField();
    @FXML ColorPicker ColorF;
    @FXML CustomTextField Custom1F;
    @FXML CustomTextField Custom2F;
    @FXML CustomTextField Custom3F;
    @FXML CustomTextField Custom4F;
    @FXML CustomTextField Custom5F;
    @FXML TextArea LyricsA;
    @FXML BorderPane coverContainer;
    @FXML StackPane coverSuperContainer;
    @FXML Label CoverL;
    Thumbnail CoverV;
    File new_cover_file = null;
    @FXML StackPane progressPane;
    @FXML ProgressIndicator progressI;
    @FXML Label infoL;
    
    
    //global variables
    ObservableList<Item> allitems = FXCollections.observableArrayList();
    List<Metadata> metas = new ArrayList();   // currently in gui active
    Task<List<Metadata>> loader;
    final List<TagField> fields = new ArrayList<>();
    boolean writing = false;    // prevents external data chagnge during writing
    private final List<Validation> validators = new ArrayList();
        
    // dependencies
    private Subscription d1;
    
    // properties
    @IsConfig(name = "Field text alignement", info = "Alignment of the text in fields.")
    public final Accessor<Pos> field_text_alignment = new Accessor<>(CENTER_LEFT, v->fields.forEach(f->f.setVerticalAlignment(v)));
    @IsConfig(name="Mood picker popup position", info = "Position of the mood picker pop up relative to the mood text field.")
    public final Accessor<NodeCentricPos> popupPos = new Accessor<>(DownCenter, MoodF::setPos);
    @IsConfig(name = "Read Mode", info = "Source of data for the widget.")
    public final Accessor<ReadMode> readMode = new Accessor<>(CUSTOM, r -> {
        d1 = Player.subscribe(r, d1, m -> {
            allitems.setAll(m);
            populate(singletonList(m));
        });
    });
    @IsConfig(name = "Allow change of playcount", info = "Change editability of playcount field. Generally to prevent change to non customary values.")
    public final Accessor<Boolean> allow_playcount_change = new Accessor<>(false, v -> {
        if(!isEmpty()) PlaycountF.setDisable(!v);
    });

    @IsConfig(name = "Read mode change on drag", info = "Change read mode to CUSTOM when data are arbitrary added to widget.")
    public Boolean changeReadModeOnTransfer = false;

    
    
    @Override
    public void init() {
        loadSkin("skin.css",entireArea);
        
        CoverV = new Thumbnail(200);
        CoverV.setDragImage(false); // we have our own implementation below
        CoverV.getPane().getStyleClass().add("tager-thumbnail");
        coverContainer.setCenter(CoverV.getPane());
        // add specialized mood text field
        grid.add(MoodF, 1, 14, 2, 1);
        
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
        fields.add(new TagField(Custom1F,CUSTOM1));
        fields.add(new TagField(Custom2F,CUSTOM2));
        fields.add(new TagField(Custom3F,CUSTOM3));
        fields.add(new TagField(Custom4F,CUSTOM4));
        fields.add(new TagField(Custom5F,CUSTOM5));
        fields.add(new TagField(LyricsA,LYRICS));
            // associate color picker with custom1 field
        Custom1F.setEditable(false);
        ColorF.disableProperty().bind(Custom1F.disabledProperty());
        ColorF.valueProperty().addListener((o,ov,nv) -> Custom1F.setText(new ColorParser().toS(nv)));
        

        // deselect text fields on click
        entireArea.setOnMousePressed( e -> {
            entireArea.requestFocus();
            fields.forEach(TagField::onLooseFocus);
        });
        
        // write on press enter
        entireArea.setOnKeyPressed( e -> {
            if (e.getCode() == KeyCode.ENTER)
                write();
        });
        
        // drag & drop content
        entireArea.setOnDragOver(DragUtil.audioDragAccepthandler);
        entireArea.setOnDragDropped(drag_dropped_handler);
        
        // add cover on click
        coverContainer.setOnMouseClicked( e -> {
            if (e.getButton()!=PRIMARY || metas.isEmpty()) return;
            
            File initial_dir = metas.stream()
                    .filter(Item::isFileBased)
                    .findFirst().map(Item::getLocation)
                    .orElse(new File(""));
            File f = Enviroment.chooseFile("Select image to add to tag",
                    false, initial_dir, entireArea.getScene().getWindow());
            if (f!= null) addImg(f);
        });
        
        // add cover on drag & drop image file
        CoverV.getPane().setOnDragOver( t -> {
            Dragboard d = t.getDragboard();
            // accept if as at least one image file, note: we dont want url
            // we are only interested in files - only those can be written in tag
            if (d.hasFiles() && d.getFiles().stream().anyMatch(ImageFileFormat::isSupported))
                t.acceptTransferModes(TransferMode.ANY);
        });
        CoverV.getPane().setOnDragDropped( t -> {
            Dragboard d = t.getDragboard();
            // check again if the image file is in the dragboard. If it isnt
            // do not consume and let the event propagate bellow. In case there
            // are playable items/files they will be captured by root's drag
            // handler
            // removing the condition would consume the event and stop propagation
            if (d.hasFiles() && d.getFiles().stream().anyMatch(ImageFileFormat::isSupported)) {
                d.getFiles().stream().filter(ImageFileFormat::isSupported)
                            .findAny().ifPresent(this::addImg);
                //end drag transfer
                t.setDropCompleted(true);
                t.consume();
            }
        });

        // remove cover on drag exit
        CoverV.getPane().setOnDragDetected( e -> CoverV.getPane().startFullDrag());
        entireArea.addEventFilter(MOUSE_DRAG_RELEASED, e-> {
            Point2D click = CoverV.getPane().sceneToLocal(e.getSceneX(),e.getSceneY());
            // only if drag starts on the cover and ends outside of it
            if(e.getGestureSource().equals(CoverV.getPane()) && !CoverV.getPane().contains(click)) {
                addImg(null); // removes image
            }
        });
        
        // cover add icon
        Text icon = Icons.createIcon(FontAwesomeIconName.PLUS, 60);
             icon.setMouseTransparent(true);
        coverSuperContainer.getChildren().add(icon);
             icon.setOpacity(0);
        coverContainer.addEventHandler(MOUSE_EXITED, e-> icon.setOpacity(0));
        coverContainer.addEventHandler(MOUSE_ENTERED, e-> icon.setOpacity(isEmpty() ? 0 : 1));
        
        // bind Rating values absolute<->relative when writing
        RatingF.setOnKeyReleased(e -> setPR());
        RatingF.setOnMousePressed(e -> setPR());
        RatingPF.setOnKeyReleased(e -> setR());
        RatingPF.setOnMousePressed(e -> setR());
        
        // show metadata list
        infoL.setOnMouseClicked(e -> showItemsPopup());
        infoL.setCursor(Cursor.HAND);
        
        entireArea.setOnKeyPressed(e -> { if(e.getCode()==CONTROL) add_not_set.set(true); });
        entireArea.setOnKeyReleased(e -> { if(e.getCode()==CONTROL) add_not_set.set(false); });
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
        readMode.applyValue();
    }
    
    /**
     * This widget is empty if it has no data.
     * @return 
     */
    @Override
    public boolean isEmpty() {
        return allitems.isEmpty();
    }
    
    @Override
    public void close() {
        if (d1!=null) d1.unsubscribe();
    }
    
    
    
/******************************************************************************/
    BooleanProperty add_not_set = new SimpleBooleanProperty(false);
    
    /**
     * Reads metadata on provided items and fills the data for tagging. 
     * @param items list of any type of items to edit. The list can be mixed and
     * contain any Item types. If list contains Metadata themselves, their data will
     * be immediately used.
     * @throws NullPointerException if param null
     */    
    @Override
    public void read(List<? extends Item> items) {
        Objects.requireNonNull(items);
        // remove duplicates
        MapSet<URI, ? extends Item> unique = new MapSet<>(Item::getURI, items);
        
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
                if(!readAll || i instanceof Metadata) ready.add((Metadata)i);
                else needs_read.add(i);
            });

        // read metadata for items
        MetadataReader.readMetadata(needs_read, (success,result) -> {
            if(success) {
                // remove duplicates
                MapSet<URI, Metadata> unique = new MapSet<>(Metadata::getURI);
                                      unique.addAll(metas);
                                      unique.addAll(ready);
                                      unique.addAll(result);
                
                metas.clear();
                metas.addAll(unique);
                populate(metas);
                Log.info("Tagger: Metadata reading succeeded.");
            } else {
                Log.info("Tagger: Metadata reading failed.");
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
    @FXML @Override
    public void write() {
        if (loader != null && loader.isRunning()) return;
        
        Validation v = validators.stream().filter(Validation::isInValid).findFirst().orElse(null);
        if(v!=null) {
            PopOver p = new PopOver(new Text(v.text));
            p.show(PopOver.ScreenCentricPos.AppCenter);
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
            ColorF.setUserData(true);
            if ((boolean)ColorF.getUserData())        w.setColor(ColorF.getValue());
            if ((boolean)Custom1F.getUserData())      w.setCustom1(Custom1F.getText());
            if ((boolean)Custom2F.getUserData())      w.setCustom2(Custom2F.getText());
            if ((boolean)Custom3F.getUserData())      w.setCustom3(Custom3F.getText());
            if ((boolean)Custom4F.getUserData())      w.setCustom4(Custom4F.getText());
            if ((boolean)Custom5F.getUserData())      w.setCustom5(Custom5F.getText());
            if ((boolean)LyricsA.getUserData())       w.setLyrics(LyricsA.getText());
            if ((boolean)CoverL.getUserData())        w.setCover(new_cover_file);
        }, refreshed -> {
            // post writing
            writing = false;
            populate(refreshed);
            App.use(Notifier.class, s->s.showTextNotification("Tagging complete", "Tagger"));
        });
        
    }
    
/******************************************************************************/
    
    /** use null to clear gui empty. */
    private void populate(List<Metadata> items) {
        // return if writing active
        if (writing) { System.out.println("illegal state. tagger writing, cant update gui!!");
            hideProgress(); return; }
        
        boolean empty = items == null || items.isEmpty();
        
        // empty previous content
        fields.forEach(TagField::emptyContent);
        CoverV.loadImage((Image)null);
        CoverV.getPane().setDisable(true);
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
            infoL.setGraphic(Icons.createIcon(items.size()==1 ? FontAwesomeIconName.TAG : TAGS));

            fields.forEach(TagField::enable);
            CoverV.getPane().setDisable(false);


            Async.run(() -> {

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
                         if (c == 0)    CoverL.setText(TAG_NO_VALUE);
                    else if (c == 1)    CoverL.setText(s);
                    else if (c == 2)    CoverL.setText(TAG_MULTIPLE_VALUE);
                        // set image
                    if (c == 1)         CoverV.loadImage(co.getImage());
                    else                CoverV.loadImage((Image)null);

                    // enable/disable playcount field
                    if(!allow_playcount_change.getValue()) PlaycountF.setDisable(true);

                    hideProgress();
                });
            });
        
        }
    }
    
    private void showProgressReading() {
        progressI.setProgress(INDETERMINATE_PROGRESS);
        progressPane.setVisible(true);
        // make inaccessible during sensitive operation
        scrollContent.setMouseTransparent(true);        
        // apply blur to content to hint inaccessibility
        // note: dont apply on root it would also blur the progres indicator!
        scrollContent.setEffect(new BoxBlur(3, 3, 1));
    }
    private void showProgressWriting() {
        progressI.setProgress(INDETERMINATE_PROGRESS);
        progressPane.setVisible(true);
        scrollContent.setEffect(new BoxBlur(3, 3, 1));
        scrollContent.setMouseTransparent(true);
    }
    private void hideProgress() {
        progressPane.setVisible(false);
        progressI.setProgress(0);
        scrollContent.setEffect(null);
        scrollContent.setMouseTransparent(false);
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
            CoverL.setText(TAG_NO_VALUE);
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
            
            c.getStyleClass().setAll(GraphicalTextField.getTextFieldStyleClass());
            c.setMinSize(0, 0);
            c.setPrefSize(-1, -1);
            
            if(valCond!=null && c instanceof CustomTextField) {
                Validation v = new Validation(c, valCond , field + " field doeas not contain valid text.");
                validators.add(v);
                Label l = new Icon(EXCLAMATION_TRIANGLE, 11);
                CustomTextField cf = (CustomTextField)c;
                c.textProperty().addListener((o,ov,nv) -> {
                    boolean b = v.isValid();
                    l.setVisible(!b);
                    if(b) if(cf.getRight()==l) cf.setRight(new Region());
                    else if(cf.getRight()!=l) cf.setRight(l);
                });
            }
            
            emptyContent();
            
            // restrain input
            if(field.isTypeNumber())
                InputConstraints.numbersOnly(c, !field.isTypeNumberNonegative(), field.isTypeFloatingNumber());
            
            // if not commitable yet, enable commitable & set text to tag value on click
            c.setOnMouseClicked(this::OnMouseClicked);
            
            // disable commitable if empty and backspace key pressed
            c.setOnKeyPressed( e -> {
                if (isIn(e.getCode(),BACK_SPACE,ESCAPE))
                    OnBackspacePressed();
            });
        }
        void enable() { 
            c.setDisable(false);
        }
        void disable() {
            c.setDisable(true);
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
        void OnMouseClicked(MouseEvent e) {
            if (!(boolean)c.getUserData()) {
                c.setUserData(true);
                c.setText("");
                c.setText("");
                c.setText(isIn(c.getPromptText(), TAG_NO_VALUE, TAG_MULTIPLE_VALUE)
                                ? "" : c.getPromptText());
                c.setPromptText("");
                c.selectAll();
            }
        }
        void OnBackspacePressed() {
            if (c.getText().isEmpty()) {
                c.setPromptText(c.getId());
                c.setUserData(false);
                entireArea.requestFocus();
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
            boolean empty = m.isFieldEmpty(f);
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
                Color c = new ColorParser().fromS(histogramS);
                ColorF.setValue(c==null ? EMPTY_COLOR : c);   
                Custom1F.setText("");
            }
            
            if      (histogramI == 0)   c.setPromptText(TAG_NO_VALUE);
            else if (histogramI == 1)   c.setPromptText(histogramS);
            else if (histogramI == 2)   c.setPromptText(TAG_MULTIPLE_VALUE);
            
            // remember prompt text
            c.setId(c.getPromptText());
            // disable if unsuported
            setSupported(formats);
        }
    }

    
    public final class Validation {
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
    Callback<ListView<Item>,ListCell<Item>> editCellFactory = listView -> 
            new ListCell<Item>() {
                CheckIcon cb = new CheckIcon();
                {
                    // allow user to de/activate item
                    cb.setOnMouseClicked(e -> {
                        Item item = getItem();
                        // avoid nulls & respect lock
                        if(item != null) {
                            if(cb.selected.get()) add(singletonList(item),false);
                            else rem(singletonList(item));
//                            if(cb.selected.get()) items.add(item);
//                            else items.remove(item);
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
                        cb.selected.set(!untaggable);
                        cb.setDisable(untaggable);

                        if (getGraphic()==null) setGraphic(cb);
                    } else {
                        setText(null);
                        setGraphic(null);
                    }
                }
            };
    
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
                                        if(cb.selected.get()) add(singletonList(item),false);
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
                                    cb.selected.set(!untaggable);
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
        Label helpB = new Icon(FontAwesomeIconName.INFO,11, "Help");                     
        helpB.setOnMouseClicked( e -> {
            // build help popup lazily
            if(helpP == null) {
                String text = "List of all items in the tagger. Highlights "
                            + "untaggable items. Taggable items can be unselected"
                            + " filtered.\n\n"
                            + "Available actions:\n"
                            + "    Drop items : Clears tagget and adds to tagger.\n"
                            + "    Drop items + CTRL : Adds to tagger.";
                helpP = PopOver.createHelpPopOver(text);
            }
            helpP.show(helpB);
            e.consume();
        });
        
        // build popup
        PopOver p = new PopOver(list);
                p.title.set("Active Items");
                p.getHeaderIcons().addAll(helpB);
                p.show(infoL);
        return p;
    }
    
    private final EventHandler<DragEvent> drag_dropped_handler = e -> {
        if (DragUtil.hasAudio(e.getDragboard())) {
            List<Item> dropped = DragUtil.getAudioItems(e);
            //end drag transfer
            e.setDropCompleted(true);
            e.consume();
            // handle result - read data
            if (!dropped.isEmpty()) {
                if (changeReadModeOnTransfer) readMode.setNapplyValue(CUSTOM);
                read(dropped);
            }
        }
    };
}