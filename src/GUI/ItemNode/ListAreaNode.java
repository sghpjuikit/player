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
 * List editor with transformation ability. Editable area with function editor
 * displaying the list contents.
 * <p>
 * Allows:
 * <ul>
 * <li> displaying the elements of the list as strings in the text area
 * <li> manual editing of the text in the text area
 * <li> creating and applying arbitrary function chain transformation on the list
 * elements producing list of different type
 * </ul>
 * <p>
 * The input list is retained and all (if any) transformations are applied on it
 * every time a change in the transformation chain occurs. Every time the text is
 * updated, all manual changes of the text are lost.
 * <p>
 * The input can be set:
 * <ul>
 * <li> as string which will be split by lines to list of strings. {@link #setData(java.lang.String)}
 * <li> list of objects. The list must be homogeneous. {@link #setData(java.util.List)}
 * </ul>
 * but is not necessary. The area can be used without input. Transformation chain
 * starts a Void.class which still allows functions that supply value, which in
 * return again allows scaling the transformation chain without limitation. 
 * <p>
 * The result can be accessed as:
 * <ul>
 * <li> text at any time equal to the visible text of the text area. {@link #getValueAsS()}
 * <li> list of strings. Each string element represents a single line in the text
 * area. List can be empty, but not null. {@link #getValue() }
 * </ul>
 * 
 * @author Plutonium_
 */
public class ListAreaNode extends ItemNodeBase<List<String>> {
    
    private final VBox root = new VBox();
    private final TextArea area = new TextArea();
    private final FunctionChainItemNode transforms = new FunctionChainItemNode(Functors::getI);
    private List<Object> input;
    
    public ListAreaNode() {
        transforms.onItemChange = f -> area.setText(toS(input,f.andThen(toString),"\n"));
        area.textProperty().addListener((o,ov,nv) -> changeValue(split(nv,"\n",x->x)));
        // layout
        root.getChildren().addAll(area,transforms.getNode());
        VBox.setVgrow(area, ALWAYS);
    }
    
    /**
     * Sets the input list directly. Must be homogeneous - all elements of the 
     * same type.
     * <p>
     * Discards previous input list. Transformation chain is cleared if the type
     * of list has changed.
     * Updates text of the text area.
     */
    public void setData(List<? extends Object> input) {
        this.input = (List) input;
        Class c = input.isEmpty() ? Void.class : input.get(0).getClass();
        transforms.setTypeIn(c);    // fires update
    }
    
    /**
     * Sets the input list by splitting the text by '\n' newline character.
     * <p>
     * Discards previous input list. Transformation chain is cleared if the type
     * of list has changed.
     * Updates text of the text area.
     * 
     * @param text 
     */
    public void setData(String text) {
        setData(split(text, "\n", c->c));
    }
    
    /** 
     * Returns the current text in the text area. Represents concatenation of
     * string representations of the elements of the list after transformation,
     * if any.
     */
    public String getValueAsS() {
        return area.getText();
    }

    /** {@inheritDoc} */
    @Override
    public VBox getNode() {
        return root;
    }
    
}