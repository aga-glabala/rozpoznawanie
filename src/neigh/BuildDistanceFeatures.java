package neigh;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import run.Paths;
import sim.FloatKMeans;
import basic.FeatureSetLoader;
import basic.IFeature;
import basic.ImageFromMikolajczykFile;
import basic.ImageMutableFeatures;
import basic.SaveMikolajczykFromFeature;
import basic.FeatureSetLoader;

/**
 * Calculates spatial neighbors for a given feature file
 * 
 * @author Mariusz Paradowski
 */
public class BuildDistanceFeatures {
	
	/**
	 * Required number of neighbors
	 */
	private int neighbors;
	

	private FeatureSetLoader lf;
	private FeatureSetLoader ln;
	private int clNum = 30000;
	private FloatKMeans cluster;
	private List<File> content = new ArrayList<File>();
	private Map<File, int[]> cFeatures = new HashMap<File, int[]>();//przypasowanie plik-klastry
	private Map<File, float[][]> cParams = new HashMap<File, float[][]>();//przypasowanie plik-parametry
	private Map<String, int[][]> neighborsCache;
	//private final Semaphore available = new Semaphore(1, true);
		
	/**
	 * Default constructor
	 */
	public BuildDistanceFeatures(int neighbors) {
		this.neighbors = neighbors;
		this.neighborsCache = new HashMap<String, int[][]>();
		lf = new FeatureSetLoader(FeatureSetLoader.HARAFFSIFT,"");
		ln = new FeatureSetLoader(FeatureSetLoader.HARAFFSIFT, ".neighs"+clNum);
		setCluster();
	}
	
	public int getNeighborsCount() {
		return this.neighbors;
	}
	
	//par1 - data, par2 - query
	//x y a b c
	//a(x-u)(x-u)+2b(x-u)(y-v)+c(y-v)(y-v)
	private double distance(float[] par1, float[] par2) {
		//return par2[2]*(par1[0]-par2[0])*(par1[0]-par2[0])+
		//	   2*par2[3]*(par1[0]-par2[0])*(par1[1]-par2[1])+
		//	   par2[4]*(par1[1]-par2[1])*(par1[1]-par2[1]);
		return
			(par1[0] - par2[0]) * (par1[0] - par2[0]) + 
			(par1[1] - par2[1]) * (par1[1] - par2[1]); 
	}
		
	/**
	 * Builds all the neighbors
	 */
	private int[] neighbors(float[][] pars, float[] par, int n) {
		double[] dist = new double[pars.length];
		for (int i = 0; i < pars.length; i++) {
			dist[i] = this.distance(pars[i], par);
		}
		int[] neighs = new int[n];
		for (int i = 0; i < neighs.length; i++) {
			double minDist = Double.MAX_VALUE;
			int minIndex = 0;
			for (int j = 0; j < dist.length; j++) {
				if (dist[j] < minDist) {
					minIndex = j;
					minDist = dist[j];
				}
			}
			dist[minIndex] = Double.MAX_VALUE;
			neighs[i] = minIndex;
		}
		return neighs;
	}
	/**
	 * 
	 * @param pars - parametry wszystkich punktów
	 * @param par - parametry puntu do którego poszukujemy sąsiadów
	 * @param values - klastry wszystkich punktów
	 * @param value - klaster punktu którego poszukujemy
	 * @param n - ile punktów
	 * @return
	 */
	private short[] uniqueNeighbors(float[][] pars, float[] par, int[] values, int value, int n) {
        double[] dist = new double[pars.length];
        for (int i = 0; i < pars.length; i++) {
                dist[i] = this.distance(pars[i], par);
        }
        boolean[] clusters = new boolean[5000];
        clusters[(int)value] = true;

        short[] neighs = new short[n];
        for (int i = 0; i < neighs.length; i++) {
                double minDist = Double.MAX_VALUE;
                short minIndex = 0;
                for (short j = 0; j < dist.length; j++) {
                        if ((dist[j] < minDist)&&(!clusters[values[j]])) {
                                minIndex = j;
                                minDist = dist[j];
                        }
                }
                dist[minIndex] = Double.MAX_VALUE;
                neighs[i] = minIndex;
                clusters[values[minIndex]] = true;
        }
        return neighs;
        
	}
	
	/**
	 * Builds neighbors for given features
	 */
	public int[][] build(float[][] params) {
		int[][] neighs = new int[params.length][];
		for (int i = 0; i < params.length; i++) {
			int[] neigh = this.neighbors(params, params[i], this.neighbors);
			neighs[i] = neigh;
		}
		return neighs;
	}
	
	public synchronized int[][] buildAndCache(String key, float[][] params) {
		//System.out.println("Syncro start");
		if (!this.neighborsCache.containsKey(key)) {
			System.out.println("Neighbors cache storage: " + key);
			this.neighborsCache.put(key, this.build(params));
		}
		int[][] result = this.neighborsCache.get(key);
		//System.out.println("Syncro end");
		return result;
	}
	
	public short[][] getShortNeighbors(IFeature features) {
		float[][] values = features.getValues();
		int elements = values[0].length;
		
		short[][] neighs = new short[values.length][elements];
		for (int i = 0; i < values.length; i++) {
			for (int j = 0; j < elements; j++) {
				neighs[i][j] = (short)values[i][j];
			}
		}
		
		return neighs;
	}
	
	public short[][] unpackNeighbors(short[] packedNeighbors, float[][] params) {
		short[][] neighs = new short[packedNeighbors.length][this.neighbors];
		//short[] equalData = new short[this.neighbors];
		for (int i = 0; i < packedNeighbors.length; i++) {
			double maxDist = this.distance(params[i], params[packedNeighbors[i]]);
			neighs[i][0] = packedNeighbors[i];
			int currentIndex = 1;
			//int equalIndex = 0;
			for (short j = 0; j < params.length; j++) {
				double cDist = this.distance(params[i], params[j]);
				if ((cDist < maxDist)||((cDist == maxDist)&&(j < packedNeighbors[i]))) {
					neighs[i][currentIndex] = j;
					currentIndex++;
				}
				/*if (cDist == maxDist) {
					equalData[equalIndex++] = j;
				}*/
			}
			/*equalIndex = 0;
			while (currentIndex < this.neighbors) {
				neighs[i][currentIndex++] = equalData[equalIndex++];
			}*/
		}
		return neighs;
	}
	
	/**
	 * Builds the neighbors file, given an image and a feature file.
	 * Created neighbors file has ".neighs" extension.
	 * TODO zwykłe sąsiedztwo
	 */
	public void build(File imageFile, String extension) {
		IFeature feature = new ImageFromMikolajczykFile(imageFile, extension);
		float[][] params = feature.getParams();
		if (params == null) {
			System.out.println("Error. Unable to read feature files for " + imageFile.getName());
			return;
		}
		int n = this.neighbors;
		
		float[][] newFeatures = new float[params.length][n];
		
		for (int i = 0; i < params.length; i++) {
			int[] neigh = this.neighbors(params, params[i], n);
			for (int j = 0; j < neigh.length; j++) {
				newFeatures[i][j] = neigh[j];
			}
		}
		
		ImageMutableFeatures distanceFeatures = new ImageMutableFeatures(params, newFeatures);
		File newFile = new File(feature.getImageFile().getPath());
		newFile= new File(newFile.getParentFile().getParent() +"/neighs/"+ feature.getImageFile().getName()+extension + ".neighs"+clNum);
		if (!SaveMikolajczykFromFeature.saveFeaturesInteger(newFile, distanceFeatures)) {
			System.out.println("Error. Feature storage failure for image " + newFile.getName());
		}
	}
	
	/**
	 * TODO unikalne sądziedztwo
	 */
	public void uniqueBuild(File imageFile, String extension) {
		IFeature feature = new ImageFromMikolajczykFile(imageFile, extension);
		float[][] params = feature.getParams();
		if (params == null) {
			System.out.println("Error. Unable to read feature files for " + imageFile.getName());
			return;
		}
		int n = this.neighbors;
		
		float[][] newFeatures = new float[params.length][n];
		int[]values=getFeaturs(imageFile);
		for (int i = 0; i < params.length; i++) {
			short[] neigh = this.uniqueNeighbors(params, params[i], values, values[i], n);
			for (int j = 0; j < neigh.length; j++) {
				newFeatures[i][j] = neigh[j];
			}
		}
		
		ImageMutableFeatures distanceFeatures = new ImageMutableFeatures(params, newFeatures);
		File newFile = new File(feature.getImageFile().getPath());
		newFile= new File(newFile.getParentFile().getParentFile().getParent() +"/neigh/haraff-sift/"+ feature.getImageFile().getName()+extension + ".unique.neighs");
		if (!SaveMikolajczykFromFeature.saveFeaturesInteger(newFile, distanceFeatures)) {
			System.out.println("Error. Feature storage failure for image " + newFile.getName());
		}
	}
	
	private void setCluster()
	{
		File clusterFile = new File(Paths.base+"9.centroids30000");
		System.out.println(clusterFile);
		if (!clusterFile.exists()) {
			System.out.print("Creating clustering data");
			List<float[]> cData = new ArrayList<float[]>();
			for (File img : this.content) {
				IFeature[] fs = lf.getFeatures(img);
				IFeature f = fs[0];
				float[][] feat = f.getValues();
				for (int i = 0; i < feat.length; i+=6) {
					//if (i % 3 == 0) {
						cData.add(feat[i]);
					//}
				}
				System.out.print(".");
			}
			System.out.println();
			System.out.println("Clustering data ("+cData.size()+")...");
			this.cluster = new FloatKMeans(clNum, 5);
			this.cluster.cluster(cData);
			cData = null;
			this.cluster.store(clusterFile);
		} else {
			System.out.println("Reading cluster centers...");
			this.cluster = new FloatKMeans(clNum, 5);
			this.cluster.read(clusterFile);
		}
	}
	
	private int[] getFeaturs(File img) 
	{
		IFeature[] fs = lf.getFeatures(img);
		IFeature f = fs[0];
		float[][] feat = f.getValues();
		int[] cFeat = new int[feat.length];
		for (int i = 0; i < feat.length; i++) {
			cFeat[i] = this.cluster.cluster(feat[i]);
			//System.out.println("Assign: " + cFeat[i]);
		}
		return cFeat;
	}

	public static void main(String[] args) {
		BuildDistanceFeatures distances = new BuildDistanceFeatures(60);
		File folder = new File(Paths.imPath);
		
		//przypisanie do klastrow
		
		
		
		
		String ext;
		if(args.length>1)
			ext = "."+args[1];
		else
			ext = "";
		File[] files = folder.listFiles();
		for (File file : files) {
			if (file.getName().toLowerCase().endsWith(".png") && !new File(Paths.base +"/neighs/"+ file.getName()+".haraff.sift" + ".neighs"+distances.clNum).exists()) {
				System.out.println("Processing: " + file.getPath());
				//distances.uniqueBuild(new File("/home/ljercinski/SIProjekt/"+args[0]+"/features/haraff-sift/"+file.getName()), ".haraff.sift"+ext);
				distances.build(new File(Paths.fPath+file.getName()), ".haraff.sift"+ext);
				
			}
		}
	}
}
