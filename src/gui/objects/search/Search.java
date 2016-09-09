package gui.objects.search;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Duration;

import static javafx.util.Duration.millis;

public  abstract class Search {
	protected long searchTime = -1;
	protected Duration searchTimeMax = millis(500);
	protected final StringProperty searchQuery = new SimpleStringProperty("");

	public void search(KeyEvent e) {
		KeyCode k = e.getCode();
		if (e.isAltDown() || e.isControlDown() || e.isShiftDown()) return;
		// typing -> scroll to
		if (k.isDigitKey() || k.isLetterKey()) {
			String st = e.getText().toLowerCase();
			// update scroll text
			long now = System.currentTimeMillis();
			boolean append = searchTime==-1 || now-searchTime<searchTimeMax.toMillis();
			searchQuery.set(append ? searchQuery.get()+st : st);
			searchTime = now;
			onSearch(searchQuery.get());
		}
	}

	public abstract void onSearch(String s);

	public abstract boolean matches(String text, String query);

}