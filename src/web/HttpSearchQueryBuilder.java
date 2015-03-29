/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package web;

import AudioPlayer.plugin.IsPluginType;
import java.net.URI;
import util.functional.functor.FunctionC;

/**
 <p>
 @author Plutonium_
 */
@IsPluginType
public interface HttpSearchQueryBuilder extends FunctionC<String,URI> {
    
}
