package analyzer;

import javafx.geometry.Pos;
import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InvalidFormatException;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;
import org.tartarus.snowball.ext.porterStemmer;
import structures.BigramLM;
import structures.LanguageModel;
import structures.Post;
import structures.Token;
import utils.MapUtils;

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;

public class LMAnalyzer {
    //N-gram to be created
    int m_N;

    //a hashset of punctuation
    HashSet<Character> m_punctuation;

    //a list of stopwords
    HashSet<String> m_stopwords;

    //you can store the loaded reviews in this arraylist for further processing
    ArrayList<Post> m_reviews;

    ArrayList<Post> m_test;

    //you might need something like this to store the counting statistics for validating Zipf's and computing IDF
    HashMap<String, Token> m_stats;

    HashMap<String, Integer> m_index_map;

    HashSet<String> m_vol;

    //we have also provided a sample implementation of language model in src.structures.LanguageModel
    Tokenizer m_tokenizer;

    //this structure is for language modeling
    LanguageModel m_langModel;


    public LMAnalyzer(String tokenModel, String puncFileAddress, String stopwordAdd, int N) throws InvalidFormatException, FileNotFoundException, IOException {
        m_N = N;
        m_reviews = new ArrayList<Post>();
        m_test = new ArrayList<>();
        m_punctuation = new HashSet<>();
        m_stopwords = new HashSet<>();
        m_tokenizer = new TokenizerME(new TokenizerModel(new FileInputStream(tokenModel)));

        m_stats = new HashMap<>();
        m_vol = new HashSet<>();

        //tricky
        //m_stopwords.add("NUM");

        loadPunctuation(puncFileAddress);
        //loadStopwords(stopwordAdd);
    }

    public void loadPunctuation(String filename) {
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

    public void addStopwords(Map<String, Token> sortedMap, int num) {
        int counter = 0;
        System.out.println("----------------Printing New Stopwords-----------------");
        for(String word: sortedMap.keySet()) {
            if(counter >= num) break;
            if(!m_stopwords.contains(word)) {
                m_stopwords.add(word);
                System.out.println(counter + "," + word);
                counter++;
            }
        }
    }

    public void applyStopwords() {
        Set<String> clone = new HashSet<>(m_stopwords);
        for(String word: clone) {
            if(m_stats.keySet().contains(word))
                m_stats.remove(word);
        }
    }

    public void removeRareWords() {
        Set<String> s = new HashSet<>(m_stats.keySet());
        for(String w: s) {
            if(!m_stats.containsKey(w)) continue;;
            Token t = m_stats.get(w);
            if(t.getDFValue()<50) {
                m_stats.remove(w);
            }
        }
    }

    // must be called after stopwords constructed.
    public void constructVal() {
        m_vol = new HashSet<>();
        for(String word : m_stats.keySet()) {
            if(!m_stopwords.contains(word)) {
                m_vol.add(word);
            }
        }
    }

    public void applyIDF(int total) {
        for(Map.Entry<String, Token> en: m_stats.entrySet()) {
            en.getValue().setIDF(total);
        }
    }

    public void analyzeDocument(JSONObject json) {
        try {
            JSONArray jarray = json.getJSONArray("Reviews");
            for (int i = 0; i < jarray.length(); i++) {
                Post review = new Post(jarray.getJSONObject(i));
                String[] tokens = Tokenize(review.getContent());
                // review.setTokens(tokens);

                /**
                 * HINT: perform necessary text processing here based on the tokenization results
                 * e.g., tokens -> normalization -> stemming -> N-gram -> stopword removal -> to vector
                 * The Post class has defined a "HashMap<String, Token> m_vector" field to hold the vector representation
                 * For efficiency purpose, you can accumulate a term's DF here as well
                 */

                String [] nom = new String[tokens.length];
                for(int j=0; j<tokens.length; j++) {
                    String word = tokens[j];
                    word = Normalization(word);
                    word = SnowballStemming(word);
                    nom[j] = word;
                }

                //TODO remove empty
                nom = MapUtils.removeEmpty(nom);
                if(nom.length == 0) return;

                if(m_N == 1) {
                    for(String word : nom) {
                        if(m_stopwords.contains(word)) continue;
                        m_langModel.addOneToModel(word);
                    }
                } else {
                    for(int j=0; j<nom.length-1; j++) {
                        if(m_stopwords.contains(nom[j+1])) continue;
                        ((BigramLM)m_langModel).addOneToModel(nom[j], nom[j+1]);
                    }
                }

                m_reviews.add(review);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void createLanguageModel(String train) {
        m_langModel = new LanguageModel(m_N);

        LoadDirectory(train, ".json");

        m_langModel.processModel();
    }

    /**
     * createLM
     * @param train
     * @param type 0 for linear, 1 for absolute
     */
    public void createLanguageModel(String train, LanguageModel ref, int type, double para) throws Exception {
        if(m_N == 1) throw new Exception("m_N can not be 1");
        m_langModel = new BigramLM(m_N, ref, type);

        LoadDirectory(train, ".json");

        if(type == 0) {
            ((BigramLM)m_langModel).processLinear(para);
        } else if(type == 1) {
            ((BigramLM)m_langModel).processAbsolute(para);
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
            //break;
        }
        size = m_reviews.size() - size;
        System.out.println("Loading " + size + " review documents from " + folder);
    }

    public void loadTest(String folder, String suffix) {
        File dir = new File(folder);
        for (File f : dir.listFiles()) {
            if (f.isFile() && f.getName().endsWith(suffix))
                analyzeTest(LoadJson(f.getAbsolutePath()));
            else if (f.isDirectory())
                LoadDirectory(f.getAbsolutePath(), suffix);
            // TODO break
            //break;
        }
    }

    public void addTestVol(String [] nom) {
        if(nom == null || nom.length == 0) return;
        for(String word : nom) {
            if(!m_vol.contains(word)) {
                m_vol.add(word);
            }
        }
    }

    public void analyzeTest(JSONObject json) {
        try {
            JSONArray jarray = json.getJSONArray("Reviews");
            for (int i = 0; i < jarray.length(); i++) {
                Post review =new Post(jarray.getJSONObject(i));
                String[] tokens = Tokenize(review.getContent());

                String [] nom = new String[tokens.length];
                for(int j=0; j<tokens.length; j++) {
                    String word = tokens[j];
                    word = Normalization(word);
                    word = SnowballStemming(word);
                    nom[j] = word;
                }

                //TODO remove empty
                nom = MapUtils.removeEmpty(nom);
                if(nom.length == 0) return;

                // remove stopWords
                String [] removed = MapUtils.removeStopwords(nom, m_stopwords);
                addTestVol(nom);

                review.setTokens(removed);
                m_test.add(review);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public Post getPostFromJson(JSONObject json) {
        Post review = new Post(json);
        review.setIndexMap(m_index_map);
        String [] newTokens = getTokens(review.getContent());
        review.setVecFromTokens(newTokens, m_stats);
        return review;
    }

    public String [] getTokens(String content) {
        String[] tokens = Tokenize(content);
        String [] nom = new String[tokens.length];
        for(int j=0; j<tokens.length; j++) {
            String word = tokens[j];
            word = Normalization(word);
            word = SnowballStemming(word);
            nom[j] = word;
        }

        //TODO remove empty
        nom = MapUtils.removeEmpty(nom);
        if(nom.length == 0) return null;

        String [] newTokens = new String[nom.length + nom.length-1];
        for(int j=0; j<nom.length; j++) {
            newTokens[j] = nom[j];
        }

        for(int j=0; j<nom.length-1; j++) {
            newTokens[nom.length+ j] = nom[j] + "-" + nom[j+1];
        }

        return newTokens;
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

    //sample code for demonstrating how to use Porter stemmer
    public String PorterStemming(String token) {
        porterStemmer stemmer = new porterStemmer();
        stemmer.setCurrent(token);
        if (stemmer.stem())
            return stemmer.getCurrent();
        else
            return token;
    }

    //sample code for demonstrating how to perform text normalization
    //you should implement your own normalization procedure here
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

    String[] Tokenize(String text) {
        return m_tokenizer.tokenize(text);
    }

    public void TokenizerDemon(String text) {
        System.out.format("Token\tNormalization\tSnonball Stemmer\tPorter Stemmer\n");
        for (String token : m_tokenizer.tokenize(text)) {
            System.out.format("%s\t%s\t%s\t%s\n", token, Normalization(token), SnowballStemming(token), PorterStemming(token));
        }
    }

    public void storeMSTATS(String filename) throws IOException {
        File file = new File(filename);
        synchronized (file) {
            FileWriter fw = new FileWriter(filename);
            for(Map.Entry<String, Token> e : m_stats.entrySet()) {
                String word = e.getKey();
                Token t = e.getValue();
                fw.write( word + ","+ t.getTTFValue() + "," + t.getDFValue() + "," + t.getIDFValue()+"\n");
            }
            fw.close();
        }
    }

    public void recoverMSTATS(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] fn = line.split(",");
                Token t = new Token(fn[0]);
                t.setTTFValue(Double.parseDouble(fn[1]));
                t.setDFValue(Double.parseDouble(fn[2]));
                t.setIDFValue(Double.parseDouble(fn[3]));
                m_stats.put(fn[0], t);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void insertHelper(Post [] max, Post insert) {
        for(int i=0; i<max.length; i++) {
            if(max[i] == null){
                max[i] = insert;
                return;
            }
            if(max[i].getCandidateSim()<insert.getCandidateSim()) {
                max[i] = insert;
                return;
            }
        }
    }

    public String sampleSentences(LanguageModel ref, int limit, ArrayList<Double> cur) throws Exception {
        if(limit<0) throw new Exception("limit can not be negative");

        StringBuilder sb = new StringBuilder();
        for(int i=0; i<limit-1; i++) {
            String pre = ((BigramLM)m_langModel).sampling("UNKNOWN", cur);
            sb.append(pre);
            sb.append(" ");
        }
        return sb.toString().trim();
    }

    public String uniSample(int limit, ArrayList<Double> cur) throws Exception {
        if(limit<0) throw new Exception("limit can not be negative");

        StringBuilder sb = new StringBuilder();
        for(int i=0; i<limit-1; i++) {
            String pre = m_langModel.sampling(cur);
            sb.append(pre);
            sb.append(" ");
        }
        return sb.toString().trim();
    }

    public static void generateTop10(BigramLM lm, String word) {
        System.out.println(Arrays.toString(lm.getTop10(word)));
    }

    public double [] evaluateLM(LanguageModel lm) throws Exception {
        double [] res = new double[m_test.size()];
        for(int i=0; i<m_test.size(); i++) {
            String [] tokens = m_test.get(i).getTokens();
            if(tokens == null || tokens.length == 0) continue;
            if(lm.m_N == 1)
                res[i] = lm.getPerplexity(m_test.get(i));
            else
                res[i] = ((BigramLM)lm).getPerplexity(m_test.get(i));
        }
        return res;
    }

    public static void main(String [] args) throws Exception {
        LMAnalyzer uniAnalyzer = new LMAnalyzer(
                "./data/Model/en-token.bin",
                "./data/punctuation",
                "./data/stopwords",
                1);
        uniAnalyzer.createLanguageModel("./Data/yelp/train");

        LMAnalyzer linearAnalyzer = new LMAnalyzer(
                "./data/Model/en-token.bin",
                "./data/punctuation",
                "./data/stopwords",
                2);
        linearAnalyzer.createLanguageModel("./Data/yelp/train", uniAnalyzer.m_langModel, 0, 0.9);

        LMAnalyzer absoluteAnalyzer = new LMAnalyzer(
                "./data/Model/en-token.bin",
                "./data/punctuation",
                "./data/stopwords",
                2);

        absoluteAnalyzer.createLanguageModel("./Data/yelp/train", uniAnalyzer.m_langModel, 1, 0.1);

        System.out.println("Finish create model");

        // generate top 10 from good.
        System.out.println("========= For Unigram model ===========");
        for(int i=0; i<10; i++) {
            ArrayList<Double> pro = new ArrayList<>();
            System.out.println(uniAnalyzer.uniSample(15, pro));
            printPro(pro);
        }

        // generate top 10 from good.
        System.out.println("========= For linear smooth Bigram model ===========");
        generateTop10((BigramLM) linearAnalyzer.m_langModel, "good");
        for(int i=0; i<10; i++) {
            ArrayList<Double> pro = new ArrayList<>();
            System.out.println(linearAnalyzer.sampleSentences(uniAnalyzer.m_langModel, 15, pro));
            printPro(pro);
        }

        System.out.println("========= For Absolute smooth Bigram model ===========");
        generateTop10((BigramLM) absoluteAnalyzer.m_langModel, "good");
        for(int i=0; i<10; i++) {
            ArrayList<Double> pro = new ArrayList<>();
            System.out.println(absoluteAnalyzer.sampleSentences(uniAnalyzer.m_langModel, 15, pro));
            printPro(pro);
        }


        System.out.println("Start analyse test");
        // part 3.
        LMAnalyzer test = new LMAnalyzer(
                "./data/Model/en-token.bin",
                "./data/punctuation",
                "./data/stopwords",
                1);

        test.loadTest("./data/yelp/test", ".json");

        System.out.println("Start reProcess");
        uniAnalyzer.m_langModel.addictSmooth(test.m_vol, 0.1);
        ((BigramLM) linearAnalyzer.m_langModel).reProcess();
        ((BigramLM) absoluteAnalyzer.m_langModel).reProcess();

        System.out.println("Start uniPer");
        double [] uniPer = test.evaluateLM(uniAnalyzer.m_langModel);
        analysePer(uniPer);

        System.out.println("Start linear");
        double [] linearPer = test.evaluateLM(linearAnalyzer.m_langModel);
        analysePer(linearPer);

        System.out.println("Start abso");
        double [] absoPer = test.evaluateLM(absoluteAnalyzer.m_langModel);
        analysePer(absoPer);
    }

    public static void analysePer(double [] pers) {
        Mean mean = new Mean();
        StandardDeviation sd = new StandardDeviation();
        double m = mean.evaluate(pers);
        double sdv = sd.evaluate(pers);
        System.out.println("Mean: "+m+", Standard Deviation: "+sdv);
    }

    public static void printPro(ArrayList<Double> pros) {
        double res = 1;
        for(Double pro: pros) {
            if(pro!=0) {
                res *= pro;
            }
        }
        System.out.println("The probability of this sentence is:" + res);
    }
}
