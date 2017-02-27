package main;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import gui.objects.icon.Icon;
import gui.objects.textfield.DecoratedTextField;
import gui.objects.textfield.autocomplete.ConfigSearch;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javafx.animation.FadeTransition;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.scene.Cursor;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import static javafx.util.Duration.millis;

public class Search {
	public final Set<Supplier<Stream<ConfigSearch.Entry>>> sources = new HashSet<>();
	public final ConfigSearch.History history = new ConfigSearch.History();

	@SuppressWarnings("Convert2MethodRef")
	public TextField build() {
		DecoratedTextField tf = new DecoratedTextField();
		Region clearButton = new Region();
		clearButton.getStyleClass().addAll("graphic");
		StackPane clearB = new StackPane(clearButton);
		clearB.getStyleClass().addAll("clear-button");
		clearB.setOpacity(0.0);
		clearB.setCursor(Cursor.DEFAULT);
		clearB.setOnMouseReleased(e -> tf.clear());
		clearB.managedProperty().bind(tf.editableProperty());
		clearB.visibleProperty().bind(tf.editableProperty());
		tf.right.set(clearB);
		FadeTransition fade = new FadeTransition(millis(250), clearB);
		tf.textProperty().addListener(new InvalidationListener() {
			@Override
			public void invalidated(Observable arg0) {
				String text = tf.getText();
				boolean isTextEmpty = text==null || text.isEmpty();
				boolean isButtonVisible = fade.getNode().getOpacity()>0;

				if (isTextEmpty && isButtonVisible) {
					setButtonVisible(false);
				} else if (!isTextEmpty && !isButtonVisible) {
					setButtonVisible(true);
				}
			}

			private void setButtonVisible(boolean visible) {
				fade.setFromValue(visible ? 0.0 : 1.0);
				fade.setToValue(visible ? 1.0 : 0.0);
				fade.play();
			}
		});

		tf.left.set(new Icon<>(FontAwesomeIcon.SEARCH));
		tf.left.get().setMouseTransparent(true);

		new ConfigSearch(tf, history, sources.stream().toArray(i -> new Supplier[i]));

		return tf;
	}
}