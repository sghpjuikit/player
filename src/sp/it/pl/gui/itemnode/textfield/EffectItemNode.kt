package sp.it.pl.gui.itemnode.textfield

import javafx.event.EventHandler
import javafx.geometry.Pos.CENTER
import javafx.scene.effect.Blend
import javafx.scene.effect.Bloom
import javafx.scene.effect.BoxBlur
import javafx.scene.effect.ColorAdjust
import javafx.scene.effect.ColorInput
import javafx.scene.effect.DisplacementMap
import javafx.scene.effect.DropShadow
import javafx.scene.effect.Effect
import javafx.scene.effect.GaussianBlur
import javafx.scene.effect.Glow
import javafx.scene.effect.ImageInput
import javafx.scene.effect.InnerShadow
import javafx.scene.effect.Lighting
import javafx.scene.effect.MotionBlur
import javafx.scene.effect.PerspectiveTransform
import javafx.scene.effect.Reflection
import javafx.scene.effect.SepiaTone
import javafx.scene.effect.Shadow
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Region
import mu.KLogging
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.gui.objects.picker.Picker
import sp.it.pl.gui.objects.popover.PopOver
import sp.it.pl.main.AppUtil.APP
import sp.it.pl.main.appTooltip
import sp.it.pl.util.conf.Configurable.configsFromFxPropertiesOf
import sp.it.pl.util.functional.Util.stream
import sp.it.pl.util.graphics.Util.layHorizontally
import sp.it.pl.util.type.ClassName
import java.lang.reflect.InvocationTargetException
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier
import kotlin.reflect.KClass

class EffectItemNode: TextFieldItemNode<Effect> {

    private val typeB: Icon
    private val propB: Icon
    private val limitedToType: Class<out Effect>?

    /** Creates effect editor which can edit an effect or create effect of any specified types or any type if no specified. */
    @JvmOverloads constructor(effectType: Class<out Effect>? = null): super({ ClassName.of(it.javaClass) }) {
        typeB = Icon().apply {
            styleclass("effect-config-field-type-button")
            tooltip(typeTooltip)
            onClick(EventHandler { openChooser(it) })
        }
        propB = Icon().apply {
            styleclass("effect-config-field-conf-button")
            tooltip(propTooltip)
            onClick(EventHandler { openProperties(it) })
        }
        limitedToType = if (effectType==Effect::class.java) null else effectType
        isEditable = false
        right.value = layHorizontally(5.0, CENTER, typeB, propB)

        value = null    // initialize value & graphics
    }

    override fun onDialogAction() {}

    override fun setValue(value: Effect?) {
        propB.isDisable = value==null
        super.setValue(value)
    }

    private fun openChooser(me: MouseEvent) {
        val p = PopOver<Region>().apply {
            title.set("Effect")
            arrowSize.value = 0.0
            isAutoFix = true
            isAutoHide = true
            contentNode.value = Picker<EffectType>().apply {
                itemSupply = limitedToType
                        ?.let { Supplier { stream(EffectType(limitedToType.kotlin), EffectType(null)) } }
                        ?: Supplier { EFFECT_TYPES.stream() }
                textConverter = Function { it.name() }
                node.setPrefSize(300.0, 500.0)
                onCancel = Runnable { hide() }
                onSelect = Consumer {
                    value = it.instantiate()
                    openProperties(me)
                    hide()
                }
            }.node
        }
        p.showInCenterOf(propB)
    }

    private fun openProperties(me: MouseEvent) {
        if (v!=null) APP.windowManager.showSettings(configsFromFxPropertiesOf(v), me)
        me.consume()
    }

    companion object: KLogging() {

        private val typeTooltip = appTooltip("Choose type of effect")
        private val propTooltip = appTooltip("Configure effect")
        @JvmField val EFFECT_TYPES = listOf(
                EffectType(Blend::class),
                EffectType(Bloom::class),
                EffectType(BoxBlur::class),
                EffectType(ColorAdjust::class),
                EffectType(ColorInput::class),
                EffectType(DisplacementMap::class),
                EffectType(DropShadow::class),
                EffectType(GaussianBlur::class),
                EffectType(Glow::class),
                EffectType(ImageInput::class),
                EffectType(InnerShadow::class),
                EffectType(Lighting::class),
                EffectType(MotionBlur::class),
                EffectType(PerspectiveTransform::class),
                EffectType(Reflection::class),
                EffectType(SepiaTone::class),
                EffectType(Shadow::class),
                EffectType(null)
        )
    }

    class EffectType {

        /** Effect type. Null represents no effect.  */
        val type: Class<out Effect>?

        constructor(type: KClass<out Effect>?) {
            this.type = type?.java
        }

        internal fun name(): String = if (type==null) "None" else ClassName.of(type)

        internal fun instantiate(): Effect? = try {
            type?.getConstructor()?.newInstance()
        } catch (e: InstantiationException) {
            logger.error(e) { "Config could not instantiate effect" }
            null
        } catch (e: IllegalAccessException) {
            logger.error(e) { "Config could not instantiate effect" }
            null
        } catch (e: NoSuchMethodException) {
            logger.error(e) { "Config could not instantiate effect" }
            null
        } catch (e: InvocationTargetException) {
            logger.error(e) { "Config could not instantiate effect" }
            null
        }
    }

}