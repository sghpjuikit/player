package audio.tagging;

import audio.playlist.PlaylistItem;
import java.util.List;

public class PlaylistItemGroup {
	public final List<PlaylistItem> items;

	public PlaylistItemGroup(List<PlaylistItem> items) {
		this.items = items;
	}
}