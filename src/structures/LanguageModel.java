/**
 * 
 */
package structures;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * @author hongning
 * Suggested structure for constructing N-gram language model
 */
public class LanguageModel {

	int m_N; // N-gram
	int m_V; // the vocabular size
	HashMap<String, Token> m_model; // sparse structure for storing the maximum likelihood estimation of LM with the seen N-grams
	LanguageModel m_reference; // pointer to the reference language model for smoothing purpose
	
	double m_lambda; // parameter for linear interpolation smoothing
	double m_delta; // parameter for absolute discount smoothing

	public LanguageModel(int N) {
		m_N = N;
		m_model = new HashMap<>();
	}
	
	public double calcMLProb(String token) {
		// return m_model.get(token).getValue(); // should be something like this
		return 0;
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

	public void processModel() {
		// addictive smooth
//		Token unseen = new Token("UNKNOWN");
//		m_model.put("UNKNOWN", unseen);

		m_V = m_model.size();
		long total = 0;
		for(Map.Entry<String, Token> en : m_model.entrySet()) {
			total += en.getValue().getTTFValue();
		}
		for(Map.Entry<String, Token> en : m_model.entrySet()) {
			Token t = en.getValue();
			double pro = t.getTTFValue()/total;
			t.setPro(pro);
		}
	}

	public double getPro(String token) {
		// TODO: addictive
		if(m_model.containsKey(token)) {
			return m_model.get(token).getPro();
		} else {
			return 0;
		}
	}

//	public double calcLinearSmoothedProb(String token) {
//		if (m_N>1)
//			return (1.0-m_lambda) * calcMLProb(token) + m_lambda * m_reference.calcLinearSmoothedProb(token);
//		else
//			return 0; // please use additive smoothing to smooth a unigram language model
//	}
	
	//We have provided you a simple implementation based on unigram language model, please extend it to bigram (i.e., controlled by m_N)
	public String sampling() {
		double prob = Math.random(); // prepare to perform uniform sampling
		for(String token:m_model.keySet()) {
			prob -= getPro(token);
			if (prob<=0)
				return token;
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
	    return -log/length;
    }

	public void addictSmooth(HashSet<String> newVol, double delta) {
		if(newVol == null || newVol.size() == 0) return;
		for(String word:newVol) {
			if(!m_model.containsKey(word)) {
				m_model.put(word, new Token(word));
			}
		}

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

	public String generateSentences(int limit) {
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<limit; i++) {
			sb.append(sampling());
			sb.append(" ");
		}
		return sb.toString().trim();
	}
}
