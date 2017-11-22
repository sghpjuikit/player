package sp.it.pl.gui.infonode

import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.layout.AnchorPane
import sp.it.pl.audio.Item
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.gui.objects.image.Thumbnail
import sp.it.pl.gui.objects.image.cover.Cover.CoverSource.ANY
import sp.it.pl.layout.widget.feature.SongReader
import sp.it.pl.util.async.FX
import sp.it.pl.util.async.future.Fut
import sp.it.pl.util.graphics.fxml.ConventionFxmlLoader
import sp.it.pl.util.graphics.leftAnchor
import sp.it.pl.util.graphics.rightAnchor
import sp.it.pl.util.graphics.setAnchors
import sp.it.pl.util.identityHashCode
import java.util.function.Consumer

/** Basic display for song information. */
class ItemInfo @JvmOverloads constructor(showCover: Boolean = true): AnchorPane(), SongReader {

    @FXML private lateinit var typeL: Label
    @FXML private lateinit var indexL: Label
    @FXML private lateinit var songL: Label
    @FXML private lateinit var artistL: Label
    @FXML private lateinit var albumL: Label
    @FXML private lateinit var infoContainer: AnchorPane
    @FXML private var coverContainer: AnchorPane? = null
    private val thumb: Thumbnail?
    private var dataId = null.identityHashCode()

    init {
        ConventionFxmlLoader(ItemInfo::class.java, this).loadNoEx<Any>()

        if (showCover) {
            thumb = Thumbnail()
            thumb.borderVisible = true
            thumb.pane.setAnchors(0.0)
            coverContainer!!.children += thumb.pane
        } else {
            thumb = null
            children -= coverContainer
            infoContainer.leftAnchor = infoContainer.rightAnchor
            coverContainer = null
        }
    }

    override fun read(items: List<Item>) = read(items.firstOrNull() ?: Metadata.EMPTY)

    override fun read(m: Item) = setValue("", m.toMeta())

    /** Displays metadata information and title. */
    fun setValue(title: String, m: Metadata) {
        dataId = m.identityHashCode()
        typeL.text = title
        thumb?.loadCoverOf(m)
        indexL.text = m.getPlaylistIndexInfo().toString()
        songL.text = m.getTitle() ?: m.getFilename()
        artistL.text = m.getArtist()
        albumL.text = m.getAlbum()
    }

    private fun Thumbnail.loadCoverOf(data: Metadata) {
        val id = data.identityHashCode()
        Fut.futWith { data.getCover(ANY).image }
           .use(FX, Consumer { if (dataId==id) loadImage(it) })
    }

}