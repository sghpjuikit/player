/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.objects.Pickers;

import AudioPlayer.tagging.MoodManager;

/**
 * Mood picker pop up.
 * <p>
 * @author Plutonium_
 */
public class MoodPicker extends Picker<String> {

    public MoodPicker() {
        super();
        setAccumulator(MoodManager.moods::stream);
    }
    
}
