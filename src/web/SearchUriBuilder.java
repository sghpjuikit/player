package web;

import java.net.URI;
import util.functional.Functors.Ƒ1;
import util.plugin.IsPluginType;

/**
 * Builds a {@link java.net.URI} for searching for a resource with a string.
 */
@IsPluginType
public interface SearchUriBuilder extends Ƒ1<String,URI> {}