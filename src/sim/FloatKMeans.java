package sim;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Classic k-means
 * 
 * @author Mariusz Paradowski
 */
public class FloatKMeans {
	protected int numberOfClusters;
	protected float [][] clusterCenters;
	protected int [] clusterCount;
	
	protected int [] assignment;
	protected int [] oldAssignment;
	
	protected int iters;
	
	public static boolean VERBOSE = false;
	public static int FIXED_RANDOM = -1;
	
	protected Random random = new Random();
	
	/**
	 * Default constructor
	 * @param numberOfClusters number of clusters
	 */
	public FloatKMeans(int numberOfClusters) {
		this.numberOfClusters = numberOfClusters;
		this.iters = 1000;
		if (FIXED_RANDOM >= 0) { random = new Random(FIXED_RANDOM); }
	}
	
	/**
	 * Constructor
	 * @param numberOfClusters number of clusters
	 * @param iters maximum number of iterations
	 */
	public FloatKMeans(int numberOfClusters, int iters) {
		this.numberOfClusters = numberOfClusters;
		this.iters = iters;
		if (FIXED_RANDOM >= 0) { random = new Random(FIXED_RANDOM); }
	}
	
	/**
	 * Select a feature vector from the dataset to perform init
	 */
	protected float [] selectInitializationFeature(List<float[]> features, int i) {
		int index = random.nextInt(features.size());
		return features.get(index);
	}
	
	/**
	 * Initialize
	 */
	protected void initialize(List<float[]> features) {
		this.clusterCenters = new float[this.numberOfClusters][];
		this.clusterCount = new int[this.numberOfClusters];
		for (int i = 0; i < this.clusterCenters.length; i++) {
			float [] descriptor = this.selectInitializationFeature(features, i);
			this.clusterCenters[i] = descriptor.clone();
		}
		
		this.assignment = new int[features.size()];
		this.oldAssignment = new int[features.size()];
		for (int i = 0; i < features.size(); i++) {
			this.assignment[i] = -1;
		}
	}
	
	/**
	 * Calculate new cluster centers
	 */
	protected void recalculateCenters(List<float[]> features) {
		for (int i = 0; i < this.clusterCenters.length; i++) {
			this.clusterCount[i] = 0;
			for (int j = 0; j < this.clusterCenters[i].length; j++) {
				this.clusterCenters[i][j] = 0;
			}
		}
		
		for (int i = 0; i < features.size(); i++) {
			int cluster = this.assignment[i];
			if (cluster < 0) {
				System.err.println("Internal error");
				System.exit(0);
			}
			this.clusterCount[cluster]++;
			float [] vector = features.get(i);
			for (int j = 0; j < this.clusterCenters[cluster].length; j++) {
				this.clusterCenters[cluster][j] += vector[j];
			}
		}
		
		for (int i = 0; i < this.clusterCenters.length; i++) {
			if (this.clusterCount[i] > 0) {
				for (int j = 0; j < this.clusterCenters[i].length; j++) {
					this.clusterCenters[i][j] = this.clusterCenters[i][j] / this.clusterCount[i];
				}
			}
		}
	}
	
	/**
	 * Euclid distance
	 */
	protected double distance(float[] v1, float[] v2) {
		float dist = 0;
		for (int i = 0; i < v1.length; i++) {
			dist = dist + (v1[i] - v2[i]) * (v1[i] - v2[i]);
		}
		return dist;
	}
	
	protected float distance(float[] v1, float[] v2, float max) {
		float dist = 0;
		for (int i = 0; i < v1.length; i++) {
			dist += (v1[i] - v2[i]) * (v1[i] - v2[i]);
			if (dist > max) {
				return max;
			}
		}
		return dist;
	}
	
	/**
	 * Assign feature vectors to clusters
	 */
	protected void assignClusters(List<float[]> features) {
		for (int i = 0; i < features.size(); i++) {
			float[] feature = features.get(i);
			double dist = Double.MAX_VALUE;
			this.oldAssignment[i] = this.assignment[i];
			this.assignment[i] = -1;
			for (int j = 0; j < this.clusterCenters.length; j++) {
				double cdist = this.distance(feature, this.clusterCenters[j]);
				if (cdist < dist) {
					dist = cdist;
					this.assignment[i] = j;
				}
			}
			if (this.assignment[i] == -1) {
				System.err.println("Internal error.");
				System.exit(0);
			}
		}
	}
	
	/**
	 * Check the stop condition
	 */
	protected boolean checkStop() {
		this.iters--;
		if (this.iters <= 0) {
			if (VERBOSE) {
				System.out.println("Iter terminated.");
			}
			return false;
		}
		for (int i = 0; i < this.assignment.length; i++) {
			if (this.assignment[i] != this.oldAssignment[i]) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Gets cluster centers.
	 * Requires prior execution of clustering.
	 */
	public float[][] getClusterCenters() {
		return this.clusterCenters;
	}
	
	/**
	 * Performs clustering, returns cluster assignment
	 */
	public int[] cluster(List<float[]> features) {
		if (VERBOSE) {
			System.out.println("Init...");
		}
		this.initialize(features);
		boolean first = true;
		while (this.checkStop()) {
			if (!first) {
				if (VERBOSE) {
					System.out.println("Recalculation...");
				}
				this.recalculateCenters(features);
			}
			if (VERBOSE) {
				System.out.println("Iters left " + this.iters + "...");
			}
			if (VERBOSE) {
				System.out.println("Assignment...");
			}
			this.assignClusters(features);
			first = false;
		}
		if (VERBOSE) {
			System.out.println("Iters left: " + this.iters);
		}
		return this.assignment;
	}
	
	/**
	 * Gets the cluster number for a feature vector
	 */
	public int cluster(float[] feature) {
		if(feature.length!=clusterCenters[0].length) System.out.println(Arrays.toString(feature)+" "+Arrays.toString(clusterCenters[0]));
		double distance = Double.MAX_VALUE;
		int cluster = -1;
		for (int i = 0; i < this.clusterCenters.length; i++) {
			double d = this.distance(feature, this.clusterCenters[i]);
			//System.out.println(i + ":" + (int)(d * 100));
			if (d < distance) {
				distance = d;
				cluster  = i;
			}
		}
		//System.out.println(distance * 100 + " " + cluster);
		return cluster;
	}
	
	public double[] clusterDistances(float[] feature) {
		double[] distances = new double[this.clusterCenters.length];
		for (int i = 0; i < this.clusterCenters.length; i++) {
			distances[i] = this.distance(feature, this.clusterCenters[i]);
		}
		return distances;
	}
	
	public boolean read(File file) {
		BufferedReader in = null;
		try {
	        in = new BufferedReader(new FileReader(file));
	        String line = in.readLine();
	        String[] head = line.split(" ");
	        this.numberOfClusters = Integer.parseInt(head[0]);
	        int dim = Integer.parseInt(head[1]);
	        this.clusterCenters = new float[this.numberOfClusters][dim];
	        for (int i = 0; i < this.numberOfClusters; i++) {
	        	line = in.readLine();
	        	String[] words = line.split(" ");
	        	for (int j = 0; j < dim; j++) {
	        		this.clusterCenters[i][j] = Float.parseFloat(words[j]);
	        	}
	        }    
	        in.close();
	    } catch (IOException ioe) {
	    	if (in != null) {
	    		try { in.close(); } catch (Exception e) {};
	    	}
	    	return false;
	    }
	    return true;
	}
	
	public boolean store(File file) {
		PrintWriter out = null;
		try {
			out = new PrintWriter(new FileWriter(file));
			out.println(this.clusterCenters.length + " " + this.clusterCenters[0].length);
			for (int i = 0; i < this.clusterCenters.length; i++) {
				for (int j = 0; j < this.clusterCenters[i].length; j++) {
					out.print(this.clusterCenters[i][j] + " ");
				}
				out.println();
			}
			out.flush();
			out.close();
			return true;
		} catch (IOException ioe) {
			if (out != null) {
				try { out.close(); } catch (Exception ex) {};
			}
			ioe.printStackTrace();
		}
		return false;
	}
}
