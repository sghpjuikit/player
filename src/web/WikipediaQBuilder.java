package web;

import java.net.URI;
import util.parsing.ParsesFromString;
import util.parsing.StringParseStrategy;
import util.parsing.StringParseStrategy.From;
import util.plugin.IsPlugin;
import static util.Util.urlEncodeUtf8;
import static util.parsing.StringParseStrategy.To.CONSTANT;

@IsPlugin
@StringParseStrategy(from = From.ANNOTATED_METHOD, to = CONSTANT, constant = "Wikipedia")
public class WikipediaQBuilder implements SearchUriBuilder {

	@ParsesFromString
	public WikipediaQBuilder() {}

	@Override
	public URI apply(String q) {
		return URI.create("https://en.wikipedia.org/wiki/" + urlEncodeUtf8(q));
	}

}