package structures;

import utils.MapUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BigramLM extends LanguageModel{

    HashMap<String, HashMap<String, Token>> m_model; // sparse structure for storing the maximum likelihood estimation of LM with the seen N-grams
    LanguageModel m_reference; // pointer to the reference language model for smoothing purpose
    int m_type;

    HashMap<String, Double> absoluteLambda;

    /**
     *
     * @param N
     * @param ref
     * @param type 0 for linear, 1 for absolute
     */
    public BigramLM(int N, LanguageModel ref, int type) {
        super(N);
        m_reference = ref;
        m_model = new HashMap<>();
        m_type = type;
    }

    public void addOneToModel(String pre, String lat) {
        if(m_model.containsKey(pre)) {
            HashMap<String, Token> temp = m_model.get(pre);
            if(temp.containsKey(lat)) {
                Token t = temp.get(lat);
                t.setTTFValue(t.getTTFValue()+1);
            } else {
                Token t = new Token(lat);
                t.setTTFValue(1);
                temp.put(lat,t);
            }
        } else {
            HashMap<String, Token> temp = new HashMap<>();
            Token t = new Token(lat);
            t.setTTFValue(1);
            temp.put(lat, t);
            m_model.put(pre, temp);
        }
    }

    public void reProcess() {
        if(m_type == 0) {
            processLinear(m_lambda);
        } else if(m_type == 1) {
            processAbsolute(m_delta);
        }
    }

    public void processLinear(double lambda) {
        // addictive smooth
//		Token unseen = new Token("UNKNOWN");
//		m_model.put("UNKNOWN", unseen);
        m_lambda = lambda;

        m_V = m_model.size();
        for(Map.Entry<String, HashMap<String, Token>> en : m_model.entrySet()) {
            long total = 0;
            HashMap<String, Token> map = en.getValue();
            for(Map.Entry<String, Token> inneren: map.entrySet()) {
                total += inneren.getValue().getTTFValue();
            }
            for(Map.Entry<String, Token> inneren: map.entrySet()) {
                Token t = inneren.getValue();
                t.setPro(m_lambda* t.getTTFValue()/total + (1-m_lambda)*m_reference.getPro(inneren.getKey()));
            }
        }
    }

    public void processAbsolute(double delta) {
        m_delta = delta;
        m_V = m_model.size();
        absoluteLambda = new HashMap<>();

        for(Map.Entry<String, HashMap<String, Token>> en : m_model.entrySet()) {
            long total = 0;
            int S = en.getValue().size();
            HashMap<String, Token> map = en.getValue();

            for(Map.Entry<String, Token> inneren: map.entrySet()) {
                total += inneren.getValue().getTTFValue();
            }
            double lambda = m_delta * S/total;
            absoluteLambda.put(en.getKey(), lambda);

            for(Map.Entry<String, Token> inneren: map.entrySet()) {
                Token t = inneren.getValue();
                t.setPro((t.getTTFValue()-m_delta)/total + lambda * m_reference.getPro(inneren.getKey()));
            }
        }
    }

    public double getPro(String pre, String las) {
        HashMap<String, Token> t = m_model.get(pre);
        // special case here, if t is null, which means the first word is not seen, we should back off to a unigram
        if(t == null || t.size() == 0) {
            return m_reference.getPro(las);
        }
        if(t.containsKey(las)) {
            return t.get(las).getPro();
        } else {
            if(m_type == 0) {
                // linear
                return (1-m_lambda) * m_reference.getPro(las);
            } else if(m_type == 1) {
                // absolute
                return absoluteLambda.get(pre) * m_reference.getPro(las);
            }
            return 0;
        }
    }

    public String sampling(String pre, ArrayList<Double> cur) {
        if(pre == "UNKNOWN" || !m_model.containsKey(pre)) {
            while (pre == "UNKNOWN") pre = m_reference.sampling(cur);
            return pre;
        }
        double prob = Math.random(); // prepare to perform uniform sampling
        for(String token:m_model.get(pre).keySet()) {
            prob -= getPro(token);
            if (prob<=0) {
                cur.add(getPro(token));
                return token;
            }
        }
        return "UNKNOWN"; //How to deal with this special case?
    }

    public String [] getTop10(String pre) {
        if(!m_model.containsKey(pre)) return null;
        Map<String, Token> sort = MapUtils.sortByTTf(m_model.get(pre));
        String [] res = new String[10];
        int i=0;
        for(String word: sort.keySet()) {
            res[i] = word;
            if(i++>=9) break;
        }
        return res;
    }

    public double logLikelihood(Post review) throws Exception {
        String [] tokens = review.getTokens();
        double likelihood = m_reference.getPro(tokens[0]);
        for(int i=0; i<tokens.length-1; i++) {
            double pro = getPro(tokens[i], tokens[i+1]);
            if(pro >=1 || pro<0) throw new Exception("wrong pro in bigram");
            if(pro == 0) throw new Exception("smoothed Bigram can't have zero probability");
            likelihood+= Math.log(pro);
        }
        return likelihood;
    }
}
