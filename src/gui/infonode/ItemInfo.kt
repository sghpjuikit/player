package gui.infonode

import audio.Item
import audio.tagging.Metadata
import gui.objects.image.Thumbnail
import gui.objects.image.cover.Cover.CoverSource.ANY
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.layout.AnchorPane
import layout.widget.feature.SongReader
import util.async.Async.FX
import util.async.future.Fut
import util.graphics.fxml.ConventionFxmlLoader
import util.graphics.leftAnchor
import util.graphics.rightAnchor
import util.graphics.setAnchors
import util.identityHashCode
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
        indexL.text = m.playlistIndexInfo
        songL.text = if (m.title.isEmpty()) m.getFilename() else m.title
        artistL.text = m.artist
        albumL.text = m.album
    }

    private fun Thumbnail.loadCoverOf(data: Metadata) {
        val id = System.identityHashCode(data)
        Fut.futWith { data.getCover(ANY).image }
           .use(FX, Consumer { if (dataId==id) loadImage(it) })
    }

}