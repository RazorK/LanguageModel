package utils;

import org.knowm.xchart.QuickChart;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import structures.Token;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Created by Raz on 2018/3/3.
 * MapUtils for this project.
 * Token hashmap utils.
 */

public class MapUtils {

    // State Hashmap MapUtils
    public static Map<String, Token> sortByTokenValue(Map<String, Token> map) {
        List<Map.Entry<String, Token>> list = new LinkedList<>(map.entrySet());
        Collections.sort(list, new Comparator<Object>() {
            @SuppressWarnings("unchecked")
            public int compare(Object o1, Object o2) {
                return -((Comparable<Double>) ((Map.Entry<String, Token>) (o1)).getValue().getValue())
                        .compareTo(((Map.Entry<String, Token>) (o2)).getValue().getValue());
            }
        });

        Map<String, Token> result = new LinkedHashMap<>();
        for (Iterator<Map.Entry<String, Token>> it = list.iterator(); it.hasNext();) {
            Map.Entry<String, Token> entry = it.next();
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }
    private static <K, V> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
        Collections.sort(list, new Comparator<Object>() {
            @SuppressWarnings("unchecked")
            public int compare(Object o1, Object o2) {
                return ((Comparable<V>) ((Map.Entry<K, V>) (o1)).getValue()).compareTo(((Map.Entry<K, V>) (o2)).getValue());
            }
        });

        Map<K, V> result = new LinkedHashMap<>();

        for (Iterator<Map.Entry<K, V>> it = list.iterator(); it.hasNext();) {
            Map.Entry<K, V> entry = (Map.Entry<K, V>) it.next();
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    public static void printMap(Map<String, Token> map) {
        printMap(map, Integer.MAX_VALUE);
    }

    public static void printMap(Map<String, Token> map, int max) {
        int i = 0;
        for(String word: map.keySet()) {
            if(i++>=max) break;
            System.out.println(word + ", "+ map.get(word).getValue());
        }
    }

    public static void exportMap(Map<String, Token> map, String filePath) throws  IOException{
        File file = new File(filePath);
        synchronized (file) {
            FileWriter fw = new FileWriter(filePath);
            int i = 0;
            for(String word: map.keySet()) {
                fw.write(i + ","+ word + ","+ map.get(word).getValue()+"\n");
                i++;
            }
            fw.close();
        }
    }

    public static void getArraysFromMap(Map<String, Token> map, double [] x, double [] y) {
        int i=0;
        for(String word: map.keySet()) {
            x[i] = i+1;
            y[i] = map.get(word).getValue();
            i++;
        }
    }

    public static void getLog2DArrayFromMap(Map<String, Token> map, double [][] data) {
        int i=0;
        for(String word: map.keySet()) {
            data[i][0] = Math.log(i+1);
            data[i][1] = Math.log(map.get(word).getValue());
            i++;
        }
    }

    public static void seperate2D(double [][] data, double [] xData, double [] yData) {
        for(int i=0; i<data.length; i++) {
            xData[i] = data[i][0];
            yData[i] = data[i][1];
        }
    }

}
