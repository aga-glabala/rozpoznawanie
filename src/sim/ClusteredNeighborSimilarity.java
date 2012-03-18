package sim;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import run.Paths;

import basic.Chart;
import basic.FeatureSetLoader;
import basic.IFeature;
import basic.Ordered;
import basic.Results;

public class ClusteredNeighborSimilarity {
	/**
	 * 1. dla każdego punktu wyznaczone jest sąsiedztwo 
	 * 2. klasteryzacja bowów sąsiedztwa 
	 * 3. do każdego punktu jest przypasowany klaster zawierający informację o jego sąsiedztwie 
	 * 4. poszukiwanie obrazów z największą liczbą punktów z takim samym klastrem sąsiedztwa 
	 */
	
	private Map<File, short[][]> cNeighs = new HashMap<File, short[][]>(); //plik -> [punkt -> [sasiedzi]]
	private Map<File, int[]> cFeatures = new HashMap<File, int[]>(); //plik -> [punkt -> klaster]
	private Map<File, int[]> cFeaturesAux = new HashMap<File, int[]>(); //plik -> [punkt -> klaster] mniejsze
	private Map<File, float[][]> cBOWNeighs = new HashMap<File, float[][]>(); // plik -> [punkt -> [bow sasiedztwa]]
	private Map<File, PointNeigh[]> cClusterNeigh = new HashMap<File, PointNeigh[]>(); //plik -> [punkt -> (klaster, klaster sąsiedztwa)]
	public List<File> content = new ArrayList<File>();
	public int clNum = 1000;
	public int clNumAux = 80;
	public int clNumNeighs = 80;
	private FeatureSetLoader lf;
	private FeatureSetLoader ln;
	
	private FloatKMeans cluster; //klasteryzacja siftów
	private FloatKMeans clusterAux; //pomocnicza klasteryzacja siftów (mniej klastrów)
	private FloatKMeans clusterNeighs; //klasteryzacja sąsiedztwa
	
	long time;
	boolean filtering=false; //czy sifty były filtrowane
	private String name; 
	public ClusteredNeighborSimilarity(String name)
	{
		this.name = name;
		if(name.equals(""))
			name="";
		else
			name="."+name;
		lf = new FeatureSetLoader(FeatureSetLoader.HARAFFSIFT,name);
		ln = new FeatureSetLoader(FeatureSetLoader.HARAFFSIFT, ".neighs",name);
	}
	
	public void init(File[] files) {
		//System.out.println("Looking for images...");
		this.content = new ArrayList<File>();
		for (File file : files) {
			if (file!=null && file.getName().toLowerCase().endsWith(".png")) {
				this.content.add(file);
			}
			else
				System.out.println(file);
		}
		System.out.println("setClusterFeature");
		setClusterFeature();
		
		for (File img : this.content) {
			int[] cFeat = this.getFeaturs(img);
			int[] cFeatAux = getFeatursAux(img);
			this.cFeatures.put(img, cFeat);
			this.cFeaturesAux.put(img, cFeatAux);
		}

		//System.out.print("Reading the neighborhood");
		for (File img : this.content) {
			short[][] neighs = this.getNeighbors(img);
			this.cNeighs.put(img, neighs);
		}
		
		//Część dotycząca nowej metody:
		//tworzenie bowów sąsiedztwa dla wszystkich obrazów 
		//System.out.print("Creating BOW of neighborhood");
		for (File img : this.content) {
			float[][] bows = this.getBOWNeighs(img);
			this.cBOWNeighs.put(img, bows);
		}
		
		
		//tworzenie nowej klasteryzacji - tym razem do sąsiedztwa
		//System.out.println("setClusterNeighs");
		setClusterNeighs();
		
		//System.out.print("Assigning cluster to neighbors of points");
		for (File img : this.content) {
			float[][] bows = this.cBOWNeighs.get(img);
			int[] clustersF = this.cFeatures.get(img);
			PointNeigh[] clusters = new PointNeigh[bows.length];
			for(int i=0; i<bows.length; i++)
			{
				clusters[i]=new PointNeigh(clustersF[i],clusterNeighs.cluster(bows[i])); //(numer klastra, numer sasiedztwa)
			}
			Arrays.sort(clusters);
			cClusterNeigh.put(img,clusters);
		}
	}
	
	private void setClusterFeature()
	{
		File clusterFile = new File(Paths.path+"-"+this.clNum+".kmeans");
		File clusterFileAux = new File(Paths.path+"-"+this.clNum+"-aux.kmeans");
		if (!clusterFile.exists() || !clusterFileAux.exists()) {
			System.out.print("Creating clustering data");
			List<float[]> cData = new ArrayList<float[]>();
			for (File img : this.content) {
				IFeature[] fs = lf.getFeatures(img);
				IFeature f = fs[0];
				float[][] feat = f.getValues();
				for (int i = 0; i < feat.length; i+=3) {
					if (feat[i].length==128) {
						cData.add(feat[i]);
					}
				}
			}
			this.cluster = new FloatKMeans(clNum, 5);
			this.cluster.cluster(cData);
			this.cluster.store(clusterFile);
			this.clusterAux = new FloatKMeans(clNumAux, 5);
			this.clusterAux.cluster(cData);
			this.clusterAux.store(clusterFileAux);
			
			cData = null;
		} else {
			this.cluster = new FloatKMeans(clNum, 50);
			this.cluster.read(clusterFile);
			this.clusterAux = new FloatKMeans(clNumAux, 50);
			this.clusterAux.read(clusterFileAux);
		}
	}
	
	private void setClusterNeighs()
	{
		//tworzenie nowej klasteryzacji - tym razem do sąsiedztwa
		File clusterFile = new File(Paths.path+"-"+clNumNeighs+"-"+clNumAux+"-neighs.kmeans");
		if (!clusterFile.exists()) {
			//System.out.print("Creating clustering data (neighs)");
			List<float[]> cData = new ArrayList<float[]>();
			for (File img : this.content) {
				float[][] bow =cBOWNeighs.get(img);
				for (int i = 0; i < bow.length; i++) {
					cData.add(bow[i]);
				}
				System.out.print(".");
			}
			//System.out.println();
			//System.out.println("Clustering data ("+cData.size()+")...");
			clusterNeighs = new FloatKMeans(clNumNeighs, 50);
			clusterNeighs.cluster(cData);
			cData = null;
			clusterNeighs.store(clusterFile);
		} else {
			//System.out.println("Reading cluster centers...");
			clusterNeighs = new FloatKMeans(clNumNeighs, 50);
			clusterNeighs.read(clusterFile);
		}
	}
	
	/**
	 * zwraca bowy sąsiedztwa dla wszystkich punktów z obrazu 
	 */
	private float[][] getBOWNeighs(File img) {
		int[] feat = cFeaturesAux.get(img); //[punkt -> klaster]
		short[][] neighs = cNeighs.get(img); //[punkt -> [sąsiedzi]]
		float[][] bows = new float[feat.length][clNumAux]; //[punkt -> [bow klastrów sasiadów]]
		for (int i = 0; i < feat.length; i++) {  
			for(int j=0; j<neighs[i].length;j++)
			{
				//sprawdzamy klaster każdego sąsiada i dodajemy do bowa
				bows[i][feat[neighs[i][j]]]+=1;
			}
		}
		return bows;
	}
	
	private int[] getFeaturs(File img) {
		IFeature[] fs = lf.getFeatures(img);
		IFeature f = fs[0];
		float[][] feat = f.getValues(); //feat[numer linii] = [tablica z cechami]
		int[] cFeat = new int[feat.length]; //tablica o takiej długości jak ilość linii
		for (int i = 0; i < feat.length; i++) {
			//do każdej linii (do każdego punktu) zwraca numer klastra
			cFeat[i] = this.cluster.cluster(feat[i]);
		}
		 
		return cFeat;
	}
	private int[] getFeatursAux(File img) {
		IFeature[] fs = lf.getFeatures(img);
		IFeature f = fs[0];
		float[][] feat = f.getValues(); //feat[numer linii] = [tablica z cechami]
		int[] cFeat = new int[feat.length]; //tablica o takiej długości jak ilość linii
		for (int i = 0; i < feat.length; i++) {
			//do każdej linii (do każdego punktu) zwraca numer klastra
			cFeat[i] = this.clusterAux.cluster(feat[i]);
		}
		 
		return cFeat;
	}
	private boolean storeFeatures(File file) {
		PrintWriter out = null;
		try {
			out = new PrintWriter(new FileWriter(file));
			out.println(this.content.size());
			for (File img : this.cFeatures.keySet()) {
				out.print(img.getPath() + " ");
				int[] cFeat = this.cFeatures.get(img);
				for (int i = 0; i < cFeat.length; i++) {
					out.print(cFeat[i] + " ");
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
	private void getClusteredNeighbor(File img) {
		
	}
	private short[][] getNeighbors(File img) {
		IFeature[] features = ln.getFeatures(img);
		IFeature distances = features[0];
		float[][] dists = distances.getValues();

		int elements = Math.min(40, dists[0].length);
		int firstN = 5;

		short[][] neighs = new short[dists.length][elements - firstN];
		for (int i = 0; i < dists.length; i++) {
			for (int j = 0; j < elements - firstN; j++) {
				neighs[i][j] = (short)dists[i][j + firstN];
			}
		}
		return neighs;
	}
	
	public List<Ordered<File>> seek(File query) {
		long start = System.currentTimeMillis();
		List<Ordered<File>> res = new ArrayList<Ordered<File>>();
		for (File img : this.content) {
			if (img.getName().equals(query.getName())) {
				continue;
			}
			float s = this.sim(query, img);
			if (s > 0) {
				res.add(new Ordered<File>(img, s));
			}
			
		}
		
		long end = System.currentTimeMillis();
		time = end - start;
		Collections.sort(res);
		return res;
		
	}
	
	private float sim(File query, File data) {
		float res=0;
		PointNeigh[] bowQ = cClusterNeigh.get(query);
		PointNeigh[] bowD = cClusterNeigh.get(data);

		
		int j=0;//wskaźnik do poruszania się po bowD
		int i=0;//wskaźnik do poruszania się po bowQ
		while(i<bowQ.length && j<bowD.length) {
			if(bowQ[i].compareTo(bowD[j])<0) i++;
			else if(bowQ[i].equals(bowD[j]))
			{
				res++;
				j++;
				i++;
			}
			else j++;
		}

		return res;
	}

	public static void main(String[] args) throws IOException
	{
		File dir =new File(Paths.imPath);
		final File[] files = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name)
			{
				return name.toLowerCase().endsWith(".png"); 
			}
		});
		for(final int clNeigh : Paths.tabN) {
			new Thread() {public void run() {
					for(int clNum : Paths.tab2) {
						for(int clAux : Paths.tabA) {
							for(String name : new String[] {"minAreaCluster", "maxAreaCluster","minArea1000","maxArea1000", ""})
							{
								if(!new File(Paths.resPath+"mp-"+"cns-"+name+"-"+clNum+"-"+clNeigh+"-"+clAux+"-matching").exists())
								{
									System.out.println(name+" "+clNum+" "+clNeigh+" "+clAux);
									ClusteredNeighborSimilarity cns = new ClusteredNeighborSimilarity(name);
									cns.clNum = clNum;
									cns.clNumAux = clAux;
									cns.clNumNeighs = clNeigh;
									
									cns.init(files);
									List<Ordered<File>> result;
									Results res = new Results("matching");
									for(int i=0; i<cns.content.size(); i++)
									{
										result = cns.seek(cns.content.get(i));
										res.add(cns.content.get(i).toString(), result, cns.time);
										//System.out.print(".");
									}
									if(name.equals("")) name="base";
									String name_exper="cns-"+name+"-"+cns.clNum+"-"+cns.clNumNeighs+"-"+cns.clNumAux;
									try {
										res.generateHtml(name_exper);
										res.generateResults(name_exper);
									} catch (IOException e) {
										e.printStackTrace();
									}
								}
							}
						}
					}
				}
			}.start();
		}
		Chart ch = new Chart("matching");
	}
	private class PointNeigh implements Comparable {
		//zawiera informację o klastrze danego punktu i klastrze jego sąsiedztwa 
		int cluster;
		int clusterNeigh;
		PointNeigh(int cl, int cln)
		{
			cluster = cl;
			clusterNeigh = cln;
		}
		@Override
		public int compareTo(Object o)
		{
			if(o instanceof PointNeigh) {
				PointNeigh pc = (PointNeigh)o;
					//System.out.println("PN "+cluster+" "+pc.cluster+" "+clusterNeigh+" "+pc.clusterNeigh);
					
				if(cluster > pc.cluster) return 1;
				else if(cluster == pc.cluster)
				{
					if(clusterNeigh == pc.clusterNeigh) return 0;
					else if(clusterNeigh > pc.clusterNeigh) return 1;
					else return 1;
				}
				else return -1;
			}
			else return -2;
		}
		public boolean equals(PointNeigh pn)
		{
			return cluster == pn.cluster && clusterNeigh==pn.clusterNeigh;
		}
	}
}
