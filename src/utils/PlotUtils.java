package utils;

import org.knowm.xchart.QuickChart;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import structures.Token;

import java.util.Map;

public class PlotUtils {

    public static void plotHashMap(Map<String, Token> map) throws Exception {
        int l = map.size();
        double [] x = new double[l];
        double [] y = new double[l];
        MapUtils.getArraysFromMap(map, x, y);
        plotXYArray(x, y);
    }

    public static void plotXYArray(double [] xData, double [] yData)  throws Exception {
        // Create Chart
        XYChart chart = QuickChart.getChart("Zipf's Law", "Word rank by frequency", "Word frequency", "data", xData, yData);
        chart.getStyler().setYAxisLogarithmic(true);
        chart.getStyler().setXAxisLogarithmic(true);
        // Show it
        new SwingWrapper(chart).displayChart();
    }

}
