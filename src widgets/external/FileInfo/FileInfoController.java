package FileInfo;


import AudioPlayer.Player;
import AudioPlayer.playlist.Item;
import AudioPlayer.tagging.Cover.Cover;
import AudioPlayer.tagging.Cover.Cover.CoverSource;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.MetadataWriter;
import Configuration.Configuration;
import Configuration.IsConfig;
import GUI.DragUtil;
import GUI.Panes.ImageFlowPane;
import GUI.objects.Rater.Rating;
import GUI.objects.Thumbnail;
import Layout.Widgets.FXMLController;
import Layout.Widgets.Widget;
import Layout.Widgets.WidgetInfo;
import PseudoObjects.ReadMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import static javafx.geometry.Orientation.VERTICAL;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.geometry.Pos.TOP_LEFT;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.image.Image;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.TilePane;

/**
 * 
 * <p>
 * @author Plutonium_
 */
@WidgetInfo(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "File Info",
    description = "Displays information about a song and cover. Supports rating change.",
    howto = "Available actions:\n" +
            "    Cover click : Toggle cover only mode\n" +
            "    Rater click : Rate displayed song\n" +
            "    Drag&Drop audio : Display information for the first item\n",
    version = "0.9",
    year = "2014",
    group = Widget.Group.OTHER
)
public class FileInfoController extends FXMLController  {
    
    @FXML AnchorPane entireArea;
    ImageFlowPane layout;
    TilePane tiles = new TilePane(VERTICAL,10,0);
    
    Label title = new Label(); 
    Label track = new Label(); 
    Label disc = new Label(); 
    Label gap1 = new Label(); 
    Label artist = new Label(); 
    Label album = new Label();    
    Label album_artist = new Label(); 
    Label year = new Label(); 
    Label genre = new Label(); 
    Label composer = new Label(); 
    Label publisher = new Label(); 
    Label gap2 = new Label(); 
    Label rating = new Label(); 
    Label playcount = new Label(); 
    Label comment = new Label(); 
    Label category = new Label(); 
    Label gap3 = new Label(); 
    Label filesize = new Label(); 
    Label filename = new Label(); 
    Label format = new Label(); 
    Label bitrate = new Label(); 
    Label encoding = new Label(); 
    Label location = new Label(); 
    final Rating rater = new Rating();
    
    private List<Label> labels;
    private final List<Label> visible_labels = new ArrayList();
    private final SimpleObjectProperty<Metadata> meta = new SimpleObjectProperty();
    
    // properties
    @IsConfig(name = "Item source", info = "Source of data for the widget.")
    public ReadMode readMode = ReadMode.PLAYING;
    @IsConfig(name = "Column min width", info = "Minimal width for field columns. Use -1 for automatic width")
    public Double minColumnWidth = 150.0;
    @IsConfig(name = "Cover source", info = "Source for cover image.")
    public CoverSource cover_source = CoverSource.ANY;
    @IsConfig(name = "Change item source on drag&drop", info = "Change read mode to CUSTOM when data are arbitrary added to widget.")
    public boolean changeReadModeOnTransfer = true;
    @IsConfig(name = "Rating editable", info = "Allow change of rating.")
    public boolean editableRating = Configuration.allowRatingChange;
    @IsConfig(name = "Rating stars", info = "Number of stars for rating. Rating value is recalculated accordingly.")
    public int maxRating = Configuration.maxRating;
    @IsConfig(name = "Rating allow partial", info = "Allow partial values for rating.")
    public boolean partialRating = Configuration.partialRating;
    @IsConfig(name = "Rating react on hover", info = "Move rating according to mouse when hovering.")
    public boolean hoverRating = Configuration.hoverRating;
    @IsConfig(name = "Rating skin", info = "Rating skin.")
    public String rating_skin = "";
    @IsConfig(name = "Overrun style", info = "Style of clipping fields' text when outside of the area.")
    public OverrunStyle overrun_style = OverrunStyle.ELLIPSIS;
    @IsConfig(name = "Display cover", info = "Show cover.")
    public boolean showCover = true;
    @IsConfig(name = "Display fields", info = "Show fields.")
    public boolean showFields = true;
    @IsConfig(name = "Display empty fields", info = "Show empty fields.")
    public boolean showEmptyFields = true;
    @IsConfig(name = "Separate fields by group", info = "Separate fields by gap and group them..")
    public boolean groupFields = true;
    @IsConfig(name = "Show title", info = "Show this field.")
    public boolean showTitle = true;
    @IsConfig(name = "Show track", info = "Show this field.")
    public boolean showtrack = true;
    @IsConfig(name = "Show disc", info = "Show this field.")
    public boolean showdisc = true;
    @IsConfig(name = "Show artist", info = "Show this field.")
    public boolean showartist = true;
    @IsConfig(name = "Show album artist", info = "Show this field.")
    public boolean showalbum_artist = true;
    @IsConfig(name = "Show album", info = "Show this field.")
    public boolean showalbum = true;
    @IsConfig(name = "Show year", info = "Show this field.")
    public boolean showyear = true;
    @IsConfig(name = "Show genre", info = "Show this field.")
    public boolean showgenre = true;
    @IsConfig(name = "Show composer", info = "Show this field.")
    public boolean showcomposer = true;
    @IsConfig(name = "Show publisher", info = "Show this field.")
    public boolean showpublisher = true;
    @IsConfig(name = "Show rating", info = "Show this field.")
    public boolean showrating = true;
    @IsConfig(name = "Show playcount", info = "Show this field.")
    public boolean showplaycount = true;
    @IsConfig(name = "Show comment", info = "Show this field.")
    public boolean showcomment = true;
    @IsConfig(name = "Show category", info = "Show this field.")
    public boolean showcategory = true;
    @IsConfig(name = "Show filesize", info = "Show this field.")
    public boolean showfilesize = true;
    @IsConfig(name = "Show filename", info = "Show this field.")
    public boolean showfilename = true;
    @IsConfig(name = "Show format", info = "Show this field.")
    public boolean showformat = true;
    @IsConfig(name = "Show bitrate", info = "Show this field.")
    public boolean showbitrate = true;
    @IsConfig(name = "Show encoding", info = "Show this field.")
    public boolean showencoding = true;
    @IsConfig(name = "Show location", info = "Show this field.")
    public boolean showlocation = true;
    
    ChangeListener<Number> tileResizer = (o,ov,nv) -> {
            int columns = (int) Math.floor((nv.doubleValue())/minColumnWidth);
            double cellW = columns==1 || columns==0 
                // dont allow 0 columns & set whole width if 1 column
                // handle 1 column manually - the below caused some problems
                ? tiles.getWidth()
                // for n elements there is n-1 gaps so we need to add 1 gap width
                // above cell width includes 1 gap width per element so substract it
                : (nv.doubleValue()+tiles.getHgap())/columns - tiles.getHgap();
            tiles.setPrefTileWidth(cellW);
        };
    
    @Override
    public void init() {
        // initialize gui
        Thumbnail thumb = new Thumbnail();
                  thumb.setBorderToImage(true);
                  
        layout = new ImageFlowPane(entireArea, thumb);
        layout.setMinContentWidth(200);
        layout.setMinContentHeight(120);
        layout.setGap(5);
//        tiles.setPrefTileWidth(150);
        
        // set autosizing for tiles to always fill the grid entirely
        tiles.widthProperty().addListener(tileResizer);
        
        // alight tiles from left top & tile content to center left
        tiles.setAlignment(TOP_LEFT);
        tiles.setTileAlignment(CENTER_LEFT);
        
        // add rater stars to rating label as grphics
        rating.setGraphic(rater);
        rating.setGraphicTextGap(8);
        rating.setContentDisplay(ContentDisplay.RIGHT);
        
        // grab fields
        labels = Arrays.asList( title, track, disc, gap1,
            artist, album, album_artist, year, genre, composer, publisher, gap2,
            rating, playcount, comment, category, gap3,
            filesize, filename, format, bitrate, encoding, location);
        
        // add to layout
        tiles.getChildren().addAll(labels);
        
        layout.addChild(tiles);
        AnchorPane.setBottomAnchor(tiles, 0d);
        AnchorPane.setRightAnchor(tiles, 0d);
        AnchorPane.setTopAnchor(tiles, 0d);
        AnchorPane.setLeftAnchor(tiles, 0d);
        
        // refresh if metadata source data changed
        meta.addListener( o -> refreshNoBinding());
        
        // write metadata on rating change
        rater.setOnRatingChanged( e -> MetadataWriter.rate(meta.get(),rater.getRatingPercent()));
        // swap skin on right mouse click
        rater.setOnSkinChanged( e -> rating_skin = rater.getSkinCurrent());
        rater.setOnMouseClicked( e -> { 
            if (e.getButton() == SECONDARY) rater.toggleSkin(); 
        });
        // hide rating if empty
        rater.visibleProperty().bind(rating.disabledProperty().not());
        
        // show/hide content on cover mouse click
        thumb.getPane().setOnMouseClicked( e -> {
            if (e.getButton() == PRIMARY) layout.toggleShowContent();
        });
        
        
        // accept drag transfer
        entireArea.setOnDragOver(DragUtil.audioDragAccepthandler);
        // handle drag transfers
        entireArea.setOnDragDropped( e -> {
            // get first item
            List<Item> items = DragUtil.getAudioItems(e);
            // getMetadata, refresh
            if (!items.isEmpty()) {
                if (changeReadModeOnTransfer) readMode = ReadMode.CUSTOM;
                Player.bindObservedMetadata(meta, readMode);
                meta.set(items.get(0).getMetadata());
            }
            // end drag
            e.setDropCompleted(true);
            e.consume();
        });
    }
    
    @Override
    public void refresh() {
        Player.bindObservedMetadata(meta, readMode);
        refreshNoBinding();
        // apply min tile width
        tileResizer.changed(null, null, tiles.getWidth());
    }

    @Override
    public void OnClosing() {
        meta.unbind();
    }
    
/******************************************************************************/
    
    private void refreshNoBinding() {
        rater.setMax(maxRating);
        rater.setPartialRating(partialRating);
        rater.setUpdateOnHover(hoverRating);
        rater.setEditable(editableRating);
        if (rating_skin.isEmpty())
            rating_skin = rater.getSkinCurrent();
        else 
            rater.setSkinCurrent(rating_skin);
        
        labels.forEach(l->l.setTextOverrun(overrun_style));
        
        populateGui(meta.get());
        setVisibility();
    }
    
    private void populateGui(Metadata m) {
        if (m == null) {
            clear();
            return;
        }
        
        // set image
        Cover c = m.getCover(CoverSource.ANY);
        if(c.getFile()!=null) layout.setImage(c.getFile());
        else layout.setImage(c.getImage());
        
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
        format.setText("format: "       + m.getFormatFromTag());
        bitrate.setText("bitrate: "     + m.getBitrate().toString());
        encoding.setText("encoding: "   + m.getEncoder());
        location.setText("location: "   + m.getPath());
    }
    
    private void clear() {
        layout.setImage((Image)null);
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
        // image visibility
        layout.setShowImage(layout.hasImage() ? showCover : false);
        
        // hide all fields
        layout.setShowContent(showFields);
        
        // initialize
        visible_labels.clear();
        visible_labels.addAll(labels);
        visible_labels.forEach(l -> l.setVisible(false));
        visible_labels.forEach(l -> l.setDisable(false));

        // disable empty fields
        if (showEmptyFields) {
            visible_labels.stream().filter(l->{
                    // filter out nonempty
                    String content = l.getText().substring(l.getText().indexOf(": ")+2).trim();
                    return content.isEmpty() || content.equalsIgnoreCase("?/?") ||
                            content.equalsIgnoreCase("n/a") || content.equalsIgnoreCase("unknown");
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
        if (!showTitle)         visible_labels.remove(title);
        if (!showtrack)         visible_labels.remove(track);
        if (!showdisc)          visible_labels.remove(disc);
        if (!showartist)        visible_labels.remove(artist);
        if (!showalbum_artist)  visible_labels.remove(album_artist);
        if (!showalbum)         visible_labels.remove(album);
        if (!showyear)          visible_labels.remove(year);
        if (!showgenre)         visible_labels.remove(genre);
        if (!showcomposer)      visible_labels.remove(composer);
        if (!showpublisher)     visible_labels.remove(publisher);
        if (!showrating)        visible_labels.remove(rating);
        if (!showplaycount)     visible_labels.remove(playcount);
        if (!showcomment)       visible_labels.remove(comment);
        if (!showcategory)      visible_labels.remove(category);
        if (!showfilesize)      visible_labels.remove(filesize);
        if (!showformat)        visible_labels.remove(format);
        if (!showfilename)      visible_labels.remove(filename);
        if (!showbitrate)       visible_labels.remove(bitrate);
        if (!showencoding)      visible_labels.remove(encoding);
        if (!showlocation)      visible_labels.remove(location);
        
        // hide separators
        if (!groupFields) {
            visible_labels.remove(gap1);
            visible_labels.remove(gap2);
            visible_labels.remove(gap3);
        }
        
        // show remaining
        visible_labels.forEach(l -> l.setVisible(true));
        
        rating.setText("rating: ");
    }

}