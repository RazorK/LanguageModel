package structures;

import java.util.*;

public class KNN {

    List<Post> reviews;
    int k;
    Map<String, Integer> index;

    public KNN(List<Post> p, int k_value, HashMap<String, Integer> index) {
        reviews = p;
        k = k_value;
        this.index = index;
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

    public int[] innerProduct(VectorWeight [] vec) {
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
        System.out.println("Printing Bucket: ");
        for(Map.Entry<Integer, List<Post>> en: buckets.entrySet()) {
            System.out.println("Index: " + en.getKey() + ", Size: " + en.getValue().size());
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

    public int predictFromList(List<Post> ps, Post p) {
        List<Post> corpus = new ArrayList<>(ps);
        System.out.println("Starting corpus sort" + corpus.size());
        Collections.sort(corpus, new Comparator<Post>() {
            @Override
            public int compare(Post o1, Post o2) {
                double s1 = p.similiarity(o1);
                double s2 = p.similiarity(o2);
                return ((Double)s1).compareTo(s2);
            }
        });
        System.out.println("end corpus sort");
        int pos = 0, neg = 0;
        for(int i=0; i<k; i++) {
            System.out.println(corpus.get(i).getContent());
            if(corpus.get(i).positive()) pos++;
            else neg++;
        }
        System.out.println("In" + k + " nearest neighbor, pos: " + pos + ", neg: " + neg);
        return pos>neg? 1:0;
    }

    public int predictFromBucket(Post p) {
        if(p.getBucVector() == null)  getVec(p);
        System.out.println("Query Bucket Index: " + getInteger(p.getBucVector()));
        List<Post> bucket = buckets.get(getInteger(p.getBucVector()));
        if(bucket.size() < k) return predictFromAll(p);
        return predictFromList(bucket, p);
    }

}
