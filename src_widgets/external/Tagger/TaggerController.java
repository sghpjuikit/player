package Tagger;


import AudioPlayer.Player;
import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.Playlist;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.playlist.SimpleItem;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.MetadataReader;
import AudioPlayer.tagging.MetadataWriter;
import Configuration.Configuration;
import Configuration.IsConfig;
import GUI.DragUtil;
import GUI.NotifierManager;
import GUI.ItemHolders.ItemTextFields.MoodTextField;
import GUI.objects.PopOver.PopOver.NodeCentricPos;
import GUI.objects.Thumbnail;
import Layout.Widgets.FXMLController;
import Layout.Widgets.SupportsTagging;
import Layout.Widgets.Widget;
import Layout.Widgets.WidgetInfo;
import PseudoObjects.ReadMode;
import PseudoObjects.TODO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javafx.beans.InvalidationListener;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.effect.BoxBlur;
import javafx.scene.image.Image;
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
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import org.controlsfx.validation.ValidationResult;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.decoration.GraphicValidationDecoration;
import org.jaudiotagger.tag.images.Artwork;
import utilities.FileUtil;
import utilities.ImageFileFormat;
import utilities.Log;
import utilities.Util;

/**
 * TaggerController graphical component.
 * <p>
 * Can read and write metadata from/into files.
 * Currently supports files only. File types are limited to those supported
 * by the application.
 * <p>
 * @author Plutonium_
 */
@TODO("cover support + support url and other types if it should be supported")
@WidgetInfo(author = "Martin Polakovic",
            description = "song tag editor",
            version = "0.7",
            year = "2014",
            group = Widget.Group.TAGGER)
public class TaggerController extends FXMLController implements SupportsTagging {
    
    AnchorPane ROOT = new AnchorPane();
    @FXML AnchorPane entireArea;
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
    List<Metadata> metadatas = new ArrayList<>();                   // currently open
    Task<List<Metadata>> loader;
    final List<TagField> fields = new ArrayList<>();
    boolean writing = false;    // prevents external data chagnge during writing
    
    // listeners
    private final InvalidationListener playlistListener = o -> 
            read(PlaylistManager.getSelectedItems());
    private final InvalidationListener playingListener = o -> 
            populate(Collections.singletonList(Player.getCurrentMetadata()));
    
    @Override
    public void init() {
        CoverV = new Thumbnail(200);
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
        
        
        ValidationSupport val = new ValidationSupport();
        val.setValidationDecorator(new GraphicValidationDecoration());
        // year validation
        val.registerValidator(YearF, (Control c, String newV) -> ValidationResult.fromErrorIf(
            YearF, "Year must be greater than 0 and not greater than current year.",
            !newV.isEmpty() && !isValidYear(newV))
        );
        // cd validation
        DiscsTotalF.textProperty().addListener(o->{
            String old = DiscF.getText();
            DiscF.setText("");old+="";
            DiscF.setText(old);
        });
        val.registerValidator(DiscF, (Control c, String newV) -> ValidationResult.fromErrorIf(
            DiscF, "Disc.",
            !newV.isEmpty() && (!isValidInt(newV) || (isValidInt(newV)&&new Integer(newV)<0) ||
                    (isValidInt(newV)&&(isValidInt(DiscsTotalF.getText())&& new Integer(newV)>Integer.parseInt(DiscsTotalF.getText())))
                    )));
        // rating validation
        val.registerValidator(RatingF, (Control c, String newV) -> ValidationResult.fromErrorIf(
            RatingF, "Rating must be between 0 and max value.",
            !newV.isEmpty() && !isValidRating(RatingPF.getText()))
        );
        val.registerValidator(RatingPF, (Control c, String newV) -> ValidationResult.fromErrorIf(
            RatingPF, "Rating must be between 0 and 1.",
            !newV.isEmpty() && !isValidRating(newV))
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
        entireArea.setOnDragOver( t -> {
            Dragboard db = t.getDragboard();
            if (db.hasFiles() || db.hasUrl() || db.hasContent(DragUtil.playlist) || db.hasContent(DragUtil.items))
                t.acceptTransferModes(TransferMode.ANY);
            t.consume();
        });
        entireArea.setOnDragDropped( t -> {
            Dragboard db = t.getDragboard();
            ArrayList<Item> items = new ArrayList<>();
            if (db.hasFiles()) {
                FileUtil.getAudioFiles(db.getFiles(), 1).stream().map(SimpleItem::new).forEach(items::add);
            } else if (db.hasUrl()) {
                //support later
            } else if (db.hasContent(DragUtil.playlist)) {
                Playlist pl = DragUtil.getPlaylist(db);
                items.addAll(pl.getItems());
            } else if (db.hasContent(DragUtil.items)) {
                List<Item> pl = DragUtil.getItems(db);
                items.addAll(pl);
            }
            // read data
            if (changeReadModeOnTransfer) setReadMode(ReadMode.CUSTOM);
            if (!items.isEmpty())
                readData(items);
            
            //end drag transfer
            t.consume();
        });
        
        // add image on drag & drop
        CoverV.getPane().setOnDragOver( t -> {
            if (t.getDragboard().hasFiles())
                t.acceptTransferModes(TransferMode.ANY);
        });
        CoverV.getPane().setOnDragDropped( t -> {
            Dragboard d = t.getDragboard();
            if (d.hasFiles())
                d.getFiles().stream()
                            .filter(ImageFileFormat::isSupported)
                            .limit(1).forEach(this::addImg);
        });
        
        // add cover on click
        coverContainer.setOnMouseClicked(e-> {
            if (e.getButton()!=PRIMARY || isEmpty()) return;
            FileChooser fc = new FileChooser();
                        fc.setInitialDirectory(new File("").getAbsoluteFile()); // ?
                        fc.setTitle("Select image to add to tag");
                        fc.getExtensionFilters().addAll(ImageFileFormat.extensions()
                          .stream().map(ext->new FileChooser.ExtensionFilter( ext,ext))
                          .collect(Collectors.toList()));
            File f = fc.showOpenDialog(entireArea.getScene().getWindow());
            if (f!= null) addImg(f);
        });
        
        // remove cover on drag exit
        CoverV.getPane().setOnDragDetected( e -> CoverV.getPane().startFullDrag());
        entireArea.addEventFilter(MOUSE_DRAG_RELEASED, e-> {
            Point2D click = CoverV.getPane().sceneToLocal(e.getSceneX(),e.getSceneY());
            if(e.getGestureSource().equals(CoverV.getPane()) && !CoverV.getPane().contains(click)) {
                addImg(null);
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
            playingListener.invalidated(null);
        }
        else if (readMode==ReadMode.LIBRARY_SELECTED){
            // to implement
        }
        else if (readMode==ReadMode.CUSTOM)
            readData(metadatas);
        
        fields.forEach(f->f.setVerticalAlignment(field_text_alignment));
        MoodF.setPos(popupPos);
    }
    
    public void setReadMode(ReadMode val) {
        readMode = val;
        if (val == ReadMode.CUSTOM) metadatas.clear();
        refresh();
    }
    
    /**
     * This widget is empty if it has no data.
     * @return 
     */
    @Override
    public boolean isEmpty() {
        // the assumption is that this widget will always display data if present
        return metadatas.isEmpty();
    }
    
    @Override
    public void OnClosing() {
        // remove listeners
        PlaylistManager.getSelectedItems().removeListener(playlistListener);
        Player.currentMetadataProperty().removeListener(playingListener);
    }
    
    
    
/******************************************************************************/
    
    /**
     * Convenience method for single item reading. For specifics read the
     * documentation for the other more general read method.
     * @throws NullPointerException if param null
     */    
    @Override
    public void read(Item item) {
        Objects.requireNonNull(item);
        read(Collections.singletonList(item));
    }
    
    /**
     * Reads metadata on provided items and fills the data for tagging. If items
     * contains Metadata themselves, they will not be read, but their data will
     * be immediately used.
     * @param items Since Metadata extends Item it can also be passed
     * as argument. The list MUST have elements of the exactly same type.
     * @throws NullPointerException if param null
     */    
    @Override
    public void read(List<? extends Item> items) {
        if (writing) return;
        if (!items.isEmpty() && items.get(0) instanceof Metadata) 
            populate((List<Metadata>)items);
        else
            readData(items);
    }
    
/******************************************************************************/

    private void readData(List<? extends Item> items) {
        if (writing) return;
        
        // cancel previous reading if ongoing
        if (loader != null && loader.isRunning())
            loader.cancel();
        
        // read new data
        loader = MetadataReader.readMetadata(items,
            result -> {
                populate(result);
                hideProgress();
            },
            () ->{
                populate(null);
                hideProgress();
                Log.mess("Tagger: Metadata reading failed.");
            }
        );
        showProgressReading();
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
        progressI.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        
        // writing
        metadatas.stream().map(MetadataWriter::create).forEach(writer -> {
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
            
            writer.commit();
        });
        
        // post writing
        writing = false;
        NotifierManager.showTextNotification("Tagging complete", "Tagger");
        readData(metadatas);
    }
    
/******************************************************************************/
    
    /** use null to clear gui empty. */
    private void populate(List<Metadata> items) {
        // return if writing active
        if (writing) return;
        
        // empty previous content
        fields.forEach(TagField::emptyContent);
        CoverV.loadImage((Image)null);  // clear cover
        CoverL.setUserData(false);
        new_cover_file = null;
        infoL.setText("No items loaded");
        
        // return if no new content
        if (items == null || items.isEmpty()) return;
        
        //populate
        metadatas.clear();              // delete previous
        metadatas.addAll(items);        // add new
        
        fields.forEach(TagField::enable);
        
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
        int Cover = 0;
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
        String CoverInfoS = "";
        Artwork CoverS = null;
        
        // check multiple values to determine general field values
        // the condition goes like this (for every field):
        // -- initializes at 0 = no value streak
        // -- if first value not empty -> break no value streak, start same value streak, otherwise continue
        //    (either all will be empty(0) or all will be same(1) - either way first value determines 0/1 streak)
        // -- if empty streak and (non first) value not empty -> conclusion = multiple values
        //    (empty and non empty values = multiple)
        // -- if same value streak and different value -> conclusion = multiple values
        // -- otherwise this ends as no value or same streak decided by 1st value
        for(Metadata m: metadatas) {
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
            if (i==0 && m.getCoverAsArtwork() != null)                          { Cover = 1; CoverS = m.getCoverAsArtwork(); CoverInfoS = m.getCoverInfo(); }
            if (Cover == 0 && i != 0 && m.getCoverAsArtwork() != null)          { Cover = 2; CoverS = m.getCoverAsArtwork(); CoverInfoS = m.getCoverInfo(); }
            if (Cover == 1 && !Util.equals(m.getCoverAsArtwork(), CoverS))      { Cover = 2; } 
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
        
        // set image info
             if (Cover == 0)    CoverL.setText(TAG_NO_VALUE);
        else if (Cover == 1)    CoverL.setText(CoverInfoS);
        else if (Cover == 2)    CoverL.setText(TAG_MULTIPLE_VALUE);
             
        // set image
        if (Cover == 1)         CoverV.loadImage(getImage(CoverS));
        else                    CoverV.loadImage((Image)null);
        
        // enable/disable playcount field
        PlaycountF.setDisable(!allow_playcount_change);
        
        // set info
        if (items.size()==1) infoL.setText("Single item loaded");
        else if (items.size() >1) infoL.setText(items.size() + " items loaded");      
    }
    
    private void showProgressReading() {
        progressI.progressProperty().bind(loader.progressProperty());
        progressPane.setVisible(true);
        entireArea.setEffect(new BoxBlur(5, 5, 1));
    }
    private void showProgressWriting() {
        progressI.progressProperty().unbind();
        progressI.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        progressPane.setVisible(true);
        entireArea.setEffect(new BoxBlur(5, 5, 1));
    }
    private void hideProgress() {
        progressPane.setVisible(false);
        entireArea.setEffect(null);
    }
        
    private void addImg(File f) {
        if (isEmpty()) return;
        
        new_cover_file = ImageFileFormat.isSupported(f) ? f : null;
        if (new_cover_file != null) {
            CoverV.loadImage(new_cover_file);
            CoverL.setText(ImageFileFormat.formatOf(new_cover_file.toURI()) + " "
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
    private Image getImage(Artwork a) {
        try {
            return SwingFXUtils.toFXImage((BufferedImage)a.getImage(), null);
        } catch (IOException ex) {
            Log.err("Error while displaying tag image");
            return null;
        }
    }
    
    
/******************************************************************************/
    
    private final class TagField {
        private final TextInputControl c;
        
        public TagField(TextInputControl control) { 
            c = control;
            
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
    
    
    private boolean isValidInt(String val) {
        try {
            int i = new Integer(val);
            return true;
        } catch(NumberFormatException e) {
            return false;
        }
    }
    private boolean isValidYear(String val) {
        try {
            int i = new Integer(val);
            int max = Year.now().getValue();
            return i>0 && i<=max;
        } catch(NumberFormatException e) {
            return false;
        }
    }
    private boolean isValidRating(String val) {
        try {
            double i = new Double(val);
            return i>=0 && i<=1;
        } catch(NumberFormatException e) {
            return false;
        }
    }
}