package audio;

import java.io.File;
import java.net.URI;

/**
 * Simplest {@link Item} implementation. Wraps {@link java.net.URI}. Immutable.
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