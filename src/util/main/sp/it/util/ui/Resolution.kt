package sp.it.util.ui

@Suppress("EnumEntryName")
enum class Resolution(@JvmField val width: Double, @JvmField val height: Double) {
   R_1x1(1.0, 1.0),
   R_3x2(3.0, 2.0),
   R_4x5(4.0, 5.0),
   R_16x9(16.0, 9.0),
   R_16x10(16.0, 10.0),
   R_1024x768(1024.0, 768.0),
   R_1600x1200(1600.0, 1200.0),
   R_1920x1080(1920.0, 1080.0),
   R_1920x1200(1920.0, 1200.0),
   R_2560x1440(2560.0, 1440.0),
   R_3840x2160(3840.0, 2160.0);

   val ratio: Double = width/height
}