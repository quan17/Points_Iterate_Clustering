/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import java.io.*;
import java.util.*;

/**
 *
 * @author lianduan
 */
public class LDBSCAN {

    //we don't provide the function to normalize the attributes. If you want to, you should normalize your data first and then save the normalized data into a csv file.
    //we only use the Euclidean distance. If you want to use other distance function, you can change the calculateDistance(int MinPts) function
    private ArrayList records = new ArrayList();
    private Quicksort qs = new Quicksort();
    private double[] lrd;
    private double[] lof;
    private int[] belong;
    private double[][] knn_dist;
    private int[][] knn_seq;
    private ArrayList clustering_sequence = new ArrayList();

    private void runLDBSCAN(String filename, int MinPts_LOF, int MinPts_LDBSCAN, double core_lof, double pct) {
        loadData(filename);
        calculateDistance(Math.max(MinPts_LOF, MinPts_LDBSCAN));
        calculateLRD(MinPts_LOF);
        calculateLOF(MinPts_LOF);
        clustering(MinPts_LDBSCAN, core_lof, pct);
        System.out.println();
    }

    //sampleData.csv
    private void loadData(String filename) {
        File file = new File(filename);
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        DataInputStream dis = null;
        try {
            fis = new FileInputStream(file);
            bis = new BufferedInputStream(fis);
            dis = new DataInputStream(bis);
            while (dis.available() != 0) {
                String data = dis.readLine();
                ArrayList record = new ArrayList();
                String[] attributes = data.split(",");
                for (int i = 0; i < attributes.length; i++) {
                    record.add(Double.parseDouble(attributes[i]));
                }
                records.add(record);
            }
            fis.close();
            bis.close();
            dis.close();
        } catch (FileNotFoundException e) {
            System.out.println("File Not Found!");
        } catch (IOException e) {
            System.out.println("IO Error!");
        }
    }

    private void calculateDistance(int MinPts) {
        int n = records.size();
        ArrayList object = (ArrayList) records.get(0);
        int m = object.size();
        knn_dist = new double[n][MinPts + 1];
        knn_seq = new int[n][MinPts + 1];
        for (int i = 0; i < n; i++) {
            ArrayList first_object = (ArrayList) records.get(i);
            double[] distance = new double[n];
            for (int j = 0; j < n; j++) {
                ArrayList second_object = (ArrayList) records.get(j);
                for (int k = 0; k < m; k++) {
                    distance[j] += ((Double) first_object.get(k) - (Double) second_object.get(k)) * ((Double) first_object.get(k) - (Double) second_object.get(k));
                }
                distance[j] = Math.sqrt(distance[j]);
            }
            int[] sequence = getTopKSequence(distance, MinPts + 1, false);
            for (int j = 0; j < sequence.length; j++) {
                knn_dist[i][j] = distance[sequence[j]];
                knn_seq[i][j] = sequence[j];
            }
        }
    }

    private void calculateLRD(int MinPts_LOF) {
        lrd = new double[records.size()];
        for (int i = 0; i < records.size(); i++) {
            double reach_dist = 0;
            for (int j = 1; j <= MinPts_LOF; j++) {
                if (knn_dist[i][j] > knn_dist[knn_seq[i][j]][MinPts_LOF]) {
                    reach_dist += knn_dist[i][j];
                } else {
                    reach_dist += knn_dist[knn_seq[i][j]][MinPts_LOF];
                }
            }
            lrd[i] = MinPts_LOF / reach_dist;
        }
    }

    private void calculateLOF(int MinPts_LOF) {
        lof = new double[records.size()];
        for (int i = 0; i < records.size(); i++) {
            double lrd_sum = 0;
            for (int j = 1; j <= MinPts_LOF; j++) {
                lrd_sum += lrd[knn_seq[i][j]];
            }
            lof[i] = lrd_sum / (MinPts_LOF * lrd[i]);
        }
    }

    private void clustering(int MinPts_LDBSCAN, double core_lof, double pct) {
        int[] id_seq = new int[records.size()];
        belong = new int[records.size()];
        for (int i = 0; i < records.size(); i++) {
            id_seq[i] = i;
        }
        int[] sequence = qs.sort(lof, id_seq, true);
        ArrayList unassigned_objs_id = new ArrayList();
        for (int i = 0; i < sequence.length; i++) {
            unassigned_objs_id.add(sequence[i]);
        }
        int cluster_id = 0;
        while (!unassigned_objs_id.isEmpty()) {
            int obj_id = (Integer) unassigned_objs_id.get(0);
            //unassigned_objs_id.remove(0);            
            if (lof[obj_id] < core_lof) {
                cluster_id++;
                ArrayList tempList = new ArrayList();
                unassigned_objs_id.remove((Integer) obj_id);
                tempList.add(obj_id);
                while (!tempList.isEmpty()) {
                    obj_id = (Integer) tempList.get(0);
                    tempList.remove(0);
                    clustering_sequence.add(obj_id);
                    belong[obj_id] = cluster_id;
                    double lrd_ub = lrd[obj_id] * (1 + pct);
                    double lrd_lb = lrd[obj_id] / (1 + pct);
                    for (int i = 1; i <= MinPts_LDBSCAN; i++) {
                        if (unassigned_objs_id.contains((Integer) knn_seq[obj_id][i])
                                & lrd[knn_seq[obj_id][i]] < lrd_ub & lrd[knn_seq[obj_id][i]] > lrd_lb
                                & !tempList.contains((Integer) knn_seq[obj_id][i])) {
                            unassigned_objs_id.remove((Integer) knn_seq[obj_id][i]);
                            tempList.add(knn_seq[obj_id][i]);
                        }
                    }
                }
            } else {
                belong[obj_id] = 0;
                unassigned_objs_id.remove((Integer) obj_id);
            }
        }
        System.out.print("id,");
        for (int i = 0; i < ((ArrayList) records.get(0)).size(); i++) {
            System.out.print("attri" + (i + 1) + ",");
        }
        System.out.println("LRD,LOF,clusterID");
        for (int i = 0; i < belong.length; i++) {
            System.out.print(i + ",");
            ArrayList record = (ArrayList) records.get(i);
            for (int j = 0; j < record.size(); j++) {
                System.out.print(record.get(j) + ",");
            }
            System.out.println(lrd[i] + "," + lof[i] + "," + belong[i]);
        }
    }

    private int[] getTopKSequence(double[] original_value, int k, Boolean get_max_value) {//the computation complexity is between n and kn
        int total_length = original_value.length;
        int[] top_k_sequence = new int[k];
        double[] top_k_value = new double[k];
        if (k == 1) {
            double cur_value = original_value[0];
            int cur_seq = 0;
            if (get_max_value) {
                for (int i = 0; i < total_length; i++) {
                    if (cur_value < original_value[i]) {
                        cur_value = original_value[i];
                        cur_seq = i;
                    }
                }
            } else {
                for (int i = 0; i < total_length; i++) {
                    if (cur_value > original_value[i]) {
                        cur_value = original_value[i];
                        cur_seq = i;
                    }
                }
            }
            top_k_sequence[0] = cur_seq;
        } else if (k <= total_length) {
            if (get_max_value) {
                for (int i = 0; i < k; i++) {
                    top_k_value[i] = Double.NEGATIVE_INFINITY;
                }
                for (int i = 0; i < total_length; i++) {
                    int position = k;
                    for (int j = 0; j < k - 1; j++) {//try to compare the current value to the jth value counted from back to forth
                        if (top_k_value[k - 1 - j] < original_value[i]) {
                            top_k_value[k - 1 - j] = top_k_value[k - 2 - j];
                            top_k_sequence[k - 1 - j] = top_k_sequence[k - 2 - j];
                            position--;
                        } else {
                            break;
                        }
                    }
                    if (position < k) {//we push the current value to the array
                        if (position == 1 & top_k_value[0] < original_value[i]) {//if position is on the second the place, we will compare it with the first one
                            position--;
                        }
                        top_k_value[position] = original_value[i];
                        top_k_sequence[position] = i;
                    }
                }
            } else {
                for (int i = 0; i < k; i++) {
                    top_k_value[i] = Double.POSITIVE_INFINITY;
                }
                for (int i = 0; i < total_length; i++) {
                    int position = k;
                    for (int j = 0; j < k - 1; j++) {//try to compare the current value to the jth value counted from back to forth
                        if (top_k_value[k - 1 - j] > original_value[i]) {
                            top_k_value[k - 1 - j] = top_k_value[k - 2 - j];
                            top_k_sequence[k - 1 - j] = top_k_sequence[k - 2 - j];
                            position--;
                        } else {
                            break;
                        }
                    }
                    if (position < k) {//we push the current value to the array
                        if (position == 1 & top_k_value[0] > original_value[i]) {//if position is on the second the place, we will compare it with the first one
                            position--;
                        }
                        top_k_value[position] = original_value[i];
                        top_k_sequence[position] = i;
                    }
                }
            }
        } else {
            System.out.println("Error: the k is greater than the array length!");
        }
        return top_k_sequence;
    }

    private class Quicksort {//the class to get the sequence array for the sorted array

        public Random RND = new Random();

        private void swap(double[] array, int[] sequence, int[] breakRank, int i, int j) {
            double tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
            int tmp_seq = sequence[i];
            sequence[i] = sequence[j];
            sequence[j] = tmp_seq;
            tmp_seq = breakRank[i];
            breakRank[i] = breakRank[j];
            breakRank[j] = tmp_seq;
        }

        private int partition(double[] array, int[] sequence, int[] breakRank, int begin, int end) {
            int index = begin + RND.nextInt(end - begin + 1);
            double pivot = array[index];
            int pivotrank = breakRank[index];
            swap(array, sequence, breakRank, index, end);
            for (int i = index = begin; i < end; ++i) {
                if (array[i] > pivot || (array[i] == pivot && breakRank[i] < pivotrank)) {
                    swap(array, sequence, breakRank, index++, i);
                }
            }
            swap(array, sequence, breakRank, index, end);
            return (index);
        }

        private void qsort(double[] array, int[] sequence, int[] breakRank, int begin, int end) {
            if (end > begin) {
                int index = partition(array, sequence, breakRank, begin, end);
                qsort(array, sequence, breakRank, begin, index - 1);
                qsort(array, sequence, breakRank, index + 1, end);
            }
        }

        public int[] sort(double[] org_array, int[] breakRank, Boolean ascending) {
            int[] sequence = new int[org_array.length];
            int[] reverse_sequence = new int[org_array.length];
            double[] array = new double[org_array.length];
            for (int i = 0; i < org_array.length; i++) {
                sequence[i] = i;
                array[i] = org_array[i];
            }
            qsort(array, sequence, breakRank, 0, array.length - 1);
            for (int i = 0; i < org_array.length; i++) {
                reverse_sequence[i] = sequence[org_array.length - 1 - i];
            }
            if (ascending) {
                return reverse_sequence;
            } else {
                return sequence;
            }
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        LDBSCAN tmp = new LDBSCAN();
        if (args.length != 5) {
            tmp.runLDBSCAN("sampleData.csv", 15, 10, 2, 0.2);
        } else {
            tmp.runLDBSCAN(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), Double.parseDouble(args[3]), Double.parseDouble(args[4]));
        }
    }
}
