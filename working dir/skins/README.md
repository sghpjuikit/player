# Skins

This document aims to help with skinning and anything that is related to it.

### Basics

- JavaFX supports changing graphical components through css
- A stylesheet (css file) can apply to the whole application or only on some component or module.
  This is done by attaching the css to that element either in .fxml file or in java code.

### Overriding

- Stylesheets can override each other
- for Java 8 and 9, Modena.css serves as the default skin
- this application however uses heavily edited versions of Modena to provide
  easier skin overriding. This default skin is always in effect but can be
  completely overriden.
- Any skin in the Skins directory will automatically override the values of the
  default skin or use them if it doesn't provide a new value
-> write only what you want to change

### Skin creation

#### Tools

- preferably use an editor with css syntax highlighting
- official css guide: http://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html
- See the default skin reference skin to determine what values to override, it includes all
  necessary classes and is directly derived from the original stylesheet 
  [Modena.css](https://gist.github.com/maxd/63691840fc372f22f470)
  
#### Requirements for a Skin

- skin must be single .css file
- located in /Skins directory and its own subdirectory with the same name
  
  for example:  .../skins/MySkin/MySkin.css

- the name of the skin file will be used in the application as the name of the skin
- the skin can refer to external files (like images). It is highly recommended to
  put all required files into the same directory as skin's css file

### Sub skinning

- some parts of the application or external widgets and the like can use their
  own skins locally. Those must be located in the widget or module's respective
  directory. Developer of those modules must manually link those css files in
  java code or .fxml file.
- some custom controls support dynamic skinning - by looking up and loading
  available external css.
  See /controls directory.

### Tips

- use global variables defined within .root{} instead of redefining the colors
  throughout the skin. Example of this practice can be found in Modena.css and the
  default skin of tis application.
- document everything with comments

### Performance

- skins can easily degrade application performance. Particularly effect such 
  shadow or blur when overused (with text for example). Use them wisely.
  The performance impact is often subtle but it adds up. 
- Test performance by comparing your skin against the Default skin.

## CSS Basics

### Styleclass

An 'id' for all elements of certain type. Example: .button, .button-arrow

Sometimes you might see something like this: `.button > .label`. It simply
means there is a label directly within a button and it doesn't have its own unique
styleclass (like 'button-label'), but rather is accessed as subcomponent of the
button

The Styleclass is specified in .fxml file or java code, native JavaFX controls have 
respective default styleclasses(e.g. Label has `label`, TreeView has `tree-view` etc).
Developers can make their own styleclasses.

Elements can have more styleclasses that override each other.

##### Syntax

.button {
    // here comes code definind appearance of element with styleclass button
}


## Pseudoclass

A state of the element. For example hover, focus, etc. 
Developer can define custom pseudoclasses for example .playlist-item:unplayable

##### Syntax

```css
.button {
    // normal state
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
