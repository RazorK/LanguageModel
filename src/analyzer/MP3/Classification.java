package analyzer.MP3;

import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;
import structures.Post;
import structures.Token;
import utils.MapUtils;

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
                if(nom.length == 0) return;

                for(String word : nom) {
                    if(m_stats.containsKey(word)) {
                        Token temp = m_stats.get(word);
                        temp.setTTFValue(temp.getTTFValue()+1);

                        if(pos) temp.addPos(); else temp.addNeg();

                    } else {
                        Token temp = new Token(word);
                        temp.setTTFValue(1);
                        m_stats.put(word, temp);

                        if(pos) temp.addPos(); else temp.addNeg();
                    }

                    if(!doc_df.contains(word)) {
                        doc_df.add(word);
                    }
                }

                for(String word: doc_df) {
                    if(m_stats.containsKey(word)) {
                        Token t = m_stats.get(word);
                        t.setDFValue(t.getDFValue()+1);
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

        Set<String> newChiq = new HashSet<>();
        count = 0;
        for(Map.Entry<String, Token> en: chiqMap.entrySet()) {
            if(count++ > 5000 || en.getValue().getChis()<3.841) break;
            newChiq.add(en.getKey());
        }

        // remove useless feature
        Set<String> temp = new HashSet<String> (m_stats.keySet());
        for(String t:temp) {
            if(!newig.contains(t) && !newChiq.contains(t)) {
                m_stats.remove(t);
            }
        }

        System.out.println("After feature selection, features remain: "+ m_stats.size());
    }

    public void reviewSelection() {

    }

    public static void main(String [] args) throws IOException {
        Classification analyzer = new Classification(
                "./data/Model/en-token.bin",
                "./data/punctuation");

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
}
