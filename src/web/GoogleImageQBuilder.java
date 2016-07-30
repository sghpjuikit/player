package web;

import java.net.URI;

import util.parsing.ParsesFromString;
import util.parsing.StringParseStrategy;
import util.plugin.IsPlugin;

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
    public URI apply(String term) {
        String s = "https://www.google.com/search?hl=en&site=imghp&tbm=isch&source=hp&q=" + term.replace(" ", "%20");
        return URI.create(s);
    }

}