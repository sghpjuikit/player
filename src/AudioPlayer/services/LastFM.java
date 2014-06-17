/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.services;
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.playback.PercentTimeEventHandler;
import AudioPlayer.playback.TimeEventHandler;
import AudioPlayer.tagging.Metadata;
import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Session;
import de.umass.lastfm.Track;
import de.umass.lastfm.scrobble.ScrobbleResult;
import java.util.prefs.Preferences;
import javafx.util.Duration;
import utilities.Log;

/**
 *
 * @author Michal
 */
public class LastFM {

    private static String apiKey;
    private final String secret;

    /**
     * Last.fm Username required for write last.fm operations - scrobbling
     * username not required for reading
     */
    private static String username;

    private static String password;

    private Session session;
    private final Preferences preferences;

    private static boolean percentSatisfied;
    private static boolean timeSatisfied;
    private boolean durationSatisfied;

    private final PercentTimeEventHandler percentEvent = new PercentTimeEventHandler(
            0.5,
            () -> {
                Log.deb("percent event for scrobbling fired");
                setPercentSatisfied(true);
            },
            "LastFM percent event handler.");

    private final TimeEventHandler timeEvent = new TimeEventHandler(
            Duration.minutes(1),
            () -> {
                Log.deb("time event for scrobbling fired");
                setTimeSatisfied(true);
            },
            "LastFM time event handler");

    /**
     *
     */
    public LastFM() {
        apiKey = acquireApiKey();
        secret = acquireSecret();

        preferences = Preferences.userNodeForPackage(LastFM.class);
    }

    public void initialize() {

        acquireUserName();
        session = Authenticator.getMobileSession(username, "avs2004", apiKey, secret);

        Player.addOnItemChange((oldValue, newValue) -> {
            if ((timeSatisfied || percentSatisfied)
                    && oldValue.getLength().greaterThan(Duration.seconds(30))) {
                scrobble(oldValue);
//                System.out.println("Conditions for scrobling satisfied. Track should scrobble now.");
            }
            System.out.println("hello from scrobbler");
            updateNowPlaying();
            reset();
        });

        PLAYBACK.realTimeProperty().setOnTimeAt(timeEvent);

        PLAYBACK.realTimeProperty().setOnTimeAt(percentEvent);
    }

    /**
     *
     * @return
     */
    private String acquireApiKey() {
        return "f429ccceafc6b81a6ffad442cec758c3";
    }

    private String acquireSecret() {
        return "8097fcb4a54a9805599060e47ab69561";
    }

    private void acquireUserName() {
//        username = preferences.get("lastfm_username", null);
        username = "myungpetrucci";
    }

    private void saveUserName(String username) {
        preferences.put("lastfm_username", username);
    }

    public final void updateNowPlaying() {
        Metadata currentMetadata = AudioPlayer.Player.getCurrentMetadata();
        ScrobbleResult result = Track.updateNowPlaying(
                currentMetadata.getArtist(),
                currentMetadata.getTitle(),
                session
        );

        System.out.println("ok: " + (result.isSuccessful() && !result.isIgnored()));
    }

    public final void scrobble(Metadata track) {
        Log.mess("Scrobbling: " + track);
        int now = (int) (System.currentTimeMillis() / 1000);
        ScrobbleResult result = Track.scrobble(track.getArtist(), track.getTitle(), now, session);
    }

    private static void reset() {
        timeSatisfied = percentSatisfied = false;
    }

    private static void setTimeSatisfied(boolean b) {
        timeSatisfied = b;
    }

    private static void setPercentSatisfied(boolean b) {
        percentSatisfied = b;
    }

    public void changeUser(String username, String password) {
        saveUserName(username);

        session = Authenticator.getMobileSession(this.username, "symphonyx", apiKey, secret);
    }

    public void destroy() {
        PLAYBACK.realTimeProperty().removeOnTimeAt(percentEvent);
        PLAYBACK.realTimeProperty().removeOnTimeAt(timeEvent);
    }

}
