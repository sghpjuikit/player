/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.ItemNode;

import gui.ItemNode.ItemNode.ItemNodeBase;
import java.util.List;
import javafx.scene.control.TextArea;
import static javafx.scene.layout.Priority.ALWAYS;
import javafx.scene.layout.VBox;
import util.functional.Functors;
import static util.functional.Util.*;

/**
 *
 * @author Plutonium_
 */
public class ListAreaNode<IN> extends ItemNodeBase<List<String>>{
    
    private final VBox root = new VBox();
    private final TextArea area = new TextArea();
    private final FunctionChainItemNode transforms;
    private List<? extends IN> input;
    
    public ListAreaNode() {
        transforms = new FunctionChainItemNode(Functors::getI);
        transforms.onItemChange = f -> area.setText(toS((List)input,f.andThen(toString),"\n"));
        area.textProperty().addListener((o,ov,nv) -> changeValue(split(nv,"\n",x->x)));
        // layout
        root.getChildren().addAll(area,transforms.getNode());
        VBox.setVgrow(area, ALWAYS);
    }
    
    public void setData(List<? extends IN> input) {
        this.input = input;
        Class c = input.isEmpty() ? Void.class : input.get(0).getClass();
        transforms.setTypeIn(c);
        transforms.onItemChange.accept(transforms.getValue());
    }

    @Override
    public VBox getNode() {
        return root;
    }
    
}