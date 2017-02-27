package services.tray;

import audio.Player;
import audio.playback.PLAYBACK;
import gui.Gui;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import org.reactfx.Subscription;
import services.Service.ServiceBase;
import util.access.V;
import util.conf.IsConfig;
import util.conf.IsConfigurable;
import static javafx.application.Platform.runLater;
import static javafx.scene.input.MouseButton.*;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static javafx.stage.StageStyle.TRANSPARENT;
import static javafx.stage.StageStyle.UTILITY;
import static main.App.APP;
import static util.dev.Util.log;
import static util.graphics.Util.menuItem;

/**
 * Provides tray facilities, including tray icon, tray tooltip, tray click
 * actions or tray bubble notification.
 *
 * @author thedoctor
 */
@IsConfigurable("Tray")
public class TrayService extends ServiceBase {

	private String tooltip_text = null;
	@IsConfig(name = "Show tooltip", info = "Enables tooltip displayed when mouse hovers tray icon.")
	public final V<Boolean> showTooltip = new V<>(true, v -> { if (isRunning()) setTooltipText(tooltip_text);});
	@IsConfig(name = "Show playing in tooltip", info = "Shows playing song title in tray tooltip.")
	public final V<Boolean> showPlayingInTooltip = new V<>(true);

	private boolean running = false;
	private ObservableList<javafx.scene.control.MenuItem> menuItems;
	private EventHandler<javafx.scene.input.MouseEvent> onClick;
	private SystemTray tray;
	private File image = new File(APP.DIR_APP, "icon16.png");
	private TrayIcon trayIcon;
	private Stage s;

	private Subscription d1;

	public TrayService() {
		super(true);
	}

	@Override
	public void start() {
		// build context menu
		s = new Stage(TRANSPARENT);
		s.initStyle(UTILITY);
		s.setOpacity(0);
		s.setScene(new Scene(new Region()));
		ContextMenu cm = new ContextMenu();
		cm.getItems().addAll(
				menuItem("New window", () -> APP.windowManager.createWindow()),
				menuItem("Play/pause", PLAYBACK::pause_resume),
				menuItem("Exit", APP::close)
		);
		s.focusedProperty().addListener((o, ov, nv) -> {
			if (!nv) {
				if (cm.isShowing()) cm.hide();
				if (s.isShowing()) s.hide();
			}
		});
		cm.setAutoFix(true);
		cm.setConsumeAutoHidingEvents(false);
		// cm.setOnShown(e -> run(3000, cm::hide));
		menuItems = cm.getItems();

		// build tray
		EventQueue.invokeLater(() -> {
			try {
				tray = SystemTray.getSystemTray();
				trayIcon = new TrayIcon(ImageIO.read(image));
				trayIcon.setToolTip(APP.name);
				trayIcon.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						// transform to javaFX MouseEvent
						int bi = e.getButton();
						MouseButton b = bi==1 ? PRIMARY : bi==3 ? SECONDARY : bi==2 ? MIDDLE : NONE;
						javafx.scene.input.MouseEvent me = new javafx.scene.input.MouseEvent(MOUSE_CLICKED, -1, -1,
								e.getXOnScreen(), e.getYOnScreen(), b, e.getClickCount(),
								e.isShiftDown(), e.isControlDown(), e.isAltDown(), e.isMetaDown(),
								b==PRIMARY, false, b==SECONDARY, false, true, true, null);

						// show menu on right click
						if (me.getButton()==SECONDARY)
							runLater(() -> {
								s.show();
								s.requestFocus();
								cm.show(s, me.getScreenX(), me.getScreenY() - 40);
							});
//                            runLater(() -> cm.show(App.getWindow().getStage(), me.getScreenX(), me.getScreenY()-40));

						if (me.getButton()==PRIMARY)
							runLater(Gui::toggleMinimize);

						// run custom mouse action
						if (onClick!=null)
							runLater(() -> onClick.handle(me));
					}
				});
				trayIcon.setImageAutoSize(true);    // icon may not show without this
				tray.add(trayIcon);
			} catch (AWTException|IOException e) {
				log(TrayService.class).error("Tray icon initialization failed.", e);
			}
		});

		d1 = Player.playingItem.onUpdate(m ->
				setTooltipText(!showPlayingInTooltip.get() || m.getTitle().isEmpty() ? APP.name : APP.name + " - " + m.getTitle()));

		running = true;
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public void stop() {
		running = false;

		if (s!=null) s.close();
		s = null;
		d1.unsubscribe();
		EventQueue.invokeLater(() -> {
			if (tray!=null) tray.remove(trayIcon);
			tray = null;
		});
	}

	@Override
	public boolean isSupported() {
		return SystemTray.isSupported();
	}

	/**
	 * Sets the tooltip string for this tray icon. The tooltip is displayed
	 * automatically when the mouse hovers over the icon.
	 * <p/>
	 * If {@link #showTooltip} is set to false no tooltip will be displayed
	 * regardless of the value. Otherwise:
	 * <ul>
	 * <li> null removes tooltip
	 * <li> empty text will display application name
	 * <li> normal text will cause it to be displayed
	 * </ul>
	 *
	 * @param text - the string for the tooltip; if the value is null no tooltip is shown
	 */
	public void setTooltipText(String text) {
		if (!isRunning()) return;

		tooltip_text = text==null ? null : text.isEmpty() ? APP.name : text;
		String s = showTooltip.getValue() ? text : null;
		EventQueue.invokeLater(() -> trayIcon.setToolTip(s));
	}

	/** Equivalent to: {@code showNotification(caption,text,NONE)}. */
	public void showNotification(String caption, String text) {
		if (!isRunning()) return;

		EventQueue.invokeLater(() -> trayIcon.displayMessage(caption, text, TrayIcon.MessageType.NONE));
	}

	/**
	 * Shows an OS tray bubble message notification.
	 *
	 * @param caption - the caption displayed above the text, usually in bold; may be null
	 * @param text - the text displayed for the particular message; may be null
	 * @param type - an enum indicating the message type
	 * @throws NullPointerException - if both caption and text are null
	 */
	public void showNotification(String caption, String text, TrayIcon.MessageType type) {
		if (!isRunning()) return;

		EventQueue.invokeLater(() -> trayIcon.displayMessage(caption, text, type));
	}

	public void setIcon(File img) {
		if (!isRunning()) return;

		image = img;
		if (trayIcon!=null) {
			Image oldImage = trayIcon.getImage();
			if (oldImage!=null) {
				oldImage.flush();
			}

			try {
				trayIcon.setImage(ImageIO.read(img));
			} catch (IOException e) {
				log(TrayService.class).error("Could not read the image for tray icon.", e);
			}
		}
	}

	/** Sets mouse event handler on the tray icon. */
	public void setOnTrayClick(EventHandler<javafx.scene.input.MouseEvent> action) {
		onClick = action;
	}

	/** Editable list of menu items of a popup menu showed on tray right mouse click. */
	public ObservableList<javafx.scene.control.MenuItem> getMenuItems() {
		return menuItems;
	}

}