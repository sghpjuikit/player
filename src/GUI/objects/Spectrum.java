
package gui.objects;

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
public class Spectrum extends AnchorPane {
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
        bc.setCategoryGap(0); // try 1 as 
        bc.setVerticalGridLinesVisible(false);
        bc.setVerticalZeroLineVisible(false);
        bc.setHorizontalGridLinesVisible(false);
        bc.setHorizontalZeroLineVisible(false);
        bc.setTitle(null);
        
        // NOT WORKING... its there but changing .audiospectrum has no effect
        bc.getStyleClass().add("audiospectrum");
        
        // setup chart
//        bc.setTitle("Live Audio Spectrum Data");
//        xAxis.setLabel("Frequency Bands");
        xAxis.setBorder(null);
        xAxis.setTickLabelsVisible(false);
        xAxis.setTickMarkVisible(false);
        xAxis.setVisible(false);
        
        yAxis.setBorder(null);
        yAxis.setTickLabelsVisible(false);
        yAxis.setTickMarkVisible(false);
        yAxis.setVisible(false);
//        yAxis.setLabel("Magnitudes");
        yAxis.setTickLabelFormatter(null);
//        yAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxis,null,"dB"));
        
        // add starting data
        XYChart.Series<String,Number> series1 = new XYChart.Series<>();
//        series1.setName("Data Series 1");
        //noinspection unchecked
        series1Data = new XYChart.Data[64];
        String[] categories = new String[64];
        for (int i=0; i<series1Data.length; i++) {
            categories[i] = Integer.toString(i+1);
            series1Data[i] = new XYChart.Data<>(categories[i],50);
            series1.getData().add(series1Data[i]);
        }
        bc.getData().add(series1);
        
        this.getChildren().add(bc);
        AnchorPane.setBottomAnchor(bc,-20.0);
        AnchorPane.setTopAnchor(bc,-20.0);
        AnchorPane.setRightAnchor(bc,-20.0);
        AnchorPane.setLeftAnchor(bc,-20.0);
        
        
        audioSpectrumListener = (double timestamp, double duration, float[] magnitudes, float[] phases) -> {
            for (int i = 0; i < series1Data.length; i++) {
                series1Data[i].setYValue(magnitudes[i] + 60);
            }
        };
    }
    
    /**
     * Starts listening to the playback.
     */
    public void startListening() {
        PLAYBACK.addAudioSpectrumListener(audioSpectrumListener);
    }
    
    /**
     * Stops listening to the playback
     */
    public void stopListening() {
        PLAYBACK.removeAudioSpectrumListener(audioSpectrumListener);
    }
}
