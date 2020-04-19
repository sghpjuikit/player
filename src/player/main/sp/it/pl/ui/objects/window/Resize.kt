package sp.it.pl.ui.objects.window

import javafx.scene.Cursor

enum class Resize(val cursor: Cursor) {
   N(Cursor.N_RESIZE),
   NE(Cursor.NE_RESIZE),
   E(Cursor.E_RESIZE),
   SE(Cursor.SE_RESIZE),
   S(Cursor.S_RESIZE),
   SW(Cursor.SW_RESIZE),
   W(Cursor.W_RESIZE),
   NW(Cursor.NW_RESIZE),
   ALL(Cursor.MOVE),
   NONE(Cursor.NONE)
}