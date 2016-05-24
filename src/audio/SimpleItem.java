/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package audio;

import java.io.File;
import java.net.URI;

/**
 * Simplest {@link Item} implementation. 
 * Wraps URI. Immutable.
 *
 * @author Martin Polakovic
 */
public class SimpleItem extends Item {
    
    private final URI uri;
    
    public SimpleItem(URI _uri) {
        uri = _uri;
    }
    
    public SimpleItem(File file) {
        uri = file.toURI();
    }
    
    public SimpleItem(Item i) {
        uri = i.getURI();
    }
    
    @Override
    public final URI getURI() {
        return uri;
    }
    
}