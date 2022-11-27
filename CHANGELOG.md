# Changelog
All notable changes to this project will be documented in this file. Format based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Latest]

- Update dependencies
- Implement preferred song order settings
- Implement `AppSearch` history to remember searched text instead of picked suggestion
- Implement `AppSearch` number conversions
- Implement `AppSearch` operating system actions
- Implement **Color interpolation** widget
- Implement **Uuid Generator** widget
- Implement **FavLocations** widget selection persistence
- Implement **GitProjects** widget follow Markdown link
- Implement **CommandBar** widget auto-close
- Implement **CommandGrid** widget auto-close
- Implement **Image** widget image operations (move & mirror - mouse & keys)
- Implement krita image dimension read support
- Implement better and optimized pdf cover support
- Implement showing persistent component name in layout mode
- Implement better `ContainerSwitchUi` tab loading UX
- Implement reactive window header content
- Implement window transparency click-through settings
- Implement json conversion generic type inference
- Implement less intrusive `Widget` focus styling
- Implement less intrusive `ContainerBi` styling
- Implement less intrusive `ComboBox` styling
- Implement styleable pop-window edge gap
- Implement `TextIcon` css support
- Implement better `OrCE` overridable config editor UX
- Implement better `ComplexCE` hierarchical auto-completable editor UX
- Implement mime extension unsealed enumerator [provides user with file extension autocomplete]
- Implement better styling [leverage blur effects]
- Implement consistent settings editors layout
- Implement better io layer output name
- Implement load/animation stutter prevention using delay load delay
- Implement load/animation stutter prevention using image loading throttling
- Fix `SpitScrollBarSkin` drag animation interaction
- Fix popup window interaction not working after widget loads in popup
- Fix `Open widget` action interfering with overlay animation
- Fix **Tagger** widget slow reading when song tag contains image 
- Fix `Configurable` hierarchy computation [affected settings hierarchy]
- Fix `Config` constraints not applied on children sometimes
- Fix `File.deleteRecursivelyOrThrow` not working correctly
- Fix `HttpClient.downloadFile` timing out
- Fix **Game View** widget not laying out placeholders sometimes
- Fix **Game View** widget slow layout performance
- Fix **Song Group Table** widget's selection output not being set
- Fix **Function View** widget precision stuck to 7
- Fix **Function View** widget slow performance
- Fix application search being affected by spaces
- Fix custom config initial value for cases when config is created later
- Fix App Search layout sometimes broken
- Fix song notifications not updating when playback changes
- Fix slow file table/grid performance
  - Use more `cmd dir`
  - Use more attribute value caching
  - Use image metadata for file creation time only if user specifically requests [improves standard sort by creation time considerably]
- Fix various styling & layout issues

### Window - reactive header content
Window header content shrinks if window is too small.
This improves UX as well as resizing issue for small windows.

### Window - overriding settings
Some window settings are now properly overridable, i.e., per-application and per-window setting is provided.
Settings also properly recognize default values.

### Window - transparency click-through
Sometimes click-through behavior is unwanted for transparent windows (click-through is enabled for fully transparent pixels).
For now transparent windows were always click-through (unless focused) 
From now on, user specify click-through (as before) or normal (no click-through) transparency for each window.

### Window - glass effect
User can now enable overridable window bgr effects (none | blur | acrylic).
The effect works on any window, which includes dock windows, sliding windows as well as popups, menus or tooltips.
Works only on Windows 10 OS.

### Window - loading
Opening windows is usually expensive operation.
This update brings delayed content loading for sliding windows to allow the animation to run smoothly.
Loading images during loading sliding window or StartScreen overlay is temporarily paused, for the same benefit.
In the future, these optimizations may be provided more implicitly for any content that loads widgets.

## Styling simplifications
- For disabled opacity, use `-fx-disabled-opacity` value instead of direct value.
- Window pseudoclasses `focused`, `window-focused`, `pop-window-focused` have been substituted with `focus-within`.
- Window pseudoclass `transparent-allowed` has been removed. Simply use `transparent` or `transparent-ct`
- SKins can now use `transparent-nct` color which is transparent, but not click-through color, as opposed to `transparent`, which is click-through

## [7.5.0] 2022 11 04

- Update dependencies
- Implement support for arbitrary widget package
- Implement JDK19 virtual thread leveraging asynchronous operations across application
- Implement volume drive add/remove detection
- Implement better empty lyrics text
- Implement better styling for some readonly elements
- Implement better grid-cell styling when cells occupy full width
- Implement **Playback knobs** song chapter creation support
- Implement lossless WebP image support
- Implement animated WebP image support
- Implement faster image loading
- Implement faster image cache
- Fix image cache not being used after widget is loaded again
- Fix text area menu select all shortcut
- Fix text area menu allowing some edits when text area is not editable
- Fix config editor caret button hiding when menu is open
- Fix config editor caret button menu not showing for read-only editors
- Fix config editor caret button menu not showing submenu menu for editor value
- Fix `File.recycle()` not providing error
- Fix **ObjectInfo** image metadata parsing causing no output
- Fix **CommandGrid** UX [disable tooltips & drag&drop]
- Fix **LibraryView** sorting by VALUE not working
- Fix `GridFileIconCell` icon layout in some cases
- Fix grid file deletion not restoring selection properly
- Fix grid cell not showing image in rare cases
- Fix grid cell styling when cell spans entire width
- Fix grid cell font size when cell spans entire width
- Fix grid cell graphics gap too narrow
- Fix tree cell icon too small
- Fix tree cell graphics gap too narrow
- Fix slider editor ticks & snapping
- Fix slider editor value formatting for Double.MIN/MAX
- Fix toggle-button hover styling
- Fix image loading not closing stream sometimes
- Fix image thumbnail not setting correct source file sometimes
- Fix widget controls sometimes showing up when drag is active
- Fix wallpaper plugin requiring application to be running to show the wallpaper
- Fix wallpaper plugin not rendering wallpaper properly in rare cases (e.g. switching screen resolution)
- Fix playback seeker chapter create cancel not removing chapter properly
- Fix playback seeker chapter ESCAPE not closing chapter popup properly
- Fix playback seeker icon layout issues
- Fix **PlayerControls** widget layout issues in popup

### JDK19
The project now uses and requires **JDK 19**.
This is to enable virtual threads, now used throughout the application.
This achieves optimal CPU utilization, removing UI stutter during CPU overload (where used).

### Loading images
Loading images has been drastically improved.

### Loading images - virtual threads
This update brings virtual threads for loading images. Full details [here](https://github.com/haraldk/TwelveMonkeys/issues/706).
This achieves optimal CPU utilization, improving parallel image loading speed and removing UI stutter in all but most demanding situations.

### Loading images - imageIO
[ImageIo](https://github.com/haraldk/TwelveMonkeys) was updated to 3.9.0.
This brings in support for lossless WebP images, some fixes and optimizations.
Loading images using this library has become much faster due to disabling disk caching `ImageIO.setCaches(false)` and
the library implementing new buffered image streams to optimize performance. For details see
* https://github.com/haraldk/TwelveMonkeys/issues/687
* https://github.com/haraldk/TwelveMonkeys/issues/691
* https://github.com/haraldk/TwelveMonkeys/issues/704

### Loading images - cache
Image cache is partitioned by a key. For **DirView** widget, this is the widget's `UUID`.
However, during widget loading, the id may change and the widget generates new cache.
This is now fixed by the widget instance generating its own cache id, which never changes.
In the future, the id may be exposed to the user in widget settings.

Image cache now caches images in efficient binary format, which means no encoding/decoding cost during serializing/deserializing.
This makes the image cache blazing fast (loads images instantly on SSD).

Image cache now caches images in the exactly requested size.
This means the least CPU work and memory is used when loading the cached image.

### Loading images - clipping
Images now avoid loading parts that will not be displayed (when aspect ratio of the thumbnail is different).
Image loading also takes into effect if the image is going to touch thumbnail from INSIDE or OUTSIDE.
This optimizes loading speed for images that have eccentric sizes.

### Loading images - interruption
Image loading now employs much better image loading interruption - the interruption is now predictable and very quick  
Loading no longer necessary images now does not delay loading speed of images.

### Loading images - animation
Animated images now load animation frames only when animation is requested.
This makes loading animated images much faster as well as compatible with image caching.
Animation now supports webp images.

### Wallpaper plugin
The plugin now sets wallpaper using Windows API instead of creating a special wallpaper windows.
This fixes several issues with the functionality
The wallpaper image is now cached with the new cache - this improves performance of the overlay window.

### Widget compilation
The widgets can now declare arbitrary package.
For Java widgets it is recommended to mirror package declarations by directory structure.

An optimization was attempted to compile widgets with **Gradle** daemon.
This did not result in faster widget compilation, so this change was reverted.

## [7.4.0] 2022 10 09

- Update **Kotlin** to 1.7.20
- Update dependencies
- Implement file download task progress
- Implement faster config de/serialization [memoize fallback converters by type]
- Implement `SongInfo` cover fit from OUTSIDE by default
- Implement `Markdown` link auto-detection
- Improve **Hue Scenes** widget [use coroutines]
- Improve **Settings** widget's UX [remove HOME icon & set to not reusable]
- Improve styling [TagTextField with plus does not emulate TextField]
- Fix object to ui string algorithm for strings of medium length
- Fix json serializing non-component properties for data classes
- Fix estimating object type for ui failing for classes with generic type parameters  
- Fix `Optional` and `Option` and `Try` ui string representation to be more readable
- Fix `Option.toString` to be consistent with `Try.toString`
- Fix config deserialization for `Action`s
- Fix config deserialization error message
- Fix Kotlin version detection not detecting RC versions
- Fix popup showing without focus even if focused
- Fix widget compilation issues [avoid using K2 compiler due to not being production ready]
- Fix widgets compiling multiple times due to temporary file change events
- Fix config editors entire height not scrollable
- Fix coroutines launched from widgets not cancelling on `Widget.close`

This update continues UX improvements from previous updates.

### Application Settings reuse
The **Settings** widget used for application settings can be used for any kind of `Configurable` object.
This creates confusion when such object requires a **Settings** widget, because widgets are by default reused and thus the contents change unexpectedly.
This updates changes the behavior of `Show Settings` action to always create new widget and set its re-usability to false.
Configuring other objects will therefore not use the widget, if it shows application settings.

This allows removing the widget's `Home` icon, which served to show application settings.
This was confusing at best.
Now using the **Settings** widget is simpler and more intuitive.

### Coroutines in Hue Scenes widget
**Hue Scenes** widget does a lot of thread switching when dealing with hue bridge configuration.
This is because it needs to update ui state on ui thread in every step of the hue bridge communication process.
The widget has been improved to use coroutines effectively.
For this, there is new API `runSuspendingFx`for easily launching coroutines from UI.

Kotlin's coroutines really shine here. None of the blocking code is due to heavy computations.
As such, CPU dispatcher is not needed. The blocking due to IO (http) is taken care of by **Ktor**'s suspending methods.
As such, no explicit thread switching is necessary, we simply use suspending methods where ui would be blocked before and rewrite the code from Future API to imperative style inside a coroutine on FX dispatcher.
Writing asynchronous UI code with coroutines is a simple matter - put away blocking behind suspending functions and forget about threads forever.
In code, it looks like we simply call blocking functions right from UI. The coroutine transparently dispatches off of UI thread where necessary.

`SimpleController` now provides coroutine scope that is cancelled on close.

### Tasks
The task list accessible from window header has been improved to show progress bar when task reports progress.
All file downloading in the application has been reimplemented to use coroutines and report progress.
This includes drag & drop for url with image, setting up kotlin compiler, ffmpeg and vlc player.

This lead to improvements in some areas.
- the `Fut<T>` is now covariant - `Fut<Int>` is subtype of `Fut<Number>`.
- the `Fut` API taking executor parameter defaults to `CURR` (current thread executor) and does so consistently, removing the previously used `CompletableFuture<Any>().defaultExecutor()` completely.
- file downloading is reimplemented using non-blocking coroutines
- the progress reporting uses Kotlin's `Flow` and uses `Flow.conflate` and `awaitPulse()` to report actual progress on each ui pulse, not overloading the ui.

### Project IDE Intellij Idea Settings
The intentions and dictionary are now added to git repository so developers leverage from the intended settings.

## [7.3.0] 2022 09 25

- Update dependencies
- Implement context menu accelerators properly
- Implement `F11/F12` for window fullscreen toggle
- Implement `Number` functors toBin, toOct, toHex
- Implement `Charset` editor to list all available charsets and to support null
- Fix widget launching from overlay closing immediately
- Fix widget creation does not respect user's preferred widget loading strategy
- Fix demo compilation [project build now works properly]
- Fix functor splitting to graphemes returning sequence
- Fix Image.url is `null` in some cases [affected contents of thumbnail context menu]
- Fix null handling and group apply for actions with receiver of `Any?`
- Fix introspection failing for anonymous classes

### Accelerators
Context menu items with shortcuts display the key combination in the menu.
Now the text is correctly displayed on the right side instead of being part of the text.
Mnemonics continue being disabled.

### Widget launching
In some cases creating widgets used hardcoded POPUP strategy for opening widgets.
Now user's preferred loading strategy (defined per widget) will be used instead.

### Window owner handling
Window owner feature allows creating window hierarchies.
Closing window closes all its children. Children windows are always on top of their parents.

This caused problem with overlay. If an action created new widget in a popup while overlay was active, it was set as an owner.
Closing the overlay (automatically) when the action finished therefore also closed the widget itself.
It is difficult to determine when this behavior is desired and when it is not, hence from now on, widget popup will not have owners set.

### Coroutines & Loom (Java virtual threads)
For now, for async code, the `Future`-based `Fut` API has been used thorough application.
It runs on `Executor`s and requires using fluent API style to switch between threads.

Kotlin provides coroutines feature, which allows writing imperative non-blocking style.
This is an analogue to the `Fut` API, employed thorough the application.

This update starts coroutine adoption by the project.
There are some new convenience methods for `Flow` or brides from `Fut` or `Subscription`.
From now on, coroutines will be used where appropriate.

Recently Java released virtual threads (preview).
This project usually runs on **IBM**'s **OpenJ9** instead of **Oracle**'s **Hotspot** JDK, so the feature will take longer to test.

Virtual threads could be used for coroutine dispatchers to
- avoid the need for `Dispatchers.IO` that contains blocked threads, thus reducing threads and resources
- manual thread switching to avoid blocking inside coroutine - on virtual thread, coroutine will never block - thus reducing code complexity

In practice though, both coroutine dispatchers and fut executors have been misused in this project.
The workload on main FX thread is properly switched to non-ui threads, but switching to IO thread during blocking has not been employed at all.
For desktop application, this is really not a problem at all, however during certain workloads this can be important, for example image loading.

### Coroutines & Loom - loading images
When loading an image, there is mixed CPU and IO workload being interspersed. Switching between CPU and IO thread involves a lot of head-scratching. 
Instead, images are loaded on practically unbounded pool, hoping not to overload CPU.
With virtual threads, the workload will automatically release threads when blocking, allowing us to use small-sized CPU pool that will be able to
properly saturate CPU without overloading it at any point.
This will finally remove the Thread.sleep() used for giving UI breathing space and simplify grid image loading code.
The loading will maintain maximum possible performance with 0 stutter and no management at all.

## [7.2.0] 2022 09 18

- Update dependencies
- Implement user configurable cover disk cache for grid file covers
- Implement `ActionData` receiver editor support for collections
- Implement & apply better algorithm computing icons for files
- Implement better `Form` layout & warning UX [use warn icon instead of text]
- Implement faster **AlbumView**, **DirView**, **GameView**, **AppLauncher** cover loading
- Implement smoother **AlbumView**, **DirView**, **GameView**, **AppLauncher** scrolling [no UI stutter] 
- Implement better **AlbumView**, **DirView**, **GameView**, **AppLauncher** grid cell style [animations, hover bold text, text padding]
- Implement **AlbumView** grid cell thumbnail inside/outside fit settings
- Implement faster **DirView** sorting [use memoized comparator]
- Fix `Component` actions not registered
- Fix pdf cover not releasing resources
- Fix hotkey consuming for `BACKSPACE`
- Fix `toConfigurableFX` not producing proper readOnly state
- Fix `GridFileIconCell` layout in certain cases
- Fix `Config.configure` popup closing immediately if it opens from another `Config.configure` popup
- Fix sliding window focus not working properly

This update continues UX improvements from previous updates.

### Actions
Action execution from application search now correctly handles actions that can consume multiple inputs.
Actions consuming collections provide list editor, allowing user to provide arbitrary number of inputs.
These actions require at least one input.
The form correctly applies constraints on the elements of the list as well as the list itself.

### Form
One problematic aspect of `Form`s was the validation output.
Until now, it was displayed as text below the **OK** button.
This caused problems with layout due to window not resizing to accommodate the warning text.
This was reworked to use a warning icon with a tooltip.

### DirView
**DirView** widget, intended for building libraries, can also be used as impromptu file explorer.
This update fixes various issues with the `LIST` layout mode when thumbnails are not enabled.
There is also massive improvement in performance and UI smoothness. See below.

### Grid image loading
There are two aspects of image loading, both very important and with various contributing factors:
- Total speed
  - using parallel image loading
  - improving image loading speed
    - using cache
    - faster algorithms
      - loading only necessary size using subsampling (this may however require scaling to get good result)
      - adjusting the need for image scaling and reduce post-pressing
- UI stutter
  - CPU overload
  - UI blocking
  - UI overload
    - improving grid layout
    - improving grid cell updating
    - using grid cell ui caching
    - using canvas

#### DirView - grid optimization - parallelism
The grid already uses parallel image loading.

#### DirView - grid optimization - algorithm
Faster algorithm is generally not possible - there are couple possible routes here, but for now, this is considered out of scope.

#### DirView - grid optimization - algorithm subsampling
Images are already loaded using subsampling, which helps a lot. Scaling is performed and may be adjusted for performance in the future.

#### DirView - grid optimization - image cache
When the widget is used as a library, it tends to display more or less finite set of items.
Loading of item thumbnails can be improved in this case by caching the images (of thumbnail size).
From now on, user can enable disk cache. This noticeably improves image loading speed.

#### DirView - grid optimization - CPU overload
So far, CPU overload was not identified as a cause for concern when loading lots of images.

#### DirView - grid optimization - UI blocking - volatile
The way to prevent stutter normally is to never call blocking operation on UI thread.
**DirView** did accidentally call expensive disk operation on UI thread.
This has been fixed, and it got rid of stutter that would appear shortly after opening the widget.

#### DirView - grid optimization - UI blocking - volatile
Volatile field in Java has a memory fence to avoid caching the value in CPU registers and to flush it to RAM instead (for all threads to see).
This can create contention and block thread, in this case UI thread.
The algorithm now avoids volatiles as much as possible as well optimizes how it uses them.

#### DirView - grid optimization - UI overload - per-image delay
To improve loading experience, `Thread.sleep/delay()` has been used in the past to give UI thread breathing space between loading each image.
The delay has been reduced from 5ms to 1ms. For 1000 thumbnails, this alone decreases total load time by 5s.
Ideally, it would be removed to 0, but this also serves as avoidance mechanism for only loading what's immediately on screen.
In the future, this will be improved

#### DirView - grid optimization - UI overload - image parallelism throttling
This was prototypes using `Semaphore`, limiting the number of concurrent images being loaded.
For now this feature is still disabled, but may be used to fix the above issue.

#### DirView - grid optimization - layout
Cells in the grid now only call `relocate()` instead of `resizeRelocate()` if resizing is not necessary.
The calculation of cell positions now also avoids duplicated computations and only happens once for each unique value.
Reused cells now also reduce amount of state changing when they are being removed/re-added to the grid. 
Cells now do not call `super.requestLayout()` unnecessarily.

#### DirView - grid optimization - cell updating
Cells in the grid now do not internally update unless the item has actually changed.
This was impossible until now, due to internal implementation.

#### DirView - grid optimization - cell ui caching
Cells in the grid now employ **JavaFX** caching, which caches the `Node` to a texture to avoid expensive redraws.

#### DirView - grid canvas
To reduce layout operations, cell thumbnails can be drawn on single shared `Canvas`.
This was prototyped, however for now remains disabled, because it requires more work to support fade animations and animated images.

## [7.1.0] 2022 09 11

- Update dependencies
- Implement improved action (`ActionData`) workflow
  - Implement support actions in app search 
  - Implement support actions with results
  - Implement action error handling and logging
- Implement Windows file menu integration
- Implement `Inspect in SpitPlayer` Microsoft Windows file menu item
- Implement better global hotkey key detection [use `Map(native key -> javafx)`]
- Implement faster global hotkey key processing
- Implement global hotkey symmetric event consuming
- Implement `MetadataGroup.Field.VALUE` column to not be resizable [improves UX]
- Implement `ActionData` to be invokable through search (`CTRL+SHIFT+I`)
- Implement better widget loading [no longer blocks UI]
- Implement proper widget directory structure (`src` for sources, `lib` for libraries, `rsc` for resources, `tst` for tests)
- Improve styling, layout & consistency for some config editors, picker icons, combo boxes and so on
- Improve `ObservableListCE` item editors if item uses single editor [inline the editor into the editor chain]
- Improve **WhiteLeaf**/**DarkLeaf** skin `TreeView` arrow layout
- Fix **CommandGrid** widget focus not working properly
- Fix **StartScreen** plugin's overlay focus not working properly
- Fix some settings saved/not saved [caused by Config.isPersistable]

This update brings improvements in several parts of the application, notably reworked `ActionData`.

### Container children order
Containers now have well-defined order of children.
Containers can define order irrelevant from index of the children.
For example the `ContainerSwitch`, which has potentially infinite children scrollable horizontally, defines order as `0, -1, +1, -2, +2, ...`.

### Widget loading
Until now, when widget was loading it always involved I/O operation on UI thread, leading to potential stutters.
Now the default widget settings load on widget start and during widget loading they are read from memory.
Improving widget loading experience is always welcome - at this moment the UI thread often has enough to do even without random I/O blocking it. 

### Widget directory structure
Finally, widget subprojects have a directory structure.
This fixes some issues with IDE and project setup.
This opens up the possibility for 2 improvements:
1. proper package structure  
   Java widgets still cause IDE warnings, because the directory path does not match the widget class package declaration, which is necessary for
   `URLClassLoader` to load the class (classes with no package can not be loaded).
   I don't like creating useless directories, but this has 2 advantages for widget developers
   1. The widget source code would be consistent with ordinary projects
   2. The widget could have arbitrary package, relaxing arbitrary restrictions
2. **Gradle** integration  
   The widgets could have their gradle definitions in their directory instead of sharing common template. This has 3 advantages:
   1. widgets become ordinary projects, which is easier to understand
   2. widgets may be compiled with a gradle daemon, which would lead to large compilation speed improvements
   3. widgets could declare dependencies using **Gradle**, instead of providing the jars. This would automate processes and avoid binaries in git repository.
These improvements will be considered in the future.

### Windows menu integration
There is a setting to add an `Inspect in SpitPlayer` menu item into the **Microsoft Windows** file menu.
Ideally, there would be setting for file associations, however doing that programmatically is a mess - it may be implemented in the future.

### Global Hotkeys
Global hotkeys are a difficult thing in Java. SpitPlayer uses `jnativehook` library, which is awesome, but
there are still couple surprises.

First change is symmetric event consuming.  
For a shortcut, e.g., `ALT+S`, comprised of events press(Alt) + press(S) + release(S) + release(Alt), only press(S) has been consumed before.
Now, release(S) will be consumed as well. This prevents interference between multiple applications/hotkeys.
As for press(Alt) and release(Alt), consuming is impossible. The former because the application does not know if this is going to be a hotkey.
The latter, because consuming release(Alt) would make consuming asymmetric - and that opens a can of worms, particularly for modifier keys.

This is of interest due to an interfering Windows feature - release(Alt) displays and focuses application menu.
User does not want this to happen if the release (Alt) is part of a hotkey (if a key is pressed while Alt has been pressed), but Windows does not care.
The only workaround is to suppress the event completely, which can be done using the **Autohotkey** program:
```
~LAlt::
Sendinput {Blind}{sc0E9}
KeyWait, LAlt ; this wasit for the key to be RELEASED. So that it doesn't keep spamming SC0E9 (as seen from an AHK window Key history and script info... window.)
Sendinput {Blind}{sc0EA}
return
;
~RAlt::
Sendinput {Blind}{sc0E9}
KeyWait, RAlt ; so that it doesn't keep spamming SC0E9
Sendinput {Blind}{sc0EA}
return
```

Another change is that all `jnativehook` keys are now mapped to JavaFX keys correctly.
This probably fixes number of potential/hidden hotkey issues.

The library has been updated from `2.1.0` to `2.2.2`, which fixes multiple potential/hidden issues,
see [changelog](https://github.com/kwhat/jnativehook/releases/tag/2.2.2) for details.

The hotkey handling code has been optimized to do less work, which is important, since the code executes for any keystroke in the OS.

### Actions
Actions (`ActionData`) are now invokable through search, even if the action is parametric.
Besides `Action`, which represents a parameterless invocable command with optional (configurable) hotkey, there is a more generic `ActionData`.
In the future `Action` and `ActionData` will be unified into one, but for now they remain separate.

This update brings that closer to reality, as `ActionData` can too now be invoked from application search.
This makes lots of inaccessible actions accessible to user through ui.

##### Actions - receiver
The interesting part is that some `ActionData` are invoked on an object (receiver) - they are basically extension methods.
In such case, the action shows up in search prefixed by its receiver type (e.g, `Text.Export to file...`) and invoking it
brings up a form to provide the receiver object. There are some UX issues with this:

1 Some actions may ask for additional parameters.
In such case, showing the appropriate form is (for now) the responsibility of the respective action, which means that user will be given
two forms, in a succession. This will be explored in the future.

2a Some actions take receiver, for which there is no appropriate config editor available.
There is no way to decide which type of receiver can be provided by the user through config editors.
Rather than hiding some actions, it becomes important to provide more and better config editors for various types.
A good example would be action taking a `Widget` as a receiver. This action requires widget autocomplete, maybe
coupled with widget picker (from UI) or even widget creator (using available widget factories).
Actions with receiver will show up in search and missing editors will may be implemented later.

2b Some actions take `Any` as receiver. Asking user to provide the receiver would involve first asking him what type of receiver
he wants to provide and then provide appropriate config editor for it. The first part is already complicated as it requires
application to know all types of objects that can be created through config editors. For now this problem has been simplified
to providing a `String` config editor. It would be interesting to apply `detectContent()` on the user provided text.
That would go a long way to be able to provide all kinds of inputs very easily, but this will probably end up being just part of the final solution.

##### Actions - unification goals
In the past, the generic action `ActionData` and context menu generators `CoreMenu` have been unified.
Now this is unified with application search. Actions user sees in `ActionPane`, in context menus and in search results, are all the same.
The ultimate idea of an ActionData is a generic action invoked upon (optional) receiver and (optional) parameters within a given context.

##### Actions - context and signature
The context of the action provides important elements to the action, such as active overlay, focused window, `Node` source ui element and so on.
The context can provide predefined fallback mechanism for any of these elements, if the context does not provide them.
ActionData is therefore, in Kotlin literally, defined as `Context.(Receiver) -> Unit`.
The context can be either:
- provided programmatically if action is invoked from code
- context menu (built around a value that becomes the `ActionData` receiver)
- `ActionPane`overlay displaying actions for an object, that is the receiver
- invocable search result, asking user to provide the receiver object by himself

The difference between the context and parameters is that context is implicit.
With the introduction of parameters, action could look like: `Context.(Receiver, Parameters) -> Unit`.
Ideally, to disambiguate these properly, multiple receiver feature would be used for context (which is truly an implicit).
The final form would become: `using Context: Receiver.(Parameters) -> Unit`

##### Actions - threading
Synchronous and asynchronous `ActionData` required separate constructors, but not anymore.
Instead, a new argument with default value is provided. This cuts the number of constructors in half, good riddance.
Ideally, the difference should simply be the declared type of result containing `Fut<Result>`.
However, that would complicate `FOR_EACH` invocation, where there would be list of futures, needed to be zipped into one.

The threading responsibility should lie in `ActionData.invoke` and entire `FOR_EACH` should be a single thread thing, as it is.
Hence, `ActionData` will continue to declare whether it is blocking or not.

Synchronous and asynchronous `ActionData.invoke` have been unified into single `Fut` returning method.

Ultimately, making the action lambda suspending could get rid of this parameter, but that will be considered afterwards.

##### Actions - result
Actions now support returning values.
If action returns a value, an overlay inspecting it will open in the end, allowing user to use the result and continue the workflow.
This is quite a dramatic workflow improvement.
If action is invoked on multiple inputs (if it supports `FOR_EACH`, it can), the result will be a list of results of all action invocations.
Action returns result if it is not `Unit`. In case of `FOR_EACH`, result is considered list that contains at least one non-`Unit` element.

##### Actions - results and transformations and workflow
For user, returning a value means the action is not a consumer but transformer.
User workflow starts with some value (in some context) and can continue with transformation actions until a consumer action is invoked.
If the consumer action ends with an error, workflow continues with the error as a result.

For developer, this means action does not need to use the overlay to display the result anymore.
Simply returning the value will cause the result to be handled appropriately.
Actions can now be pure functions and avoid duplicating code.

##### Actions - errors
With proper (and consistent across contexts) threading and action result support, proper error handling has been introduced as well.
Action can now throw exceptions, and they will be consistently handled:
- exception will be wrapped with one with appropriate message and logged without fail in all cases
- exception is considered a result of the action and just like with normal result, returned and inspected in overlay
- if the action returns `Try` instead of throwing exception, it works just the same

##### Overlay - exception inspection
Exceptions are now better supported in overlay content inspection - exception stacktrace is shown as text in the `TextArea`

## [7.0.0] 2022 08 28

- Update Kotlin to 1.7.10
- Update dependencies
- Implement table column any/no name support
- Implement table sort memoization (considerably improves sort speed in some situations)
- Implement table column nesting (arbitrary depth)
- Implement table export (.md, .csv) functionality
- Implement table column description & provide ui menu for user
- Implement table auto-sizing to content & provide ui menu for user
- Implement **ObjectInfo** table preview for collections
- Implement json support for value classes
- Implement `WeatherInfo` forecast
- Implement way to obtain dependencies' licence report [gradle licenceReport gradle task]
- Implement `SongInfo` rating change using ui
- Implement `inspect` CLI command
- Improve Authorization Bearer content detection
- Improve json serialization/deserialization performance [instantiate ObjectMapper only once]
- Improve JavaFX property detection [avoid getChildren twice]
- Improve nested config editor layout
- Improve config editor default button [turn into caret with menu]
- Improve slider styling [gradient effect]
- Improve logging output
- Improve tables [unify ObjectInfo table, `WeatherInfoForecastMeteors` table, `ActionPane` table into a generic table]
- Improve **ObjectInfo** and `ActionPane` data inspection output in some cases
- Improve `SpitSliderSkin` value changing tooltip layout [less intrusive]
- Improve content detection [support FileSize and Duration]
- Improve `WeatherInfo` settings UX [add config descriptions]
- Fix `WeatherInfo` not refreshing ui sometimes
- Fix `String` property editor autocomplete in some scenarios
- Fix inconsistent **ObjectInfo** and `ActionPane` data inspection in some cases
- Fix table not respecting column order
- Fix table search fails if primary column null
- Fix table filter empty if table computes its own columns
- Fix table header hover effect styling
- Fix table column resizing not taking column/cell padding & column font into consideration properly
- Fix table horizontal scrollbar visible sometimes when using `CONSTRAINED_TABLE_RESIZE_POLICY`
- Fix volume inc/dec on mouse scroll when scrollable element is hovered
- Fix rating skin settings not being applied
- Fix skin extensions not being reactive
- Fix `Orientation` config editor icon
- Fix `ActionPane` displaying json & data class `TextArea` preview in compact print
- Fix `ActionPane` displaying json & data class simple preview as multiline
- Fix `HBox`/`VBox` styling not taking effect
- Fix `ObservableListCE` not using user set layout
- Fix playlist table resizing [fill entire width]
- Fix libraryView song table resizing [do not show horizontal scrollbar]
- Fix shortcut `ALT + F4` not closing window sometimes
- Fix json parsing succeeding for invalid json input in some cases
- Fix window MOVE cursor while moving not reverting to normal in rare situations
- Fix reading webp images
- Fix reading bmp images when subSampling>1 (reading image smaller than 50% of original size)

This update continues with tons of UX improvements and fixes.

#### Weather improvements
The **Weather Info** widget is receiving hourly and daily forecast as well as meteor shower table.
There is also a link to www.windy.com (for appropriate location) - a useful weather site with additional information.

#### Data inspection improvements
Data inspection capabilities - the overlay `ActionPane` and the widget **ObjectInfo** were mostly unified in functionality.
The data information has been improved or fixed for several cases.
Long texts or texts with multiple lines are previewed on single line. Data classes are previewed as compact json.
The `TextArea` preview however displays pretty formatted json.
The table used for `Collection` previews is now more generic, supporting any data class.

#### Table column nesting
The tables now support nested columns.
This has been put off for a long time, but it's finally been done.
The feature is an eye candy, but is useful for nested data classes or visual column grouping.
The leaf columns are data driven, while their parents are merely visual.

The core of the tables is `ObjectField`, i.e. field, a definition of a getter/attribute of objects of certain type. Fields represent and generate columns.
Thus, column nesting is an `ObjectField flatMap ObjectField operation.
Table only requires to know the leaf fields.
The parent columns can be computed at any time by traversing the visible leaf columns/fields by upwards the flatMap hierarchy.
Now `table.columns` returns root columns and `table.fields` and `table.visibleColumnLeafs` return only leaf fields and visible leaf columns.
During serialization only leaf columns are serialized. Upon column state change the column parents/groups are reconstructed.
For clarity, `table.columnRoots` and `table.columnLeafs` haven been added to the API and delegate to `table.columns` and `table.visibleColumnLeafs`.

Using flatMapped fields has consequences that are beyond just nested column:
- Column root  
  Single column that is parent of all other columns. Can be used as simple table header.
- Column nesting lvl  
  Hierarchy with unlimited depth  
  For now there is few types of fields so this is still manageable problem, but in the future, could things will have to be improved:
  - Computing all the possible leaf fields, lazily
  - Detecting and possibly disallowing column nesting loops
  - Large number of leaf fields as a result of combinatorial expansion  
- Column visibility context menu  
  So far, the menu is flat list of leaf fields (which was already unwieldy before due to number of song tags).  
  The menu could use submenus for nested columns, however with lazy fields, it will be necessary to adjust the column visibility menu depending
  on which column/group has been clicked on, and only providing the toggles within given nesting level.  
  This will make it possible to build column complexity of any hierarchy, through UI. This will be considered in the future.
- Column searching picker menu  
  Similarly to column visibility menu, non menu component with search will have to be used to pick menu to search by. This will be considered in the future.
- Column filtering combobox  
  Filters already support arbitrary function chains, therefore intuitively, the filter should only provide top level columns.
  However, provided flat-mapped leaf columns as set of initial filter builders is convenient.
- Creating/deleting `ObjectField` definitions for common types to generate the most intuitive tables.  
  For example, `Song` has a field `Filename`, but now the nested`File.Name` makes more sense.  
  There are considerations to be made, including performance and backwards compatibility and sorting out field implementations will be done in the future.
- Table sorting performance
  - Nested columns cause degradation of sorting speed due to nesting field value extractors.  
    Top level columns usually extract value directly from the object, but with nesting, the value computation quickly adds up.
    This problem turns into a disaster (sorting 50k items taking potentially 10s)  
    Sorting a collection requires N*logN operations, each calling field value extractor.
    During comparisons, extractors are called for each value multiple (logN) times. Thus `list.sortBy { extractor(it) }` puts lots of pressure to how fast the extractor is.
    To fix this, `ObjectField` can now be turned into `memoized` version of itself so that it uses `IdentityHashMap` to only extract value from each input at most once.
    Memoized field produces memoized comparator, which compares only the extracted values.
    The performance improvement is for 50k items 50-fold, which is satisfactory. Also, the sorting is now more deterministic and stable (important for io and non-deterministic extractors).
    The downside is that memoized fields and memoized comparators are stateful and not only use potentially lots of memory but can be used only once.
    This complicates things, as table comparator can be and is accessed and used outside table.
    For now, the table exposes standard (and slow) comparator through its API and only uses the fast one internally.
    Alternatively, table API may expose comparator builder instead, but that makes it more difficult to use. This will be considered in the future. 
  - The comparators are already heavyweight, due to being build generically from table column sort order,
    casting extracted value (to `Comparable`), always assuming nullable values, thus doing null checks and wrapping comparators with nullFirst/Last.
    Hence, the actual comparator could actually be `chainComparators(list(reversed(nullsLast(naturalOrderBy(nullsLast(comparableCaster(nullCheck(fieldValueExtractor))))))))`
    The comparator has been simplified. The table does not use nullable comparator anymore. Non-null comparators do not put null checks inside extractors.
    Casting to Comparable inside extractor has been removed in favour of casting the entire field to `ObjectField<T,Comparable<*>>`.
    If the extractor is not nullable, nullFirst/Last wrapping is not applied either, which means `naturalOrder()` can be avoided as well.
    This does require switching over various cases and building appropriate comparators as well as separate methods for building nullable and non-null comparators.
    But it is worth it.
  - `Song.file` has been optimized to know to be a file, which avoids `isFile`/`isDirectory` IO calls in sorting by certain columns

#### Other table improvements
Table columns can now have any text as name.
Previously, certain characters would cause column serialization issues, but now serialization uses `column.id` instead of `column.text`.
More importantly, columns that display graphics can specify their name to be empty, to improve UX.
Multiple columns can also contain the same text.

The tables provide csv/md export and column description features in menu.
The tables can auto-size to content and this is done automatically where appropriate.
Several minor issues with table search, filter, resizing and sort have been fixed.
The table header ui effect has been improved/fixed for some skins.

#### Settings and Form IX
**Settings** and forms now handle nesting better and take less horizontal as well as vertical space.
Inspected `Pane`s also do not display children twice (`getChildren`, `getUnmodifiableChildren`).
The editors now have caret with menu instead of simple default button. This adds additional functionalities to editors.
Overall, the layout is more consistent and easier on the eyes. 

#### Json
Json conversion boasts value class support.
This is useful for validation (in value constructor) and type-safety.
The application has been discovered to parse invalid json inputs like `1 2` as valid json.
This caused issues during dynamic content detection. The issue has been fixed.

#### Licence reports
The project can generate licence reports for dependencies with the gradle task `licenseReport`.
In the future, the report may be committed to the git repository or even be available to user.

## [6.0.0] 2022 06 21

- Update Kotlin to 1.7.0
- Implement **WeatherInfo** widget
- Implement **GpuNvidiaInfo**.kt nvidia-smi.exe path settings
- Implement `application/pdf` cover support
- Implement config editor for `TextAlignment` & `Orientation`
- Improve **Converter** widget UX/performance [hide data action - show only in popup]
- Improve **Node** widget
  - provide node instance settings in widget settings, properly separated and configurable as JavaFX object
  - persist the node instance state with widget settings [restores node state on reload/restart without using widget i/o]
  - support userLocation specific to the node instance type instead of one for all Node widgets [supports custom default settings per instance type]
- Improve ObjectInfo.kt
  - add CTRL+V support
  - add content detection support
  - add text preview support
  - add song & image tag description support
- Improve context menus
- Improve data info output for images [ActionPane, ObjectInfo, Tooltip]
- Improve logging output
- Improve json reading robustness & performance [use jackson instead of Klaxon]
- Improve json types conversion support
- Improve string types conversion support
- Improve nullability discovery for JavaFX properties
- Improve read-only discovery for JavaFX properties
- Fix json deserialization failing for nullable properties sometimes
- Fix oshi CPU usage crashes for Windows 10
- Fix window interaction !work when it has no focusable content

This update brings general improvements all around.

Compatibility with Windows 10 has been improved. Various issues has been identified and fixed.

Under hood, Kotlin has been updated to 1.7 and widgets now compile with K2 compiler (2.5 times faster reload after change).
The `Klaxon` library for json has been replaced with `Jackson`, which is more robust and performant.
The plan for the future with `Json` is to separate it to own tiny module, expose more API, use coroutines and context
receivers to get rid of the explicit `Json` object (analogue to Jackson's`ObjectMapper`). That would be killer json API.

The conversion capabilities required for settings and persisting application state have been enhanced.
The JavaFX property discovery is now more typesafe, identifying correctly read-only & nullability characteristics (in common cases).
Some JavaFX properties have been hidden from certain component settings. These improve UI performance, reduce validation warnings and other issues in forms/settings.
Additionally, `String` converters for additional types have been implemented. All data classes are now supported automatically, producing json output.
Json converters have been added for all types that already had `String` converter.
With this, the application is more prepared to tackle additional widgets and features.

The **Node** widget, being a launcher for custom user widgets that merely instantiate JavaFX-based UI components, is greatly improved.
The various types of Nodes recommended for instantiation are now considered their own widgets in all aspects.
These widgets have their own user data folder, support their own user-defined default settings, cloning, reloading, etc.
Also, **Node** widget now allows configuring the `Node` instance in its settings, providing UX familiar from other widgets.
The widget stores its entire `Node`'s state and applies it on widget reload or application restart.
It is still possible to use widget i/o to turn the various properties of the `Node` instance into Inputs, that can be fed values across time,
but this is no longer required (to persist the state).
With this, any `Node` subclass can be truly turned into a widget with full feature support that user would expect.

On widget side, **GpuNvidiaInfo**'s **nvidia-smi.exe** path is now configurable. Its UI may become modular in the future.
The new **WeatherInfo** widget displays local weather. For now, it still lacks hourly and daily forecast, which will be added in the future. 
The **Converter** widget no longer suffers broken layout when shrank to small size and the **Use Data** portion of Ui is hidden behind an icon.
The **ObjectInfo** widget sports scrollable content and besides the image preview, there is also one for text. In the future,
table support will be added with automatic data class decomposition to columns. 

There are improvements prototyped to the **WallpaperChanger** plugin. These changes may or may not be realized.

There are also improvements prototyped to allow windows to have acrylic blur background effect in **Windows 10** style.
The functionality is there, but the interplay with skinning and transparent content windows must be figured out before roll out.

## [5.0.0] 2022 04 24

- Update Kotlin to 1.6.20
- Implement **Mouse Info** widget
- Implement reopen widget settings after recompiling/reloading widget
- Improve widget focus traversal to work when only one widget is open
- Improve SwitchContainer tab aligning UX [smarter Alt+Up shortcut]
- Improve **Spektrum** widget
  - Implement effects (mirroring, pulsing, shifting)space smoothing, bar styles
  - Implement spatial smoothing
  - Implement multiple bar styles
  - Implement multiple bar input data choices 
- Improve transparent content styling and UX
- Improve window radius styling and UX
- Improve window & Component context menus
- Improve Song/File context menus
- Improve **Settings** widget layout in some situations
- Improve **Converter** widget:
  - Improve `CTRL + V` handling
  - Simplify initial UX state
  - Improve functor picker to use text field with autocompletion search instead of combobox
- Improve **Widget management** UI [add more information in some situations]
- Fix widget compilation never finishing and not reporting errors sometimes 
- Fix **SongTable**, **SongGroupTable**, **Playlist** widgets not focusing properly
- Fix form layout type settings not changed though form icon
- Fix window menu fullscreen item not working
- Fix launching widgets in new process failing due to jvm args considered app args
- Fix notification hiding prematurely when reused and starts hovered
- Fix notification changing position when reused when another n. is show

This update continues improving UX by fixing issues and preventing user to get into unintuitive UI situations.

Windows UX have been improved. Popups respect onTop and focus of their parents and gain/lose top z-order properly.
Transparent windows now support click-through behavior and remain interactive. For better UX, click-through is off when window is focused.
This allows using non-interactive always-on-top widgets for HUDs and overlays.
There is still work to be done regarding cumbersome control of transparent windows with no content, window resizing and so on.

The **Spektrum** widget has been massively improved.
It now supports large number of separate settings that can be freely combined into unique effects.
For instance, the widget allows specifying various data as input (FFT, volume, etc), various data transformations, drawing styles and
multiple effects, such as pulsing, mirroring, shifting and more. These are all applied separately and more choices could easily be implemented.
The widget could still do with some drawing optimizations and also suffers absolute vs relative coordinate/sizing issue that I am not sure how to tackle at the moment.

The **Comet** widget has been brought up to date.
Several issues have been fixed, mainly small game graphics, which is scaled up 2x.
Performance has been improved to cause no issues even in 4k, by rendering the Canvas at smaller size. This does introduce slight blurring for Canvas elements.
There are still number of issue, such as experimental features and performance issues regarding number of objects potentially crashing the game.
The widget may become bundled with the application in the future.

The **Converter** widget have long suffered from two issues.
First, initial state was set to Unit with transformation chain of containing `As Is` function.
The initial chain is now empty and pasting any content changes the input, rather than appends it to transformation chain.
Second, the functor picker, using ComboBox, did not make it easy to find sought after functor.
The selector is now a TextField with an autocomplete, which allows smartly to select the functor based on name and type.

For this both the chain and autocompletion components needed some improvements.
Particularly the autocomplete. It now handles events properly so to allow controlling the suggestions list and text field at once.
This solution is a satisfactory replacement for the non-existent ComboBox with search, which proved too problematic to realize. 

## [4.0.0] 2022 03 03

- Update **JDK** to 17 [improves performance & security]
- Implement recommended **Custom** widget classes as widgets
- Implement pause/resume icon for playlist table rows
- Improve icon hover/focus/select interaction (fixes subtle UX issues in some controls)
- Improve names/icons for some widgets
- Improve check menu check icons [use more compact icons]
- Implement application process name argument [makes it easier to identify Java process from process command]
- Implement **Song Info (Small)** widget [uses `SongInfo`]
- Implement **Album Info (Small)** widget [uses `SongAlbumInfo`]
- Implement **White Leaf** skin
- Implement **Dark Leaf** skin
- Improve styling
  - Table/List/Grid/Tree/TreeTable view headers are bigger, bold, align based on content, have proper padding, show borders on hover
  - Table/List/Grid/Tree/TreeTable view footers are bigger, better vertically aligned, align based on content, have proper padding, shw borders on hover
  - Separator is more sleek and less distracting
  - **ContainerSwitch** tab gap respects tab index [the center tab always covers entire window]
  - Check menu check icons use more compact icons
  - Check menu check icons use hover effect on item hover
  - Use bold font for some selections
  - Window content clipping respects radius
- Improve popup content complexity [use Layout instead of ContainerSwitch]
- Improve **Icon browser** widget
  - Improve layout & UX
- Improve **Git projects** widget
  - Improve layout & UX
- Improve **Tester** widget
  - Implement remembering selected content
  - Implement **Mouse events** testing suite
  - Implement **CssBorders** testing suite
  - Improve layout & UX
- Improve widget **Spektrum**
- Improve widget **Voronoi**
  - Update `JTS` dependency
  - Fix moiré effect by improving cell position precision
  - Implement better smoothing
  - Implement use skin colors 
- Improve widget **Function Viewer**
  - Handle very large numbers correctly [Use `BigDecimal` everywhere]
  - Implement function area highlighting
  - Implement use skin colors 
- Improve `GridView` context menus
- Fix `ProgressBar` not hiding after showing 0/0 progress
- Fix importing songs to library not working properly
- Fix `ActionPane` not returning data correctly sometimes
- Fix `ConfigPane` not respecting margins
- Fix `Configurator` widget content scroll pane not respecting height
- Fix windows opened through actions in **Search close** immediately close after showing
- Fix showing object settings showing application settings temporarily and with bad UI performance

This update brings lots of pleasant improvements. Particularly styling.

First, finally, **JDK** has been updated. It took 3 years to get over **JDK12**. Time flies.
The `linkJDK` Gradle task has been removed from build dependencies and now requires manual run. It is more of a convenience anyway.
Now, **JDK** updates are very comfortable. By the way, the project still uses **OpenJ9**, now called Semeru.

The application process command has received a no-op argument to help to distinguish the process from other Java processes.
This would not be such an issue if the **Launch4j** was set up to wrap the .jar into .exe, but it currently does not work well for 64-bit version.
This requires more research and testing to implement right, so for now, at least the command was enhanced.

The widgets **Icon browser**, **Git projects** and **Tester** now use much improved and unified UI for selecting content.
Focusing, hovering, selecting from the list of content choices using both mouse and keys is supported and works very nicely.

There are two new skins: **White Leaf** and **Dark Leaf**.
These are light and dark green themes easy on the eyes and with good contrast.

There are two new widgets: **Song Info (small)** and **Album Info (small)**.
These display basic song and song album information. The former is an improved version of the default **Now Playing** notification content.
This change is following the trend of many small widgets instead of few complicated ones. This gives more power to the user.

New names and icons for some widgets make them easier to find and use.
Even better, all the registered `Node` classes, viable to be used as widgets are now available as separate widgets.
This was surprisingly trivial to implement, and it truly improves the UX.
In the future, there may be widget metadata support for these widgets, such as icons or descriptions. There is potential for a lots of improvements here.

Another new feature is a graphical playlist table column for a pause/resume icon.
The icon allows controlling the playback from the **Playlist** widget - definitely useful.
In the future, there will probably be more such columns and with automatic width resizing and styling support.

Application's tables and other table-like components have also become more usable and elegant.
The ui elements relying on `Canvas` now support skin colors.

The **Function Viewer** widget has full `BigDecimal` support now.
This was not easy, different library for expression evaluation was necessary and using `BigDecimal`s properly is difficult.
There were lots of arithmetic errors related to precision, resulting in incorrect values, bad performance, exceptions and even infinite loops.
These are all solved and the plotter not supports unlimited zooming and precision.
The graph also highlights area under the function, which is elegant and quite helpful.

The **Spektrum** widget has also become prettier, as it uses more sophisticated and configurable smoothing.
These changes have been applied from the original project the widget is based on.

## [3.1.0] 2022 02 07

- Implement **widget I/O** for any type of window (docks, overlays)
- Implement **Save component** action to save component loaded from a previously exported file
- Implement **Convert Image** action for images. Supports batch conversion and is very fast on SSD due to io parallelism
- Implement **GitProjects** widget selection & context menu
- Implement **Inspect data** action content detection (user can get use raw data using **CTRL** on **drag**, **SHIFT** on **CTRL+V**)
- Implement **Inspect data** action content preview for `String`
- Implement `TextArea.wrapText` context menu item
- Implement **Change font size** on **CTRL+SCROLL**
- Implement **Show Lyrics** action (this is candidate for new widget)
- Implement **Synchronize file times** action for restoring file creation/modification time after copy
- Implement better tracking of background activities and their error results
- Improve entering/exiting layout mode performance
- Improve logging output [reduce logging for some libraries]
- Improve context menus, context menu item icon tooltip, tooltip layout
- Improve `DatePicker` & `DatePickerContent`
  - Implement selection
  - Implement locale-specific day of the week order
  - Improve styling
  - Fix incorrect calendar day numbers sometimes
  - Fix incorrect calendar layout sometimes
  - Fix unable to instantiate inside **Custom** widget
- Fix content detection often detecting text as a relative file or uri
- Fix image loading some images at full resolution (improves performance)
- Fix application not shutting down gracefully sometimes
- Fix `Collection.size` & `Map.size` functor output type
- Fix some configs and editors not using `UnsealedEnumerator` constraint in some cases resulting in no autocompletion
- Fix `GridView`s `GridInfo` not taking active filter into account, resulting in incorrect selection statistics
- Fix some actions/menu items are not available due to not registering 
- Fix drag & drop when external application sets `FILES` to empty list
- Fix **Logger** widget `area.wrapText` observability and default value not applied correctly
- Fix **cmd** file lookup not finding certain files sometimes

This update brings lots of usability improvements.

The main improvement is layout I/O UI.
The I/O ui was originally tied to **Switch** container, which provides the ui move/zoom capability, which affects the **IO**.
Because of this, layouts, which do not use **Switch** container (docks and overlays) did not support input/output editing.
The I/O layer has been made to support both **Layout** and **Switch** (which effectively acts as top level).
So there is no need to figure out which of the two needs to have it and no extra containers just for I/O.
When **Layout** contains **Switch**, it lets **Switch** take care of I/O. Now every content supports I/O, including docs and overlays.

Another improvement is content inspection/processing using **ActionPane**.
First, there is automatic content detection after **CTRL+V** or **drag & drop**.
Second, just like `Collection`s are displayed in table, `String`s are now displayed in `TextArea`, which can now change its wrapText settings through context menu.
This will be further improved to support images, or displaying text representation of objects that have no natural representation.
Ideally, this functionality would be absorbed into **Object Info** widget.

Another improvement is bgr task management.
First, task invoked through forms/popups now also display in task list. This makes closing the window not a destructive operation (as progress could no longer be tracked).
Second, if the task ends with error, this error can be displayed by clicking on the task result warn icon in the task list, which opens **ActionPane**. This makes it possible to track task errors.
Third, tasks that do parallel processing or produce multiple outputs (potentially with errors) also return future, can be tracked and have their errors displayed.
Actually acting on the errors is still not very easy.
The table should get `Try.Ok/Error` filtering predicates, nested columns (for `Ok` and `Success` branch) and transformation capabilities (like **Converter** widget).
These may come in future updates.

This update refactors the code to further unify actions for `ActionPane` and actions for `ContextMenu`.
Introduced is `ActContext`- an invocation context carrying auxiliary data, such as source of the event.
This enables using actions outside `ActionPane`, even if they use it. Now any action can be used in context menu.

Regarding the application shutdown fix - the old solution was brought back to forcefully shut down JVM even if non-daemon threads are running.
This is because proper cleanup is never guaranteed even if `App` is already stopped.

## [3.0.0] 2022 01 24

- Update **Kotlin** to `1.6.10`
- Implement application locale settings
- Implement locale-specific formatting for data in various parts of the application
- Implement **Node** widget object instantiation error notification
- Implement window move hint cursor - show **MOVE** cursor when **ALT** is pressed
- Implement **FreeForm** container window move hint cursor - show **MOVE** cursor in layout mode
- Implement **FreeForm** container window styling (show gap between windows)
- Implement more functors (word/sentence splitting, list operators)
- Improve tables
  - Sorting performance (up to 50 times faster)
  - Widget/ui loading performance (sorting does not block ui now)
- Improve component controls
  - Remove lock icon from component control header icons
  - Expose **Switch** container ui controls via ui, like every other container
  - More Component context menu entries
  - unify **Custom** widget menu with any widget controls header icons menu
- Improve context menus
  - Add more items for component
  - Unify Custom widget menu with widget controls header icons menu
- Improve song drag & drop UX - do not show placeholder for song tables
- Improve component drag & drop UX
  - Fix **FreeForm** container window closing sometimes after swapping content
  - Fix drag & drop placeholder sometimes at wrong position
  - Fix **FreeForm** container window move preventing component drag
  - Fix incorrect placeholder size sometimes
  - Avoid fading out content when placeholder does not cover entire area
- Improve editor constraint messages with values (uses human-readable converter)
- Improve widget **Command Grid**
  - Add max cell columns settings
- Improve widget **Image**
  - Make focusable
- Improve & refactor code, update dependencies
- Improve widget **Function Viewer**
  - Handle very large numbers correctly
  - Add validations
  - Use `BigDecimal` for coordinates
  - Use human-readable text for numbers where possible (avoids scientific notation)
  - Hide coordinates label when mouse is not hovering
- Fix `DateClockDigitalIos` crashing for some dates
- Fix slow selecting of lots of items in table, such as when **CTRL + A** is used (original JavaFX bug)
- Fix song group table showing incorrect selection statistics sometimes

I took a little break from coding, but I am back. This version brings major improvements, so I am bumping up to **3.0.0**.

First, the project is now finally using **Kotlin**/**Java** toolchain, which automates **JDK** setup into a build.
Yes, it means the build does not require any JDK installed!
So developers can now simply clone the repository and build/run through **Gradle** with no extra steps, without leaving **CLI**.
The build is much simplified, as most of the Java checking/setup has been removed.
Previously, migrating **Java** was a headache, but not anymore. I plan to use **Java 17**, a much-needed upgrade from **12**.

As for the application, several areas have received major upgrade.

Tables are the heart of the library as well as pride of this player.
This version finally fixes slow selecting, which could in large tables take up to several minutes!

Table sorting has been identified to cause problems, such as taking up to several seconds in some scenarios!
After some refactor, the speedup has turned out to be huge, for **Song Table (Big)**, which is the view-everything table, the reduction for 50k songs was 2.5s -> 50ms.
50ms still blocks ui for 3 frames (on 60Hz display), enough to cause ui loading problems, particularly during animations.
UI loading in general is problematic, because the single-threaded nature of UI frameworks will not allow for optimization.
But, I managed to make sorting for all tables asynchronous. This was tricky because in some scenarios the application must not expect the items in correct order.
But now table performance does not degrade with item count. UI responsiveness when table is loading or sorting is now 60hz smooth!

I also simplified widget/container controls. There is fewer icons and much better menu.
The changes provided a noticeable performance gain while moving/zooming the UI, which is now 60Hz smooth as well.
In edit mode, the layout can be traversed by **RMB**/**LMB**, and now it includes **Switch** container, which, as top container, was not previously accessible.

**FreeForm** container, which provides windows-in-window, can be used in screen overlays or bottom-most screen overlays.
Therefore, proper functioning is absolutely integral. After more use/testing several problems have been identified.
With this update, it has become much easier to use thx to improved styling and drag&drop, which it relies on in lots of situations.
This component is complicated, and I will continue to improve it.

Besides these changes, many smaller improvements and fixes have been made.
Both the project and the application have never been in better state.

For the future, I have no exact plans yet. I will continue to go through the backlog - it contains lots of non-trivial ideas.
However, the priority will also be to keep improving UX.

## [2.1.0] 2021 09 26

- Improve widget i/o system
  - Avoid creating ui elements for i/o in other windows
    - Improves performance
    - Removes visual artefacts
  - Implement context menus for linking/unlinking widget i/o
  - Implement support for application-wide `Input` and `Output`
  - Implement **Song Library** and **Playing song** as `Output` instead of `InOutput`
  - Remove ui support for application-wide i/o
- Improve markdown styling
- Improve Json serialization for arrays, unsigned numbers and nullable types
- Improve String char16, char32, grapheme facilities
- Improve widget & plugin settings UX
  - Add context menu support for ComponentFactory & PluginBox
  - Simplify & improve layout
  - Improve interaction (remove obscure behavior, add hover effects)
- Improve component ui controls header icons  
  **Settings** and **Actions** icons were merged into **Menu icon**, which is more flexible as well as consistent with window headers.
  In the future, the menu may be customizable through `CoreMenus`.
- Improve code
- Fix several widget serialization/deserialization issues

## [2.0.0] 2021 09 19

- Implement smart window resize (`window drag + SHIFT + LMB`) - resize window to available screen area  
  This behavior may be tweaked in the future, and leveraged more extensively.
- Implement running programs through .lnk files
- Implement `Class` & `KClass` editor autocomplete  
  It is difficult to obtain loaded class list, so instead hardcoded list is provided by developer in form of a resource file.
  More entries may be added to the list in the future.
- Improve markdown styling and UX
- Improve code
- Fix wrong table group column name in some table column menus

## [2.0.0] 2021 09 10

- Much improved handling of song's `TAGS: Set<String>` tag. The field:
  - has better editor
  - is displayed and can be quick-edited through **FileInfo** widget
  - supports autocompletion
  - supports song table grouping
- improved song chapters support in **PlayerControlsCircle** widget's circular seeker
  - layout and sizing issues
  - chapter hover effect
  - chapter click shows chapter popup & edit
- more converting functions
- fixes related to styling `MenuBar`, song tagging (`wav`), threading issues, reflection, and more

The tags grouping was tricky, because it is a multivalued tag.
Now that multivalued tags are supported, artist tag could also become `List<String>`, which would fix long-standing issue of grouping by artist.
Adding song chapters through circular seeker is not yet done, because it is not clear what the UX should be like.

## [2.0.0] 2021 09 03

- improved **Node** widget with better context menu & class suggestions. Also renamed to **Custom**.
- Improved ComboBox styling & autocompletion UX
- Improved table sorting through context menu & add playlist table sorting by db columns. It is finally possible to sort playlist by rating
- Improve **Converter** widget layout and Action settings
- Improved TextArea context menu
- Improve markdown node (drag&drop, anchors, animations)
- Improve ContainerFreeForm (resizing, absolute sizes, position aligning)
- Simplify window header & introduce window context menus
- Unify App actions, App context menu, Tray context menu, Window context menu
- More minor tweaks

The new context menus are an important addition and solve the issue of feature discoverability. I wanted to avoid this for years, but there simply is no better (compact) way. Anyway, now I can add Help/Documentation/About pages

The markdown still needs more work with link resolving as well as file monitoring and automatic reload. [RichTextFX](https://github.com/FXMisc/RichTextFX) may be used to produce more optimized and powerful UI.

## [2.0.0] 2021 08 27

- Support for Markdown text was implemented.
  - **GameView** widget is now using this for game info
  - new **GitProjects** widget has been implemented to browse git projects. It simply lists git projects in a directory and shows the `README.md` files. More features may come later
- **Kotlin** `1.5.30`. In the future I'll use gradle jvm toolchain so JVM sets up automatically. This will alleviate the remaining pains when setting up the project.
- Added cool stuff to **Voronoi** visualisation widget

Finally, an app user documentation can be written, as markdown of course. This will take a while and probably result in improving UX all around.

What's next? Well, as is, the application is very polished and has a lot of powerful features. I'd consider this state more or less ready for making available to anyone out there.

1. Right now, I'd like to keep improving the window system to make it easier to set up desktop-level widgets.
   Improving the Node widget will be necessary as well as discovery of recommended ui components for Node widget.
   This also involves more FreeFormContainer features, such as absolute resizing, snapping and styling.
2. I also want to integrate context menus and object actions (icons in `ActionPane` overlay) more closely, so that these are merely a different UX for the same thing.
   This requires implicit action parameter handling in menu (probably by displaying a Form for each parameter) and more convenient navigation in `ActionPane` (back & forth, custom ui, etc.).
   This also several other technicalities, so it will be challenging to come up with pleasant solution.
   The actions will probably end up defined as custom objects (and registered in global pool) rather than ordinary functions, which is unfortunate, but maybe special delegated properties could help.
3. Better initial experience & application documentation would also be nice, for example there is still no **About page**.
   Window headers are also too cluttered, maybe ordinary menu bar would be a better idea. Window headers could be their own components as well.

## [2021 08 21]

- improved how windows & window headers auto-hide and improved styling. The reworked functionalities make the application much more pleasant to use.
- improved `GpuNvidiaInfo` UI, although it requires more work.

## [2021 08 19]

- improved documentation, provided commands to build executable application from source  
  I'm thinking this may actually be better than doing releases, will see.
- improved `Container`s. They now have settings and support padding. Free-form has several issues fixed, further improving its use in various use-cases.

## Older

### Added
- Rename application to `Spit Player`
- Reduced memory consumption (up to 30%) due to adopting AdoptOpenJDK with OpenJ9
- Reduce application size (largely due to removal of javafx.web)
- Implement `--dev` developer mode argument option
- Implement `--uiless` argument option
- Implement ui less application mode
- Implement widget recompilation prioritizes open widgets
- Implement widget recompilation when application jars are more recent
- Implement widget reload button for widgets that do not load properly
- Implement widget focusing when widget opens to be used (e.g. from context menu)
- Implement auto-setup for Vlc player (Windows-only)
- Implement auto-setup for Kotlin compiler
- Implement auto-setup for ffmpeg compiler
- Implement application header progress indicator title showing running task count
- Implement new popups (fixes various issues related to popups)
- Implement application settings import/export/defaults actions
- Implement application settings filter
- Implement select settings group when opening setting from application search
- Implement confirmation popup for remove songs from library
- Implement padding settings editor
- Implement checklist settings editor
- Implement 0 length support for list editor
- Implement slider fill
- Implement Unicode character icons
- Implement more skins
- Implement application help page & shortcut
- Implement component help page & shortcut
- Implement window ALT+drag resizing+maximizing
- Implement window header animation
- Implement Webp image read support
- Implement Form support for asynchronous actions
- Implement webp image support
- Improve application settings order/hierarchy
- Improve SLAVE application startup (due to not starting some plugins)
- Improve initial window showing performance
- Improve video cover extraction performance (reuses previous output)
- Improve grid cell hover/focus effect
- Improve overlay animation performance and ux
- Improve overlay icon layout
- Improve drag&drop placeholder ux
- Improve shortcuts ui display (use unicode glyphs for keys)
- Improve SwitchContainer UX:
    - Focusing now aligns tabs so the focused content is visible when switching focus
- Improve BiContainer UX:
    - snapping to relevant positions (edges, 1:1 ratio, centre)
    - collapsing only taking visual effect on mouse release
    - resizing glitches
    - absolute position not restoring properly sometimes
    - absolute position being distorted on certain resize operations
- Improve object inspection type inference
    - supports generics, displays `List<SomeType>`
    - displays nullability, displays `List<SomeType?>?`
    - displays variance, displays `List<in/out SomeType>`
    - displays best-effort list type by inspecting elements
    - displays unknown types as STAR projections `*`, displays `List<*>`
- Improve image loading performance for common formats dramatically
- Improve image loading performance
- Plugins
    - Implement plugin metadata (simple)
    - Implement plugin management settings
    - Implement `Screen Rotator` plugin dialog
    - Implement `Start Screen` plugin
    - Implement `Wallpaper` plugin File.setWallpaper action
    - Implement `Wallpaper` plugin to retain wallpaper when application closes
    - Implement `Notifier` plugin setting that shows enabled notification types
- Widgets
    - Implement multiple ways to open widgets (configurable by user), i.e., window, popup, overlay, etc
    - Implement widget metadata (simple)
    - Implement widget management settings
    - Implement widget global state
    - Implement `Tester` widget for developers, showcases property sheets and animation interpolators
    - Implement `Object Info` widget
    - Implement `Favourite Locations` widget
    - Implement `File Explorer` composed widget (exported layout)
    - Implement `Hue` widget to control Phillips Hue light system
    - Implement `Dir Viewer` icon cell & option to toggle thumbnails/icons
    - Improve `Dir Viewer` cell size change performance
    - Improve `Tagger` widget UX
    - Improve `Converter` widget UX
    - Improve `Tagger` widget UX
    - Improve `Tagger` reading performance
    - Improve `FileInfo`
        - use checklist to select which fields to display
        - improves UX by adjusting layout automatically depending on visible fields (unifies cover & rating with other fields)
    - Improve `ImageViewer` widget UX
        - remove ability to display thumbnails (use grid widget for this)     
- Display date & time in UI in human-readable format
- Hide technical settings from UI
- Avoid persisting computed and other unwanted settings
- Handle icon focus traversal (TAB) and action invoking (ENTER) better
- Popup close button has been removed and pin button UX improved
- Info buttons in various parts of UI have been removed, `,` and `.` shortcuts now intended for same use case

### Fix
- Playback seek volume fade
- Playback loop song stopping playback
- Main window icon showing in ui-less application mode
- Widgets launched as new process not closing application 
- Widgets not recompiling when application jars have newer version
- Widget infinite recompilation when compiling invalid class
- Widget type id & widget name used interchangeably
- Widget compilation not showed in ui sometimes
- Widget factory removed when file with the same name as widget directory is removed from widgets directory
- Widget not reloading after factory becomes available sometimes
- Widget's technical name displayed instead of user defined name in ui sometimes
- Widget inputs not rebound correctly on recompilation
- Widget inputs not rebound correctly on fxwl loading due to duplicated id 
- Widget inputs applied too extensively during widget loading
- Image size reading sometimes fail
- Image loading blocks ui sometimes
- Audio Library loading fails if class version changed (now automatically reloads)
- Audio Library import ui visibility and layout
- Audio Library logging errors when no saved library exists yet
- Shortcuts settings not being applied after restart
- Settings editors for complex values not understanding exact type (JVM erasure)
- Settings editors general null support
- Settings editors not applying value sometimes
- Settings editors not showing correct value for some observables
- Settings editors not showing correct warning tooltip text sometimes
- Settings editors UX
- GridView focus not applied properly sometimes
- GridView search style not discarded on cancel
- GridView search not selecting proper item sometimes
- GridView scrolling when selecting last row
- GridView cover size change throws exceptions
- Overlay settings change not taking effect
- Overlay blur affecting content in some scenarios
- Icon with text has wrong layout
- Check icon with one icon not distinguishing on/off state
- Starting some processes blocked until application stopped
- Progress indicator not centered in certain sizes
- Drag & Drop support for COPY/MOVE/LINK
- Writing certain custom fields' values not working correctly
- Widgets
    - `Tagger` widget displaying wrong cover in rare cases
    - `Tagger` widget displaying wrong cover size cases
- Song `COMMENT` field never reading as `<none>`
- View filter not working in some cases
- View filter closing instead of focusing on Ctrl+F
- Search text field clear button showing incorrectly sometimes
- Application search results are hard to disambiguate (show icons)
- Application search slower for lots of items
- Application search cell graphics layout
- Application search empty tooltip visible
- Application skin setting not restored properly
- Application rating skin not allowing null value sometimes
- Plugins
  - Non-running plugins are instantiated
  - `Waiffu2k` plugin `waiffu2k` binary config is not persisted
  - `Waiffu2k` plugin scaling coefficient not configurable
  - `Guide` is now a plugin and can be disabled completely
  - `Directory Search` does not find any results
  - `Directory Search` & `AppSearch` logging errors for inaccessible files
- JavaFX Screen.getScreens crashing in rare situations
- Popup window fixes
  - popup does not receive focus on show not even when clicked
  - popup not returning focus to previously focused OS window
  - popup not resizable by OS
  - popup initial size and position not correct sometimes
  - hiding popups with ESCAPE closes only focused popup
  - popup not handling key events sometimes (instead passing them to owner)
  - popup focus style not having effect
  - popup close throwing 'window already closed' exception
  - certain popup properties not styleable
  - help popups closing too eagerly
  
### Removed
- Runtime JVM options editing. Was experimental and proved unnecessary/complicated.
- `Spectrum` widget. Was experimental and incomplete
- `WebReader` widget. Was experimental and proved incomplete
- `Terminal` widget for its dependencies and maintenance overhead. Standalone terminal applications can do a better job.
- Application (?) icons with UX tooltips (use help/component help)
- Dependencies: javafx.web, javafx.fxml
- Annotation support for Config constraints, use delegated configurable properties and ConfigDefinition instead

## [v0.7.0]
### Added
- Initial release features