/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.ItemNode;

import gui.ItemNode.ItemNode.ItemNodeBase;
import java.util.List;
import java.util.function.Supplier;
import static java.util.stream.Collectors.joining;
import javafx.scene.control.TextArea;
import static javafx.scene.layout.Priority.ALWAYS;
import javafx.scene.layout.VBox;
import util.collections.PrefList;
import util.functional.Functors.PF;
import static util.functional.Util.split;

/**
 *
 * @author Plutonium_
 */
public class StringListItemNode extends ItemNodeBase<List<String>>{
    
    private final VBox root = new VBox();
    private final TextArea area = new TextArea();
    private final FunctionChainItemNode<String> transforms;
    private List<String> input;
    
    public StringListItemNode(Supplier<PrefList<PF<String,String>>> functionPool) {
        transforms = new FunctionChainItemNode(functionPool);
        transforms.onItemChange = f -> area.setText(input.stream().map(f).collect(joining("\n")));
        area.textProperty().addListener((o,ov,nv) -> changeValue(split(nv, "\n", x->x)));
        // layout
        root.getChildren().addAll(area,transforms.getNode());
        VBox.setVgrow(area, ALWAYS);
    }
    
    public void setData(List<String> input) {
        this.input = input;
        transforms.onItemChange.accept(transforms.getValue());
    }

    @Override
    public VBox getNode() {
        return root;
    }
    
}
