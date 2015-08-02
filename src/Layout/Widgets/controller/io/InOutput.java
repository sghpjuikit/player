/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Layout.Widgets.controller.io;

import java.util.UUID;

import javafx.collections.ObservableList;

import static javafx.collections.FXCollections.observableArrayList;

/**
 *
 * @author Plutonium_
 */
public class InOutput<T> {
    public final Input<T> i;
    public final Output<T> o;
    
    public InOutput(UUID id, String name, Class<? super T> c) {
        o = new Output<>(id, name, c);
        i = new Input<>(name, c, o::setValue);

        inoutputs.add(this);
    }
    
    public static ObservableList<InOutput> inoutputs = observableArrayList();
}
