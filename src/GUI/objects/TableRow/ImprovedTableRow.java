/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects.TableRow;

import java.util.function.BiConsumer;
import javafx.scene.control.TableRow;
import javafx.scene.input.MouseButton;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import javafx.scene.input.MouseEvent;

/**
 * {@link TableRow} with additional methods.
 * <p>
 * <li> flow API for frequently used mouse handlers
 * For example: {@code new ImprovedRow().onLeftSingleClick((row,event) -> {...});}
 * <p>
 * @author Plutonium_
 */
public class ImprovedTableRow<T> extends TableRow<T>{
    
    private BiConsumer<ImprovedTableRow<T>,MouseEvent> onL1Click = null;
    private BiConsumer<ImprovedTableRow<T>,MouseEvent> onL2Click = null;
    private BiConsumer<ImprovedTableRow<T>,MouseEvent> onR1Click = null;
    private BiConsumer<ImprovedTableRow<T>,MouseEvent> onR2Click = null;

    public ImprovedTableRow() {
        super();
        
        setOnMouseClicked(e -> {
            if(!isEmpty()) {
                MouseButton b = e.getButton();
                int clicks = e.getClickCount();
                if(b==PRIMARY && clicks==1 && onL1Click!=null) onL1Click.accept(this,e);
                if(b==PRIMARY && clicks==2 && onL2Click!=null) onL2Click.accept(this,e);
                if(b==SECONDARY && clicks==1 && onR1Click!=null) onR1Click.accept(this,e);
                if(b==SECONDARY && clicks==2 && onR2Click!=null) onR2Click.accept(this,e);
            }
        });
    }
    /** 
     * Registers handler for single left click. Does nothing if row empty.
     * @param handler, which takes this row as additional parameter.
     * 
     * @return this
     */
    public ImprovedTableRow<T> onLeftSingleClick(BiConsumer<ImprovedTableRow<T>,MouseEvent> handler) {
        onL1Click = handler;
        return this;
    }
    public ImprovedTableRow<T> onLeftDoubleClick(BiConsumer<ImprovedTableRow<T>,MouseEvent> handler) {
        onL2Click = handler;
        return this;
    }
    public ImprovedTableRow<T> onRightSingleClick(BiConsumer<ImprovedTableRow<T>,MouseEvent> handler) {
        onR1Click = handler;
        return this;
    }
    public ImprovedTableRow<T> onRightDoubleClick(BiConsumer<ImprovedTableRow<T>,MouseEvent> handler) {
        onR2Click = handler;
        return this;
    }
}
