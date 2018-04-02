package utils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * a helper for cross validation
 */
public class CVUtils {
    int size;
    int k;
    int foldSize;

    // index map
    List<List<Integer>> indexMap;

    /**
     * Constructor
     * @param s for total number of sentences
     * @param foldsK for k folds(folds number)
     */
    public CVUtils(int s, int foldsK) {
        size = s;
        k = foldsK;
        foldSize = (int)Math.ceil((size+0.0)/k);
        indexMap = new ArrayList<>();

        generateIndex();
    }

    public void generateIndex() {
        // init map
        for(int i=0; i<k; i++) {
            indexMap.add(new ArrayList<>());
        }

        // generate index pool
        List<Integer> pool = new ArrayList<>();
        for(int i=0; i<size; i++) pool.add(i);

        int cur_k = 0;
        int count = 0;
        for(int i=0; i<size; i++) {
            int gIndex = generateRand(pool.size());
            int sel = pool.remove(gIndex);
            indexMap.get(cur_k).add(sel);
            count++;
            if(count >= foldSize) {
                cur_k ++;
                count = 0;
            }
        }
    }

    /**
     * get train set index from round
     * @param round here round must start from 0, to k - 1
     * @return list of train set index
     */
    public List<Integer> getTrainIndex(int round) {
        List<Integer> res = new ArrayList<>();
        for(int i=0; i<k; i++) {
            if(i == round) continue;
            List<Integer> fold = indexMap.get(i);
            res.addAll(fold);
        }
        return res;
    }

    public List<Integer> getTestIndex(int round) {
        return new ArrayList<>(indexMap.get(round));
    }

    public static int generateRand(int size) {
        Random rand = new Random();
        int  n = rand.nextInt(size);
        return n;
    }

    public static void main(String [] args) {
        CVUtils cv = new CVUtils(51,5);
        System.out.println(cv.getTestIndex(0));
    }

    public static <T> ArrayList<T> getItems(List<T> total, List<Integer> index) {
        ArrayList<T> res = new ArrayList<>();
        for (Integer anIndex : index) {
            res.add(total.get(anIndex));
        }
        return res;
    }

}
