# Spit Player User Guide

- [Installation bundle](#installation-bundle)
- [Installation](#installation)
- [Uninstallation](#uninstallation)
- [Starting](#starting)
- [Stopping](#stopping)
- [Privacy](#privacy-&-security)
- [Disclaimer](#disclaimer)
- [Licence](#licence)
- [Usage](#usage)
- [Reporting problems](#reporting-problems)

## Installation bundle

- Is `VLC` be bundled?  
No.  
It will be auto-discovered if proper version is installed on the operating system.
Otherwise, user will be prompted to let the application setup `VLC` automatically or asked to do it on his own.

- Are (`Java`) libs bundled?  
Yes.
They have to be.

- Is `Java` bundled?  
No.  
Perhaps in the future.
Right now there are uncertainties regarding bundle size and licencing.

- Is `javac` bundled?  
No.  
But it is bundled with `JDK`, so practically yes.

- Is `Kotlin` bundled?  
Yes.  
It is just a lib.

- Is `kotlinc` bundled?  
No.  
It is automatically setup by the application.

- Are plugins bundled?  
Yes.  
Because no user supplied plugin support is not implemented yet, all plugins are provided internally in the `Player.jar`.
In the future, some of them will become external/user modifiable.

- Are widgets bundled?  
Yes.  
All are external/user modifiable.
The application has proper API and UX to handle if user removes a widget (or all of them).
Of course some are essential for proper functionality (e.g., `Playback`, `Playlist`).

- Are templates be bundled?  
Yes.  
These are called initial templates. Some are in the `app/templates` directory, some are bundled internally in `Player.jar`.

- What launchers are bundled?  
`.jar`, console `.exe`, gui `.exe`, `.sh`. All provide the same functionality.

And with that:

- Is application be usable once user downloads it?  
No, `JDK` is missing, so it will not start at all. `VLC` is missing so playback will not be possible.

- Is this application be portable?
Yes, on all platforms. See [Privacy](#privacy-&-security) for details

- Is there an installation wizard to avoid extra installation steps?
No. Perhaps in the future.

- Does the application provide any file associations or such settings?
No. Use your operating system to associate files with this application.

## Installation

User installation steps necessary:
- download an application release from [GitHub releases](https://github.com/sghpjuikit/player/releases)
- set up `JDK`  
  Make `app/java` directory contain or link to `JDK` 15
  
## Uninstallation

- remove application directory

## Starting

##### Windows

As gui application: open `./SpitPlayer.exe` file   
As console application: open `./SpitPlayerc.exe` file

The difference between `./SpitPlayer.exe` and `./SpitPlayerc.exe` is that
- `./SpitPlayer.exe` launched from terminal/commandline does not wait for process end and shows no output (stdout/stderr).   
  Recommended using for starting the application normally, from file explorer or by shortcut.
- `./SpitPlayerc.exe` launched from outside terminal will open new commandline window
  Recommended using for starting the application from terminal, particularly to see its output or use its commands like help.

From terminal (using bash):
- `cd /drive-letter/path/to/application/directory`
- `./SpitPlayerc.exe` to launch the application as console application
- for options and commands see help to use `./SpitPlayerc -h` or `./SpitPlayerc --help`

##### Linux

From terminal:
- `cd /path/to/application/directory`
- `chmod +x SpitPlayer.sh` to make the launcher executable
- `./SpitPlayer.sh` to launch the application
- for options and commands see help to use `./SpitPlayer -h` or `./SpitPlayer --help`

## Stopping

The application stops running when the main window is closed.

The main window has a designated icon in its header. Using this icon, it is possible to make any window main window. There is always exactly one main window at any time.

When the application is stopped, necessary settings and application state (like all windows and their content) is saved, so it can be restored on the next application start.

## Privacy & Security

This application tries to be as private/secure as possible within reason and is certainly less invasive than the majority of
apps you already use. But it may not run with 0 trace and, as everything else, may be exploited.

The application is developed as to follow certain privacy/security policies as well as complete transparency about 
them.

The application:
- does not write to registry
- does not connect or integrate to any service (unless such functionality is provided and enabled by the user as 
  plugin/widget) and never sends any data outside the computer through network or any other means
  
  This may not apply to the underlying `VLC` player doing the actual playback, however the application attempts to
  prevent it from doing so by passing `--no-metadata-network-access` option to it.
  
  This application does open a network port for the sole reason of communicating among its app instances on the same 
  machine (`Java RMI`).
- does not collect any data (to help improve the UX or any other nonsense like that) except generating standard logs.
  These logs may be essential for bug reporting, should a user open an issue. These (if accessed) can be used to trace user activity (to an extent, and depending on logging settings), but only user can share the log files, as application never reads them.
  
  This may not apply to the underlying `VLC` player doing the actual playback and this application is unaware and does 
  not attempt to prevent it from its own logging.
  
- does not use any website or cloud storage
- does not execute any malicious behavior unless exploited by a 3rd party.
 
  The application could run malicious widget/plugin (such as keylogger) if user or 3rd party provides it. The application automatically 
  compiles and loads widget class files provided in the `app/widgets` directory and currently this behavior can not be disabled.
  
- is portable, hence
  - writes all files produced during its running in `app/user` directory
  - can be safely moved between computers with all settings
  - does not inject or create any file in system files, system temporary directory or system user directory or anywhere for that matter
  - does not require installation
  - can be safely 'uninstalled' and all its user data by deleting the application directory

Due to the use of many 3rd party libraries and technologies, it may be difficult to provide 100% guarantee of these
policies. If any of them are discovered to be broken, please contact the developers immediately.


## Disclaimer

Application developers/organization providing the source code for the official release (publicly available on `Github`) are not
responsible for any damages/problems caused by running this application in any way, which also includes loss of user data or resulting hardware malfunction as 
well as any damages/problems caused by using this application with other 3rd party software, which also includes widgets/plugins not bundled
in the release or VLC. Application developers/organization are also not responsible for failing to adhere to the outlined
privacy/security policies.

## Licence

Free distribution for personal use is permitted.
Commercial use or use intended for illegal or malicious purposes is forbidden.

## Usage

### Playback

The application supports only single playback.

### Playlists

The application supports multiple playlists. These can be open and managed (used by widgets) simultaneously.
However, at most one playlist can be *active*, i.e. be source of playback.

Widgets that implements `PlaylistFeature` (like the provided `Playlist` widget) can be bound to a (single) playlist.
It is up to the widget when and how it creates and disposes of the playlist.

Playlists that are not bound to a widget are called *dangling*.
Dangling playlists may still be used and bound to widgets, but when application closes, any dangling playlist will be lost.

`Playlist` widget will upon loading attempt to initialize playlist in order:
- look for playlist it was bound to before
- look for active (playing) dangling playlist
- look for any dangling playlist
- create new empty playlist

This makes it possible to maintain playback even when `Playlist` widget is not open and also re-open the playlist if possible. 

### Reducing memory usage

There are several application usage strategies for minimizing memory consumption

#### Avoid many open widgets

More open widgets will consumer more memory.

#### Manual widget load

Widget loading can be:
- AUTOMATIC, which means the widget instance loads with the layout
- MANUAL, which postpones widget instance loading until user clicks on the widget. Until then the widget does not function and displays a placeholder ui.

Manually loaded widgets make it possible for relatively large number of widgets to exist in the layout with minor overhead. This technique is suitable for those widgets, that user does not need to be readily available.

Note that widget inputs and outputs do not exist until widget instance is loaded, so if other widgets link to not yet loaded widget, the connection links will not receive any input. Linking will be delayed until the widget loads.

Widget instance loading can be changed
- using the icon in the widget menu displayed in layout mode
- in widget instance settings

#### Export widgets

Alternative to manual loading is exporting component into the file. This technique is suitable for those widgets, that user does not need to use often or as part of standard application use.

Pros:
- any part of the layout can be exported, with multiple widgets or even entire window layout
- widget can be exported with actual or default settings

Cons:
- the exported widget will not appear in the layout

#### Launch widgets in new process

Widget or exported component can be launched in new independent process as another application instance. This technique is suitable for those widgets, that user wishes to run as standalone applications.

Pros:
- independent memory
- independent lifecycle (when one application closes, the other keeps running)

Cons:
- the two application instances can not operate with one another seamlessly as if they were single application

#### Avoid many running plugins

More running plugins will consumer more memory.

## Reporting problems

When application detects problem it will display a message notifying the user along with all the details and logs.

Problems, feedback or requests can be provided on the application GitHub page, which can also be opened through the application. Search for `GitHub` in the application search.

The logs can be found in the application `app/user/log` directory.