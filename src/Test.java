import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Raz on 2018/3/2.
 * test
 */
public class Test {

    public static void main(String[] args) {
        new SwingWrapper(getChart()).displayChart();
    }

    public static XYChart getChart() {

        // generates Log data
        List<Integer> xData = new ArrayList<Integer>();
        List<Double> yData = new ArrayList<Double>();
        for (int i = -3; i <= 3; i++) {
            xData.add(i);
            yData.add(Math.pow(10, i));
        }

        // Create Chart
        XYChart chart = new XYChartBuilder().width(800).height(600).title("Powers of Ten").xAxisTitle("Power").yAxisTitle("Value").build();

        // Customize Chart
        chart.getStyler().setChartTitleVisible(true);
        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNW);
        chart.getStyler().setYAxisLogarithmic(true);
        chart.getStyler().setXAxisLabelRotation(45);

        // chart.getStyler().setXAxisLabelAlignment(TextAlignment.Right);
        // chart.getStyler().setXAxisLabelRotation(90);
        // chart.getStyler().setXAxisLabelRotation(0);

        // Series
        chart.addSeries("10^x", xData, yData);

        return chart;
    }
}