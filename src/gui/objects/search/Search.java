package gui.objects.search;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.css.PseudoClass;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Duration;

import static javafx.css.PseudoClass.getPseudoClass;
import static javafx.scene.input.KeyCode.BACK_SPACE;
import static javafx.scene.input.KeyCode.SPACE;
import static javafx.util.Duration.millis;
import static util.Util.removeLastChar;

public abstract class Search {
	public static final PseudoClass SEARCHMATCHPC = getPseudoClass("searchmatch");
	public static final PseudoClass SEARCHMATCHNOTPC = getPseudoClass("searchmatchnot");

	protected long searchTime = -1;
	protected Duration searchTimeMax = millis(500);
	/**
	 * Text against which the records are being matched against.
	 * Can be empty or consist of only whitespaces.
	 */ public final StringProperty searchQuery = new SimpleStringProperty("");

	public void search(KeyEvent e) {
		KeyCode k = e.getCode();
		if (e.isAltDown() || e.isControlDown() || e.isShiftDown()) return;
		// typing -> scroll to
		if (k.isDigitKey() || k.isLetterKey() || k==SPACE || k== BACK_SPACE) {
			String letter = e.getText();
			// update scroll text
			long now = System.currentTimeMillis();
			boolean append = searchTime==-1 || now-searchTime<searchTimeMax.toMillis();
			searchQuery.set(k==BACK_SPACE ? removeLastChar(searchQuery.get()) : append ? searchQuery.get()+letter : letter);
			searchTime = now;
			onSearch(searchQuery.get());
			e.consume();
		}
	}

	public abstract void onSearch(String s);

	public abstract boolean matches(String text, String query);

}