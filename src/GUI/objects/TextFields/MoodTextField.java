
package GUI.objects.TextFields;

import AudioPlayer.tagging.MoodManager;
import GUI.objects.PopOver.PopOver;
import java.util.List;
import java.util.stream.Collectors;
import javafx.scene.control.TextField;
import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;
import utilities.functional.functor.UnProcedure;

/**
 * Text field intended for mood tagging specifically. It provides two additional
 * functionalities - auto-completion from list moods registered in application 
 * and button to open mood picker popup. The position of the picker popup can be
 * customized. 
 *
 * @author Plutonium_
 */
public class MoodTextField extends CustomTextField {
    
    private static final List<String> STYLE_CLASS = new TextField().getStyleClass();
    
    private final UnProcedure<String> pickMood = mood -> setText(mood);
    private PopOver.NodeCentricPos pos = PopOver.NodeCentricPos.RightCenter;

    public MoodTextField() {
        
        // set the button to the right
        setRight(new DialogButton("..."));
        getRight().setOnMouseClicked(e-> {
            GUI.GUI.MOOD_PICKER.show(this, pos, pickMood);
        });
        
        // set autocompletion
        TextFields.bindAutoCompletion(this, p -> MoodManager.moods.stream()
                    .filter( t -> t.startsWith(p.getUserText()) )
                    .collect(Collectors.toList()));
        
        //set same cass style as TextField
        getStyleClass().setAll(STYLE_CLASS);
    }
    
    /** 
     * Adds word to moods. New moods are registered by application automatically
     * suring tagging, so this method doesnt need to be invoked, it is possible
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
}
