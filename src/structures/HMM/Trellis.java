package structures.HMM;

import java.util.ArrayList;
import java.util.List;

/**
 * To store the word sequence and the tag sequence.
 */
public class Trellis {
    public List<Word> getWordL() {
        return wordL;
    }

    public void setWordL(List<Word> wordL) {
        this.wordL = wordL;
    }

    public void setTagL(List<Tag> tagL) {
        this.tagL = tagL;
    }

    public List<Tag> getTagL() {
        return tagL;
    }

    public void setLogPro(double logPro) {
        this.logPro = logPro;
    }

    public void setCur(Tag cur) {
        this.cur = cur;
    }

    List<Word> wordL;
    List<Tag> tagL;
    double logPro;
    Tag cur;
    StartTag myStart;

    public Trellis(StartTag start) {
        this.wordL = new ArrayList<>();
        this.tagL = new ArrayList<>();
        cur = start;
        myStart = start;
        logPro = 0;
    }

    public Trellis clone() {
        Trellis res = new Trellis(myStart);
        res.setTagL(new ArrayList<>(getTagL()));
        res.setWordL(new ArrayList<>(getWordL()));
        res.setLogPro(getLogPro());
        res.setCur(getCur());
        return res;
    }

    public void addWord(Word word, Tag tag) {
        // check tag != null
        if(tag == null) throw new RuntimeException("null tag when adding word into Trellis");

        // probability process
        logPro += tag instanceof UnknownTag? Math.log(cur.getUnknownTagPro()): Math.log(cur.getTagPro(tag.getStr()));
        logPro += word == null? Math.log(tag.getUnknownWordPro()):Math.log(tag.getWordPro(word.getWord()));

        // list process
        wordL.add(word);
        tagL.add(tag);
        cur = tag;
    }

    public double getPro() {
        return Math.exp(logPro);
    }

    public double getLogPro() {
        return logPro;
    }

    public double getNextTagPro(Tag t) {
        return cur.getTagPro(t.getStr());
    }

    public Tag getCur() {
        return cur;
    }


    // for DP transmit
    public double getWTLogPro(Word w, Tag t) {
        double res = logPro;
        //System.out.println(cur.getStr() + ","+t.getStr()+","+w.getWord());
        //System.out.println(res);
        res += t instanceof UnknownTag? Math.log(cur.getUnknownTagPro()): Math.log(cur.getTagPro(t.getStr()));
        //System.out.println(res);
        res += w == null? Math.log(t.getUnknownWordPro()):Math.log(t.getWordPro(w.getWord()));
        //System.out.println(res);
        return res;
    }

    public int size() {
        if(wordL.size()!=tagL.size()) throw new RuntimeException("Trellis size not same");
        return wordL.size();
    }

    public Tag getTag(int index) {
        return tagL.get(index);
    }

    public Word getWord(int index) {
        return wordL.get(index);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Sentence: ");
        for(int i=0; i<size(); i++) {
            sb.append(wordL.get(i).getWord() + "[" + tagL.get(i).getStr()+"] ");
        }
        sb.append("\n");
        sb.append("log-probability: " + logPro);
        return sb.toString();
    }
}
