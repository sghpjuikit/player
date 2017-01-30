package web;

import java.net.URI;

import util.parsing.ParsesFromString;
import util.parsing.StringParseStrategy;
import util.plugin.IsPlugin;

import static util.Util.urlEncodeUtf8;
import static util.parsing.StringParseStrategy.From.ANNOTATED_METHOD;
import static util.parsing.StringParseStrategy.To.CONSTANT;

/**
 * @author Martin Polakovic
 */
@IsPlugin
@StringParseStrategy( from = ANNOTATED_METHOD, to = CONSTANT, constant = "Google" )
public class GoogleImageQBuilder implements SearchUriBuilder {

	@ParsesFromString
	public GoogleImageQBuilder() {}

	@Override
	public URI apply(String q) {
		return URI.create("https://www.google.com/search?hl=en&site=imghp&tbm=isch&source=hp&q=" + urlEncodeUtf8(q));
	}

}