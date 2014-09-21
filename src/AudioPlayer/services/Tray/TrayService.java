/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.services.Tray;

import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.services.Service;
import GUI.GUI;
import java.awt.AWTException;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javax.imageio.ImageIO;
import main.App;
import org.reactfx.util.Tuple2;
import org.reactfx.util.Tuples;
import util.Log;

/**
 *
 * @author thedoctor
 */
public class TrayService implements Service{
    private File imgUrl = new File(App.getLocation(), "icon.png");
    private final List<Tuple2<String, Runnable>> menuActions = new ArrayList();
    private static SystemTray tray;
    private static TrayIcon trayIcon;

    public void setTootip(String text){
        trayIcon.setToolTip(text.isEmpty() ? "PlayerFX" : text);
    }

    /** Equivalent to: {@code setNotification(caption,text,NONE)}*/
    public void setNotification(String caption, String text){
        EventQueue.invokeLater(() -> trayIcon.displayMessage(caption, text, TrayIcon.MessageType.NONE));
    }
    
    public void setNotification(String caption, String text, TrayIcon.MessageType type){
        EventQueue.invokeLater(() -> trayIcon.displayMessage(caption, text, type));
    }

    public void nowPlaying(String name){
        EventQueue.invokeLater(() -> {
                setTootip(name);
            if(!name.isEmpty()) {
                setTootip("PlayerFX playing: " + name);
                setNotification("",name);
            }
        });
    }

    public void addItem(String name, Runnable r){
        menuActions.add(Tuples.t(name, r));
    }

    public void setIcon(File img){
        imgUrl = img;
        if (trayIcon != null){
            Image oldImage = trayIcon.getImage();
            if (oldImage != null){
                oldImage.flush();
            }

            try{
                trayIcon.setImage(ImageIO.read(img));
            }
            catch (IOException ex){
                Log.err("Couldnt read the image for tray icon.");
            }
        }
    }

    public List<Tuple2<String, Runnable>> getMenu(){
        return menuActions;
    }

    @Override
    public boolean isDependency(){
        return false;
    }

    @Override
    public void start(){
        EventQueue.invokeLater(() -> {
            menuActions.add(Tuples.t("Play/Pause", () -> Platform.runLater(
                                     PLAYBACK::pause_resume)));
            menuActions.add(Tuples.t("Quit", () -> Platform.runLater(
                                     Platform::exit)));
            try {
                tray = SystemTray.getSystemTray();
                trayIcon = new TrayIcon(ImageIO.read(imgUrl));
                buildContextmenu();
                trayIcon.setToolTip("PlayerFX");
//                trayIcon.addActionListener(event -> {
//                    Platform.runLater(GUI::toggleMinimize);
//                });
                trayIcon.addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) {
                        if (e.getClickCount() == 1)
                            Platform.runLater(GUI::toggleMinimize);
                    }
                });
                tray.add(trayIcon);
            } 
            catch (AWTException | IOException e){
                Log.err("Tray icon initialization failed.");
            }
        });
    }

    @Override
    public void stop(){
        EventQueue.invokeLater(() -> {
            if (tray != null) tray.remove(trayIcon);
            tray = null; // stop this service
        });
    }

    @Override
    public boolean isRunning(){
        return tray != null;
    }

    @Override
    public boolean isSupported(){
        return SystemTray.isSupported();
    }
    
/******************************************************************************/

    private void buildContextmenu(){
        PopupMenu p = new PopupMenu();
        menuActions.forEach(i -> {
            MenuItem n = new MenuItem(i._1);
            p.add(n).addActionListener(event -> Platform.runLater(i._2));
        });
        trayIcon.setPopupMenu(p);
    }
}
