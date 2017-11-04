
package audio.tagging;

import audio.Item;
import audio.playlist.PlaylistItem;
import audio.playlist.PlaylistManager;
import audio.tagging.chapter.Chapter;
import gui.objects.image.cover.Cover;
import gui.objects.image.cover.Cover.CoverSource;
import gui.objects.image.cover.FileCover;
import gui.objects.image.cover.ImageCover;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javafx.beans.binding.ListExpression;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
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
import util.SwitchException;
import util.access.fieldvalue.ObjectField;
import util.file.AudioFileFormat;
import util.file.ImageFileFormat;
import util.file.Util;
import util.functional.Functors.Ƒ1;
import util.parsing.Parser;
import util.units.Bitrate;
import util.units.Dur;
import util.units.FileSize;
import util.units.NofX;
import static audio.tagging.ExtKt.getRatingMax;
import static audio.tagging.ExtKt.readAudioFile;
import static audio.tagging.Metadata.Field.COVER_INFO;
import static audio.tagging.Metadata.Field.FULLTEXT;
import static java.lang.Integer.parseInt;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static util.Util.emptyOr;
import static util.Util.localDateTimeFromMillis;
import static util.dev.Util.log;
import static util.file.Util.EMPTY_URI;
import static util.file.UtilKt.getNameWithoutExtensionOrRoot;
import static util.file.UtilKt.listChildren;
import static util.functional.Util.equalNull;
import static util.functional.Util.list;
import static util.functional.Util.setRO;
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
 * Metadata can be empty and should be used instead of null. See {@link #EMPTY}
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
 */
@Entity(name = "MetadataItem")
public final class Metadata extends Item {

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
	static final String TAG_ID_PLAYED_LAST = "PLAYED_LST";
	static final String TAG_ID_PLAYED_FIRST = "PLAYED_1ST";
	static final String TAG_ID_LIB_ADDED = "LIB_ADDED_";
	static final String TAG_ID_COLOR = "COLOR_____";
	static final String TAG_ID_TAGS = "TAG_______";

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
		uri = item.getUri().toString();
		if (item instanceof PlaylistItem) {
			PlaylistItem pItem = (PlaylistItem) item;
			artist = pItem.getArtist();
			duration = pItem.getTime().toMillis();
			title = pItem.getTitle();
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
	 */
	@SuppressWarnings("SpellCheckingInspection")
	Metadata(AudioFile audiofile) {
		File file = audiofile.getFile().getAbsoluteFile();
		uri = file.toURI().toString();
		filesize = FileSize.inBytes(file);

		loadHeaderFields(audiofile);

		// We would like to make sure tag always exists, but this probably involves
		// writing the tag. We want to stay just reading. If we don't mind, use:
		Tag tag = audiofile.getTagOrCreateAndSetDefault();
		// If the tag is null, we skip reading
//        Tag tag = audiofile.getTag();
		if (tag!=null) {
			loadGeneralFields(tag);
			switch (getFormat()) {
				case mp3: loadFieldsID3((AbstractID3Tag) tag); break;
				case flac: loadFieldsVorbis(((FlacTag) tag).getVorbisCommentTag()); break;
				case ogg: loadFieldsVorbis((VorbisCommentTag) tag); break;
				case wav: loadFieldsWAV((WavTag) tag); break;
				case mp4:
				case m4a: loadFieldsMP4((Mp4Tag) tag); break;
				default:   // do nothing for the rest;
			}
		}

	}

	/** loads all header fields */
	private void loadHeaderFields(AudioFile aFile) {
		AudioHeader header = aFile.getAudioHeader();
		if (header==null) {  // just in case
			log(Metadata.class).info("Header not found: " + getUri());
		} else {
			bitrate = (int) header.getBitRateAsNumber();
			duration = 1000*header.getTrackLength();
			// format and encoding type are switched in jaudiotagger library...
			encoding = emptyOr(header.getFormat());
			channels = emptyOr(header.getChannels());
			sample_rate = emptyOr(header.getSampleRate());
		}
	}

	/** loads all generally supported fields */
	@SuppressWarnings("UnnecessaryContinue")
	private void loadGeneralFields(Tag tag) {
		encoder = getGeneral(tag, FieldKey.ENCODER);

		title = getGeneral(tag, FieldKey.TITLE);
		album = getGeneral(tag, FieldKey.ALBUM);
//        artists = getGenerals(tag,FieldKey.ARTIST);
		artist = getGeneral(tag, FieldKey.ARTIST);
		album_artist = getGeneral(tag, FieldKey.ALBUM_ARTIST);
		composer = getGeneral(tag, FieldKey.COMPOSER);

		// track
		String tr = getGeneral(tag, FieldKey.TRACK);
		int i = tr.indexOf('/');
		if (i!=-1) {
			// some apps use TRACK for "x/y" string format, we cover that
			track = number(tr.substring(0, i));
			tracks_total = number(tr.substring(i + 1, tr.length()));
		} else {
			track = number(tr);
			tracks_total = getNumber(tag, FieldKey.TRACK_TOTAL);
		}

		// disc
		String dr = getGeneral(tag, FieldKey.DISC_NO);
		int j = dr.indexOf('/');
		if (j!=-1) {
			// some apps use DISC_NO for "x/y" string format, we cover that
			disc = number(dr.substring(0, j));
			discs_total = number(dr.substring(j + 1, dr.length()));
		} else {
			disc = number(dr);
			discs_total = getNumber(tag, FieldKey.DISC_TOTAL);
		}

		playcount = getNumber(tag, FieldKey.CUSTOM3);
		genre = getGeneral(tag, FieldKey.GENRE);
		year = getNumber(tag, FieldKey.YEAR);
		category = getGeneral(tag, FieldKey.GROUPING);
		comment = getComment(tag);
		lyrics = getGeneral(tag, FieldKey.LYRICS);
		mood = getGeneral(tag, FieldKey.MOOD);
		custom1 = getGeneral(tag, FieldKey.CUSTOM1);
		custom2 = getGeneral(tag, FieldKey.CUSTOM2);
		custom3 = getGeneral(tag, FieldKey.CUSTOM3);
		custom4 = getGeneral(tag, FieldKey.CUSTOM4);
		custom5 = getGeneral(tag, FieldKey.CUSTOM5);

		// Following acquire data from special tag
		if (!custom5.isEmpty()) {
			for (String tagField : list(split(custom5, SEPARATOR_GROUP.toString()))) {
				if (tagField.length()<10) continue;      // skip deformed to avoid exception
				String tagId = tagField.substring(0, 10);
				String tagValue = tagField.substring(10);
				switch (tagId) {
					case TAG_ID_PLAYED_FIRST: playedFirst = tagValue; continue;
					case TAG_ID_PLAYED_LAST: playedLast = tagValue; continue;
					case TAG_ID_LIB_ADDED: libraryAdded = tagValue; continue;
					case TAG_ID_COLOR: color = tagValue; continue;
					case TAG_ID_TAGS: tags = tagValue; continue;
				}
			}
		}
	}

	private String getGeneral(Tag tag, FieldKey f) {
		try {
			String s = tag.getFirst(f); // can throw UnsupportedOperationException
			if (s==null) {
				log(Metadata.class).warn("Jaudiotagger returned null for {} of {}", f, uri);
			}
			return s;
		} catch (UnsupportedOperationException|KeyNotFoundException e) {
			log(Metadata.class).warn("Jaudiotagger failed to read {} of {}", f, uri, e);
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
		} catch (NumberFormatException e) {
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
		for (TagField t : fields) // custom
			if (!t.toString().contains("Description"))
				i = fields.indexOf(t);
		if (i>-1) return tag.getValue(FieldKey.COMMENT, i);
		else return "";
	}

	private static List<String> getGenerals(Tag tag, FieldKey f) {
		List<String> out = new ArrayList<>();
		if (!tag.hasField(f)) return out;

		try {
//        for (TagField field: tag.getFields(f)) {
//            String tmp = field.toString();
//                   tmp = tmp.substring(6, tmp.length()-3);
//            out.add(Util.emptyOr(tmp));
//        }
			for (String val : tag.getAll(f))
				out.add(util.Util.emptyOr(val));
		} catch (Exception w) {
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

	@SuppressWarnings("StatementWithEmptyBody")
	private void loadFieldsID3(AbstractID3Tag tag) {
		if (tag instanceof AbstractID3v1Tag) {
			// RATING + PLAYCOUNT +PUBLISHER + CATEGORY ----------------------------
			// id3 has no fields for this (note that when we write these fields we convert tag to
			// ID3v2, so this only happens with untagged songs, which we don't care about
		} else if (tag instanceof AbstractID3v2Tag) {
			AbstractID3v2Tag t = (AbstractID3v2Tag) tag;
			// RATING + PLAYCOUNT --------------------------------------------------
			// we use POPM field (rating + counter + mail/user)
			AbstractID3v2Frame frame1 = t.getFirstField(ID3v24Frames.FRAME_ID_POPULARIMETER);
			// if not present we get null and leave default values
			if (frame1!=null) {
				// we obtain body for the field
				FrameBodyPOPM body1 = (FrameBodyPOPM) frame1.getBody();
				// once again the body of the field might not be there
				if (body1!=null) {
					long rat = body1.getRating(); //returns null if empty
					long cou = body1.getCounter(); //returns null if empty

					// i do not know why the values themselves are Long, but we only need int
					// both for rating and playcount.
					// all is good until the tag is actually damaged and the int can really
					// overflow during conversion and we get ArithmeticException
					// so we catch it and ignore the value
					try {
						rating = Math.toIntExact(rat);
					} catch (ArithmeticException ignored) {}

					try {
						int pc = Math.toIntExact(cou);
						if (pc>playcount) playcount = pc;
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
	@SuppressWarnings("StatementWithEmptyBody")
	private void loadFieldsWAV(WavTag tag) {
		AbstractID3v2Tag t = tag.getID3Tag();
		if (t!=null) {
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
		// sometimes we get unintelligible values for some reason (don't ask me),
		// Mp3Tagger app can recognize them somehow
		// the values appear consistent so lets use them
		if (r==12592) rating = 100;
		else if (r==14384) rating = 80;
		else if (r==13872) rating = 60;
		else if (r==13360) rating = 40;
		else if (r==12848) rating = 20;
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
		if (publisher.isEmpty()) publisher = emptyOr(tag.getFirst(Mp4FieldKey.MM_PUBLISHER));

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
		if (0<rating && rating<6) rating *= 20;

		// PLAYCOUNT -----------------------------------------------------------
		// if we want to support playcount in vorbis specific field, we can
		// just lets not overwrite value we obtained with previous methods
		// if (playcount==-1) playcount = number(emptyOr(tag.getFirst("PLAYCOUNT")));

		// PUBLISHER -----------------------------------------------------------
		publisher = emptyOr(tag.getFirst("PUBLISHER"));

		// CATEGORY ------------------------------------------------------------
		// try to get category the winamp way, don't if we already have a value
		if (category.isEmpty()) category = emptyOr(tag.getFirst("CATEGORY"));
	}

	@Override
	public String getId() {
		return uri;
	}

	public void setId(String id) {
		this.uri = id;
	}

	@Override
	public URI getUri() {
		return URI.create(uri.replace(" ", "%20"));
	}

	/**
	 * @return true if this metadata is empty.
	 * @see #EMPTY
	 */
	public boolean isEmpty() {
		return this==EMPTY;
	}

	@Override
	public String getPathAsString() {
		return isEmpty() ? "" : super.getPathAsString();
	}

	@Override
	public String getFilename() {
		return isEmpty() ? "" : super.getFilename();
	}

	@Override
	public FileSize getFileSize() {
		return new FileSize(filesize);
	}

	/** {@link #getFileSize()})} in bytes */
	public long getFileSizeInB() {
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
	 *
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
	 * For example: {@literal MPEG-1 Layer 3}.
	 *
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
	public Dur getLength() {
		return new Dur(duration);
	}

	/** @return the length in milliseconds */
	public double getLengthInMs() {
		return duration;
	}

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
	 * If you want to get all artists don't use this method.
	 *
	 * @return the first artist "" if empty.
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
	 * and don't intend to sort or use the numerical values
	 * then use this method instead of {@link #getTrack()} and {@link #getTracksTotal())}.
	 * <p/>
	 * If disc or disc_total information is unknown the respective portion in the
	 * output will be substituted by character '?'.
	 * Example: 1/1, 4/23, ?/?, ...
	 *
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
	 * and don't intend to sort or use the numerical values
	 * then use this method instead of {@link #getDisc() ()} and {@link #getDiscsTotal()}.
	 * <p/>
	 * If disc or disc_total information is unknown the respective portion in the
	 * output will be substituted by character '?'.
	 * Example: 1/1, ?/?, 5/?, ...
	 *
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
		switch (source) {
			case TAG: return getCoverOfTag();
			case DIRECTORY: return getCoverOfDir().map(f -> (Cover) new FileCover(f, "")).orElse(Cover.EMPTY);
			case ANY: return stream(CoverSource.TAG, CoverSource.DIRECTORY)
					.map(this::getCover)
					.filter(c -> !c.isEmpty())
					.findFirst().orElse(Cover.EMPTY);
			default: throw new SwitchException(source);
		}
	}

	private void loadCover() {
		if (cover!=null || cover_loaded) return;
		cover_loaded = true;
		AudioFile af = isFileBased() ? readAudioFile(getFile()) : null;
		Tag tag = af!=null ? af.getTagOrCreateAndSetDefault() : null;
		cover = tag==null ? null : tag.getFirstArtwork();
	}

	@SuppressWarnings("TryWithIdenticalCatches")
	private Cover getCoverOfTag() {
		try {
			loadCover();
			if (cover==null) return new ImageCover((Image) null, getCoverInfo());
			else return new ImageCover((BufferedImage) cover.getImage(), getCoverInfo());
		} catch (IOException e) {
			return new ImageCover((Image) null, getCoverInfo());
		} catch (NullPointerException ex) {
			// jaudiotagger bug, Artwork.getImage() can throw NullPointerException sometimes
			// at java.io.ByteArrayInputStream.<init>(ByteArrayInputStream.java:106) ~[na:na]
			// at org.jaudiotagger.tag.images.StandardArtwork.getImage(StandardArtwork.java:95) ~[jaudiotagger-2.2.6-SNAPSHOT.jar:na]
			return new ImageCover((Image) null, getCoverInfo());
		}
	}

	@Transient
	private boolean cover_loaded = false;

	private String getCoverInfo() {
		try {
			loadCover();
			return cover.getDescription() + " " + cover.getMimeType() + " "
					+ ((RenderedImage) cover.getImage()).getWidth() + "x"
					+ ((RenderedImage) cover.getImage()).getHeight();
		} catch (IOException|NullPointerException e) {
			// ^ why do we catch NullPointerException here? Not a bug fix! Investigate & remove!
			return "";  // never mind errors. Return "" on fail.    TODO: return Try or something, this is just embarrassing
		}
	}

	/**
	 * Returns the cover image file or no-op if this item is not file based.
	 * <p/>
	 * If the Image object of the cover suffices, it is recommended to avoid this method, or even better use
	 * {@link #getCover(gui.objects.image.cover.Cover.CoverSource)}
	 *
	 * @return cover file
	 */
	private Optional<File> getCoverOfDir() {
		if (!isFileBased()) return Optional.empty();

		List<File> fs = listChildren(getLocation()).collect(toList());
		return stream(getFilename(), getTitle(), getAlbum(), "cover", "folder")
				.flatMap(filename -> fs.stream().filter(f -> getNameWithoutExtensionOrRoot(f).equalsIgnoreCase(filename)))
				.filter(ImageFileFormat::isSupported)
				.findFirst();
	}

	/** @return the rating or -1 if empty. */
	public int getRating() {
		return rating;
	}

	/**
	 * Recommended method to use to obtain rating.
	 *
	 * @return the rating in 0-1 percent range
	 */
	public double getRatingPercent() {
		return rating==-1 ? 0 : rating/(double) getRatingMax(this);
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
		return playcount==-1 ? 0 : playcount;
	}

	/**
	 * tag field: grouping
	 *
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
		return color.isEmpty() ? null : Parser.DEFAULT.ofS(Color.class, color).getOr(null);
	}

	/** @return the tags, "" if empty. */
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

	public LocalDateTime getTimePlayedFirst() {
		if (playedFirst.isEmpty()) return null;
		return localDateTimeFromMillis(playedFirst);
	}

	public LocalDateTime getTimePlayedLast() {
		if (playedLast.isEmpty()) return null;
		return localDateTimeFromMillis(playedLast);
	}

	public LocalDateTime getTimeLibraryAdded() {
		if (libraryAdded.isEmpty()) return null;
		return localDateTimeFromMillis(libraryAdded);
	}

	private static final Field[] STRING_FIELDS = stream(Field.FIELDS)
			.filter(f -> String.class.equals(f.getType()))
			.filter(f -> f!=FULLTEXT && f!=COVER_INFO)  // prevents StackOverflowException
			.toArray(Field[]::new);

	// TODO: cache
	public String getFulltext() {
		return stream(STRING_FIELDS)
				.map(f -> (String) f.getOf(this))
				.collect(() -> new StringBuilder(150), StringBuilder::append, StringBuilder::append)
				.toString();
	}

	/**
	 * @return index of the item in playlist belonging to this metadata or -1 if not on playlist.
	 */
	public int getPlaylistIndex() {
		return PlaylistManager.use(p -> p.indexOfSame(this) + 1, -1);
	}

	/**
	 * Returns index of the item in playlist belonging to this metadata.
	 * Convenience method. Example: "15/30". "Order/total" items in playlist.
	 * Note: This is non-tag information.
	 *
	 * @return Empty string if not on playlist.
	 */
	public String getPlaylistIndexInfo() {
		int i = getPlaylistIndex();
		return i==-1 ? "" : i + "/" + PlaylistManager.use(ListExpression::size, 0);
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
		if (chapterString.isEmpty()) return list();

		List<Chapter> cs = new ArrayList<>();
		for (String c : getCustom2().split("\\|", 0)) {
			try {
				if (c.isEmpty()) continue;
				cs.add(new Chapter(c));
			} catch (IllegalArgumentException e) {
				log(Metadata.class).error("String '{}' not be parsed as chapter. Will be ignored.", c);
			}
		}

		cs.sort(Chapter::compareTo);
		return cs;
	}

	public boolean containsChapterAt(Duration at) {
		return getChapters().stream().anyMatch(ch -> ch.getTime().equals(at));
	}

	@Override
	public Metadata toMeta() {
		return this;
	}

	@Override
	public PlaylistItem toPlaylist() {
		return new PlaylistItem(getUri(), getArtist(), getTitle(), getLengthInMs());
	}

/* --------------------- AS FIELDED TYPE ---------------------------------------------------------------------------- */

	private <T> T getField(Field<T> field) {
		return field.getOf(this);
	}

	public <T> String getFieldS(Field<T> f) {
		T o = getField(f);
		return o==null ? "<none>" : f.toS(o, "<none>");
	}

	public <T> String getFieldS(Field<T> f, String no_val) {
		T o = getField(f);
		return o==null ? no_val : f.toS(o, no_val);
	}

	public Field<?> getMainField() { return Field.TITLE; }

/* --------------------- AS OBJECT ---------------------------------------------------------------------------------- */

	/**
	 * Complete information on this object, non-tag data and types not
	 * representable by string (e.g. cover) are excluded.
	 *
	 * @return comprehensive information about all string representable fields
	 */
	public String getInfo() {
		return stream(Field.FIELDS)
				.filter(Field::isTypeStringRepresentable)
				.map(f -> f.name() + ": " + getField(f))
				.collect(joining("\n"));
	}

	@Override
	public String toString() {
		return Metadata.class.toString() + " " + getUri();
	}

	@Override
	public boolean equals(Object o) {
		return this==o || (o instanceof Metadata && Objects.equals(uri, ((Metadata) o).uri));
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 79*hash + Objects.hashCode(this.uri);
		return hash;
	}

	/**
	 * Compares with other metadata by attributes in the exact order:
	 * <ul>
	 * <li> artist
	 * <li> album
	 * <li> disc number
	 * <li> track number
	 * <li> title
	 * </ul>
	 */
	public int compareTo(Metadata m) {
		int r = getArtist().compareTo(m.getArtist());
		if (r!=0) return r;
		r = getAlbum().compareTo(m.getAlbum());
		if (r!=0) return r;
		r = Integer.compare(getDisc(), m.getDisc());
		if (r!=0) return r;
		r = Integer.compare(getTrack(), m.getTrack());
		if (r!=0) return r;
		r = getTitle().compareTo(m.getTitle());
		return r;
	}

	/**
	 *
	 */
	@SuppressWarnings("SpellCheckingInspection")
	public static class Field<T> implements ObjectField<Metadata,T> {

		public static final Set<Field<?>> FIELDS = new HashSet<>();
		public static final Set<String> FIELD_NAMES = new HashSet<>();

		public static final Field<String> PATH = new Field<>(Metadata::getPathAsString, "Path", "Song location");
		public static final Field<String> FILENAME = new Field<>(Metadata::getFilename, "Filename", "Song file name without suffix");
		public static final Field<AudioFileFormat> FORMAT = new Field<>(Metadata::getFormat, "Format", "Song file type ");
		public static final Field<FileSize> FILESIZE = new Field<>(Metadata::getFileSize, "Filesize", "Song file size");
		public static final Field<String> ENCODING = new Field<>(Metadata::getEncodingType, "Encoding", "Song encoding");
		public static final Field<Bitrate> BITRATE = new Field<>(Metadata::getBitrate, "Bitrate", "Bits per second of the song - quality aspect.");
		public static final Field<String> ENCODER = new Field<>(Metadata::getEncoder, "Encoder", "Song encoder");
		public static final Field<String> CHANNELS = new Field<>(Metadata::getChannels, "Channels", "Number of channels");
		public static final Field<String> SAMPLE_RATE = new Field<>(Metadata::getSampleRate, "Sample_rate", "Sample frequency");
		public static final Field<Dur> LENGTH = new Field<>(Metadata::getLength, "Length", "Song length");
		public static final Field<String> TITLE = new Field<>(Metadata::getTitle, "Title", "Song title");
		public static final Field<String> ALBUM = new Field<>(Metadata::getAlbum, "Album", "Song album");
		public static final Field<String> ARTIST = new Field<>(Metadata::getArtist, "Artist", "Artist of the song");
		public static final Field<String> ALBUM_ARTIST = new Field<>(Metadata::getAlbumArtist, "Album_artist", "Artist of the song album");
		public static final Field<String> COMPOSER = new Field<>(Metadata::getComposer, "Composer", "Composer of the song");
		public static final Field<String> PUBLISHER = new Field<>(Metadata::getPublisher, "Publisher", "Publisher of the album");
		public static final Field<Integer> TRACK = new Field<>(Metadata::getTrack, "Track", "Song number within album");
		public static final Field<Integer> TRACKS_TOTAL = new Field<>(Metadata::getTracksTotal, "Tracks_total", "Number of songs in the album");
		public static final Field<NofX> TRACK_INFO = new Field<>(Metadata::getTrackInfo, "Track_info", "Complete song number in format: track/track total");
		public static final Field<Integer> DISC = new Field<>(Metadata::getDisc, "Disc", "Disc number within album");
		public static final Field<Integer> DISCS_TOTAL = new Field<>(Metadata::getDiscsTotal, "Discs_total", "Number of discs in the album");
		public static final Field<NofX> DISCS_INFO = new Field<>(Metadata::getDiscInfo, "Discs_info", "Complete disc number in format: disc/disc total");
		public static final Field<String> GENRE = new Field<>(Metadata::getGenre, "Genre", "Genre of the song");
		public static final Field<Year> YEAR = new Field<>(Metadata::getYear, "Year", "Year the album was published");
		public static final Field<Cover> COVER = new Field<>(Metadata::getCoverOfTag, "Cover", "Cover of the song");
		public static final Field<String> COVER_INFO = new Field<>(Metadata::getCoverInfo, "Cover_info", "Cover information");
		public static final Field<Double> RATING = new Field<>(Metadata::getRatingPercent, "Rating", "Song rating in 0-1 range");
		public static final Field<Integer> RATING_RAW = new Field<>(Metadata::getRating, "Rating_raw", "Song rating tag value. Depends on tag type");
		public static final Field<Integer> PLAYCOUNT = new Field<>(Metadata::getPlaycount, "Playcount", "Number of times the song was played.");
		public static final Field<String> CATEGORY = new Field<>(Metadata::getCategory, "Category", "Category of the song. Arbitrary");
		public static final Field<String> COMMENT = new Field<>(Metadata::getComment, "Comment", "User comment of the song. Arbitrary");
		public static final Field<String> LYRICS = new Field<>(Metadata::getLyrics, "Lyrics", "Lyrics for the song");
		public static final Field<String> MOOD = new Field<>(Metadata::getMood, "Mood", "Mood the song evokes");
		public static final Field<Color> COLOR = new Field<>(Metadata::getColor, "Color", "Color the song evokes");
		public static final Field<String> TAGS = new Field<>(Metadata::getTags, "Tags", "Tags associated with this song");
		public static final Field<List<Chapter>> CHAPTERS = new Field<>(Metadata::getChapters, "Chapters", "Comments at specific time points of the song");
		public static final Field<String> FULLTEXT = new Field<>(Metadata::getFulltext, "Fulltext", "All possible fields merged into single text. Use for searching.");
		public static final Field<String> CUSTOM1 = new Field<>(Metadata::getCustom1, "Custom1", "Custom field 1. Reserved for chapters.");
		public static final Field<String> CUSTOM2 = new Field<>(Metadata::getCustom2, "Custom2", "Custom field 2. Reserved for color.");
		public static final Field<String> CUSTOM3 = new Field<>(Metadata::getCustom3, "Custom3", "Custom field 3. Reserved for playback.");
		public static final Field<String> CUSTOM4 = new Field<>(Metadata::getCustom4, "Custom4", "Custom field 4");
		public static final Field<String> CUSTOM5 = new Field<>(Metadata::getCustom5, "Custom5", "Custom field 5");
		public static final Field<LocalDateTime> LAST_PLAYED = new Field<>(Metadata::getTimePlayedLast, "Last_played", "Marks time the song was played the last time.");
		public static final Field<LocalDateTime> FIRST_PLAYED = new Field<>(Metadata::getTimePlayedFirst, "First_played", "Marks time the song was played the first time.");
		public static final Field<LocalDateTime> ADDED_TO_LIBRARY = new Field<>(Metadata::getTimeLibraryAdded, "Added_to_library", "Marks time the song was added to the library.");

		private static final Set<Field> NOT_AUTO_COMPLETABLE = setRO(
				TITLE, RATING_RAW,
				COMMENT, LYRICS, COLOR, PLAYCOUNT, PATH, FILENAME, FILESIZE, ENCODING,
				LENGTH, TRACK, TRACKS_TOTAL, TRACK_INFO, DISC, DISCS_TOTAL, DISCS_INFO,
				COVER, COVER_INFO, RATING, CHAPTERS
		);
		private static final Set<Field> VISIBLE = setRO(
				TITLE, ALBUM, ARTIST, LENGTH, TRACK_INFO, DISCS_INFO, RATING, PLAYCOUNT
		);
		private static final Set<Field> NOT_STRING_REPRESENTABLE = setRO(
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
			for (int i = 1; i<63; i++)
				GROUPS_FILESIZE[i] = new FileSize(GROUPS_FILESIZE[i - 1].inBytes()*2);
			// note that 2^63==Long.MAX_VALUE+1, meaning such value is impossible to use, so we
			// use max value instead. Note that this changes the 'unit' from 16EiB to 15.999... PiB
			GROUPS_FILESIZE[63] = new FileSize(Long.MAX_VALUE);
			// for (int i=0; i<=64; i++) // debug
			//     System.out.println(i + " : " + FILESIZE_GROUPS[i]);

			// initialize rating group cache
			for (int i = 0; i<=20; i++)
				GROUPS_RATING[i] = i*5/100d;
		}

		private final String name;
		private final String description;
		private final Ƒ1<? super Metadata,? extends T> extractor;

		Field(Ƒ1<? super Metadata,? extends T> extractor, String name, String description) {
			this.name = name;
			this.description = description;
			this.extractor = extractor;
			FIELDS.add(this);
			FIELD_NAMES.add(name);
		}

		public static Field<?> valueOf(String s) {
			// TODO: use Map
			if (PATH.name().equals(s)) return PATH;
			if (FILENAME.name().equals(s)) return FILENAME;
			if (FORMAT.name().equals(s)) return FORMAT;
			if (FILESIZE.name().equals(s)) return FILESIZE;
			if (ENCODING.name().equals(s)) return ENCODING;
			if (BITRATE.name().equals(s)) return BITRATE;
			if (ENCODER.name().equals(s)) return ENCODER;
			if (CHANNELS.name().equals(s)) return CHANNELS;
			if (SAMPLE_RATE.name().equals(s)) return SAMPLE_RATE;
			if (LENGTH.name().equals(s)) return LENGTH;
			if (TITLE.name().equals(s)) return TITLE;
			if (ALBUM.name().equals(s)) return ALBUM;
			if (ARTIST.name().equals(s)) return ARTIST;
			if (ALBUM_ARTIST.name().equals(s)) return ALBUM_ARTIST;
			if (COMPOSER.name().equals(s)) return COMPOSER;
			if (PUBLISHER.name().equals(s)) return PUBLISHER;
			if (TRACK.name().equals(s)) return TRACK;
			if (TRACKS_TOTAL.name().equals(s)) return TRACKS_TOTAL;
			if (TRACK_INFO.name().equals(s)) return TRACK_INFO;
			if (DISC.name().equals(s)) return DISC;
			if (DISCS_TOTAL.name().equals(s)) return DISCS_TOTAL;
			if (DISCS_INFO.name().equals(s)) return DISCS_INFO;
			if (GENRE.name().equals(s)) return GENRE;
			if (YEAR.name().equals(s)) return YEAR;
			if (COVER.name().equals(s)) return COVER;
			if (COVER_INFO.name().equals(s)) return COVER_INFO;
			if (RATING.name().equals(s)) return RATING;
			if (RATING_RAW.name().equals(s)) return RATING_RAW;
			if (PLAYCOUNT.name().equals(s)) return PLAYCOUNT;
			if (CATEGORY.name().equals(s)) return CATEGORY;
			if (COMMENT.name().equals(s)) return COMMENT;
			if (LYRICS.name().equals(s)) return LYRICS;
			if (MOOD.name().equals(s)) return MOOD;
			if (COLOR.name().equals(s)) return COLOR;
			if (TAGS.name().equals(s)) return TAGS;
			if (CHAPTERS.name().equals(s)) return CHAPTERS;
			if (FULLTEXT.name().equals(s)) return FULLTEXT;
			if (CUSTOM1.name().equals(s)) return CUSTOM1;
			if (CUSTOM2.name().equals(s)) return CUSTOM2;
			if (CUSTOM3.name().equals(s)) return CUSTOM3;
			if (CUSTOM4.name().equals(s)) return CUSTOM4;
			if (CUSTOM5.name().equals(s)) return CUSTOM5;
			if (LAST_PLAYED.name().equals(s)) return LAST_PLAYED;
			if (FIRST_PLAYED.name().equals(s)) return FIRST_PLAYED;
			if (ADDED_TO_LIBRARY.name().equals(s)) return ADDED_TO_LIBRARY;
			throw new SwitchException(s);
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public String toString() {
			return name;
		}

		@Override
		public String description() {
			return description;
		}

		@Override
		public boolean isTypeStringRepresentable() {
			return !NOT_STRING_REPRESENTABLE.contains(this);
		}

		@Override
		public T getOf(Metadata m) {
			return extractor.apply(m);
		}

		public Object getGroupedOf(Metadata m) {
			// Note that groups must include the 'empty' group for when the value is empty

			if (this==FILESIZE) {
				// file size can not have empty value, every file has some size.
				return GROUPS_FILESIZE[64 - Long.numberOfLeadingZeros(m.filesize - 1)];
			}
			if (this==RATING) {
				if (m.rating==EMPTY.rating) return -1d; // empty group
				return GROUPS_RATING[(int) (m.getRatingPercent()*100/5)];
			}
			return extractor.apply(m);
		}

		public boolean isFieldEmpty(Metadata m) {
			return equalNull(getOf(m), getOf(EMPTY));
		}

		@SuppressWarnings("unchecked")
		@Override
		public Class<T> getType() {
			// TODO: use type field
			// Because empty fields may return null, we can not always rely on Metadata.EMPTY, so we handle
			// those fields manually.
			if (this==BITRATE) return (Class<T>) Bitrate.class;
			if (this==COLOR) return (Class<T>) Color.class;
			if (this==YEAR) return (Class<T>) Year.class;
			if (this==FULLTEXT) return (Class<T>) String.class;
			if (this==FIRST_PLAYED || this==LAST_PLAYED || this==ADDED_TO_LIBRARY)
				return (Class<T>) LocalDateTime.class;
			return (Class<T>) Metadata.EMPTY.getField(this).getClass();
		}

		/**
		 * Returns true.
		 * <p/>
		 * {@inheritDoc}
		 */
		@Override
		public boolean isTypeNumberNoNegative() { return true; }

		public boolean isAutoCompletable() {
			return isTypeStringRepresentable() && !NOT_AUTO_COMPLETABLE.contains(this);
		}

		@Override
		public String toS(T o, String substitute) {
			if (o==null || "".equals(o)) return substitute;
			if (this==RATING_RAW)
				return RATING_EMPTY==o ? substitute : o.toString(); // we leverage Integer caching, hence ==
			if (this==RATING) return RATINGP_EMPTY.equals(o) ? substitute : String.format("%.2f", (double) o);
			if (this==DISC || this==DISCS_TOTAL || this==TRACK || this==TRACKS_TOTAL || this==PLAYCOUNT)
				return equalNull(getOf(EMPTY), o) ? substitute : o.toString();
			return o.toString();
		}

		@Override
		public boolean c_visible() {
			return VISIBLE.contains(this);
		}

		@Override
		public double c_width() {
			return this==PATH || this==TITLE ? 160 : 60;
		}
	}
}