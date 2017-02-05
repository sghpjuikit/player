package gui.objects.search;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.css.PseudoClass;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Duration;

import static javafx.css.PseudoClass.getPseudoClass;
import static javafx.scene.input.KeyCode.*;
import static javafx.util.Duration.millis;
import static util.Util.removeLastChar;

/**
 * <p/>
 * Recommended use:<br/>
 * <pre>{@code
 *  addEventHandler(KeyEvent.KEY_TYPED, search::onKeyTyped);
 *  addEventHandler(KeyEvent.KEY_PRESSED, search::onKeyPressed);
 *  addEventFilter(KeyEvent.KEY_PRESSED, search::onEscPressHide);
 * }</pre>
 */
public abstract class Search {
	public static final PseudoClass SEARCHMATCHPC = getPseudoClass("searchmatch");
	public static final PseudoClass SEARCHMATCHNOTPC = getPseudoClass("searchmatchnot");

	protected long searchTime = -1;
	protected Duration searchTimeMax = millis(500);
	/**
	 * Text against which the records are being matched against.
	 * Can be empty or consist of only whitespaces.
	 */ public final StringProperty searchQuery = new SimpleStringProperty("");
	protected KeyCode pressedKeyCode;

	/**
	 * Note: must be called on {@link KeyEvent#KEY_PRESSED} event.
	 *
	 * @apiNote Use as event filter in form of method reference.
	 *
	 * @param e event to handle
	 */
	public void onKeyPressed(KeyEvent e) {
		pressedKeyCode = e.getCode();
	}

	/**
	 * Note: must be called on {@link KeyEvent#KEY_TYPED} event.
	 *
	 * @apiNote Use as event handler in form of method reference.
	 *
	 * @param e event to handle
	 */
	public void onKeyTyped(KeyEvent e) {
		if (pressedKeyCode==null || pressedKeyCode==ESCAPE || pressedKeyCode==TAB || pressedKeyCode==ENTER) return;
		if (pressedKeyCode.isNavigationKey() || pressedKeyCode.isFunctionKey() || e.isAltDown() || e.isShortcutDown()) return;
		if ((!isActive() && (e.isShiftDown() || pressedKeyCode==SPACE))) return;

		KeyCode k = pressedKeyCode;
		String letter = e.getCharacter();
		if (!letter.isEmpty()) {
			// update scroll text
			long now = System.currentTimeMillis();
			boolean append = searchTime == -1 || now - searchTime < searchTimeMax.toMillis();
			searchQuery.set(k == BACK_SPACE ? removeLastChar(searchQuery.get()) : append ? searchQuery.get() + letter : letter);
			searchTime = now;
			onSearch(searchQuery.get());
			e.consume();
		}
	}

	/**
	 * Cancels search and consumes even iff event's key code is {@link KeyCode#ESCAPE}
	 * Note: must be called on {@link KeyEvent#KEY_PRESSED} event.
	 *
	 * @apiNote Use as event filter in form of method reference.
	 *
	 * @param e event to handle
	 */
	public void onEscPressHide(KeyEvent e) {
		if (e.getCode()==ESCAPE && isActive()) {
			cancel();
			e.consume(); // must cause all KEY_PRESSED handlers to be ignored
		}
	}

	public abstract void onSearch(String s);

	public abstract boolean matches(String text, String query);

	/**
	 * Returns whether search is active.
	 * <p/>
	 * Every search must be ended, either automatically {@link gui.objects.search.SearchAutoCancelable#isCancelable},
	 * or manually {@link #cancel()}.
	 */
	public boolean isActive() {
		return !searchQuery.get().isEmpty();
	}

	/**
	 * Ends search.
	 */
	public void cancel() {
		searchQuery.set("");
	}
}