package web;

import java.net.URI;
import util.parsing.ParsesFromString;
import util.parsing.StringParseStrategy;
import util.parsing.StringParseStrategy.From;
import util.plugin.IsPlugin;
import static util.Util.urlEncodeUtf8;
import static util.parsing.StringParseStrategy.To.CONSTANT;

/**
 * @author Martin Polakovic
 */
@IsPlugin
@StringParseStrategy(from = From.ANNOTATED_METHOD, to = CONSTANT, constant = "DuckDuckGo")
public class DuckDuckGoImageQBuilder implements SearchUriBuilder {

	@ParsesFromString
	public DuckDuckGoImageQBuilder() {}

	@Override
	public URI apply(String q) {
		return URI.create("https://duckduckgo.com/?q=" + urlEncodeUtf8(q));
	}

}