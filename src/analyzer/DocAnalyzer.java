/**
 *
 */
package analyzer;

import java.io.*;
import java.util.*;

import javafx.geometry.Pos;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;
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


    public DocAnalyzer(String tokenModel, String puncFileAddress, int N) throws InvalidFormatException, FileNotFoundException, IOException {
        m_N = N;
        m_reviews = new ArrayList<Post>();
        m_test = new ArrayList<>();
        m_punctuation = new HashSet<>();
        m_stopwords = new HashSet<>();
        m_tokenizer = new TokenizerME(new TokenizerModel(new FileInputStream(tokenModel)));

        m_stats = new HashMap<>();

        loadPunctuation(puncFileAddress);
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
        // TODO this can be improve by traverse stopword hashset.
        Set<String> clone = new HashSet<>(m_stats.keySet());
        for(String word: clone) {
            if(m_stopwords.contains(word))
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
                HashSet<String> doc_df = new HashSet<>();

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

                String [] newTokens = new String[nom.length + nom.length-1];
                for(int j=0; j<nom.length; j++) {
                    newTokens[j] = nom[j];
                }
                for(int j=0; j<nom.length-1; j++) {
                    newTokens[nom.length+ j] = nom[j] + "-" + nom[j+1];
                }

                for(String word : newTokens) {
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
                    }
                }

                m_reviews.add(review);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void generateIndexMap() {
        m_index_map = new HashMap<>();
        int index = 0;
        for(String word: m_stats.keySet()) {
            if(!m_index_map.containsKey(word)) {
                m_index_map.put(word, index++);
            }
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
            // break;
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
            System.out.println(m_test.size());
        }
    }

    public void analyzeTest(JSONObject json) {
        try {
            JSONArray jarray = json.getJSONArray("Reviews");
            for (int i = 0; i < jarray.length(); i++) {
                Post review = getPostFromJson(jarray.getJSONObject(i));
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

    public static void orimain(String [] args) throws Exception {
        DocAnalyzer analyzer = new DocAnalyzer(
                "./data/Model/en-token.bin",
                "./data/punctuation",
                2);
        //code for demonstrating tokenization and stemming
        //analyzer.TokenizerDemon("whatever I've practiced for 30 years in pediatrics, and I've never seen anything quite like this.");

        //entry point to deal with a collection of documents

        analyzer.LoadDirectory("./Data/yelp/train", ".json");
        // analyzer.LoadDirectory("./Data/yelp/test", ".json");

        // P1 1.1
        Map<String, Token> sortedMap;

        // sortedMap = MapUtils.sortByTokenValue(analyzer.m_stats);
        // MapUtils.exportMap(sortedMap, "./test");
        // MapUtils.printRegression(sortedMap);
        // PlotUtils.plotHashMap(sortedMap);

        // p1 1.2
        analyzer.loadStopwords("./data/stopwords");

        sortedMap = MapUtils.sortByDf(analyzer.m_stats);
        analyzer.addStopwords(sortedMap, 100);

        //analyzer.constructVal();

        // change to directly remove from m_stats
        System.out.println(analyzer.m_stats.size());
        analyzer.applyStopwords();

        System.out.println(analyzer.m_stats.size());
        analyzer.removeRareWords();


        System.out.println(analyzer.m_stats.size());
        sortedMap = MapUtils.sortByDf(analyzer.m_stats);

        System.out.println(analyzer.m_reviews.size());
        analyzer.applyIDF(analyzer.m_reviews.size());

        Object [] entryArray = sortedMap.entrySet().toArray();
        for(int i=0; i<100; i++) {
            int index = i>=50?entryArray.length-1-(i-50):i;
            Map.Entry<String, Token> entry = (Map.Entry<String, Token>) entryArray[index];

            System.out.println(index + "," + entry.getKey() + "," +entry.getValue().getIDFValue());
        }

        analyzer.applyIDF(analyzer.m_reviews.size());
        analyzer.storeMSTATS("./test");
    }

    public static void main(String[] args) throws Exception {
        DocAnalyzer analyzer = new DocAnalyzer(
                "./data/Model/en-token.bin",
                "./data/punctuation",
                2);

        analyzer.recoverMSTATS("./test");

//        // p1 1.3
//        analyzer.applyIDF(analyzer.m_reviews.size());


        // indexMap
        analyzer.generateIndexMap();

        analyzer.loadTest("./data/yelp/test", ".json");

        // query
        JSONObject query = LoadJson("./data/query.json");

//        Comparator<Post> comp = new Comparator<Post>() {
//            @Override
//            public int compare(Post l, Post r) {
//                if(l.getCandidateSim() == r.getCandidateSim()) return 0;
//                if(l.getCandidateSim() < r.getCandidateSim()) return -1;
//                else if(l.getCandidateSim()>r.getCandidateSim()) return 1;
//                return 0;
//            }
//        };

        JSONArray jarray = query.getJSONArray("Reviews");
        for (int i = 0; i < jarray.length(); i++) {
            Post q = analyzer.getPostFromJson(jarray.getJSONObject(i));
            Post [] max = new Post[3];
            for(Post can: analyzer.m_test) {
                double simi = q.similiarity(can);
                can.setCandidateValue(simi);
                insertHelper(max, can);
            }
            System.out.println("=========================================================");
            System.out.println("Printing Corresponding Post for query" + i);
            for(int j=0; j<max.length; j++) {
                max[j].printPost();
                System.out.println("Cosine Similarity: " + max[j].getCandidateSim());
            }
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
}
