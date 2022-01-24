
###### Styling
One of the pain-points is styling. The styling css file has, over time, become huge! It has 4500 lines of css, mode than JavaFX' **Modena**!
The size would not be an issue, if the css was not severely lacking in features. Variables, traits/mixins, etc.
I have been thinking about using some preprocessor, such as sass/less, but I am very reluctant to bring these technologies in.
Integrating them with the project would bring in some challenges, such as style reloading, which would require a compilation step.
In theory, **Kotlin** CSS DSL could be used as alternative.
However, I do not know which is worse - forcing the user write CSS, Kotlin or Less. I guess, ideally, all of those would be supported, but who's got time for that!
Plus, going this route would require a large effort to leverage the benefits - rewrite the 4500 lines of css to discover/define variables.
Sure, doing this would bring the styling up to a higher standard, but I do not think this will be ever worth it for SpitPlayer.
Few years ago, yes, now, not without extra pair of hands.

###### Documentation
Another pain-point is documentation. I keep postponing writing a user guide, with a good excuse - I do not know how should a good guide look like.
I want to start small and write guides for widgets first, but then I get tangled in a web of thoughts about the nature of metadata.
The issue is widget metadata, such as version, description or even shortcuts are defined in widget's code - in the Controller's companion object.
Now this was a thoughtful decision at the beginning, because the code packs everything together and no extra file reading is necessary.
The metadata is also guaranteed to be defined and well-formed. The code is not suited for extensive texts, but **Kotlin**'s `"""` is doing great job here.
However, the real issue is versioning and compatibility - because the metadata is in code, failure to load the widget's **class** will result in loss of all metadata.
This is clearly not a good design, as the user has no good way to troubleshoot potential issues, discover compatibility mismatch, and so on.
This is a trade-off situation and until now, I have been doing the most efficient thing - nothing.
I also am not sure if documentation/guide belongs inside the widget's directory or into application's. This also concerns the **CHANGELOG**.
Finally, some widgets are integral to the application, so they should be packaged along the application.
So, this needs to be carefully figured out.

###### I/O ui
Another painful area is the widget input/output system.

The I/O ui is currently tied to **Switch** container, which provides the ui move/zoom capability, which affects the **IO**.
Because of this, layouts, which do not use **Switch** container (docks and overlays) do not support input/output editing.
However, the obvious - separating the I/O ui from **Switch** to an actual top container, i.e. **Layout** - is problematic.
And the change must enable another capability - cross-window I/O ui. The obvious idea - having single I/O layer as screen overlay - is not as simple as it sounds.  
This is because, again, **Switch** container. In short, if there is screen I/O layer, it is no longer bound to the window content, which creates lots of serious issues:
- moving/zooming the content brings questions of how these should translate to I/O ui
- suddenly, content hidden outside window becomes clunky - raising questions of how to link to invisible/clipped widgets.
- screen overlay will also still require user to be able to interact with the underlying content, which will be technically difficult (replicating events)
- react to window move/resize
- handle window Z order (what if one of the windows is behind other application window?)
- handle windows laying on top of each other
- this would also require duplicating widget's controls, which will have to stay tied to widget's ui

In theory, click/focus transparent overlay could be used, but I have had difficulties using that in the past.
And there are other issues with I/O, such as displaying value generators (currently only accessible through context menu) or not-so-ideal force layout for labels.

###### Searchable combobox
This may sound trivial, but combobox with lots of items is very difficult to use.
Yes, **JavaFX** combobox allows editing the value as in text field, but this has no effect on the choices.
This functionality is useless, as it is provided better with custom text fields with custom autocomplete.
However, that is only suitable for unsealed sets - when the autocompletion only provides suggestions. There is no reason to allow editing when the set is closed.
This is elegantly handled in Intellij Idea, where combobox displays a filter if user types.
Implementing this with **JavaFX** **ComboBox** class turns out to be truly impossible (I tried).
Writing one's own **ComboBoxSkin** is non-trivial effort and even worse, ends up conflating styling with more redundant components.