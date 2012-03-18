package run;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import ransac.Model;
import ransac.Ransac;
import sim.ClusteredSimilarityMass;

import basic.BOW;
import basic.FeatureSetLoader;
import basic.IFeature;
import basic.ImageData;
import basic.Ordered;
import basic.Result;

public class Main {	
	public static int clNum = 15000;
	
	//RANSAC PARAMS
	static final int iters=100;  //ile razy losujemy punkty dla modelu
	static final double eps=0.01; //maksymalna odległość z jaką model mógł się pomylić i tak uważamy ten punkt za inliera
	public static int nElem = 5; //ile punktów o najlepszym sąsiedztwie dla klastra bierzemy
	static float min=1.3f; //minimalny stosunek sumy dwóch krawędzi trójkąta do trzeciej krawędzi
	static float minCoverage=0.04f; //minimalna możliwa długość boku trójkąta
	static float maxCoverage=0.3f; //maksymalna możliwa długość boku trójkąta
	
	//CSM PARAMS
	boolean unique = false;
	boolean ellipse = false;
	int elements = 40;
	int firstN = 5;
	boolean cntpp = false;
	String method = "maxAreaCluster"; //{"maxAreaCluster", "minAreaCluster", "minArea1000", "maxArea1000"};
	
	static String[] modes = new String[] {"Fast","Slow","Neigh"}; //tryb 0-szybki, 1-wolny, 2-z sąsiadami
	static int mode = 2;
	
	//DATA
	float[][][] params;  //plik -> punkt -> param
	float[][][] values;  //plik -> punkt -> sift
	int[][] clusters; //plik -> punkt -> klaster
	short[][][] neighs; //plik -> punkt -> sąsiedzi
	File[] files;
	int[][] bows;
	BOW b;
	List<Integer>[][] clusterpts;//List[file][cluster] -> set of points
	Map<File, Integer> indices;
	//METHODS
	static final int CNS = 0;
	static final int CSMFast = 1;
	static final int CSM = 2;
	static final int BOW = 3;
	static final int BOWTFiDF = 4;
	static final int BOWTFiDF_L1 = 5;
	static final int RANSAC = 6;
	
	ClusteredSimilarityMass seek_fast;
	ClusteredSimilarityMass seek_full;
	
	public static void main(String[] args) throws IOException {
		Main m = new Main();
		m.loadData();
		
	}
	
	private void save(Result[] res) {
		switch (res[0].method) {
		case Main.CNS:
			for (Result d : res) {
				// TODO
			}
			break;
		case Main.CSMFast:
			for (Result d : res) {
				seek_fast.
			}
			break;
		case Main.CSM:
			for (ImageData d : ids) {
				list.add(new Ordered<ImageData>(d,seek_full.sim(q.clusters, q.neighs, d.clusters, d.neighs, q.params, d.params, q.f, d.f)));
			}
			break;
		case Main.BOW:
			for (ImageData d : ids) {
				list.add(new Ordered<ImageData>(d, b.dist(indices.get(q.f), indices.get(d.f))));
			}
			break;
		case Main.BOWTFiDF:
			for (ImageData d : ids) {
				list.add(new Ordered<ImageData>(d, b.distTFIDF(indices.get(q.f), indices.get(d.f))));
			}
			break;
		case Main.BOWTFiDF_L1:
			for (ImageData d : ids) {
				list.add(new Ordered<ImageData>(d, b.distTFIDFl1(indices.get(q.f), indices.get(d.f))));	
			}				
			break;
		case Main.RANSAC:
			for (ImageData d : ids) {
				list.add(new Ordered<ImageData>(d, ransac(q,d)));
			}
			break;
		default:
			break;
	}
	}
	
	private Result[] next(Result[] results, int method, int cut) {
		Result[] res = new Result[results.length];
		for(int i=0; i<results.length; i++) {
			res[i]=getResult(results[i].ffor, results[i].getFiles(cut),method);
		}
		return res;
	}
	
	private Result getResult(ImageData q, ImageData[] ids, int method) {
		List<Ordered<ImageData>> list = new ArrayList<Ordered<ImageData>>();
		switch (method) {
			case Main.CNS:
				for (ImageData d : ids) {
					// TODO
				}
				break;
			case Main.CSMFast:
				for (ImageData d : ids) {
					list.add(new Ordered<ImageData>(d,seek_fast.sim(q.clusters, q.neighs, d.clusters, d.neighs, q.params, d.params, q.f, d.f)));
				}
				break;
			case Main.CSM:
				for (ImageData d : ids) {
					list.add(new Ordered<ImageData>(d,seek_full.sim(q.clusters, q.neighs, d.clusters, d.neighs, q.params, d.params, q.f, d.f)));
				}
				break;
			case Main.BOW:
				for (ImageData d : ids) {
					list.add(new Ordered<ImageData>(d, b.dist(indices.get(q.f), indices.get(d.f))));
				}
				break;
			case Main.BOWTFiDF:
				for (ImageData d : ids) {
					list.add(new Ordered<ImageData>(d, b.distTFIDF(indices.get(q.f), indices.get(d.f))));
				}
				break;
			case Main.BOWTFiDF_L1:
				for (ImageData d : ids) {
					list.add(new Ordered<ImageData>(d, b.distTFIDFl1(indices.get(q.f), indices.get(d.f))));	
				}				
				break;
			case Main.RANSAC:
				for (ImageData d : ids) {
					list.add(new Ordered<ImageData>(d, ransac(q,d)));
				}
				break;
			default:
				break;
		}
		return new Result(list, method==Main.RANSAC ? -1 : 1, q, method);
	}
	
	private int ransac(ImageData d, ImageData q) {
		Ransac r = new Ransac(q,d);
		List<int[]> pairs = mode==0 ? q.generatePairsFast(d) : mode==1 ? q.generatePairs(d) : q.neighBasedPairs(d);
		Model m = r.getBestModel(pairs);
		return m.getInliers().size();
	}
	
	/**
	 * 
	 * @param query - pairs of query
	 * @param data - pairs of data
	 * @throws IOException 
	 */
	private void drawPairs(ImageData q, ImageData d, List<int[]> pairs, File outF) throws IOException {
		BufferedImage out = new BufferedImage(Math.max(q.img.getWidth(), d.img.getWidth()), q.img.getHeight()+ d.img.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics g = out.getGraphics();
		g.drawImage(q.img, 0, 0, null);
		g.drawImage(d.img, 0, q.img.getHeight(), null);
		for(int[] p : pairs) {
				g.drawLine((int)(q.params[p[0]][0]*q.img.getWidth()), (int)(q.params[p[0]][1]*q.img.getHeight()), 
					       (int)(d.params[p[1]][0]*d.img.getWidth()), q.img.getHeight()+ (int)(d.params[p[1]][1]*d.img.getHeight()));
		}
		ImageIO.write(out, "png", outF);
	}
	
	private void loadData() {
		indices = new HashMap<File, Integer>();
		files = new File(Paths.imPath).listFiles(new FileFilter() {
			
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
			indices.put(files[j], j);
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
		b = new BOW(files, clNum);
		b.allBows();
		bows = b.getBows(); 
		b.evaluateTF();
		clusters = b.clusters;
		params = new float[files.length][][];
		values = new float[files.length][][];
		clusterpts = new ArrayList[files.length][clNum];
		FeatureSetLoader loader = new FeatureSetLoader(FeatureSetLoader.HARAFFSIFT, "");
		for (int j = 0; j < files.length; j++) {
			File img = files[j];
			IFeature[] fs = loader.getFeatures(new File(Paths.fPath+img.getName()));
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
		//CSMFast
		seek_fast = new ClusteredSimilarityMass("", method);
		seek_fast.filtering=true;
		seek_fast.clNum = clNum;
		seek_fast.firstN = firstN;
		seek_fast.elements = elements;
		seek_fast.cntpp = cntpp;
		seek_fast.ellipse =ellipse;
		seek_fast.unique = unique;
		//CSM
		seek_full = new ClusteredSimilarityMass("", "");
		seek_full.filtering=false;
		seek_full.clNum = clNum;
		seek_full.firstN = firstN;
		seek_full.elements = elements;
		seek_full.cntpp = cntpp;
		seek_full.ellipse =ellipse;
		seek_full.unique = unique;
	}
	
}
