package sp.it.pl.audio.tagging

import sp.it.pl.util.file.Util.getSuffix
import java.net.URI

enum class AudioFileFormat {
    MP3,
    OGG,
    FLAC,
    WAV,
    M4A,
    MP4,
    SPX,
    SND,
    AIFC,
    AIF,
    AU,
    MP1,
    MP2,
    AAC,
    UNKNOWN;

    companion object {

        private val formats = values().associateBy { it.name.toLowerCase() }

        fun of(uri: URI): AudioFileFormat = of(getSuffix(uri.path))

        fun of(uri: String): AudioFileFormat = formats[getSuffix(uri).toLowerCase()] ?: UNKNOWN

    }

}