package web;

import java.net.URI;

import util.parsing.ParsesFromString;
import util.parsing.StringParseStrategy;
import util.parsing.StringParseStrategy.From;
import util.plugin.IsPlugin;

import static util.parsing.StringParseStrategy.To.CONSTANT;

/**
 * @author Martin Polakovic
 */
@IsPlugin
@StringParseStrategy( from = From.ANNOTATED_METHOD, to = CONSTANT, constant = "DuckDuckGo" )
public class DuckDuckGoImageQBuilder implements SearchUriBuilder {

    @ParsesFromString
    public DuckDuckGoImageQBuilder() {}

    @Override
    public URI apply(String term) {
        String s =  "https://duckduckgo.com/?q=" + term.replace(" ", "%20") + "&iax=1&ia=images";
        return URI.create(s);
    }

}