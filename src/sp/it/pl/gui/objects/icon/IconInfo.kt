package sp.it.pl.gui.objects.icon

import de.jensd.fx.glyphs.GlyphIcons
import javafx.geometry.Pos.CENTER
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import sp.it.pl.util.graphics.lay
import sp.it.pl.util.graphics.stackPane
import sp.it.pl.util.system.copyToSysClipboard

/**
 * Displays an icon with its name. Has tooltip displaying additional information.
 * Mouse click copies icon name to system clipboard.
 */
class IconInfo(icon: GlyphIcons?, iconSize: Double): VBox(5.0) {

    private val nameLabel = Label()
    private val graphics: Icon
    private var glyph: GlyphIcons? = icon

    init {
        alignment = CENTER
        graphics = Icon(icon, iconSize).styleclass("icon-info-icon")
        graphics.setOnMouseClicked { copyToSysClipboard(glyph?.name() ?: "") }
        lay += stackPane(graphics)
        lay += nameLabel
    }

    fun setGlyph(icon: GlyphIcons?) {
        this.glyph = icon
        nameLabel.text = icon?.name()?.toLowerCase()?.capitalize() ?: ""
        graphics.icon(icon)
        graphics.tooltip(if (icon==null) "" else "${icon.name()}\n${icon.unicodeToString()}\n${icon.fontFamily}")
    }

    fun select(value: Boolean) {
        graphics.select(value)
    }
}