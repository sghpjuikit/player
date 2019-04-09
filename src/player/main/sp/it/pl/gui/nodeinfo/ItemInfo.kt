package sp.it.pl.gui.nodeinfo

import javafx.fxml.FXML
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.HBox
import sp.it.pl.audio.Player
import sp.it.pl.audio.Song
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.gui.objects.image.Thumbnail
import sp.it.pl.gui.objects.image.cover.Cover.CoverSource.ANY
import sp.it.pl.gui.objects.rating.Rating
import sp.it.pl.layout.widget.feature.SongReader
import sp.it.util.async.runOn
import sp.it.util.identityHashCode
import sp.it.util.ui.fxml.ConventionFxmlLoader
import sp.it.util.ui.setAnchors

/** Basic display for song information. */
class ItemInfo @JvmOverloads constructor(showCover: Boolean = true): HBox(), SongReader {

    @FXML private lateinit var indexL: Label
    @FXML private lateinit var songL: Label
    @FXML private lateinit var artistL: Label
    @FXML private lateinit var ratingL: Label
    @FXML private lateinit var albumL: Label
    @FXML private var coverContainer: AnchorPane? = null
    private val rating = Rating()
    private val thumb: Thumbnail?
    private var dataId = null.identityHashCode()

    init {
        ConventionFxmlLoader(this).loadNoEx<Any>()

        ratingL.graphic = rating
        rating.alignment.value = Pos.CENTER_LEFT

        if (showCover) {
            thumb = Thumbnail()
            thumb.borderVisible = true
            coverContainer!!.children += thumb.pane
            thumb.pane.setAnchors(0.0)
        } else {
            thumb = null
            children -= coverContainer
            coverContainer = null
        }
    }

    override fun read(songs: List<Song>) = read(songs.firstOrNull() ?: Metadata.EMPTY)

    override fun read(m: Song) = setValue(m.toMeta())

    /** Displays metadata information. */
    fun setValue(m: Metadata) {
        dataId = m.identityHashCode()
        thumb?.loadCoverOf(m)
        indexL.text = m.getPlaylistIndexInfo().toString()
        songL.text = m.getTitle() ?: m.getFilename()
        artistL.text = m.getArtist() ?: "<none>"
        rating.rating.value = m.getRatingPercent()
        albumL.text = m.getAlbum() ?: "<none>"
    }

    private fun Thumbnail.loadCoverOf(data: Metadata) {
        val id = data.identityHashCode()
        runOn(Player.IO_THREAD) {
            data.getCover(ANY).image
        } ui {
            if (dataId==id) loadImage(it)
        }
    }

}