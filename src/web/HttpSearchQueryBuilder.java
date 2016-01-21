/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package web;

import java.net.URI;

import AudioPlayer.plugin.IsPluginType;
import util.functional.Functors.Ƒ1;

/**
 * Transforms text into a search query.
 */
@IsPluginType
public interface HttpSearchQueryBuilder extends Ƒ1<String,URI> {}
