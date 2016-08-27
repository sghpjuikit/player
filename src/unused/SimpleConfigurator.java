package unused;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Screen;

import gui.itemnode.ConfigField;
import util.access.V;
import util.conf.Config;
import util.conf.Configurable;
import util.graphics.fxml.ConventionFxmlLoader;

import static javafx.scene.input.KeyCode.ENTER;
import static util.dev.Util.noØ;
import static util.functional.Util.byNC;
import static util.functional.Util.stream;

/**
 * Configurable state transformer graphical control. Graphics to configure 
 * {@link util.conf.Configurable}.
 * <p/>
 * When OK button is clicked all changed {@link gui.itemnode.ConfigField}s with unapplied values
 * will be set and applied. Then the specified behavior is executed.
 * 
 * @param <T> Specifies generic type of Configurable for this component. Only
 * use it for singleton configurables or configurables that contain configs with
 * the same value type.
 * <p/>
 * The advantage of using generic version is in accessing the values in the
 * OK button callback. The configurable is provided as a parameter and if this
 * object is generic it will provide correct configurable returning correct
 * values without type casting.
 * @see util.conf.Configurable
 * @author Martin Polakovic
 */
public class SimpleConfigurator<T> extends AnchorPane {
    
    @FXML private GridPane fields;
    @FXML private BorderPane buttonPane;
    @FXML private ScrollPane fieldsPane;
    
    private final double anchor;
    private final List<ConfigField<T>> configFields = new ArrayList<>();
    private final Configurable<T> configurable;
    /**
     * Procedure executed when user finishes the configuring. 
     * Invoked when ok button is pressed.
     * Default implementation does nothing. Must not be null.
     * <p/>
     * For example one might want to close this control when no item is selected.
     */
    public final Consumer<? super Configurable<T>> onOK;
	/**
	 * Denotes whether parent window or popup should close immediately after {@link #onOK} executes. Default false.
	 */
	public final V<Boolean> hideOnOk = new V<>(false);

	public SimpleConfigurator(Config<T> c, Consumer<? super T> on_OK) {
		this((Configurable<T>) c, ignored -> on_OK.accept(c.getValue()));
	}

    /**
     * @param c configurable object
     * @param on_OK OK button click action. Null if none. Affects visibility
     * of the OK button. It is only visible if there is an action to execute.
     */
    @SafeVarargs
    public SimpleConfigurator(Configurable<T> c, Consumer<? super Configurable<T>>... on_OK) {
	    noØ(c);
        
        configurable = c;
        onOK = cf -> stream(on_OK).forEach(action -> action.accept(cf));

        // load fxml part
        new ConventionFxmlLoader(this).loadNoEx();

        fieldsPane.setMaxHeight(Screen.getPrimary().getBounds().getHeight()*0.7);
        anchor = AnchorPane.getBottomAnchor(fieldsPane);
        setOkButtonVisible(on_OK!=null);
        
        // set configs
        configFields.clear();
        fields.getChildren().clear();
        c.getFields().stream()
         .sorted(byNC(Config::getGuiName))
         .filter(Config::isEditable)
         .forEach(f -> {
            ConfigField cf = ConfigField.create(f);                 // create
            configFields.add(cf);                                   // add
            fields.add(cf.createLabel(), 0, configFields.size()-1);    // populate
            fields.add(cf.getNode(), 1, configFields.size()-1);
        });
        

	    fieldsPane.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
	    	if (e.getCode()==ENTER)
			    ok();
	    });
        fieldsPane.setOnScroll(Event::consume); // prevent scroll event leak
    }

    @FXML
    public void ok() {
        // set and apply values and refresh if needed
        configFields.forEach(ConfigField::apply);
        onOK.accept(configurable);
	    if (hideOnOk.get() && getScene()!=null && getScene().getWindow()!=null && getScene().getWindow().isShowing()) {
		    if (getScene().getWindow().isShowing()) getScene().getWindow().hide();
	    }
    }

    public void focusFirstConfigField() {
    	if (!configFields.isEmpty())
    		configFields.get(0).focus();
    }

    public Configurable getConfigurable() {
        return configurable;
    }
    
    public final void setOkButtonVisible(boolean val) {
        buttonPane.setVisible(val);
        AnchorPane.setBottomAnchor(fieldsPane, val ? anchor : 0d);
    }
}
