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
@StringParseStrategy( from = From.ANNOTATED_METHOD, to = CONSTANT, constant = "Bing" )
public class BingImageSearchQBuilder implements SearchUriBuilder {

    @ParsesFromString
    public BingImageSearchQBuilder() {}

    @Override
    public URI apply(String term) {
        String s = "http://www.bing.com/images/search?q=" + term.replace(" ", "%20") + "&qs=n&form=QBIR&pq=ggg&sc=8-3&sp=-1";
        return URI.create(s);
    }

}