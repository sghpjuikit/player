package sp.it.pl.ui.objects.search;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Duration;
import sp.it.util.access.V;
import static java.lang.Math.max;
import static javafx.scene.input.KeyCode.BACK_SPACE;
import static javafx.scene.input.KeyCode.DELETE;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.KeyCode.SPACE;
import static javafx.scene.input.KeyCode.TAB;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.input.KeyEvent.KEY_TYPED;
import static sp.it.util.ui.UtilKt.pseudoclass;

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
	public static final PseudoClass PC_SEARCH_MATCH = pseudoclass("searchmatch");
	public static final PseudoClass PC_SEARCH_MATCH_NOT = pseudoclass("searchmatchnot");

	protected long searchTime = -1;
	protected final V<Duration> searchTimeMax = SearchAutoCancelable.Companion.getCancelQueryDelay();

	/**
	 * Text against which the records are being matched against.
	 * Can be empty or consist of only whitespaces.
	 */
	public final StringProperty searchQuery = new SimpleStringProperty("");
	private Integer searchIndex = 0;
	private Integer selectionIndex = 0;
	private final KeyCode keyCancel = KeyCode.ESCAPE;
	private final KeyCode keyNextOccurrence = KeyCode.ENTER;
	private KeyCode pressedKeyCode;


	/**
	 * Installs the behavior of this search on specified node. This involves adding appropriate event handlers for
	 * actions like canceling search or traversing search matches.
	 */
	public void installOn(Node targetNode) {
		targetNode.addEventHandler(KEY_TYPED, this::onKeyTyped);
		targetNode.addEventFilter(KEY_PRESSED, this::onKeyPress);
	}

	/**
	 * Invokes search with specified query.
	 *
	 * @param query string to match records against
	 */
	public final void search(String query) {
		search(query, System.currentTimeMillis());
	}

	/**
	 * Note: must be called on {@link KeyEvent#KEY_TYPED} event as event handler
	 */
	private void onKeyTyped(KeyEvent e) {
		if (pressedKeyCode==null || pressedKeyCode==ESCAPE || pressedKeyCode==TAB || pressedKeyCode==ENTER || pressedKeyCode==DELETE) return;
		if (pressedKeyCode.isNavigationKey() || pressedKeyCode.isFunctionKey() || e.isAltDown() || e.isShortcutDown()) return;
		if (!isActive() && (e.isShiftDown() || pressedKeyCode==SPACE)) return;

		var letter = e.getCharacter();
		if (!letter.isEmpty()) {
			// update scroll text
			var now = System.currentTimeMillis();
			var append = searchTime==-1 || now - searchTime<searchTimeMax.getValue().toMillis();
			var query = pressedKeyCode==BACK_SPACE
				? removeLastChar(searchQuery.get())
				: append
					? searchQuery.get() + letter
					: letter;
			selectionIndex = 0;
			search(query, now);
			e.consume();
		}
	}

	/**
	 * Note: must be called on {@link KeyEvent#KEY_PRESSED} event as event filter.
	 */
	private void onKeyPress(KeyEvent e) {
		pressedKeyCode = e.getCode();

		if (isActive()) {
			if (e.getCode()==keyCancel) {
				cancel();
				e.consume(); // must cause all KEY_PRESSED handlers to be ignored
			}
			if (e.getCode()==keyNextOccurrence) {
				if (e.isShiftDown()) selectionIndex = max(0,selectionIndex-1);
				else selectionIndex++;
				// else min(matchesCount-1, selectionIndex+1)	// TODO: requires complete search iteration, cache matchesCount and enable

				search(searchQuery.get());
				e.consume(); // must cause all KEY_PRESSED handlers to be ignored
			}
		}
	}

	private void search(String query, long now) {
		searchQuery.set(query);
		searchTime = now;
		searchIndex = 0;
		doSearch(query);
	}

	protected abstract void doSearch(String query);

	protected abstract boolean isMatch(String text, String query);

	protected boolean isMatchNth(String text, String query) {
		return isMatch(text, query) && isMatchSelected();
	}

	private boolean isMatchSelected() {
		boolean b = selectionIndex.equals(searchIndex);
		searchIndex++;
		return b;
	}

	/**
	 * Returns whether search is active.
	 * <p/>
	 * Every search must be ended, either automatically {@link sp.it.pl.ui.objects.search.SearchAutoCancelable#isCancelable},
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
		searchIndex = 0;
		selectionIndex = 0;
	}

	private static String removeLastChar(String text) {
		return text.isEmpty() ? text : text.substring(0, text.length() - 1);
	}
}