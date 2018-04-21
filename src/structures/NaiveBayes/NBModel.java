package structures.NaiveBayes;

import structures.LanguageModel;
import structures.Post;
import utils.MapUtils;

import java.util.*;

/**
 * model for Naive Bayes Classification Model
 */
public class NBModel {
    LanguageModel posModel;
    LanguageModel negModel;
    double delta;

    // switch for appear feature
    boolean withoutAppearFeature;

    public NBModel(double smoothdelta) {
        posModel = new LanguageModel(1);
        negModel = new LanguageModel(1);
        delta = smoothdelta;

        // TODO: modify here
        // default without appearfeature
        withoutAppearFeature = true;
    }

    public void setType(boolean appear) {
        withoutAppearFeature = !appear;
    }

    // logP(y=1) - logp(y=0)
    double zeroPara;

    public void setZeroPara(int pos, int neg) {
        double ppos = (pos+0.0)/(pos+neg);
        double pneg = 1-ppos;
        zeroPara = Math.log(ppos/pneg);
    }

    public void addToModel(String token, boolean pos) {
        LanguageModel ptr = (pos? posModel:negModel);

        if(withoutAppearFeature) ptr.addOneToModel(token);
        else ptr.addOneToAppearModel(token);
    }

    Set<String> totalVol;

    public void additiveSmooth(Set<String> newVol) {
        totalVol = newVol;
        posModel.addictSmooth(new HashSet<>(newVol), delta);
        negModel.addictSmooth(new HashSet<>(newVol), delta);

        processParameter();
    }

    public void train(List<Post> re) {
        int pos = 0, neg = 0;
        for(Post p : re) {
            if(p.positive()) pos++; else neg++;
            Iterator<String> it = p.getFeatureIt();
            while (it.hasNext()) {
                String f = it.next();
                addToModel(f, p.positive());
            }
        }
        setZeroPara(pos, neg);
    }

    /**
     * calculate log
     */
    Map<String, Double> map;
    public void processParameter() {
        map = new HashMap<>();
        for(String temp : totalVol) {
            map.put(temp, Math.log(posModel.getPro(temp)/negModel.getPro(temp)));
        }
    }

    /**
     * Get the evaluation from NB model
     * @param features list of words after preprocessing.
     * @return f(X)
     */
    public double getFX(List<String> features) {
        double res = 0;
        res += zeroPara;
        for(String t : features) {
            if(map.containsKey(t)) {
                res+= map.get(t);
            }
        }
        return res;
    }

    /**
     * test array of post based on NB model
     * @param testSet
     * @return
     */
    public Map<Post, Double> testFx(List<Post> testSet) {
        Map<Post, Double> temp = new HashMap<>();
        for(Post p : testSet) {
            temp.put(p, getFX(p.getFeatures()));
        }

        return MapUtils.sortByTokenValue(temp, (en1, en2) -> -en1.getValue().compareTo(en2.getValue()));
    }

    /**
     * get F1 score, precision, recall for result.
     * @param res
     * @return array of double, f1, precision, recall.
     */
    public static double [] getFPR(Map<Post, Double> res) {
        int tp = 0, pos = 0, fp = 0, neg = 0;
        for(Map.Entry<Post, Double> en: res.entrySet()) {
            if(en.getKey().positive()) pos++; else neg++;
            if(en.getValue()>=0) {
                if(en.getKey().positive()) tp++; else fp++;
            }
        }

        int fn = pos - tp, tn = neg - fp;
        double precision = (tp + 0.0)/ (tp + fp);
        double recall = (tp+0.0)/(tp + fn);
        double f1 = 2.0/(1.0/recall + 1.0/precision);
        return new double[] {f1, precision, recall};
    }

    // output
    public void showMap() {
        Map<String, Double> sorted = MapUtils.sortByLogRatio(map);
        List<String> keys = new ArrayList<>(sorted.keySet());
        System.out.println("Printing the top words by log ratio: ");
        System.out.println("Top 20 highest log pro ratio: ");
        for(int i=0; i<20; i++) {
            System.out.println(keys.get(i) + ", " + map.get(keys.get(i)));
        }

        System.out.println("Top 20 lowest log pro ratio: ");
        for(int i=keys.size()-1; i>=keys.size()-20; i--) {
            System.out.println(keys.get(i) + ", " + map.get(keys.get(i)));
        }
    }
}
