package structures.HMM;

import java.util.ArrayList;
import java.util.List;

/**
 * To store the word sequence and the tag sequence.
 */
public class Trellis {
    List<Word> wordL;
    List<Tag> tagL;

    double logPro;
    Tag cur;

    public Trellis(StartTag start) {
        this.wordL = new ArrayList<>();
        this.tagL = new ArrayList<>();
        cur = start;
        logPro = 1;
    }

    public void addWord(Word word, Tag tag) {
        // check tag != null
        if(tag == null) throw new RuntimeException("null tag when adding word into Trellis");

        // probability process
        logPro += Math.log(cur.getTagPro(tag.getStr()));
        logPro += Math.log(tag.getWordPro(word.getWord()));

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
        return logPro + Math.log(getNextTagPro(t)) + Math.log(t.getWordPro(w.getWord()));
    }
}
