
package GUI.objects.ItemTextFields;

import AudioPlayer.tagging.MoodManager;
import GUI.objects.PopOver.PopOver;
import java.util.stream.Collectors;
import org.controlsfx.control.textfield.TextFields;
import utilities.Parser.StringParser;
import utilities.Parser.StringStringParser;
import utilities.functional.functor.UnProcedure;

/**
 * Text field intended for mood tagging specifically. It provides two additional
 * functionalities - auto-completion from list moods registered in application 
 * and button to open mood picker popup. The position of the picker popup can be
 * customized. 
 *
 * @author Plutonium_
 */
public class MoodTextField extends ItemTextField<String,StringParser<String>> {
    
    private final UnProcedure<String> pickMood = this::setItem;
    private PopOver.NodeCentricPos pos = PopOver.NodeCentricPos.RightCenter;

    public MoodTextField() {
        this(StringStringParser.class);
    }
    public MoodTextField(Class<? extends StringParser<String>> parser_type) {
        super(parser_type);
        
        // set autocompletion
        TextFields.bindAutoCompletion(this, p -> MoodManager.moods.stream()
                    .filter( t -> t.startsWith(p.getUserText()) )
                    .collect(Collectors.toList()));
        
        setEditable(true);
    }
    
    /** 
     * Adds word to moods. New moods are registered by application automatically
     * during tagging, so this method doesn't need to be invoked, it is possible
     * though to extend this field's functionality by it.
     */
    private void autoCompletionLearnWord(String newWord){
        MoodManager.moods.add(newWord);
    }
    
    /** @return the position for the picker to show on */
    public PopOver.NodeCentricPos getPos() {
        return pos;
    }

    /** @param pos the position for the picker to show on */
    public void setPos(PopOver.NodeCentricPos pos) {
        this.pos = pos;
    }

    @Override
    void onDialogAction() {
        GUI.GUI.MOOD_PICKER.setOnSelect(pickMood);
        GUI.GUI.MOOD_PICKER.show(this, pos);
    }

    @Override
    String itemToString(String item) {
        return item;
    }
}
