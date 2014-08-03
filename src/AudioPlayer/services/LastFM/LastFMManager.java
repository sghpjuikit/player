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
import AudioPlayer.ItemChangeEvent.ItemChangeHandler;
import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.playback.PercentTimeEventHandler;
import AudioPlayer.playback.TimeEventHandler;
import AudioPlayer.tagging.Metadata;
import Configuration.IsConfig;
import Configuration.IsConfigurable;
import Configuration.ValueConfig;
import Configuration.ValueConfigurable;
import GUI.objects.SimpleConfigurator;
import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Caller;
import de.umass.lastfm.Result;
import de.umass.lastfm.Session;
import de.umass.lastfm.Track;
import de.umass.lastfm.scrobble.ScrobbleResult;
import java.util.prefs.Preferences;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.util.Duration;
import utilities.Log;
import utilities.Password;
import utilities.TODO;

/**
 *
 * @author Michal
 */
@IsConfigurable("LastFM")
public class LastFMManager {

    private static String username;
    private static final String apiKey = "f429ccceafc6b81a6ffad442cec758c3";
    private static final String secret = "8097fcb4a54a9805599060e47ab69561";

    private static Session session;
    private static final Preferences preferences = Preferences.userNodeForPackage(LastFMManager.class);

    private static boolean percentSatisfied;
    private static boolean timeSatisfied;

    
    private static boolean loginSuccess;
    private boolean durationSatisfied;
    @IsConfig(name = "Scrobbling on")
    private static final BooleanProperty scrobblingEnabled = new SimpleBooleanProperty(false){

        @Override
        public void set(boolean newValue) {
       
           
                if(newValue){
                    if(isLoginSet()){
                        session = Authenticator.getMobileSession(
                                acquireUserName(), 
                                acquirePassword().get(), 
                                apiKey, secret);
                        Result lastResult = Caller.getInstance().getLastResult();                    
                        if(lastResult.getStatus() != Result.Status.FAILED){
                            LastFMManager.setLoginSuccess(true);
                            LastFMManager.initialize();
                            super.set(true);
                        }else{
                            LastFMManager.setLoginSuccess(false);
                            LastFMManager.destroy();
                            super.set(false);
                        }
                    }
                }
                else {
                    LastFMManager.destroy();
                    super.set(false);
                }
            }
        
    
    };

 
    
    private static void setLoginSuccess(boolean b) {
       loginSuccess = b; 
    }
    public static boolean isLoginSuccess(){
        return loginSuccess;
    }
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
            Player.remOnItemUpdate(itemChange);
            Player.addOnItemChange(itemChange);   
            
            PLAYBACK.realTimeProperty().setOnTimeAt(timeEvent);
            PLAYBACK.realTimeProperty().setOnTimeAt(percentEvent);
              
    }
    
    
    @TODO("Implement")
    public static boolean isLoginSet() {
        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        return !"".equals(acquireUserName());
    }
    
    public static void saveLogin(String value, Password value0) {
        if(saveUserName(value) && savePassword(value0)){
            scrobblingEnabled.set(true);
        }else{
            scrobblingEnabled.set(false);
        } 
    }
    
    
    public static SimpleConfigurator getLastFMconfig(){
        return new SimpleConfigurator(
            new ValueConfigurable(
                new ValueConfig("Username", LastFMManager.acquireUserName()),
                new ValueConfig("Password", LastFMManager.acquirePassword())                                  
            ), 
            vc -> LastFMManager.saveLogin(
               (String)vc.getFields().get(0).getValue(),
               (Password)vc.getFields().get(1).getValue())                                     
        );    

    }
    
    public static final boolean saveUserName(String username) {
        preferences.put("lastfm_username", username);  
        return preferences.get("lastfm_username", "").equals(username);
        
    }
    public static final boolean savePassword(Password pass) {
        preferences.put("lastfm_password", pass.get());
        return preferences.get("lastfm_password", "").equals(pass.get());
    }
    public static String acquireUserName() {
        return preferences.get("lastfm_username", "");
    }
    private static Password acquirePassword(){
        return new Password(preferences.get("lastfm_password", ""));
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
        Log.info("Scrobbling: " + track.getArtist() + " - " + track.getTitle());
        int now = (int) (System.currentTimeMillis() / 1000);
        ScrobbleResult result = Track.scrobble(track.getArtist(), track.getTitle(), now, session);
    }
    
    private static void reset() {
        timeSatisfied = percentSatisfied = false;
    }
       
    private static final PercentTimeEventHandler percentEvent = new PercentTimeEventHandler(
            0.5,
            () -> {
                Log.deb("Percent event for scrobbling fired");
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
    
    private static final ItemChangeHandler<Metadata> itemChange = (oldValue, newValue) -> {
                if ((timeSatisfied || percentSatisfied)
                        && oldValue.getLength().greaterThan(Duration.seconds(30))) {
                    scrobble(oldValue);
    //                System.out.println("Conditions for scrobling satisfied. Track should scrobble now.");
                }
                updateNowPlaying();
                reset();
            };
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
    
    
    
      public static void destroy() {
        Player.remOnItemUpdate(itemChange);
        PLAYBACK.realTimeProperty().removeOnTimeAt(percentEvent);
        PLAYBACK.realTimeProperty().removeOnTimeAt(timeEvent);
    }
}
