/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package web;

import AudioPlayer.plugin.IsPlugin;
import java.net.URI;
import util.parsing.StringParseStrategy;
import static util.parsing.StringParseStrategy.From.CONSTRUCTOR;
import static util.parsing.StringParseStrategy.To.CONSTANT;

/**
 <p>
 @author Plutonium_
 */
@IsPlugin
@StringParseStrategy( from = CONSTRUCTOR, to = CONSTANT, constant = "Bing" )
public class BingImageSearchQBuilder implements HttpSearchQueryBuilder {

    @Override
    public URI apply(String term) {
        String s = "http://www.bing.com/images/search?q=" + term.replace(" ", "%20") + "&qs=n&form=QBIR&pq=ggg&sc=8-3&sp=-1";
        return URI.create(s);
    }
    
}           