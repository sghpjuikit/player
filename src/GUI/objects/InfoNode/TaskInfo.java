/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects.InfoNode;

import javafx.concurrent.Task;
import javafx.scene.control.Labeled;
import javafx.scene.control.ProgressIndicator;

/**
 *  Provides information about the task and its progres.
 * 
 * @author Plutonium_
 */
public class TaskInfo implements InfoNode<Task> {
    
    public Labeled labeled;
    public ProgressIndicator progressIndicator;
    

    public TaskInfo(Labeled labeled, ProgressIndicator pi) {
        this.labeled = labeled;
        this.progressIndicator = pi;
        
    }
    
    /** {@inheritDoc} */
    @Override
    public void setVisible(boolean v) {
        if(labeled!=null) labeled.setVisible(v);
        if(progressIndicator!=null) progressIndicator.setVisible(v);
    }
    
    /** {@inheritDoc} */
    @Override
    public void bind(Task t) {
        unbind();
        if(progressIndicator!=null) progressIndicator.progressProperty().bind(t.progressProperty());
        if(labeled!=null) labeled.textProperty().bind(t.messageProperty());
    }
    
    /** {@inheritDoc} */
    @Override
    public void unbind() {
        if(progressIndicator!=null) progressIndicator.progressProperty().unbind();
        if(labeled!=null) labeled.textProperty().unbind();
    }
    
}
