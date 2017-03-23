package gui.infonode;

import audio.Item;
import audio.tagging.Metadata;
import gui.objects.image.Thumbnail;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import layout.widget.feature.SongReader;
import util.graphics.fxml.ConventionFxmlLoader;
import static gui.objects.image.cover.Cover.CoverSource.ANY;
import static util.dev.Util.noØ;
import static util.graphics.Util.setAnchors;

/**
 * Info about song.
 */
public class ItemInfo extends AnchorPane implements SongReader {

	@FXML private Label typeL, indexL, songL, artistL, albumL;
	@FXML private AnchorPane infoContainer;
	@FXML private AnchorPane coverContainer;
	private final Thumbnail thumb;

	public ItemInfo() {
		this(true);
	}

	public ItemInfo(boolean include_cover) {
		new ConventionFxmlLoader(ItemInfo.class, this).loadNoEx();

		if (include_cover) {
			// create
			thumb = new Thumbnail();
			thumb.setBorderVisible(true);
			coverContainer.getChildren().add(thumb.getPane());
			setAnchors(thumb.getPane(), 0d);
		} else {
			thumb = null;
			// remove cover
			getChildren().remove(coverContainer);
			AnchorPane.setLeftAnchor(infoContainer, AnchorPane.getRightAnchor(infoContainer));
			coverContainer = null;
		}

	}

	@Override
	public void read(List<? extends Item> items) {
		read(items.isEmpty() ? null : items.get(0));
	}

	@Override
	public void read(Item m) {
		setValue("", m.toMeta());
	}

	/**
	 * Displays metadata information and title.
	 *
	 * @param title nullable title
	 * @param m non-null metadata
	 */
	public void setValue(String title, Metadata m) {
		noØ(m);
		typeL.setText(title);
		if (thumb!=null) thumb.loadImage(m.getCover(ANY));
		indexL.setText(m.getPlaylistIndexInfo());
		songL.setText(m.getTitle().isEmpty() ? (m.isFileBased() ? m.getFilename() : m.getPath()) : m.getTitle());
		artistL.setText(m.getArtist());
		albumL.setText(m.getAlbum());
	}

}