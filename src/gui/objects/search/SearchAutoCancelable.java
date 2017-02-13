package gui.objects.search;

import java.util.function.BiPredicate;
import javafx.util.Duration;
import util.Util;
import util.access.V;
import util.async.executor.FxTimer;
import util.conf.IsConfig;
import util.conf.IsConfigurable;
import static gui.objects.search.SearchAutoCancelable.Match.CONTAINS;
import static javafx.util.Duration.millis;
import static util.type.Util.mapEnumConstantName;

/**
 * Search that auto-cancels after given period of time.
 */
@IsConfigurable("Search")
public abstract class SearchAutoCancelable extends Search {

	@IsConfig(name = "Search delay", info = "Maximal time delay between key strokes. Search text is reset after the delay runs out.")
	public static Duration cancelQueryDelay = millis(500);
	@IsConfig(name = "Search auto-cancel", info = "Deactivates search after period of inactivity.")
	public static boolean isCancelable = true;
	@IsConfig(name = "Search auto-cancel delay", info = "Period of inactivity after which search is automatically deactivated.")
	public static Duration cancelActivityDelay = millis(3000);
	@IsConfig(name = "Search algorithm", info = "Algorithm for string matching.")
	public static final V<Match> matcher = new V<>(CONTAINS);
	@IsConfig(name = "Search ignore case", info = "Algorithm for string matching will ignore case.")
	public static boolean isIgnoreCase = true;

	protected long searchTime = -1;
	protected final FxTimer searchAutoCanceller = new FxTimer(3000, 1, this::cancel);

	@Override
	public boolean matches(String text, String query) {
		String t = isIgnoreCase ? text.toLowerCase() : text;
		String q = isIgnoreCase ? query.toLowerCase() : query;
		return matcher.getValue().predicate.test(t, q);
	}

	public enum Match {
		CONTAINS(String::contains),
		STARTS_WITH(String::startsWith),
		ENDS_WITH(String::endsWith);

		final BiPredicate<String,String> predicate;

		Match(BiPredicate<String,String> p) {
			mapEnumConstantName(this, Util::enumToHuman);
			predicate = p;
		}
	}
}