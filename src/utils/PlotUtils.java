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
        plotLogXYArray(x, y);
    }

    public static void plotLogXYArray(double [] xData, double [] yData)  throws Exception {
        // Create Chart
        XYChart chart = QuickChart.getChart("Zipf's Law", "Word rank by frequency", "Word frequency", "data", xData, yData);
        chart.getStyler().setYAxisLogarithmic(true);
        chart.getStyler().setXAxisLogarithmic(true);
        // Show it
        new SwingWrapper(chart).displayChart();
    }

    public static void plot2DArrayZipfLaw(double [][] data) {
        // Create Chart
        double [] x = new double[data.length];
        double [] y = new double[data.length];
        MapUtils.seperate2D(data, x, y);
        XYChart chart = QuickChart.getChart("Zipf's Law", "Word rank by frequency", "Word frequency", "data", x, y);
        new SwingWrapper(chart).displayChart();
    }

    public static XYChart plot2D(double [][] data, String title, String xTitle, String yTitle, String seriesName) {
        double [] x = new double[data.length];
        double [] y = new double[data.length];
        MapUtils.seperate2D(data, x, y);
        XYChart chart = QuickChart.getChart(title, xTitle, yTitle, seriesName, x, y);
        new SwingWrapper(chart).displayChart();
        return chart;
    }

    public static XYChart addSeries(double [][] newData, String name, XYChart chart) {
        double [] x = new double[newData.length];
        double [] y = new double[newData.length];
        MapUtils.seperate2D(newData, x, y);
        chart.addSeries(name, x, y);
        return chart;
    }



}
