/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.infonode;

import java.util.List;
import java.util.Objects;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;

import audio.Item;
import audio.tagging.Metadata;
import layout.widget.feature.SongReader;
import gui.objects.image.Thumbnail;
import util.graphics.fxml.ConventionFxmlLoader;

import static gui.objects.image.cover.Cover.CoverSource.ANY;
import static util.graphics.Util.setAnchors;

/** Info about song. */
public class ItemInfo extends AnchorPane implements SongReader {

    @FXML private Label typeL;
    @FXML private Label indexL;
    @FXML private Label songL;
    @FXML private Label artistL;
    @FXML private Label albumL;
    @FXML private AnchorPane infoContainer;
    @FXML private AnchorPane coverContainer;
    private final Thumbnail thumb;

    public ItemInfo() {
	this(true);
    }

    public ItemInfo(boolean include_cover) {
        // load fxml part
        new ConventionFxmlLoader(ItemInfo.class,this).loadNoEx();

        if (include_cover) {
            // create
            thumb = new Thumbnail();
            coverContainer.getChildren().add(thumb.getPane());
            setAnchors(thumb.getPane(),0d);
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
     Displays metadata information and title.
     <p/>
     @param title
     @param m
     */
    public void setValue(String title, Metadata m) {
	Objects.requireNonNull(m);
	typeL.setText(title);
	if (thumb != null) thumb.loadImage(m.getCover(ANY));
	indexL.setText(m.getPlaylistIndexInfo());
	songL.setText(m.getTitle().isEmpty() ? (m.isFileBased() ? m.getFilename() : m.getPath()) : m.getTitle());
	artistL.setText(m.getArtist());
	albumL.setText(m.getAlbum());
    }

}
