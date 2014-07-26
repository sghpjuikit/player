
package Tagger;

import AudioPlayer.Player;
import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.tagging.Cover.Cover;
import static AudioPlayer.tagging.Cover.Cover.CoverSource.TAG;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.MetadataReader;
import AudioPlayer.tagging.MetadataWriter;
import Configuration.Configuration;
import Configuration.IsConfig;
import GUI.DragUtil;
import GUI.ItemHolders.ItemTextFields.MoodTextField;
import GUI.NotifierManager;
import GUI.objects.PopOver.PopOver;
import GUI.objects.PopOver.PopOver.NodeCentricPos;
import GUI.objects.Thumbnail;
import Layout.Widgets.FXMLController;
import Layout.Widgets.Features.TaggingFeature;
import Layout.Widgets.Widget;
import Layout.Widgets.WidgetInfo;
import PseudoObjects.ReadMode;
import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import static de.jensd.fx.fontawesome.AwesomeIcon.TAGS;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.BoxBlur;
import javafx.scene.image.Image;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import static javafx.scene.input.KeyCode.BACK_SPACE;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseDragEvent.MOUSE_DRAG_RELEASED;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import org.controlsfx.validation.ValidationResult;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.decoration.GraphicValidationDecoration;
import utilities.ImageFileFormat;
import utilities.Log;
import utilities.Parser.ColorParser;
import utilities.functional.impl.Validators.IsBetween0And1;
import utilities.functional.impl.Validators.Validator;
import utilities.functional.impl.Validators.isIntString;
import utilities.functional.impl.Validators.isYearString;

/**
 * TaggerController graphical component.
 * <p>
 * Can read and write metadata from/into files.
 * Currently supports files only. File types are limited to those supported
 * by the application.
 * 
 * @author Plutonium_
 */
@WidgetInfo(
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
    year = "2014",
    group = Widget.Group.TAGGER)
public class TaggerController extends FXMLController implements TaggingFeature {
    
    @FXML AnchorPane entireArea;
    @FXML ScrollPane scrollPaneContent;
    @FXML GridPane grid;
    @FXML TextField TitleF;
    @FXML TextField AlbumF;
    @FXML TextField ArtistF;
    @FXML TextField AlbumArtistF;
    @FXML TextField ComposerF;
    @FXML TextField PublisherF;
    @FXML TextField TrackF;
    @FXML TextField TracksTotalF;
    @FXML TextField DiscF;
    @FXML TextField DiscsTotalF;
    @FXML TextField GenreF;
    @FXML TextField CategoryF;
    @FXML TextField YearF;
    @FXML TextField RatingF;
    @FXML TextField RatingPF;
    @FXML TextField PlaycountF;
    @FXML TextField CommentF;
          MoodTextField MoodF = new MoodTextField();
    @FXML ColorPicker ColorF;
    @FXML TextField Custom1F;
    @FXML TextField Custom2F;
    @FXML TextField Custom3F;
    @FXML TextField Custom4F;
    @FXML TextField Custom5F;
    @FXML TextArea LyricsA;
    @FXML BorderPane coverContainer;
    @FXML Label CoverL;
    Thumbnail CoverV;
    File new_cover_file = null;
    @FXML StackPane progressPane;
    @FXML ProgressIndicator progressI;
    @FXML Label infoL;
    @FXML Rectangle img_border;
    @FXML AnchorPane img_borderContainer;
    
    // properties
    @IsConfig(name = "Read Mode", info = "Source of data for the widget.")
    public ReadMode readMode = ReadMode.CUSTOM;
    @IsConfig(name = "Read mode change on drag", info = "Change read mode to CUSTOM when data are arbitrary added to widget.")
    public Boolean changeReadModeOnTransfer = false;
    @IsConfig(name = "No value text", info = "Field text in case no tag value for field.")
    public String TAG_NO_VALUE = Configuration.TAG_NO_VALUE;
    @IsConfig(name = "Multiple value text", info = "Field text in case multiple tag values per field.")
    public String TAG_MULTIPLE_VALUE = Configuration.TAG_MULTIPLE_VALUE;
    @IsConfig(name = "Allow change of playcount", info = "Change editability of playcount field. Generally to prevent change to non customary values.")
    public Boolean allow_playcount_change = false;
    @IsConfig(name = "Field text alignement", info = "Alignment of the text in fields.")
    public Pos field_text_alignment = Pos.CENTER_LEFT;
    @IsConfig(name="Mood picker popup position", info = "Position of the mood picker pop up relative to the mood text field.")
    public NodeCentricPos popupPos = NodeCentricPos.DownCenter;
    
    //global variables
    ObservableList<Item> allitems = FXCollections.observableArrayList();
    ObservableList<Item> items = FXCollections.observableArrayList();
    ObservableList<Metadata> metas = FXCollections.observableArrayList();   // currently active
    Task<List<Metadata>> loader;
    final List<TagField> fields = new ArrayList<>();
    boolean writing = false;    // prevents external data chagnge during writing
    Task<List<Metadata>> metaReader;
//    BiProcedure<Boolean,List<Metadata>> onMetaReadingFinish = 
            
    // listeners
    private final InvalidationListener playlistListener = o -> 
            read(PlaylistManager.getSelectedItems());
    private final ChangeListener<Metadata> playingListener = (o,oldV,newV) ->
            read(Collections.singletonList(newV));
            
            
    
    @Override
    public void init() {
        
        items.addListener((ListChangeListener.Change<? extends Item> c) -> {System.out.println("has change");
            while (c.next()) {
                // on remove: remove concerned metadatas and populate gui
                if (c.wasRemoved()) {
                    // get removed and remove
                    List<? extends Item> rem = c.getRemoved();
                    metas.removeIf( m -> rem.stream().anyMatch(i -> i.same(m)));
                    populate(metas);
                }
                // on add: get metadata for all items and populate
                else if (c.wasAdded()) {System.out.println("added " + c.getAddedSize());
                    // get added
                    List<Metadata> ready = new ArrayList();
                    List<Item> needs_read = new ArrayList();
                    c.getAddedSubList().stream()
                        // filter out untaggable
                        .filter(i -> !i.isCorrupt() && i.isFileBased())
                        // separate Metadata and Items
                        .forEach(i -> {
                            if(i instanceof Metadata) ready.add((Metadata)i);
                            else needs_read.add(i);
                        });
                    
                    // prevent double reading while reading
                    if(metaReader!=null && metaReader.isRunning()) {
                        System.out.println("DOUBLE READING");
                    }
                    // read metadata for items
                    MetadataReader.readMetadata(needs_read, (success,result) -> {
                        if(success) {
                            ready.addAll(result);
                            ready.forEach(i->{
                                if(!metas.stream().anyMatch(ii->ii.same(i)))
                                    metas.add(i);
                            });
                            // bugged most probably because this listener is called
                            // twice for some unknown reason
//                            metas.addAll(result);
                            System.out.println(result.size() + " "
//                                    + ready.size()
                                    + " " + metas.size());
                            populate(metas);
                            Log.mess("Tagger: Metadata reading succeeded.");
                        } else {
                            Log.mess("Tagger: Metadata reading failed.");
                        }
                        hideProgress();
                    });
                    showProgressReading();
                }
            }
        });
        
        CoverV = new Thumbnail(200);
        CoverV.setDragImage(false); // we have our own implementation below
        coverContainer.setCenter(CoverV.getPane());
        // add specialized mood text field
        grid.add(MoodF, 1, 14, 2, 1);
        
        // initialize fields
        fields.add(new TagField(TitleF));
        fields.add(new TagField(AlbumF));
        fields.add(new TagField(ArtistF));
        fields.add(new TagField(AlbumArtistF));
        fields.add(new TagField(ComposerF));
        fields.add(new TagField(PublisherF));
        fields.add(new TagField(TrackF));
        fields.add(new TagField(TracksTotalF));
        fields.add(new TagField(DiscF));
        fields.add(new TagField(DiscsTotalF));
        fields.add(new TagField(GenreF));
        fields.add(new TagField(CategoryF));
        fields.add(new TagField(YearF));
        fields.add(new TagField(RatingF));
        fields.add(new TagField(RatingPF));
        fields.add(new TagField(PlaycountF));
        fields.add(new TagField(CommentF));
        fields.add(new TagField(MoodF));
        fields.add(new TagField(Custom1F));
        fields.add(new TagField(Custom2F));
        fields.add(new TagField(Custom3F));
        fields.add(new TagField(Custom4F));
        fields.add(new TagField(Custom5F));
        fields.add(new TagField(LyricsA));
            // associate color picker with custom1 field
        Custom1F.setEditable(false);
        ColorF.disableProperty().bind(Custom1F.disabledProperty());
        ColorF.valueProperty().addListener( (o,oldV,newV) -> {
            Custom1F.setText(new ColorParser().toS(newV));
        });
        
        // validating input
        Validator<String> isPercent = new IsBetween0And1();
        Validator<String> isYear = new isYearString();
        Validator<String> isInt = new isIntString();
        
        
        ValidationSupport val = new ValidationSupport();
        val.setValidationDecorator(new GraphicValidationDecoration());
            // year validation
        val.registerValidator(YearF, (Control c, String text) -> ValidationResult.fromErrorIf(
            YearF, "Year must be greater than 0 and not greater than current year.",
            !text.isEmpty() && !isYear.test(text)
        ));
            // cd validation
        DiscsTotalF.textProperty().addListener(o->{
            String old = DiscF.getText();
            DiscF.setText("");old+="";
            DiscF.setText(old);
        });
        val.registerValidator(DiscF, (Control c, String newV) -> ValidationResult.fromErrorIf(
            DiscF, "Disc.",
            !newV.isEmpty() && (!isInt.test(newV) || (isInt.test(newV)&&new Integer(newV)<0) ||
                    (isInt.test(newV)&&(isInt.test(DiscsTotalF.getText())&& new Integer(newV)>Integer.parseInt(DiscsTotalF.getText())))
                    )));
            // rating validation
        val.registerValidator(RatingF, (Control c, String newV) -> ValidationResult.fromErrorIf(
            RatingF, "Rating must be between 0 and max value.",
            !newV.isEmpty() && !isPercent.test(RatingPF.getText()))
        );
        val.registerValidator(RatingPF, (Control c, String newV) -> ValidationResult.fromErrorIf(
            RatingPF, "Rating must be between 0 and 1.",
            !newV.isEmpty() && !isPercent.test(newV))
        );
        
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
            
            // get initial directory
            File initial_dir = metas.stream().filter(Item::isFileBased)
                                         .findFirst().map(Item::getLocation)
                                         .orElse(new File(""));
            FileChooser fc = new FileChooser();
                        fc.setInitialDirectory(initial_dir);
                        fc.setTitle("Select image to add to tag");
                        fc.getExtensionFilters().addAll(ImageFileFormat.extensions()
                          .stream().map(ext->new FileChooser.ExtensionFilter(ext,ext))
                          .collect(Collectors.toList()));
            File f = fc.showOpenDialog(entireArea.getScene().getWindow());
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
        
        // hoverable img border
        coverContainer.addEventHandler(MOUSE_EXITED, e-> img_border.setOpacity(0));
        coverContainer.addEventHandler(MOUSE_ENTERED, e-> {
            if (!isEmpty()) img_border.setOpacity(1);
        });
        img_border.setOpacity(0);
        
        // set img border clip
        AnchorPane clip = new AnchorPane();
            clip.layoutXProperty().bind(img_border.layoutXProperty());
            clip.layoutYProperty().bind(img_border.layoutYProperty());
            clip.prefWidthProperty().bind(img_border.widthProperty());
            clip.prefHeightProperty().bind(img_border.heightProperty());
            Rectangle r1 = new Rectangle(35, 35);
            Rectangle r2 = new Rectangle(25, 25);
            Rectangle r3 = new Rectangle(25, 25);
            Rectangle r4 = new Rectangle(35, 35);
            clip.getChildren().addAll(r1,r2,r3,r4);
            r1.relocate(0, 0);
            r2.relocate(175, 0);
            r3.relocate(0, 175);
            r4.relocate(175, 175);
        img_border.setClip(clip);
        
        
        // bind Rating values absolute<->relative when writing
        RatingF.setOnKeyReleased(e -> setPR());
        RatingF.setOnMousePressed(e -> setPR());
        RatingPF.setOnKeyReleased(e-> setR());
        RatingPF.setOnMousePressed(e-> setR());
        
        // show metadata list
        infoL.setOnMouseClicked( e -> showItemsPopup());
        infoL.setCursor(Cursor.HAND);
        
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
        } catch (NumberFormatException | NullPointerException ex) {
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
        PlaylistManager.getSelectedItems().removeListener(playlistListener);
        Player.currentMetadataProperty().removeListener(playingListener);
        
        // rebind
        if (readMode==ReadMode.PLAYLIST_SELECTED) {
            PlaylistManager.getSelectedItems().addListener(playlistListener);
            playlistListener.invalidated(null);
        }
        else if (readMode==ReadMode.PLAYING){
            Player.currentMetadataProperty().addListener(playingListener);
            playingListener.changed(null,null,Player.getCurrentMetadata());
        }
        else if (readMode==ReadMode.LIBRARY_SELECTED){
            // to implement
        }
//        else if (readMode==ReadMode.CUSTOM)
//            readData(metadatas);
        
        fields.forEach(f->f.setVerticalAlignment(field_text_alignment));
        MoodF.setPos(popupPos);
    }
    
    public void setReadMode(ReadMode val) {
        readMode = val;
        if (val == ReadMode.CUSTOM) {
            items.clear();
            allitems.clear();
        }
        refresh();
    }
    
    /**
     * This widget is empty if it has no data.
     * @return 
     */
    @Override
    public boolean isEmpty() {
        // the assumption is that this widget will always display data if present
        return allitems.isEmpty();
    }
    
    @Override
    public void OnClosing() {
        // remove listeners
        PlaylistManager.getSelectedItems().removeListener(playlistListener);
        Player.currentMetadataProperty().removeListener(playingListener);
    }
    
    
    
/******************************************************************************/
    
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
        
        List<Item> unique = new ArrayList();
        for(Item i : items) {
            boolean has_double = false;
            for (Item o : unique) has_double |= o.same(i);
            // add if doesnt
            if (!has_double) unique.add(i);
        }
        
        
//        List<Item> to_add = new ArrayList(unique);
//        List<Item> to_del = new ArrayList<>(metas);
//        for (Item o: unique) {
//            // if exists already dont do anything
//            for(Item i : metas) {
//                if(i.same(o)) {
//                    to_del.remove(i);
//                    to_add.remove(o);
//                }
//            }
//        }
//        
//        // do not use setAll we need to fire remove & add events
//        // remove all unwanted and keep those that are the same as new ones
//        this.allitems.removeAll(to_del);
//        this.items.removeAll(to_del);
//        // add new ones that werent on the list before
//        this.allitems.addAll(to_add);
//        this.items.addAll(to_add);
        
        
        this.allitems.clear();
        this.items.clear();
        this.allitems.addAll(unique);
        this.items.addAll(unique);
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
        
        // pre
        writing = true;
        showProgressWriting();
        
        // writing
        metas.stream().map(MetadataWriter::create).forEach( writer -> {
            // write to tag if field commitable
            if ((boolean)TitleF.getUserData())        writer.setTitle(TitleF.getText());
            if ((boolean)AlbumF.getUserData())        writer.setAlbum(AlbumF.getText());
            if ((boolean)ArtistF.getUserData())       writer.setArtist(ArtistF.getText());
            if ((boolean)AlbumArtistF.getUserData())  writer.setAlbum_artist(AlbumArtistF.getText());
            if ((boolean)ComposerF.getUserData())     writer.setComposer(ComposerF.getText());
            if ((boolean)PublisherF.getUserData())    writer.setPublisher(PublisherF.getText());
            if ((boolean)TrackF.getUserData())        writer.setTrack(TrackF.getText());
            if ((boolean)TracksTotalF.getUserData())  writer.setTracks_total(TracksTotalF.getText());
            if ((boolean)DiscF.getUserData())         writer.setDisc(DiscF.getText());
            if ((boolean)DiscsTotalF.getUserData())   writer.setDiscs_total(DiscF.getText());
            if ((boolean)GenreF.getUserData())        writer.setGenre(GenreF.getText());
            if ((boolean)CategoryF.getUserData())     writer.setCategory(CategoryF.getText());
            if ((boolean)YearF.getUserData())         writer.setYear(YearF.getText());
            if ((boolean)RatingF.getUserData())       writer.setRatingPercent(RatingPF.getText());
            if ((boolean)PlaycountF.getUserData())    writer.setPlaycount(PlaycountF.getText());
            if ((boolean)CommentF.getUserData())      writer.setComment(CommentF.getText());
            if ((boolean)MoodF.getUserData())         writer.setMood(MoodF.getText());
            ColorF.setUserData(true);
            if ((boolean)ColorF.getUserData())        writer.setColor(ColorF.getValue());
            if ((boolean)Custom1F.getUserData())      writer.setCustom1(Custom1F.getText());
            if ((boolean)Custom2F.getUserData())      writer.setCustom2(Custom2F.getText());
            if ((boolean)Custom3F.getUserData())      writer.setCustom3(Custom3F.getText());
            if ((boolean)Custom4F.getUserData())      writer.setCustom4(Custom4F.getText());
            if ((boolean)Custom5F.getUserData())      writer.setCustom5(Custom5F.getText());
            if ((boolean)LyricsA.getUserData())       writer.setLyrics(LyricsA.getText());
            if ((boolean)CoverL.getUserData())        writer.setCover(new_cover_file);
            
            writer.write();
        });
        
        // post writing
        hideProgress();
        writing = false;
        NotifierManager.showTextNotification("Tagging complete", "Tagger");
        read(metas);
    }
    
/******************************************************************************/
    
    /** use null to clear gui empty. */
    private void populate(List<Metadata> items) {
        // return if writing active
        if (writing) return;
        
        boolean empty = items == null || items.isEmpty();
        
        // empty previous content
        fields.forEach(TagField::emptyContent);
        CoverV.loadImage((Image)null);
        CoverV.getPane().setDisable(true);
        CoverL.setUserData(false);
        new_cover_file = null;
        infoL.setText("No items loaded");
        infoL.setGraphic(null);
        
        // return if no new content
        if (empty) return;
        
        // set info label graphics
        AwesomeDude.setIcon(infoL, items.size()==1 ? AwesomeIcon.TAG : TAGS);
        
        fields.forEach(TagField::enable);
        CoverV.getPane().setDisable(false);
        
        // initializing checkers for multiple values
            //0 = no value in all items       write "no assigned value"
            //1 = same value in all items     write actual value
            //2 = multiple value in all items write "multiple value"
        int Title = 0;
        int Album = 0;
        int Artist = 0;
        int AlbumArtist = 0;
        int Composer = 0;
        int Publisher = 0;
        int Track = 0;
        int TracksTotal = 0;
        int Disc = 0;
        int DiscsTotal = 0;
        int Genre = 0;
        int Category = 0;
        int Year = 0;
        int Rating = 0;
        int Playcount = 0;
        int Comment = 0;
        int Mood = 0;
        int Custom1 = 0;
        int Custom2 = 0;
        int Custom3 = 0;
        int Custom4 = 0;
        int Custom5 = 0;
        int Lyrics = 0;
        int Cov = 0;
        String TitleS = "";
        String AlbumS = "";
        String ArtistS = "";
        String AlbumArtistS = "";
        String ComposerS = "";
        String PublisherS = "";
        String TrackS = "";
        String TracksTotalS = "";
        String DiscS = "";
        String DiscsTotalS = "";
        String GenreS = "";
        String CategoryS = "";
        String YearS = "";
        String RatingS = "";
        String RatingPS = "";
        String PlaycountS = "";
        String CommentS = "";
        String MoodS = "";
        String Custom1S = "";
        String Custom2S = "";
        String Custom3S = "";
        String Custom4S = "";
        String Custom5S = "";
        String LyricsS = "";
        String CovInfoS = "";
        Cover CovS = null;
        
        // check multiple values to determine general field values
        // the condition goes like this (for every field):
        // -- initializes at 0 = no value streak
        // -- if first value not empty -> break no value streak, start same value streak, otherwise continue
        //    (either all will be empty(0) or all will be same(1) - either way first value determines 0/1 streak)
        // -- if empty streak and (non first) value not empty -> conclusion = multiple values
        //    (empty and non empty values = multiple)
        // -- if same value streak and different value -> conclusion = multiple values
        // -- otherwise this ends as no value or same streak decided by 1st value
        for(Metadata m: items) {
            int i = items.indexOf(m);
            if (i==0 && !m.getTitle().isEmpty())                                { Title = 1; TitleS = m.getTitle(); }
            if (Title == 0 && i != 0 && !m.getTitle().isEmpty())                { Title = 2; TitleS = m.getTitle(); }
            if (Title == 1 && !m.getTitle().equals(TitleS))                     { Title = 2; }
            if (i==0 && !m.getAlbum().isEmpty())                                { Album = 1; AlbumS = m.getAlbum(); }
            if (Album == 0 && i != 0 && !m.getAlbum().isEmpty())                { Album = 2; AlbumS = m.getAlbum(); }
            if (Album == 1 && !m.getAlbum().equals(AlbumS))                     { Album = 2; }
            if (i==0 && !m.getArtist().isEmpty())                               { Artist = 1; ArtistS = m.getArtist(); }
            if (Artist == 0 && i != 0 && !m.getArtist().isEmpty())              { Artist = 2; ArtistS = m.getArtist(); }
            if (Artist == 1 && !m.getArtist().equals(ArtistS))                  { Artist = 2; }
            if (i==0 && !m.getAlbumArtist().isEmpty())                          { AlbumArtist = 1; AlbumArtistS = m.getAlbumArtist(); }
            if (AlbumArtist == 0 && i != 0 && !m.getAlbumArtist().isEmpty())    { AlbumArtist = 2; AlbumArtistS = m.getAlbumArtist(); }
            if (AlbumArtist == 1 && !m.getAlbumArtist().equals(AlbumArtistS))   { AlbumArtist = 2; }
            if (i==0 && !m.getComposer().isEmpty())                             { Composer = 1; ComposerS = m.getComposer(); }
            if (Composer == 0 && i != 0 && !m.getComposer().isEmpty())          { Composer = 2; ComposerS = m.getComposer(); }
            if (Composer == 1 && !m.getComposer().equals(ComposerS))            { Composer = 2; }
            if (i==0 && !m.getPublisher().isEmpty())                            { Publisher = 1; PublisherS = m.getPublisher(); }
            if (Publisher == 0 && i != 0 && !m.getPublisher().isEmpty())        { Publisher = 2; PublisherS = m.getPublisher(); }
            if (Publisher == 1 && !m.getPublisher().equals(PublisherS))         { Publisher = 2; }
            if (i==0 && !m.getTrack().isEmpty())                                { Track = 1; TrackS = m.getTrack(); }
            if (Track == 0 && i != 0 && !m.getTrack().isEmpty())                { Track = 2; TrackS = m.getTrack(); }
            if (Track == 1 && !m.getTrack().equals(TrackS))                     { Track = 2; }
            if (i==0 && !m.getTracksTotal().isEmpty())                          { TracksTotal = 1; TracksTotalS = m.getTracksTotal(); }
            if (TracksTotal == 0 && i != 0 && !m.getTracksTotal().isEmpty())    { TracksTotal = 2; TracksTotalS = m.getTracksTotal(); }
            if (TracksTotal == 1 && !m.getTracksTotal().equals(TracksTotalS))   { TracksTotal = 2; }
            if (i==0 && !m.getDisc().isEmpty())                                 { Disc = 1; DiscS = m.getDisc(); }
            if (Disc == 0 && i != 0 && !m.getDisc().isEmpty())                  { Disc = 2; DiscS = m.getDisc(); }
            if (Disc == 1 && !m.getDisc().equals(DiscS))                        { Disc = 2; }
            if (i==0 && !m.getDiscsTotal().isEmpty())                           { DiscsTotal = 1; DiscsTotalS = m.getDiscsTotal(); }
            if (DiscsTotal == 0 && i != 0 && !m.getDiscsTotal().isEmpty())      { DiscsTotal = 2; DiscsTotalS = m.getDiscsTotal(); }
            if (DiscsTotal == 1 && !m.getDiscsTotal().equals(DiscsTotalS))      { DiscsTotal = 2; }
            if (i==0 && !m.getGenre().isEmpty())                                { Genre = 1; GenreS = m.getGenre(); }
            if (Genre == 0 && i != 0 && !m.getGenre().isEmpty())                { Genre = 2; GenreS = m.getGenre(); }
            if (Genre == 1 && !m.getGenre().equals(GenreS))                     { Genre = 2; }
            if (i==0 && !m.getCategory().isEmpty())                             { Category = 1; CategoryS = m.getCategory(); }
            if (Category == 0 && i != 0 && !m.getCategory().isEmpty())          { Category = 2; CategoryS = m.getCategory(); }
            if (Category == 1 && !m.getCategory().equals(CategoryS))            { Category = 2; }
            if (i==0 && !m.getYear().isEmpty())                                 { Year = 1; YearS = m.getYear(); }
            if (Year == 0 && i != 0 && m.getYear().isEmpty())                   { Year = 2; YearS = m.getYear(); }
            if (Year == 1 && !m.getYear().equals(YearS))                        { Year = 2; }
            if (i==0 && !m.getRatingPercentAsString().isEmpty())                { Rating = 1; RatingPS = m.getRatingPercentAsString(); RatingS = m.getRatingAsString();}
            if (Rating == 0 && i !=0 && !m.getRatingPercentAsString().isEmpty()){ Rating = 2; RatingPS = m.getRatingPercentAsString(); RatingS = m.getRatingAsString();}
            if (Rating == 1 && !m.getRatingPercentAsString().equals(RatingPS))  { Rating = 2; }
            if (i==0 && !m.getPlaycountAsString().isEmpty())                    { Playcount = 1; PlaycountS = m.getPlaycountAsString(); }
            if (Playcount == 0 && i != 0 && !m.getPlaycountAsString().isEmpty()){ Playcount = 2; PlaycountS = m.getPlaycountAsString(); }
            if (Playcount == 1 && !m.getPlaycountAsString().equals(PlaycountS)) { Playcount = 2; }
            if (i==0 && !m.getComment().isEmpty())                              { Comment = 1; CommentS = m.getComment(); }
            if (Comment == 0 && i != 0 && !m.getComment().isEmpty())            { Comment = 2; CommentS = m.getComment(); }
            if (Comment == 1 && !m.getComment().equals(CommentS))               { Comment = 2; }
            if (i==0 && !m.getMood().isEmpty())                                 { Mood = 1; MoodS = m.getMood(); }
            if (Mood == 0 && i != 0 && !m.getMood().isEmpty())                  { Mood = 2; MoodS = m.getMood(); }
            if (Mood == 1 && !m.getMood().equals(MoodS))                        { Mood = 2; }
            if (i==0 && !m.getCustom1().isEmpty())                              { Custom1 = 1; Custom1S = m.getCustom1(); }
            if (Custom1 == 0 && i != 0 && !m.getCustom1().isEmpty())            { Custom1 = 2; Custom1S = m.getCustom1(); }
            if (Custom1 == 1 && !m.getCustom1().equals(Custom1S))               { Custom1 = 2; }
            if (i==0 && !m.getCustom2().isEmpty())                              { Custom2 = 1; Custom2S = m.getCustom2(); }
            if (Custom2 == 0 && i != 0 && !m.getCustom2().isEmpty())            { Custom2 = 2; Custom2S = m.getCustom2(); }
            if (Custom2 == 1 && !m.getCustom2().equals(Custom2S))               { Custom2 = 2; }
            if (i==0 && !m.getCustom3().isEmpty())                              { Custom3 = 1; Custom3S = m.getCustom3(); }
            if (Custom3 == 0 && i != 0 && !m.getCustom3().isEmpty())            { Custom3 = 2; Custom3S = m.getCustom3(); }
            if (Custom3 == 1 && !m.getCustom3().equals(Custom3S))               { Custom3 = 2; }
            if (i==0 && !m.getCustom4().isEmpty())                              { Custom4 = 1; Custom4S = m.getCustom4(); }
            if (Custom4 == 0 && i != 0 && !m.getCustom4().isEmpty())            { Custom4 = 2; Custom4S = m.getCustom4(); }
            if (Custom4 == 1 && !m.getCustom4().equals(Custom4S))               { Custom4 = 2; }
            if (i==0 && !m.getCustom5().isEmpty())                              { Custom5 = 1; Custom5S = m.getCustom5(); }
            if (Custom5 == 0 && i != 0 && !m.getCustom5().isEmpty())            { Custom5 = 2; Custom5S = m.getCustom5(); }
            if (Custom5 == 1 && !m.getCustom5().equals(Custom5S))               { Custom5 = 2; }
            if (i==0 && !m.getLyrics().isEmpty())                               { Lyrics = 1; LyricsS = m.getLyrics(); }
            if (Lyrics == 0 && i != 0 && !m.getLyrics().isEmpty())              { Lyrics = 2; LyricsS = m.getLyrics(); }
            if (Lyrics == 1 && !m.getLyrics().equals(LyricsS))                  { Lyrics = 2; }
            Cover c = m.getCover(TAG);
            if (i==0 && !c.isEmpty())                                           { Cov = 1; CovS = c; CovInfoS = c.getDestription(); }
            if (Cov == 0 && i != 0 && !c.isEmpty())                             { Cov = 2; CovS = c; CovInfoS = c.getDestription(); }
            if (Cov == 1 && !(!(c.isEmpty()&&CovS.isEmpty())||c.equals(CovS)))  { Cov = 2; }
        }
        
        // set fields prompt text
        setPromptText(TitleF, Title, TitleS);
        setPromptText(AlbumF, Album, AlbumS);
        setPromptText(AlbumArtistF, AlbumArtist, AlbumArtistS);
        setPromptText(ArtistF, Artist, ArtistS);
        setPromptText(ComposerF, Composer, ComposerS);
        setPromptText(PublisherF, Publisher, PublisherS);
        setPromptText(TrackF, Track, TrackS);
        setPromptText(TracksTotalF, TracksTotal, TracksTotalS);
        setPromptText(DiscF, Disc, DiscS);
        setPromptText(DiscsTotalF, DiscsTotal, DiscsTotalS);
        setPromptText(GenreF, Genre, GenreS);
        setPromptText(CategoryF, Category, CategoryS);
        setPromptText(YearF, Year, YearS);
        setPromptText(RatingF, Rating, RatingS);
        setPromptText(RatingPF, Rating, RatingPS);
        setPromptText(PlaycountF, Playcount, PlaycountS);
        setPromptText(CommentF, Comment, CommentS);
        setPromptText(MoodF, Mood, MoodS);
        setPromptText(Custom1F, Custom1, Custom1S);
        setPromptText(Custom2F, Custom2, Custom2S);
        setPromptText(Custom3F, Custom3, Custom3S);
        setPromptText(Custom4F, Custom4, Custom4S);
        setPromptText(Custom5F, Custom5, Custom5S);
        setPromptText(LyricsA, Lyrics, LyricsS);
        fields.forEach(TagField::rememberPromptText);
        // set color value
        Color c = new ColorParser().fromS(Custom1S);
        ColorF.setValue(c==null ? Color.WHITE : c);
                
        // set image info
             if (Cov == 0)    CoverL.setText(TAG_NO_VALUE);
        else if (Cov == 1)    CoverL.setText(CovInfoS);
        else if (Cov == 2)    CoverL.setText(TAG_MULTIPLE_VALUE);
             
        // set image
        if (Cov == 1)         CoverV.loadImage(CovS.getImage());
        else                  CoverV.loadImage((Image)null);
        
        // enable/disable playcount field
        PlaycountF.setDisable(!allow_playcount_change);
        
        // set info
        if (items.size()==1) infoL.setText("Single item loaded");
        else if (items.size() >1) infoL.setText(items.size() + " items loaded");      
    }
    
    private void showProgressReading() {
//        progressI.progressProperty().bind(loader.progressProperty());
        progressI.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        progressPane.setVisible(true);
        // add blur effect to content to hint inaccessibility
        // note: dont apply on root it would also blur the progres indicator!
        scrollPaneContent.setEffect(new BoxBlur(3, 3, 1));
        // make inaccessible
        scrollPaneContent.setMouseTransparent(true);        
    }
    private void showProgressWriting() {
//        progressI.progressProperty().unbind();
        progressI.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        progressPane.setVisible(true);
        // add blur effect to content to hint inaccessibility
        // note: dont apply on root it would also blur the progres indicator!
        scrollPaneContent.setEffect(new BoxBlur(3, 3, 1));
        // make inaccessible
        scrollPaneContent.setMouseTransparent(true);
    }
    private void hideProgress() {
        progressPane.setVisible(false);
//        progressI.progressProperty().unbind();
        progressI.setProgress(0);
        // remove effect
        scrollPaneContent.setEffect(null);
        // make accessible again
        scrollPaneContent.setMouseTransparent(false);
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
    private void setPromptText(TextInputControl control, int type, String s) {
        if (type == 0)        control.setPromptText(TAG_NO_VALUE);
        else if (type == 1)   control.setPromptText(s);
        else if (type == 2)   control.setPromptText(TAG_MULTIPLE_VALUE);
    }
    
    
/******************************************************************************/
    
    private final class TagField {
        private final TextInputControl c;
        
        public TagField(TextInputControl control) { 
            c = control;
            emptyContent();
            
            // if not commitable yet, enable commitable & set text to tag value on click
            c.setOnMouseClicked( e -> OnMouseClicked() );
            
            // disable commitable if empty and backspace key pressed
            c.setOnKeyPressed( e -> {
                if (e.getCode() == BACK_SPACE)
                    OnBackspacePressed();
            });
        }
        void enable() { c.setDisable(false); }
        void disable() { c.setDisable(true); }
        void emptyContent() {
            c.setText("");              // set empty
            c.setPromptText("");        // set empty
            c.setUserData(false);       // set uncommitable
            c.setDisable(true);         // set disabled
            c.setId("");                // set empty prompt text backup
        }
        void rememberPromptText() {
            c.setId(c.getPromptText());
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
                c.setText(c.getId());
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
    }
    
/**************************** active items popup ******************************/
    
    private static PseudoClass corrupt = PseudoClass.getPseudoClass("corrupt");
    PopOver helpP;
    Callback<ListView<Item>,ListCell<Item>> defCellFactory;
    Callback<ListView<Item>,ListCell<Item>> editCellFactory;
    
    private PopOver showItemsPopup() {
        // build popup
        ListView<Item> list = new ListView();
                       // factory is set dynamically
                       list.setCellFactory(getDefCellFactory());
                       // list will be atomatically updated now
                       list.setItems(allitems);
                       // support same drag & drop as tagger
                       list.setOnDragOver(DragUtil.audioDragAccepthandler);
                       list.setOnDragDropped(drag_dropped_handler);
           
        
        // build content controls
        Label helpB = AwesomeDude.createIconLabel(AwesomeIcon.INFO,"11");                     
        helpB.setOnMouseClicked( e -> {
            // build help content for help popup if not yet built
            // with this we avoid constructing multuple popups
            if(helpP == null) {
                String text = "List of all items in the tagger. Highlights " + 
                              "untaggable items. Taggable items can be unselected " +
                              "in selective list mode.\n\n" +
                              "Available actions:\n" +
                              "    Edit button : Switch to selectable list\n" +
                              "    Drop items : Adds them to tagger.\n" +
                              "    Drop audio files : Adds them to tagger.";
                helpP = PopOver.createHelpPopOver(text);
            }
            helpP.show(helpB);
            e.consume();
        });
        helpB.setTooltip(new Tooltip("Help"));
        Label editSwitchB = AwesomeDude.createIconLabel(AwesomeIcon.PENCIL,"11");                     
        editSwitchB.setOnMouseClicked( e -> {
            // switch factories
            list.setCellFactory(list.getCellFactory().equals(defCellFactory)
                                        ? getEditCellFactory()
                                        : getDefCellFactory());
            e.consume();
        });
        editSwitchB.setTooltip(new Tooltip("Edit"));
        
        // build popup
        PopOver p = new PopOver(list);
                p.setTitle("Active Items");
                p.getHeaderIcons().addAll(editSwitchB,helpB);
                p.show(infoL);
        return p;
    }
    
    private Callback<ListView<Item>,ListCell<Item>> getEditCellFactory() {
        if (editCellFactory == null) editCellFactory = listView -> {
            CheckBox cb = new CheckBox();
            BooleanProperty lock = new SimpleBooleanProperty(false);
            ListCell<Item> cell =  new ListCell<Item>() {
                @Override 
                protected void updateItem(Item item, boolean empty) {
                    super.updateItem(item, empty);
                    if(!empty) {
                        int index = getIndex() + 1;
                        setText(index + " " + item.getFilenameFull());
                        boolean untaggable = item.isCorrupt() || !item.isFileBased();
                        // mark corrupt items with css style
                        pseudoClassStateChanged(corrupt, untaggable);
                        // mark corrupt item as not taggable
                        // because there is listener on selectedProperty and
                        // we want it to fir eonly as a result of user's action
                        // lock out this change
                        // the untaggable filtering is done during reading
                        lock.set(true);
                        cb.setSelected(!untaggable);
                        lock.set(false);
                        // forbid corrupt items to get tagged
                        cb.setDisable(untaggable);

                        if (getGraphic()==null) setGraphic(cb);
                    } else {
                        // we have to clean up old content
                        setText("");
                        setGraphic(null);
                    }
                }
            };    
            // allow user to de/activate item
            cb.selectedProperty().addListener((o,oldV,newV) -> {
                Item item = cell.getItem();
                // avoid nulls
                // also respect lock
                if(item != null && !lock.get()) {
                    if(newV) items.add(item);
                    else items.remove(item);
                }
            });
            return cell;
        };
        return editCellFactory;
    }
    private Callback<ListView<Item>,ListCell<Item>> getDefCellFactory() {
        if (defCellFactory == null) defCellFactory = listView -> {
            return new ListCell<Item>() {
                @Override 
                protected void updateItem(Item item, boolean empty) {
                    super.updateItem(item, empty);
                    if(!empty) {
                        int index = getIndex()+1;
                        setText(index + " " + item.getFilenameFull());
                        boolean iscorrupt = item.isCorrupt() || !item.isFileBased();
                        // mark corrupt items with css style
                        pseudoClassStateChanged(corrupt, iscorrupt);
                    } else
                        setText("");
                }
            };
        };
        return defCellFactory;
    }
    
    private final EventHandler<DragEvent> drag_dropped_handler = e -> {
        List<Item> dropped = DragUtil.getAudioItems(e);
        //end drag transfer
        e.setDropCompleted(true);
        e.consume();
        // handle result - read data
        if (!dropped.isEmpty()) {
            if (changeReadModeOnTransfer) setReadMode(ReadMode.CUSTOM);
            read(dropped);
        }
    };
    
    
}