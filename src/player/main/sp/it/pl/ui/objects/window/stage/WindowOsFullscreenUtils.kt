package sp.it.pl.ui.objects.window.stage

import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.win32.W32APIOptions
import java.lang.Long.toHexString
import sp.it.pl.ui.objects.window.stage.Shell32Ex.QUERY_USER_NOTIFICATION_STATE.Companion.QUNS_ACCEPTS_NOTIFICATIONS
import sp.it.pl.ui.objects.window.stage.Shell32Ex.QUERY_USER_NOTIFICATION_STATE.Companion.QUNS_BUSY
import sp.it.pl.ui.objects.window.stage.Shell32Ex.QUERY_USER_NOTIFICATION_STATE.Companion.QUNS_QUIET_TIME
import sp.it.util.bool.Bool3
import sp.it.util.bool.UNKNOWN
import sp.it.util.dev.fail
import sp.it.util.system.Os

fun osHasWindowExclusiveFullScreen(): Bool3 {
   if (!Os.WINDOWS.isCurrent) return UNKNOWN

   val state = Memory(4)
   val result = Shell32Ex.INSTANCE.SHQueryUserNotificationState(state).toLong()
   if (result!=0L) fail { "SHQueryUserNotificationState error=${toHexString(result)}" } //
   val s = state.getInt(0)
   return Bool3(s!=0 && s!=QUNS_BUSY && s!=QUNS_ACCEPTS_NOTIFICATIONS && s!=QUNS_QUIET_TIME)
}

fun osHasWindowMaximized(): Bool3 {
   if (!Os.WINDOWS.isCurrent) return UNKNOWN

   val foregroundWindow = User32.INSTANCE.GetForegroundWindow() ?: return UNKNOWN
   val foregroundRectangle = WinDef.RECT()
   val desktopWindowRectangle = WinDef.RECT()
   User32.INSTANCE.GetWindowRect(foregroundWindow, foregroundRectangle)
   val desktopWindow = User32.INSTANCE.GetDesktopWindow() ?: return UNKNOWN
   User32.INSTANCE.GetWindowRect(desktopWindow, desktopWindowRectangle)
   return Bool3(foregroundRectangle.toString()==desktopWindowRectangle.toString())
}

@Suppress("ClassName", "FunctionName", "SpellCheckingInspection")
private interface Shell32Ex: User32 {

   /**
    * Checks the state of the computer for the current user to determine whether sending a notification is appropriate.
    * See https://learn.microsoft.com/en-us/windows/win32/api/shellapi/nf-shellapi-shqueryusernotificationstate
    *
    * @return https://learn.microsoft.com/en-us/windows/win32/seccrypto/common-hresult-values
    */
   fun SHQueryUserNotificationState(data: Pointer): WinNT.HRESULT

   companion object {
      val INSTANCE = Native.load("shell32", Shell32Ex::class.java, W32APIOptions.DEFAULT_OPTIONS)!!
   }

   /**
    * Specifies the state of the machine for the current user in relation to the propriety of sending a notification. Used by [Shell32Ex.SHQueryUserNotificationState].
    * See https://learn.microsoft.com/en-us/windows/win32/api/shellapi/ne-shellapi-query_user_notification_state
    */
   interface QUERY_USER_NOTIFICATION_STATE {
      companion object {

         /** A screen saver is displayed, the machine is locked, or a nonactive Fast User Switching session is in progress. */
         const val QUNS_NOT_PRESENT = 1

         /**
          * A full-screen application is running or Presentation Settings are applied.
          * Presentation Settings allow a user to put their machine into a state fit for an uninterrupted presentation,
          * such as a set of PowerPoint slides, with a single click.
          */
         const val QUNS_BUSY = 2

         /** A full-screen (exclusive mode) Direct3D application is running. */
         const val QUNS_RUNNING_D3D_FULL_SCREEN = 3

         /** The user has activated Windows presentation settings to block notifications and pop-up messages. */
         const val QUNS_PRESENTATION_MODE = 4

         /** None of the other states are found, notifications can be freely sent. */
         const val QUNS_ACCEPTS_NOTIFICATIONS = 5

         /**
          * Introduced in Windows 7. The current user is in "quiet time",
          * which is the first hour after a new user logs into his or her account for the first time.
          * During this time, most notifications should not be sent or shown.
          * This lets a user become accustomed to a new computer system without those distractions.
          * Quiet time also occurs for each user after an operating system upgrade or clean installation.
          */
         const val QUNS_QUIET_TIME = 6

         /** Introduced in Windows 8. A Windows Store app is running. */
         const val QUNS_APP = 7

      }
   }
}