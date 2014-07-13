package AudioPlayer.services.LastFM;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

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
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.util.Duration;
import utilities.Log;
import utilities.TODO;

/**
 *
 * @author Michal
 */
public class LastFMManager {

    private static String apiKey;
    
        private static BooleanProperty scrobblingEnabled = new SimpleBooleanProperty(false);

    /**
     * Get the value of scrobblingEnabled
     *
     * @return the value of scrobblingEnabled
     */
    public static boolean getScrobblingEnabled() {
        return scrobblingEnabled.get();
    }
    
    public static BooleanProperty scrobblingEnabledProperty(){
        return scrobblingEnabled;
    }

    public static String getHiddenPassword() {
        return "****";
    }

    public static void saveLogin(String value, String value0) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Set the value of scrobblingEnabled
     *
     * @param value
     */
    public void setScrobblingEnabled(boolean value) {
        LastFMManager.scrobblingEnabled.set(value);
    }
    
    public static void toggleScrobbling(){
        scrobblingEnabled.set(!scrobblingEnabled.get());
    }
    
    private final String secret;

    /**
     * Last.fm Username required for write last.fm operations - scrobbling
     * username not required for reading
     */
    private static String username;

    private static String password;

    private Session session;
    private static Preferences preferences = Preferences.userNodeForPackage(LastFMManager.class);

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
            Duration.minutes(4),
            () -> {
                Log.deb("Time event for scrobbling fired");
                setTimeSatisfied(true);
            },
            "LastFM time event handler");

    /**
     *
     */
    public LastFMManager() {
        apiKey = acquireApiKey();
        secret = acquireSecret();

    }

    public void initialize() {

        acquireUserName();
        session = Authenticator.getMobileSession(username, "yourpassword", apiKey, secret);

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

    public static String acquireUserName() {
//        username = preferences.get("lastfm_username", null);
        return "yourusername";
    }
    @TODO("Implement")
    public static boolean isLoginSet(){
        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        return false;
    }
    public static final void saveUserName(String username) {
        preferences.put("lastfm_username", username);
    }

    public final void updateNowPlaying() {
        Metadata currentMetadata = AudioPlayer.Player.getCurrentMetadata();
        ScrobbleResult result = Track.updateNowPlaying(
                currentMetadata.getArtist(),
                currentMetadata.getTitle(),
                session
        );

//        System.out.println("ok: " + (result.isSuccessful() && !result.isIgnored()));
    }

    private void scrobble(Metadata track) {
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

    public void destroy() {
        PLAYBACK.realTimeProperty().removeOnTimeAt(percentEvent);
        PLAYBACK.realTimeProperty().removeOnTimeAt(timeEvent);
    }

}
