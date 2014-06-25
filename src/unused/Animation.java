
package unused;

import java.util.ArrayList;
import java.util.List;
import static javafx.animation.Animation.INDEFINITE;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
 
/**
*
* @author thedoctor
*/
public class Animation extends Region{
 
BooleanProperty playingProperty = new SimpleBooleanProperty(false);
List<SequentialTransition> animation;
double barWidth = 5;
double barHeigth = 5;
int nBars;
int barLenght = 25; //nubmer of bars in the whole thing
double spacing = 0.8;
 
public Animation(double width, double height){
setHeight(height);
setWidth(width);
animation = new ArrayList();
barWidth = getWidth() / barLenght;
nBars = (int) Math.ceil(getWidth() / barWidth);
 
for (int x = 0; x < nBars; x++){
// 4/5 = spacing ( cutting in from the end of barWidth )
Rectangle r = new Rectangle(x * barWidth, 0, barWidth * spacing,
getHeight());
getChildren().add(x, r);
animation.add(fade(r, nBars, x, 5));
}
widthProperty().addListener((o, oldV, newV) -> {
resizeWi((int) Math.ceil(newV.doubleValue() / barWidth),
newV.doubleValue(), oldV.doubleValue(),
newV.doubleValue() / barLenght);
barWidth = newV.doubleValue() / barLenght;
});
heightProperty().addListener((o, oldV, newV) -> {
barHeigth = newV.doubleValue();
getChildren().stream().filter(c -> c instanceof Rectangle)
.map(c -> (Rectangle) c).forEach(r -> {
r.setHeight(barHeigth);
});
});
playingProperty.addListener((o, oldVal, newVal) -> {
if (newVal){
play();
}
else{
pause(); // change to stop() if you'd like
}
});
 
}
 
public final void resizeWi(int newSize, double newW, double oldW,
double newBW){
//// getChildren().clear();
//// animation.clear();
////// For tomorrow
// int delta;
// if (newSize > nBars){
// delta = newSize - nBars;
// }
//
// for (int x = 0; x < newSize; x++){
// Rectangle r = new Rectangle(x * barWidth, 0, barWidth * 0.8,
// getHeight());
// getChildren().add(x, r);
// animation.add(x, fade(r, nBars, x, 5));
// getNewAnimation(r, nBars, x, 5);
// }
 
// Currently this is increadibly ugly and I definitely plan to rework it when
// I'm not this sleepy.
getChildren().stream().filter(c -> c instanceof Rectangle)
.map(c -> (Rectangle) c).forEach(r -> {
System.out.println(newW);
r.setWidth(newBW * spacing);
r.setX((r.getX() / barWidth) * newBW);
});
nBars = newSize;
}
 
public final void play(){
animation.forEach(i -> i.play());
}
 
public final void pause(){
animation.forEach(i -> i.pause());
}
 
public final void stop(){
animation.forEach(i -> i.stop());
}
 
public boolean isPlaying(){
return playingProperty.getValue();
}
 
static SequentialTransition fade(Node node, int max, int delta, int dur){
double del = 0.01; // delay (avoids Duration = 0 too )
PauseTransition p0 = pt(delta * dur + del);
FadeTransition s1 = ft((max - delta) * dur + delta * del, 0, 0.8);
PauseTransition p2 = pt(dur * delta);
FadeTransition s2 = ft((max - delta) * del, 0.8, 1);
FadeTransition s3 = ft(delta * dur + del, 1, 0.06);
PauseTransition p4 = pt((max - delta) * 2 * dur);
FadeTransition s5 = ft((max - delta) * dur + del, 0.06, 0);
PauseTransition p6 = pt(delta * dur + del);
SequentialTransition a = new SequentialTransition(node, p0, s1, s2, p2,
s3,
p4, s5, p6);
a.setCycleCount(INDEFINITE);
return a;
}
 
void getNewAnimation(Node node, int max, int delta, int dur){
animation.forEach(e -> {
e = fade(node, max, delta, dur);
});
}
 
static FadeTransition ft(double dur, double start, double stop){
FadeTransition f = new FadeTransition(Duration.millis(dur * 10));
f.setFromValue(start);
f.setToValue(stop);
f.setCycleCount(1);
//f.setInterpolator(Interpolator.EASE_OUT);
return f;
}
 
static PauseTransition pt(double dur){
PauseTransition p = new PauseTransition(Duration.millis(dur * 10));
p.setCycleCount(1);
return p;
}
 
}