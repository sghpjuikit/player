package FileInfo;


import AudioPlayer.Player;
import AudioPlayer.playlist.Item;
import AudioPlayer.tagging.Cover.Cover;
import AudioPlayer.tagging.Cover.Cover.CoverSource;
import static AudioPlayer.tagging.Cover.Cover.CoverSource.ANY;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.MetadataWriter;
import Configuration.IsConfig;
import GUI.DragUtil;
import GUI.Panes.ImageFlowPane;
import GUI.objects.Rater.Rating;
import GUI.objects.Thumbnail;
import Layout.Widgets.FXMLController;
import Layout.Widgets.Widget;
import PseudoObjects.ReadMode;
import static PseudoObjects.ReadMode.PLAYING;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import static javafx.geometry.Orientation.VERTICAL;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.geometry.Pos.TOP_LEFT;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import static javafx.scene.control.OverrunStyle.ELLIPSIS;
import javafx.scene.image.Image;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.TilePane;
import main.App;
import org.reactfx.Subscription;
import utilities.FileUtil;
import utilities.Util;
import utilities.access.Accessor;

/**
 * File info widget controller.
 * <p>
 * @author Plutonium_
 */
@Widget.Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "File Info",
    description = "Displays information about a song and cover. Supports rating change.",
    howto = "Available actions:\n" +
            "    Cover left click : Toggles show fields\n" +
            "    Cover left click : Opens over context menu\n" +
            "    Rater left click : Rates displayed song\n" +
            "    Rater right click : Toggles rater skin\n" +
            "    Drag&Drop audio : Displays information for the first dropped item\n" +
            "    Drag&Drop image : Copies images into current item's locaton\n",
    version = "1.0",
    year = "2014",
    group = Widget.Group.OTHER
)
public class FileInfoController extends FXMLController  {
    
    @FXML AnchorPane entireArea;
    ImageFlowPane layout;
    TilePane tiles = new TilePane(VERTICAL,10,0);
    
    private final Label title = new Label(); 
    private final Label track = new Label(); 
    private final Label disc = new Label(); 
    private final Label gap1 = new Label(); 
    private final Label artist = new Label(); 
    private final Label album = new Label();    
    private final Label album_artist = new Label(); 
    private final Label year = new Label(); 
    private final Label genre = new Label(); 
    private final Label composer = new Label(); 
    private final Label publisher = new Label(); 
    private final Label gap2 = new Label(); 
    private final Label rating = new Label(); 
    private final Label playcount = new Label(); 
    private final Label comment = new Label(); 
    private final Label category = new Label(); 
    private final Label gap3 = new Label(); 
    private final Label filesize = new Label(); 
    private final Label filename = new Label(); 
    private final Label format = new Label(); 
    private final Label bitrate = new Label(); 
    private final Label encoding = new Label(); 
    private final Label location = new Label(); 
    private final Rating rater = new Rating();
    
    private List<Label> labels;
    private final List<Label> visible_labels = new ArrayList();
    private final ChangeListener<Number> tileResizer = (o,ov,nv) -> resize(nv.doubleValue());
    private Subscription dataMonitoring;
    private Metadata data;
    
    // auto applied configurable values    
    @IsConfig(name = "Column width", info = "Minimal width for field columns.")
    public final Accessor<Double> minColumnWidth = new Accessor<>(150.0, v -> resize(tiles.getWidth()));
    @IsConfig(name = "Cover source", info = "Source for cover image.")
    public final Accessor<CoverSource> cover_source = new Accessor<>(ANY, this::setCover);
    @IsConfig(name = "Rating editable", info = "Allow change of rating. Defaults to application settings")
    public final Accessor<Boolean> editableRating = new Accessor<>(App.allowRatingChange, rater::setEditable);
    @IsConfig(name = "Rating stars number", info = "Number of stars for rating. Rating value is recalculated accordingly. Defaults to application settings")
    public final Accessor<Integer> maxRating = new Accessor<>(App.maxRating, rater::setMax);
    @IsConfig(name = "Rating allow partial", info = "Allow partial values for rating. Defaults to application settings")
    public final Accessor<Boolean> partialRating = new Accessor<>(App.partialRating, rater::setPartialRating);
    @IsConfig(name = "Rating react on hover", info = "Move rating according to mouse when hovering. Defaults to application settings")
    public final Accessor<Boolean> hoverRating = new Accessor<>(App.hoverRating, rater::setUpdateOnHover);
    @IsConfig(name = "Rating skin", info = "Rating skin.", editable = false)
    public final Accessor<String> rating_skin = new Accessor<>("",rater::setSkinCurrent);
    @IsConfig(name = "Overrun style", info = "Style of clipping fields' text when outside of the area.")
    public final Accessor<OverrunStyle> overrun_style = new Accessor<>(ELLIPSIS, v -> labels.forEach(l->l.setTextOverrun(v)));
    @IsConfig(name = "Display cover", info = "Show cover.")
    public final Accessor<Boolean> showCover = new Accessor<>(true, this::setCoverVisible);
    @IsConfig(name = "Display fields", info = "Show fields.")
    public final Accessor<Boolean> showFields = new Accessor<>(true, v -> layout.setShowContent(v));
    @IsConfig(name = "Item source", info = "Source of data for the widget.")
    public final Accessor<ReadMode> readMode = new Accessor<>(PLAYING, v -> dataMonitoring = Player.bindObservedMetadata(v,dataMonitoring, this::populateGui));
    @IsConfig(name = "Display empty fields", info = "Show empty fields.")
    public final Accessor<Boolean> showEmptyFields = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Separate fields by group", info = "Separate fields by gap to group them.")
    public final Accessor<Boolean> groupFields = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show title", info = "Show this field.")
    public final Accessor<Boolean> showTitle = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show track", info = "Show this field.")
    public final Accessor<Boolean> showtrack = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show disc", info = "Show this field.")
    public final Accessor<Boolean> showdisc = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show artist", info = "Show this field.")
    public final Accessor<Boolean> showartist = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show album artist", info = "Show this field.")
    public final Accessor<Boolean> showalbum_artist = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show album", info = "Show this field.")
    public final Accessor<Boolean> showalbum = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show year", info = "Show this field.")
    public final Accessor<Boolean> showyear = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show genre", info = "Show this field.")
    public final Accessor<Boolean> showgenre = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show composer", info = "Show this field.")
    public final Accessor<Boolean> showcomposer = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show publisher", info = "Show this field.")
    public final Accessor<Boolean> showpublisher = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show rating", info = "Show this field.")
    public final Accessor<Boolean> showrating = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show playcount", info = "Show this field.")
    public final Accessor<Boolean> showplaycount = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show comment", info = "Show this field.")
    public final Accessor<Boolean> showcomment = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show category", info = "Show this field.")
    public final Accessor<Boolean> showcategory = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show filesize", info = "Show this field.")
    public final Accessor<Boolean> showfilesize = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show filename", info = "Show this field.")
    public final Accessor<Boolean> showfilename = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show format", info = "Show this field.")
    public final Accessor<Boolean> showformat = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show bitrate", info = "Show this field.")
    public final Accessor<Boolean> showbitrate = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show encoding", info = "Show this field.")
    public final Accessor<Boolean> showencoding = new Accessor<>(true, v -> setVisibility());
    @IsConfig(name = "Show location", info = "Show this field.")
    public final Accessor<Boolean> showlocation = new Accessor<>(true, v -> setVisibility());
    
    // manually applied configurable values
    
    // non appliable configurable values
    @IsConfig(name = "Set custom item source on drag&drop", info = "Change read mode to CUSTOM when data are dragged to widget.")
    public boolean changeReadModeOnTransfer = true;
    @IsConfig(name = "Show previous content when empty", info = "Keep showing previous content when the new content is empty.")
    public boolean keepContentOnEmpty = true;
    @IsConfig(name = "Copy image as cover", info = "Drag & dropped images will be"
            + " renamed to be used as cover for the album instead of simply added to the directory. Old cover will be preserved.")
    public boolean copyImageAsCover = true;
    
    @Override
    public void init() {
        // initialize gui
        Thumbnail thumb = new Thumbnail();
                  thumb.setBorderToImage(true);
                  
        layout = new ImageFlowPane(entireArea, thumb);
        layout.setMinContentWidth(200);
        layout.setMinContentHeight(120);
        layout.setGap(5);
        
        // set autosizing for tiles to always fill the grid entirely
        tiles.widthProperty().addListener(tileResizer);
        tiles.heightProperty().addListener(tileResizer);
        
        // alight tiles from left top & tile content to center left
        tiles.setAlignment(TOP_LEFT);
        tiles.setTileAlignment(CENTER_LEFT);
        
        // add rater stars to rating label as graphics
        rating.setGraphic(rater);
        rating.setContentDisplay(ContentDisplay.RIGHT);
        
        // grab fields
        labels = Arrays.asList( title, track, disc, gap1,
            artist, album, album_artist, year, genre, composer, publisher, gap2,
            rating, playcount, comment, category, gap3,
            filesize, filename, format, bitrate, encoding, location);
        
        layout.addChild(tiles);
        Util.setAPAnchors(tiles, 0);
        
        
        // write metadata on rating change
        rater.setOnRatingChanged( r -> MetadataWriter.rate(data, r));
        
        // swap skin on right mouse click
        rater.setOnMouseClicked( e -> { 
            if (e.getButton() == SECONDARY) rater.toggleSkin(); 
        });
        // remember changed rating
        rater.setOnSkinChanged(rating_skin::setValue);
        // hide rating if empty
        rater.visibleProperty().bind(rating.disabledProperty().not());
        
        // show/hide content on cover mouse click
        thumb.getPane().setOnMouseClicked( e -> {
            if (e.getButton() == PRIMARY) {
                layout.toggleShowContent();
                showFields.setValue(layout.isShowContent());
            }
        });
        
        
        // accept drag transfer
        entireArea.setOnDragOver(DragUtil.audioDragAccepthandler);
        entireArea.setOnDragOver(DragUtil.imageFileDragAccepthandler);
        // handle drag transfers
        entireArea.setOnDragDropped( e -> {
            if(DragUtil.hasAudio(e.getDragboard())) {
                // get first item
                List<Item> items = DragUtil.getAudioItems(e);
                // getMetadata, refresh
                if (!items.isEmpty()) {
                    // change mode if desired
                    if (changeReadModeOnTransfer) readMode.setNapplyValue(ReadMode.CUSTOM);
                    // set data
                    populateGui(items.get(0).getMetadata());
                }
                // end drag
                e.setDropCompleted(true);
                e.consume();
            }
            if(data!=null && data.isFileBased() && DragUtil.hasImage(e.getDragboard())) {
                // grab images
                DragUtil.doWithImageItems(e, imgs ->  {
                    if(copyImageAsCover) {
                        if(!imgs.isEmpty()) {
                            // copy files to displayed item's location & preserve old
                            FileUtil.copyFileSafe(imgs.get(0), data.getLocation(), "cover");
                            // refres picture
                            cover_source.applyValue();
                        }
                    } else {
                        // copy files to displayed item's location
                        FileUtil.copyFiles(imgs, data.getLocation());
                    }
                });
                // end drag
                e.setDropCompleted(true);
                e.consume();
            }
        });
    }
    
    @Override
    public void OnClosing() {
        if (dataMonitoring != null) dataMonitoring.unsubscribe();
    }
    
/********************************* PUBLIC API *********************************/
 
    @Override
    public void refresh() {
        // apply configurables
        readMode.applyValue();
        minColumnWidth.applyValue();
        cover_source.applyValue();
        editableRating.applyValue();
        maxRating.applyValue();
        partialRating.applyValue();
        hoverRating.applyValue();
        rating_skin.applyValue();
        overrun_style.applyValue();
        showCover.applyValue();
        showFields.applyValue();
    }
    
    @Override
    public boolean isEmpty() {
        return data == null;
    }
    
/****************************** HELPER METHODS ********************************/
    
    private void populateGui(Metadata m) {
        // prevent refreshing location if shouldnt
        if(keepContentOnEmpty && m==null) return;
        
        // remember data
        data = m;
        // gui (fill out data)
        if (m == null) {
            clear();
        } else {
            // set image
            setCover(cover_source.getValue());

            // set rating
            rater.setRating(m.getRatingToStars(rater.getMax()));

            // set other fields
            title.setText("title: "         + m.getTitle());
            track.setText("track: "         + m.getTrackInfo());
            disc.setText("disc: "           + m.getDiscInfo());
            gap1.setText(" ");
            artist.setText("artist: "       + m.getArtist());
            album.setText("album: "         + m.getAlbum());
            album_artist.setText("album artist: " + m.getAlbumArtist());
            year.setText("year: "           + m.getYear());
            genre.setText("genre: "         + m.getGenre());
            composer.setText("composer: "   + m.getComposer());
            publisher.setText("publisher: " + m.getPublisher());
            gap2.setText(" ");
            rating.setText("rating: "       + m.getRatingAsString());
            playcount.setText("playcount: " + m.getPlaycountAsString());
            comment.setText("comment: "     + m.getComment());
            category.setText("category: "   + m.getCategory());
            gap3.setText(" ");
            filesize.setText("filesize: "   + m.getFilesize().toString());
            filename.setText("filename: "   + m.getFilenameFull());
            format.setText("format: "       + m.getFormat());
            bitrate.setText("bitrate: "     + m.getBitrate().toString());
            encoding.setText("encoding: "   + m.getEncoder());
            location.setText("location: "   + m.getPath());
        } 
        
        // manually applied c.v.
        setVisibility();
    }
    
    private void clear() {
        rater.setRating(0d);
        title.setText("title: ");
        track.setText("track: ");
        disc.setText("disc: ");
        gap1.setText(" ");
        artist.setText("artist: ");
        album.setText("album: ");
        album_artist.setText("album artist: ");
        year.setText("year: ");
        genre.setText("genre: " );
        composer.setText("composer: ");
        publisher.setText("publisher: ");
        gap2.setText(" ");
        rating.setText("rating: ");
        playcount.setText("playcount: ");
        comment.setText("comment: " );
        category.setText("category: ");
        gap3.setText(" ");
        filesize.setText("filesize: ");
        filename.setText("filename: ");
        format.setText("format: ");
        bitrate.setText("bitrate: ");
        encoding.setText("encoding: ");
        location.setText("location: ");
    }
    
    private void setVisibility() {        
        // initialize
        visible_labels.clear();
        visible_labels.addAll(labels);
        tiles.getChildren().clear();
        visible_labels.forEach(l -> l.setDisable(false));

        // disable empty fields
        if (showEmptyFields.getValue()) {
            visible_labels.stream().filter(l->{
                    // filter out nonempty
                    String content = l.getText().substring(l.getText().indexOf(": ")+2).trim();
                    return content.isEmpty() || 
                             content.equalsIgnoreCase("?/?") ||
                               content.equalsIgnoreCase("n/a") || 
                                 content.equalsIgnoreCase("unknown");
                })
                .forEach(l->l.setDisable(true));
        } else {
            labels.stream()
                .filter(ll -> ll.getText().substring(ll.getText().indexOf(": ")+1).equals(" "))
                .forEach(visible_labels::remove);
        }
        // never disable rating, we want to be able to set the value
        rating.setDisable(false);
        
        // hide individual fields
        if (!showTitle.getValue())         visible_labels.remove(title);
        if (!showtrack.getValue())         visible_labels.remove(track);
        if (!showdisc.getValue())          visible_labels.remove(disc);
        if (!showartist.getValue())        visible_labels.remove(artist);
        if (!showalbum_artist.getValue())  visible_labels.remove(album_artist);
        if (!showalbum.getValue())         visible_labels.remove(album);
        if (!showyear.getValue())          visible_labels.remove(year);
        if (!showgenre.getValue())         visible_labels.remove(genre);
        if (!showcomposer.getValue())      visible_labels.remove(composer);
        if (!showpublisher.getValue())     visible_labels.remove(publisher);
        if (!showrating.getValue())        visible_labels.remove(rating);
        if (!showplaycount.getValue())     visible_labels.remove(playcount);
        if (!showcomment.getValue())       visible_labels.remove(comment);
        if (!showcategory.getValue())      visible_labels.remove(category);
        if (!showfilesize.getValue())      visible_labels.remove(filesize);
        if (!showformat.getValue())        visible_labels.remove(format);
        if (!showfilename.getValue())      visible_labels.remove(filename);
        if (!showbitrate.getValue())       visible_labels.remove(bitrate);
        if (!showencoding.getValue())      visible_labels.remove(encoding);
        if (!showlocation.getValue())      visible_labels.remove(location);
        
        // hide separators
        if (!groupFields.getValue()) {
            visible_labels.remove(gap1);
            visible_labels.remove(gap2);
            visible_labels.remove(gap3);
        }
        
        // show remaining
        tiles.getChildren().addAll(visible_labels);
        
        // fix rating value (we have to set text anyway to be able to tell, if
        // rating is empty (same way the other labels)
        // in the end we must remove the text because we use stars instead
        rating.setText("rating: ");
    }
    
    private void setCover(CoverSource source) {
        // get image
        if (data == null) {
            layout.setImage((Image)null); // clear
        } else {
            Cover c = data.getCover(source);
            if(c.getFile()!=null) layout.setImage(c.getFile());
            else layout.setImage(c.getImage());
        }
    }
    
    private void setCoverVisible(boolean v) {
        layout.setShowImage(v);        
    }
    
    private void resize(double width) {
        double cellH = 15+tiles.getVgap();
        int rowsize = (int)Math.floor(Math.max(tiles.getHeight(), 5)/cellH);
            if(rowsize==0) rowsize=1;
        int columns = 1+(int) Math.ceil(visible_labels.size()/rowsize);
        double cellW = columns==1 || columns==0 
            // dont allow 0 columns & set whole width if 1 column
            // handle 1 column manually - the below caused some problems
            ? tiles.getWidth()
            // for n elements there is n-1 gaps so we need to add 1 gap width
            // above cell width includes 1 gap width per element so substract it
            : (width + tiles.getHgap())/columns - tiles.getHgap();
        
        // adhere to requested minimum size
        cellW = Math.max(cellW, minColumnWidth.getValue());
        
        tiles.setPrefTileWidth(cellW);
    }
    
}