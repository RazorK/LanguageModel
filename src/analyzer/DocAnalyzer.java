/**
 *
 */
package analyzer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;
import org.tartarus.snowball.ext.porterStemmer;

import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InvalidFormatException;
import structures.LanguageModel;
import structures.Post;
import structures.Token;
import utils.MapUtils;
import utils.PlotUtils;

/**
 * @author hongning
 *         Sample codes for demonstrating OpenNLP package usage
 *         NOTE: the code here is only for demonstration purpose,
 *         please revise it accordingly to maximize your implementation's efficiency!
 */
public class DocAnalyzer {
    //N-gram to be created
    int m_N;

    //a list of stopwords
    HashSet<String> m_stopwords;

    //you can store the loaded reviews in this arraylist for further processing
    ArrayList<Post> m_reviews;

    //you might need something like this to store the counting statistics for validating Zipf's and computing IDF
    HashMap<String, Token> m_stats;

    //we have also provided a sample implementation of language model in src.structures.LanguageModel
    Tokenizer m_tokenizer;

    //this structure is for language modeling
    LanguageModel m_langModel;

    //a hashset of punctuation
    HashSet<Character> m_punctuation;

    public DocAnalyzer(String tokenModel, String puncFileAddress, String stopWordsAddress, int N) throws InvalidFormatException, FileNotFoundException, IOException {
        m_N = N;
        m_reviews = new ArrayList<Post>();
        m_punctuation = new HashSet<>();
        m_stopwords = new HashSet<>();
        m_tokenizer = new TokenizerME(new TokenizerModel(new FileInputStream(tokenModel)));

        m_stats = new HashMap<>();

        loadPunctuation(puncFileAddress);
        loadStopwords(stopWordsAddress);
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

    //sample code for loading a list of stopwords from file
    //you can manually modify the stopword file to include your newly selected words
    public void loadStopwords(String filename) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                //it is very important that you perform the same processing operation to the loaded stopwords
                //otherwise it won't be matched in the text content
                line = SnowballStemming(Normalization(line));
                if (!line.isEmpty())
                    m_stopwords.add(line);
            }
            reader.close();
            System.out.format("Loading %d stopwords from %s\n", m_stopwords.size(), filename);
        } catch (IOException e) {
            System.err.format("[Error]Failed to open file %s!!", filename);
        }
    }

    public void analyzeDocument(JSONObject json) {
        try {
            JSONArray jarray = json.getJSONArray("Reviews");
            for (int i = 0; i < jarray.length(); i++) {
                Post review = new Post(jarray.getJSONObject(i));
                String[] tokens = Tokenize(review.getContent());
                review.setTokens(tokens);

                /**
                 * HINT: perform necessary text processing here based on the tokenization results
                 * e.g., tokens -> normalization -> stemming -> N-gram -> stopword removal -> to vector
                 * The Post class has defined a "HashMap<String, Token> m_vector" field to hold the vector representation
                 * For efficiency purpose, you can accumulate a term's DF here as well
                 */

                for(String word : tokens) {
                    word = Normalization(word);
                    word = SnowballStemming(word);
                    if(m_stats.containsKey(word)) {
                        Token temp = m_stats.get(word);
                        temp.setValue(temp.getValue()+1);
                    } else {
                        Token temp = new Token(word);
                        temp.setValue(1);
                        m_stats.put(word, temp);
                    }
                }
                m_reviews.add(review);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void createLanguageModel() {
        m_langModel = new LanguageModel(m_N, m_stats.size());

        for (Post review : m_reviews) {
            String[] tokens = Tokenize(review.getContent());
            /**
             * HINT: essentially you will perform very similar operations as what you have done in analyzeDocument()
             * Now you should properly update the counts in LanguageModel structure such that we can perform maximum likelihood estimation on it
             */
        }
    }

    //sample code for loading a json file
    public JSONObject LoadJson(String filename) {
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
            // break;
        }
        size = m_reviews.size() - size;
        System.out.println("Loading " + size + " review documents from " + folder);
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
                sb.deleteCharAt(i);
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

    public static void main(String[] args) throws Exception {
        DocAnalyzer analyzer = new DocAnalyzer(
                "./data/Model/en-token.bin",
                "./data/punctuation",
                "./data/stopwords",
                2);
        //code for demonstrating tokenization and stemming
        //analyzer.TokenizerDemon("I've practiced for 30 years in pediatrics, and I've never seen anything quite like this.");

        //entry point to deal with a collection of documents

        Map<String, Token> sortedMap;
        analyzer.LoadDirectory("./Data/yelp/train", ".json");
        analyzer.LoadDirectory("./Data/yelp/test", ".json");

        sortedMap = MapUtils.sortByTokenValue(analyzer.m_stats);
        double [][] data = new double[sortedMap.size()][2];
        MapUtils.get2DArrayFromMap(sortedMap, data);

        SimpleRegression regression = new SimpleRegression();
        regression.addData(data);
        System.out.println("Linear Regression Slope: "+ regression.getSlope());
        System.out.println("Linear Regression Interception" + regression.getIntercept());
        // MapUtils.exportMap(sortedMap, "./test");
        PlotUtils.plotHashMap(sortedMap);
    }
}
