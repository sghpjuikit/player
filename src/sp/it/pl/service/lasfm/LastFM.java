package sp.it.pl.service.lasfm;

import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Caller;
import de.umass.lastfm.Result;
import de.umass.lastfm.Session;
import de.umass.lastfm.Track;
import de.umass.lastfm.scrobble.ScrobbleResult;
import java.util.function.Consumer;
import java.util.prefs.Preferences;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.util.Duration;
import org.reactfx.Subscription;
import sp.it.pl.audio.Player;
import sp.it.pl.audio.tagging.Metadata;
import sp.it.pl.gui.objects.form.Form;
import sp.it.pl.util.conf.Config;
import sp.it.pl.util.conf.IsConfig;
import sp.it.pl.util.conf.IsConfigurable;
import sp.it.pl.util.conf.MapConfigurable;
import sp.it.pl.util.conf.ValueConfig;
import sp.it.pl.util.text.Password;
import sp.it.pl.util.validation.Constraint.PasswordNonEmpty;
import sp.it.pl.util.validation.Constraint.StringNonEmpty;
import static sp.it.pl.gui.objects.form.Form.form;
import static sp.it.pl.util.dev.DebugKt.logger;
import static sp.it.pl.util.functional.UtilKt.consumer;

// TODO: make thread-safe, remove static, implement Service
@IsConfigurable("Services.LastFM")
public class LastFM {

	private static String username;
	private static final String apiKey = "f429ccceafc6b81a6ffad442cec758c3";
	private static final String secret = "8097fcb4a54a9805599060e47ab69561";

	private static Session session;
	private static final Preferences preferences = Preferences.userNodeForPackage(LastFM.class);

	private static boolean percentSatisfied;
	private static boolean timeSatisfied;
	private static boolean loginSuccess;
	private static boolean durationSatisfied;

	@IsConfig(name = "Scrobbling on")
	private static final BooleanProperty scrobblingEnabled = new SimpleBooleanProperty(false) {
		@Override
		public void set(boolean newValue) {
			if (newValue) {
				if (isLoginSet()) {
					session = Authenticator.getMobileSession(
							acquireUserName(),
							acquirePassword().getValue(),
							apiKey, secret);
					Result lastResult = Caller.getInstance().getLastResult();
					if (lastResult.getStatus()!=Result.Status.FAILED) {
						LastFM.setLoginSuccess(true);
						LastFM.start();
						super.set(true);
					} else {
						LastFM.setLoginSuccess(false);
						LastFM.stop();
						super.set(false);
					}
				}
			} else {
				LastFM.stop();
				super.set(false);
			}
		}
	};

	private static void setLoginSuccess(boolean b) {
		loginSuccess = b;
	}

	public static boolean isLoginSuccess() {
		return loginSuccess;
	}

	public static String getHiddenPassword() {
		return "****";
	}

	public static void toggleScrobbling() {
		scrobblingEnabled.set(!scrobblingEnabled.get());
	}

	public LastFM() { }

	public static void start() {
		playingItemMonitoring = Player.playingItem.onChange(itemChangeHandler);

//        PLAYBACK.realTimeProperty().setOnTimeAt(timeEvent);
//        PLAYBACK.realTimeProperty().setOnTimeAt(percentEvent);
	}

	// TODO: implement
	public static boolean isLoginSet() {
		return !"".equals(acquireUserName());
	}

	public static void saveLogin(String value, Password value0) {
		if (saveUserName(value) && savePassword(value0)) {
			scrobblingEnabled.set(true);
		} else {
			scrobblingEnabled.set(false);
		}
	}

	@SuppressWarnings("unchecked")
	public static Form<Object> getLastFMconfig() {
		return form(
				new MapConfigurable<Object>(
					(Config) new ValueConfig<>(String.class, "Username", acquireUserName()).constraints(new StringNonEmpty()),
					(Config) new ValueConfig<>(Password.class, "Password", acquirePassword()).constraints(new PasswordNonEmpty())
				),
				consumer(c -> saveLogin(
					(String) c.getField("Username").getValue(),
					(Password) c.getField("Password").getValue()
				))
		);

	}

	public static boolean saveUserName(String username) {
		preferences.put("lastfm_username", username);
		return preferences.get("lastfm_username", "").equals(username);

	}

	public static boolean savePassword(Password pass) {
		preferences.put("lastfm_password", pass.getValue());
		return preferences.get("lastfm_password", "").equals(pass.getValue());
	}

	public static String acquireUserName() {
		return preferences.get("lastfm_username", "");
	}

	private static Password acquirePassword() {
		return new Password(preferences.get("lastfm_password", ""));
	}

	/************** Scrobble logic - event handlers etc ***********************/

	public static void updateNowPlaying() {
		Metadata currentMetadata = sp.it.pl.audio.Player.playingItem.get();
		ScrobbleResult result = Track.updateNowPlaying(
				currentMetadata.getArtist(),
				currentMetadata.getTitle(),
				session
		);
	}

	private static void scrobble(Metadata track) {
		logger(LastFM.class).info("Scrobbling: " + track.getArtist() + " - " + track.getTitle());
		int now = (int) (System.currentTimeMillis()/1000);
		ScrobbleResult result = Track.scrobble(track.getArtist(), track.getTitle(), now, session);
	}

	private static void reset() {
		timeSatisfied = percentSatisfied = false;
	}

	private static Subscription playingItemMonitoring;

//    private static final PercentTimeEventHandler percentEvent = new PercentTimeEventHandler(
//            0.5,
//            () -> {
//                Log.deb("Percent event for scrobbling fired");
//                setPercentSatisfied(true);
//            },
//            "LastFM percent event handler.");
//
//    private static final TimeEventHandler timeEvent = new TimeEventHandler(
//            Duration.minutes(4),
//            () -> {
//                Log.deb("Time event for scrobbling fired");
//                setTimeSatisfied(true);
//            },
//            "LastFM time event handler");

	private static final Consumer<Metadata> itemChangeHandler = item -> {
		if ((timeSatisfied || percentSatisfied)
				&& item.getLength().greaterThan(Duration.seconds(30))) {
			scrobble(item);
		}
		updateNowPlaying();
		reset();
	};

	public static boolean getScrobblingEnabled() {
		return scrobblingEnabled.get();
	}

	public static BooleanProperty scrobblingEnabledProperty() {
		return scrobblingEnabled;
	}

	public static void setScrobblingEnabled(boolean value) {
		scrobblingEnabled.set(value);
	}

	private static void setTimeSatisfied(boolean b) {
		timeSatisfied = b;
	}

	private static void setPercentSatisfied(boolean b) {
		percentSatisfied = b;
	}

	public static void stop() {
		if (playingItemMonitoring!=null) playingItemMonitoring.unsubscribe();
//        PLAYBACK.realTimeProperty().removeOnTimeAt(percentEvent);
//        PLAYBACK.realTimeProperty().removeOnTimeAt(timeEvent);
	}
}
