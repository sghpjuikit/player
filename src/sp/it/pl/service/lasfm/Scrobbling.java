package sp.it.pl.service.lasfm;

import de.umass.lastfm.Session;
import de.umass.lastfm.Track;
import de.umass.lastfm.scrobble.ScrobbleResult;
import java.util.prefs.Preferences;
import sp.it.pl.audio.Player;
import sp.it.pl.audio.tagging.Metadata;
import static sp.it.pl.util.dev.DebugKt.logger;

public class Scrobbling {

	private static Session session;

	private final Preferences preferences;

	public Scrobbling() {
		String apiKey = acquireApiKey();
		String secret = acquireSecret();

		preferences = Preferences.userNodeForPackage(LastFM.class);
	}

	protected void updateNowPlaying() {
		Metadata currentMetadata = Player.playingSong.get();
		ScrobbleResult result = Track.updateNowPlaying(
				currentMetadata.getArtist(),
				currentMetadata.getTitle(),
				session
		);
	}

	protected void scrobble(Metadata track) {
		logger(Scrobbling.class).info("Scrobbling: {}", track);
		int now = (int) (System.currentTimeMillis()/1000);
		ScrobbleResult result = Track.scrobble(
				track.getArtist(),
				track.getTitle(),
				now,
				session);

	}

	protected final String acquireApiKey() {
		return "f429ccceafc6b81a6ffad442cec758c3";
	}

	protected final String acquireSecret() {
		return "8097fcb4a54a9805599060e47ab69561";
	}

	Preferences getPreferences() {
		return preferences;
	}

	Session getSession() {
		return session;
	}
}