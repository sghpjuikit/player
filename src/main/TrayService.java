/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javax.imageio.ImageIO;
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

    void setTootip(String text){
        trayIcon.setToolTip(text);
    }

    void popUpBaloon(String caption, String text){
        EventQueue.invokeLater(() -> trayIcon.displayMessage(text, text,
                                                               TrayIcon.MessageType.NONE));
    }

    public void nowPlaying(String name){
        EventQueue.invokeLater(() -> {
            if(!name.isEmpty()){
            setTootip("Now playinh:" + name);
            popUpBaloon("",name);}
            else{
            setTootip("PlayerFX");
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
    public boolean isRunning(){
        return tray != null;
    }

    @Override
    public boolean isSupported(){
        return SystemTray.isSupported();
    }

    @Override
    public void start(){
        EventQueue.invokeLater(() -> {
            menuActions.add(Tuples.t("Play/Pause", () -> Platform.runLater(
                                     PLAYBACK::pause_resume)));
            menuActions.add(Tuples.t("Quit", () -> Platform.runLater(
                                     Platform::exit)));
            try{
                tray = SystemTray.getSystemTray();
//trayIcon = new TrayIcon(ImageIO.read(new URL("http://icons.iconarchive.com/icons/scafer31000/bubble-circle-3/16/GameCenter-icon.png")));
                trayIcon = new TrayIcon(ImageIO.read(imgUrl));
                buildContextmenu();
                trayIcon.setToolTip("PlayerFX");
                trayIcon.addActionListener(event -> {
                    Platform.runLater(GUI::toggleMinimizeWithFocus);
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
            if (tray != null){
                tray.remove(trayIcon);
            }
            tray = null; // stop this service
        });
    }

    private void buildContextmenu(){
        PopupMenu p = new PopupMenu();
        menuActions.forEach(i -> {
            MenuItem n = new MenuItem(i._1);
            p.add(n).addActionListener(event -> Platform.runLater(i._2));
        });
        trayIcon.setPopupMenu(p);
    }
}
