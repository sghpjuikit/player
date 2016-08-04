/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package services.lasfm;

import java.util.prefs.Preferences;

import audio.Player;
import audio.tagging.Metadata;
import de.umass.lastfm.Session;
import de.umass.lastfm.Track;
import de.umass.lastfm.scrobble.ScrobbleResult;

import static util.dev.Util.log;

/**
 *
 * @author Michal Szeman
 */
public class Scrobbling {

    static Session session;

    private final String apiKey;
    private final String secret;

    private final Preferences preferences;

    public Scrobbling() {
        apiKey = acquireApiKey();
        secret = acquireSecret();

        preferences = Preferences.userNodeForPackage(LastFM.class);
    }

    protected void updateNowPlaying() {
        Metadata currentMetadata = Player.playingItem.get();
        ScrobbleResult result = Track.updateNowPlaying(
                currentMetadata.getArtist(),
                currentMetadata.getTitle(),
                session
        );

//        System.out.println("ok: " + (result.isSuccessful() && !result.isIgnored()));
    }

    protected void scrobble(Metadata track) {
        log(Scrobbling.class).info("Scrobbling: {}", track);
        int now = (int) (System.currentTimeMillis() / 1000);
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
