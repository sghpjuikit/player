package sp.it.pl.ui.itemnode.textfield

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
import mu.KLogging
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.picker.Picker
import sp.it.pl.ui.objects.window.NodeShow.RIGHT_UP
import sp.it.pl.ui.objects.window.popup.PopWindow
import sp.it.pl.main.APP
import sp.it.pl.main.appTooltip
import sp.it.pl.main.toUi
import sp.it.util.conf.toConfigurableFx
import sp.it.util.functional.net
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.ui.Util.layHorizontally
import kotlin.reflect.KClass

class EffectTextField: ValueTextField<Effect> {
   private val typeB: Icon
   private val propB: Icon
   private val limitedToType: KClass<out Effect>?

   /** Creates effect editor which can edit an effect or create effect of any specified types or any type if no specified. */
   constructor(effectType: KClass<out Effect>? = null): super() {
      styleClass += STYLECLASS
      isEditable = false
      limitedToType = if (effectType==Effect::class) null else effectType

      typeB = Icon().apply {
         styleclass("effect-config-editor-type-button")
         tooltip(typeTooltip)
         onClickDo(::openChooser)
      }
      propB = Icon().apply {
         styleclass("effect-config-editor-conf-button")
         isDisable = value==null
         tooltip(propTooltip)
         onClickDo(::openProperties)
      }
      onValueChange += {
         propB.isDisable = value==null
      }
      right.value = layHorizontally(5.0, CENTER, typeB, propB)
   }

   override fun onDialogAction() {}

   private fun openChooser(i: Icon) {
      PopWindow().apply {
         title.value = "Effect"
         isAutohide.value = true
         content.value = Picker<EffectType>().apply {
            root.setPrefSize(300.0, 500.0)
            itemSupply = limitedToType
               ?.net { { sequenceOf(EffectType(limitedToType), EffectType(null)) } }
               ?: { EFFECT_TYPES.asSequence() }
            textConverter = { it.name }
            onCancel = { hide() }
            onSelect = {
               value = it.instantiate()
               openProperties(i)
               hide()
            }
            buildContent()
         }.root

         show(RIGHT_UP(propB))
      }
   }

   private fun openProperties(i: Icon) {
      value?.let { APP.windowManager.showSettings(it.toConfigurableFx(), i) }
   }

   companion object: KLogging() {
      const val STYLECLASS = "effect-text-field"
      private val typeTooltip = appTooltip("Choose type of effect")
      private val propTooltip = appTooltip("Configure effect")
      val EFFECT_TYPES = listOf(
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

   class EffectType(type: KClass<out Effect>?) {

      /** Effect type. Null represents no effect.  */
      val type = type
      val name = type?.toUi() ?: "None"

      fun instantiate(): Effect? = runTry {
         type?.java?.getConstructor()?.newInstance()
      }.ifError {
         logger.error(it) { "Config could not instantiate effect" }
      }.orNull()
   }

}