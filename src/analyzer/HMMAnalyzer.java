package analyzer;

import structures.HMM.StartTag;
import structures.HMM.Tag;
import structures.HMM.Word;
import structures.Pair;

import java.io.*;
import java.util.*;

public class HMMAnalyzer {
    HashMap<String, Word> wordMap;
    HashMap<String, Tag> tagMap;

    // start tag
    Tag start;
    public HMMAnalyzer() {
        // initialize variable
        wordMap = new HashMap<>();
        tagMap = new HashMap<>();

        start = new StartTag();
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
                processWT(w, t, l);;
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

        List<Pair<Word, Tag>> list = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            processLine(line, list);
        }
        reader.close();

        // TODO: list process
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


    // requirement: , w_delta = 0.1, tag_delta = 0.5
    public void tagSmoothing(double w_delta, double t_delta) {
        for(Tag t : tagMap.values()) {
            t.additiveSmoothing(w_delta, t_delta);
        }
    }

    // sample code for demonstrating how to recursively load files in a directory
    public void loadDirectory(String folder, String suffix) throws IOException {
        File dir = new File(folder);
        int count = 0;
        for (File f : dir.listFiles()) {
            if (f.isFile() && f.getName().endsWith(suffix)) {
                analyzeDocument(f.getAbsolutePath());
                System.out.println("Finish loading file number: "+ (++count));
            }
            else if (f.isDirectory())
                loadDirectory(f.getAbsolutePath(), suffix);
        }
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

    public static void main(String [] args) throws IOException {
        HMMAnalyzer analyzer = new HMMAnalyzer();
        String address = "./data\\postag\\tagged";
        analyzer.loadDirectory(address, "pos");
        analyzer.tagSmoothing(0.1, 0.5);
        analyzer.printTopWords("NN", 10);
        analyzer.printTopTags("VB", 10);
    }
}
