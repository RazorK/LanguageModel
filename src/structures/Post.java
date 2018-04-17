/**
 *
 */
package structures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import json.JSONException;
import json.JSONObject;

/**
 * @author hongning
 * @version 0.1
 * @category data structure
 * data structure for a Yelp review document
 * You can create some necessary data structure here to store the processed text content, e.g., bag-of-word representation
 */
public class Post {
    //unique review ID from Yelp
    String m_ID;

    public void setID(String ID) {
        m_ID = ID;
    }

    public String getID() {
        return m_ID;
    }

    //author's displayed name
    String m_author;

    public String getAuthor() {
        return m_author;
    }

    public void setAuthor(String author) {
        this.m_author = author;
    }

    //author's location
    String m_location;

    public String getLocation() {
        return m_location;
    }

    public void setLocation(String location) {
        this.m_location = location;
    }

    //review text content
    String m_content;

    public String getContent() {
        return m_content;
    }

    public void setContent(String content) {
        if (!content.isEmpty())
            this.m_content = content;
    }

    public boolean isEmpty() {
        return m_content == null || m_content.isEmpty();
    }

    //timestamp of the post
    String m_date;

    public String getDate() {
        return m_date;
    }

    public void setDate(String date) {
        this.m_date = date;
    }

    //overall rating to the business in this review
    double m_rating;

    public boolean positive() {
        if(m_rating>=4) return true;
        else return false;
    }

    public double getRating() {
        return m_rating;
    }

    public void setRating(double rating) {
        this.m_rating = rating;
    }

    public Post(String ID) {
        m_ID = ID;
    }

    public VectorWeight[] getVec() {
        return vec;
    }

    public void setVecFromTokens(String [] tokens, HashMap<String, Token> map) {
        vec = new VectorWeight[indexMap.size()];
        if(tokens == null || tokens.length == 0) return;
        for(String word: tokens) {
            if(!indexMap.containsKey(word)) continue;
            int index = indexMap.get(word);
            if(vec[index] == null) {
                VectorWeight insert = new VectorWeight(word);
                insert.setTF(1);
                insert.setIDF(map.get(word).getIDFValue());
                vec[index] = insert;
            } else {
                vec[index].setTF(vec[index].getTF()+1);
            }
        }
    }

    VectorWeight [] vec;

    public HashMap<String, Integer> getIndexMap() {
        return indexMap;
    }

    public void setIndexMap(HashMap<String, Integer> indexMap) {
        this.indexMap = indexMap;
    }

    HashMap<String, Integer> indexMap;

    public double similiarity(Post p) {
        //compute the cosine similarity between this post and input p based on their vector space representation
        if(indexMap != p.indexMap){
            throw new java.lang.Error("Size not the same, can not calculate similarity");
        }

        double res = 0;
        for(int i=0; i<vec.length; i++) {
            if(vec[i]!=null && p.vec[i]!=null) {
                res+=vec[i].getWeight() * p.vec[i].getWeight();
            }
        }
        return res/(getLength()*p.getLength());
    }

    public double getLength() {
        double res = 0;
        for(int i=0; i<vec.length; i++) {
            if(vec[i]!=null)
                res+= Math.pow(vec[i].getWeight(), 2);
        }
        return Math.sqrt(res);
    }

    // candidate
    double candidateSim;
    public void setCandidateValue(double i) {
        candidateSim = i;
    }

    public double getCandidateSim() {
        return candidateSim;
    }

    public void printPost() {
        System.out.println("------------------Start Print Post------------------");
        System.out.println("Author: " + getAuthor());
        System.out.println("Data: " + getDate());
        System.out.println("Content: " + getContent());
        System.out.println("ReviewID: "+ m_ID );
    }

    public Post(JSONObject json) {
        try {
            m_ID = json.getString("ReviewID");
            setAuthor(json.getString("Author"));

            setDate(json.getString("Date"));
            setContent(json.getString("Content"));
            setRating(json.getDouble("Overall"));
            setLocation(json.getString("Author_Location"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        features = new ArrayList<>();
    }

    public JSONObject getJSON() throws JSONException {
        JSONObject json = new JSONObject();

        json.put("ReviewID", m_ID);//must contain
        json.put("Author", m_author);//must contain
        json.put("Date", m_date);//must contain
        json.put("Content", m_content);//must contain
        json.put("Overall", m_rating);//must contain
        json.put("Author_Location", m_location);//must contain

        return json;
    }

    public String[] getTokens() {
        return tokens;
    }

    public void setTokens(String[] tokens) {
        this.tokens = tokens;
    }

    String [] tokens;

    List<String> features;

    public int getFeatureLength() {
        return features.size();
    }

    public void addFeature(String t) {
        features.add(t);
    }

    public Iterator<String> getFeatureIt() {
        return features.iterator();
    }

}
