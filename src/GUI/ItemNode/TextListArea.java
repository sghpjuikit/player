/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.ItemNode;

import GUI.ItemNode.FunctionChainItemNode;
import GUI.ItemNode.ItemNode;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import static util.Util.capitalizeStrong;
import util.collections.PrefList;
import util.functional.Functors.PF;
import static util.functional.Util.split;

/**
 *
 * @author Plutonium_
 */
public class TextListArea extends ItemNode<List<String>>{
    
    private final VBox root = new VBox();
    private final Label nameL = new Label();
    private final TextArea area = new TextArea();
    private final FunctionChainItemNode<String> transforms;
    private List<String> input;
    
    public TextListArea(Supplier<PrefList<PF<String,String>>> functionPool) {
        transforms = new FunctionChainItemNode(functionPool);
        transforms.onItemChange = f -> area.setText(input.stream().map(f).collect(Collectors.joining("\n")));
        area.textProperty().addListener((o,ov,nv) -> changeValue(split(nv, "\n", x->x)));
        // layout
        root.getChildren().addAll(new StackPane(nameL),area,transforms.getNode());
    }
    
    public void setData(String name, List<String> input) {
        nameL.setText(capitalizeStrong(name));
        this.input = input;
        transforms.onItemChange.accept(transforms.getValue());
    }

    @Override
    public Node getNode() {
        return root;
    }
    
}
