
package Tagger;

import AudioPlayer.Player;
import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.services.Notifier.Notifier;
import AudioPlayer.tagging.Cover.Cover;
import static AudioPlayer.tagging.Cover.Cover.CoverSource.TAG;
import AudioPlayer.tagging.Metadata;
import static AudioPlayer.tagging.Metadata.Field.*;
import AudioPlayer.tagging.MetadataReader;
import AudioPlayer.tagging.MetadataWriter;
import Configuration.IsConfig;
import GUI.DragUtil;
import GUI.ItemHolders.ItemTextFields.MoodTextField;
import GUI.objects.GraphicalTextField;
import GUI.objects.PopOver.PopOver;
import GUI.objects.PopOver.PopOver.NodeCentricPos;
import static GUI.objects.PopOver.PopOver.NodeCentricPos.DownCenter;
import GUI.objects.Text;
import GUI.objects.Thumbnail;
import Layout.Widgets.FXMLController;
import Layout.Widgets.Features.TaggingFeature;
import Layout.Widgets.Widget;
import PseudoObjects.ReadMode;
import static PseudoObjects.ReadMode.*;
import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import static de.jensd.fx.fontawesome.AwesomeIcon.TAGS;
import java.io.File;
import java.util.*;
import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.singletonList;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
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
import static javafx.scene.input.KeyCode.BACK_SPACE;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseDragEvent.MOUSE_DRAG_RELEASED;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;
import main.App;
import static org.atteo.evo.inflector.English.plural;
import org.controlsfx.control.textfield.CustomTextField;
import org.reactfx.Subscription;
import util.File.AudioFileFormat;
import util.File.AudioFileFormat.Use;
import util.File.Enviroment;
import util.File.ImageFileFormat;
import util.InputConstraints;
import util.Parser.ParserImpl.ColorParser;
import static util.Util.createIcon;
import util.access.Accessor;
import util.dev.Log;
import static util.functional.impl.Validator.*;

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
    year = "2014",
    group = Widget.Group.TAGGER)
public class TaggerController extends FXMLController implements TaggingFeature {
    
    @FXML AnchorPane entireArea;
    @FXML ScrollPane scrollPaneContent;
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
    @FXML Label CoverL;
    Thumbnail CoverV;
    File new_cover_file = null;
    @FXML StackPane progressPane;
    @FXML ProgressIndicator progressI;
    @FXML Label infoL;
    @FXML Rectangle img_border;
    @FXML AnchorPane img_borderContainer;
    
    
    //global variables
    ObservableList<Item> allitems = FXCollections.observableArrayList();
    ObservableList<Item> items = FXCollections.observableArrayList();
    ObservableList<Metadata> metas = FXCollections.observableArrayList();   // currently active
    Task<List<Metadata>> loader;
    final List<TagField> fields = new ArrayList<>();
    boolean writing = false;    // prevents external data chagnge during writing
    Task<List<Metadata>> metaReader;
    private final List<Validation> validators = new ArrayList();
        
    // listeners
    private Subscription playingItemMonitoring;
    private Subscription selectedItemsMonitoring;
    private final Consumer<List<PlaylistItem>> playlistListener = selectedItems -> 
            read(selectedItems);
    private final Consumer<Metadata> playingListener = item ->
            read(singletonList(item));
    
    // properties
    @IsConfig(name = "Field text alignement", info = "Alignment of the text in fields.")
    public final Accessor<Pos> field_text_alignment = new Accessor<>(CENTER_LEFT, v->fields.forEach(f->f.setVerticalAlignment(v)));
    @IsConfig(name="Mood picker popup position", info = "Position of the mood picker pop up relative to the mood text field.")
    public final Accessor<NodeCentricPos> popupPos = new Accessor<>(DownCenter, MoodF::setPos);
    @IsConfig(name = "Read Mode", info = "Source of data for the widget.")
    public final Accessor<ReadMode> readMode = new Accessor<>(CUSTOM, this::apllyReadMode);
    @IsConfig(name = "Allow change of playcount", info = "Change editability of playcount field. Generally to prevent change to non customary values.")
    public final Accessor<Boolean> allow_playcount_change = new Accessor<>(false, v -> {
        if(!isEmpty()) PlaycountF.setDisable(!v);
    });

    @IsConfig(name = "Read mode change on drag", info = "Change read mode to CUSTOM when data are arbitrary added to widget.")
    public Boolean changeReadModeOnTransfer = false;
    @IsConfig(name = "No value text", info = "Field text in case no tag value for field.")
    public String TAG_NO_VALUE = App.TAG_NO_VALUE;
    @IsConfig(name = "Multiple value text", info = "Field text in case multiple tag values per field.")
    public String TAG_MULTIPLE_VALUE = App.TAG_MULTIPLE_VALUE;


    
    private void apllyReadMode(ReadMode v) {
        if(playingItemMonitoring!=null) playingItemMonitoring.unsubscribe();
        if(selectedItemsMonitoring!=null) selectedItemsMonitoring.unsubscribe();
        
        // rebind
        if (v == SELECTED_PLAYLIST) {            
            selectedItemsMonitoring = PlaylistManager.selectedItemsES.subscribe(playlistListener);
            playlistListener.accept(PlaylistManager.selectedItemsES.getValue());
        } else
        if (v == PLAYING){
            playingItemMonitoring = Player.playingtem.subscribeToUpdates(playingListener);
            playingListener.accept(Player.playingtem.get());
        } else
        if (v==SELECTED_LIBRARY){
            selectedItemsMonitoring = Player.librarySelectedItemsES.subscribe(list->{
                this.allitems.setAll(list);
                populate(list);
            });
        } else
        if (v==SELECTED_ANY){
            selectedItemsMonitoring = Player.selectedItemsES.subscribe(list->{
                this.allitems.setAll(list);
                populate(list);
            });
        } else
        if (v==CUSTOM)
            read(EMPTY_LIST);
    }
    
    
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
                        .filter(i -> !i.isCorrupt(Use.DB) && i.isFileBased())
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
                            Log.info("Tagger: Metadata reading succeeded.");
                        } else {
                            Log.info("Tagger: Metadata reading failed.");
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
        fields.add(new TagField(CommentF,COMMENT));
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
        RatingPF.setOnKeyReleased(e -> setR());
        RatingPF.setOnMousePressed(e -> setR());
        
        // show metadata list
        infoL.setOnMouseClicked(e -> showItemsPopup());
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
        // remove listeners
        if (selectedItemsMonitoring!=null) selectedItemsMonitoring.unsubscribe();
        if (playingItemMonitoring!=null) playingItemMonitoring.unsubscribe();
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
        });
        
        // post writing
        hideProgress();
        writing = false;
        App.use(Notifier.class, s->s.showTextNotification("Tagging complete", "Tagger"));
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
        fields.forEach(f -> f.histogramEnd(formats));
        // handle cover separately
            // set image info
             if (coverI == 0)    CoverL.setText(TAG_NO_VALUE);
        else if (coverI == 1)    CoverL.setText(covDesS);
        else if (coverI == 2)    CoverL.setText(TAG_MULTIPLE_VALUE);
            // set image
        if (coverI == 1)         CoverV.loadImage(CovS.getImage());
        else                     CoverV.loadImage((Image)null);

        // enable/disable playcount field
        if(!allow_playcount_change.getValue()) PlaycountF.setDisable(true);
        
        // set info
        infoL.setText(items.size() + " " + plural("item", items.size()) + " loaded.");      
    }
    
    private void showProgressReading() {
//        progressI.progressProperty().bind(loader.progressProperty());
        progressI.setProgress(INDETERMINATE_PROGRESS);
        progressPane.setVisible(true);
        // add blur effect to content to hint inaccessibility
        // note: dont apply on root it would also blur the progres indicator!
        scrollPaneContent.setEffect(new BoxBlur(3, 3, 1));
        // make inaccessible
        scrollPaneContent.setMouseTransparent(true);        
    }
    private void showProgressWriting() {
//        progressI.progressProperty().unbind();
        progressI.setProgress(INDETERMINATE_PROGRESS);
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
                Label l = createIcon(AwesomeIcon.EXCLAMATION_TRIANGLE, 11, null, null);
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
                if (e.getCode() == BACK_SPACE)
                    OnBackspacePressed();
            });
        }
        void enable() { 
            c.setDisable(false);
        }
        void disable() {
            c.setDisable(true);
        }
        public void setEnabled(Collection<AudioFileFormat> formats) {
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
            if      (histogramI == 0)   c.setPromptText(TAG_NO_VALUE);
            else if (histogramI == 1)   c.setPromptText(histogramS);
            else if (histogramI == 2)   c.setPromptText(TAG_MULTIPLE_VALUE);
            
            if(f==CUSTOM1) {
                ColorF.setValue(new ColorParser().fromS(histogramS));                
            }
            
            // remember prompt text
            c.setId(c.getPromptText());
            // disable if unsuported
            setEnabled(formats);
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
                        boolean untaggable = item.isCorrupt(Use.DB) || !item.isFileBased();
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
            cb.selectedProperty().addListener((o,ov,nv) -> {
                Item item = cell.getItem();
                // avoid nulls
                // also respect lock
                if(item != null && !lock.get()) {
                    if(nv) items.add(item);
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
                        boolean iscorrupt = item.isCorrupt(Use.DB) || !item.isFileBased();
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