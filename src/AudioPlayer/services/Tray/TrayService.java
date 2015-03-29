/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.services.Tray;

import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.services.Service;
import GUI.GUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import static javafx.application.Platform.runLater;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.MouseButton;
import static javafx.scene.input.MouseButton.*;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import static javafx.stage.StageStyle.*;
import javax.imageio.ImageIO;
import main.App;
import org.reactfx.Subscription;
import static util.Util.menuItem;
import util.dev.Log;

/**
 * Provides tray facilities, including tray icon, tray tooltip, tray click
 * actions or tray bubble notification.
 *
 * @author thedoctor
 */
public class TrayService implements Service{

    private boolean running = false;
    private ObservableList<javafx.scene.control.MenuItem> menuItems;
    private EventHandler<javafx.scene.input.MouseEvent> onClick;
    private static SystemTray tray;
    private File imgUrl = new File(App.getLocation(), "icon.jpg");
    private static TrayIcon trayIcon;
    
    // disposable
    Subscription d1;

    @Override
    public void start(){
        // build context menu
        Stage s = new Stage(TRANSPARENT);
              s.initStyle(UTILITY);
              s.setOpacity(0);
              s.setScene(new Scene(new Region()));
//              s.show();
        ContextMenu cm = new ContextMenu();
                    cm.getItems().addAll(menuItem("Play/pause", PLAYBACK::pause_resume),
                        menuItem("Exit", App::close)
                    );
                    s.focusedProperty().addListener((o,ov,nv) -> {
                        if(!nv) {
                            if(cm.isShowing()) cm.hide();
                            if(s.isShowing()) s.hide();
                        }
                    });
                    cm.setAutoFix(true);
                     cm.setConsumeAutoHidingEvents(false);
//                    cm.setOnShown(e -> run(3000, cm::hide));
        menuItems = cm.getItems();
        // build tray
        EventQueue.invokeLater(() -> {
            try {
                tray = SystemTray.getSystemTray();
                trayIcon = new TrayIcon(ImageIO.read(imgUrl));
                trayIcon.setToolTip("PlayerFX");
                trayIcon.addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) {
                        // transform to javaFX MouseEvent
                        int bi = e.getButton();
                        MouseButton b = bi==1 ? PRIMARY : bi==3 ? SECONDARY : bi==2 ? MIDDLE : NONE;
                        javafx.scene.input.MouseEvent me = new javafx.scene.input.MouseEvent(MOUSE_CLICKED, -1, -1,
                                        e.getXOnScreen(), e.getYOnScreen(), b, e.getClickCount(),
                                        e.isShiftDown(),e.isControlDown(),e.isAltDown(),e.isMetaDown(),
                                        b==PRIMARY, false, b==SECONDARY, false, true, true, null);
                        
                        // show menu on right click
                        if(me.getButton()==SECONDARY)
                            runLater(()->{
                                s.show();
                                s.requestFocus();
                                cm.show(s, me.getScreenX(), me.getScreenY()-40);
                            });
//                            runLater(() -> cm.show(App.getWindow().getStage(), me.getScreenX(), me.getScreenY()-40));
                        
                        if(me.getButton()==PRIMARY)
                            runLater(GUI::toggleMinimize);
                        
                        // run custom mouse action
                        if(onClick!=null)
                            runLater(()->onClick.handle(me));
                    }
                });
                tray.add(trayIcon);
            } 
            catch (AWTException | IOException e){
                Log.err("Tray icon initialization failed.");
            }
        });
        
        d1 = Player.playingtem.subscribeToUpdates(m -> setTooltipText(m.getTitle().isEmpty() ? "PlayerFX" : "PlayerFX -  " + m.getTitle()));
        
        running = true;
    }

    @Override
    public boolean isRunning(){
        return running;
    }

    @Override
    public void stop(){
        running = false;
        d1.unsubscribe();
        EventQueue.invokeLater(() -> {
            if (tray != null) tray.remove(trayIcon);
            tray = null;
        });
    }

    @Override
    public boolean isSupported(){
        return SystemTray.isSupported();
    }

    @Override
    public boolean isDependency(){
        return false;
    }
    
    /**
    Sets the tooltip string for this TrayIcon. The tooltip is displayed 
    automatically when the mouse hovers over the icon. Setting the tooltip to
    null removes any tooltip text. When displayed, the tooltip string may be 
    truncated on some platforms; the number of characters that may be displayed 
    is platform-dependent.
    
    @param text - the string for the tooltip; if the value is null no tooltip is shown
    */
    public void setTooltipText(String text){
        EventQueue.invokeLater(() -> trayIcon.setToolTip(text.isEmpty() ? "PlayerFX" : text));
    }

    /**  Equivalent to: {@code showNotification(caption,text,NONE)} */
    public void showNotification(String caption, String text){
        EventQueue.invokeLater(() -> trayIcon.displayMessage(caption, text, TrayIcon.MessageType.NONE));
    }
    
    /**
    Shows an OS tray bubble message notification.
    
    @param caption - the caption displayed above the text, usually in bold; may 
    be null 
    @param text - the text displayed for the particular message; may be null
    @param messageType - an enum indicating the message type
    
    @throws NullPointerException - if both caption and text are null
    */
    public void showNotification(String caption, String text, TrayIcon.MessageType type){
        EventQueue.invokeLater(() -> trayIcon.displayMessage(caption, text, type));
    }

    public void setIcon(File img){
        imgUrl = img;
        if (trayIcon != null){
            Image oldImage = trayIcon.getImage();
            if (oldImage != null){
                oldImage.flush();
            }

            try {
                trayIcon.setImage(ImageIO.read(img));
            }
            catch (IOException ex){
                Log.err("Couldnt read the image for tray icon.");
            }
        }
    }
    
    /** Sets mouse event handler on the tray icon. */
    public void setOnTrayClick(EventHandler<javafx.scene.input.MouseEvent> action) {
        onClick = action;
    }

    /** Editable list of menu items of a popup menu showed on tray right mouse click. */
    public ObservableList<javafx.scene.control.MenuItem> getMenuItems(){
        return menuItems;
    }
    
}
