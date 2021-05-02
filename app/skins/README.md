# Skins

This document aims to help with skinning and anything that is related to it.

### Basics

Skinning/styling is done through css.

**A stylesheet** is a css file. It can apply to the whole application or only on some component or module. This is done
by attaching the stylesheet to that element either in code.

**Skin** is a stylesheet recognized by the application. There are [application skins](#application-skin) and [widget skins](#widget-skin). Application skin
can be specified in application settings. Skin can import stylesheets and thus extend other skin,
see [extending](#extending).

**Skin extension** is a stylesheet which is applied on top of skin. This allows combining styles flexibly without
creating skins solely for composing other skins. User can specify multiple extensions in application settings. Skin
extensions override skin styles and are applied in order user specifies them.

**Styleclass** is a name of a style, Denoted `.name`

**Pseudoclass** is a state a styleclass can be in. Denoted `.name:pseudoclass1:pseudoclass2:...`

### Skin creation

#### Tools

Preferably use an editor with css syntax highlighting Consult:

- [official css guide](http://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html)
- [Modena.css](https://gist.github.com/maxd/63691840fc372f22f470) to determine what values to override,
- [Main](Main) to determine what values to override,

### Application skin

- Skin must be a `.css` file (it may import/extend other skins).
- Located in [skins](.) directory and its own subdirectory with the same name.

  For example: `.../skins/MySkin/MySkin.css`
- Skin will be applied as **userAgentStylesheet** (see [extending](#extending))
- The name of the skin file will be used in the application as the name of the skin
- The skin can refer to external files (like images). It is recommended to put all required files into the same
  directory as skin's css file

### Widget skin

Some parts of the application or external widgets and the like can use their own skins. Those must be located in the
widget or module's respective directory. If the skin is named `skin.css` it is automatically picked up. Otherwise,
developer of those modules must manually specify/load those css files in code or .

#### Extending

Stylesheets can extend other stylesheets by importing them with `@import url("../skin-name/skin-name.css");`.
Application skins are considered **userAgentStylesheet**, which means:
- they provide all the styling. Unless the skin provides styling for all JavaFX controls, it should inherit either
  skin that inherits default javaFx userAgentStylesheet **Modena.css** (like **Main**: `@import url("../Main/Main.css");`
  or inherit it directly: `@import "com/sun/javafx/scene/control/skin/modena/modena.css";` 
- have lower priority compared to normal stylesheets (see [priority](#priority))

[Modena.css](https://gist.github.com/maxd/63691840fc372f22f470) always serves as the base skin. Any skin in the /skins
directory will automatically extend Modena skin This application however uses heavily edited versions of Modena and
defines custom controls. It is recommended to extend the skin [Main](Main), as it is a complete skin for the
application.

#### Priority

Styleable values of components take into consideration the priority of the origin the value comes from. The priorities
are defined in javadoc of `javafx.css.StyleableProperty` and are in increasing order:
- a style from a user agent stylesheet (`javafx.application.Application.setUserAgentStylesheet(String)`)
- value set from code (`javafx.css.StyleableProperty.setValue()` or `javafx.scene.Node.setOpacity(double)`)
- a style from a stylesheet in `javafx.scene.Scene.getStylesheets()` or `javafx.scene.Parent.getStylesheets()`
- a style from `javafx.scene.Node.setStyle(String)`

In other words programmatic values override skins, but stylesheets override even those.
Therefore: In order for programmatic values to be applied over skin values, all skin values must be defined in skin files. 

#### Reloading

Skin changes are automatically detected with file monitoring, and the skin is reapplied immediately. The changes in
imported skins however require application restart.

#### Tips

- use global variables defined within `.root {}` instead of redefining the colors throughout the skin. Example of this
  practice can be found in Modena.css, and the default skin of this application.
- document everything with comments

#### Performance

- skins can easily degrade application performance. Particularly effect such shadow or blur when overused (on text for
  example). Use them wisely. The performance impact is often subtle, but it adds up.
- Test performance by comparing your skin against the Default skin.

### CSS Basics

### Styleclass

An 'id' for all elements of certain type. Example: .button, .button-arrow

Sometimes you might see something like this: `.button > .label`. It simply means there is a label directly within a
button, and it doesn't have its own unique styleclass (like 'button-label'), but rather is accessed as subcomponent of
the button

The styleclass is specified in .fxml file or java code, native JavaFX controls have respective default styleclasses (
e.g. Label has `label`, TreeView has `tree-view` etc). Developers can make their own styleclasses.

### Pseudoclass

A state of the element. For example hover, focus, etc. Developer can define custom pseudoclasses for example
.playlist-item:unplayable

### Syntax

```css
.button {
    // normal state for all buttons
}

.button:hover {
    // set appearance for mouse hover state
    // do not repeat values, they will be copied from normal state
}

.button:hover > label {
    // set label appearance when button is in hover state
}

.button > .label:hover {
    // set label appearance when label is in hover state
}
```
