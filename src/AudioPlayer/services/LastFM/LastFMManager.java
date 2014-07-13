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

    private static String username;
    private static final String apiKey = "f429ccceafc6b81a6ffad442cec758c3";
    private static final String secret = "8097fcb4a54a9805599060e47ab69561";

    private static Session session;
    private static final Preferences preferences = Preferences.userNodeForPackage(LastFMManager.class);

    private static boolean percentSatisfied;
    private static boolean timeSatisfied;
    private boolean durationSatisfied;
    private static final BooleanProperty scrobblingEnabled = new SimpleBooleanProperty(false);

 

    public static String getHiddenPassword() {
        return "****";
    }

    



    public static void toggleScrobbling() {
        scrobblingEnabled.set(!scrobblingEnabled.get());
    }


    /**
     *
     */
    public LastFMManager() {  
    }

    public static void initialize() {
        if(scrobblingEnabled.get()){
            acquireUserName();
            session = Authenticator.getMobileSession(acquireUserName(), acquirePassword() , apiKey, secret);

            Player.addOnItemChange((oldValue, newValue) -> {
                if ((timeSatisfied || percentSatisfied)
                        && oldValue.getLength().greaterThan(Duration.seconds(30))) {
                    scrobble(oldValue);
    //                System.out.println("Conditions for scrobling satisfied. Track should scrobble now.");
                }

                updateNowPlaying();
                reset();
            });
            PLAYBACK.realTimeProperty().setOnTimeAt(timeEvent);
            PLAYBACK.realTimeProperty().setOnTimeAt(percentEvent);
        }        
    }


 

    @TODO("Implement")
    public static boolean isLoginSet() {
        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        return true;
    }
    
    public static void saveLogin(String value, String value0) {
        saveUserName(value);
        savePassword(value0);
        initialize();
    }
    
    public static final void saveUserName(String username) {
        preferences.put("lastfm_username", username);
    }
    public static final void savePassword(String pass) {
        preferences.put("lastfm_password", pass);
    }
    public static String acquireUserName() {
        return preferences.get("lastfm_username", null);        
    }
    private static String acquirePassword(){
        return preferences.get("lastfm_password", null);
    }
    
    /************** Scrobble logic - event handlers etc ***********************/
     
    public static final void updateNowPlaying() {
        Metadata currentMetadata = AudioPlayer.Player.getCurrentMetadata();
        ScrobbleResult result = Track.updateNowPlaying(
                currentMetadata.getArtist(),
                currentMetadata.getTitle(),
                session
        );
    }

    private static void scrobble(Metadata track) {
        Log.mess("Scrobbling: " + track);
        int now = (int) (System.currentTimeMillis() / 1000);
        ScrobbleResult result = Track.scrobble(track.getArtist(), track.getTitle(), now, session);
    }
    
    private static void reset() {
        timeSatisfied = percentSatisfied = false;
    }
       
    private static final PercentTimeEventHandler percentEvent = new PercentTimeEventHandler(
            0.5,
            () -> {
                Log.deb("percent event for scrobbling fired");
                setPercentSatisfied(true);
            },
            "LastFM percent event handler.");

    private static final TimeEventHandler timeEvent = new TimeEventHandler(
            Duration.minutes(4),
            () -> {
                Log.deb("Time event for scrobbling fired");
                setTimeSatisfied(true);
            },
            "LastFM time event handler");
    
/*     *************   GETTERS and SETTERS    ****************************    */
    
    public static boolean getScrobblingEnabled() {
        return scrobblingEnabled.get();
    }

    public static BooleanProperty scrobblingEnabledProperty() {
        return scrobblingEnabled;
    }

 
    public void setScrobblingEnabled(boolean value) {
        LastFMManager.scrobblingEnabled.set(value);
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
