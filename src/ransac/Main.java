package ransac;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import basic.BOW;
import basic.FeatureSetLoader;
import basic.IFeature;
import basic.ImageData;

public class Main {
	static final int iters=100;  //ile razy losujemy punkty dla modelu
	static final double eps=0.01; //maksymalna odległość z jaką model mógł się pomylić i tak uważamy ten punkt za inliera
	public static int clNum = 15000;
	public static int nElem = 5; //ile punktów o najlepszym sąsiedztwie dla klastra bierzemy
	static float min=1.3f; //minimalny stosunek sumy dwóch krawędzi trójkąta do trzeciej krawędzi
	static float minCoverage=0.04f; //minimalna możliwa długość boku trójkąta
	static float maxCoverage=0.3f; //maksymalna możliwa długość boku trójkąta
	//static String path = "/files/workspace/AnalizaObrazow/";
	//static String path = "/home/agnis/Desktop/AnalizaObrazow/";
	static String path = "/media/AD81-FCC4/AnalizaObrazow/";
	static String imagesPath = path+"images/"; //obrazy png
	static String featuresPath = path+"features/"; //sifty
	static String tmpPath = path+"tmp"; //tymczasowy na obrazki
	static String[] modes = new String[] {"Fast","Slow","Neigh"}; //tryb 0-szybki, 1-wolny, 2-z sąsiadami
	static int mode = 2;
	public static void main(String[] args) throws IOException {
		float[][][] params;  //plik -> punkt -> param
		float[][][] values;  //plik -> punkt -> sift
		int[][] clusters; //plik -> punkt -> klaster
		short[][][] neighs; //plik -> punkt -> sąsiedzi
		File[] files;
		List<Integer>[][] clusterpts;//List[file][cluster] -> set of points
		files = new File(imagesPath).listFiles(new FileFilter() {
			
			@Override
			public boolean accept(File pathname) {
				if(pathname.getName().endsWith("png")) return true;
				return false;
			}
		});
		//read clusters files
		
		
		
		FeatureSetLoader ln = new FeatureSetLoader(FeatureSetLoader.HARAFFSIFT, ".neighs");
		neighs = new short[files.length][][];
		for (int j = 0; j < files.length; j++) {
			IFeature[] features = ln.getFeatures(new File(files[j].getParentFile().getParent()+"/neighs/"+files[j].getName()));
			IFeature distances = features[0];
			float[][] dists = distances.getValues();

			
			short[][] neighsFile = new short[dists.length][55];
			for (int i = 0; i < dists.length; i++) {
				for (int k = 0; k < 55; k++) {
					neighsFile[i][k] = (short)dists[i][k+5];
				}
			}
			neighs[j]=neighsFile;
		}
//		String[][] names = {
//				new String[] {"all_souls_000002.png","all_souls_000006.png"}, 
//				new String[] {"all_souls_000008.png","all_souls_000027.png"}, 
//				new String[] {"all_souls_000093.png","all_souls_000132.png"}, 
//				new String[] {"all_souls_000006.png","all_souls_000015.png"}, 
//				new String[] {"all_souls_000002.png","all_souls_000026.png"}, 
//				new String[] {"all_souls_000087.png","all_souls_000093.png"}, 
//				new String[] {"all_souls_000073.png","all_souls_000090.png"}, 
//				new String[] {"all_souls_000090.png","all_souls_000136.png"}, 
//				new String[] {"all_souls_000087.png","all_souls_000136.png"}, 
//				new String[] {"all_souls_000022.png","all_souls_000026.png"}, 
// 			    };
		String[][] names = {
				new String[] {"all_souls_000022.png","all_souls_000026.png"}, 
				new String[] {"all_souls_000087.png","all_souls_000136.png"}, 
				new String[] {"all_souls_000090.png","all_souls_000136.png"}, 
				new String[] {"all_souls_000073.png","all_souls_000090.png"}, 
				new String[] {"all_souls_000087.png","all_souls_000093.png"}, 
				new String[] {"all_souls_000002.png","all_souls_000026.png"}, 
				new String[] {"all_souls_000006.png","all_souls_000015.png"}, 
				new String[] {"all_souls_000093.png","all_souls_000132.png"}, 
				new String[] {"all_souls_000008.png","all_souls_000027.png"}, 
				new String[] {"all_souls_000002.png","all_souls_000006.png"}, 
				};
			BOW b = new BOW(files, clNum);
			b.allBows();
			int[][] bows = b.getBows(); 
			clusters = b.clusters;
			params = new float[files.length][][];
			values = new float[files.length][][];
			
			clusterpts = new ArrayList[files.length][clNum];
			FeatureSetLoader loader = new FeatureSetLoader(FeatureSetLoader.HARAFFSIFT, "");
			for (int j = 0; j < files.length; j++) {
				File img = files[j];
				IFeature[] fs = loader.getFeatures(new File(featuresPath+img.getName()));
				//IFeature f = fs[0];
				params[j] = fs[0].getParams();
				
				values[j] = fs[0].getValues();
				for (int i = 0; i < clusters[j].length; i++) {
					if(clusterpts[j][(int)clusters[j][i]]==null) {
						clusterpts[j][(int)clusters[j][i]] = new ArrayList<Integer>();
					}
					clusterpts[j][(int)clusters[j][i]].add(i);
				}
			}
			
		for (int k = 0; k < names.length; k++) {
			int i=getIndex(files, names[k][0]); 
			int j=getIndex(files, names[k][1]); 
			//for (int j = 0; j < files.length; j++) {	
			ImageData q = new ImageData(params[i], values[i], clusters[i], neighs[i], clusterpts[i], bows[i], files[i]);
			ImageData d = new ImageData(params[j], values[j], clusters[j], neighs[j], clusterpts[j], bows[j], files[j]);
			Ransac r = new Ransac(q,d);
			List<int[]> pairs = mode==0 ? q.generatePairsFast(d) : mode==1 ? q.generatePairs(d) : q.neighBasedPairs(d);
			System.out.println(k+"/"+names.length+" Pairs: " + pairs.size());
			//q.drawPairs(d, pairs, new File(tmpPath+pairs.size()+"_AllPairs5_"+files[i].getName()+"_"+files[j].getName()));
			Model m = r.getBestModel(pairs);
			System.out.println(k+"/"+names.length+" Ransac: " +m.getInliers().size());
			String path = tmpPath+"/";//+"_"+modes[mode]+"_"+clNum+"/";
			q.drawPairs(d, m.getInliers(), new File(path+m.getInliers().size()+"_RANSAC-"+files[i].getName()+"_"+files[j].getName()+"_"+modes[mode]+"_"+clNum), m);
		}
	}
	static int getIndex(File[] files, String name) {
		for (int i = 0; i < files.length; i++) {
			if(files[i].getName().equals(name)) return i;
		}
		return -1;
	}
}
