package analyzer.MP3;

import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.jfree.chart.plot.Plot;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;
import structures.NaiveBayes.NBModel;
import structures.Post;
import structures.Token;
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
        System.out.println("reviews size: " + map.size());

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

        // 2.1
//        analyzer.initNBModel();
//        analyzer.nb.showMap();

        // 2.2
        XYChart chart = analyzer.generatePRGraph(0.1, 50000, 1000);
        for(double s = 0.1; s<=10; s+= 1) {
            NBModel nb = analyzer.createModel(s);
            double [][] rp = analyzer.getRPArray(nb, 50000, 1000);
            PlotUtils.addSeries(rp, "smooth of" + s, chart);
        }
        new SwingWrapper(chart).displayChart();
    }
}
