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
import java.util.List;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.image.Image;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;

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
    notes = "Plans: Improve layout.\n",
    version = "0.8",
    year = "2014",
    group = Widget.Group.OTHER
)
public class FileInfoController extends FXMLController  {
    
    @FXML AnchorPane entireArea;
    ImageFlowPane layout;
    
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
    GridPane t;
    
    final List<Label> labels = new ArrayList();
    final List<Label> visible_labels = new ArrayList();
    
    final SimpleObjectProperty<Metadata> meta = new SimpleObjectProperty();
    
    // properties
    @IsConfig(name = "Read mode", info = "Source of data for the widget.")
    public ReadMode readMode = ReadMode.PLAYING;
    @IsConfig(name = "Column min width", info = "Minimal width for field columns. Use -1 for automatic width")
    public Double minColumnWidth = 150.0;
    @IsConfig(name = "Cover source", info = "Source for cover image.")
    public CoverSource cover_source = CoverSource.ANY;
    @IsConfig(name = "Read mode change on drag", info = "Change read mode to CUSTOM when data are arbitrary added to widget.")
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
    @IsConfig(name = "Group fields", info = "Separate fields by gap and group them..")
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
    
    @Override
    public void init() {
        // initialize gui
        Thumbnail thumb = new Thumbnail();
                  thumb.setBorderToImage(false);
                  
        layout = new ImageFlowPane(entireArea, thumb);
        layout.setPadding(5);
        layout.setMinContentWidth(200);
        layout.setMinContentHeight(120);
        
        // initialize fields
        labels.clear();
        labels.add(title);
        labels.add(track);
        labels.add(disc);
        labels.add(gap1);
        labels.add(artist);
        labels.add(album);  
        labels.add(album_artist);
        labels.add(year);
        labels.add(genre);
        labels.add(composer);
        labels.add(publisher);
        labels.add(gap2);
        labels.add(rating);
        labels.add(playcount);
        labels.add(comment);
        labels.add(category); 
        labels.add(gap3);
        labels.add(filesize);
        labels.add(filename);
        labels.add(format);
        labels.add(bitrate);
        labels.add(encoding);
        labels.add(location);
        
        // add to layout
        layout.setChildren(labels);
        layout.addChild(rater);
        
        // refresh if metadata source data changed
        meta.addListener( o -> refreshNoBinding());
        
        // bind rater to rating
        rater.disableProperty().bind(rating.disableProperty());
        rater.visibleProperty().bind(rating.visibleProperty());
        // write metadata on rating change
        rater.setOnRatingChanged( e -> MetadataWriter.rate(meta.get(),rater.getRatingPercent()));
        // swap skin on right mouse click
        rater.setOnSkinChanged( e -> rating_skin = rater.getSkinCurrent());
        rater.setOnMouseClicked(e -> { 
            if (e.getButton() == SECONDARY) rater.toggleSkin(); 
        });
        
        // show/hide content on cover mouse click
        thumb.getPane().setOnMouseClicked( e -> {
            if (e.getButton() == PRIMARY) layout.toggleShowContent();
        });
        
        // redo layout if resized
        layout.getContentPane().widthProperty().addListener( o -> reposition());
        layout.getContentPane().heightProperty().addListener( o -> reposition());
        
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
    }

    @Override
    public void OnClosing() {
        // unbind
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
        reposition();
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
            rater.setRating(0.0);
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
            visible_labels.stream()
                    .filter(l->{
                        // filter out nonempty
                        String content = l.getText().substring(l.getText().indexOf(": ")+2).trim();
                        return content.isEmpty() || content.equalsIgnoreCase("?/?") ||
                                content.equalsIgnoreCase("n/a") || content.equalsIgnoreCase("unknown");
                    })
                    .forEach(l->l.setDisable(true));
        } else {
            labels.stream()
                .filter(ll -> ll.getText().substring(ll.getText().indexOf(": ")+1).equals(" "))
                .forEach((ll) -> visible_labels.remove(ll));
        }
        
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
                
    }
    private void reposition() {
        double gapX = 5;
        double gapY = 3;
        double itemHeight = location.getFont().getSize() + gapY;                // label height
        int itemsPerColumn = (int)Math.floor(layout.contentHeight()/itemHeight);
            itemsPerColumn = (itemsPerColumn==0) ? 1 : itemsPerColumn; // prevent division by zero
        int columns = 1+(visible_labels.size()/itemsPerColumn);                 // number of columns
        double columnWidth = layout.contentWidth()/columns;                     // column width
               columnWidth = columnWidth<minColumnWidth ? minColumnWidth : columnWidth;
        double itemWidth = columnWidth-gapX;                                    // label width
        for (Label l : visible_labels) {
            int index = visible_labels.indexOf(l);
            int column = (index / itemsPerColumn);
            int row = (index % itemsPerColumn);
            l.relocate(column * columnWidth, row * itemHeight);
            l.setMaxWidth(itemWidth);
            // rating
            if (l.getText().startsWith("rating")) {
                l.setText("rating:");
                rater.relocate(column * columnWidth + l.getWidth() + 5, row * itemHeight-2);
            }
        }
    }
}