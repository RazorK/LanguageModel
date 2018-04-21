package structures.KNN;

import structures.Pair;
import structures.Post;
import structures.VectorWeight;

import java.util.*;

public class KNN {

    List<Post> reviews;
    int k;
    Map<String, Integer> index;

    public KNN(List<Post> p, int k_value, HashMap<String, Integer> index) {
        reviews = p;
        k = k_value;
        this.index = index;

        debug = false;
    }

    public void setK(int k) {
        this.k = k;
    }

    double [][] random;
    int l;

    public void generateRandom(int l) {
        this.l = l;
        random = new double[l][index.size()];
        for(int i=0; i<l; i++) {
            for(int j=0; j<index.size(); j++) {
                random[i][j] = rand(-1, 1);
            }
        }
//        System.out.println("Printing RandomVector:");
//        System.out.println(Arrays.deepToString(random));
    }

    public double [][] getRandom() {
        return random;
    }

    public double rand(double min, double max) {
        Random random = new Random();
        return min + (max - min) * random.nextDouble();
    }

    public static int sgn(double x) {
        return x >= 0? 1 : 0;
    }

    public int[] innerProduct(VectorWeight[] vec) {
        int [] result = new int [l];
        for(int j=0; j<l; j++) {
            double res = 0;
            for (int i = 0; i < index.size(); i++) {
                if(vec[i] == null) continue;
                res+= random[j][i] * vec[i].getWeight();
            }
            result[j] = sgn(res);
        }
        return result;
    }

    public void getVec(Post p) {
        if(!p.getIndexMap().equals(index))  throw new RuntimeException("Not same Index Map");
        p.setBucVector(innerProduct(p.getVec()));
    }

    public void generateCorpusVec() {
        for(Post p : reviews) {
            getVec(p);
        }
    }

    Map<Integer, List<Post>> buckets;
    public void generateBucket() {
        buckets = new HashMap<>();
        for(Post p: reviews) {
            int index = getInteger(p.getBucVector());
            if(!buckets.containsKey(index)) buckets.put(index, new ArrayList<>());
            buckets.get(index).add(p);
        }
        printBucket();
    }

    public void printBucket() {
        debug("Printing Bucket: ");
        for(Map.Entry<Integer, List<Post>> en: buckets.entrySet()) {
            debug("Index: " + en.getKey() + ", Size: " + en.getValue().size());
        }
    }
    /**
     * get Integer index for buckets
     * @param vec
     * @return
     */
    public static int getInteger(int [] vec) {
        int res = 0;
        for(int i=0; i<vec.length; i++) {
            res += vec[i] * (int)Math.pow(2, i);
        }
        return res;
    }


    /**
     * NOTE: the input post should already calculate the vec
     * @param p
     * @return 0 for negtive, 1 for positive
     */
    public int predictFromAll(Post p) {
        List<Post> corpus = new ArrayList<>(reviews);

        return predictFromList(corpus, p);
    }

    boolean debug;
    public void setDebug(boolean de) {
        debug =de;
    }

    public void debug(String s) {
        if(debug) {
            System.out.println(s);
        }
    }

    public int predictFromList_V3(List<Post> ps, Post p) {
        List<Post> corpus = new ArrayList<>(ps);
        List<Pair<Post, Double>> cops = new ArrayList<>();
        for(Post cor: corpus) {
            cops.add(new Pair<>(cor, p.similiarity(cor)));
        }
        debug("Starting corpus sort" + corpus.size());
        Collections.sort(cops, new Comparator<Pair<Post, Double>>() {
            @Override
            public int compare(Pair<Post, Double> o1, Pair<Post, Double> o2) {
                return -((Double) o1.getRight()).compareTo(o2.getRight());
            }
        });
        debug("end corpus sort");
        int pos = 0, neg = 0;
        for(int i=0; i<k; i++) {
            if(cops.get(i).getLeft().positive()) pos++;
            else neg++;
        }
        debug("In" + k + " nearest neighbor, pos: " + pos + ", neg: " + neg);
        return pos>neg? 1:0;
    }

    public int predictFromList(List<Post> ps, Post p) {
        List<Post> corpus = new ArrayList<>(ps);
        debug("Starting corpus sort" + corpus.size());
        KNN_Sorting st = new KNN_Sorting(k, p);
        for(Post one: ps) {
            st.addOneToModel(one);
        }
        debug("end corpus sort");
        int pos = 0, neg = 0;
        Map<Post, Double> map = st.getMap();
        for(Map.Entry<Post, Double> en:map.entrySet()) {
            debug("FromListDebug" + en.getValue());
            if(en.getKey().positive()) pos ++; else neg++;
        }
        debug("In" + k + " nearest neighbor, pos: " + pos + ", neg: " + neg);
        return pos>neg? 1:0;
    }

    public int Old_predictFromList(List<Post> ps, Post p) {
        List<Post> corpus = new ArrayList<>(ps);
        debug("Starting corpus sort" + corpus.size());
        Collections.sort(corpus, new Comparator<Post>() {
            @Override
            public int compare(Post o1, Post o2) {
                double s1 = p.similiarity(o1);
                double s2 = p.similiarity(o2);
                return ((Double)s1).compareTo(s2);
            }
        });
        debug("end corpus sort");
        int pos = 0, neg = 0;
        for(int i=0; i<k; i++) {
            debug(corpus.get(i).getContent());
            if(corpus.get(i).positive()) pos++;
            else neg++;
        }
        debug("In" + k + " nearest neighbor, pos: " + pos + ", neg: " + neg);
        return pos>neg? 1:0;
    }

    public int predictFromBucket(Post p) {
        if(p.getBucVector() == null)  getVec(p);
        debug("Query Bucket Index: " + getInteger(p.getBucVector()));
        List<Post> bucket = buckets.get(getInteger(p.getBucVector()));
        if(bucket == null||bucket.size() < k) return predictFromAll(p);
        return predictFromList(bucket, p);
    }

    public double [] getFPR(List<Post> ps) {
        int tp = 0, fp = 0, fn = 0, tn = 0;
        int count = 0;
        int cur = 0;
        for(Post p : ps) {
            count++;
            if(count - cur > 1000) {
                System.out.println("Cur: " + count);
                cur = count;
            }
            if(p.positive()) {
                if(predictFromBucket(p) == 1) {
                    tp++;
                } else fn++;
            } else {
                if(predictFromBucket(p) == 0) tn ++;
                else fp++;
            }
        }

        double precision = (tp + 0.0)/ (tp + fp);
        double recall = (tp+0.0)/(tp + fn);
        double f1 = 2.0/(1.0/recall + 1.0/precision);
        return new double[] {f1, precision, recall};
    }
}
