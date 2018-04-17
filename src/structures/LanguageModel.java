package structures;

import utils.MapUtils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * @author hongning
 * Suggested structure for constructing N-gram language model
 */
public class LanguageModel {

	public int m_N; // N-gram
	int m_V; // the vocabular size
	HashMap<String, Token> m_model; // sparse structure for storing the maximum likelihood estimation of LM with the seen N-grams

	double m_lambda; // parameter for linear interpolation smoothing
	double m_delta; // parameter for absolute discount smoothing
	double add_delta;
	private double unknown;
	private int total;

	public LanguageModel(int N) {
		m_N = N;
		m_model = new HashMap<>();
	}

	public void addOneToModel(String word) {
        if(m_model.containsKey(word)) {
            Token temp = m_model.get(word);
            temp.setTTFValue(temp.getTTFValue()+1);
        } else {
            Token temp = new Token(word);
            temp.setTTFValue(1);
            m_model.put(word, temp);
        }
    }

    public void addOneToAppearModel(String word) {
        if (!m_model.containsKey(word)) {
            Token temp = new Token(word);
            temp.setTTFValue(1);
            m_model.put(word, temp);
        }
    }

	public void processModel() {
		// addictive smooth
//		Token unseen = new Token("UNKNOWN");
//		m_model.put("UNKNOWN", unseen);

		m_V = m_model.size();
		double total = 0;
		for(Map.Entry<String, Token> en : m_model.entrySet()) {
			total += en.getValue().getTTFValue();
		}
		for(Map.Entry<String, Token> en : m_model.entrySet()) {
			Token t = en.getValue();
			double pro = (t.getTTFValue()+0.0)/total;
			t.setPro(pro);
		}
		unknown = add_delta/total;
	}

	public double getPro(String token) {
		if(m_model.containsKey(token)) {
			return m_model.get(token).getPro();
		} else {
			return unknown;
		}
	}

	public double getUnknown() {
		return unknown;
	}

	//We have provided you a simple implementation based on unigram language model, please extend it to bigram (i.e., controlled by m_N)
	public String sampling(ArrayList<Double> cur) {
		double prob = Math.random(); // prepare to perform uniform sampling
		for(String token:m_model.keySet()) {
			prob -= getPro(token);
			if (prob<=0) {
				cur.add(getPro(token));
				return token;
			}
		}
		return "UNKNOWN"; //How to deal with this special case?
	}
	
	public double logLikelihood(Post review) throws Exception {
		double likelihood = 0;
		for(String token:review.getTokens()) {
		    double pro = getPro(token);
		    if(pro==0) throw new Exception("Unigram can't have unseen words");
			likelihood += Math.log(pro);
		}
		return likelihood;
	}

	public double getPerplexity(Post review) throws Exception {
	    double log = logLikelihood(review);
	    double length = review.getTokens().length;
	    double pow = -log/length;
	    return Math.exp(pow);
    }


	public void addictSmooth(HashSet<String> newVol, double delta) {
		add_delta = delta;
		if(newVol == null || newVol.size() == 0) return;
		total = newVol.size();

//		for(String word:newVol) {
//			if(!m_model.containsKey(word)) {
//				m_model.put(word, new Token(word));
//			}
//		}

//		for(Map.Entry<String, Token> en : m_model.entrySet()) {
//			Token t = en.getValue();
//			t.setTTFValue(t.getTTFValue()+delta);
//		}

        double under = 0;
        for(Map.Entry<String, Token> en : m_model.entrySet()) {
            under += en.getValue().getTTFValue();
        }
        under += total*delta;
        for(Map.Entry<String, Token> en : m_model.entrySet()) {
            Token t = en.getValue();
            double pro = (t.getTTFValue()+0.0)/under;
            t.setPro(pro);
        }
        unknown = add_delta/under;
	}

	public void addictSmooth(double delta) {
		add_delta = delta;
		for(Map.Entry<String, Token> en : m_model.entrySet()) {
			Token t = en.getValue();
			t.setTTFValue(t.getTTFValue()+delta);
		}

		processModel();
	}

	public void checkValid() {
		double sum = 0;
		for(Token t: m_model.values()) {
			sum += t.getPro();
		}
		System.out.println("============================");
		System.out.println(sum);
		System.out.println("============================");
	}

	public String generateSentences(int limit, ArrayList<Double> cur) {
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<limit; i++) {
			sb.append(sampling(cur));
			sb.append(" ");
		}
		return sb.toString().trim();
	}

	public String[] getTopString(int limit) {
	    String [] res = new String[limit];
	    Map<String, Token> sort = MapUtils.sortByTTf(m_model);
        int i=0;
        for(String word: sort.keySet()) {
            res[i] = word;
            if(i++>=limit-1) break;
        }
        return res;
    }

    public void printModel() {
	    for(Map.Entry<String, Token> e : m_model.entrySet()) {
	        System.out.println("Str: "+e.getKey() + ", Pro: "+e.getValue().getPro() + ", TTF:"+e.getValue().getTTFValue());
        }
        System.out.println("Unknown Pro: "+ getUnknown());
    }
}
