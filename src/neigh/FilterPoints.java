package neigh;
/**
 * @author Agnieszka Glabala
 * 
 * Tworzy nowy plik z SIFT-ami w tym samym miejscu (dodaje rozszrezenie z EXT)
 * maxArea - zostawia tylko te sifty które mają nawiększe pole dla danego klastra
 */

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;

import run.Paths;
import sim.FloatKMeans;
import basic.FeatureSetLoader;
import basic.IFeature;

public class FilterPoints 
{
	private static String EXT;
	private String db;
	private String[] imgs;
	private FeatureSetLoader lf;
	private FeatureSetLoader ln; 
	private int clNum = 1000;
	private FloatKMeans cluster;
	
	private List<File> content = new ArrayList<File>();
	private Map<File, int[]> cFeatures = new HashMap<File, int[]>();//przypasowanie plik-klastry
	private Map<File, float[][]> cParams = new HashMap<File, float[][]>();//przypasowanie plik-parametry

	//private float[][] cAreas = new float[1000][];
	public FilterPoints(String db, String ext)
	{
		EXT = ext;
		File dir =new File(Paths.imPath); 
		this.db=db;
		
		imgs = dir.list(new FilenameFilter() {
			public boolean accept(File dir, String name)
			{
				return name.toLowerCase().endsWith(".png");
			}
		});
		lf = new FeatureSetLoader(FeatureSetLoader.HARAFFSIFT,"",db);
		ln = new FeatureSetLoader(FeatureSetLoader.HARAFFSIFT, ".neighs","");

	}
	
	private void setFeatures()
	{
		File featFile = new File(Paths.path+db+"-"+this.clNum+".feats");
		if (!featFile.exists()) {
			System.out.print("Assigning of cluster centers");
			for (File img : this.content) {
				int[] cFeat = this.getFeaturs(img);
				this.cFeatures.put(img, cFeat);
				float[][] cParams = this.getParams(img);
				this.cParams.put(img, cParams);
				System.out.print(".");
			}
			System.out.println();
			this.storeFeatures(featFile);
		} else {
			System.out.println("Read clustered features...");
			this.readFeatures(featFile);
		}
	}
	
	private float[][] getParams(File img) {
		IFeature[] fs = ln.getFeatures(img);
		IFeature f = fs[0];
		float[][] feat = f.getParams();
		return feat;
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
	private void setCluster()
	{
		File clusterFile = new File(Paths.path+db+"-"+this.clNum+".kmeans");
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
	private boolean readFeatures(File file) {
		BufferedReader in = null;
		System.out.println(file);
		try {
	        in = new BufferedReader(new FileReader(file));
	        String line = in.readLine();
	        int n = Integer.parseInt(line);
	        
	        for (int i = 0; i < n; i++) {
	        	line = in.readLine();
	        	String[] words = line.split(" ");
	        	File img = new File(words[0]);
	        	int[] cl = new int[words.length - 1];
	        	for (int j = 1; j < words.length; j++) {
	        		cl[j - 1] = Integer.parseInt(words[j]);
	        	}
	        	this.cFeatures.put(img, cl);
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
	
	
	
	public static void main(String[] args) throws IOException
	{
			FilterPoints fp = new FilterPoints(args[0], args[1]);
			
			fp.setFeatures();
			fp.setCluster();
			int j=0;
			for(String img : fp.imgs)
			{
				System.out.println("["+j+"/"+fp.imgs.length+"]");
				j++;
				//wczytanie danych obrazu
				File imgF = new File(Paths.fPath+img);
				
				//przypasowanie dla każdej linii klastra
				int[] przypasowania = fp.getFeaturs(imgF); 
				
				//przypasowanie dla każdej linii parametrów
				float[][] params = fp.getParams(imgF); //parametry dla każdej linii
				
				//wczytanie wszystkich linii z siftami w stringach (żeby je potem łatwo do nowego pliku zapisać)
				Scanner sc = new Scanner(new File(Paths.fPath + img+ ".haraff.sift"));
				sc.nextInt();//długość linii
				int n = sc.nextInt(); //ile linii
				sc.nextLine();
				String[] linie = new String[n];
				for(int i=0; i<n; i++)
				{
					linie[i]=sc.nextLine();
				}
				
				
				//pojemnik na wszystkie linie:
				SIFTLine[] sls = new SIFTLine[params.length];
				
				//dodanie elementów z danymi
				for(int i=0;i<params.length;i++)//po wszystkich elipsach
				{
					float[] p = params[i];
					sls[i]=fp.new SIFTLine(linie[i],przypasowania[i],(float)(Math.PI/Math.sqrt(p[2]*p[4]-p[3]*p[3])), p[0], p[1],EXT);
				}
				/*
				 * wersja maxArea1000
				 */
				if(EXT.equals("maxArea1000")) {
					Arrays.sort(sls);
					sls=Arrays.copyOfRange(sls, 0, Math.min(1000,params.length));
				}
				
				/*
				 * wersja maxAreaCluster
				 */
				else if(EXT.equals("maxAreaCluster")) {
					Arrays.sort(sls);
					ArrayList<SIFTLine> sl_temp= new ArrayList<SIFTLine>();
					sl_temp.add(sls[0]);//dodajemy punkt z nawiększym polem
					//leci po wszystkich posortowanych siftach i sprawdza czy jest już taki o tym samym klastrze, jak nie to go dodaje
					for(SIFTLine sl : sls)
					{
						if(!fp.isIn(sl_temp, sl)) sl_temp.add(sl);
					}
					sls=sl_temp.toArray(new SIFTLine[sl_temp.size()]);
				}
	
				/*
				 * wersja minArea1000
				 */
				else if(EXT.equals("minArea1000")){
					Arrays.sort(sls);
					sls=Arrays.copyOfRange(sls, 0, Math.min(1000,params.length));
				}
				
				/*
				 * wersja minAreaCluster
				 */ 
				else {
					Arrays.sort(sls);
					ArrayList<SIFTLine> sl_temp= new ArrayList<SIFTLine>();
					sl_temp.add(sls[0]);//dodajemy punkt z nawiększym polem
					//leci po wszystkich posortowanych siftach i sprawdza czy jest już taki o tym samym klastrze, jak nie to go dodaje
					for(SIFTLine sl : sls)
					{
						if(!fp.isIn(sl_temp, sl)) sl_temp.add(sl);
					}
					sls=sl_temp.toArray(new SIFTLine[sl_temp.size()]);
				}
				//zapis tablicy do pliku
				PrintWriter out = new PrintWriter(new FileWriter(new File(Paths.fPath + img+ ".haraff.sift."+EXT)));
				String temp="";
				n=0;
				for(SIFTLine sl : sls)
				{
					temp+=sl.line+"\n";
				}
	
				out.println(128);
				out.println(sls.length);
				out.print(temp.trim());
				out.flush();
				out.close();
				
			}
			
	}
	private boolean isIn(ArrayList<SIFTLine> sls, SIFTLine sl)
	{
		for(SIFTLine s : sls)
		{
			if(s.cluster==sl.cluster)
				return true;
		}
		return false;
	}
	
		

	private class SIFTLine implements Comparable
	{
		String line;
		int cluster;
		double area;
		double x,y;
		String ext;
		SIFTLine(String line, int cluster, double area, double x, double y, String ext)
		{
			this.line=line;
			this.cluster=cluster;
			this.area=area;
			this.x=x;
			this.y=y;
			this.ext=ext;
		}

		@Override
		public int compareTo(Object o) {
			SIFTLine sl = (SIFTLine)o;
			
			//z minusem sortuje rosnąco 
			//bez - malejąco
			if(this.ext.equals("minAreaCluster") || this.ext.equals("minArea1000"))
				return -(int)((this.area-sl.area)*100);
			else
				return (int)((this.area-sl.area)*100);
		}
		
	}
} 
