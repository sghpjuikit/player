package sp.it.util.conf

import sp.it.util.conf.Config.ConfigBase

/** Simple [sp.it.util.conf.Config] wrapping a value. */
class ValueConfig<V>: ConfigBase<V> {

   private var value: V? = null

   constructor(type: Class<V>, name: String, gui_name: String, value: V, category: String, info: String, editable: EditMode): super(type, name, gui_name, value, category, info, editable) {
      this.value = value
   }

   constructor(type: Class<V>, name: String, value: V): super(type, name, name, value, "", "", EditMode.USER) {
      this.value = value
   }

   constructor(type: Class<V>, name: String, value: V, info: String): super(type, name, name, value, "", info, EditMode.USER) {
      this.value = value
   }

   override fun getValue(): V? {
      return value
   }

   override fun setValue(v: V?) {
      value = v
   }

}