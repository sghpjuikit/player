package sp.it.pl.gui.itemnode.textfield;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.Blend;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.ColorInput;
import javafx.scene.effect.DisplacementMap;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.effect.Glow;
import javafx.scene.effect.ImageInput;
import javafx.scene.effect.InnerShadow;
import javafx.scene.effect.Lighting;
import javafx.scene.effect.MotionBlur;
import javafx.scene.effect.PerspectiveTransform;
import javafx.scene.effect.Reflection;
import javafx.scene.effect.SepiaTone;
import javafx.scene.effect.Shadow;
import javafx.scene.input.MouseEvent;
import sp.it.pl.gui.objects.icon.Icon;
import sp.it.pl.gui.objects.picker.Picker;
import sp.it.pl.gui.objects.popover.PopOver;
import sp.it.pl.util.type.ClassName;
import static javafx.geometry.Pos.CENTER;
import static sp.it.pl.main.AppUtil.APP;
import static sp.it.pl.main.AppBuildersKt.appTooltip;
import static sp.it.pl.util.conf.Configurable.configsFromFxPropertiesOf;
import static sp.it.pl.util.dev.Util.log;
import static sp.it.pl.util.functional.Util.list;
import static sp.it.pl.util.functional.Util.stream;
import static sp.it.pl.util.graphics.Util.layHorizontally;

public class EffectItemNode extends TextFieldItemNode<Effect> {

	public static class EffectType {
		/** Effect class. Can be null. Null represents no effect. */
		public final Class<? extends Effect> type;

		public EffectType(Class<? extends Effect> type) {
			this.type = type;
		}

		String name() {
			return type==null ? "None" : ClassName.of(type);
		}

		Effect instantiate() {
			try {
				return type==null ? null : type.getConstructor().newInstance();
			} catch (InstantiationException|IllegalAccessException|NoSuchMethodException|InvocationTargetException x) {
				log(EffectItemNode.class).error("Config could not instantiate effect", x);
				return null;
			}
		}
	}

	private static final Tooltip typeTooltip = appTooltip("Choose type of effect");
	private static final Tooltip propTooltip = appTooltip("Configure effect");
	public static final List<EffectType> EFFECT_TYPES = list(
			new EffectType(Blend.class),
			new EffectType(Bloom.class),
			new EffectType(BoxBlur.class),
			new EffectType(ColorAdjust.class),
			new EffectType(ColorInput.class),
			new EffectType(DisplacementMap.class),
			new EffectType(DropShadow.class),
			new EffectType(GaussianBlur.class),
			new EffectType(Glow.class),
			new EffectType(ImageInput.class),
			new EffectType(InnerShadow.class),
			new EffectType(Lighting.class),
			new EffectType(MotionBlur.class),
			new EffectType(PerspectiveTransform.class),
			new EffectType(Reflection.class),
			new EffectType(SepiaTone.class),
			new EffectType(Shadow.class),
			new EffectType(null)
	);

	private final Icon typeB = new Icon();
	private final Icon propB = new Icon();
	private final Class<? extends Effect> limitedToType;

	/** Creates effect editor which can edit an effect or create effect of any type. */
	public EffectItemNode() {
		this(null);
	}

	/** Creates effect editor which can edit an effect or create effect of specified type. */
	public EffectItemNode(Class<? extends Effect> effectTypeLimit) {
		super(et -> ClassName.of(et.getClass()));
		limitedToType = effectTypeLimit==Effect.class ? null : effectTypeLimit;
		setEditable(false);

		typeB.styleclass("effect-config-field-type-button");
		typeB.tooltip(typeTooltip);
		typeB.onClick(this::openChooser);
		propB.styleclass("effect-config-field-conf-button");
		propB.tooltip(propTooltip);
		propB.onClick(this::openProperties);

		setRight(layHorizontally(5, CENTER, typeB, propB));

		// initialize value & graphics
		setValue(null);
	}

	@Override
	void onDialogAction() {}

	@Override
	public void setValue(Effect value) {
		propB.setDisable(value==null);
		super.setValue(value);
	}

	private void openChooser(MouseEvent me) {
		Picker<EffectType> w = new Picker<>();
		w.itemSupply = limitedToType==null ? EFFECT_TYPES::stream : () -> stream(new EffectType(limitedToType), new EffectType(null));
		w.textConverter = EffectType::name;
		w.getNode().setPrefSize(300, 500);
		PopOver<?> p = new PopOver<>(w.getNode());
		p.title.set("Effect");
		p.setArrowSize(0);
		p.setAutoFix(true);
		p.setAutoHide(true);
		p.show(propB);
		w.onCancel = p::hide;
		w.onSelect = ec -> {
			setValue(ec.instantiate());
			openProperties(me);
			p.hide();
		};
	}

	private void openProperties(MouseEvent me) {
		if (v!=null) APP.windowManager.showSettings(configsFromFxPropertiesOf(v), me);
		me.consume();
	}
}