package sp.it.util.file.json

interface JsConverter<T> {

   /** @return whether this converter can convert the specified value to json (by default true) */
   fun canConvert(value: T): Boolean = true
   /** @return json representing the specified value; can throw exceptions */
   fun toJson(value: T): JsValue
   /** @return value representing the specified json; can throw exceptions */
   fun fromJson(value: JsValue): T?

}