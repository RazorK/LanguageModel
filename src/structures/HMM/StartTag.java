package structures.HMM;

public class StartTag extends Tag {
    public StartTag() {
        super("OwnStart");
    }

    public void addWord(String word) {
        throw new RuntimeException("Start Tag can't add word");
    }

    public void additiveSmoothing(double t_delta) {
        tagLM.addictSmooth(t_delta);
    }
}
