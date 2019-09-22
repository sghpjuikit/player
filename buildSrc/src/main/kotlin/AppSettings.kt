
val appSetting = Setting.root {
   "General" {
      config("Close app")
      config("Developer mode")
      config("Manage VM options")
   }
   "Logging" {

   }
   "Ui" {
      "View" {
         config("Overlay area") {
            info = "Covered area. Screen overlay provides more space than window, but it can disrupt work flow."
         }
         config("Overlay background") {
            info = "Background image source."
         }
         "Action Viewer" {

         }
         "Shortcut Viewer" {

         }
      }
   }
}