/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI.objects.Pickers;

import AudioPlayer.tagging.MoodManager;

/** Mood picker. */
public class MoodPicker extends Picker<String> {

    public MoodPicker() {
        super();
        itemSupply = MoodManager.moods::stream;
    }
    
}
