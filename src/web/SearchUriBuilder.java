/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package web;

import java.net.URI;

import util.functional.Functors.Ƒ1;
import util.plugin.IsPluginType;

/**
 * Builds a {@link java.net.URI} for searching for a resource with a string.
 *
 * @author Martin Polakovic
 */
@IsPluginType
public interface SearchUriBuilder extends Ƒ1<String,URI> {}