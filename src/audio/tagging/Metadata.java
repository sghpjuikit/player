
package audio.tagging;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.KeyNotFoundException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagField;
import org.jaudiotagger.tag.flac.FlacTag;
import org.jaudiotagger.tag.id3.AbstractID3Tag;
import org.jaudiotagger.tag.id3.AbstractID3v1Tag;
import org.jaudiotagger.tag.id3.AbstractID3v2Frame;
import org.jaudiotagger.tag.id3.AbstractID3v2Tag;
import org.jaudiotagger.tag.id3.ID3v24Frames;
import org.jaudiotagger.tag.id3.framebody.FrameBodyPOPM;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.mp4.Mp4FieldKey;
import org.jaudiotagger.tag.mp4.Mp4Tag;
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag;
import org.jaudiotagger.tag.wav.WavTag;

import audio.Item;
import audio.playlist.PlaylistItem;
import audio.playlist.PlaylistManager;
import audio.tagging.chapter.Chapter;
import gui.objects.image.cover.Cover;
import gui.objects.image.cover.Cover.CoverSource;
import gui.objects.image.cover.FileCover;
import gui.objects.image.cover.ImageCover;
import util.file.AudioFileFormat;
import util.file.Util;
import util.file.ImageFileFormat;
import util.SwitchException;
import util.access.fieldvalue.ObjectField;
import util.dev.TODO;
import util.functional.Functors.Ƒ1;
import util.parsing.Parser;
import util.units.Bitrate;
import util.units.FileSize;
import util.units.FormattedDuration;
import util.units.NofX;

import static audio.tagging.Metadata.Field.COVER_INFO;
import static audio.tagging.Metadata.Field.FULLTEXT;
import static java.lang.Integer.parseInt;
import static java.util.stream.Collectors.joining;
import static util.file.Util.EMPTY_URI;
import static util.Util.emptyOr;
import static util.type.Util.mapEnumConstantName;
import static util.dev.Util.log;
import static util.functional.Util.equalNull;
import static util.functional.Util.list;
import static util.functional.Util.split;
import static util.functional.Util.stream;

/**
 * Information about audio file.
 * Provided by reading tag and audio file header (mostly).
 * Everything that is possible to know about the audio file is accessible through
 * this class. This class also provides some non-tag, application specific
 * or external information associated with this song, like cover files.
 * <p/>
 * The class is practically immutable and does not provide any setters, nor
 * allows updating of its state or any of its values.
 * <p/>
 * Metadata can be empty and hsould be used instead of null. See {@link #EMPTY}
 * <p/>
 * The getters of this class return mostly string. For empty fields the output
 * is "" (empty string) and for non-string getters it varies, but it is never
 * null. See documentation
 * for the specific field. The rule is however, that the check for empty value
 * should be necessary only in rare cases.
 * <p/>
 * Every field returns string, primitive, or Object with toString method that
 * returns the best possible string representation of the field's value, including
 * its empty value.
 * <p/>
 * To access any field in a general way, see {@link audio.tagging.Metadata.Field}
 *
 * @author Martin Polakovic
 */
@Entity(name = "MetadataItem")
public final class Metadata extends MetaItem<Metadata> {

    /**
     * Delimiter between sections of data.
     * In this case, between different tags (concatenated to single string written to single tag)
     */
    public static final Character SEPARATOR_GROUP = 29;
    /**
     * Delimiter between records or rows.
     * In this case, between multiple values in a tag.
     */
    public static final Character SEPARATOR_RECORD = 30;
    /**
     * Delimiter between fields of a record, or members of a row.
     * In this case, between multiple items in a tag value.
     */
    public static final Character SEPARATOR_UNIT = 31;

    // Custom tag ids. Ordinary string. Length 10 mandatory. Unique. Dev is free to use any
    // value - there is no predefined set of ids. Once set, never change!
    static final String TAGID_PLAYED_LAST =   "PLAYED_LST";
    static final String TAGID_PLAYED_FIRST =  "PLAYED_1ST";
    static final String TAGID_LIB_ADDED =     "LIB_ADDED_";
    static final String TAGID_COLOR =         "COLOR_____";
    static final String TAGID_TAGS =          "TAG_______";

    static {
        // configure jaudiotagger
//        TagOptionSingleton.getInstance().setID3V2Version(ID3_V24);
//        TagOptionSingleton.getInstance().setWavOptions(READ_ID3_UNLESS_ONLY_INFO);
//        TagOptionSingleton.getInstance().setWavSaveOptions(SAVE_BOTH);
    }

    // use to debug tag
    // tag.getFields().forEachRemaining(f->System.out.println(f.getId()+" "+f));

    /**
     * EMPTY metadata. Substitute for null. Always use instead of null. Also
     * corrupted items should transform into EMPTY metadata.
     * <p/>
     * All fields are at their default values.
     * <p/>
     * There are two ways to check whether Metadata object is EMPTY. Either use
     * reference operator this == Metadata.EMPTY or call {@link #isEmpty()}.
     * <p/>
     * Note: The reference operator works, because there is always only one
     * instance of EMPTY metadata.
     */
    public static final Metadata EMPTY = new Metadata();

    public static String metadataID(URI u) {
        return u.toString();
    }

    // note: for some fields the initialized values below are not their
    // 'default' values. Rather, those are auto-generated when requested

    // identification fields
    // note: there were some problems with serializing URI, although it should be
    // supported. For now... String it is. Use URI.toString(), not getPath()!
    // Might change to URI later.
    // Before anyone attempts to do so, note that this is primary key we are talking
    // about.
    //
    // primary key & resource identifier. see getURI() and constructors.
    // for empty metadata use special 'empty' uri
    @Id
    private String uri = Util.EMPTY_URI.toString();

            // header fields
    private long filesize = 0;
    private String encoding = "";
    private int bitrate = -1;
    private String encoder = "";
    private String channels = "";
    private String sample_rate = "";
    private double duration = 0;
            // tag fields
    private String title = "";
    private String album = "";
    private String artist = "";
            @Transient
    private List<String> artists = null; // unsupported as of now
    private String album_artist = "";
    private String composer = "";
    private String publisher = "";
    private int track = -1;
    private int tracks_total = -1;
    private int disc = -1;
    private int discs_total = -1;
    private String genre = "";
    private int year = -1;
            @Transient
    private Artwork cover = null;
    private int rating = -1;
            @Transient
    private double ratingP = -1;
    private int playcount = -1;
    private String category = "";
    private String comment = "";
    private String lyrics = "";
    private String mood = "";
            // some custom fields contain synthetic field's values
    private String custom1 = "";
    private String custom2 = "";
    private String custom3 = "";
    private String custom4 = "";
    private String custom5 = "";

    private String playedFirst = "";
    private String playedLast = "";
    private String libraryAdded = "";
    private String color = "";
    private String tags = "";


    // public - this type of metadata is simply a conversion, harmless & we need
    // to allow the access from Item subclasses
    /**
     * Creates metadata from specified item. Attempts to fill as many metadata
     * information fields from the item as possible, leaving everything else empty.
     * For example, if the item is playlist item, sets artist, length and title fields.
     */
    public Metadata(Item item) {
        uri = item.getURI().toString();
        if(item instanceof PlaylistItem) {
            PlaylistItem pitem = (PlaylistItem)item;
            artist = pitem.getArtist();
            duration = pitem.getTime().toMillis();
            title = pitem.getTitle();
        }
    }

    // constructs empty metadata
    // private - do not allow anyone create new empty metadata
    private Metadata() {
        uri = EMPTY_URI.toString();
    }

    // the constructor for creating new metadata by reading the tag
    // package access - only allow the MetadataReaders to create metadata like this
    /**
     * Reads and creates metadata info for file.
     * Everything is read at once. There is no additional READ operation after the
     * object has been created.
     *
     */
    Metadata(AudioFile audiofile) {
        File file = audiofile.getFile().getAbsoluteFile();
        uri = file.toURI().toString();
        filesize = FileSize.inBytes(file);

        loadHeaderFields(audiofile);

        // We would like to make sure tag always exists, but this probably involves
        // writing the tag. We want to stay just reading. If we dont mind, use:
        Tag tag = audiofile.getTagOrCreateAndSetDefault();
        // If the tag is null, we skip reading
//        Tag tag = audiofile.getTag();
        if(tag!=null) {
            loadGeneralFields(tag);
            switch (getFormat()) {
                case mp3:  loadFieldsID3((AbstractID3Tag)tag);                     break;
                case flac: loadFieldsVorbis(((FlacTag)tag).getVorbisCommentTag()); break;
                case ogg:  loadFieldsVorbis((VorbisCommentTag)tag);                break;
                case wav:  loadFieldsWAV((WavTag)tag);                             break;
                case mp4:
                case m4a:  loadFieldsMP4((Mp4Tag)tag);                             break;
                default:   // do nothing for the rest;
            }
        }

    }

    /** loads all header fields  */
    private void loadHeaderFields(AudioFile aFile) {
        AudioHeader header = aFile.getAudioHeader();
        if(header==null) {  // just in case
            log(Metadata.class).info("Header not found: " + getURI());
        } else {
            bitrate = (int)header.getBitRateAsNumber();
            duration = 1000 * header.getTrackLength();
            // format and encoding type are switched in jaudiotagger library...
            encoding = emptyOr(header.getFormat());
            channels = emptyOr(header.getChannels());
            sample_rate = emptyOr(header.getSampleRate());
        }
    }

    /** loads all generally supported fields  */
    private void loadGeneralFields(Tag tag) {
        encoder = getGeneral(tag,FieldKey.ENCODER);

        title = getGeneral(tag,FieldKey.TITLE);
        album = getGeneral(tag,FieldKey.ALBUM);
//        artists = getGenerals(tag,FieldKey.ARTIST);
        artist = getGeneral(tag,FieldKey.ARTIST);
        album_artist = getGeneral(tag,FieldKey.ALBUM_ARTIST);
        composer = getGeneral(tag,FieldKey.COMPOSER);

        // track
        String tr = getGeneral(tag,FieldKey.TRACK);
        int i = tr.indexOf('/');
        if(i!=-1) {
            // some apps use TRACK for "x/y" string format, we cover that
            track = number(tr.substring(0, i));
            tracks_total = number(tr.substring(i+1, tr.length()));
        } else {
            track = number(tr);
            tracks_total = getNumber(tag,FieldKey.TRACK_TOTAL);
        }

        // disc
        String dr = getGeneral(tag,FieldKey.DISC_NO);
        int j = dr.indexOf('/');
        if(j!=-1) {
            // some apps use DISC_NO for "x/y" string format, we cover that
            disc = number(dr.substring(0, j));
            discs_total = number(dr.substring(j+1, dr.length()));
        } else {
            disc = number(dr);
            discs_total = getNumber(tag,FieldKey.DISC_TOTAL);
        }

        playcount = getNumber(tag, FieldKey.CUSTOM3);
        genre = getGeneral(tag,FieldKey.GENRE);
        year = getNumber(tag,FieldKey.YEAR);
        category = getGeneral(tag,FieldKey.GROUPING);
        comment = getComment(tag);
        lyrics = getGeneral(tag,FieldKey.LYRICS);
        mood = getGeneral(tag,FieldKey.MOOD);
        custom1 = getGeneral(tag,FieldKey.CUSTOM1);
        custom2 = getGeneral(tag,FieldKey.CUSTOM2);
        custom3 = getGeneral(tag,FieldKey.CUSTOM3);
        custom4 = getGeneral(tag,FieldKey.CUSTOM4);
        custom5 = getGeneral(tag,FieldKey.CUSTOM5);

        // Following acquire data from special tag
        if(!custom5.isEmpty()) {
            for(String tagField : list(split(custom5,SEPARATOR_GROUP.toString()))) {
                if(tagField.length()<10) continue;      // skip deformed to avoid exception
                String tagId = tagField.substring(0,10);
                String tagValue = tagField.substring(10);
                switch(tagId) {
                    case TAGID_PLAYED_FIRST: playedFirst = tagValue; continue;
                    case TAGID_PLAYED_LAST: playedLast = tagValue; continue;
                    case TAGID_LIB_ADDED: libraryAdded = tagValue; continue;
                    case TAGID_COLOR: color = tagValue; continue;
                    case TAGID_TAGS: tags = tagValue; continue;
                }
            }
        }
    }

    private String getGeneral(Tag tag, FieldKey f) {
        try {
            String s = tag.getFirst(f); // can throw UnsupportedOperationException
            if (s == null) {
                log(Metadata.class).warn("Jaudiotagger returned null for {} of {}", f,uri);
            }
            return s;
        } catch (UnsupportedOperationException | KeyNotFoundException e) {
            log(Metadata.class).warn("Jaudiotagger failed to read {} of {}", f,uri, e);
            return "";
        }
    }

    private int getNumber(Tag tag, FieldKey field) {
        return number(getGeneral(tag, field));
    }

    /** Returns parsed int from the text or -1 if impossible. */
    private int number(String text) {
        try {
            return text.isEmpty() ? -1 : parseInt(text);
        } catch(NumberFormatException e){
            return -1;
        }
    }

    // use this to get comment, not getField(COMMENT); because it is bugged
    private static String getComment(Tag tag) {
        // there is a bug where getField(Comment) returns CUSTOM1 field, this is workaround
        // this is how COMMENT field look like:
        //      Language="English"; Text="example";
        // this is how CUSTOM fields look like:
        //      Language="Media Monkey Format"; Description="Songs-DB_Custom5"; Text="example";
        if (!tag.hasField(FieldKey.COMMENT)) return "";
        int i = -1;
        List<TagField> fields = tag.getFields(FieldKey.COMMENT);
        // get index of comment within all comment-type tags
        for(TagField t: fields) // custom
            if (!t.toString().contains("Description"))
                i = fields.indexOf(t);
        if(i>-1) return tag.getValue(FieldKey.COMMENT, i);
        else return "";
    }

    private static List<String> getGenerals(Tag tag, FieldKey f) {
        List<String> out = new ArrayList<>();
        if (!tag.hasField(f)) return out;

        try{
//        for (TagField field: tag.getFields(f)) {
//            String tmp = field.toString();
//                   tmp = tmp.substring(6, tmp.length()-3);
//            out.add(Util.emptyOr(tmp));
//        }
        for (String val: tag.getAll(f))
            out.add(util.Util.emptyOr(val));
        } catch(Exception w) {
            // contrary to what compiler is saying, no, exception is not too broad
            // do not change the exception or some weird stuff will be happening
            // jaudiotagger throws some additional exceptions here and there...
            // needs to be investigated
            w.printStackTrace();
        }
        return out;
    }

    // There are three types of fields in terms of  how they are stored/read:
    // tag agnostic - jaudiotagger handles the field transparently in a tag independent way
    // tag specific - we have to handle the field manually per tag type, using jaudiotagger
    // special - Custom fields just for this application. They are all aggregated in a single
    //           field, but that is an implementation detail (This works similar to a vorbis
    //           tag, where each field has a string id and there is no limit to order,
    //           multiplicity or content of the fields).

    // Following methods acquire data from tag type specific fields
    // RATING - Each tag handles rating differently, simply read it
    // PLAYCOUNT - We store playcount separately, but some tag types support it, so we check it
    //             and use it if we have no data on our own
    // PUBLISHER - Each tag handles it differently, simply read it
    // CATEGORY - Each tag handles it differently, simply read it

    private void loadFieldsID3(AbstractID3Tag tag) {
        if(tag instanceof AbstractID3v1Tag) {
            // RATING + PLAYCOUNT +PUBLISHER + CATEGORY ----------------------------
            // id3 has no fields for this (note that when we write these fields we convert tag to
            // ID3v2, so this only happens with untagged songs, which we dont care about
        } else if(tag instanceof AbstractID3v2Tag) {
            AbstractID3v2Tag t = (AbstractID3v2Tag) tag;
            // RATING + PLAYCOUNT --------------------------------------------------
            // we use POPM field (rating + counter + mail/user)
            AbstractID3v2Frame frame1 = t.getFirstField(ID3v24Frames.FRAME_ID_POPULARIMETER);
            // if not present we get null and leave default values
            if (frame1 != null) {
                // we obtain body for the field
                FrameBodyPOPM body1 = (FrameBodyPOPM) frame1.getBody();
                // once again the body of the field might not be there
                if (body1 != null) {
                    long rat = body1.getRating(); //returns null if empty
                    long cou = body1.getCounter(); //returns null if empty

                    // i do not know why the values themselves are Long, but we only need int
                    // both for rating and playcount.
                    // all is good until the tag is actually damaged and the int can really
                    // overflow during conversion and we get ArithmeticException
                    // so we catch it and ignore the value
                    try {
                        rating = Math.toIntExact(rat);
                    } catch (ArithmeticException ignored){}

                    try {
                        int pc = Math.toIntExact(cou);
                        if(pc > playcount) playcount = pc;
                    } catch (ArithmeticException ignored) {}
                }
            }
            // todo: also check ID3v24Frames.FRAME_ID_PLAY_COUNTER

            // PUBLISHER -----------------------------------------------------------
            publisher = emptyOr(t.getFirst(ID3v24Frames.FRAME_ID_PUBLISHER));

            // CATEGORY ------------------------------------------------------------
            // the general acquisition is good enough
        }
    }

    // wav
    private void loadFieldsWAV(WavTag tag) {
        AbstractID3v2Tag t = tag.getID3Tag();
        if(t!=null) {
            loadFieldsID3(t);
        } else {
            // todo: implement fallback
            // i do not know how this works, so for now unimplemented
            // tag.getInfoTag();
            // RATING --------------------------------------------------------------

            // PLAYCOUNT -----------------------------------------------------------

            // PUBLISHER -----------------------------------------------------------

            // CATEGORY ------------------------------------------------------------
        }
    }

    // mp4 & m4a
    private void loadFieldsMP4(Mp4Tag tag) {
        // RATING --------------------------------------------------------------
        // id: 'rate'
        // all are equivalent:
        //      tag.getFirst(FieldKey.RATING)
        //      tag.getValue(FieldKey.RATING,0)
        //      tag.getItem("rate",0)
        int r = getNumber(tag, FieldKey.RATING);
        // sometimes we get unintelligible values for some reason (dont ask me),
        // Mp3Tagger app can recognize them somehow
        // the values appear consistent so lets use them
        if(r==12592) rating = 100;
        else if(r==14384) rating = 80;
        else if(r==13872) rating = 60;
        else if(r==13360) rating = 40;
        else if(r==12848) rating = 20;
        // handle normally
        else rating = (r<0 || r>100) ? -1 : r;

        // PLAYCOUNT -----------------------------------------------------------
        // no support

        // PUBLISHER -----------------------------------------------------------
        // id: '----:com.nullsoft.winamp:publisher'
        //      tag.getFirst(FieldKey.PRODUCER) // nope
        //      tag.getFirst(Mp4FieldKey.WINAMP_PUBLISHER) // works
        //      tag.getFirst(Mp4FieldKey.LABEL) // not same as WINAMP_PUBLISHER, but perhaps also valid
        //      tag.getFirst(FieldKey.KEY)
        publisher = emptyOr(tag.getFirst(Mp4FieldKey.WINAMP_PUBLISHER));
        if(publisher.isEmpty()) publisher = emptyOr(tag.getFirst(Mp4FieldKey.MM_PUBLISHER));

        // CATEGORY ------------------------------------------------------------
        // the general acquisition is good enough
    }

    // ogg & flac
    // both use vorbis tag
    // the only difference between the two is cover handling, which jaudiotagger takes care of
    private void loadFieldsVorbis(VorbisCommentTag tag) {
        // RATING --------------------------------------------------------------
        // some players use 0-5 value, so extends it to 100
        rating = number(emptyOr(tag.getFirst("RATING")));
        if(0<rating && rating<6) rating *=20;

        // PLAYCOUNT -----------------------------------------------------------
        // if we want to support playcount in vorbis specific field, we can
        // just lets not overwrite value we obtained with previous methods
        // if(playcount==-1) playcount = number(emptyOr(tag.getFirst("PLAYCOUNT")));

        // PUBLISHER -----------------------------------------------------------
        publisher = emptyOr(tag.getFirst("PUBLISHER"));

        // CATEGORY ------------------------------------------------------------
        // try to get category the winamp way, dont if we already have a value
        if(category.isEmpty()) category = emptyOr(tag.getFirst("CATEGORY"));
    }

/******************************************************************************/

    @Override
    public String getId() {
       return uri;
    }
    public void setId(String id) {
        this.uri = id;
    }

    @Override
    public URI getURI() {
        return URI.create(uri.replace(" ","%20"));
    }

    /**
     * @see #EMPTY
     * @return true if this metadata is empty.
     */
    public boolean isEmpty() {
        return this == EMPTY;
    }

    @Override
    public String getPath() {
        return isEmpty() ? "" : super.getPath();
    }

    @Override
    public String getFilename() {
        return super.getFilename();
    }

    @Override
    public FileSize getFilesize() {
        return new FileSize(filesize);
    }

    /** {@link #getFilesize()})} in bytes */
    public long getFilesizeInB() {
        return filesize;
    }

    /** {@inheritDoc} */
    @Override
    public AudioFileFormat getFormat() {
        return AudioFileFormat.of(uri);
    }

    /** @return the bitrate */
    public Bitrate getBitrate() {
        return bitrate==-1 ? null : new Bitrate(bitrate);
    }

    /**
     * For example: Stereo.
     * @return channels as String
     */
    public String getChannels() {
        return channels;
    }

    /**
     * For example: 44100
     *
     * @return sample rate as String
     */
    public String getSampleRate() {
        return sample_rate;
    }

    /**
     * For example: MPEG-1 Layer 3.
     * @return encoding type
     */
    public String getEncodingType() {
        return encoding;
    }

    /** @return the encoder or empty String if not available. */
    public String getEncoder() {
        return encoder;
    }

    /** @return the length */
    public FormattedDuration getLength() {
        return new FormattedDuration(duration);
    }

    /** @return the length in milliseconds */
    public double getLengthInMs() {
        return duration;
    }

/******************************************************************************/

    /** @return the title "" if empty. */
    public String getTitle() {
        return title;
    }

    /** @return the album "" if empty. */
    public String getAlbum() {
        return album;
    }

    /**
     * Returns list all of artists.
     *
     * @return the artist empty List if empty.
     */
    public List<String> getArtists() {
        return artists;
    }

    /**
     * Returns first artist found in tag.
     * If you want to get all artists dont use this method.
     * @return the first artist
     * "" if empty.
     */
    public String getArtist() {
        return artist;
    }

    /** @return the album_artist, "" if empty. */
    public String getAlbumArtist() {
        return album_artist;
    }

    /** @return the composer, "" if empty. */
    public String getComposer() {
        return composer;
    }

    /** @return the publisher, "" if empty. */
    public String getPublisher() {
        return publisher;
    }

    /** @return the track, -1 if empty. */
    public int getTrack() {
        return track;
    }

    /** @return the tracks total, -1 if empty. */
    public int getTracksTotal() {
        return tracks_total;
    }

    /**
     * Convenience method. Returns complete info about track order within album in
     * format track/tracks_total. If you just need this information as a string
     * and dont intend to sort or use the numerical values
     * then use this method instead of {@link #getTrack()} and {@link #getTracksTotal())}.
     * <p/>
     * If disc or disc_total information is unknown the respective portion in the
     * output will be substitued by character '?'.
     * Example: 1/1, 4/23, ?/?, ...
     * @return track album order information.
     */
    public NofX getTrackInfo() {
        return new NofX(track, tracks_total);
    }

    /** @return the disc, -1 if empty. */
    public int getDisc() {
        return disc;
    }

    /** @return the discs total, -1 if empty. */
    public int getDiscsTotal() {
        return discs_total;
    }


    /**
     * Convenience method. Returns complete info about disc number in album in
     * format disc/discs_total. If you just need this information as a string
     * and dont intend to sort or use the numerical values
     * then use this method instead of {@link #getDisc() ()} and {@link #getDiscsTotal()}.
     * <p/>
     * If disc or disc_total information is unknown the respective portion in the
     * output will be substitued by character '?'.
     * Example: 1/1, ?/?, 5/?, ...
     * @return disc information.
     */
    public NofX getDiscInfo() {
        return new NofX(disc, discs_total);
    }

    /** @return the genre, "" if empty. */
    public String getGenre() {
        return genre;
    }

    /** @return the year or null if empty. */
    public Year getYear() {
        try {
            return year==-1 ? null : Year.of(year);
        } catch (DateTimeException e) {
            return null;
        }
    }

    /** @return year integer or -1 if none. */
    public int getYearAsInt() {
        return year;
    }

    /**
     * Returns cover from the respective source.
     *
     * @param source strategy for the cover file lookup
     */
    public Cover getCover(CoverSource source) {
        switch(source) {
            case TAG: return getCover();
            case DIRECTORY: return new FileCover(getCoverFromDirAsFile(), "");
            case ANY : {
                Cover c = getCover();
                return c.getImage()!=null
                        ? c
                        : new FileCover(getCoverFromDirAsFile(), "");
            }
            default: throw new SwitchException(source);
        }
    }

    private void loadCover() {
        if(cover!=null || cover_loaded) return;
        cover_loaded = true;
        AudioFile af = isFileBased() ? readAudioFile(getFile()) : null;
        Tag tag = af!=null ? af.getTagOrCreateAndSetDefault() : null;
        cover = tag==null ? null : tag.getFirstArtwork();
    }

    private Cover getCover() {
        try {
            loadCover();
            if(cover==null) return new ImageCover((Image)null, getCoverInfo());
            else return new ImageCover((BufferedImage) cover.getImage(), getCoverInfo());
        } catch (IOException e) {
            return new ImageCover((Image)null, getCoverInfo());
        } catch(NullPointerException ex) {
            // jaudiotagger bug, Artwork.getImage() can throw NullPointerException sometimes
            // at java.io.ByteArrayInputStream.<init>(ByteArrayInputStream.java:106) ~[na:na]
            // at org.jaudiotagger.tag.images.StandardArtwork.getImage(StandardArtwork.java:95) ~[jaudiotagger-2.2.6-SNAPSHOT.jar:na]
            return new ImageCover((Image)null, getCoverInfo());
        }
    }

    @Transient
    private boolean cover_loaded = false;

    private String getCoverInfo() {
        try {
            loadCover();
            return cover.getDescription() + " " + cover.getMimeType() + " "
                       + ((RenderedImage)cover.getImage()).getWidth() + "x"
                       + ((RenderedImage)cover.getImage()).getHeight();
        } catch(IOException | NullPointerException e) {
            // ^ why do we catch NullPointerException here? Not a bug fix! Investigate & remove!
            // never mind errors. Return "" on fail.
            return "";
        }
    }

    /**
     * Identical to getCoverFromDir() method, but returns the image file
     * itself. Only file based items.
     * <p/>
     * If the Image object of the cover suffices, it is recommended to
     * avoid this method, or even better use getCoverFromAnySource()
     *
     * @return cover file
     */
    @TODO(note = "ensure null does not cause any problems")
    private File getCoverFromDirAsFile() {
        if(!isFileBased()) return null;

        File dir = getFile().getParentFile();
        if (!Util.isValidDirectory(dir)) return null;

        File[] files;
        files = dir.listFiles( f -> {
            String filename = f.getName();
            int i = filename.lastIndexOf('.');
            if(i == -1) return false;
            String name = filename.substring(0, i);
            return (ImageFileFormat.isSupported(f.toURI()) && (
                        name.equalsIgnoreCase("cover") ||
                        name.equalsIgnoreCase("folder") ||
                        name.equalsIgnoreCase(getFilenameFull()) ||
                        name.equalsIgnoreCase(getFilename()) ||
                        name.equalsIgnoreCase(getTitle()) ||
                        name.equalsIgnoreCase(getAlbum())
                ));
        });

        if (files.length == 0) return null;
        else return files[0];
    }

    /** @return the rating or -1 if empty. */
    public int getRating() {
        return rating;
    }

    /**
     * Recommended method to use to obtain rating.
     * @return the rating in 0-1 percent range
     */
    public double getRatingPercent() {
        return rating==-1 ? 0 : rating/(double)getRatingMax();
    }

    /**
     * @param max_stars upper limit of an int representation of the rating, e.g., 5.
     * @return the current rating value in 0-max_stars value system. 0 if not available.
     */
    public double getRatingToStars(int max_stars) {
        return getRatingPercent()*max_stars;
    }

    /** @return the playcount, 0 if empty. */
    public int getPlaycount() {
        return playcount == -1 ? 0 : playcount;
    }

    /**
     * tag field: grouping
     * @return the category "" if empty.
     */
    public String getCategory() {
        return category;
    }

    /** @return the comment, "" if empty. */
    public String getComment() {
        return comment;
    }

    /**
     * @return the lyrics, "" if empty.
     */
    public String getLyrics() {
        return lyrics;
    }

    /** @return the mood, "" if empty. */
    public String getMood() {
        return mood;
    }

    /** @return the color value associated with the song from tag or null if none. */
    public Color getColor() {
         return color.isEmpty() ? null : Parser.DEFAULT.fromS(Color.class,color);
    }

    /**  @return the tags, "" if empty. */
    public String getTags() {
         return tags;
    }

    /** @return the value of custom1 field. "" if empty. */
    public String getCustom1() {
        return custom1;
    }
    /** @return the value of custom2 field. "" if empty. */
    public String getCustom2() {
        return custom2;
    }
    /** @return the value of custom3 field. "" if empty. */
    public String getCustom3() {
        return custom3;
    }
    /** @return the value of custom4 field. "" if empty. */
    public String getCustom4() {
        return custom4;
    }
    /** @return the value of custom5 field. "" if empty. */
    public String getCustom5() {
        return custom5;
    }

    private static final ZoneId ZONE_ID = ZoneId.systemDefault();

    public static LocalDateTime localDateTimeFromMillis(long epochMillis) {
        try {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis),ZONE_ID);
        } catch(DateTimeException e) {
            return null;
        }
    }

    public LocalDateTime getTimePlayedFirst() {
        if(playedFirst.isEmpty()) return null;
        try {
            long epochMillis = Long.parseLong(playedFirst);
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis),ZONE_ID);
        } catch(NumberFormatException | DateTimeException e) {
            return null;
        }
    }

    public LocalDateTime getTimePlayedLast() {
        if(playedLast.isEmpty()) return null;
        try {
            long epochMillis = Long.parseLong(playedLast);
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis),ZONE_ID);
        } catch(NumberFormatException | DateTimeException e) {
            return null;
        }
    }

    public LocalDateTime getTimeLibraryAdded() {
        if(libraryAdded.isEmpty()) return null;
        try {
            long epochMillis = Long.parseLong(libraryAdded);
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis),ZONE_ID);
        } catch(NumberFormatException | DateTimeException e) {
            return null;
        }
    }

    private static final Field[] STRING_FIELDS = stream(Field.values())
                .filter(f -> String.class.equals(f.getType()))
                .filter(f -> f!=FULLTEXT && f!=COVER_INFO).toArray(Field[]::new);   // stackoverlow, duh

    public String getFulltext() {
        return stream(STRING_FIELDS)
                .map(f -> (String)f.getOf(this))
                .collect(() -> new StringBuilder(150),StringBuilder::append,StringBuilder::append)
                .toString();
    }

/******************************************************************************/

    /**
     * @return index of the item in playlist belonging to this metadata or -1 if
     * not on playlist.
     */
    public int getPlaylistIndex() {
        return PlaylistManager.use(p -> p.indexOfSame(this)+1, -1);
    }

    /**
     * Returns index of the item in playlist belonging to this metadata.
     * Convenience method. Example: "15/30". "Order/total" items in playlist.
     * Note: This is non-tag information.
     * @return Empty string if not on playlist.
     */
    public String getPlaylistIndexInfo() {
        int i = getPlaylistIndex();
        return i==-1 ? "" : i + "/" + PlaylistManager.use(List::size,0);
    }

    /**
     * Returns chapters associated with this item. A {@link Chapter} represents
     * a time specific song comment. The result is ordered by natural order.
     * <p/>
     * Chapters are concatenated into string located in the Custom2 tag field.
     *
     * @return ordered list of chapters parsed from tag data
     */
    public List<Chapter> getChapters() {
        String chapterString = getCustom2();
        if(chapterString.isEmpty()) return list();

        List<Chapter> cs = new ArrayList<>();
        for(String c: getCustom2().split("\\|", 0)) {
            try {
                if(c.isEmpty()) continue;
                cs.add(new Chapter(c));
            } catch( IllegalArgumentException e) {
                log(Metadata.class).error("String '{}' not be parsed as chapter. Will be ignored.", c);
            }
        }

        cs.sort(Chapter::compareTo);
        return cs;
    }

    public boolean containsChapterAt(Duration at) {
        return getChapters().stream().anyMatch(ch->ch.getTime().equals(at));
    }

    @Override
    public Metadata toMeta() {
        return this;
    }

    @Override
    public PlaylistItem toPlaylist() {
        return new PlaylistItem(getURI(), getArtist(), getTitle(), getLengthInMs());
    }

/***************************** AS FIELDED TYPE ********************************/

    private Object getField(Field field) {
        return field.getOf(this);
    }

    public String getFieldS(Field f) {
        Object o = getField(f);
        return o==null ? "<none>" : f.toS(o, "<none>");
    }

    public String getFieldS(Field f, String no_val) {
        Object o = getField(f);
        return o==null ? no_val : f.toS(o,no_val);
    }

    public Field getMainField() { return Field.TITLE; }

/******************************* AS OBJECT ************************************/

    /**
     * Complete information on this object, non-tag data and types not
     * representable by string (e.g. cover) are excluded.
     *
     * @return comprehensive information about all string representable fields
     */
    public String getInfo() {
        return stream(Field.values()).filter(Field::isTypeStringRepresentable)
            .map(f -> f.name() + ": " + getField(f)).collect(joining("\n"));
    }

    @Override
    public String toString() {
        return Metadata.class.toString() + " " + getURI();
    }

    @Override
    public boolean equals(Object o) {
        return this==o || (o instanceof Metadata && Objects.equals(uri, ((Metadata) o).uri));
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + Objects.hashCode(this.uri);
        return hash;
    }

    @Override
    public int compareTo(Metadata i) {
        int r = getArtist().compareTo(i.getArtist());
        if (r!=0) return r;
            r = getAlbum().compareTo(i.getAlbum());
        if (r!=0) return r;
            r = Integer.compare(getDisc(), i.getDisc());
        if (r!=0) return r;
            r = Integer.compare(getTrack(), i.getTrack());
        if (r!=0) return r;
        return getTitle().compareTo(i.getTitle());
    }


    /**
     *
     */
    public enum Field implements ObjectField<Metadata> {

        PATH(Metadata::getPath,"Song location"),
        FILENAME(Metadata::getFilename,"Song file name without suffix"),
        FORMAT(Metadata::getFormat,"Song file type "),
        FILESIZE(Metadata::getFilesize,"Song file size"),
        ENCODING(Metadata::getEncodingType,"Song encoding"),
        BITRATE(Metadata::getBitrate,"Bits per second of the song - quality aspect."),
        ENCODER(Metadata::getEncoder,"Song encoder"),
        CHANNELS(Metadata::getChannels,"Number of channels"),
        SAMPLE_RATE(Metadata::getSampleRate,"Sample frequency"),
        LENGTH(Metadata::getLength,"Song length"),
        TITLE(Metadata::getTitle,"Song title"),
        ALBUM(Metadata::getAlbum,"Song album"),
        ARTIST(Metadata::getArtist,"Artist of the song"),
        ALBUM_ARTIST(Metadata::getAlbumArtist,"Artist of the song album"),
        COMPOSER(Metadata::getComposer,"Composer of the song"),
        PUBLISHER(Metadata::getPublisher,"Publisher of the album"),
        TRACK(Metadata::getTrack,"Song number within album"),
        TRACKS_TOTAL(Metadata::getTracksTotal,"Number of songs in the album"),
        TRACK_INFO(Metadata::getTrackInfo,"Complete song number in format: track/track total"),
        DISC(Metadata::getDisc,"Disc number within album"),
        DISCS_TOTAL(Metadata::getDiscsTotal,"Number of discs in the album"),
        DISCS_INFO(Metadata::getDiscInfo,"Complete disc number in format: disc/disc total"),
        GENRE(Metadata::getGenre,"Genre of the song"),
        YEAR(Metadata::getYear,"Year the album was published"),
        COVER(Metadata::getCover,"Cover of the song"),
        COVER_INFO(Metadata::getCoverInfo,"Cover information"),
        RATING(Metadata::getRatingPercent,"Song rating in 0-1 range"),
        RATING_RAW(Metadata::getRating,"Song rating tag value. Depends on tag type"),
        PLAYCOUNT(Metadata::getPlaycount,"Number of times the song was played."),
        CATEGORY(Metadata::getCategory,"Category of the song. Arbitrary"),
        COMMENT(Metadata::getComment,"User comment of the song. Arbitrary"),
        LYRICS(Metadata::getLyrics,"Lyrics for the song"),
        MOOD(Metadata::getMood,"Mood the song evokes"),
        COLOR(Metadata::getColor,"Color the song evokes"),
        TAGS(Metadata::getTags,"Tags associated with this song"),
        CHAPTERS(Metadata::getChapters,"Comments at specific time points of the song"),
        FULLTEXT(Metadata::getFulltext,"All possible fields merged into single text. Use for searching."),
        CUSTOM1(Metadata::getCustom1,"Custom field 1. Reserved for chapters."),
        CUSTOM2(Metadata::getCustom2,"Custom field 2. Reserved for color."),
        CUSTOM3(Metadata::getCustom3,"Custom field 3. Reserved for playback."),
        CUSTOM4(Metadata::getCustom4,"Custom field 4"),
        CUSTOM5(Metadata::getCustom5,"Custom field 5"),
        LAST_PLAYED(Metadata::getTimePlayedLast,"Marks time the song was played the last time."),
        FIRST_PLAYED(Metadata::getTimePlayedFirst,"Marks time the song was played the first time."),
        ADDED_TO_LIBRARY(Metadata::getTimeLibraryAdded,"Marks time the song was added to the library.");

        private static final Set<Field> NOT_AUTO_COMPLETABLE = EnumSet.of(
            TITLE,RATING_RAW,
            COMMENT,LYRICS,COLOR,PLAYCOUNT,PATH,FILENAME,FILESIZE,ENCODING,
            LENGTH,TRACK,TRACKS_TOTAL,TRACK_INFO,DISC,DISCS_TOTAL,DISCS_INFO,
            COVER,COVER_INFO,RATING,CHAPTERS
        );
        private static final Set<Field> VISIBLE = EnumSet.of(
            TITLE,ALBUM,ARTIST,LENGTH,TRACK_INFO,DISCS_INFO,RATING,PLAYCOUNT
        );
        private static final Set<Field> NOT_STRING_REPRESENTABLE = EnumSet.of(
            COVER, // can not be converted to string
            COVER_INFO, // requires cover to load (would kill performance).
            CHAPTERS, // raw string form unsuitable for viewing
            FULLTEXT // purely for search purposes
        );
        private static FileSize[] GROUPS_FILESIZE = new FileSize[65];
        private static Double[] GROUPS_RATING = new Double[21];
        private static final Double RATINGP_EMPTY = -1d;
        private static final Integer RATING_EMPTY = -1;

        static {
            // initialize file size group cache
            GROUPS_FILESIZE[64] = new FileSize(0);
            GROUPS_FILESIZE[0] = new FileSize(1);
            for(int i=1; i<63; i++)
                GROUPS_FILESIZE[i] = new FileSize(GROUPS_FILESIZE[i-1].inBytes()*2);
            // note that 2^63==Long.MAX_VALUE+1, meaning such value is impossible to use, so we
            // use max value instead. Note that this changes the 'unit' from 16EiB to 15.999... PiB
            GROUPS_FILESIZE[63] = new FileSize(Long.MAX_VALUE);
            // for(int i=0; i<=64; i++) // debug
            //     System.out.println(i + " : " + FILESIZE_GROUPS[i]);

            // initialize rating group cache
            for(int i=0; i<=20; i++)
                GROUPS_RATING[i] = i*5/100d;
        }

        private final String desc;
        private final Ƒ1<Metadata,?> extr;

        Field(Ƒ1<Metadata,?> extractor, String description) {
            mapEnumConstantName(this, util.Util::enumToHuman);
            this.desc = description;
            this.extr = extractor;
        }

        @Override
        public String description() {
            return desc;
        }

        @Override
        public boolean isTypeStringRepresentable() {
            return !NOT_STRING_REPRESENTABLE.contains(this);
        }

        @Override
        public Object getOf(Metadata m) {
            return extr.apply(m);
        }

        public Object getGroupedOf(Metadata m) {
            // Note that groups must include the 'empty' group for when the value is empty

            if(this==FILESIZE) {
                // filesize can not have empty value, every file has some size.
                return GROUPS_FILESIZE[64 - Long.numberOfLeadingZeros(m.filesize - 1)];
            }
            if(this==RATING) {
                if(m.rating==EMPTY.rating) return -1d; // empty group
                return GROUPS_RATING[(int)(m.getRatingPercent()*100/5)];
            }
            return extr.apply(m);
        }

        public boolean isFieldEmpty(Metadata m) {
            return equalNull(getOf(m), getOf(EMPTY));
        }

        @Override
        public Class getType() {
            // Because empty fields may return null, we can not rely on Metadata.EMPTY, so we handle
            // those fields manually.
            switch(this) {
                case BITRATE: return Bitrate.class;
                case COLOR: return Color.class;
                case YEAR: return Year.class;
                case FULLTEXT: return String.class;
                case FIRST_PLAYED:
                case LAST_PLAYED:
                case ADDED_TO_LIBRARY: return LocalDateTime.class;
                default : return Metadata.EMPTY.getField(this).getClass();
            }
        }

        /**
         * Returns true.
         * <p/>
         * {@inheritDoc}
         */
        @Override
        public boolean isTypeNumberNonegative() { return true; }

        public boolean isAutoCompleteable() {
            return isTypeStringRepresentable() && !NOT_AUTO_COMPLETABLE.contains(this);
        }

        @Override
        public String toS(Object o, String empty_val) {
            if(o==null || "".equals(o)) return empty_val;
            switch(this) {
                case RATING_RAW : return RATING_EMPTY==o ? empty_val : o.toString(); // we leverage Integer caching, hence ==
                case RATING : return RATINGP_EMPTY.equals(o) ? empty_val : String.format("%.2f", (double)o);
                case DISC :
                case DISCS_TOTAL :
                case TRACK :
                case TRACKS_TOTAL :
                case PLAYCOUNT : return equalNull(getOf(EMPTY),o) ? empty_val : o.toString();
                default : return o.toString();
            }
        }

        @Override
        public boolean c_visible() {
            return VISIBLE.contains(this);
        }

        @Override
        public double c_width() {
            return this==PATH || this==TITLE ? 150 : 50;
        }
    }
}