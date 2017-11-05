package audio

import audio.playlist.PlaylistItem
import audio.tagging.Metadata
import util.file.AudioFileFormat
import util.file.AudioFileFormat.Use
import util.file.AudioFileFormat.Use.PLAYBACK
import util.units.FileSize
import java.io.File
import java.net.URI

/** Representation of audio resource based on [URI]. */
abstract class Item {

    /** @return URI of the audio resource this item represents */
    abstract val uri: URI

    /** @return internal id of this item */
    open val id get() = uri.toString()

    /** @return true iff this item represents a file on a local system, false indicates external resource (e.g. http) */
    fun isFileBased(): Boolean = "file"==uri.scheme

    /**
     * @return absolute file this item represents
     * @throws RuntimeException if this item is not file based
     */
    open fun getFile() = File(uri).absoluteFile!!

    /** @return human-readable path of the resource this item represents */
    open fun getPathAsString(): String {
        val path = uri.path
        return if (path.isNullOrBlank()) "" else uri.path.substring(1)
    }

    /**
     * Parent directory of the resource. Only for file based items.
     *
     * @return parent directory of the item in the file system
     * @throws RuntimeException if this item is not file based
     */
    fun getLocation() = getFile().parentFile!!

    /** @return human-readable location of the resource this item represents */
    fun getLocationAsString() = if (isFileBased()) getLocation().path else getPathAsString()

    /** @return the filename without suffix or empty string if none */
    open fun getFilename() = getFilenameFull().substringBeforeLast(".")

    /** @return filename with suffix or empty string if none */
    fun getFilenameFull(): String {
        val p = uri.path
        if (p==null || p.isEmpty()) return ""
        val i = p.lastIndexOf('/')
        return if (i==-1 || p.length<2) p else p.substring(i+1)
    }

    /** @return the suffix of the resource of this item or empty string if none, e.g.: mp3 */
    fun getSuffix() = uri.path.substringAfterLast('.', "")

    /**
     * @return file format of this item as recognized by the application. It can differ from simple suffix string. This
     * is recommended way to obtain type of file as it utilizes application's built-in mechanism.
     */
    open fun getFormat(): AudioFileFormat = AudioFileFormat.of(uri)

    /**
     * Returns filesize of the file resource of this item. The filesize will
     * remain unknown if unable to determine.
     *
     * @return the filesize of this item. Never null.
     */
    open fun getFileSize(): FileSize = if (isFileBased()) FileSize(getFile()) else FileSize(0)

    /**
     * Returns initial name. Name derived purely from URI of the item.
     *
     * Name can denote an item such as PlaylistItem.
     *
     * Use as an initialization value when only URI is known about the item and
     * more user-friendly information is desired than the raw uri.
     *
     * Default implementation is equivalent to [.getFilename]
     *
     * @return initial name of the item.
     */
    fun getInitialName(): String = getFilename()


    /** Equivalent to `isCorrupt(PLAYBACK)` */
    fun isNotPlayable(): Boolean = isCorrupt(PLAYBACK)

    /** @return true iff this item's underlying resource (e.g. file) is being played */
    fun isPlayingSame(): Boolean = same(Player.playingItem.get())

    /**
     * Checks whether the item can be played. Only non corrupted items can be played.
     *
     * Item is labeled corrupt iff it fulfills at any of the conditions for file based items:
     * * file does not exist
     * * file is not a file (is a directory)
     * * is not supported audio file
     * * file can not be read
     *
     * Also see [.isCorruptWeak].
     *
     * @return playability/validity of the item
     */
    open fun isCorrupt(use: Use): Boolean = !getFormat().isSupported(use) || isCorruptWeak()

    // TODO: improve
    protected fun isCorruptWeak(): Boolean =
            if (isFileBased()) {
                getFile().let { !it.isFile || !it.exists() || !it.canRead() }
            } else {
                false
            }

    /** @return true iff the URIs of the items are equal */
    fun same(i: Item?): Boolean = i!=null && i.uri==uri

    /** @return true iff the provided uri equals to that of this item */
    fun same(r: URI?): Boolean = r!=null && r==uri

    /** @return metadata representation of this item (no io) */
    open fun toMeta() = Metadata(this)

    /** @return playlist item representation of this item */
    open fun toPlaylist() = PlaylistItem(uri)

    /** @return simple item representation this item */
    open fun toSimple() = SimpleItem(getFile())

}