/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.ItemNode;

import java.util.function.Supplier;
import java.util.stream.Stream;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import util.collections.PrefList;
import util.functional.Functors.PF;

/**
 *
 * @author Plutonium_
 */
public class TransformableTextListArea extends StringListItemNode implements TransformableList<String,String> {
    
    private ObservableList<String> inputList;
    private final ObservableList<String> outputList = FXCollections.observableArrayList();
    
    public TransformableTextListArea(Supplier<PrefList<PF<String, String>>> functionPool) {
        super(functionPool);
    }

    public void bind(TransformableList<String,String> source) {
//        inputList
    }
    
    @Override
    public Stream<String> getListIn() {
        return inputList.stream();
    }

    @Override
    public Stream<String> getListOut() {
        return outputList.stream();
    }
}
