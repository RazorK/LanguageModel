/**
 *
 */
package structures;

/**
 * @author hongning
 *         Suggested structure for constructing N-gram language model and vector space representation
 */
public class Token {

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

    //default constructor
    public Token(String token) {
        m_token = token;
        m_id = -1;
        m_ttf_value = 0;
        m_df_value = 0;
    }

    //default constructor
    public Token(int id, String token) {
        m_token = token;
        m_id = id;
        m_ttf_value = 0;
        m_df_value = 0;
    }

    public double getIDFValue(int total) {
        if(m_df_value != 0)
            return 1+Math.log(total/m_df_value);
        return -1;
    }
}
