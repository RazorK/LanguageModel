package structures;

public class VectorWeight {
    String word;
    double TF;
    double IDF;
    double sub_linear_TF;

    public VectorWeight(String word) {
        this.word = word;
    }

    public double getTF() {
        return TF;
    }

    public void setTF(double TF) {
        this.TF = TF;
        if(TF > 0) {
            sub_linear_TF = 1+ Math.log(TF);
        } else {
            sub_linear_TF = 0;
        }
    }

    public double getIDF() {
        return IDF;
    }

    public void setIDF(double IDF) {
        this.IDF = IDF;
    }

    public double getWeight() {
        return sub_linear_TF*IDF;
    }
}
