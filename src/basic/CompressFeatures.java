package basic;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class CompressFeatures {
	private static String path =  "/home/aglabala/oxford/";
	private FeatureSetLoader loader = new FeatureSetLoader("", "");
	private FloatKMeans cluster;
	private int clusters = 10000;
	private int modulo = 10;
	private static String bowPath = path+"clusters/";
	public CompressFeatures(int clusters, int modulo) {
		this.clusters = clusters;
		this.modulo = modulo;
		FeatureSetLoader.USECACHE = false;
	}
		
	public void initialize(File clusterFile, List<File> content) {
		System.out.println("Modulo: " + this.modulo);
		this.cluster = new FloatKMeans(clusters, 1);
		if (clusterFile.exists()) {
			this.cluster.read(clusterFile);
			return;
		}
		else {
			List<float[]> cData = new ArrayList<float[]>();
			for (int j = 0; j < content.size(); j++) {
				File img = content.get(j);
				IFeature[] fs = loader.getFeatures(img);
				IFeature f = fs[0];
				float[][] feat = f.getValues();
				for (int i = 0; i < feat.length; i++) {
					if (i % modulo == 0) {
						cData.add(feat[i]);
					}
				}
				System.out.print(".");
				if (j % 50 == 0) {
					System.out.println(j + " of " + content.size() + " is " + cData.size());
				}
			}
			System.out.println();
			System.out.println("Clustering data ("+cData.size()+")...");
			this.cluster = new FloatKMeans(clusters, 10);
			FloatKMeans.FIXED_RANDOM = 0;
			FloatKMeans.VERBOSE = true;
			this.cluster.dir = clusterFile.getParentFile();
			this.cluster.cluster(cData);
			cData = null;
			this.cluster.store(clusterFile);
		}
	}
	
	public void compress(File image) {
		System.out.println("Kompresuje: " + image.getName());
		IFeature feat = loader.getFeatures(image)[0];
		float[][] par = feat.getParams();
		float[][] val = feat.getValues();
		float[][] nVal = new float[val.length][1];
		for (int i = 0; i < val.length; i++) {
			//int cl = this.cluster.cluster(this.featureToByte(val[i]));
			int cl = this.cluster.cluster(val[i]);
			nVal[i][0] = cl;
		}
		
		IFeature stor = new ImageMutableFeatures(par, nVal);
		File zapis = new File(bowPath+image.getName() + ".cl" + clusters);
		System.out.println("Zapisuje: " + zapis.getPath());
		SaveMikolajczykFromFeature.save(zapis, stor);
	}
	
	public static void main(String[] args) {
		//File folder = new File("/home/mparadowski/dane/oxbuildings/oxbuild_images");
		//File folder = new File("/home/mparadowski/dane/databases/matching");
		System.out.println("CompressFeatures SIFT-BOW generator, ver 2011.09.02.");
		File folder = new File(path+"features/");

		//args = new String[] {"30000", "10"};
		if (args.length < 2) {
			System.out.println("CompressFeatures.jar [number-of-clusters] [modulo] [-r]");
			return;
		}
		int clusters = Integer.parseInt(args[0]);
		int modulo = Integer.parseInt(args[1]);
		boolean recursive = false;
		if (args.length >= 3) {
			if (args[2].equals("-r")) {
				recursive = true;
			}
		}
		
		System.out.println("Clusters: " + clusters);
		System.out.println("Modulo  : " + modulo);
		
		List<File> imageFiles = new ArrayList<File>();
		List<File> fs = null;
		if (recursive) {
			fs = FileStructure.read(folder);
		} else {
			fs = new ArrayList<File>();
			for (File f : folder.listFiles()) {
				fs.add(f);
			}
		}
		for (File f : fs) {
			if (!f.getName().toLowerCase().endsWith(".sift")) {
				continue;
			}
			imageFiles.add(f);
		}

		
		CompressFeatures compress = new CompressFeatures(clusters, modulo);
		compress.initialize(new File(path+"centroids/1-"+clusters+".out"), imageFiles);
		for (File f : imageFiles) {
			compress.compress(new File(path+"features/"+f.getName()));
		}
	}
}
