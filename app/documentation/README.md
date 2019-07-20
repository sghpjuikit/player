# PlayerFX User Guide

- [Installation bundle](#installation-bundle)
- [Installation](#installation)
- [Uninstallation](#uninstallation)
- [Privacy](#privacy-&-security)
- [Disclaimer](#disclaimer)
- [Licence](#licence)
- [Usage](#usage)
- [Reporting problems](#reporting-problems)

## Installation bundle

- Is `VLC` be bundled?
No. It will be auto-discovered if proper version is installed on the operating system. Otherwise user has to set it up.

- Does application without `VLC` set up support any playback?
No. It could (using its internal `javafx.media` playback implementation), but at this point this feature is old and costly to support. Perhaps in the future.

- Are (`Java`) libs bundled?
Yes. They have to be.

- Is `Java` bundled?
No. Perhaps in the future. Reason: it is too big for release bundle and I have no time to investigate licencing and `JDK` vs `JRE` stuff (the application needs `JDK` to run).

- Is `javac` bundled?
No. But it is bundled with `JDK`, so practically yes.

- Is `Kotlin` bundled?
Yes. It is just a lib.

- Is `kotlinc` bundled?
No. But it must be present, so we include a discovery/download mechanism to avoid bundling. There will be an `use experimental/native kotlinc` option, although right now `ekotlinc` has a bug and can not be used for our needs.

- Are plugins bundled?
Yes. Because no user supplied plugin support is implemented yet, all plugins are provided internally in the `Player.jar`. In the future, some of them will become external/user modifiable.

- Are widgets bundled?
Yes. All are external/user modifiable. The application has proper API and UX to handle if user removes a widget (or all of them). Of course some are essential for proper functionality (e.g., `Playback`, `Playlist`).

- Are templates be bundled?
Yes. These are called initial templates. Some are in the `app/templates` directory, pure container types are provided internally in `Player.jar`.

- What launchers are bundled?
`.jar`, `.exe`, `.sh`. Maybe `.bat` in the future. All provide the same functionality.

And with that:

- Is application be usable once user downloads it?
No, `JDK` is missing so it will not start at all. `VLC` is missing so playback will not be possible.

- Is this application be portable?
Yes, on all platforms. See _Privacy_ for details

- Is there an installation wizzard to avoid extra installation steps?
No. Perhaps in the future.

- Does the application provide any file associations or such settings?
No. Use your operating system to associate files with this application.

## Installation

User installation steps necessary:
- download an application release from [Github releases](https://github.com/sghpjuikit/player/releases)
- set up `JDK`  
  Make `app/java` directory contain or link to `JDK` 11 or 12
- set up latest 64-bit `VLC` using one of:
  - make `app/vlc` contain/link to (optionally portable) installation of `VLC`   
    This is the recommended way, as the application does not depend on external `VLC` version or location
  - let application discover your `VLC` automatically   
    This requires `VLC` to be installed.   
    Note that updating or changing this `VLC` may interfere with this application
  - add `VLC` location in application settings in `Settings > Playback > Vlc player locations`
    This is recommended if you do have `VLC` available, but not installed or integrated in your system
    Note that updating or changing this `VLC` may interfere with this application
  
## Uninstallation

- remove application directory

## Privacy & Security

TLDR: This application tries to be as private/secure as possible and is certainly less invasive then majority of
apps you already use. But it may not run with 0 trace and as everything else may be exploited. Long version:

The application is developed as to follow the certain privacy/security policies as well as complete transparency about 
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
  These logs can be used to trace user activity (to an extend depending on file log level settings), but only user can
  share the log files, as application never reads them.
  
  This may not apply to the underlying `VLC` player doing the actual playback and this application is unaware and does 
  not attempt to prevent it from such behavior.
  
- does not use any website or cloud storage
- does not execute any malicious behavior unless exploited by a 3rd party.
 
  The application could run malicious widget/plugin if user or 3rd party provides it. The application automatically 
  compiles and loads class file provided in the `app/widgets` directory and currently this behavior can not be disabled.
  
- is portable, hence
  - writes all files produced during its running in `app/user` directory
  - can be safely moved between computers with all settings
  - does not inject or create any file in system files, system temporary directory or system user directory or anywhere or that matter
  - does not require installation
  - can be safely 'uninstalled' and all its user data by deleting the application directory

Due to the use of many 3rd party libraries and technologies, it may be difficult to provide 100% guarantee of these
policies. If any of them are dicovered to be broken, please contact the developers immediately.


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

### Reducing memory usage

There are several application usage strategies for minimizing memory consumption

#### Avoid many open widgets

More open widgets will consumer more memory.

#### Manual widget load

Widget loading can be:
- AUTOMATIC, which means the widget instance loads with the layout
- MANUAL, which postpones widget instance loading until user clicks on the widget. Until then the widget does not function and displays a placeholder ui.

Manually loaded widgets make it possible for relatively large number of widgets to exist in the layout with minor overhead. This technique is suitable for those widgets user does not need to be readily available.

Note that widget inputs and outputs do not exist until widget instance is loaded, so if other widgets link to not yet loaded widget, the connection links will not receive any input. Linking will be delayed until the widget loads.

Widget instance loading can be changed
- using the icon in the widget menu displayed in layout mode
- in widget instance settings

#### Export widgets

Alternative to manual loading is exporting component into the file. This technique is suitable for those widgets user does not need to use often or as part of standard application use.

Pros:
- any part of the layout can be exported, with multiple widgets or even entire window layout
- widget can be exported with actual or default settings

Cons:
- the exported widget will not appear in the layout

#### Launch widgets in new process

Widget or exported component can be launched in new independent process as another application instance. This technique is suitable for those widgets user wishes to run as standalone applications.

Pros:
- independent memory
- independent lifecycle (when one application closes, the other keeps running)

Cons:
- the two application instances can not operate with one another seamlessly as if they were single application

#### Avoid many running plugins

More running plugins will consumer more memory.

## Reporting problems

When application detects problem it will display a message notifying the user along with all the details and logs.

Problems, feedback or requests can be provided on the application github page, which can also be opened through the application. Search for `Github` in the application search.

The logs can be found in the application `app/user/log` directory.