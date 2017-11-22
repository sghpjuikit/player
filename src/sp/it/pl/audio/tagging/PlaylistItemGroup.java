package sp.it.pl.audio.tagging;

import java.util.List;
import sp.it.pl.audio.playlist.PlaylistItem;

public class PlaylistItemGroup {
	public final List<PlaylistItem> items;

	public PlaylistItemGroup(List<PlaylistItem> items) {
		this.items = items;
	}
}