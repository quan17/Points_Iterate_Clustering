Use the following command pattern to run the code:

java clustering/LDBSCAN "filename" "MinPts for LOF" "MinPts for LDBSCAN" "LOFUB" "percentage of fluctuation" 

for example: java clustering/LDBSCAN sampleData.csv 15 10 2 0.2 > output.csv


Output:

id: start from 0;
attr1 to attri m: your original data information
lrd: the local reachable density of the current record
lof: the local outlier factor of the current record
classID: the cluster id of the current record. If it is 0, it indicates outliers. If it is a non-zero number i, it indicates the current record belongs to the cluster i.


Note:
1. The input data don't need the first line to describe the attributes.
2. We don't provide the function to normalize the attributes or do any type of feature selection. If you want to, you should preprocess your data first and then save them into a csv file.
3. We only use the Euclidean distance function. If you want to use the other distance function, you can change the calculateDistance(int MinPts) function in the source code.