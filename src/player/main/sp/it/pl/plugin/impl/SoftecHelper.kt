package sp.it.pl.plugin.impl

import sp.it.pl.plugin.PluginBase
import sp.it.pl.plugin.PluginInfo
import sp.it.util.action.IsAction
import sp.it.util.system.browse
import sp.it.util.system.edit
import java.io.File
import java.net.URI

class SoftecHelper: PluginBase() {

   @IsAction(name = "Open SPLAW")
   fun a1() = URI("https://bobcat.softec.sk/splaw/app").browse()
   @IsAction(name = "Open Dochadzka")
   fun a2() = URI("https://jportal.softec.sk/dochadzka/pages/main/Dochadzka.jsf").browse()
   @IsAction(name = "Open dcos (iszi)")
   fun a3a() = URI("https://iszi.dcos.softec.sk").browse()
   @IsAction(name = "Open dcos (bigbee)")
   fun a3b() = URI("https://bigbee.dcos.softec.sk").browse()
   @IsAction(name = "Open leader/mezos")
   fun a4() = URI("https://leader.mesos.softec.sk").browse()
   @IsAction(name = "Open NCZI issues")
   fun a5() = URI("https://gitlabee.softec.sk/groups/nczi/-/boards").browse()
   @IsAction(name = "Open Products issues")
   fun a6() = URI("https://gitlabee.softec.sk/groups/produkty/-/boards").browse()

   @IsAction(name = "Edit hosts file")
   fun a7() = File("C:\\Windows\\System32\\drivers\\etc\\hosts").edit()
   @IsAction(name = "Open hosts file location")
   fun a8() = File("C:\\Windows\\System32\\drivers\\etc\\hosts").browse()

   companion object: PluginInfo {
      override val name = "NCZI Helper"
      override val description = "Provides the actions to help with Softec work."
      override val isSupported = true
      override val isSingleton = false
      override val isEnabledByDefault = false
   }
}
