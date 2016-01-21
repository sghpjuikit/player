/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package web;

import java.net.URI;

import AudioPlayer.plugin.IsPlugin;
import util.parsing.ParsesFromString;
import util.parsing.StringParseStrategy;
import util.parsing.StringParseStrategy.From;

import static util.parsing.StringParseStrategy.To.CONSTANT;

/**
 *
 * @author Plutonium_
 */
@IsPlugin
@StringParseStrategy( from = From.ANNOTATED_METHOD, to = CONSTANT, constant = "Wikipedia" )
public class WikipediaQBuilder implements HttpSearchQueryBuilder {

    @ParsesFromString
    public WikipediaQBuilder() {}

    @Override
    public URI apply(String q) {
        return URI.create("https://en.wikipedia.org/wiki/" + q.replace(" ", "%20"));
    }

}