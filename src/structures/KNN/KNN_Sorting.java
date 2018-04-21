package structures.KNN;

import structures.Post;

import java.util.HashMap;
import java.util.Map;

public class KNN_Sorting { ;
    double min;
    int k;
    Post minP;
    Post target;
    Map<Post, Double> map;

    public KNN_Sorting(int k, Post p) {
        this.k = k;
        map = new HashMap<>();
        target = p;
    }

    public void insert(Post p, double simi) {
        if(map.size() == 0) {
            min = simi;
            minP = p;
            map.put(p,simi);
            return;
        }

        if(map.size()<k) {
            if(simi<min) {
                min = simi;
                minP = p;
            }
            map.put(p, simi);
            return;
        }

        // remove p;
        map.remove(minP);
        map.put(p, simi);

        // update min
        double min = Integer.MAX_VALUE;
        for(Map.Entry<Post, Double> en: map.entrySet()) {
            if(en.getValue() < min) {
                min = en.getValue();
                minP = en.getKey();
            }
        }

        this.min = min;
    }

    public void addOneToModel(Post p) {
        double simi = target.similiarity(p);
        if(map.size()<k) {
            insert(p, simi);
            return;
        }

        if(simi > min) {
            insert(p, simi);
        }

        if(map.size()>k) throw new RuntimeException("k exceed");
    }

    public Map<Post, Double> getMap() {
        return map;
    }
}
