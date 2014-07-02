
package AudioPlayer.tagging;

import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.playlist.Item;
import GUI.NotifierManager;
import PseudoObjects.TODO;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import javafx.scene.paint.Color;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldDataInvalidException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.KeyNotFoundException;
import org.jaudiotagger.tag.id3.AbstractID3v2Frame;
import org.jaudiotagger.tag.id3.ID3v24Frame;
import org.jaudiotagger.tag.id3.ID3v24Frames;
import org.jaudiotagger.tag.id3.framebody.FrameBodyPOPM;
import org.jaudiotagger.tag.id3.framebody.FrameBodyTPUB;
import org.jaudiotagger.tag.images.ArtworkFactory;
import utilities.Log;
import utilities.Parser.Parser;

/**
 * 
 * Manages writing Metadata objects back into files. Handles all tag related data
 * for items.
 * <p>
 * The writer must be instantiated for use. It is reusable for an item.
 *  
 * @author uranium
 */
@TODO("limit rating bounds value, multiple values, id3 popularimeter mail settings")
public class MetadataWriter extends MetaItem {
    
    private final File file;
    private final AudioFile audioFile;
    int fields_changed = 0;

    // dont provide access here
    private MetadataWriter(File file, AudioFile audioFile) {
        this.file = file;
        this.audioFile = audioFile;
    }
    
    /** 
     * Constructs metadata writer for given item.
     * @return writer or null if error occurs.
     * @throws UnsupportedOperationException if item not file based
     */
    public static MetadataWriter create(Item item) {
        if (!item.isFileBased()) throw new UnsupportedOperationException("Item must be file based");
                
        AudioFile f = readAudioFile(item.getFile());
        if(f==null) return null;
        else return new MetadataWriter(item.getFile(), f);
    }
    
    @Override
    public URI getURI() {
        return file.toURI();
    }
    @Override
    public File getFile() {
        return file;
    }

    /**
     * Changes name of the file of the item.
     * @param filename without extension
     */
    public void changeFilename(String filename) {
//        this.filename = filename;
    }

    /** @param encoder the encoder to set */
    public void setEncoder(String encoder) {
        setField(FieldKey.ENCODER, encoder);
    }

    /** @param album the album to set */
    public void setAlbum(String album) {
        setField(FieldKey.ALBUM, album);
    }

    /**  @param val the artist to set */
    public void setArtist(String val) {
        setField(FieldKey.ARTIST, val);
    }
    
    /**  @param val the album_artist to set */
    public void setAlbum_artist(String val) {
        setField(FieldKey.ALBUM_ARTIST, val);
    }
    
    /**  @param artists the artists to set */
    public void setArtists(List<String> artists) {
        if (artists == null || artists.isEmpty())
            setArtist(null);
        else
            artists.stream().filter(String::isEmpty).forEach(a->{
                try {
                    audioFile.getTag().createField(FieldKey.ARTIST, a);
                    fields_changed++;
                } catch (KeyNotFoundException ex) {
                Log.mess("Artist field not found.");
                } catch (FieldDataInvalidException ex) {
                    Log.mess("Invalid artist field data.");
                }
            });
    }
    
    /** @param val the composer to set */
    public void setComposer(String val) {
        setField(FieldKey.COMPOSER, val);
    }
    
    /**  @param val the category to set  */
    public void setCategory(String val) {
        setField(FieldKey.GROUPING, val);
    }

    /** @param val the comment to set */
    public void setComment(String val) {
        setField(FieldKey.COMMENT, val);
    }
    
    /** @param cover the cover to set */
    public void setCover(File cover) {
        try {
            if (cover == null)
                audioFile.getTag().deleteArtworkField();
            else {
                audioFile.getTag().deleteArtworkField();
                audioFile.getTag().setField(ArtworkFactory.createArtworkFromFile(cover));
            }
            fields_changed++;
        } catch (KeyNotFoundException ex) {
            Log.mess("Category field not found.");
        } catch (FieldDataInvalidException ex) {
            Log.mess("Invalid category field data.");
        } catch (IOException ex) {
            Log.mess("Problem with the file reading when setting cover to tag.");
        }
    }
    
    /** @param disc the disc to set */
    public void setDisc(String disc) {
        setField(FieldKey.DISC_NO, disc);
    }

    /**  @param discs_total the discs_total to set */
    public void setDiscs_total(String discs_total) {
        setField(FieldKey.DISC_TOTAL, discs_total);
    }

    /** @param genre the genre to set */
    public void setGenre(String genre) {
        setField(FieldKey.GENRE, genre);
    }
    
    /**  
     * Sets rating to specified value. Valid number String will be parsed
     * otherwise this method is no-op. Rating value will be clipped to range
     * supported by specific tag. Note that not all tags have the same maximum
     * rating value. Because of this, it is recommended to avoid this method and
     * use percentage alternative.
     * @param rating rating value to set. Empty or null to remove the field from tag.
     * Negative value will result in no-op.
     */
    public void setRating(String rating) {
        if (rating == null || rating.isEmpty())
            setRating(-1);
        else {
            try {
                double val = Double.parseDouble(rating);
                if (val < 0)
                    throw new IllegalArgumentException("Rating number must not be negative");
                setRating(val);
            } catch (NumberFormatException ex) {
                Log.mess("Rating field value not a number");
            }
        }
    }
    
    /**
     * Sets rating to specified value expressed in percentage <0-1>. Valid number
     * String will be parsed otherwise this method is no-op.
     * @param rating rating value to set. Empty or null to remove the field from tag.
     * Param not in <0-1> range will result in no-op.
     */
    public void setRatingPercent(String rating) {
        if (rating == null || rating.isEmpty())
            setRating(-1);
        else {
            try {
                double val = Double.parseDouble(rating);
                setRatingPercent(val);
            } catch (NumberFormatException ex) {
                Log.mess("Rating field value not a number");
            }
        }
    }
    
    /** 
     * Sets rating to specified value expressed in percentage <0-1>. It is recommended
     * to use this method to avoid value corruption by clipping it.
     * @param val rating to set in percentage <0-1>.
     * Param not in <0-1> range will result in no-op.
     */
    public void setRatingPercent(double val){
        if (val == -1) setRating(val);
        else if (val > 1)
            Log.err("Rating number must be <= 1");
        else if (val < 0)
            Log.err("Rating number must be >= 0");
        else setRating(getRatingMax() * val);
    }
    
    /** 
     * Sets rating to specified value. Rating value will be clipped to range
     * supported by specific tag. Note that not all tags have the same maximum
     * rating value. Because of this, it is recommended to avoid this method and
     * use percentage alternative.
     * @param val rating to set. -1 to remove the field from tag. Value will be 
     * clipped to 0-max value. */
    public void setRating(double val) {
        val = val==-1 ? -1 : clipRating(val);
        switch(getFormat()) {
            case mp3:   setRatingMP3(val); break;
            case flac:  Log.mess("Unsupported operation."); break;
            case ogg:   Log.mess("Unsupported operation."); break;
            case wav:   Log.mess("Unsupported operation."); break;
            default: break;
        } 
    }
    
    private void setRatingMP3(double val) {
        AbstractID3v2Frame f = ((MP3File)audioFile).getID3v2TagAsv24().getFirstField(ID3v24Frames.FRAME_ID_POPULARIMETER);
        if ( f == null) { 
             f = new ID3v24Frame(ID3v24Frames.FRAME_ID_POPULARIMETER); 
             f.setBody(new FrameBodyPOPM());
        }
        // set value, prevent writing corrupt data
        try {
            if (val == -1) {
                ((FrameBodyPOPM) f.getBody()).setRating(0);
                ((MP3File)audioFile).getID3v2Tag().setField(f);
            } else {
                ((FrameBodyPOPM) f.getBody()).setRating((long)val);
                ((MP3File)audioFile).getID3v2Tag().setField(f);
            }
            fields_changed++;
        } catch (FieldDataInvalidException ex) {
            Log.mess("Ignoring rating field. Data invalid.");
        }
    }

    /** @param title the title to set  */
    public void setTitle(String title) {
        setField(FieldKey.TITLE, title);
    }
    
    /** @param track the track to set */
    public void setTrack(String track) {
        setField(FieldKey.TRACK, track);
    }

    /** @param tracks_total the tracks_total to set  */
    public void setTracks_total(String tracks_total) {
        setField(FieldKey.TRACK_TOTAL, tracks_total);
    }
    
    /**  @param count the rating to set */
    public void setPlaycount(String count) {
        if (count == null || count.isEmpty())
            setPlaycount(-1);
        else {
            try {
                int val = Integer.parseInt(count);
                if (val < 0) 
                    throw new NumberFormatException("Playcount number must not be negative");
                setPlaycount(val);
            } catch (NumberFormatException ex) {
                Log.mess("Playcount field value not a number");
            }
        }
    }
    
    /** @param val rating to set. -1 to remove the field from tag. */
    public void setPlaycount(int val) {
        switch(getFormat()) {
            case mp3:   setPlaycountMP3(val); break;
            case flac:  Log.mess("Unsupported operation."); break;
            case ogg:   Log.mess("Unsupported operation."); break;
            case wav:   Log.mess("Unsupported operation."); break;
            default: break;
        } 
    }
    
    private void setPlaycountMP3(int val) {
        try {
            AbstractID3v2Frame f = ((MP3File)audioFile).getID3v2TagAsv24().getFirstField(ID3v24Frames.FRAME_ID_POPULARIMETER);
            // prevent null
            if ( f == null) { 
                 f = new ID3v24Frame(ID3v24Frames.FRAME_ID_POPULARIMETER); 
                 f.setBody(new FrameBodyPOPM()); }
            // set value
            if (val == -1) {
                ((FrameBodyPOPM) f.getBody()).setCounter(0);
                ((MP3File)audioFile).getID3v2Tag().setField(f);
            } else {
                ((FrameBodyPOPM) f.getBody()).setCounter(val);
                ((MP3File)audioFile).getID3v2Tag().setField(f);
            }
            fields_changed++;
        } catch (FieldDataInvalidException ex) {
            Log.mess("Ignoring playcount field. Data invalid.");
        }
    }
 

    /** @param val the publisher to set  */
    public void setPublisher(String val) {
        switch(getFormat()) {
            case mp3:   setPublisherMP3(val); break;
            case flac:  Log.mess("Unsupported operation."); break;
            case ogg:   Log.mess("Unsupported operation."); break;
            case wav:   Log.mess("Unsupported operation."); break;
            default: break;
        }        
    }
    
    private void setPublisherMP3(String val) {
        AbstractID3v2Frame f = ((MP3File)audioFile).getID3v2TagAsv24().getFirstField(ID3v24Frames.FRAME_ID_PUBLISHER);
        if ( f == null) { 
             f = new ID3v24Frame(ID3v24Frames.FRAME_ID_PUBLISHER); 
             f.setBody(new FrameBodyTPUB());
        }
        // set value, prevent writing corrupt data
        try {
            if (val == null || val.isEmpty())
                ((MP3File)audioFile).getID3v2Tag().removeFrameOfType(f.getIdentifier());
            else {
                ((FrameBodyTPUB) f.getBody()).setText(val);
                ((MP3File)audioFile).getID3v2Tag().setField(f);
            }
            fields_changed++;
        } catch (FieldDataInvalidException ex) {
            Log.mess("Ignoring publisher field. Data invalid.");
        }
    }
    
    /**
     * Change user/mail within POPM3 field of id3 tag. Supports only files
     * supporting id3 tag (mp3). For other types (flac, ogg, wav) does nothing.
     */
    public void setUserMailID3(String val) {
        switch(getFormat()) {
            case mp3:   seUserPopmID3(val); break;
            case flac:  Log.mess("Unsupported operation."); break;
            case ogg:   Log.mess("Unsupported operation."); break;
            case wav:   Log.mess("Unsupported operation."); break;
            default: break;
        }
    }
    private void seUserPopmID3(String val) {
        try {
            AbstractID3v2Frame f = ((MP3File)audioFile).getID3v2TagAsv24().getFirstField(ID3v24Frames.FRAME_ID_POPULARIMETER);
            // prevent null
            if ( f == null) { 
                 f = new ID3v24Frame(ID3v24Frames.FRAME_ID_POPULARIMETER); 
                 f.setBody(new FrameBodyPOPM()); }
            // set value
            if (val == null || val.isEmpty()) {
                ((FrameBodyPOPM) f.getBody()).setEmailToUser("");
                ((MP3File)audioFile).getID3v2Tag().setField(f);
            } else {
                ((FrameBodyPOPM) f.getBody()).setEmailToUser(val);
                ((MP3File)audioFile).getID3v2Tag().setField(f);
            }
            fields_changed++;
        } catch (FieldDataInvalidException ex) {
            Log.mess("Ignoring playcount field. Data invalid.");
        }
    }

    /** @param val the lyrics to set */
    public void setLyrics(String val) {
        setField(FieldKey.LYRICS, val);
    }

    /** @param val the mood to set */
    public void setMood(String val) {
        setField(FieldKey.MOOD, val);
        MoodManager.moods.add(val);
    }
    
    /** @param c the color to set */
    public void setColor(Color c) {
        setField(FieldKey.CUSTOM1, Parser.toS(c));
    }
    
    public void setChapters(List<Chapter> chapters) {
        setCustom2(chapters.stream().map(Chapter::toString).collect(Collectors.joining("\\|")));
    }
    
    /** @param val the year to set  */
    public void setYear(String val) {
        setField(FieldKey.YEAR, val);
    }
    
    /** @param val custom1 field value to set  */
    public void setCustom1(String val) {
        setField(FieldKey.CUSTOM1, val);
    }
    
    /** @param val custom1 field value to set  */
    public void setCustom2(String val) {
        setField(FieldKey.CUSTOM2, val);
    }
    
    /** @param val custom3 field value to set  */
    public void setCustom3(String val) {
        setField(FieldKey.CUSTOM3, val);
    }
    
    /** @param val custom4 field value to set  */
    public void setCustom4(String val) {
        setField(FieldKey.CUSTOM4, val);
    }
    
    /** @param val custom5 field value to set  */
    public void setCustom5(String val) {
        setField(FieldKey.CUSTOM5, val);
    }
    
    /** sets field */
    private void setField(FieldKey field, String val) {
        try {
            audioFile.getTagOrCreateDefault();
            if (val == null || val.isEmpty())
                audioFile.getTag().deleteField(field);
            else
                audioFile.getTag().setField(field, val);
            fields_changed++;
        } catch (KeyNotFoundException ex) {
            Log.mess(field.toString() + " field not found.");
        } catch (FieldDataInvalidException ex) {
            Log.mess("Invalid " + field.toString() + " field data.");
        } catch (UnsupportedOperationException ex) {
            Log.mess("Unsupported operation.");
        }
    }
    
/*******************************************************************************/
    
    /** 
     * Writes all changes to tag.
     * @return true if data were written to tag, false if nothing to write or
     * if writing ends unsuccessfully.
     */
    public boolean write() {
        // do nothing if nothing to write
        if (!hasFields()) {
            Log.mess("No changes.");
            return false;
        }
        
        // write changes to tag
        String f = (fields_changed == 1) ? "field" : "fields";
        Log.mess(fields_changed + " " + f + " changed.");
        Log.mess("Writing data to tag for: " + file.toString() + ".");
        try {
            
            PLAYBACK.suspend();
                        
            audioFile.commit();         // save tag
            
            PLAYBACK.loadLastState();
            
            
            Player.refreshItem(this);   // update this item for application
            Log.mess("Done.");
            return true;
        } catch (CannotWriteException ex) {
            Log.err("Can not write to tag for file: " + audioFile.getFile().getPath());
            return false;
        } finally {
            reset();
        }
    }
    
    /**
     * Finds out how many fields this writer needs to commit.
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
        return fields() != 0;
    }
    
    /**
     * Resets all fields to allow reuse of this object. Committing immediately
     * after invoking this method will have no effect. Committing also calls this
     * method automatically.
     */
    public void reset() {
        fields_changed = 0;
    }
    
/*******************************************************************************/
    
    /**
     * Increments playcount of item by exactly one for specified item.
     * @param item to increment playcount of.
     */
    public static void incrementPlaycount(Metadata item) {
        int count = item.getPlaycount() + 1;
        MetadataWriter writer = MetadataWriter.create(item);
                       writer.setPlaycount(String.valueOf(count));
        if (writer.write())
            NotifierManager.showTextNotification("Song playcount incremented to: "+count, "Update");
    }
    
    /**
     * Rates playing item specified by percentage rating.
     * @param item to rate.
     * @param rating <0-1> representing percentage of the rating, 0 being minimum
     * and 1 maximum possible rating for current item. Value outside range will
     * be ignored.
     */
    public static void rate(Metadata item, double rating) {
        MetadataWriter writer = MetadataWriter.create(item);
                       writer.setRatingPercent(rating);
        if (writer.write())
            NotifierManager.showTextNotification("Song rating changed to: " + rating, "Update");
    }

}
