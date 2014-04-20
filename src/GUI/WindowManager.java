/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI;

import Configuration.IsConfig;
import Layout.Widgets.WidgetManager;
import Serialization.Serializator;
import main.App;

/**
 *
 * @author Plutonium_
 */
public class WindowManager {
    
    @IsConfig(name="Docked mode", info="Whether application layout is in mini - docked mode.")
    public static boolean mini = false;
    
    static Window miniWindow;
    
    public static void toggleMini() {
        setMini(!mini);
    }
    public static void setMini(boolean val) {
        mini = val;
            App.getInstance().getWindow().setVisible(false);
        if(val) {
            Window tmp = Serializator.deserializeWindow();
            if(tmp!=null) {
                miniWindow = tmp;
            } else {
                miniWindow = Window.create();
            }
            miniWindow.setContent(WidgetManager.getFactory("Tiny").create().load());
            miniWindow.setVisible(val);
            miniWindow.setShowHeader(false);
            miniWindow.update();
        } else {
            Serializator.serializeWindow(miniWindow);
            App.getInstance().getWindow().setVisible(true);
            miniWindow.close();
            miniWindow=null;
        }
    }
}
