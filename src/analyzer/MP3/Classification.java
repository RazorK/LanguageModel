package analyzer.MP3;

import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;
import structures.KNN.KNN;
import structures.NaiveBayes.NBModel;
import structures.Post;
import structures.Token;
import utils.CVUtils;
import utils.MapUtils;
import utils.PlotUtils;

import java.io.*;
import java.util.*;

/**
 * class for MP3
 */

public class Classification {

    //we have also provided a sample implementation of language model in src.structures.LanguageModel
    Tokenizer m_tokenizer;

    //a hashset of punctuation
    HashSet<Character> m_punctuation;

    //a list of stopwords
    HashSet<String> m_stopwords;

    //you can store the loaded reviews in this arraylist for further processing
    ArrayList<Post> m_reviews;

    HashMap<String, Token> m_stats;

    private int pos_num;
    private int neg_num;

    public Classification(String tokenModel, String puncFileAddress) throws IOException {
        m_stopwords = new HashSet<>();
        m_tokenizer = new TokenizerME(new TokenizerModel(new FileInputStream(tokenModel)));
        m_reviews = new ArrayList<>();
        m_stats = new HashMap<>();
        m_punctuation = new HashSet<>();

        pos_num = 0;
        neg_num = 0;

        loadPunctuation(puncFileAddress);
    }

    private void loadPunctuation(String filename) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                char punc = line.charAt(0);
                if (punc!=0)
                    m_punctuation.add(punc);
            }
            reader.close();
            System.out.format("Loading %d punctuation from %s\n", m_punctuation.size(), filename);
        } catch (IOException e) {
            System.err.format("[Error]Failed to open file %s!!", filename);
        }
    }

    String[] Tokenize(String text) {
        return m_tokenizer.tokenize(text);
    }

    public String Normalization(String token) {
        // remove all non-word characters
        // please change this to removing all English punctuation
        // token = token.replaceAll("\\W+", "");

        StringBuilder sb = new StringBuilder(token);
        for(int i=0; i<sb.length(); i++) {
            if(m_punctuation.contains(sb.charAt(i))) {
                sb.deleteCharAt(i--);
            }
        }
        token = sb.toString();

        // convert to lower case
        token = token.toLowerCase();

        // add a line to recognize integers and doubles via regular expression
        // and convert the recognized integers and doubles to a special symbol "NUM"
        token = token.replaceAll("\\d+(\\.\\d+)?", "NUM");

        return token;
    }

    //sample code for demonstrating how to use Snowball stemmer
    public String SnowballStemming(String token) {
        SnowballStemmer stemmer = new englishStemmer();
        stemmer.setCurrent(token);
        if (stemmer.stem())
            return stemmer.getCurrent();
        else
            return token;
    }

    public void loadStopwords(String filename) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                //it is very important that you perform the same processing operation to the loaded stopwords
                //otherwise it won't be matched in the text content
                line = Normalization(line);
                line = SnowballStemming(line);
                if (!line.isEmpty())
                    m_stopwords.add(line);
            }
            reader.close();
            System.out.format("Loading %d stopwords from %s\n", m_stopwords.size(), filename);
        } catch (IOException e) {
            System.err.format("[Error]Failed to open file %s!!", filename);
        }
    }

    // sample code for demonstrating how to recursively load files in a directory
    public void LoadDirectory(String folder, String suffix) {
        File dir = new File(folder);
        int size = m_reviews.size();
        for (File f : dir.listFiles()) {
            System.out.println(m_reviews.size());
            if (f.isFile() && f.getName().endsWith(suffix))
                analyzeDocument(LoadJson(f.getAbsolutePath()));
            else if (f.isDirectory())
                LoadDirectory(f.getAbsolutePath(), suffix);
            // TODO break
            // break;
        }
        size = m_reviews.size() - size;
        System.out.println("Loading " + size + " review documents from " + folder);
        System.out.println("Current Pos: "+ pos_num + ", Neg: " + neg_num);
    }

    public void analyzeDocument(JSONObject json) {
        try {
            JSONArray jarray = json.getJSONArray("Reviews");
            for (int i = 0; i < jarray.length(); i++) {
                Post review = new Post(jarray.getJSONObject(i));
                boolean pos = review.positive();

                if(pos) pos_num++; else neg_num++;

                String[] tokens = Tokenize(review.getContent());
                HashSet<String> doc_df = new HashSet<>();

                String [] nom = new String[tokens.length];
                for(int j=0; j<tokens.length; j++) {
                    String word = tokens[j];
                    word = Normalization(word);
                    word = SnowballStemming(word);
                    nom[j] = word;
                }

                nom = MapUtils.removeEmpty(nom);
                review.setTokens(nom);
                if(nom.length == 0) return;

                for(String word : nom) {
                    if(m_stats.containsKey(word)) {
                        Token temp = m_stats.get(word);
                        temp.setTTFValue(temp.getTTFValue()+1);
                    } else {
                        Token temp = new Token(word);
                        temp.setTTFValue(1);
                        m_stats.put(word, temp);
                    }

                    if(!doc_df.contains(word)) {
                        doc_df.add(word);
                    }
                }

                for(String word: doc_df) {
                    if(m_stats.containsKey(word)) {
                        Token t = m_stats.get(word);
                        t.setDFValue(t.getDFValue()+1);
                        if(pos) t.addPos(); else t.addNeg();
                    }
                }

                m_reviews.add(review);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //sample code for loading a json file
    public static JSONObject LoadJson(String filename) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
            StringBuffer buffer = new StringBuffer(1024);
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
            reader.close();
            return new JSONObject(buffer.toString());
        } catch (IOException e) {
            System.err.format("[Error]Failed to open file %s!", filename);
            e.printStackTrace();
            return null;
        } catch (JSONException e) {
            System.err.format("[Error]Failed to parse json file %s!", filename);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 1. filter out the token with low DF
     * 2. filter out the stop word
     * 3. calculate ig, chiq.
     * @param dfLimit limit for df value.
     */
    public void preProcess(double dfLimit) {

        // filter for DF and stop word.
        Set<String> copy = new HashSet<>(m_stats.keySet());
        for(String t: copy) {
            if(m_stopwords.contains(t) || m_stats.get(t).getDFValue() < 10) {
                m_stats.remove(t);
            }
        }

        System.out.println("After filter by stopword and DF value, size: " + m_stats.size());

        for(Token t : m_stats.values()) {
            t.process(pos_num, neg_num);
        }
    }

    public void featureSelection(Map<String, Token> igMap, Map<String, Token> chiqMap) {
        // feature select for each map
        Set<String> newig = new HashSet<>();
        int count = 0;
        for(String token:igMap.keySet()) {
            newig.add(token);
            if(++count == 5000) break;
        }
        System.out.println("Size of Ig map : " + newig.size());

        Set<String> newChiq = new HashSet<>();
        count = 0;
        for(Map.Entry<String, Token> en: chiqMap.entrySet()) {
            if(count++ > 5000 || en.getValue().getChis()<3.841) break;
            newChiq.add(en.getKey());
        }
        System.out.println("size of chiq map" + newChiq.size());

        // remove useless feature
        Set<String> temp = new HashSet<String> (m_stats.keySet());
        System.out.println("Before remove" + m_stats.size());
        for(String t:temp) {
            if(!newig.contains(t) && !newChiq.contains(t)) {
                m_stats.remove(t);
            }
        }

        System.out.println("After feature selection, features remain: "+ m_stats.size());
    }

    public void reviewSelection() {
        List<Integer> rm = new ArrayList<>();
        for(int i=0; i<m_reviews.size(); i++) {
            Post p = m_reviews.get(i);
            String [] ts = p.getTokens();
            for(String t: ts) {
                if(m_stats.containsKey(t)) {
                    p.addFeature(t);
                }
            }

            if(p.getFeatureLength() <= 5) {
                rm.add(i);
            }
        }

        System.out.println("Before remove: " + m_reviews.size());
        System.out.println("remove num: " + rm.size());
        for(int i = rm.size()-1; i>=0; i--) {
            m_reviews.remove((int)rm.get(i));
        }

        System.out.println("After remove, size of review: " + m_reviews.size() );

        System.out.println("After feature selection, review remains: " + m_reviews.size());
    }

    public static void printMap(Map<String, Token> map, boolean ig) {
        String name = ig? "Information Gain" : "Chi Square";
        System.out.println("Top 20 words selected by " + name + ": ");
        int i = 0;
        for(Map.Entry<String, Token> en : map.entrySet()) {
            if(++i == 20) break;
            System.out.println(en.getKey() + ", " + name + ": " + (ig?en.getValue().getIG() : en.getValue().getChis()));
        }
    }

    // Task2
    public NBModel nb;
    /**
     * construct two language model for NB.
     * add features to the model.
     */
    public void initNBModel() {
        nb = createModel(0.1);
    }

    public NBModel createModel(double smooth) {
        NBModel res = new NBModel(smooth);
        res.train(m_reviews);
        res.additiveSmooth(m_stats.keySet());
        return res;
    }

    public NBModel createModel(double smooth, List<Post> reviews) {
        NBModel res = new NBModel(smooth);
        res.train(reviews);
        res.additiveSmooth(m_stats.keySet());
        return res;
    }



    /**
     * getPR from the sorted map, which contains the FX for each post
     * @param map sorted map
     * @param limit fx limit
     * @return double array of length 2, first as recall, second as precision.
     */
    public double [] getPR(Map<Post, Double> map, double limit) {
        int TP = 0, FP = 0;

        int count = 0;
        for(Map.Entry<Post, Double> en: map.entrySet()) {
            if(count++ >= limit) break;
            if(en.getKey().positive()) TP ++; else FP++;
        }

        int FN = pos_num - TP, TN = neg_num - FP;
        double precision = (TP + 0.0)/(TP + FP);
        double recall = (TP + 0.0)/ (TP + FN);
        return new double[]{recall, precision};
    }

    public double [][] getRPArray(NBModel nb, int num, int step) {
        Map<Post, Double> map = nb.testFx(m_reviews);
        //System.out.println("reviews size: " + map.size());

        int total = (num-1)/step;
        //System.out.println(total);
        double [][] RParray = new double[total+1][2];

        int count = 0;
        for(int i=0; i<num; i+=step,count++) {
            //System.out.println(count);
            double [] temp = getPR(map, (count+1)*step);
            RParray[count][0] = temp[0];
            RParray[count][1] = temp[1];
        }
        return RParray;
    }

    public XYChart generatePRGraph(double smooth, int num, int step) {
        NBModel nb = createModel(smooth);

        double [][] RParray = getRPArray(nb, num, step);
        //System.out.println(Arrays.deepToString(RParray));

        XYChart chart = PlotUtils.plot2D(RParray, "Precision-Recall", "Recall", "Precision", smooth + "");
        return chart;

    }


    // Task 3
    public void applyIDF() {
        for(Map.Entry<String, Token> en : m_stats.entrySet()) {
            en.getValue().setIDF(m_reviews.size());
        }
    }

    HashMap<String, Integer> indexMap;
    public HashMap<String, Integer> getIndex() {
        if(indexMap == null) {
            HashMap<String, Integer> res = new HashMap<>();
            int index = 0;
            for(Map.Entry<String, Token> en : m_stats.entrySet()) {
                res.put(en.getKey(), index++);
            }
            indexMap = res;
            return res;
        }
        return indexMap;
    }

    public void applyVector() {
        applyIDF();
        HashMap<String, Integer> indexMap = getIndex();
        for(Post p: m_reviews) {
            p.calculateVec(indexMap, m_stats);
        }
    }

    public KNN getKNN(int k, int l) {
        KNN res = new KNN(m_reviews, 5, getIndex());
        res.generateRandom(l);
        res.generateCorpusVec();
        res.generateBucket();
        return res;
    }

    public static KNN getKNN(List<Post> reviews, int k, int l, HashMap<String, Integer> index) {
        KNN res = new KNN(reviews, k, index);
        res.generateRandom(l);
        res.generateCorpusVec();
        res.generateBucket();
        return res;
    }

    public List<Post> getQuery(String add) {
        File dir = new File(add);
        return analyzeQuery(LoadJson(dir.getAbsolutePath()));
    }

    private List<Post> analyzeQuery(JSONObject json) {
        List<Post> res = new ArrayList<>();
        try {
            JSONArray jarray = json.getJSONArray("Reviews");
            for (int i = 0; i < jarray.length(); i++) {
                Post review = new Post(jarray.getJSONObject(i));

                String[] tokens = Tokenize(review.getContent());

                String [] nom = new String[tokens.length];
                for(int j=0; j<tokens.length; j++) {
                    String word = tokens[j];
                    word = Normalization(word);
                    word = SnowballStemming(word);
                    nom[j] = word;
                }

                nom = MapUtils.removeEmpty(nom);
                review.setTokens(nom);
                review.setIndexMap(getIndex());
                review.setVecFromTokens(nom, m_stats);
                res.add(review);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return res;
    }

    //task 4
    public static void printResult(double [][][] res, int k_fold) {
        for(int i=0; i<2; i++) {
            for(int k=0; k<k_fold; k++) {
                System.out.println(res[i][k][0] + "," + res[i][k][1] + "," +res[i][k][2] );
            }
        }
    }
    /**
     * analyze result for task4
     * @param input 3-d double array, 1-d : 0 for NB, 1 for KNN; 2-d: k-fold, 3-d f1, precision, recall.
     */
    public static void analyzeResult(double [][][] input, int k_fold) {
        printResult(input, k_fold);

        double [][] print = new double[2][3];

        // i: 0 for nb, 1 for knn
        for(int i=0; i<2; i++) {
            // k for f1, precision, recall
            for(int k = 0; k<3; k++) {
                double res = 0;
                for(int j=0; j<k_fold; j++) {
                    res += input[i][j][k];
                }
                print[i][k] = res/k_fold;
            }
        }

        for(int i=0; i<2; i++) {
            for(int j=0; j<3; j++) {
                String name = i == 0? "Naive Bayes" : "KNN";
                String score;
                if(j==0) score = "F-1 Score"; else if(j==1) score = "Precision"; else score = "Recall";

                System.out.println("Average" + score + " for " + name + " Model: " + print[i][j]);
            }
        }
    }

    public static void main(String [] args) throws IOException {
        Classification analyzer = new Classification(
                "./data/Model/en-token.bin",
                "./data/punctuation");

        // Task1
        analyzer.loadStopwords("./data/stopwords");
        analyzer.LoadDirectory("./Data/yelp/train", ".json");
        analyzer.LoadDirectory("./Data/yelp/test", ".json");
        analyzer.preProcess(10);

        Map<String, Token> igMap = MapUtils.sortByIG(analyzer.m_stats);
        printMap(igMap, true);
        Map<String, Token> chiqMap = MapUtils.sortByChis(analyzer.m_stats);
        printMap(chiqMap, false);

        analyzer.featureSelection(igMap, chiqMap);
        analyzer.reviewSelection();

        // Task2

//        // 2.1
//        analyzer.initNBModel();
//        analyzer.nb.showMap();
//
//        // 2.2
//        XYChart chart = analyzer.generatePRGraph(0.1, 50000, 1000);
//        for(double s = 0.1; s<=10; s+= 1) {
//            NBModel nb = analyzer.createModel(s);
//            double [][] rp = analyzer.getRPArray(nb, 50000, 1000);
//            PlotUtils.addSeries(rp, "smooth of" + s, chart);
//        }
//        new SwingWrapper(chart).displayChart();

        // Task3
        analyzer.applyVector();

//        KNN knn = analyzer.getKNN(5,5);
//        knn.setDebug(true);
//        List<Post> query = analyzer.getQuery("./data/query.json");
//        long startTime=System.currentTimeMillis();
//        for(Post q : query) {
//            knn.predictFromAll(q);
//        }
//        long endTime=System.currentTimeMillis();
//        System.out.println("Run Time for KNN with full corpus： "+ (endTime-startTime) +"ms");
//
//        startTime=System.currentTimeMillis();
//        for(Post q:query) {
//            knn.predictFromBucket(q);
//        }
//        endTime=System.currentTimeMillis();
//        System.out.println("Run Time for KNN with bucket corpus： "+ (endTime-startTime) +"ms");

        // Task4
        //CrossValidation

//        int k_fold = 10;
//        CVUtils cv = new CVUtils(analyzer.m_reviews.size(), k_fold);
//        double [][][] result = new double [2][k_fold][3];
//        for(int i=0; i<k_fold; i++) {
//            System.out.println("Starting fold-" + i);
//            List<Post> train = CVUtils.getItems(analyzer.m_reviews, cv.getTrainIndex(i));
//            List<Post> test = CVUtils.getItems(analyzer.m_reviews, cv.getTestIndex(i));
//            NBModel nb = analyzer.createModel(0.1, train);
//            result[0][i] = NBModel.getFPR(nb.testFx(test));
//
//            KNN knn1 = getKNN(train, 5, 5, analyzer.getIndex());
//            result[1][i] = knn1.getFPR(test);
//        }
//
//        analyzeResult(result, k_fold);

        // Task 5.

        int l = 6;
        for(int k = 6; k<8; k++) {
            double [][] k_res = new double[10][3];
            CVUtils cv = new CVUtils(analyzer.m_reviews.size(), 10);
            for(int i=0; i<10; i++) {
                System.out.println("Starting fold-" + i);
                List<Post> train = CVUtils.getItems(analyzer.m_reviews, cv.getTrainIndex(i));
                List<Post> test = CVUtils.getItems(analyzer.m_reviews, cv.getTestIndex(i));

                KNN knn1 = getKNN(train, k, l, analyzer.getIndex());
                //knn1.setDebug(true);
                double [] res = knn1.getFPR(test);
                k_res[i] = res;
                System.out.println("k: " + k +", l:" +l + ", f-1: " + res[0] + ", precision: " + res[1]
                    + ", recall: " + res[2]);
            }
            printKFold(k_res);
        }

    }

    public static void printKFold(double[][] k_res) {
        double [] res = new double[3];
        for(int i=0; i<k_res.length; i++) {
            for(int j=0; j<3; j++) {
                res[j] += k_res[i][j];
            }
        }

        for(int i=0; i<3; i++) {
            res[i] = res[i]/10.0;
        }

        System.out.println("Printing k_fold avg result: " );
        System.out.println(Arrays.toString(res));
    }
}
