package sp.it.pl.ui.objects.textfield

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
import javafx.scene.layout.VBox
import kotlin.reflect.KClass
import mu.KLogging
import sp.it.pl.main.AppTexts.textNoVal
import sp.it.pl.main.toUi
import sp.it.pl.ui.objects.SpitComboBox
import sp.it.pl.ui.pane.ConfigPane
import sp.it.util.access.v
import sp.it.util.access.vn
import sp.it.util.collections.setTo
import sp.it.util.conf.Configurable
import sp.it.util.conf.toConfigurableFx
import sp.it.util.functional.net
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.reactive.attach
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncTo
import sp.it.util.ui.lay

/** Effect editor which can edit/create [Effect]. */
class EffectTextField(isNullable: Boolean, effectType: KClass<out Effect>? = null, initialValue: Effect?): VBox() {
   private val isNullable: Boolean = isNullable
   private val limitedToType: KClass<out Effect>? = if (effectType==Effect::class) null else effectType
   private val comboBox = SpitComboBox<EffectType>({ it.name })
   private val editors = ConfigPane<Any?>()
   val value = vn(initialValue)
   val editable = v(true)

   init {
      this.styleClass += STYLECLASS

      lay += comboBox
      lay += editors

      comboBox.editor.isEditable = false
      comboBox.items setTo when {
         limitedToType==null -> EFFECT_TYPES.filter { isNullable || it.type!=null }
         else -> listOf(EffectType(limitedToType), EffectType(null)).filter { isNullable || it.type!=null }
      }
      comboBox.value = comboBox.items.first()
      comboBox.valueProperty() attach {
         if (editable.value && it!=null)
            value.value = it.instantiate()
      }

      value sync { v ->
         comboBox.value = comboBox.items.find { it.type==v?.net { it::class } }
         editors.configure(v?.toConfigurableFx() ?: Configurable.EMPTY)
      }

      // readonly
      editable sync { comboBox.readOnly.value = !it }
      editable syncTo editors.editable
   }

   companion object: KLogging() {
      const val STYLECLASS = "effect-text-field"
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
      /** Effect type. Null represents no effect. */
      val type = type

      /** Human-readable effect name. */
      val name = type?.toUi() ?: textNoVal

      fun instantiate(): Effect? = runTry { type?.java?.getConstructor()?.newInstance() }
         .ifError { logger.error(it) { "Config could not instantiate effect" } }
         .orNull()
   }

}