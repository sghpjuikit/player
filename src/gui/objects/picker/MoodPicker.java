/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gui.objects.picker;

import services.database.Db;
import audio.tagging.Metadata;

/** Mood picker. */
public class MoodPicker extends Picker<String> {

    public MoodPicker() {
        super();
        itemSupply = () -> Db.string_pool.getStrings(Metadata.Field.MOOD.name()).stream();
    }
    
}
