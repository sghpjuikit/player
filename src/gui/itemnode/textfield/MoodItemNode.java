
package gui.itemnode.textfield;

import java.util.function.Consumer;

import javafx.scene.layout.Region;

import AudioPlayer.services.database.Db;
import AudioPlayer.tagging.Metadata;
import gui.objects.Pickers.MoodPicker;
import gui.objects.PopOver.PopOver;
import util.parsing.Parser;

import static org.controlsfx.control.textfield.TextFields.bindAutoCompletion;
import static util.functional.Util.filter;

/**
 * Text field intended for mood tagging specifically. It provides two additional
 * functionalities - auto-completion from list moods registered in application
 * and button to open mood picker popup. The position of the picker popup can be
 * customized.
 *
 * @author Plutonium_
 */
public class MoodItemNode extends TextFieldItemNode<String> {

    private final Consumer<String> pickMood = this::setValue;
    private PopOver.NodePos pos = PopOver.NodePos.RightCenter;

    public MoodItemNode() {
        super(Parser.DEFAULT.toConverter(String.class));
        setEditable(true);
        // set autocompletion
        bindAutoCompletion(this, p -> filter(Db.string_pool.getStrings(Metadata.Field.MOOD.name()), t -> Db.autocmplt_filter.apply(t,p.getUserText())));
    }

    /** @return the position for the picker to show on */
    public PopOver.NodePos getPos() {
        return pos;
    }

    /** @param pos the position for the picker to show on */
    public void setPos(PopOver.NodePos pos) {
        this.pos = pos;
    }

    @Override
    void onDialogAction() {
        MoodPicker picker = getCM();
        PopOver p = new PopOver(picker.getNode());
        p.detachable.set(false);
        p.setArrowSize(0);
        p.setArrowIndent(0);
        p.setCornerRadius(0);
        p.setAutoHide(true);
        p.setAutoFix(true);
        picker.onCancel = p::hide;
        picker.onSelect = mood -> {
            setValue(mood);
            p.hide();
        };
        ((Region)picker.getNode()).setPrefSize(800,600);
        p.show(this, pos);
    }

/******************************* CONTEXT MENU *********************************/

    private static MoodPicker cm;

    private static MoodPicker getCM() {
        if(cm==null) cm = new MoodPicker();
        return cm;
    }
}
