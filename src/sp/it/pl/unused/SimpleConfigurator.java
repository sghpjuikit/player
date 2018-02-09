package sp.it.pl.unused;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.octicons.OctIcon;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import sp.it.pl.gui.itemnode.ConfigField;
import sp.it.pl.gui.objects.icon.Icon;
import sp.it.pl.util.access.V;
import sp.it.pl.util.conf.Config;
import sp.it.pl.util.conf.Configurable;
import sp.it.pl.util.functional.Try;
import sp.it.pl.util.graphics.fxml.ConventionFxmlLoader;
import static javafx.scene.input.KeyCode.ENTER;
import static sp.it.pl.gui.itemnode.ConfigField.STYLECLASS_CONFIG_FIELD_WARN_BUTTON;
import static sp.it.pl.util.functional.Util.byNC;
import static sp.it.pl.util.functional.Util.stream;

/**
 * Configurable state transformer graphical control. Graphics to configure
 * {@link sp.it.pl.util.conf.Configurable}.
 * <p/>
 * When OK button is clicked all changed {@link sp.it.pl.gui.itemnode.ConfigField}s with un-applied values
 * will be set and applied. Then the specified behavior is executed.
 *
 * @param <T> Specifies generic type of Configurable for this component. Only use it for singleton configurables or
 * configurables that contain configs with the same value type.
 * <p/>
 * The advantage of using generic version is in accessing the values in the OK button callback. The configurable is
 * provided as a parameter and if this object is generic it will provide correct configurable returning correct values
 * without type casting.
 *
 * @see sp.it.pl.util.conf.Configurable
 */
public class SimpleConfigurator<T> extends AnchorPane {

	@FXML private GridPane fields;
	@FXML private BorderPane buttonPane;
	@FXML private StackPane okPane;
	@FXML private ScrollPane fieldsPane;
	@FXML private Label warnLabel;
	private final Icon okB = new Icon(OctIcon.CHECK, 25);

	@SuppressWarnings("FieldCanBeLocal")
	private final double anchorOk, anchorWarn = 20;
	private final List<ConfigField<T>> configFields = new ArrayList<>();
	public final Configurable<T> configurable;
	/**
	 * Procedure executed when user finishes the configuring.
	 * Invoked when ok button is pressed.
	 * Default implementation does nothing. Must not be null.
	 * <p/>
	 * For example one might want to close this control when no item is selected.
	 */
	public final Consumer<? super Configurable<T>> onOK;
	/**
	 * Denotes whether parent window or popup should close immediately after {@link #onOK} executes.
	 * Default false.
	 */
	public final V<Boolean> hideOnOk = new V<>(false);
	/**
	 * Denotes whether there is action that user can execute.
	 */
	public final V<Boolean> hasAction = new V<>(false);

	public SimpleConfigurator(Config<T> c, Consumer<? super T> on_OK) {
		this((Configurable<T>) c, ignored -> on_OK.accept(c.getValue()));
	}

	/**
	 * @param c configurable object
	 * @param on_OK OK button click actions taking the configurable as input parameter. Affects visibility of the OK
	 * button. It is only visible if there is an action to execute.
	 */
	@SafeVarargs
	@SuppressWarnings("unchecked")
	public <C extends Configurable<T>> SimpleConfigurator(C c, Consumer<? super C>... on_OK) {
		configurable = c==null ? Configurable.EMPTY_CONFIGURABLE : c;
		onOK = c==null ? cf -> {} : cf -> stream(on_OK).forEach(action -> action.accept((C) cf)); // cas is safe because we know its always C
		hasAction.set(on_OK!=null && on_OK.length>0);

		// load fxml part
		new ConventionFxmlLoader(this).loadNoEx();

		okPane.getChildren().add(okB);
		fieldsPane.setMaxHeight(Screen.getPrimary().getBounds().getHeight()*0.7);
		anchorOk = AnchorPane.getBottomAnchor(fieldsPane);
		setOkButton(hasAction.get());

		// set configs
		configFields.clear();
		fields.getChildren().clear();
		configurable.getFields().stream()
				.sorted(byNC(Config::getGuiName))
				.forEach(f -> {
					ConfigField<T> cf = ConfigField.create(f);                  // create
					configFields.add(cf);                                       // add
					fields.add(cf.createLabel(), 0, configFields.size() - 1);   // populate
					fields.add(cf.getNode(), 1, configFields.size() - 1);
				});
		Consumer<Try<T,String>> observer = v -> validate();
		configFields.forEach(f -> f.observer = observer);

		fieldsPane.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
			if (e.getCode()==ENTER) {
				ok();
				e.consume();
			}
		});
		fieldsPane.setOnScroll(Event::consume); // prevent scroll event leak

		validate();
	}

	@FXML
	public void ok() {
		if (!hasAction.get()) return;

		Try<T,String> validation = validate();
		if (validation.isOk()) {
			configFields.forEach(ConfigField::apply);
			onOK.accept(configurable);
			if (hideOnOk.get() && getScene()!=null && getScene().getWindow()!=null && getScene().getWindow().isShowing()) {
				if (getScene().getWindow().isShowing()) getScene().getWindow().hide();
			}
		}
	}

	private Try<T,String> validate() {
		Try<T,String> validation = configFields.stream()
				.map(f -> f.value.mapError(e -> f.config.getGuiName() + ": " + e))
				.reduce(Try::and).orElse(Try.ok(null));
		showWarnButton(validation);
		return validation;
	}

	public void focusFirstConfigField() {
		if (!configFields.isEmpty())
			configFields.get(0).focus();
	}

	private void showWarnButton(Try<T,String> validation) {
		if (validation.isError()) okB.styleclass(STYLECLASS_CONFIG_FIELD_WARN_BUTTON);
		okB.icon(validation.isOk() ? OctIcon.CHECK : FontAwesomeIcon.EXCLAMATION_TRIANGLE);
		okB.setMouseTransparent(validation.isError());
		buttonPane.setBottom(validation.isOk() ? null : warnLabel);
		updateAnchor();
		warnLabel.setText(validation.map(v -> "").getAny());
	}

	private void setOkButton(boolean val) {
		buttonPane.setVisible(val);
		updateAnchor();
	}

	private void updateAnchor() {
		boolean isOkVisible = buttonPane.isVisible();
		boolean isWarnVisible = buttonPane.getBottom()!=null;
		double a = (isOkVisible ? anchorOk : 0d) + (isWarnVisible ? anchorWarn : 0d);
		AnchorPane.setBottomAnchor(fieldsPane, a);
	}
}