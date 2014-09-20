/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.playlist;

import java.io.File;
import java.io.Serializable;
import java.net.URI;
import jdk.nashorn.internal.ir.annotations.Immutable;

/**
 * Simple {@link Item} implementation. 
 * Wraps URI, providing simplest Item implementation. Serializable.
 * <p>
 * @author uranium
 */
@Immutable
public class SimpleItem extends Item implements Serializable {
    private static final long serialVersionUID = 100034L;
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
