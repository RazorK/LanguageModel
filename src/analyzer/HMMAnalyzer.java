package analyzer;

import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;
import structures.HMM.*;
import structures.Pair;
import utils.CVUtils;

import java.io.*;
import java.util.*;

public class HMMAnalyzer {
    HashMap<String, Word> wordMap;
    HashMap<String, Tag> tagMap;

    // sentence storage
    List<List<Pair<Word, Tag>>> sentences;

    // start tag
    StartTag start;

    UnknownTag unTag;

    public HMMAnalyzer() {
        // initialize variable
        wordMap = new HashMap<>();
        tagMap = new HashMap<>();
        sentences = new ArrayList<>();

        start = new StartTag();
        unTag = new UnknownTag();
    }

    public HMMAnalyzer(List<List<Pair<Word, Tag>>> sentences) {
        wordMap = new HashMap<>();
        tagMap = new HashMap<>();
        start = new StartTag();
        unTag = new UnknownTag();

        for(int i=0; i<sentences.size(); i++) {
            processFromSentence(sentences.get(i));
        }
    }

    // process Sentence
    public void processFromSentence(List<Pair<Word, Tag>> list) {
        if(list.size() <= 1) return;

        // process each(t->t, t->w)
        boolean first = true;
        for(int i=0; i<list.size(); i++) {
            Pair p = list.get(i);
            Tag tt = (Tag) p.getRight();
            Word ww = (Word) p.getLeft();

            Tag myTag;
            Word myWord;

            if(tagMap.containsKey(tt.getStr())) {
                myTag = tagMap.get(tt.getStr());
            } else {
                myTag = new Tag(tt.getStr());
                tagMap.put(tt.getStr(), myTag);
            }

            if(wordMap.containsKey(ww.getWord())) {
                myWord = wordMap.get(ww.getWord());
            } else {
                myWord = new Word(ww.getWord());
                wordMap.put(ww.getWord(), myWord);
            }

            // add word
            myTag.addWord(ww.getWord());

            // add tag
            if(first) {
                start.addTag(tt.getStr());
                first = false;
            } else {
                tagMap.get(list.get(i-1).getRight().getStr()).addTag(tt.getStr());
            }
        }
    }

    // processing line
    public void processLine(String line, List<Pair<Word, Tag>> l) {
        line = line.trim();
        if(line.equals("")) return;
        if(line.charAt(0) == '[') {
            line = line.substring(1, line.length()-1);
            line = line.trim();
        }
        if(line.equals("")) return;
        String [] seg = line.split(" ");
        for (String aSeg : seg) {
            aSeg = aSeg.trim();
            if(aSeg.equals("")) continue;
            String[] part = aSeg.split("/");
            if(part.length!=2 || part[0].equals("") || part[1].equals("")) continue;

            // word
            String w = part[0];

            // Tag
            String t = part[1];
            if (t.contains("|")) {
                String [] tags = t.split("|");
                for(String tag : tags) {
                    processWT(w, tag, l);
                }
            } else {
                processWT(w, t, l);
            }
        }
    }

    // process Sentence
    public void processSentence(List<Pair<Word, Tag>> list) {
        if(list.size() <=1) return;

        // add to storage
        sentences.add(list);

        // process each(t->t, t->w)
        boolean first = true;
        for(int i=0; i<list.size(); i++) {
            Pair p = list.get(i);
            Tag tt = (Tag) p.getRight();
            Word ww = (Word) p.getLeft();

            // add word
            tt.addWord(ww.getWord());

            // add tag
            if(first) {
                start.addTag(tt.getStr());
                first = false;
            } else {
                list.get(i-1).getRight().addTag(tt.getStr());
            }
        }
    }

    private void processWT(String word, String tag, List<Pair<Word, Tag>> l) {
        Word w;
        Tag t;
        if(!wordMap.containsKey(word)) {
            w = new Word(word);
            wordMap.put(word, w);
        } else {
            w = wordMap.get(word);
        }

        if(tagMap.containsKey(tag)) {
            t = tagMap.get(tag);
        } else {
            t = new Tag(tag);
            tagMap.put(tag, t);
        }

        l.add(new Pair<>(w,t));
    }

    public void analyzeDocument(String add) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(add), "UTF-8"));
        String line;

        List<Pair<Word, Tag>> sentence = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            if(line.equals("")) {
                processSentence(sentence);
                sentence = new ArrayList<>();
            } else {
                processLine(line, sentence);
            }
        }
        reader.close();
    }


    // requirement: , w_delta = 0.1, tag_delta = 0.5
    public void tagSmoothing(double w_delta, double t_delta) {
        start.additiveSmoothing(t_delta);
        for(Tag t : tagMap.values()) {
            if(t.getStr().equals("|")) {
                System.out.println("tst");
            }
            t.additiveSmoothing(w_delta, t_delta, new HashSet<>(wordMap.keySet()), new HashSet<>(tagMap.keySet()));
        }
    }

    // requirement: , w_delta = 0.1, tag_delta = 0.5
    public void tagSmoothing(double w_delta, double t_delta, HashSet<String> wordVol, HashSet<String> tagVol) {
        start.additiveSmoothing(t_delta);
        for(Tag t : tagMap.values()) {
            t.additiveSmoothing(w_delta, t_delta, wordVol, tagVol);
        }
        unTag.additiveSmoothing(w_delta, t_delta, wordVol, tagVol);
    }

    // sample code for demonstrating how to recursively load files in a directory
    public void loadDirectory(String folder, String suffix) throws IOException {
        File dir = new File(folder);
        int count = 0;
        for (File f : dir.listFiles()) {
            if (f.isFile() && f.getName().endsWith(suffix)) {
                analyzeDocument(f.getAbsolutePath());
            }
            else if (f.isDirectory())
                loadDirectory(f.getAbsolutePath(), suffix);
        }

        System.out.println("Total sentences number: "+sentences.size());
    }

    public void printTopWords(String t, int limit) {
        Tag tt = tagMap.get(t);
        System.out.println("Top "+limit+" words of Tag "+t+" is:");
        StringBuilder sb = new StringBuilder();
        String [] l = tt.topWords(limit);
        for(int i=0; i<limit; i++) {
            sb.append(l[i]);
            sb.append(" ");
        }
        System.out.println(sb.toString());
    }

    public void printTopTags(String t, int limit) {
        Tag tt = tagMap.get(t);
        System.out.println("Top "+limit+" tags of Tag "+t+" is:");
        StringBuilder sb = new StringBuilder();
        String [] l = tt.topTags(limit);
        for(int i=0; i<limit; i++) {
            sb.append(l[i]);
            sb.append(" ");
        }
        System.out.println(sb.toString());
    }

    // Viterbi Algorithm Implementation
    public Trellis POSTagging(List<Pair<Word, Tag>> sentence) {
        String [] words = getStringsetFromSentence(sentence);
        if(words.length<=1) return null;
        Map<Tag, Trellis> map = new HashMap<>();

        // init map
        for(Tag t : tagMap.values()) {
            Trellis tr = new Trellis(start);
            map.put(t, tr);
            tr.addWord(wordMap.get(words[0]), t);
        }

        // TODO
        // unknown tag
        //map.put(unTag, new Trellis(start));

        // Viterbi
        for(int i=1; i<words.length; i++) {
            String word = words[i];
            HashMap<Tag, Tag> bestTag =  new HashMap<>();

            for(Tag ct : tagMap.values()) {
                double maxLogPro = Integer.MIN_VALUE;
                Tag maxTag = null;
                for(Tag pt : tagMap.values()) {
//                    if(pt.getStr().equals("|"))
//                        System.out.println("What");
                    //System.out.println("PT: " + pt.getStr() +", CT:"+ct.getStr() + ", Word: "+word);
                    double temp = map.get(pt).getWTLogPro(wordMap.get(word), ct);
                    //System.out.println(temp);
                    if(temp>=maxLogPro) {
                        maxTag = pt;
                        maxLogPro = temp;
                    }
                }
                bestTag.put(ct, maxTag);
            }

            Map<Tag, Trellis> newMap = new HashMap<>();
            for(Tag ct: tagMap.values()) {
                Trellis tre = map.get(bestTag.get(ct)).clone();
                tre.addWord(wordMap.get(word), ct);
                newMap.put(ct, tre);
            }
            map = newMap;
        }

        // after finish, find the most likely pos tagging
        double maxLogPro = Integer.MIN_VALUE;
        Trellis res = null;
        for(Trellis t : map.values()) {
            if(t.getLogPro() > maxLogPro) {
                maxLogPro = t.getLogPro();
                res = t;
            }
        }

        if(res == null) {
            throw new RuntimeException("Return Trellis shouldn't be null");
        }
        if(res.size() != words.length) {
            System.out.println(res.size() + "," + words.length);
            throw new RuntimeException("what'sup");
        }
        return res;
    }

    public HashSet<String> getTagStringSet() {
        return new HashSet<>(tagMap.keySet());
    }

    public HashSet<String> getWordStringSet() {
        return new HashSet<String>(wordMap.keySet());
    }

    public List<List<Pair<Word, Tag>>> getSentences() {
        return sentences;
    }

    public static HMMAnalyzer generateCVAnalyzer(HMMAnalyzer ref, double w_delta, double t_delta, CVUtils cv, int round) {
        List<List<Pair<Word, Tag>>> train = CVUtils.getItems(ref.getSentences(), cv.getTrainIndex(round));
        HMMAnalyzer res = new HMMAnalyzer(train);
        res.tagSmoothing(w_delta, t_delta, ref.getWordStringSet(), ref.getTagStringSet());

        return res;
    }

    public static String[] getStringsetFromSentence(List<Pair<Word, Tag>> sentence) {
        String [] res = new String[sentence.size()];
        for(int i=0; i<sentence.size(); i++) {
            res[i] = sentence.get(i).getLeft().getWord();
        }
        return res;
    }

    public void analyseTest(List<List<Pair<Word, Tag>>> test, int [][] matrix, HashMap<String, Integer> indexMap) {
        for(int i=0; i<test.size(); i++) {
            Trellis tre = POSTagging(test.get(i));
            if(tre == null) continue;

            for(int j=0; j<tre.size(); j++) {
                //System.out.print("Truth"+test.get(i).get(j).getRight().getStr());
                //System.out.println("Predict"+tre.getTag(j).getStr());
                if(!indexMap.containsKey(test.get(i).get(j).getRight().getStr()) || !indexMap.containsKey(tre.getTag(j).getStr())) continue;
                int truth = indexMap.get(test.get(i).get(j).getRight().getStr());
                int predict = indexMap.get(tre.getTag(j).getStr());
                matrix[predict][truth]++;
            }
        }
    }

    public static double accuracy(int [][] matrix) {
        long total = 0;
        for(int i=0; i<matrix.length; i++) {
            for(int j=0; j<matrix[0].length; j++) {
                total += matrix[i][j];
            }
        }

        long correct = 0;
        for(int i=0; i<matrix.length; i++) {
            correct += matrix[i][i];
        }
        return (correct+0.0)/total;
    }

    public static double precision(int [][] matrix, int index) {
        if(index>=matrix.length) return -1;
        long total = 0;
        for(int i=0; i<matrix.length; i++) {
            total += matrix[i][index];
        }
        if(total == 0) return 0;
        return (matrix[index][index]+0.0)/total;
    }

    public static double recall(int [][] matrix, int index) {
        if(index>=matrix.length) return -1;
        long total = 0;
        for(int i=0; i<matrix.length; i++) {
            total += matrix[index][i];
        }
        if(total == 0) return 0;
        return (matrix[index][index]+0.0)/total;
    }

    public static double avg(List<Double> l) {
        double tt = 0;
        for(int i=0; i<l.size(); i++) {
            tt += l.get(i);
        }
        return tt/l.size();
    }

    public Trellis generateSentence(int limit) {
        Trellis res = new Trellis(start);
        Tag preTag = null;
        for(int i=0; i<limit; i++) {
            ArrayList<Double> tagPro = new ArrayList<>();
            ArrayList<Double> wordPro = new ArrayList<>();
            String TagStr = null;
            do {
                TagStr = (preTag == null? start:preTag).tagLM.sampling(tagPro);
            } while(TagStr.equals("UNKNOWN"));

            Tag t = tagMap.get(TagStr);
            preTag = t;
            String word = null;
            do {
                word = t.wordLM.sampling(wordPro);
            } while(word.equals("UNKNOWN"));

            Word w = wordMap.get(word);
            res.addWord(w, t);
        }
        return res;
    }


    public static void main(String [] args) throws IOException {
        // part 1
//        HMMAnalyzer analyzer = new HMMAnalyzer();
//        String address = "./data\\postag\\tagged";
//        analyzer.loadDirectory(address, "pos");
//        analyzer.tagSmoothing(0.1, 0.5);
//        analyzer.printTopWords("NN", 10);
//        analyzer.printTopTags("VB", 10);
//
//        // part 2
//        int foldNum = 5;
//        HMMAnalyzer [] list = new HMMAnalyzer [foldNum];
//        CVUtils cv = new CVUtils(analyzer.getSentences().size(), foldNum);
//
//        // index preparation
//        HashMap<String, Integer> indexMap = new HashMap<>();
//        int index = 0;
//        for(Tag t : analyzer.tagMap.values()) {
//            indexMap.put(t.getStr(), index++);
//        }
//
//        // init matrix, already inited to 0
//        int [][] matrix = new int[indexMap.size()][indexMap.size()];
//
//
//        for(int i=0; i<foldNum; i++) {
//            HMMAnalyzer cur = generateCVAnalyzer(analyzer, 0.1, 0.5, cv, i);
//            list[i] = cur;
//            List<List<Pair<Word, Tag>>> test = CVUtils.getItems(analyzer.getSentences(), cv.getTestIndex(i));
//            cur.analyseTest(test, matrix, indexMap);
//        }
//        //System.out.println(Arrays.deepToString(matrix));
//
//        List<Double> precisions = new ArrayList<>();
//        List<Double> recalls = new ArrayList<>();
//        for(int i=0; i<matrix.length; i++) {
//            precisions.add(precision(matrix, i));
//            recalls.add(recall(matrix, i));
//        }
//
//        String [] requirement = new String[] {"NN", "VB", "JJ", "NNP"};
//        for(String tagS : requirement) {
//            int ii = indexMap.get(tagS);
//            System.out.println("Results for "+tagS);
//            System.out.println("precision:" + precisions.get(ii));
//            System.out.println("recall: " + recalls.get(ii));
//        }
//
//        System.out.println("Overall accuracy: "+ accuracy(matrix));
//        System.out.println("Overall precision: "+ avg(precisions));
//        System.out.println("Overall recall: "+ avg(recalls));

        // parameter tuning

//        double max_w_delta = 0.01;
//        double max_t_delta = 0.01;
//        double max_accuracy = -1;
//        for(double w_delta = 0.01; w_delta<=0.03; w_delta+= 0.01) {
//            for(double t_delta = 0.75; t_delta<=0.85; t_delta += 0.05) {
//                int foldNum = 5;
//                HMMAnalyzer [] list = new HMMAnalyzer [foldNum];
//                CVUtils cv = new CVUtils(analyzer.getSentences().size(), foldNum);
//
//                // index preparation
//                HashMap<String, Integer> indexMap = new HashMap<>();
//                int index = 0;
//                for(Tag t : analyzer.tagMap.values()) {
//                    indexMap.put(t.getStr(), index++);
//                }
//
//                // init matrix, already inited to 0
//                int [][] matrix = new int[indexMap.size()][indexMap.size()];
//
//
//                for(int i=0; i<foldNum; i++) {
//                    HMMAnalyzer cur = generateCVAnalyzer(analyzer, w_delta, t_delta, cv, i);
//                    list[i] = cur;
//                    List<List<Pair<Word, Tag>>> test = CVUtils.getItems(analyzer.getSentences(), cv.getTestIndex(i));
//                    cur.analyseTest(test, matrix, indexMap);
//                }
//
//                double acc = accuracy(matrix);
//                System.out.println("w_delta: " + w_delta +", t_delta: " + t_delta + ", acc: " + acc);
//                if(acc>max_accuracy) {
//                    max_accuracy = acc;
//                    max_t_delta = t_delta;
//                    max_w_delta = w_delta;
//                }
//            }
//        }
//
//        System.out.println("max w_delta: " + max_w_delta +", max t_delta: " + max_t_delta + ", max_acc: " + max_accuracy);

        // part 3
        HMMAnalyzer analyzer = new HMMAnalyzer();
        String address = "./data\\postag\\tagged";
        analyzer.loadDirectory(address, "pos");
        analyzer.tagSmoothing(0.02, 0.75);

        List<Trellis> sentences = new ArrayList<>();
        for(int i=0; i<100; i++) {
            sentences.add(analyzer.generateSentence(10));
        }

        for(Trellis tt: sentences) {
            //System.out.println(tt);
        }

        Collections.sort(sentences, new Comparator<Trellis>() {
            @Override
            public int compare(Trellis tt1, Trellis tt2)
            {
                if(tt1.getLogPro()>tt2.getLogPro()) return -1;
                else if(tt1.getLogPro()<tt2.getLogPro()) return 1;
                else return 0;
            }
        });

        System.out.println("====================  Top 10 =====================");
        for(int i=0; i<10; i++) {
            //System.out.println(sentences.get(i));
        }
        List<List<Pair<Word, Tag>>> pairList = new ArrayList<>();
        for(int i=0; i<sentences.size(); i++) {
            pairList.add(helper(sentences.get(i)));
        }

        HashMap<String, Integer> indexMap = new HashMap<>();
        int index = 0;
        for(Tag t : analyzer.tagMap.values()) {
            indexMap.put(t.getStr(), index++);
        }
        int [][] matrix = new int[indexMap.size()][indexMap.size()];
        analyzer.analyseTest(pairList, matrix, indexMap);

        System.out.println("Overall Tag Accuracy: " + accuracy(matrix));

        for(int i=0; i<100; i++) {
            Trellis pos = analyzer.POSTagging(helper(sentences.get(i)));
            System.out.println(sentences.get(i));
            System.out.println("Pos tagging acc: " + calculateAcc(sentences.get(i), pos));
        }
    }

    public static List<Pair<Word, Tag>> helper(Trellis t) {
        List<Pair<Word, Tag>> res = new ArrayList<>();
        for(int i=0; i<t.size(); i++) {
            Pair<Word, Tag> pp = new Pair<>(t.getWordL().get(i), t.getTag(i));
            res.add(pp);
        }
        return res;
    }

    public static double calculateAcc(Trellis a, Trellis b) {
        double total = 0;
        double cor = 0;
        for(int i=0; i<a.size(); i++) {
            if(a.getTag(i).getStr().equals(b.getTag(i).getStr())) {
                cor++;
            }
            total ++;
        }
        return cor/total;
    }
}
