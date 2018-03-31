package structures.HMM;

import structures.LanguageModel;
import structures.Token;

import java.util.HashMap;
import java.util.Map;

public class Tag {
    // Tag representation
    String tagRepre;

    // Language Model for Tag
    LanguageModel tagLM;

    // language model for word
    LanguageModel wordLM;

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

    public String [] topWords(int limit) {
        return wordLM.getTopString(limit);
    }

    public String [] topTags(int limit) {
        return tagLM.getTopString(limit);
    }
}
