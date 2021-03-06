package structures.HMM;

import structures.LanguageModel;
import structures.Token;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class Tag {
    // Tag representation
    String tagRepre;

    // Language Model for Tag
    public LanguageModel tagLM;

    // language model for word
    public LanguageModel wordLM;

    public Tag(String t) {
        tagRepre = t;
        tagLM = new LanguageModel(1);
        wordLM = new LanguageModel(1);
    }

    public void addWord(String word) {
        wordLM.addOneToModel(word);
    }

    public void addTag(String tag) {
        tagLM.addOneToModel(tag);
    }

    public String getStr() {
        return tagRepre;
    }

    public void additiveSmoothing(double w_delta, double t_delta) {
        tagLM.addictSmooth(t_delta);
        wordLM.addictSmooth(w_delta);
    }

    public void additiveSmoothing(double w_delta, double t_delta , HashSet<String> wordVol, HashSet<String> tagVol) {
        tagLM.addictSmooth(tagVol, t_delta);
        wordLM.addictSmooth(wordVol, w_delta);
    }

    public double getUnknownTagPro() {
        return tagLM.getUnknown();
    }

    public double getUnknownWordPro() {
        return wordLM.getUnknown();
    }

    public String [] topWords(int limit) {
        return wordLM.getTopString(limit);
    }

    public String [] topTags(int limit) {
        return tagLM.getTopString(limit);
    }

    public double getTagPro(String t) {
        return tagLM.getPro(t);
    }

    public double getWordPro(String t) {
        return wordLM.getPro(t);
    }
}
