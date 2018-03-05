import org.apache.commons.math3.stat.regression.SimpleRegression;

/**
 * Created by Raz on 2018/3/2.
 * test
 */
public class Test {

    public static void main(String[] args) {
        double[][] data = { { 1, 3 }, {2, 5 }, {3, 7 }, {4, 9 }, {5, 11 }};
        SimpleRegression regression = new SimpleRegression(true);
        //the argument, false, tells the class not to include a constant
        regression.addData(data);

        System.out.println(regression.getIntercept());
        // displays intercept of regression line, since we have constrained the constant, 0.0 is returned

        System.out.println(regression.getSlope());
        // displays slope of regression line

        System.out.println(regression.getSlopeStdErr());
        // displays slope standard error

        System.out.println(regression.getInterceptStdErr() );
        // will return Double.NaN, since we constrained the parameter to zero

    }
}