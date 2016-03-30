/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
@StringParseStrategy( from = From.ANNOTATED_METHOD, to = CONSTANT, constant = "Google" )
public class GoogleImageQBuilder implements SearchUriBuilder {

    @ParsesFromString
    public GoogleImageQBuilder() {}

    @Override
    public URI apply(String term) {
        String s = "https://www.google.com/search?hl=en&site=imghp&tbm=isch&source=hp&q=" + term.replace(" ", "%20");
        return URI.create(s);
    }

}