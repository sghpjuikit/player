package sp.it.pl.main

import sp.it.pl.ui.pane.ActionData.Threading.BLOCK
import sp.it.pl.ui.pane.action
import sp.it.util.dev.fail

/** Denotes actions for 'App.Test...' */
object AppTest

/** Denotes actions for [AppTest] */
object AppActionsAppTest {

   val init = action<App>("Test...", "Set of actions to test actions", IconMD.OPEN_IN_APP, constriction = { APP.developerMode.value }) { AppTest }

   val test_u_1 = action<AppTest>("Test () -> Unit", "", IconFA.PLAY) { }

   val test_u_1Ex = action<AppTest>("Test fail () -> Unit", "", IconFA.PLAY) { fail { "Failure" } }

   val test_u_2 = action<AppTest>("Test async () -> Unit", "", IconFA.PLAY, BLOCK) { Thread.sleep(5000) }

   val test_u_2Ex = action<AppTest>("Test async fail () -> Unit", "", IconFA.PLAY, BLOCK) { Thread.sleep(5000); fail { "Failure" } }

   val test_i_1 = action<AppTest>("Test () -> Int", "", IconFA.PLAY) { 5 }

   val test_i_1Ex = action<AppTest>("Test fail () -> Int", "", IconFA.PLAY) { fail { "Failure" } }

   val test_i_2 = action<AppTest>("Test async () -> Int", "", IconFA.PLAY, BLOCK) { Thread.sleep(5000); 5 }

   val test_i_2Ex = action<AppTest>("Test async fail () -> Int", "", IconFA.PLAY, BLOCK) { Thread.sleep(5000); fail { "Failure" } }

}