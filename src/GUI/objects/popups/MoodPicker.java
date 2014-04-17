/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.objects.popups;

import AudioPlayer.tagging.MoodManager;
import GUI.objects.PopOver.PopOver;
import GUI.objects.PopOver.PopOver.NodeCentricPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import utilities.functional.functor.UnProcedure;

/**
 *
 * @author Plutonium_
 */
public class MoodPicker {
    
    private final GridPane grid = new GridPane();
    private final ScrollPane scroll = new ScrollPane(grid);
    private final PopOver popup = new PopOver(scroll);
    private UnProcedure onMoodSelect;
    
    public MoodPicker() {        
        grid.setHgap(5);
        grid.setVgap(5);
        scroll.setPrefWidth(400);
        scroll.setPrefHeight(400);
        
        popup.setDetachable(false);
        popup.setArrowSize(0);
        popup.setArrowIndent(0);
        popup.setCornerRadius(0);
        popup.setAutoHide(true);
        popup.setAutoFix(false);
        buildContent();
    }
    
    private void buildContent() {
        int size = MoodManager.moods.size();
        String[] moods = MoodManager.moods.toArray(new String[0]);
        for (int i=0; i<size; i++) {
            Button b = new Button(moods[i]);
                   b.setOnAction( e -> {
                       onMoodSelect.accept(b.getText());
                       popup.hide();
                    });
            grid.add(b, i%5, i/5);
        }
    }

    public void show(Node n, NodeCentricPos pos, UnProcedure<String> onMoodSelect) {
        this.onMoodSelect = onMoodSelect;
        buildContent();
        popup.show(n, pos);
    }
    public boolean isShowing() {
        return popup.isShowing();
    }
}
