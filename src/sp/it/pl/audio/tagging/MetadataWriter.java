
package sp.it.pl.audio.tagging;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.scene.paint.Color;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldDataInvalidException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.KeyNotFoundException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.flac.FlacTag;
import org.jaudiotagger.tag.id3.AbstractID3v2Frame;
import org.jaudiotagger.tag.id3.AbstractID3v2Tag;
import org.jaudiotagger.tag.id3.ID3v24Frame;
import org.jaudiotagger.tag.id3.ID3v24Frames;
import org.jaudiotagger.tag.id3.ID3v24Tag;
import org.jaudiotagger.tag.id3.framebody.FrameBodyPCNT;
import org.jaudiotagger.tag.id3.framebody.FrameBodyPOPM;
import org.jaudiotagger.tag.id3.framebody.FrameBodyTPUB;
import org.jaudiotagger.tag.images.ArtworkFactory;
import org.jaudiotagger.tag.mp4.Mp4FieldKey;
import org.jaudiotagger.tag.mp4.Mp4Tag;
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag;
import org.jaudiotagger.tag.wav.WavTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sp.it.pl.audio.Item;
import sp.it.pl.audio.Player;
import sp.it.pl.service.notif.Notifier;
import sp.it.pl.util.SwitchException;
import sp.it.pl.util.file.AudioFileFormat;
import sp.it.pl.util.units.NofX;
import static java.lang.Math.max;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.jaudiotagger.tag.FieldKey.CUSTOM3;
import static org.jaudiotagger.tag.FieldKey.RATING;
import static sp.it.pl.audio.tagging.ExtKt.clipRating;
import static sp.it.pl.audio.tagging.ExtKt.getRatingMax;
import static sp.it.pl.audio.tagging.ExtKt.readAudioFile;
import static sp.it.pl.audio.tagging.Metadata.SEPARATOR_GROUP;
import static sp.it.pl.audio.tagging.Metadata.SEPARATOR_UNIT;
import static sp.it.pl.audio.tagging.Metadata.TAG_ID_COLOR;
import static sp.it.pl.audio.tagging.Metadata.TAG_ID_LIB_ADDED;
import static sp.it.pl.audio.tagging.Metadata.TAG_ID_PLAYED_FIRST;
import static sp.it.pl.audio.tagging.Metadata.TAG_ID_PLAYED_LAST;
import static sp.it.pl.audio.tagging.Metadata.TAG_ID_TAGS;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.pl.util.Util.clip;
import static sp.it.pl.util.Util.emptyOr;
import static sp.it.pl.util.async.AsyncKt.runFX;
import static sp.it.pl.util.functional.Util.list;
import static sp.it.pl.util.functional.Util.split;
import static sp.it.pl.util.functional.Util.stream;

/**
 * Manages writing Metadata objects back into files. Handles all tag related data
 * for items.
 * <p/>
 * The writer must be instantiated for use. It is reusable for an item.
 */
// TODO: limit rating bounds value, multiple values, id3 popularimeter mail settings
public class MetadataWriter extends Item {

	private static final Logger LOGGER = LoggerFactory.getLogger(MetadataWriter.class);

	/**
	 * Constructs metadata writer for given item.
	 *
	 * @return writer or null if error occurs.
	 * @throws UnsupportedOperationException if item not file based
	 */
	private static MetadataWriter create(Item item) {
		if (!item.isFileBased()) throw new UnsupportedOperationException("Item must be file based");

		MetadataWriter w = new MetadataWriter();
		w.reset(item);
		return w.audioFile==null ? null : w;
	}

	private static AbstractID3v2Tag wavToId3(WavTag tag) {
		AbstractID3v2Tag t = tag.getID3Tag();
		if (t==null) {
			tag.setID3Tag(new org.jaudiotagger.tag.id3.ID3v24Tag());
			t = tag.getID3Tag();
		}
		return t;
	}

/* ------------------------------------------------------------------------------------------------------------------ */

	// state
	private File file;
	private AudioFile audioFile;
	private Tag tag;
	private int fields_changed;
	private boolean hasCorruptedTag;
	// properties
	private final ReadOnlyBooleanWrapper isWriting = new ReadOnlyBooleanWrapper(false);
	public final ReadOnlyBooleanProperty writing = isWriting.getReadOnlyProperty();

	// dont provide access here
	public MetadataWriter() {}

	private MetadataWriter(File file, AudioFile audioFile) {
		this.file = file;
		this.audioFile = audioFile;
	}

	@Override
	public URI getUri() {
		if (file==null) throw new IllegalStateException("Illegal getUri call. metadata writer state not initialized.");
		return file.toURI();
	}

	@Override
	public File getFile() {
		return file;
	}

	/** @param encoder the encoder to set */
	public void setEncoder(String encoder) {
		setGeneralField(FieldKey.ENCODER, encoder);
	}

	/** @param album the album to set */
	public void setAlbum(String album) {
		setGeneralField(FieldKey.ALBUM, album);
	}

	/** @param val the artist to set */
	public void setArtist(String val) {
		setGeneralField(FieldKey.ARTIST, val);
	}

	/** @param val the album_artist to set */
	public void setAlbum_artist(String val) {
		setGeneralField(FieldKey.ALBUM_ARTIST, val);
	}

	/** @param artists the artists to set */
	public void setArtists(List<String> artists) {
		if (artists==null || artists.isEmpty())
			setArtist(null);
		else
			artists.stream().filter(String::isEmpty).forEach(a -> {
				try {
					tag.createField(FieldKey.ARTIST, a);
					fields_changed++;
				} catch (KeyNotFoundException ex) {
					LOGGER.info("Artist field not found.", ex);
				} catch (FieldDataInvalidException ex) {
					LOGGER.info("Invalid artist field data.", ex);
				}
			});
	}

	/** @param val the composer to set */
	public void setComposer(String val) {
		setGeneralField(FieldKey.COMPOSER, val);
	}

	/** @param val the category to set */
	public void setCategory(String val) {
		setGeneralField(FieldKey.GROUPING, val);
	}

	/** @param val the comment to set */
	public void setComment(String val) {
		setGeneralField(FieldKey.COMMENT, val);
	}

	/** @param cover the cover to set */
	public void setCover(File cover) {
		try {
			if (cover==null)
				tag.deleteArtworkField();
			else {
				tag.deleteArtworkField();
				tag.setField(ArtworkFactory.createArtworkFromFile(cover));
			}
			fields_changed++;
		} catch (KeyNotFoundException ex) {
			LOGGER.info("Category field not found.", ex);
		} catch (FieldDataInvalidException ex) {
			LOGGER.info("Invalid category field data.", ex);
		} catch (IOException ex) {
			LOGGER.info("Problem with the file reading when setting cover to tag.", ex);
		}
	}

	/** @param disc the disc to set */
	public void setDisc(String disc) {
		setGeneralField(FieldKey.DISC_NO, disc);
	}

	/** @param discs_total the discs_total to set */
	public void setDiscsTotal(String discs_total) {
		setGeneralField(FieldKey.DISC_TOTAL, discs_total);
	}

	public void setDiscsInfo(NofX discsInfo) {
		setDisc(String.valueOf(discsInfo.getN()));
		setDiscsTotal(String.valueOf(discsInfo.getOf()));
	}

	/** @param genre the genre to set */
	public void setGenre(String genre) {
		setGeneralField(FieldKey.GENRE, genre);
	}

	/**
	 * Sets rating to specified value. Valid number String will be parsed
	 * otherwise this method is no-op. Rating value will be clipped to range
	 * supported by specific tag. Note that not all tags have the same maximum
	 * rating value. Because of this, it is recommended to avoid this method and
	 * use percentage alternative.
	 *
	 * @param rating rating value to set. Empty or null to remove the field from tag. Negative value will result in
	 * no-op.
	 */
	public void setRating(String rating) {
		if (rating==null || rating.isEmpty())
			setRating(-1);
		else {
			try {
				double val = Double.parseDouble(rating);
				if (val<0)
					throw new IllegalArgumentException("Rating number must not be negative");
				setRating(val);
			} catch (NumberFormatException ex) {
				LOGGER.warn("Rating field value not a number");
			}
		}
	}

	/**
	 * Sets rating to specified value expressed in percentage <0-1>. Valid number
	 * String will be parsed otherwise this method is no-op.
	 *
	 * @param rating rating value to set. Empty or null to remove the field from tag. Param not in <0-1> range will
	 * result in no-op.
	 */
	public void setRatingPercent(String rating) {
		if (rating==null || rating.isEmpty())
			setRating(-1);
		else {
			try {
				double val = Double.parseDouble(rating);
				setRatingPercent(val);
			} catch (NumberFormatException ex) {
				LOGGER.warn("Rating field value not a number");
			}
		}
	}

	/**
	 * Sets rating to specified value expressed in percentage <0-1>. It is recommended
	 * to use this method to avoid value corruption by clipping it.
	 *
	 * @param val rating to set in percentage <0-1>. Param not in <0-1> range will result in no-op.
	 */
	public void setRatingPercent(double val) {
		if (val==-1) setRating(val);
		else if (val>1)
			LOGGER.error("Rating number must be <= 1");
		else if (val<0)
			LOGGER.error("Rating number must be >= 0");
		else setRating(getRatingMax(tag)*val);
	}

	/**
	 * Sets rating to specified value. Rating value will be clipped to range
	 * supported by specific tag. Note that not all tags have the same maximum
	 * rating value. Because of this, it is recommended to avoid this method and
	 * use percentage alternative.
	 *
	 * @param val rating to set. -1 to remove the field from tag. Value will be clipped to 0-max value.
	 */
	private void setRating(double val) {
		double v = val<0 ? -1 : clipRating(tag, val);
		switch (getFormat()) {
			case mp3: setRatingMP3((AbstractID3v2Tag) tag, v); break;
			case wav: setRatingMP3(wavToId3((WavTag) tag), v); break;
			case flac:
			case ogg: setRatingVorbisOgg(v); break;
			case mp4:
			case m4a: setRatingMP4(v); break;
			default:    // rest not supported
		}
	}

	private void setRatingMP3(AbstractID3v2Tag tag, double val) {
		AbstractID3v2Frame f = tag.getFirstField(ID3v24Frames.FRAME_ID_POPULARIMETER);
		if (f==null) {
			f = new ID3v24Frame(ID3v24Frames.FRAME_ID_POPULARIMETER);
			f.setBody(new FrameBodyPOPM()); // TODO: this sets playcount to 0 if it was 0, avoid this
		}
		try {
			if (val==-1) {
				((FrameBodyPOPM) f.getBody()).setRating(0);
				tag.setField(f);
			} else {
				((FrameBodyPOPM) f.getBody()).setRating((long) val);
				tag.setField(f);
			}
			fields_changed++;
		} catch (FieldDataInvalidException ex) {
			LOGGER.info("Ignoring rating field. Data invalid.");
		}
	}

	private void setRatingMP4(double val) {
		try {
			if (val==-1) {
				tag.deleteField(RATING);
			} else {
				int r = clip(0, (int) val, 100);
				tag.setField(RATING, Integer.toString(r));
			}
			fields_changed++;
		} catch (FieldDataInvalidException ex) {
			LOGGER.info("Ignoring rating field. Data invalid.");
		}
	}

	private void setRatingVorbisOgg(double v) {
		String sv = v<0 ? null : Integer.toString((int) v); // lets stay decimal
		setVorbisField("RATING", sv);
	}

	/** @param title the title to set */
	public void setTitle(String title) {
		setGeneralField(FieldKey.TITLE, title);
	}

	/** @param track the track to set */
	public void setTrack(String track) {
		setGeneralField(FieldKey.TRACK, track);
	}

	/** @param tracks_total the tracks_total to set */
	public void setTracksTotal(String tracks_total) {
		setGeneralField(FieldKey.TRACK_TOTAL, tracks_total);
	}

	public void setTracksInfo(NofX tracksInfo) {
		setTrack(String.valueOf(tracksInfo.getN()));
		setTracksTotal(String.valueOf(tracksInfo.getOf()));
	}

	/** @param count the rating to set */
	public void setPlaycount(String count) {
		if (count==null || count.isEmpty())
			setPlaycount(-1);
		else {
			try {
				int val = Integer.parseInt(count);
				if (val<0)
					throw new NumberFormatException("Playcount number must not be negative");
				setPlaycount(val);
			} catch (NumberFormatException ex) {
				LOGGER.info("Playcount field value not a number");
			}
		}
	}

	/** @param val rating to set. -1 to remove the field from tag. */
	public void setPlaycount(int val) {
		// set universally
		setGeneralField(CUSTOM3, val<0 ? "" : String.valueOf(val));
		// set to id3 tag if available
		if (tag instanceof AbstractID3v2Tag) setPlaycountID3((AbstractID3v2Tag) tag, val);
		else if (tag instanceof WavTag) setPlaycountID3(wavToId3((WavTag) tag), val);
	}

	/** Increments playcount by 1. */
	public void inrPlaycount(Metadata m) {
		setPlaycount(m.getPlaycountOr0() + 1);
	}

	private void setPlaycountID3(AbstractID3v2Tag tag, int val) {
		// POPM COUNT
		try {
			// get tag
			AbstractID3v2Frame f = tag.getFirstField(ID3v24Frames.FRAME_ID_POPULARIMETER);
			if (f==null) {
				f = new ID3v24Frame(ID3v24Frames.FRAME_ID_POPULARIMETER);
				f.setBody(new FrameBodyPOPM());
			}
			// set value
			((FrameBodyPOPM) f.getBody()).setCounter(max(0, val));
			tag.setField(f);
			// fields_changed++;
		} catch (FieldDataInvalidException ex) {
			LOGGER.info("Ignoring playcount field. Data invalid.");
		}
		// PLAY COUNT
		try {
			// get tag
			AbstractID3v2Frame f = tag.getFirstField(ID3v24Frames.FRAME_ID_PLAY_COUNTER);
			if (f==null) {
				f = new ID3v24Frame(ID3v24Frames.FRAME_ID_PLAY_COUNTER);
				f.setBody(new FrameBodyPCNT());
			}
			// set value
			((FrameBodyPCNT) f.getBody()).setCounter((max(0, val)));
			tag.setField(f);
			// fields_changed++;
		} catch (FieldDataInvalidException ex) {
			LOGGER.info("Ignoring playcount field. Data invalid.");
		}
	}

	/** @param val the publisher to set */
	public void setPublisher(String val) {
		switch (getFormat()) {
			case flac:
			case ogg: setVorbisField("PUBLISHER", val); break;
			case mp3: setPublisherID3((AbstractID3v2Tag) tag, val); break;
			case wav: setPublisherID3(wavToId3((WavTag) tag), val); break;
			case mp4:
			case m4a: setPublisherMP4(val); break;
			default:    // rest not supported
		}
		// increment fields_changed in implementations
	}

	private void setPublisherID3(AbstractID3v2Tag tag, String val) {
		AbstractID3v2Frame f = tag.getFirstField(ID3v24Frames.FRAME_ID_PUBLISHER);
		if (f==null) {
			f = new ID3v24Frame(ID3v24Frames.FRAME_ID_PUBLISHER);
			f.setBody(new FrameBodyTPUB());
		}
		// set value, prevent writing corrupt data
		try {
			if (val==null || val.isEmpty())
				tag.removeFrameOfType(f.getIdentifier());
			else {
				((FrameBodyTPUB) f.getBody()).setText(val);
				tag.setField(f);
			}
			fields_changed++;
		} catch (FieldDataInvalidException ex) {
			LOGGER.info("Ignoring publisher field. Data invalid.");
		}
	}

	private void setPublisherMP4(String val) {
		try {
			if (val==null || val.isEmpty()) {
				((Mp4Tag) tag).deleteField(Mp4FieldKey.WINAMP_PUBLISHER);
				((Mp4Tag) tag).deleteField(Mp4FieldKey.MM_PUBLISHER);
			} else {
				((Mp4Tag) tag).setField(Mp4FieldKey.WINAMP_PUBLISHER, val);
				((Mp4Tag) tag).setField(Mp4FieldKey.MM_PUBLISHER, val);
			}
			fields_changed++;
		} catch (FieldDataInvalidException ex) {
			LOGGER.info("Ignoring publisher field. Data invalid.");
		}
	}

	/**
	 * Change user/mail within POPM3 field of id3 tag. Supports only files
	 * supporting id3 tag (mp3). For other types (flac, ogg, wav) does nothing.
	 */
	public void setUserMailID3(String val) {
		AudioFileFormat f = getFormat();
		switch (f) {
			case mp3: seUserPopmID3(val); break;
			default:    // rest not supported
		}
	}

	private void seUserPopmID3(String val) {
		try {
			AbstractID3v2Frame f = ((MP3File) audioFile).getID3v2TagAsv24().getFirstField(ID3v24Frames.FRAME_ID_POPULARIMETER);
			// prevent null
			if (f==null) {
				f = new ID3v24Frame(ID3v24Frames.FRAME_ID_POPULARIMETER);
				f.setBody(new FrameBodyPOPM());
			}
			// set value
			if (val==null || val.isEmpty()) {
				((FrameBodyPOPM) f.getBody()).setEmailToUser("");
				((MP3File) audioFile).getID3v2Tag().setField(f);
			} else {
				((FrameBodyPOPM) f.getBody()).setEmailToUser(val);
				((MP3File) audioFile).getID3v2Tag().setField(f);
			}
			fields_changed++;
		} catch (FieldDataInvalidException ex) {
			LOGGER.info("Ignoring playcount field. Data invalid.");
		}
	}

	/** @param val the lyrics to set */
	public void setLyrics(String val) {
		setGeneralField(FieldKey.LYRICS, val);
	}

	/** @param val the mood to set */
	public void setMood(String val) {
		setGeneralField(FieldKey.MOOD, val);
	}

	/** @param c the color to set */
	public void setColor(Color c) {
		setCustomField(TAG_ID_COLOR, APP.converter.general.toS(c));
	}

	/** @param tags tags to set */
	public void setColor(String tags) {
		setCustomField(TAG_ID_TAGS, tags);
	}

	/**
	 * Write chapters to tag. This method rewrites any previous chapter data.
	 * In order to not lose the original data, the chapters first need to be
	 * obtained and the modified list passed as an argument to this method.
	 *
	 * @param chapters chapters that to write to tag
	 * @see #addChapter(Chapter, Metadata)
	 * @see #removeChapter(Chapter, Metadata)
	 */
	public void setChapters(Collection<Chapter> chapters) {
		setCustom2(chapters.stream().map(Chapter::toString).collect(joining("|")));
	}

	/**
	 * Convenience method.
	 * Adds the given chapter to the metadata or rewrites it if it already exists.
	 * For chapter identity consult {@link Chapter#equals(java.lang.Object)}.
	 * <p/>
	 * Note: Dont abuse this method in loops and use {@link #setChapters(java.util.Collection)}.
	 *
	 * @param chapter chapter to ad
	 * @param metadata Source metadata for chapter data. In order to retain rest of the chapters, the metadata for the
	 * item are necessary.
	 */
	public void addChapter(Chapter chapter, Metadata metadata) {
		List<Chapter> chaps = list(metadata.getChapters().getChapters());
		int i = chaps.indexOf(chapter);
		if (i==-1) chaps.add(chapter);
		else chaps.set(i, chapter);
		setChapters(chaps);
	}

	/**
	 * Convenience method.
	 * Removes the given chapter from the metadata if it already exists or does
	 * nothing otherwise.
	 * <p/>
	 * For chapter identity consult {@link Chapter#equals(java.lang.Object)}.
	 * Dont abuse this method in loops and use {@link #setChapters(java.util.Collection)}.
	 *
	 * @param chapter chapter to remove. Object equality will be used to remove the chapter.
	 * @param metadata Source metadata for chapter data. In order to retain rest of the chapters, the metadata for the
	 * item are necessary.
	 */
	public void removeChapter(Chapter chapter, Metadata metadata) {
		List<Chapter> cs = metadata.getChapters().getChapters();
		if (cs.remove(chapter)) setChapters(cs);
	}

	/** @param val the year to set */
	public void setYear(String val) {
		setGeneralField(FieldKey.YEAR, val);
	}

	private static final ZoneId ZONE_ID = ZoneId.systemDefault();

	public void setPlayedFirst(LocalDateTime at) {
		long epochmillis = at.atZone(ZONE_ID).toInstant().toEpochMilli();
		setCustomField(TAG_ID_PLAYED_FIRST, String.valueOf(epochmillis));
	}

	public void setPlayedFirstNow() {
		long epochmillis = System.currentTimeMillis(); // same as LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
		setCustomField(TAG_ID_PLAYED_FIRST, String.valueOf(epochmillis));
	}

	public void setPlayedFirstNowIfEmpty() {
		if (hasCustomField(TAG_ID_PLAYED_FIRST)) return;
		long epochmillis = System.currentTimeMillis();
		setCustomField(TAG_ID_PLAYED_FIRST, String.valueOf(epochmillis));
	}

	public void setPlayedLast(LocalDateTime at) {
		long epochmillis = at.atZone(ZONE_ID).toInstant().toEpochMilli();
		setCustomField(TAG_ID_PLAYED_LAST, String.valueOf(epochmillis));
	}

	public void setPlayedLastNow() {
		long epochmillis = System.currentTimeMillis();
		setCustomField(TAG_ID_PLAYED_LAST, String.valueOf(epochmillis));
	}

	public void setLibraryAdded(LocalDateTime at) {
		long epochmillis = at.atZone(ZONE_ID).toInstant().toEpochMilli();
		setCustomField(TAG_ID_LIB_ADDED, String.valueOf(epochmillis));
	}

	public void setLibraryAddedNow() {
		long epochmillis = System.currentTimeMillis();
		setCustomField(TAG_ID_LIB_ADDED, String.valueOf(epochmillis));
	}

	public void setLibraryAddedNowIfEmpty() {
		if (hasCustomField(TAG_ID_LIB_ADDED)) return;
		long epochmillis = System.currentTimeMillis();
		setCustomField(TAG_ID_LIB_ADDED, String.valueOf(epochmillis));
	}

	public void setTags(Set<String> tags) {
		setTags(tags.isEmpty() ? "" : SEPARATOR_UNIT + tags.stream().collect(joining(String.valueOf(SEPARATOR_UNIT) + SEPARATOR_UNIT)));
	}

	public void setTags(String tags) {
		setCustomField(TAG_ID_TAGS, tags);
	}

	/** @param val custom1 field value to set */
	public void setCustom1(String val) {
		setGeneralField(FieldKey.CUSTOM1, val);
	}

	/**
	 * Do not use. Used for chapters.
	 *
	 * @param val custom1 field value to set
	 */
	public void setCustom2(String val) {
		setGeneralField(FieldKey.CUSTOM2, val);
	}

	/** @param val custom3 field value to set */
	public void setCustom3(String val) {
		setGeneralField(FieldKey.CUSTOM3, val);
	}

	/** @param val custom4 field value to set */
	public void setCustom4(String val) {
		setGeneralField(FieldKey.CUSTOM4, val);
	}

	/** @param val custom5 field value to set */
	public void setCustom5(String val) {
		setGeneralField(FieldKey.CUSTOM5, val);
	}

/* --------------------- GENERAL SETTERS ---------------------------------------------------------------------------- */

	/** Sets field for any format (supported by jaudiotagger) */
	private void setGeneralField(FieldKey field, String val) {
		boolean empty = val==null || val.isEmpty();
		try {
			if (empty) tag.deleteField(field); else tag.setField(field, val);
			fields_changed++;
		} catch (KeyNotFoundException e) {
			LOGGER.info(field + " field not found", e);
		} catch (FieldDataInvalidException e) {
			LOGGER.info("Invalid " + field + " field data", e);
		} catch (UnsupportedOperationException e) {
			LOGGER.info("Unsupported operation", e);
		}
	}

	/**
	 * Sets field for flac/ogg - use for non standard flac/ogg fields.
	 *
	 * @param field arbitrary (vorbis is that cool) value denoting the field
	 * @param val null or "" deletes field, otherwise value to be set
	 */
	private void setVorbisField(String field, String val) {
		boolean empty = val==null || val.isEmpty();
		// get tag
		VorbisCommentTag t = tag instanceof FlacTag
				? ((FlacTag) tag).getVorbisCommentTag()
				: (VorbisCommentTag) tag;
		// set if possible
		try {
			if (empty) t.deleteField(field);
			else t.setField(field, val);
			fields_changed++;
		} catch (KeyNotFoundException|FieldDataInvalidException e) {
			LOGGER.warn("Failed to set vorbis field {} to {} for {}", field, val, file, e);
		}
	}

	/**
	 * Sets field for custom tag recognized only by this application.
	 *
	 * @param id field id
	 * @param val value to set
	 */
	private void setCustomField(String id, String val) {
		boolean isEmpty = val==null || val.isEmpty();	// TODO: unimplemented!!
		String ov = tag.hasField(FieldKey.CUSTOM5) ? emptyOr(tag.getFirst(FieldKey.CUSTOM5)) : "";
		List<String> tagfields = list(split(ov, String.valueOf(SEPARATOR_GROUP)));
		tagfields.removeIf(tagfield -> tagfield.startsWith(id));
		tagfields.add(id + val);
		String nv = tagfields.stream().collect(joining(String.valueOf(SEPARATOR_GROUP)));
		nv = SEPARATOR_GROUP + nv + SEPARATOR_GROUP;
		setCustom5(nv);
	}

	private boolean hasCustomField(String id) {
		String ov = tag.hasField(FieldKey.CUSTOM5) ? emptyOr(tag.getFirst(FieldKey.CUSTOM5)) : "";
		return ov.contains(SEPARATOR_GROUP + id);
	}

	public void setFieldS(Metadata.Field field, String data) {
		if (field==Metadata.Field.PATH ||
			field==Metadata.Field.FILENAME ||
			field==Metadata.Field.FORMAT ||
			field==Metadata.Field.FILESIZE ||
			field==Metadata.Field.ENCODING ||
			field==Metadata.Field.BITRATE ||
			field==Metadata.Field.CHANNELS ||
			field==Metadata.Field.SAMPLE_RATE ||
			field==Metadata.Field.LENGTH) return;
		if (field==Metadata.Field.ENCODER) { setEncoder(data); return; }
		if (field==Metadata.Field.TITLE) { setTitle(data); return; }
		if (field==Metadata.Field.ALBUM) { setAlbum(data); return; }
		if (field==Metadata.Field.ARTIST) { setArtist(data); return; }
		if (field==Metadata.Field.ALBUM_ARTIST) { setAlbum_artist(data); return; }
		if (field==Metadata.Field.COMPOSER) { setComposer(data); return; }
		if (field==Metadata.Field.PUBLISHER) { setPublisher(data); return; }
		if (field==Metadata.Field.TRACK) { setTrack(data); return; }
		if (field==Metadata.Field.TRACKS_TOTAL) { setTracksTotal(data); return; }
		if (field==Metadata.Field.TRACK_INFO) { NofX.fromString(data).ifOk(this::setTracksInfo); return; }
		if (field==Metadata.Field.DISC) { setDisc(data); return; }
		if (field==Metadata.Field.DISCS_TOTAL) { setDiscsTotal(data); return; }
		if (field==Metadata.Field.DISCS_INFO) { NofX.fromString(data).ifOk(this::setDiscsInfo); return; }
		if (field==Metadata.Field.GENRE) { setGenre(data); return; }
		if (field==Metadata.Field.YEAR) { setYear(data); return; }
		if (field==Metadata.Field.COVER) return;
		if (field==Metadata.Field.RATING) { setRatingPercent(data); return; }
		if (field==Metadata.Field.RATING_RAW) { setRating(data); return; }
		if (field==Metadata.Field.PLAYCOUNT) { setPlaycount(data); return; }
		if (field==Metadata.Field.CATEGORY) { setCategory(data); return; }
		if (field==Metadata.Field.COMMENT) { setComment(data); return; }
		if (field==Metadata.Field.LYRICS) { setLyrics(data); return; }
		if (field==Metadata.Field.MOOD) { setMood(data); return; }
		if (field==Metadata.Field.COLOR) { setCustomField(TAG_ID_COLOR, data); return; }
		if (field==Metadata.Field.TAGS) { setCustomField(TAG_ID_TAGS, data); return; }
		if (field==Metadata.Field.CHAPTERS) return;
		if (field==Metadata.Field.CUSTOM1) { setCustom1(data); return; }
		if (field==Metadata.Field.CUSTOM2) { setCustom2(data); return; }
		if (field==Metadata.Field.CUSTOM3) { setCustom3(data); return; }
		if (field==Metadata.Field.CUSTOM4) { setCustom4(data); return; }
		if (field==Metadata.Field.CUSTOM5) { setCustom5(data); return; }
		if (field==Metadata.Field.FIRST_PLAYED) { setCustomField(TAG_ID_PLAYED_FIRST, data); return; }
		if (field==Metadata.Field.LAST_PLAYED) { setCustomField(TAG_ID_PLAYED_LAST, data); return; }
		if (field==Metadata.Field.ADDED_TO_LIBRARY) { setCustomField(TAG_ID_LIB_ADDED, data); return; }
		throw new SwitchException(field);
	}

/* ------------------------------------------------------------------------------------------------------------------ */

	/**
	 * Writes all changes to tag.
	 * <p/>
	 * Must never execute on main thread. This method is blocking due to I/O
	 * and possibly sleeping the thread.
	 *
	 * @return true if data were written to tag or false if tag didnt change, either because there was nothing to change
	 * or writing failed.
	 */
	private boolean write() {
		if (!hasFields()) return false;
		LOGGER.debug("Writing {} tag fields to: {}", fields_changed, file);

		if (hasCorruptedTag) {
			LOGGER.warn("Can not write to tag, because it could not be read: {} ", file);
			return false;
		}

		try {
			audioFile.commit();
		} catch (Exception ex) {
			if (isPlayingSame()) {
				LOGGER.debug("File being played, will attempt to suspend playback");
				Player.suspend(); // asynchronous, we don't know how long it will take
				// so we sleep the thread and try tagging again, twice once quickly, once longer
				for (int i = 1; i<=3; i += 2) {
					int tosleep = i*i*250;
					LOGGER.debug("Attempt {}, sleeping for {}", 1 + i/2, tosleep);
					try {
						Thread.sleep(tosleep);
						audioFile.commit();
						break;
					} catch (CannotWriteException|InterruptedException e) {
						if (i<3) {
							LOGGER.info("Can not write file tag (attempt {}): {} {}", 1 + i/2, audioFile.getFile().getPath());
						} else {
							LOGGER.error("Can not write file tag (attempt {}): {} {}", 1 + i/2, audioFile.getFile().getPath(), e);
							Player.activate();
							return false;
						}
					}
				}
				Player.activate();
			} else {
				LOGGER.error("Can not write file tag: {}", audioFile.getFile().getPath(), ex);
				return false;
			}
		}

		return true;
	}

	/**
	 * Finds out how many fields this writer needs to commit.
	 *
	 * @return number of fields that will be written to
	 */
	public int fields() {
		return fields_changed;
	}

	/**
	 * Returns true if nonempty - if there are fields that need to be commited.
	 * More formally returns fields() != 0.
	 */
	public boolean hasFields() {
		return fields_changed>0;
	}

	/**
	 * Resets all fields to allow reuse of this object. Committing immediately
	 * after invoking this method will have no effect. Committing also calls this
	 * method automatically.
	 */
	public void reset() {
		file = null;
		audioFile = null;
		tag = null;
		fields_changed = 0;
		hasCorruptedTag = false;
		isWriting.set(false);
	}

	public void reset(Item i) {
		if (!i.isFileBased()) {
			reset();
		} else {
			file = i.getFile();
			Optional.ofNullable(file)
				.map(f -> readAudioFile(f).getOr(null))
				.ifPresentOrElse(
					f -> {
						audioFile = f;
						tag = audioFile.getTagOrCreateAndSetDefault(); // this can throw NullPointerException
						hasCorruptedTag = false;
					},
					() -> {
						audioFile = null;
						tag = new ID3v24Tag(); // fake tag to write into
						hasCorruptedTag = true;
						LOGGER.warn("Couldn't initialize MetadataWriter, writing to tag will be ignored");
					}
				);
			fields_changed = 0;
			isWriting.set(false);
		}
	}

	/******************************************************************************/

	// TODO: use Fut
	public static <I extends Item> void use(I item, Consumer<MetadataWriter> setter) {
		use(singletonList(item), setter);
	}

	public static <I extends Item> void use(Collection<I> items, Consumer<MetadataWriter> setter) {
		use(items, setter, null);
	}

	public static <I extends Item> void use(Collection<I> items, Consumer<MetadataWriter> setter, Consumer<List<Metadata>> action) {
		Player.IO_THREAD.execute(() -> {
			MetadataWriter w = new MetadataWriter();
			for (I i : items)
				if (i.isFileBased()) {
					w.reset(i);
					setter.accept(w);
					w.write();
				}
			List<Metadata> ms = stream(items).map(MetadataReader::readMetadata).filter(m -> !m.isEmpty()).collect(toList());
			Player.refreshItemsWith(ms);
			if (action!=null) runFX(() -> action.accept(ms));
		});
	}

	public static <I extends Item> void use(I item, Consumer<MetadataWriter> setter, Consumer<Boolean> action) {
		if (item.isFileBased()) {
			Player.IO_THREAD.execute(() -> {
				MetadataWriter w = new MetadataWriter();
				w.reset(item);
				setter.accept(w);
				boolean b = w.write();

				Metadata m = MetadataReader.readMetadata(item);
				if (!m.isEmpty()) Player.refreshItemWith(m);
				if (action!=null) runFX(() -> action.accept(b));
			});
		}
	}

	public static <I extends Item> void useNoRefresh(I item, Consumer<MetadataWriter> setter) {
		if (item.isFileBased()) {
			MetadataWriter w = new MetadataWriter();
			w.reset(item);
			setter.accept(w);
			w.write();
		}
	}

	public static <I extends Item> void useNoRefresh(Collection<I> items, Consumer<MetadataWriter> setter) {
		MetadataWriter w = new MetadataWriter();
		for (I i : items) {
			if (i.isFileBased()) {
				w.reset(i);
				setter.accept(w);
				w.write();
			}
		}
	}

	/**
	 * Rates item.
	 *
	 * @param item to useToRate.
	 * @param rating <0-1> representing percentage of the rating, 0 being minimum and 1 maximum possible rating for
	 * current item. Value outside range will be ignored.
	 */
	public static void useToRate(Metadata item, double rating) {
		use(item, w -> w.setRatingPercent(rating));
		APP.services.use(Notifier.class, n -> n.showTextNotification("Song rating changed to: " + rating, "Update"));
	}

}
