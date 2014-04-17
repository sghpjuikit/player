
package GUI.objects;

import AudioPlayer.playback.PLAYBACK;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.AnchorPane;
import javafx.scene.media.AudioSpectrumListener;

/**
 *
 * @author uranium
 */
public class Spectrum extends AnchorPane{
    private final XYChart.Data<String, Number>[] series1Data;
    private final AudioSpectrumListener audioSpectrumListener;
    private final BarChart<String, Number> bc;
    private final CategoryAxis xAxis;
    private final NumberAxis yAxis;
        
    public Spectrum() {
        //*********** create chart
        xAxis = new CategoryAxis();
        yAxis = new NumberAxis(0,50,10);
        bc = new BarChart<>(xAxis,yAxis);
        bc.setId("barAudioDemo");
        bc.setLegendVisible(false);
        bc.setAnimated(false);
        bc.setBarGap(0);
        bc.setCategoryGap(1);
        bc.setVerticalGridLinesVisible(false);
        // setup chart
        bc.setTitle("Live Audio Spectrum Data");
        xAxis.setLabel("Frequency Bands");
        yAxis.setLabel("Magnitudes");
        yAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxis,null,"dB"));
        // add starting data
        XYChart.Series<String,Number> series1 = new XYChart.Series<>();
        series1.setName("Data Series 1");
        //noinspection unchecked
        series1Data = new XYChart.Data[128];
        String[] categories = new String[128];
        for (int i=0; i<series1Data.length; i++) {
            categories[i] = Integer.toString(i+1);
            series1Data[i] = new XYChart.Data<>(categories[i],50);
            series1.getData().add(series1Data[i]);
        }
        bc.getData().add(series1);
        this.getChildren().add(bc);
        
        audioSpectrumListener = (double timestamp, double duration, float[] magnitudes, float[] phases) -> {
            for (int i = 0; i < series1Data.length; i++) {
                series1Data[i].setYValue(magnitudes[i] + 60);
            }
        };
        PLAYBACK.addAudioSpectrumListener(audioSpectrumListener);
    }
}
