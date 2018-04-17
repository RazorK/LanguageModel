/**
 *
 */
package structures;

/**
 * @author hongning
 *         Suggested structure for constructing N-gram language model and vector space representation
 */
public class Token {
    //default constructor
    public Token(String token) {
        m_token = token;
        m_id = -1;
        m_ttf_value = 0;
        m_df_value = 0;
        m_idf_value = 0;
        pro = 0;

        pos = 0;
        neg = 0;
    }

    //default constructor
    public Token(int id, String token) {
        m_token = token;
        m_id = id;
        m_ttf_value = 0;
        m_df_value = 0;
        m_idf_value = 0;
        pro = 0;

        pos = 0;
        neg = 0;
    }

    int m_id; // the numerical ID you assigned to this token/N-gram

    public int getID() {
        return m_id;
    }

    public void setID(int id) {
        this.m_id = id;
    }

    String m_token; // the actual text content of this token/N-gram

    public String getToken() {
        return m_token;
    }

    public void setToken(String token) {
        this.m_token = token;
    }

    double m_ttf_value; // frequency or count of this token/N-gram

    public double getTTFValue() {
        return m_ttf_value;
    }

    public void setTTFValue(double value) {
        this.m_ttf_value = value;
    }

    double m_df_value; // documnet frequency

    public double getDFValue() {
        return m_df_value;
    }

    public void setDFValue(double m_df_value) {
        this.m_df_value = m_df_value;
    }

    double m_idf_value;

    public double getIDFValue() {
        return m_idf_value;
    }

    public void setIDF(int total) {
        if(m_df_value != 0){
            m_idf_value = 1+Math.log(total/m_df_value);
        }
    }

    public void setIDFValue(double idf) {
        m_idf_value = idf;
    }

    public double getPro() {
        return pro;
    }

    public void setPro(double pro) {
        this.pro = pro;
    }

    double pro;

    int pos;
    int neg;

    public void addPos() {pos++;}
    public void addNeg() {neg++;}

    int t_pos;
    int t_neg;

    double IG;
    double chis;

    // calculate IG and Chi Square.
    public void process(int total_pos, int total_neg) {
        t_pos = total_pos;
        t_neg = total_neg;

        double A = pos, C = neg, B = t_pos-pos, D = t_neg-neg, total = total_neg + total_pos;
        IG = ((A+C)/total) * (getEntro(A,C)) + ((B+D)/total) * getEntro(B, D) - getEntro(A+B, C+D);

        chis = total * (A*D - B*C) * (A*D - B*C) / ((A+C) * (B+D) * (A+B) * (C+D));
    }

    private static double getEntro(double pos, double neg) {
        double p_pos = pos/(pos+neg);
        double p_neg = neg/(pos+neg);

        double res = 0;
        if(p_pos != 0) res += p_pos * Math.log(p_pos);

        if(p_neg != 0) res += p_neg * Math.log(p_neg);

        return res;
    }

    public double getIG() {return IG;}
    public double getChis() {return chis;}



}
