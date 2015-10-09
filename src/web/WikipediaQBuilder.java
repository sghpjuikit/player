/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package web;

import java.net.URI;

import AudioPlayer.plugin.IsPlugin;
import util.parsing.StringParseStrategy;

import static util.parsing.StringParseStrategy.From.CONSTRUCTOR;
import static util.parsing.StringParseStrategy.To.CONSTANT;

/**
 *
 * @author Plutonium_
 */
@IsPlugin
@StringParseStrategy( from = CONSTRUCTOR, to = CONSTANT, constant = "Wikipedia" )
public class WikipediaQBuilder implements HttpSearchQueryBuilder {

    @Override
    public URI apply(String q) {
        return URI.create("https://en.wikipedia.org/wiki/" + q.replace(" ", "%20"));
    }

}