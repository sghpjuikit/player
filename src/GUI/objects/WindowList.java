/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.objects;

import GUI.Window;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Plutonium_
 */
public class WindowList {
    public HashMap<Integer,Window> list;

    public WindowList(List<Window> ws) {
        list = new HashMap();
        for(int i=0; i<ws.size(); i++) {
            list.put(i, ws.get(i));
        }
    }
}